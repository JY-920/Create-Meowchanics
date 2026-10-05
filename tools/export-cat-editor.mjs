import assert from 'node:assert/strict';
import {copyFileSync, mkdirSync, readFileSync, writeFileSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const sourceRoot = path.join(root, 'assets-source/cat-editor');
const round = n => Math.round(n * 1e10) / 1e10;

// Same face winding/UV conventions as RuntimeBlockbenchModel and Java cube faces.
// Keep the signed from/to coordinates: sorting changes winding and mirrored UVs.
function faceVertices(direction, a, b, uv) {
  const [x0, y0, z0] = a, [x1, y1, z1] = b, [u1, v1, u2, v2] = uv;
  const faces = {
    east: [[x1,y1,z0,u2,v1], [x1,y1,z1,u1,v1], [x1,y0,z1,u1,v2], [x1,y0,z0,u2,v2]],
    west: [[x0,y0,z1,u2,v2], [x0,y1,z1,u2,v1], [x0,y1,z0,u1,v1], [x0,y0,z0,u1,v2]],
    up: [[x0,y1,z1,u1,v2], [x1,y1,z1,u2,v2], [x1,y1,z0,u2,v1], [x0,y1,z0,u1,v1]],
    down: [[x1,y0,z0,u2,v2], [x1,y0,z1,u2,v1], [x0,y0,z1,u1,v1], [x0,y0,z0,u1,v2]],
    south: [[x1,y0,z1,u2,v2], [x1,y1,z1,u2,v1], [x0,y1,z1,u1,v1], [x0,y0,z1,u1,v2]],
    north: [[x0,y1,z0,u2,v1], [x1,y1,z0,u1,v1], [x1,y0,z0,u1,v2], [x0,y0,z0,u2,v2]],
  };
  assert(faces[direction], `Unknown face: ${direction}`);
  return faces[direction];
}

function rotatePoint(vertex, element) {
  const origin = element.origin ?? [0, 0, 0];
  let [x, y, z] = vertex.map((v, axis) => v - origin[axis]);
  const [rx, ry, rz] = (element.rotation ?? [0, 0, 0]).map(v => v * Math.PI / 180);
  // Authored cube rotations are single-axis. Refuse ambiguous compound Euler order.
  assert([rx, ry, rz].filter(Boolean).length <= 1, 'Bake compound rotations before cat editor OBJ export');
  if (rx) [y, z] = [y * Math.cos(rx) - z * Math.sin(rx), y * Math.sin(rx) + z * Math.cos(rx)];
  if (ry) [x, z] = [x * Math.cos(ry) + z * Math.sin(ry), -x * Math.sin(ry) + z * Math.cos(ry)];
  if (rz) [x, y] = [x * Math.cos(rz) - y * Math.sin(rz), x * Math.sin(rz) + y * Math.cos(rz)];
  // Blockbench Bedrock authoring is centered on X/Z; OBJ is in block units.
  return [x, y, z].map((v, axis) => round((v + origin[axis] + (axis === 1 ? 0 : 8)) / 16));
}

/** Exact static OBJ export: arbitrary paper angle, signed extents and UV corners. */
export function exportEditorObj(source) {
  assert(!source.animations?.length, 'Cat editor OBJ export requires a static source');
  const groups = new Map((source.groups ?? []).map(g => [g.uuid, g]));
  const included = new Set();
  function collect(nodes) {
    for (const node of nodes ?? []) {
      if (typeof node === 'string') { included.add(node); continue; }
      const group = groups.get(node.uuid) ?? node;
      if (group.export === false) continue;
      assert(!group.rotation?.some(v => v), 'Bake group rotations before cat editor OBJ export');
      collect(node.children);
    }
  }
  collect(source.outliner);
  const lines = ['# Exact static export of the supplied cat editor Blockbench model.', 'mtllib cat_editor.mtl', 'usemtl editor'];
  let count = 0;
  for (const element of source.elements.filter(e => e.export !== false && included.has(e.uuid))) {
    assert.equal(element.type ?? 'cube', 'cube', 'Only authored cubes are supported');
    lines.push(`o editor_${element.uuid}`);
    for (const [direction, face] of Object.entries(element.faces)) {
      if (face.texture === null || face.texture === undefined) continue;
      assert.equal(face.texture, 0, 'Unexpected cat editor texture index');
      const raw = faceVertices(direction, element.from, element.to, face.uv);
      const a = raw[1].map((v, i) => v - raw[0][i]), b = raw[2].map((v, i) => v - raw[0][i]);
      const normal = [a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]];
      // The authored zero-thickness paper has four zero-area edges. They cannot
      // produce baked normals; its real up/down faces remain separate quads.
      if (!normal.some(v => v !== 0)) continue;
      assert(!element.rescale, 'Unexpected rescaled cat editor cube');
      const positions = raw.map(v => rotatePoint(v.slice(0, 3), element));
      const shift = ((face.rotation ?? 0) / 90 % 4 + 4) % 4;
      assert.equal(shift % 1, 0, 'UV rotation must use quarter turns');
      const uvs = raw.map((_, i) => {
        const v = raw[(i - shift + 4) % 4];
        // OBJ is bottom-origin; the configured flip_v=true turns it back into
        // the original top-origin PNG UV. Without this, opaque areas disappear.
        return [round(v[3] / source.resolution.width), round(1 - v[4] / source.resolution.height)];
      });
      lines.push(...positions.map(p => `v ${p.join(' ')}`), ...uvs.map(p => `vt ${p.join(' ')}`));
      lines.push(`f ${[1,2,3,4].map(v => `${count+v}/${count+v}`).join(' ')}`);
      count += 4;
    }
  }
  return lines.join('\n') + '\n';
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const write = process.argv.includes('--write');
  if (process.argv.includes('--import')) {
    assert(write, '--import requires --write');
    mkdirSync(sourceRoot, {recursive: true});
    for (const [from, to] of [
      ['D:/Project_minecraft/待实现/猫词条机/猫词条机.bbmodel', 'model.bbmodel'],
      ['D:/Project_minecraft/待实现/猫词条机/猫词条机.geo.json', 'model.geo.json'],
      ['D:/Project_minecraft/待实现/猫词条机/猫词条机.png', 'texture.png'],
      ['D:/Project_minecraft/待实现/UI猫大本营.png', 'gui.png'],
      ['C:/Users/16611/AppData/Local/Temp/codex-clipboard-b9a8b8cf-a153-4d01-9e51-83c9ed124db6.png', 'cat_trait_token.png'],
    ]) copyFileSync(from, path.join(sourceRoot, to));
  }
  const source = JSON.parse(readFileSync(path.join(sourceRoot, 'model.bbmodel'), 'utf8'));
  const obj = exportEditorObj(source);
  const common = new Map([
    ['models/block/cat_editor.obj', Buffer.from(obj)],
    ['models/block/cat_editor.mtl', Buffer.from('newmtl editor\nmap_Kd #0\n')],
  ]);
  for (const [target, file] of [
    ['models/block/cat_editor.bbmodel', 'model.bbmodel'],
    ['models/entity/cat_editor.geo.json', 'model.geo.json'],
    ['textures/block/cat_editor.png', 'texture.png'],
    ['textures/gui/cat_editor.png', 'gui-v2.png'],
    ['textures/item/cat_trait_token.png', 'cat_trait_token.png'],
  ]) common.set(target, readFileSync(path.join(sourceRoot, file)));
  for (const loader of ['forge-1.20.1', 'neoforge-1.21.1']) {
    const assets = new Map(common);
    const add = (name, data) => assets.set(name, Buffer.from(JSON.stringify(data, null, 2) + '\n'));
    const block = {parent: 'minecraft:block/block', loader: `${loader.startsWith('neo') ? 'neoforge' : 'forge'}:obj`,
      model: 'laowu:models/block/cat_editor.obj', automatic_culling: false, shade_quads: true,
      flip_v: true, render_type: 'minecraft:cutout',
      textures: {'0': 'laowu:block/cat_editor', particle: 'laowu:block/cat_editor'}};
    add('models/block/cat_editor.json', block);
    add('models/item/cat_editor.json', {...block, display: {
      gui: {rotation: [30, 225, 0], scale: [.8, .8, .8]},
      ground: {translation: [0, 3, 0], scale: [.4, .4, .4]},
      fixed: {scale: [.7, .7, .7]},
      thirdperson_righthand: {rotation: [75, 45, 0], translation: [0, 2.5, 0], scale: [.5, .5, .5]},
      firstperson_righthand: {rotation: [0, 45, 0], scale: [.5, .5, .5]},
      firstperson_lefthand: {rotation: [0, 225, 0], scale: [.5, .5, .5]},
    }});
    add('blockstates/cat_editor.json', {variants: Object.fromEntries(
      Object.entries({north: 0, east: 90, south: 180, west: 270}).map(([facing, y]) =>
        [`facing=${facing}`, {model: 'laowu:block/cat_editor', ...(y ? {y} : {})}]))});
    add('models/item/cat_trait_token.json', {"parent":"minecraft:builtin/entity","gui_light":"front","textures":{"particle":"laowu:item/cat_trait_token"},"display":{"ground":{"translation":[0,2,0],"scale":[0.5,0.5,0.5]},"head":{"rotation":[0,180,0],"translation":[0,13,7]},"thirdperson_righthand":{"translation":[0,3,1],"scale":[0.55,0.55,0.55]},"thirdperson_lefthand":{"translation":[0,3,1],"scale":[0.55,0.55,0.55]},"firstperson_righthand":{"rotation":[0,-90,25],"translation":[1.13,3.2,1.13],"scale":[0.68,0.68,0.68]},"firstperson_lefthand":{"rotation":[0,90,-25],"translation":[1.13,3.2,1.13],"scale":[0.68,0.68,0.68]},"fixed":{"rotation":[0,180,0]}}});
    for (const [name, bytes] of assets) {
      const target = path.join(root, loader, 'src/main/resources/assets/laowu', name);
      if (write) { mkdirSync(path.dirname(target), {recursive: true}); writeFileSync(target, bytes); }
      else assert.deepEqual(readFileSync(target), bytes, `${loader}/${name} is out of date`);
    }
  }
  console.log('PASS: 11 cat editor assets per loader; exact static OBJ and original texture/UI/token bytes');
}
