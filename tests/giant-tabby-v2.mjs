import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';
const require=createRequire(import.meta.url);
const {PNG}=require('C:/Users/16611/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/pngjs');
const target=process.argv[2]||'art/giant-tabby-boss-v2/giant_tabby_boss.bbmodel';
const m=JSON.parse(fs.readFileSync(target,'utf8'));
// Density is measured on the mesh, not by the total atlas resolution.
const texture=PNG.sync.read(Buffer.from(m.textures[0].source.split(',')[1],'base64'));
assert.equal(m.textures.length,1,'Only the used material belongs in the deliverable');
const byName=new Map(m.elements.map(e=>[e.name,e]));
const body=byName.get('body_cube'),head=byName.get('head_main'),nose=byName.get('nose');
assert(body&&head&&nose,'Preserve the supplied vanilla body, head and projecting nose');
const size=e=>e.to.map((v,i)=>v-e.from[i]);
const dims=size(head),scale=dims[0]/5;
assert(Math.abs(dims[1]/scale-4)<.01&&Math.abs(dims[2]/scale-5)<.01,'Vanilla 5:4:5 head proportions must survive');
// Read world-space body bounds, including the supplied vanilla -90-degree rest bone.
const owner=new Map();
function index(nodes,parent){for(const n of nodes)if(typeof n==='string')owner.set(n,parent);else{owner.set(n.uuid,parent);index(n.children||[],n.uuid)}}
index(m.outliner,null);
const groups=new Map(m.groups.map(g=>[g.uuid,g]));
function world(p,id){while(id){const g=groups.get(id),r=(g.rotation||[0,0,0]).map(v=>v*Math.PI/180);let [x,y,z]=p.map((v,i)=>v-g.origin[i]);[y,z]=[y*Math.cos(r[0])-z*Math.sin(r[0]),y*Math.sin(r[0])+z*Math.cos(r[0])];[x,z]=[x*Math.cos(r[1])+z*Math.sin(r[1]),-x*Math.sin(r[1])+z*Math.cos(r[1])];[x,y]=[x*Math.cos(r[2])-y*Math.sin(r[2]),x*Math.sin(r[2])+y*Math.cos(r[2])];p=[x,y,z].map((v,i)=>v+g.origin[i]);id=owner.get(id)}return p}
const corners=Array.from({length:8},(_,j)=>world(body.from.map((v,i)=>j&(1<<i)?body.to[i]:v),owner.get(body.uuid)));
const b=[0,1,2].map(i=>Math.max(...corners.map(p=>p[i]))-Math.min(...corners.map(p=>p[i])));
assert(Math.abs(b[0]/scale-4)<.01&&Math.abs(b[1]/scale-6)<.01&&Math.abs(b[2]/scale-16)<.01,'Body must stay the slender vanilla 4:6:16 after baking its rotation');
assert(byName.has('front_belly'),'Requested chest/front-belly volume must exist');
assert(nose.from[2]<head.from[2],'Muzzle needs actual projecting geometry, not just a painted nose');
assert(m.elements.length<=17,'Do not turn the simple cat into a many-piece bulky sculpt');
const ids=new Set(m.groups.map(g=>g.uuid));
let uvChecks=0;
for(const e of m.elements){
  assert(e.from.every((v,i)=>Number.isFinite(v)&&Number.isFinite(e.to[i])&&v<e.to[i]));
  for(const [direction,f] of Object.entries(e.faces)){
    assert.equal(f.texture,0);
    assert(f.uv.every(Number.isFinite));
    const w=m.textures[0].uv_width,h=m.textures[0].uv_height;
    const [dx,dy,dz]=size(e);
    const [fw,fh]=['up','down'].includes(direction)?[dx,dz]:['east','west'].includes(direction)?[dz,dy]:[dx,dy];
    assert(Math.abs(Math.abs(f.uv[2]-f.uv[0])*texture.width/w-fw)<1e-5,`${e.name}/${direction}: must have exactly 16 horizontal texels per block`);
    assert(Math.abs(Math.abs(f.uv[3]-f.uv[1])*texture.height/h-fh)<1e-5,`${e.name}/${direction}: must have exactly 16 vertical texels per block`);
    assert(f.uv[0]>=0&&f.uv[2]>=0&&f.uv[0]<=w&&f.uv[2]<=w);
    assert(f.uv[1]>=0&&f.uv[3]>=0&&f.uv[1]<=h&&f.uv[3]<=h);
    // Sample the real assigned interior, not just a center that can hide clipped swatch edges.
    for(const u of [.08,.25,.5,.75,.92])for(const v of [.08,.25,.5,.75,.92]){
     const x=Math.min(texture.width-1,Math.floor((f.uv[0]+u*(f.uv[2]-f.uv[0]))/w*texture.width));
     const y=Math.min(texture.height-1,Math.floor((f.uv[1]+v*(f.uv[3]-f.uv[1]))/h*texture.height));
     assert(texture.data[(y*texture.width+x)*4+3]>230,`${e.name}: UV lands outside material at ${u},${v}`);
    }
    uvChecks++;
  }
}
for(const a of m.animations)for(const [id,bone]of Object.entries(a.animators)){
  assert(ids.has(id),'Animation targets an absent bone');
  for(const k of bone.keyframes||[])assert(k.time>=0&&k.time<=a.length&&Object.values(k.data_points[0]).every(n=>Number.isFinite(Number(n))));
}
const tones=new Set();
for(let i=0;i<texture.data.length;i+=4){
 assert([0,255].includes(texture.data[i+3]),'Hard pixel boundaries only: no feathered alpha');
 if(texture.data[i+3])tones.add(texture.data.slice(i,i+3).toString('hex'));
}
assert(tones.size<=180,'Use a small vanilla-like palette, not high-frequency fur or gradients');
const face=body.faces.east, x0=face.uv[0], y0=face.uv[1];
let refined=0;
for(let y=0;y<Math.abs(face.uv[3]-y0)-3;y+=4)for(let x=0;x<Math.abs(face.uv[2]-x0)-3;x+=4){
 const block=new Set();for(let j=0;j<4;j++)for(let i=0;i<4;i++){
  const k=((y0+y+j)*texture.width+x0+x+i)*4;block.add(texture.data.slice(k,k+3).toString('hex'));
 }
 if(block.size>1&&block.size<=4)refined++;
}
assert(refined>=8,'Subdivide some original large pixels into small discrete patches; do not just nearest-neighbour upscale');
console.log(`PASS: ${uvChecks} faces at exactly 16 texels/block, ${tones.size} palette colours, ${refined} subdivided patches, vanilla proportions and ${uvChecks*25} opaque UV samples`);
