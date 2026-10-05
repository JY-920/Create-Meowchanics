import assert from 'node:assert/strict';
import {existsSync, readFileSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath, pathToFileURL} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const exporter = path.join(root, 'tools/export-cat-machines.mjs');
assert(existsSync(exporter), 'Cat machine asset exporter must exist');
const {exportBasin} = await import(pathToFileURL(exporter));
const face = {uv: [16, 0, 28, 14], texture: 0};
const fixture = {
  resolution: {width: 64, height: 64},
  groups: [{uuid: 'body', name: 'Basins', rotation: [0, 0, 0]},
    {uuid: 'spout', name: 'Spoutput', rotation: [0, 0, 0]}],
  outliner: [{uuid: 'body', children: ['inner']}, {uuid: 'spout', children: ['outlet']}],
  elements: [
    {uuid: 'inner', name: 'Basin Interior', from: [1.95, 16, 1.95], to: [14.05, 2, 14.05],
      faces: {north: face, up: {uv: [40, 12, 28, 0], texture: 0}, down: {uv: [0, 0, 0, 0], texture: null}}},
    {uuid: 'outlet', name: 'Outlet', from: [5, 2, 22], to: [11, 8, 21],
      origin: [1, 9, 17], rotation: [22.5, 0, 0], rescale: false,
      faces: {south: {uv: [18, 8, 24, 14], texture: 1, rotation: 90}}},
  ],
};
// Normalizing this Y range would make the basin's inner wall face outwards.
const closed = exportBasin(fixture, false);
assert.equal(closed.elements.length, 1, 'non-directional basin must exclude the outlet group');
assert.deepEqual(closed.elements[0].from, [1.95, 16, 1.95]);
assert.deepEqual(closed.elements[0].to, [14.05, 2, 14.05]);
assert.deepEqual(closed.elements[0].faces.north, {uv: [4, 0, 7, 3.5], texture: '#0'});
assert.deepEqual(closed.elements[0].faces.up.uv, [10, 3, 7, 0]);
assert(!('down' in closed.elements[0].faces), 'null-textured faces must not seal the basin');
const directional = exportBasin(fixture, true);
assert.equal(directional.elements.length, 2);
assert.deepEqual(directional.elements[1].rotation,
  {angle: 22.5, axis: 'x', origin: [1, 9, 17], rescale: false});
assert.deepEqual(directional.elements[1].from, [5, 2, 22]);
assert.deepEqual(directional.elements[1].to, [11, 8, 21]);
assert.deepEqual(directional.elements[1].faces.south,
  {uv: [4.5, 2, 6, 3.5], texture: '#1', rotation: 90});
assert.throws(() => exportBasin({...fixture, elements: [
  {...fixture.elements[0], rotation: [22.5, 22.5, 0]}, fixture.elements[1],
]}, false), /single axis/, 'unsupported compound rotations must not silently lose geometry');

if (!process.argv.includes('--unit-only')) {
  const read = (loader, name) => JSON.parse(readFileSync(path.join(root, loader,
    'src/main/resources/assets/laowu', name), 'utf8'));
  for (const loader of ['forge-1.20.1', 'neoforge-1.21.1']) {
    const basin = read(loader, 'models/block/haji_basin.json');
    const spout = read(loader, 'models/block/haji_basin_directional.json');
    assert.equal(basin.elements.length, 6);
    assert.equal(spout.elements.length, 10);
    assert(basin.elements.some(e => e.name === 'Basin Interior' && e.from[1] > e.to[1]));
    const variants = read(loader, 'blockstates/haji_basin.json').variants;
    assert.equal(Object.keys(variants).length,30);
    for(const bottom of ['down','up','north','south','east','west'])for(const facing of ['down','east','north','south','west'])
      assert(variants[`bottom=${bottom},facing=${facing}`],'Every local spout must exist at every bottom');
    for (const size of ['', '_large']) {
      const id = `cat_encased${size}_cogwheel`;
      const states = read(loader, `blockstates/${id}.json`).variants;
      assert.equal(Object.keys(states).length, 12, 'each axis and independent shaft toggle needs a model');
      for (const axis of ['x', 'y', 'z']) {
        for (const bottom of [false, true]) for (const top of [false, true]) {
          const variant = states[`axis=${axis},bottom_shaft=${bottom},top_shaft=${top}`];
          assert(variant);
          const model = read(loader, `models/${variant.model.replace('laowu:', '')}.json`);
          assert.equal(model.textures.casing, 'laowu:block/cat_casing');
          assert.equal(model.textures['4'], 'laowu:block/cat_casing_shaft_opening');
          assert.equal(model.parent, `create:block/encased${size}_cogwheel/block${top ? '_top' : ''}${bottom ? '_bottom' : ''}`);
        }
      }
    }
    for (const [name, size] of [['cat_casing', 16], ['cat_casing_connected', 128], ['haji_basin', 64], ['haji_basin_spout', 64]]) {
      const bytes = readFileSync(path.join(root, loader, 'src/main/resources/assets/laowu/textures/block', name + '.png'));
      assert.equal(bytes.readUInt32BE(16), size);
      assert.equal(bytes.readUInt32BE(20), size);
    }
  }
}
console.log('PASS: cat machine UVs, reversed winding, rotations, outlet exclusion, state coverage and textures');
