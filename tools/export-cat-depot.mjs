import assert from 'node:assert/strict';
import {mkdirSync, readFileSync, writeFileSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const rounded = value => Math.round(value * 1e8) / 1e8;
// Blockbench's Bedrock authoring space is centered on X/Z; Java blocks start at 0.
const point = value => value.map((v, axis) => rounded(v + (axis === 1 ? 0 : 8)));

export function exportDepot(source) {
  const groups = new Map((source.groups ?? []).map(group => [group.uuid, group]));
  const included = new Set();
  function collect(nodes) {
    for (const node of nodes) {
      if (typeof node === 'string') { included.add(node); continue; }
      const group = groups.get(node.uuid) ?? node;
      if (group.export === false) continue;
      assert(!group.rotation?.some(v => v !== 0), 'Group rotations must be baked before depot export');
      collect(node.children ?? []);
    }
  }
  collect(source.outliner);
  const elements = source.elements.filter(e => e.export !== false && included.has(e.uuid)).map(e => {
    const element = {name: e.name, from: point(e.from), to: point(e.to), faces: {}};
    if (e.shade === false) element.shade = false;
    const rotations = (e.rotation ?? [0, 0, 0]).map((angle, axis) => ({angle, axis})).filter(r => r.angle);
    assert(rotations.length <= 1, 'Java block elements support only a single axis rotation');
    if (rotations.length) {
      const {angle, axis} = rotations[0];
      assert([-45, -22.5, 22.5, 45].includes(angle), 'Unsupported depot element rotation');
      element.rotation = {angle, axis: 'xyz'[axis], origin: point(e.origin), rescale: e.rescale === true};
    }
    for (const [direction, face] of Object.entries(e.faces)) {
      if (face.texture === null || face.texture === undefined) continue;
      assert.equal(face.texture, 0, 'Unexpected depot texture index');
      const result = {uv: face.uv.map((v, index) => rounded(v * 16 / (index % 2 ? source.resolution.height : source.resolution.width))), texture: '#0'};
      if (face.rotation) result.rotation = face.rotation;
      if (face.cullface) result.cullface = face.cullface;
      element.faces[direction] = result;
    }
    return element;
  });
  return {parent: 'minecraft:block/block', render_type: 'minecraft:cutout',
    textures: {'0': 'laowu:block/cat_depot', particle: 'laowu:block/cat_depot'}, elements};
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const sourceRoot = path.join(root, 'art/cat-machines/sixway/cat_depot');
  const source = JSON.parse(readFileSync(path.join(sourceRoot, 'model.bbmodel'), 'utf8'));
  const assets = new Map();
  const add = (name, data) => assets.set(name, Buffer.from(JSON.stringify(data, null, 2) + '\n'));
  add('models/block/cat_depot.json', exportDepot(source));
  add('models/item/cat_depot.json', {parent: 'laowu:block/cat_depot'});
  add('blockstates/cat_depot.json', {variants: {'': {model: 'laowu:block/cat_depot'}}});
  assets.set('textures/block/cat_depot.png', readFileSync(path.join(sourceRoot, 'texture.png')));
  for (const loader of ['forge-1.20.1', 'neoforge-1.21.1']) for (const [name, bytes] of assets) {
    if(name==='blockstates/cat_depot.json')continue; // Owned by export-cat-sixway.mjs.
    const target = path.join(root, loader, 'src/main/resources/assets/laowu', name);
    if (process.argv.includes('--write')) {
      mkdirSync(path.dirname(target), {recursive: true});
      writeFileSync(target, bytes);
    } else assert.deepEqual(readFileSync(target), bytes, `${loader}/${name} is out of date`);
  }
  console.log('PASS: four cat depot assets per loader; original texture bytes preserved');
}
