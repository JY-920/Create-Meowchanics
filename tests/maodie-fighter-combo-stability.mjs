import assert from 'node:assert/strict';
import {readFileSync,writeFileSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const version=process.argv[2]||'v11',out=path.join(root,'art/maodie-fighter-'+version);
const model=JSON.parse(readFileSync(path.join(out,'maodie_fighter_'+version+'.bbmodel'),'utf8'));
const target=(await(await fetch((process.env.MAODIE_BLOCKBENCH_URL||'http://127.0.0.1:9337')+'/json/list')).json()).find(t=>t.type==='page'&&t.url.includes('blockbench/resources/app.asar/index.html'));
assert(target,'Native isolated Blockbench instance required');
const socket=new WebSocket(target.webSocketDebuggerUrl);
await new Promise((r,j)=>{socket.onopen=r;socket.onerror=j});
try{
 const expression=`(()=>{
  newProject(Formats.free);Codecs.project.parse(${JSON.stringify(model)});Project.saved=true;Canvas.updateAll();
  Animation.all.forEach(a=>a.playing=false);const clip=Animation.all.find(a=>a.name==='animation.maodie.combo');clip.playing=true;
  const g=name=>Group.all.find(g=>g.name===name).mesh;
  let base,hipBase,kneeBase,maxHipMovement=0,maxKneeMovement=0,maxAnkleSlide=0,maxSoleHeight=0,maxRootRoll=0;const hipYaws=[];
  for(let i=0;i<=288;i++){
   Timeline.time=clip.length*i/288;Animator.preview();scene.updateMatrixWorld(true);
   const ankles=['left','right'].map(s=>g(s+'_ankle').getWorldPosition(new THREE.Vector3()));base||=ankles;
   ankles.forEach((v,j)=>maxAnkleSlide=Math.max(maxAnkleSlide,Math.hypot(v.x-base[j].x,v.z-base[j].z)));
   const hip=g('hips').getWorldPosition(new THREE.Vector3()),knee=g('left_knee').getWorldPosition(new THREE.Vector3());hipBase||=hip;kneeBase||=knee;
   maxHipMovement=Math.max(maxHipMovement,hip.distanceTo(hipBase));maxKneeMovement=Math.max(maxKneeMovement,knee.distanceTo(kneeBase));
   hipYaws.push(new THREE.Euler().setFromQuaternion(g('hips').getWorldQuaternion(new THREE.Quaternion()),'ZYX').y*180/Math.PI);
   maxRootRoll=Math.max(maxRootRoll,Math.abs(new THREE.Euler().setFromQuaternion(g('root').getWorldQuaternion(new THREE.Quaternion()),'ZYX').z));
   for(const c of Cube.all.filter(c=>c.name.endsWith('_shin'))){let low=Infinity;const p=c.mesh.geometry.attributes.position;for(let j=0;j<p.count;j++)low=Math.min(low,new THREE.Vector3().fromBufferAttribute(p,j).applyMatrix4(c.mesh.matrixWorld).y);maxSoleHeight=Math.max(maxSoleHeight,low)}
  }
  Animation.all.forEach(a=>a.playing=false);Animator.showDefaultPose();return {samples:289,maxHipMovement,maxKneeMovement,maxAnkleSlide,maxSoleHeight,maxRootRoll,hipYawSpan:Math.max(...hipYaws)-Math.min(...hipYaws)};
 })()`;
 const report=await new Promise((resolve,reject)=>{const timer=setTimeout(()=>reject(new Error('Native combo support check timed out')),60000);socket.onmessage=e=>{const r=JSON.parse(e.data);if(r.id===1){clearTimeout(timer);if(r.error||r.result.exceptionDetails)reject(new Error(JSON.stringify(r)));else resolve(r.result.result.value)}};socket.send(JSON.stringify({id:1,method:'Runtime.evaluate',params:{expression,returnByValue:true,awaitPromise:true}}))});
 console.log('Native combo support',report);
 writeFileSync(path.join(out,'combo-stability-validation.json'),JSON.stringify(report,null,2)+'\n');
 assert(report.maxHipMovement>.15&&report.maxHipMovement<1.5,'Pelvis must shift weight without whole-body travel');
 assert(report.maxKneeMovement>.5&&report.maxKneeMovement<3,'Knees must follow weight transfer, not freeze or swing wildly');
 assert(report.hipYawSpan>15,'Pelvis does not participate in punch rotation');
 assert(report.maxAnkleSlide<.3,'Support feet slide instead of staying planted');
 assert(report.maxSoleHeight<.25,'Support feet visibly float above the floor');
 assert(report.maxRootRoll<.001,'Whole actor rolls instead of articulating hips and chest');
 console.log('PASS: natural hip/knee support with planted soles and no global roll');
}finally{socket.close()}
