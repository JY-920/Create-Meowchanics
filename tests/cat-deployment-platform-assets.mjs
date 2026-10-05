import assert from 'node:assert/strict';
import {existsSync, readFileSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath, pathToFileURL} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const exporter = path.join(root, 'tools/export-cat-deployment-platforms.mjs');
assert(existsSync(exporter), 'Deployment platform exporter is missing');
const {exportDeploymentModel, exportDeploymentParts} = await import(pathToFileURL(exporter));

// A min/max normalization loses the author's inside-facing aperture walls.
// A missed quarter turn leaves both sideways apertures outside the block.
const fixture = {resolution: {width: 64, height: 64}, groups: [], outliner: ['port'], elements: [{
  uuid: 'port', name: 'cube_outline', from: [12, 10, 1], to: [2, 3, -2],
  rotation: [0, 90, 0], origin: [7, 0, 0],
  faces: {north: {uv: [44, 14, 54, 21], texture: 0},
    up: {uv: [10, 55, 0, 52], texture: 0}, down: {texture: null}},
}]};
const model = exportDeploymentModel(fixture, 'fixture');
assert.deepEqual(model.elements[0].from, [16, 10, 13]);
assert.deepEqual(model.elements[0].to, [13, 3, 3]);
assert(!model.elements[0].rotation, 'Quarter turns must be baked, not invalid Java rotations');
assert.deepEqual(model.elements[0].faces.west, {uv: [11, 3.5, 13.5, 5.25], texture: '#0'});
assert.deepEqual(model.elements[0].faces.up, {uv: [2.5, 13.75, 0, 13], texture: '#0', rotation: 270});
assert(!('down' in model.elements[0].faces));
assert.throws(() => exportDeploymentModel({...fixture, elements: [{...fixture.elements[0], rotation: [45, 90, 0]}]}, 'fixture'), /single axis/);
assert.throws(() => exportDeploymentModel({...fixture, elements: [{...fixture.elements[0], rotation: [0, 13, 0]}]}, 'fixture'), /Unsupported/);

const ids = ['cat_deployment_platform', 'cat_ejecting_deployment_platform'];
for (const id of ids) {
  const source = path.join(root, 'assets-source/cat-deployment-platforms', id);
  const authored = JSON.parse(readFileSync(path.join(source, 'model.bbmodel'), 'utf8'));
  const parts = exportDeploymentParts(authored, id);
  assert.equal(parts.complete.elements.length, id === ids[0] ? 6 : 8, 'Do not import stale six-cube ejector geometry');
  assert.deepEqual(parts.complete.elements[0].from, [0, 0, 0]);
  assert.deepEqual(parts.complete.elements[0].to, [16, 13, 16]);
  assert.deepEqual(parts.complete.elements[3].from, [16, 10, 13]);
  assert.deepEqual(parts.complete.elements[3].to, [13, 3, 3]);
  if (id === ids[1]) {
    assert.equal(parts.base.elements.length, 6);
    assert.deepEqual(parts.plate.elements[0].from, [2, 12, 2]);
    assert.deepEqual(parts.plate.elements[0].to, [14, 14, 14]);
    assert.deepEqual(parts.rod.elements[0].from, [6.5, 10, 6.5]);
    assert.deepEqual(parts.rod.elements[0].to, [9.5, 12, 9.5]);
    assert.equal(authored.animations[0].length, 1);
    const animators = authored.animations[0].animators;
    const atPeak = (group, channel) => animators[group].keyframes.find(k => k.channel === channel && k.time === .5).data_points[0];
    assert.equal(atPeak('3d6eab67-310d-436c-ef93-4732e401e307', 'position').y, '16');
    assert.equal(atPeak('06b181df-5cf1-403f-560e-26d653926445', 'position').y, '-7');
    assert.equal(Number(atPeak('06b181df-5cf1-403f-560e-26d653926445', 'scale').y), 8);
  }
  for (const loader of ['forge-1.20.1', 'neoforge-1.21.1']) {
    const assets = path.join(root, loader, 'src/main/resources/assets/laowu');
    const read = name => JSON.parse(readFileSync(path.join(assets, name), 'utf8'));
    const block = read(`models/block/${id}.json`);
    assert.equal(block.elements.length, 6);
    assert.equal(block.render_type, 'minecraft:cutout');
    assert.equal(read(`models/item/${id}.json`).elements.length, id === ids[0] ? 6 : 8, 'Inventory must include the retracted moving parts');
    const variants = read(`blockstates/${id}.json`).variants;
    assert.deepEqual(Object.keys(variants).sort(), ['facing=east', 'facing=north', 'facing=south', 'facing=west']);
    assert.deepEqual(variants['facing=north'], {model: `laowu:block/${id}`});
    assert.equal(variants['facing=east'].y, 90);
    assert.equal(variants['facing=south'].y, 180);
    assert.equal(variants['facing=west'].y, 270);
    assert.deepEqual(readFileSync(path.join(assets, `textures/block/${id}.png`)), readFileSync(path.join(source, 'texture.png')), 'Authored pixels must remain byte-exact');
    if (id === ids[1]) {
      assert.equal(read(`models/block/${id}_plate.json`).elements.length, 1);
      assert.equal(read(`models/block/${id}_rod.json`).elements.length, 1);
    }
  }
}
console.log('PASS: deployment platform signed apertures, baked quarter turns, inventory, animated ejector parts, exact textures and dual-loader resource parity');
