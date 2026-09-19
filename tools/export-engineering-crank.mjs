import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import {createHash} from 'node:crypto';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const source='docs/blockbench/cat_engineering_crank.bbmodel';
const bytes=readFileSync(path.join(root,source)), model=JSON.parse(bytes);
const bones=['head','body','left_hind_leg','right_hind_leg','left_front_leg','right_front_leg','tail1','tail2'];
const animation=model.animations.find(a=>a.name==='animation.cat.engineering_crank');
assert(animation && animation.loop==='loop' && animation.length===3);
const times=[];
const channels=bones.map(name=>{
  const g=model.groups.find(g=>g.name===name);
  assert(g && g.rotation.every(v=>v===0));
  const tracks={};
  for(const ch of ['position','rotation','scale']){
    const keys=animation.animators[g.uuid].keyframes.filter(k=>k.channel===ch).sort((a,b)=>a.time-b.time);
    assert.equal(keys.length,181);
    tracks[ch]=keys.map((k,i)=>{
      assert.equal(k.interpolation,'linear');
      assert(Math.abs(k.time-i/60)<0.00001);
      if(times[i]===undefined)times[i]=k.time;else assert.equal(times[i],k.time);
      return ['x','y','z'].map(axis=>{const v=Number(k.data_points[0][axis]);assert(Number.isFinite(v));return v;});
    });
  }
  return {g,tracks};
});
const frames=times.map((_,i)=>channels.flatMap(({g,tracks:t})=>[
  ...t.position[i].map((v,j)=>v+g.origin[j]),...t.rotation[i].map(v=>v*Math.PI/180),...t.scale[i]
].map(v=>Number(v.toFixed(7)))));
const clip={format_version:1,source,source_sha256:createHash('sha256').update(bytes).digest('hex'),
  duration_seconds:3,sample_rate:60,bone_order:bones,reference_seat_y:1.784,
  reference_grip_left:[-4,17.784,7],reference_grip_right:[-1,17.784,7],frames};
for(const loader of ['forge-1.20.1','neoforge-1.21.1']){
  const file=path.join(root,loader,'src/main/resources/assets/laowu/cat_animation_clips/cat_engineering_crank.json');
  const content=JSON.stringify(clip)+'\n';
  if(process.argv.includes('--write')){mkdirSync(path.dirname(file),{recursive:true});writeFileSync(file,content);}
  else assert.equal(readFileSync(file,'utf8'),content,loader+' stale animation');
}
console.log('PASS: MCP-authored sideways two-paw crank clip, 181 frames, 8 bones, both loaders');
