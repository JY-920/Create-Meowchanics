import assert from 'node:assert/strict';
import {readFileSync,writeFileSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const version=process.argv[2]||'v11',out=path.join(root,'art/maodie-fighter-'+version);
const model=JSON.parse(readFileSync(path.join(out,'maodie_fighter_'+version+'.bbmodel'),'utf8'));
const body=model.elements.find(e=>e.name==='continuous_curved_torso');
const head=model.elements.find(e=>e.name==='cat_head'),shin=model.elements.find(e=>e.name==='left_shin');
const vertices=Object.values(body.vertices),top=Math.max(...vertices.map(v=>v[1]));
const front=Math.min(...vertices.filter(v=>v[1]===top).map(v=>v[2]));
const visibleLeg=Math.min(...vertices.map(v=>v[1]))-shin.from[1],errors=[];
if(visibleLeg<4||visibleLeg>5.5)errors.push('Visible lower legs must have 4–5.5 pixels below the torso, got '+visibleLeg);
if(head.from[1]<top-1)errors.push('Head remains too low inside the upper torso');
if(head.from[2]>front-.35)errors.push('Upper torso extends past the front of the head');
const target=(await(await fetch((process.env.MAODIE_BLOCKBENCH_URL||'http://127.0.0.1:9335')+'/json/list')).json()).find(t=>t.type==='page'&&t.url.includes('blockbench/resources/app.asar/index.html'));
assert(target,'Native isolated Blockbench instance required');
const socket=new WebSocket(target.webSocketDebuggerUrl);
await new Promise((resolve,reject)=>{socket.onopen=resolve;socket.onerror=reject});
try{
 const expression=`(()=>{newProject(Formats.free);Codecs.project.parse(${JSON.stringify(model)});Project.saved=true;Canvas.updateAll();Animation.all.forEach(a=>a.playing=false);Animation.all.find(a=>a.name==='animation.maodie.roundhouse').playing=true;Timeline.time=.20;Animator.preview();scene.updateMatrixWorld(true);const hip=Group.all.find(g=>g.name==='right_thigh').mesh.getWorldPosition(new THREE.Vector3()),ankle=Group.all.find(g=>g.name==='right_ankle').mesh.getWorldPosition(new THREE.Vector3()),v=ankle.sub(hip);Animation.all.forEach(a=>a.playing=false);Animator.showDefaultPose();return {horizontal_reach:Math.hypot(v.x,v.z),leg_reach:v.length(),vector:v.toArray()}})()`;
 const report=await new Promise((resolve,reject)=>{const timer=setTimeout(()=>reject(new Error('Native proportion check timed out')),60000);socket.onmessage=e=>{const r=JSON.parse(e.data);if(r.id===1){clearTimeout(timer);if(r.error||r.result.exceptionDetails)reject(new Error(JSON.stringify(r)));else resolve(r.result.result.value)}};socket.send(JSON.stringify({id:1,method:'Runtime.evaluate',params:{expression,returnByValue:true}}))});
 console.log('Proportion / native kick measures',{visibleLeg,headBottom:head.from[1],bodyTop:top,headFront:head.from[2],bodyFront:front,...report});
 writeFileSync(path.join(out,'proportion-validation.json'),JSON.stringify({visible_leg:visibleLeg,...report},null,2)+'\n');
 if(report.horizontal_reach<10)errors.push('Roundhouse still tucks/shortens the leg: horizontal reach '+report.horizontal_reach);
}finally{socket.close()}
assert.deepEqual(errors,[]);
console.log('PASS: visible leg length, raised forward head coverage, native fully extended roundhouse reach');
