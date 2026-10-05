import assert from 'node:assert/strict';
import {existsSync, readFileSync} from 'node:fs';
import {fileURLToPath, pathToFileURL} from 'node:url';
import path from 'node:path';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const exporter = path.join(root, 'tools/export-cat-depot.mjs');
assert(existsSync(exporter), 'Cat depot exporter is missing');
const {exportDepot} = await import(pathToFileURL(exporter));
// A dropped coordinate offset, reversed UV, or rotation origin detaches the cat's ears.
const fixture = {resolution: {width: 64, height: 64}, groups: [], outliner: ['ear'], elements: [{
  uuid: 'ear', name: 'ear', from: [-7.4, 8.5, 7.6], to: [-4.4, 11.5, 7.6],
  rotation: [0, 0, -45], origin: [-5.9, 10, 7.6],
  faces: {north: {uv: [33, 0, 30, 3], texture: 0}, down: {texture: null}},
}]};
const model = exportDepot(fixture);
assert.deepEqual(model.elements[0].from, [0.6, 8.5, 15.6]);
assert.deepEqual(model.elements[0].to, [3.6, 11.5, 15.6]);
assert.deepEqual(model.elements[0].rotation, {angle: -45, axis: 'z', origin: [2.1, 10, 15.6], rescale: false});
assert.deepEqual(model.elements[0].faces.north, {uv: [8.25, 0, 7.5, 0.75], texture: '#0'});
assert(!('down' in model.elements[0].faces));
assert.throws(() => exportDepot({...fixture, elements: [{...fixture.elements[0], rotation: [45, 0, -45]}]}), /single axis/);

if (!process.argv.includes('--unit-only')) for (const loader of ['forge-1.20.1', 'neoforge-1.21.1']) {
  const assets = path.join(root, loader, 'src/main/resources/assets/laowu');
  const read = name => JSON.parse(readFileSync(path.join(assets, name), 'utf8'));
  const actual = read('models/block/cat_depot.json');
  assert.equal(actual.elements.length, 7, 'All seven updated authored depot parts must be exported');
  assert.deepEqual(actual.elements[0].from, [0, 0, 0]);
  assert.deepEqual(actual.elements[1].to, [15, 13, 15], 'Replacement tabletop is 13 pixels high');
  assert.equal(actual.render_type, 'minecraft:cutout');
  assert.equal(Object.keys(read('blockstates/cat_depot.json').variants).length,6);
  assert.equal(read('blockstates/cat_depot.json').variants['bottom=down'].model,'laowu:block/cat_depot');
  assert.equal(read('models/item/cat_depot.json').parent, 'laowu:block/cat_depot');
  assert.deepEqual(readFileSync(path.join(assets, 'textures/block/cat_depot.png')),
    readFileSync(path.join(root, 'art/cat-machines/sixway/cat_depot/texture.png')));
}
console.log('PASS: depot centered geometry, rotated ears, mirrored UVs, original texture, block and item models');
