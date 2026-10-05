import assert from 'node:assert/strict';
import {createHash} from 'node:crypto';
import {existsSync, readFileSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath, pathToFileURL} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const exporter = path.join(root, 'tools/export-cat-editor.mjs');
assert(existsSync(exporter), 'Cat editor OBJ exporter is missing');
const {exportEditorObj} = await import(pathToFileURL(exporter));
const parseObj = obj => ({
  vertices: obj.split('\n').filter(s => s.startsWith('v ')).map(s => s.slice(2).split(' ').map(Number)),
  uvs: obj.split('\n').filter(s => s.startsWith('vt ')).map(s => s.slice(3).split(' ').map(Number)),
  faces: obj.split('\n').filter(s => s.startsWith('f ')),
});
// A real inverted fixture catches min/max normalization and reflected UV changes.
const fixture = {resolution: {width: 64, height: 64}, groups: [], outliner: ['signed'], elements: [{
  uuid: 'signed', from: [2, 3, 1], to: [-2, 1, -1], origin: [0, 0, 0],
  faces: {north: {uv: [8, 12, 4, 16], texture: 0}},
}]};
const signed = parseObj(exportEditorObj(fixture));
assert.deepEqual(signed.vertices, [[.625, .0625, .5625], [.375, .0625, .5625], [.375, .1875, .5625], [.625, .1875, .5625]]);
// Standard OBJ stores bottom-origin V. flip_v=true converts back to PNG's top-origin.
// Feeding PNG V directly into that loader samples the transparent lower atlas half.
assert.deepEqual(signed.uvs, [[.0625, .8125], [.125, .8125], [.125, .75], [.0625, .75]]);
const paperFixture = {...fixture, outliner: ['paper'], elements: [{uuid: 'paper',
  from: [-1, 0, 0], to: [1, 0, 2], origin: [0, 0, 0], rotation: [-17.5, 0, 0],
  faces: {up: {uv: [0, 0, 2, 2], texture: 0}, north: {uv: [0, 0, 2, 0], texture: 0}},
}]};
const slope = parseObj(exportEditorObj(paperFixture));
assert.equal(slope.faces.length, 1, 'Omit only zero-area paper edges, retain its real face');
assert(Math.abs(slope.vertices[0][1] - .037588224938) < 1e-9, 'Exact -17.5-degree slope must not become -22.5');
assert(Math.abs(slope.vertices[0][2] - .61921461884) < 1e-9);
const source = path.join(root, 'assets-source/cat-editor');
assert(existsSync(path.join(source, 'model.bbmodel')), 'Cat editor authored source is missing');
const bytes = name => readFileSync(path.join(source, name));
for (const [name, hash] of [
  ['model.bbmodel', 'f309bccade71898231e5c62d8c6fe07279d7fbcd704c6b2b4e43ba5958d8cb33'],
  ['model.geo.json', '2de6f8fc3ff3aee820f5128c96615cf86eb83ea7e7a9afe60ca69597f2fda2b6'],
  ['texture.png', '0d6ab74bbb689c02a46bebb016ad1f3b064d4473efac2014c6b6dd6a7ee03961'],
  ['gui.png', '6ffa6f3b34e669917efd395d21016cc42a8685f4d2e4d658515b637dbae8c994'],
  ['cat_trait_token.png', '99f44298f90412e375c0796c79f0c3ef201ad1fcbb0f8021063861d87da1c1d1'],
]) assert.equal(createHash('sha256').update(bytes(name)).digest('hex'), hash, `Do not alter supplied source ${name}`);
const authored = JSON.parse(bytes('model.bbmodel'));
assert.deepEqual(authored.resolution, {width: 64, height: 64});
assert.equal(authored.elements.length, 11, 'The paper and both controls must not disappear');
assert.deepEqual(authored.elements[0].from, [-5, 0, -5]);
assert.deepEqual(authored.elements[0].to, [5, 3, 5]);
assert.deepEqual(authored.elements[10].rotation, [-17.5, 0, 0], 'Keep the exact paper slope');
assert.deepEqual(authored.elements[10].from, [-1.5, 1, -9]);
assert.deepEqual(authored.elements[10].to, [1.5, 1, -5]);
assert.deepEqual(authored.elements[6].faces.west.uv, [10, 28, 8, 30], 'Do not normalize mirrored UVs');
assert(!authored.animations?.length, 'This export is intentionally static');

function dimensions(data) {
  assert.equal(data.subarray(0, 8).toString('hex'), '89504e470d0a1a0a');
  return [data.readUInt32BE(16), data.readUInt32BE(20)];
}
assert.deepEqual(dimensions(bytes('texture.png')), [64, 64]);
assert.deepEqual(dimensions(bytes('gui.png')), [512, 512]);
assert.deepEqual(dimensions(bytes('cat_trait_token.png')), [16, 16]);

for (const loader of ['forge-1.20.1', 'neoforge-1.21.1']) {
  const assets = path.join(root, loader, 'src/main/resources/assets/laowu');
  const read = name => JSON.parse(readFileSync(path.join(assets, name), 'utf8'));
  const obj = parseObj(readFileSync(path.join(assets, 'models/block/cat_editor.obj'), 'utf8'));
  assert.equal(obj.faces.length, 62, 'All ten cubes and both sides of the paper must render');
  assert(obj.vertices.flat().every(Number.isFinite), 'No degenerate normals/positions');
  assert.deepEqual(obj.vertices.slice(-8, -6), [[.40625, .0812941125, .1846073094], [.59375, .0812941125, .1846073094]], 'Exported source paper must retain its exact authored tilt');
  for (const modelPath of ['models/block/cat_editor.json', 'models/item/cat_editor.json']) {
    const model = read(modelPath);
    assert.equal(model.loader, `${loader.startsWith('neo') ? 'neoforge' : 'forge'}:obj`);
    assert.equal(model.model, 'laowu:models/block/cat_editor.obj');
    assert.equal(model.automatic_culling, false, 'Rotated controls and two-sided paper must not be culled');
    assert.equal(model.flip_v, true, 'PNG atlas UVs start at the top');
    assert.equal(model.textures['0'], 'laowu:block/cat_editor');
    assert.equal(model.render_type, 'minecraft:cutout');
  }
  for (const [target, original] of [
    ['textures/block/cat_editor.png', 'texture.png'],
    ['textures/gui/cat_editor.png', 'gui-v2.png'],
    ['textures/item/cat_trait_token.png', 'bottled-trait.png'],
    ['models/entity/cat_editor.geo.json', 'model.geo.json'],
    ['models/block/cat_editor.bbmodel', 'model.bbmodel'],
  ]) assert.deepEqual(readFileSync(path.join(assets, target)), bytes(original), `${loader}/${target} must retain exact source bytes`);
  const variants = read('blockstates/cat_editor.json').variants;
  assert.deepEqual(Object.keys(variants).sort(), ['facing=east', 'facing=north', 'facing=south', 'facing=west']);
  assert.deepEqual(variants['facing=north'], {model: 'laowu:block/cat_editor'});
  for (const [direction, y] of [['east', 90], ['south', 180], ['west', 270]])
    assert.deepEqual(variants[`facing=${direction}`], {model: 'laowu:block/cat_editor', y});
  assert.equal(read('models/item/cat_trait_token.json').parent, 'minecraft:builtin/entity');
}
console.log('PASS: cat editor exact 11-element/62-quad OBJ, paper slope, signed fixture, mirrored UVs, four facings, atlas/token dimensions and dual-loader exact resource copies');
