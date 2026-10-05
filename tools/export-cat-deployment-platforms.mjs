import assert from 'node:assert/strict';
import {copyFileSync, mkdirSync, readFileSync, writeFileSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const sourceRoot = path.join(root, 'assets-source/cat-deployment-platforms');
const definitions = [
  ['cat_deployment_platform', '哈基部署平台'],
  ['cat_ejecting_deployment_platform', '哈基弹射部署平台'],
];
const round = value => Math.round(value * 1e8) / 1e8;
const point = value => value.map((v, axis) => round(v + (axis === 1 ? 0 : 8)));
const sideTurn = {north: 'west', west: 'south', south: 'east', east: 'north', up: 'up', down: 'down'};

function turnElement(element, origin) {
  const turn = ([x, y, z]) => [round(origin[0] + z - origin[2]), y, round(origin[2] - x + origin[0])];
  const a = turn(element.from), b = turn(element.to);
  // Swap only the sign-reversed Z axis. Sorting bounds destroys inside-facing cubes.
  element.from = [a[0], a[1], b[2]];
  element.to = [b[0], b[1], a[2]];
  element.faces = Object.fromEntries(Object.entries(element.faces).map(([side, original]) => {
    const face = {...original};
    if (side === 'up' || side === 'down') {
      face.rotation = ((face.rotation ?? 0) + (side === 'up' ? 270 : 90)) % 360;
      if (!face.rotation) delete face.rotation;
    }
    if (face.cullface) face.cullface = sideTurn[face.cullface];
    return [sideTurn[side], face];
  }));
}

/** Preserve Blockbench signed bounds and mirrored UV corners in a Java baked model. */
export function exportDeploymentModel(source, id, includedGroups = null) {
  const groups = new Map((source.groups ?? []).map(group => [group.uuid, group]));
  const included = new Set();
  function collect(nodes, selected = includedGroups === null) {
    for (const node of nodes ?? []) {
      if (typeof node === 'string') {
        if (selected) included.add(node);
        continue;
      }
      const group = groups.get(node.uuid) ?? node;
      if (group.export === false) continue;
      assert(!group.rotation?.some(v => v !== 0), 'Group rotations must be baked before deployment export');
      collect(node.children, selected || includedGroups?.includes(group.name));
    }
  }
  collect(source.outliner);
  const elements = source.elements.filter(e => e.export !== false && included.has(e.uuid)).map(e => {
    const element = {name: e.name, from: point(e.from), to: point(e.to), faces: {}};
    if (e.shade === false) element.shade = false;
    for (const [direction, face] of Object.entries(e.faces)) {
      if (face.texture === null || face.texture === undefined) continue;
      assert.equal(face.texture, 0, 'Unexpected deployment texture index');
      const result = {uv: face.uv.map((v, index) => round(v * 16 / (index % 2 ? source.resolution.height : source.resolution.width))), texture: '#0'};
      if (face.rotation) result.rotation = face.rotation;
      if (face.cullface) result.cullface = face.cullface;
      element.faces[direction] = result;
    }
    const rotations = (e.rotation ?? [0, 0, 0]).map((angle, axis) => ({angle, axis})).filter(r => r.angle);
    assert(rotations.length <= 1, 'Java block elements support only a single axis rotation');
    if (rotations.length) {
      const {angle, axis} = rotations[0];
      if (axis === 1 && angle % 90 === 0) {
        const turns = ((angle / 90) % 4 + 4) % 4;
        for (let n = 0; n < turns; n++) turnElement(element, point(e.origin));
      } else {
        assert([-45, -22.5, 22.5, 45].includes(angle), `Unsupported deployment rotation: ${angle}`);
        element.rotation = {angle, axis: 'xyz'[axis], origin: point(e.origin), rescale: e.rescale === true};
      }
    }
    return element;
  });
  return {parent: 'minecraft:block/block', render_type: 'minecraft:cutout',
    textures: {'0': `laowu:block/${id}`, particle: `laowu:block/${id}`}, elements};
}

/** Separate the retractable plate and its nested telescopic rod from the static housing. */
export function exportDeploymentParts(source, id) {
  const complete = exportDeploymentModel(source, id);
  if (id !== 'cat_ejecting_deployment_platform') return {complete, base: complete};
  const baseSource = structuredClone(source);
  const groups = new Map(source.groups.map(group => [group.uuid, group]));
  baseSource.outliner = baseSource.outliner.filter(node => typeof node === 'string' || groups.get(node.uuid)?.name === 'bone');
  const plateSource = structuredClone(source);
  const rodGroup = source.outliner.find(node => typeof node !== 'string' && groups.get(node.uuid)?.name === 'bone2')
    ?.children.find(node => typeof node !== 'string' && groups.get(node.uuid)?.name === 'bone3');
  assert(rodGroup, 'Authored ejector nested rod is missing');
  const rodIds = new Set(rodGroup.children);
  plateSource.elements = plateSource.elements.filter(e => !rodIds.has(e.uuid));
  return {complete, base: exportDeploymentModel(baseSource, id),
    plate: exportDeploymentModel(plateSource, id, ['bone2']),
    rod: exportDeploymentModel(source, id, ['bone3'])};
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const write = process.argv.includes('--write');
  if (process.argv.includes('--import')) {
    assert(write, '--import requires --write');
    const artistRoot = 'D:/Project_minecraft/待实现/猫战斗';
    for (const [id, folder] of definitions) {
      const target = path.join(sourceRoot, id);
      mkdirSync(target, {recursive: true});
      for (const file of ['model.bbmodel', 'model.geo.json', 'texture.png'])
        copyFileSync(path.join(artistRoot, folder, file), path.join(target, file));
    }
  }
  const assets = new Map();
  const add = (name, data) => assets.set(name, Buffer.from(JSON.stringify(data, null, 2) + '\n'));
  for (const [id] of definitions) {
    const folder = path.join(sourceRoot, id);
    const source = JSON.parse(readFileSync(path.join(folder, 'model.bbmodel'), 'utf8'));
    const parts = exportDeploymentParts(source, id);
    add(`models/block/${id}.json`, parts.base);
    add(`models/item/${id}.json`, {...parts.complete, display: {
      gui: {rotation: [30, 225, 0], translation: [0, 0, 0], scale: [.6, .6, .6]},
      ground: {translation: [0, 3, 0], scale: [.25, .25, .25]},
      fixed: {scale: [.5, .5, .5]},
      thirdperson_righthand: {rotation: [75, 45, 0], translation: [0, 2.5, 0], scale: [.375, .375, .375]},
      firstperson_righthand: {rotation: [0, 45, 0], scale: [.4, .4, .4]},
      firstperson_lefthand: {rotation: [0, 225, 0], scale: [.4, .4, .4]},
    }});
    add(`blockstates/${id}.json`, {variants: Object.fromEntries(
      Object.entries({north: 0, east: 90, south: 180, west: 270}).map(([facing, y]) =>
        [`facing=${facing}`, {model: `laowu:block/${id}`, ...(y ? {y} : {})}]))});
    assets.set(`textures/block/${id}.png`, readFileSync(path.join(folder, 'texture.png')));
    if (parts.plate) {
      add(`models/block/${id}_plate.json`, parts.plate);
      add(`models/block/${id}_rod.json`, parts.rod);
    }
  }
  for (const loader of ['forge-1.20.1', 'neoforge-1.21.1']) for (const [name, bytes] of assets) {
    const target = path.join(root, loader, 'src/main/resources/assets/laowu', name);
    if (write) {
      mkdirSync(path.dirname(target), {recursive: true});
      writeFileSync(target, bytes);
    } else assert.deepEqual(readFileSync(target), bytes, `${loader}/${name} is out of date`);
  }
  console.log(`PASS: ${assets.size} deployment assets per loader; original textures preserved`);
}
