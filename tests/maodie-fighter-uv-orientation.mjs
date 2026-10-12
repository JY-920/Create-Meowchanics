import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const m=JSON.parse(readFileSync(new URL('../art/maodie-fighter-v11/maodie_fighter_v11.bbmodel',import.meta.url),'utf8'));
const body=m.elements.find(e=>e.name==='continuous_curved_torso');
// Physical corner expectations from native Blockbench Cube geometry/updateUV
// using the unchanged vanilla body atlas. Area/opacity cannot detect mirroring.
const cases=[
 ['side_0_0','r0_1',[26,22]], ['side_0_0','r0_0',[30,22]],
 ['side_11_0','r12_0',[30,6]], ['side_11_0','r12_1',[26,6]],
 ['side_11_1','r12_1',[26,6]], ['side_11_1','r12_2',[20,6]],
 ['side_11_2','r12_2',[40,6]], ['side_11_2','r12_3',[36,6]],
 ['side_11_3','r12_3',[36,6]], ['side_11_3','r12_0',[30,6]],
 ['cap_12','r12_0',[30,6]], ['cap_12','r12_1',[26,6]],
 ['cap_12','r12_2',[26,0]], ['cap_12','r12_3',[30,0]],
 ['cap_0','r0_0',[34,6]], ['cap_0','r0_1',[30,6]],
 ['cap_0','r0_2',[30,0]], ['cap_0','r0_3',[34,0]],
];
for(const [face,corner,expected] of cases)assert.deepEqual(body.faces[face].uv[corner],expected,`${face}/${corner}: original body UV must not mirror`);
console.log('PASS: all four sides and both end caps retain original native cube UV orientation');
