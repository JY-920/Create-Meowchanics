import assert from 'node:assert/strict';
import {readFileSync,writeFileSync} from 'node:fs';
import path from 'node:path';
const root=path.resolve(import.meta.dirname,'..'),out=path.join(root,'art/maodie-fighter-v11');
const model=JSON.parse(readFileSync(path.join(out,'maodie_fighter_v11.bbmodel')));
const endpoint=process.env.MAODIE_BLOCKBENCH_URL||'http://127.0.0.1:9338';
const target=(await(await fetch(endpoint+'/json/list')).json()).find(t=>t.type==='page'&&t.url.includes('blockbench/resources/app.asar/index.html'));
const socket=new WebSocket(target.webSocketDebuggerUrl);await new Promise((r,j)=>{socket.onopen=r;socket.onerror=j});
try{
 const setup=process.argv.includes('--reuse')?`if(Project.name!==${JSON.stringify(model.name)})throw Error('Wrong loaded preview');`:`newProject(Formats.free);Codecs.project.parse(${JSON.stringify(model)});Project.saved=true;Canvas.updateAll();`;
 const expression=`(()=>{${setup}Animation.all.forEach(a=>a.playing=false);Animation.all.find(a=>a.name==='animation.maodie.combo').playing=true;
 const g=n=>Group.all.find(g=>g.name===n).mesh,at=t=>{Timeline.time=t;Animator.preview();scene.updateMatrixWorld(true)},wrist=s=>g(s+'_wrist').getWorldPosition(new THREE.Vector3()).sub(g('root').getWorldPosition(new THREE.Vector3()));
 const hits=[];for(const [time,side,want] of [[.0864,'left',[1.4,15.8,-11.2]],[.2064,'right',[-.5,15.8,-11.5]],[.3456,'left',[-.8,15.3,-10.1]],[.4752,'right',[.8,11.8,-9.9]],[.6096,'left',[.2,16.4,-10.1]],[.744,'right',[-.6,15.8,-11.1]]]){at(time);const actual=wrist(side);hits.push({time,side,actual:actual.toArray(),error:actual.distanceTo(new THREE.Vector3(...want)),guard:wrist(side==='left'?'right':'left').toArray()})}
 let highestHand=-Infinity,lowestHand=Infinity,rearmostHand=-Infinity,maxFrameStep=0,worstStep;const previous={};for(let i=0;i<=960;i++){at(.8736*i/960);for(const s of ['left','right']){const v=wrist(s);highestHand=Math.max(highestHand,v.y);lowestHand=Math.min(lowestHand,v.y);rearmostHand=Math.max(rearmostHand,v.z);if(previous[s]&&v.distanceTo(previous[s])>maxFrameStep){maxFrameStep=v.distanceTo(previous[s]);worstStep={time:Timeline.time,side:s,from:previous[s].toArray(),to:v.toArray()}}previous[s]=v}}
 Animation.all.forEach(a=>a.playing=false);Animator.showDefaultPose();return {hits,highestHand,lowestHand,rearmostHand,maxFrameStep,worstStep}})()`;
 const report=await new Promise((resolve,reject)=>{const timer=setTimeout(()=>reject(Error('Native pose sampling timed out')),60000);socket.onmessage=e=>{const r=JSON.parse(e.data);if(r.id===1){clearTimeout(timer);r.error||r.result.exceptionDetails?reject(Error(JSON.stringify(r))):resolve(r.result.result.value)}};socket.send(JSON.stringify({id:1,method:'Runtime.evaluate',params:{expression,returnByValue:true}}))});
 console.log(JSON.stringify(report,null,2));writeFileSync(path.join(out,'combo-target-validation.json'),JSON.stringify(report,null,2));
 assert(report.hits.every(p=>p.error<.2),'A strike misses its authored front target');
 assert(report.hits.every(p=>p.guard[1]>13.5&&p.guard[2]<-3),'Off hand drops its guard at impact');
 assert(report.highestHand<18.5&&report.lowestHand>10,'Hands swing over the head or below the intended body shot');
 assert(report.rearmostHand<2.5,'Hand swings behind the fighter');
 // Native interpolate() snaps to keys within 1/1200 s. At the accelerated
 // speed this creates ~2px steps even at finer sampling; reject full-arm jumps.
 assert(report.maxFrameStep<3,'A hand makes a large discontinuous jump between sub-frame samples');
 console.log('PASS: six aimed strikes, off-hand guard, controlled arcs and continuous native motion');
}finally{socket.close()}
