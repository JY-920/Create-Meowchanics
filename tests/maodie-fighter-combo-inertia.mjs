import assert from 'node:assert/strict';
import {readFileSync,writeFileSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const version=process.argv[2]||'v8',out=path.join(root,'art/maodie-fighter-'+version);
const model=JSON.parse(readFileSync(path.join(out,'maodie_fighter_'+version+'.bbmodel'),'utf8'));
const target=(await(await fetch((process.env.MAODIE_BLOCKBENCH_URL||'http://127.0.0.1:9337')+'/json/list')).json()).find(t=>t.type==='page'&&t.url.includes('blockbench/resources/app.asar/index.html'));
assert(target,'Native isolated Blockbench instance required');
const socket=new WebSocket(target.webSocketDebuggerUrl);
await new Promise((r,j)=>{socket.onopen=r;socket.onerror=j});
try{
 const expression=`(()=>{newProject(Formats.free);Codecs.project.parse(${JSON.stringify(model)});Project.saved=true;Canvas.updateAll();Animation.all.forEach(a=>a.playing=false);Animation.all.find(a=>a.name==='animation.maodie.combo').playing=true;const g=name=>Group.all.find(g=>g.name===name).mesh,sample=(side,t)=>{Timeline.time=t;Animator.preview();scene.updateMatrixWorld(true);const fist=g(side+'_wrist').getWorldPosition(new THREE.Vector3()),v=fist.clone().sub(g(side+'_arm').getWorldPosition(new THREE.Vector3())).applyQuaternion(g('upper_spine').getWorldQuaternion(new THREE.Quaternion()).invert());return {fist:fist.toArray(),rear:v.z,forward:-v.z,yaw:new THREE.Euler().setFromQuaternion(g('lower_spine').getWorldQuaternion(new THREE.Quaternion()),'ZYX').y*180/Math.PI}};const punches=[];for(let n=0;n<8;n++){const side=n%2?'right':'left',hit=.12+n*.135,wind=sample(side,hit-.045),impact=sample(side,hit),follow=sample(side,hit+.03);punches.push({side,windRear:wind.rear,impactForward:impact.forward,followDistance:new THREE.Vector3(...follow.fist).distanceTo(new THREE.Vector3(...impact.fist)),continuedTorsoTurn:(follow.yaw-impact.yaw)*(n%2?-1:1)})}Animation.all.forEach(a=>a.playing=false);Animator.showDefaultPose();return punches})()`;
 const report=await new Promise((resolve,reject)=>{const timer=setTimeout(()=>reject(new Error('Native combo inertia check timed out')),60000);socket.onmessage=e=>{const r=JSON.parse(e.data);if(r.id===1){clearTimeout(timer);if(r.error||r.result.exceptionDetails)reject(new Error(JSON.stringify(r)));else resolve(r.result.result.value)}};socket.send(JSON.stringify({id:1,method:'Runtime.evaluate',params:{expression,returnByValue:true,awaitPromise:true}}))});
 console.log('Native combo inertia',report);
 writeFileSync(path.join(out,'combo-inertia-validation.json'),JSON.stringify(report,null,2)+'\n');
 assert(report.every(p=>p.windRear>.5),'Punches lack a clearly pulled-back fist before launch');
 assert(report.every(p=>p.impactForward>4),'Loaded fists do not swing forward');
 assert(report.every(p=>p.followDistance>2),'Fists stop at impact instead of swinging through');
 assert(report.every(p=>p.continuedTorsoTurn>4),'Torso immediately reverses instead of following the punch');
 console.log('PASS: eight rear-loaded punches continue through impact with torso inertia');
}finally{socket.close()}
