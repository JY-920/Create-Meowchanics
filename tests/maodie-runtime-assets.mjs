import assert from 'node:assert/strict';
import {readFileSync,existsSync} from 'node:fs';
const source=JSON.parse(readFileSync('art/maodie-fighter-v11/maodie_fighter_v11.bbmodel'));
let previous;
for(const port of ['forge-1.20.1','neoforge-1.21.1']){
 const root=port+'/src/main/resources/assets/laowu/';
 assert(existsSync(root+'models/entity/maodie_fighter.bbmodel'),'Approved boxer model missing from runtime');
 const model=JSON.parse(readFileSync(root+'models/entity/maodie_fighter.bbmodel'));
 assert.deepEqual(model.elements,source.elements,'Geometry or UVs differ from approved preview');
 assert.deepEqual(model.groups,source.groups);assert.deepEqual(model.outliner,source.outliner);
 const raw=readFileSync(root+'cat_animation_clips/maodie_fighter.json','utf8'),data=JSON.parse(raw);
 assert.equal(Object.keys(data.clips).length,8);assert(!data.clips.combat_flow,'Demo is not a gameplay move');
 for(const clip of source.animations.filter(a=>!a.name.endsWith('.combat_flow'))){
  const exported=data.clips[clip.name.replace('animation.maodie.','')];assert.equal(exported.length,clip.length);
  for(const track of Object.values(clip.animators))for(const channel of ['position','rotation','scale']){
   const wanted=track.keyframes.filter(k=>k.channel===channel).map(k=>[k.time,...['x','y','z'].map(n=>+k.data_points[0][n])]);
   assert.deepEqual(exported.bones[track.name][channel],wanted,'Baked tracks must not be retimed or angle-wrapped');
  }
 }
 if(previous)assert.equal(raw,previous);previous=raw;
 assert(readFileSync(root+'textures/entity/maodie_fighter.png').equals(Buffer.from(source.textures[0].source.split(',')[1],'base64')));
}
console.log('PASS: exact v11 geometry, UVs, atlas and eight linear runtime clips, identical on both loaders');
