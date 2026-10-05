import assert from 'node:assert/strict';
import {createHash} from 'node:crypto';
import {existsSync, readFileSync} from 'node:fs';
import path from 'node:path';
const root = path.resolve(import.meta.dirname, '..');
const source = path.join(root, 'assets-source/cat-auto-laser/creature_filter.png');
assert(existsSync(source), 'Original creature filter icon is missing');
const original = readFileSync(source);
assert.equal(createHash('sha256').update(original).digest('hex'), '9f98ca37163bdb85908a96fa332f81a54119b9a272b9a0049eb659f8d15b67d9', 'Do not redraw or crop the supplied icon');
assert.deepEqual([original.readUInt32BE(16), original.readUInt32BE(20)], [16, 16]);
for (const loader of ['forge-1.20.1', 'neoforge-1.21.1']) {
  const assets = path.join(root, loader, 'src/main/resources/assets/laowu');
  assert.deepEqual(readFileSync(path.join(assets, 'textures/item/creature_filter.png')), original);
  assert.deepEqual(JSON.parse(readFileSync(path.join(assets, 'models/item/creature_filter.json'))), {
    parent: 'minecraft:item/generated', textures: {layer0: 'laowu:item/creature_filter'},
  }, 'The filter item must use its normal generated 16px sprite model');
}
console.log('PASS: exact supplied creature filter 16x16 icon and generated item model in both loaders');
