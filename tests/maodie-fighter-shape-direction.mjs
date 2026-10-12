import assert from 'node:assert/strict';
import {readFileSync,writeFileSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const version=process.argv[2]||'v11',out=path.join(root,'art/maodie-fighter-'+version);
const model=JSON.parse(readFileSync(path.join(out,'maodie_fighter_'+version+'.bbmodel'),'utf8'));
const errors=[],torso=model.elements.find(e=>e.name==='continuous_curved_torso');
const rings=new Map();
for(const v of Object.values(torso.vertices)){const row=rings.get(v[1])||[];row.push(v);rings.set(v[1],row)}
for(const row of rings.values()){
 if(row.length!==4){errors.push('Body cross-section is bevelled/rounded instead of a rectangular four-corner section');break}
 const xs=new Set(row.map(p=>p[0])),zs=new Set(row.map(p=>p[2]));
 if(xs.size!==2||zs.size!==2)errors.push('Body side is not rectangular');
}
const strips=Object.entries(torso.faces).filter(([name])=>name.startsWith('side_')).map(([,face])=>face);
// The original body atlas has four sides with total UV area 20*16=320.
let area=0;
for(const f of strips){const u=Object.values(f.uv);area+=(Math.max(...u.map(p=>p[0]))-Math.min(...u.map(p=>p[0])))*(Math.max(...u.map(p=>p[1]))-Math.min(...u.map(p=>p[1])))}
if(Math.abs(area-320)>1e-6)errors.push('Body UV strips are duplicated: '+area+' instead of 320 texels');
const target=(await(await fetch((process.env.MAODIE_BLOCKBENCH_URL||'http://127.0.0.1:9336')+'/json/list')).json()).find(t=>t.type==='page'&&t.url.includes('blockbench/resources/app.asar/index.html'));
assert(target,'Native isolated Blockbench instance required');
const socket=new WebSocket(target.webSocketDebuggerUrl);
await new Promise((r,j)=>{socket.onopen=r;socket.onerror=j});
try{
 const expression=`(()=>{newProject(Formats.free);Codecs.project.parse(${JSON.stringify(model)});Project.saved=true;Canvas.updateAll();const report=[];for(const [clip,times] of [['jab',[.075]]]){Animation.all.forEach(a=>a.playing=false);Animation.all.find(a=>a.name==='animation.maodie.'+clip).playing=true;for(let n=0;n<times.length;n++){Timeline.time=times[n];Animator.preview();scene.updateMatrixWorld(true);const side=clip==='jab'||n%2===0?'left':'right',group=name=>Group.all.find(g=>g.name===name).mesh;const arm=group(side+'_arm').getWorldPosition(new THREE.Vector3()),fist=group(side+'_wrist').getWorldPosition(new THREE.Vector3()),v=fist.sub(arm);v.applyQuaternion(group('root').getWorldQuaternion(new THREE.Quaternion()).invert());report.push({clip,time:times[n],side,forward:-v.z,lateral:Math.abs(v.x),angle:Math.atan2(Math.abs(v.x),-v.z)*180/Math.PI})}}Animation.all.forEach(a=>a.playing=false);Animator.showDefaultPose();return report})()`;
 const report=await new Promise((resolve,reject)=>{const timer=setTimeout(()=>reject(new Error('Blockbench direction check timed out')),60000);socket.onmessage=e=>{const r=JSON.parse(e.data);if(r.id===1){clearTimeout(timer);if(r.error||r.result.exceptionDetails)reject(new Error(JSON.stringify(r)));else resolve(r.result.result.value)}};socket.send(JSON.stringify({id:1,method:'Runtime.evaluate',params:{expression,returnByValue:true,awaitPromise:true}}))});
 console.log('Native forward punch vectors',report);
 writeFileSync(path.join(out,'punch-direction-validation.json'),JSON.stringify(report,null,2)+'\n');
 for(const r of report)if(r.forward<6||r.angle>7)errors.push(`${r.clip}/${r.side}@${r.time}: punch veers sideways (${r.angle.toFixed(2)} degrees)`);
}finally{socket.close()}
assert.deepEqual(errors,[]);
console.log('PASS: simple rectangular bent body, original UV strips once, native straight jab forward (combo intentionally multidirectional)');
