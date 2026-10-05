import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
for(const loader of ['forge-1.20.1','neoforge-1.21.1']){
 const r=path.join(root,loader,'src/main/resources');
 const file=path.join(r,'assets/laowu/models/block/creature_transmitter.json');
 assert.ok(fs.existsSync(file),'Missing creature transmitter model');
 const model=JSON.parse(fs.readFileSync(file));
 assert.deepEqual(model.elements[0].from,[0,0,0]);
 assert.deepEqual(model.elements[0].to,[16,16,16]);
 assert.deepEqual(model.elements[0].faces.up.uv,[8,16,0,8],'Preserve authored top UV');
 assert.deepEqual(model.elements[0].faces.down.uv,[16,0,8,8],'Preserve authored bottom UV');
 assert.equal(JSON.parse(fs.readFileSync(path.join(r,'assets/laowu/models/item/creature_transmitter.json'))).parent,'laowu:block/creature_transmitter');
 assert.ok(fs.readFileSync(path.join(r,'assets/laowu/textures/block/creature_transmitter.png')).equals(fs.readFileSync('D:/Project_minecraft/待实现/生物转信器/texture.png')),'Use exact supplied texture');
}
console.log('PASS: supplied model UV, geometry, item parent and exact texture in both loaders');
