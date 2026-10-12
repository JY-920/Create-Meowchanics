import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
const require=createRequire(import.meta.url);
const {PNG}=require('C:/Users/16611/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/pngjs');
const file=new URL('../art/maodie-fighter-v11/maodie_fighter_v11.bbmodel',import.meta.url);
assert(existsSync(file),'Missing editable Blockbench boxer with seven authored combat clips');
const m=JSON.parse(readFileSync(file,'utf8'));
const png=PNG.sync.read(Buffer.from(m.textures[0].source.split(',')[1],'base64'));
const faces=['north','south','east','west','up','down'];
// Catches missing caps, out-of-atlas UVs and sampling transparent cat-atlas gaps.
let checked=0;
for(const e of m.elements){
 if(e.type==='mesh'){
  const edges=new Map();
  for(const f of Object.values(e.faces)){
   assert(f.texture===0&&f.vertices.length>=3,'Untextured body polygon');
   for(let i=0;i<f.vertices.length;i++){const a=f.vertices[i],b=f.vertices[(i+1)%f.vertices.length],key=[a,b].sort().join('/');edges.set(key,(edges.get(key)||0)+1);assert(e.vertices[a]&&f.uv[a].every(Number.isFinite),'Invalid connected mesh vertex');}
   const points=Object.values(f.uv),x0=Math.min(...points.map(p=>p[0])),x1=Math.max(...points.map(p=>p[0])),y0=Math.min(...points.map(p=>p[1])),y1=Math.max(...points.map(p=>p[1]));
   for(let y=Math.floor(y0);y<Math.ceil(y1);y++)for(let x=Math.floor(x0);x<Math.ceil(x1);x++){assert.equal(png.data[(y*png.width+x)*4+3],255,'Transparent curved body polygon');checked++}
  }
  assert([...edges.values()].every(n=>n===2),'Body mesh has open cracks or non-manifold seams');
  continue;
 }
 for(const direction of faces){
  const f=e.faces[direction];
  assert(f&&f.texture===0,`${e.name}/${direction}: missing textured closed face`);
  const [u,v,a,b]=f.uv;
  const x0=Math.min(u,a),x1=Math.max(u,a),y0=Math.min(v,b),y1=Math.max(v,b);
  assert(x1>x0&&y1>y0&&x0>=0&&y0>=0&&x1<=png.width&&y1<=png.height,'UV outside original atlas');
  for(let y=Math.floor(y0);y<Math.ceil(y1);y++)for(let x=Math.floor(x0);x<Math.ceil(x1);x++){
   assert.equal(png.data[(y*png.width+x)*4+3],255,`${e.name}/${direction}: transparent UV at ${x},${y}`);
   checked++;
  }
 }
}
const ids=new Set(m.groups.map(g=>g.uuid));
const channels=new Set(['rotation','position','scale']);
const expected=['uppercut','jab','combo','block','backstep','roundhouse','jump_slam'];
for(const name of expected){
 const a=m.animations.find(x=>x.name==='animation.maodie.'+name);
 assert(a&&a.length>0&&a.length<=(name==='combo'?2:.8),'Missing responsive authored clip: '+name);
 let animated=0;
 for(const [id,track] of Object.entries(a.animators)){
  assert(ids.has(id),'Animation points to missing bone');
  let end=false;
  for(const k of track.keyframes){
   assert(channels.has(k.channel)&&k.time>=0&&k.time<=a.length+.00001,'Invalid timeline');
   assert(k.data_points.every(p=>['x','y','z'].every(axis=>Number.isFinite(Number(p[axis])))),'Non-numeric preview keyframe');
   if(k.time>0)animated++;
   if(Math.abs(k.time-a.length)<.00001)end=true;
  }
  assert(end,'Clip must finish with an explicit recovery pose');
 }
 assert(animated>15,'Clip lacks articulated body/limb animation');
}
const spine=m.elements.find(e=>e.name==='continuous_curved_torso');
assert(spine&&spine.type==='mesh','Waist still made of disconnected cuboids instead of a continuous curved surface');
const points=Object.values(spine.vertices),bodyHeight=Math.max(...points.map(v=>v[1]))-Math.min(...points.map(v=>v[1]));
assert(bodyHeight<=13&&bodyHeight>=10,'Compact torso proportion lost');
assert(!m.elements.some(e=>e.name.endsWith('hind_paw')),'Independent feet must be removed');
assert(m.groups.find(g=>g.name==='left_arm').origin[1]<m.groups.find(g=>g.name==='head').origin[1]-2,'Shoulder still attached beside head');
assert(m.groups.some(g=>g.name==='left_elbow')&&m.groups.some(g=>g.name==='right_knee'),'Missing articulated limbs');
const jab=m.animations.find(a=>a.name==='animation.maodie.jab'),root=m.groups.find(g=>g.name==='root');
assert(Math.min(...jab.animators[root.uuid].keyframes.filter(k=>k.channel==='position').map(k=>Number(k.data_points[0].z)))<=-4,'Punch lacks whole-body lunge');
assert(Object.values(jab.animators).some(a=>a.keyframes.some(k=>k.channel==='scale'&&Number(k.data_points[0].y)>1.2)),'Punch lacks exaggerated extension');
console.log('PASS: seven explosive clips, continuous compact torso, no independent feet, '+checked+' opaque UV texels');
