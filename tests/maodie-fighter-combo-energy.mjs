import assert from 'node:assert/strict';
import {readFileSync,writeFileSync} from 'node:fs';
import path from 'node:path';
// Native world-space gaze must stay on the opponent while head position follows
// the torso. Also catches accidentally retaining the slower combo timing.
const version=process.argv[2]||'v11',out=path.resolve(import.meta.dirname,'../art/maodie-fighter-'+version);
const model=JSON.parse(readFileSync(path.join(out,'maodie_fighter_'+version+'.bbmodel')));
const target=(await(await fetch((process.env.MAODIE_BLOCKBENCH_URL||'http://127.0.0.1:9339')+'/json/list')).json()).find(t=>t.type==='page'&&t.url.includes('blockbench/resources/app.asar/index.html'));
const ws=new WebSocket(target.webSocketDebuggerUrl);await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j});
try{
 const expression=`(()=>{newProject(Formats.free);Codecs.project.parse(${JSON.stringify(model)});Project.saved=true;Canvas.updateAll();Animation.all.forEach(a=>a.playing=false);const clip=Animation.all.find(a=>a.name==='animation.maodie.combo');clip.playing=true;const g=n=>Group.all.find(g=>g.name===n).mesh,heads=[],rolls=[],punches=[];let previous={},maxGazeError=0;const n=Math.ceil(clip.length*480),dt=clip.length/n;
 for(let i=0;i<=n;i++){Timeline.time=i*dt;Animator.preview();scene.updateMatrixWorld(true);const head=g('head').getWorldPosition(new THREE.Vector3());heads.push(head.x);rolls.push(new THREE.Euler().setFromQuaternion(g('head').getWorldQuaternion(new THREE.Quaternion()),'ZYX').z*180/Math.PI);
 const target=g('root').getWorldPosition(new THREE.Vector3()).add(new THREE.Vector3(0,18.4,-28)),forward=new THREE.Vector3(0,0,-1).applyQuaternion(g('head').getWorldQuaternion(new THREE.Quaternion()));maxGazeError=Math.max(maxGazeError,forward.angleTo(target.sub(head))*180/Math.PI);
 for(const side of ['left','right']){const v=g(side+'_wrist').getWorldPosition(new THREE.Vector3());if(previous[side]){const speed=(previous[side].z-v.z)/dt;if(speed>0)punches.push(speed)}previous[side]=v}}
 Animation.all.forEach(a=>a.playing=false);Animator.showDefaultPose();punches.sort((a,b)=>b-a);return {length:clip.length,headTravel:Math.max(...heads)-Math.min(...heads),headRollSpan:Math.max(...rolls)-Math.min(...rolls),maxGazeError,forwardBurstSpeed:punches[Math.floor(punches.length*.02)]}})()`;
 const report=await new Promise((r,j)=>{const timer=setTimeout(()=>j(Error('Native motion sample timed out')),60000);ws.onmessage=e=>{const x=JSON.parse(e.data);if(x.id===1){clearTimeout(timer);x.error||x.result.exceptionDetails?j(Error(JSON.stringify(x))):r(x.result.result.value)}};ws.send(JSON.stringify({id:1,method:'Runtime.evaluate',params:{expression,returnByValue:true}}))});
 console.log(report);writeFileSync(path.join(out,'combo-energy-validation.json'),JSON.stringify(report,null,2));
 assert(report.headTravel>5,'Head remains stabilised instead of visibly swinging with the strikes');
 assert(report.maxGazeError<2,'The face turns away from the opponent during punching');
 assert(report.headRollSpan<5,'Head rolls sideways instead of keeping the opponent in view');
 assert(report.forwardBurstSpeed>600,'Fists retain the previous slower burst timing');
 assert(report.length<1,'Six-punch sequence still uses the slower demonstration timing');
 console.log('PASS: target-locked gaze, moving head position and accelerated combo');
}finally{ws.close()}
