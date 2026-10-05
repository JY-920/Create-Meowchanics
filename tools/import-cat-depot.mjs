// Import an authored Blockbench asset without repainting or resampling its texture.
import {readFileSync, writeFileSync, mkdirSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const sourcePath=process.argv[2];
assert(sourcePath, 'Usage: node tools/import-cat-depot.mjs <model.bbmodel>');
const bytes=readFileSync(sourcePath);
const model=JSON.parse(bytes);
assert.equal(model.textures.length,1);
assert(model.textures[0].source.startsWith('data:image/png;base64,'));
const target=path.join(root,'art/cat-machines/sixway/cat_depot');
mkdirSync(target,{recursive:true});
writeFileSync(path.join(target,'model.bbmodel'),bytes);
writeFileSync(path.join(target,'texture.png'),Buffer.from(model.textures[0].source.split(',')[1],'base64'));
console.log('Imported authored model and embedded PNG; no geometry or pixels altered');
