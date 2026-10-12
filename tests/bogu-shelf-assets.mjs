import assert from 'node:assert/strict';
import {readFileSync, existsSync} from 'node:fs';
import path from 'node:path';
const root=path.resolve(import.meta.dirname,'..');
for(const port of ['forge-1.20.1','neoforge-1.21.1']) {
 const base=path.join(root,port,'src/main');
 const keys=readFileSync(path.join(base,'java/cn/laowu/mod/client/ClientModEvents.java'),'utf8');
 assert.ok(/"key.laowu.held_item_transform",\s*InputConstants.Type.KEYSYM,\s*InputConstants.UNKNOWN.getValue\(\)/.test(keys),'display adjuster defaults to unbound');
 assert.ok(!keys.includes('PILOT_DESCEND'),'descent is not a configurable mapping');
 const flight=readFileSync(path.join(base,'java/cn/laowu/mod/client/CatPilotFlightClient.java'),'utf8');
 const shelf=readFileSync(path.join(base,'java/cn/laowu/mod/block/BoguShelfBlock.java'),'utf8');
 assert.match(shelf,/mayInteract\(c.getPlayer\(\),at\)/,'all-cell placement must check server protection, not only adventure permissions');
 assert.match(flight,/KEY_LCONTROL/);assert.match(flight,/KEY_RCONTROL/);assert.match(flight,/isWindowActive/);
 for(const suffix of ['blockstates/round_bogu_shelf.json','models/item/round_bogu_shelf.json'])
  assert.ok(existsSync(path.join(base,'resources/assets/laowu',suffix)),suffix+' exists');
}
assert.ok(existsSync(path.join(root,'tools/export-bogu-shelf.mjs')),'exact shelf exporter exists');
const {exportShelf}=await import('../tools/export-bogu-shelf.mjs');
const model=JSON.parse(readFileSync(path.join(root,'assets-source/bogu-shelf/model.bbmodel')));
const result=exportShelf(model);
assert.ok(result.full.includes('mtllib round_bogu_shelf.mtl')&&!result.full.includes('mtllib cat_editor.mtl'),'shelf item uses its own material library, not a Create machine asset');
assert.equal(result.parts.length,9);
assert.ok(result.faces>1000);
assert.ok(Math.abs(result.sourceArea-result.partArea)<1e-7,'splitting conserves surface area');
for(const part of result.parts) for(const line of part.obj.split('\n').filter(l=>l.startsWith('v '))) {
 const v=line.slice(2).split(' ').map(Number);assert.ok(v.every(x=>x>=-1e-8&&x<=1+1e-8),'local part geometry stays in its occupied cell');
}
assert.equal(model.textures.length,1);
console.log('PASS: shelf geometry / key contract');
