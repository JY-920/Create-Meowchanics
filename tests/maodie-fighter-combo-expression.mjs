import assert from 'node:assert/strict';
import {readFileSync,writeFileSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const version=process.argv[2]||'v8',out=path.join(root,'art/maodie-fighter-'+version);
const model=JSON.parse(readFileSync(path.join(out,'maodie_fighter_'+version+'.bbmodel'),'utf8'));
const target=(await(await fetch((process.env.MAODIE_BLOCKBENCH_URL||'http://127.0.0.1:9336')+'/json/list')).json()).find(t=>t.type==='page'&&t.url.includes('blockbench/resources/app.asar/index.html'));
assert(target,'Native isolated Blockbench instance required');
const socket=new WebSocket(target.webSocketDebuggerUrl);
await new Promise((r,j)=>{socket.onopen=r;socket.onerror=j});
try{
 const expression=`(()=>{newProject(Formats.free);Codecs.project.parse(${JSON.stringify(model)});Project.saved=true;Canvas.updateAll();Animation.all.forEach(a=>a.playing=false);Animation.all.find(a=>a.name==='animation.maodie.combo').playing=true;const group=name=>Group.all.find(g=>g.name===name).mesh;let headBase,chestBase,maxHeadMovement=0,maxChestMovement=0;for(let i=0;i<=173;i++){Timeline.time=1.2*i/173;Animator.preview();scene.updateMatrixWorld(true);const head=group('head').getWorldPosition(new THREE.Vector3()),chest=group('upper_spine').getWorldPosition(new THREE.Vector3());headBase||=head;chestBase||=chest;maxHeadMovement=Math.max(maxHeadMovement,head.distanceTo(headBase));maxChestMovement=Math.max(maxChestMovement,chest.distanceTo(chestBase))}const punches=[];for(let n=0;n<8;n++){Timeline.time=.12+n*.135;Animator.preview();scene.updateMatrixWorld(true);const side=n%2?'right':'left',v=group(side+'_wrist').getWorldPosition(new THREE.Vector3()).sub(group(side+'_arm').getWorldPosition(new THREE.Vector3()));punches.push({side,length:v.length(),forward:-v.z,yaw:Math.atan2(v.x,-v.z)*180/Math.PI,elevation:Math.atan2(v.y,Math.hypot(v.x,v.z))*180/Math.PI})}Animation.all.forEach(a=>a.playing=false);Animator.showDefaultPose();return {maxHeadMovement,maxChestMovement,punches}})()`;
 const report=await new Promise((resolve,reject)=>{const timer=setTimeout(()=>reject(new Error('Native combo expression check timed out')),60000);socket.onmessage=e=>{const r=JSON.parse(e.data);if(r.id===1){clearTimeout(timer);if(r.error||r.result.exceptionDetails)reject(new Error(JSON.stringify(r)));else resolve(r.result.result.value)}};socket.send(JSON.stringify({id:1,method:'Runtime.evaluate',params:{expression,returnByValue:true,awaitPromise:true}}))});
 console.log('Native combo expression',report);
 writeFileSync(path.join(out,'combo-expression-validation.json'),JSON.stringify(report,null,2)+'\n');
 assert(report.maxHeadMovement>3,'Combo lacks substantial head movement');
 assert(report.maxChestMovement>2,'Combo lacks upper body follow-through');
 assert(report.punches.every(p=>p.length>8&&p.forward>1),'Punches collapse or aim behind the fighter');
 const span=key=>Math.max(...report.punches.map(p=>p[key]))-Math.min(...report.punches.map(p=>p[key]));
 assert(span('yaw')>60,'All combo punches use the same horizontal direction');
 assert(span('elevation')>35,'Combo lacks high and low strikes');
 console.log('PASS: exaggerated head/upper-body follow-through and eight alternating multidirectional punches');
}finally{socket.close()}
