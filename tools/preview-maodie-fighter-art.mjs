import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const out=path.join(root,'art/maodie-fighter-v11');
const model=JSON.parse(readFileSync(path.join(out,'maodie_fighter_v11.bbmodel'),'utf8'));
const clips=JSON.parse(readFileSync(path.join(out,'clips.json'),'utf8'));
const endpoint=process.env.MAODIE_BLOCKBENCH_URL||'http://127.0.0.1:9335';
const target=(await(await fetch(endpoint+'/json/list')).json()).find(t=>t.type==='page'&&t.url.includes('blockbench/resources/app.asar/index.html'));
assert(target,'Start the isolated Blockbench preview instance at '+endpoint);
const socket=new WebSocket(target.webSocketDebuggerUrl),pending=new Map();let sequence=0;
await new Promise((resolve,reject)=>{socket.onopen=resolve;socket.onerror=reject});
socket.onmessage=e=>{const r=JSON.parse(e.data),p=pending.get(r.id);if(p){clearTimeout(p.timer);pending.delete(r.id);r.error?p.reject(new Error(JSON.stringify(r.error))):p.resolve(r.result)}};
async function run(expression){
 const id=++sequence;
 const r=await new Promise((resolve,reject)=>{const timer=setTimeout(()=>{pending.delete(id);reject(new Error('Native Blockbench evaluation timed out'))},60000);pending.set(id,{resolve,reject,timer});socket.send(JSON.stringify({id,method:'Runtime.evaluate',params:{expression,returnByValue:true,awaitPromise:true}}))});
 if(r.exceptionDetails)throw new Error(JSON.stringify(r.exceptionDetails));
 return r.result.value;
}
const mode=process.argv[2]||'all';
try{
 if(['all','import'].includes(mode)){
  await run(`(()=>{newProject(Formats.free);Codecs.project.parse(${JSON.stringify(model)});Project.name=${JSON.stringify(model.name)};Project.save_path=${JSON.stringify(path.join(out,'maodie_fighter_v11.bbmodel'))};Project.saved=true;Canvas.updateAll();return true})()`);
  // Hidden Electron renderer timers can be background-throttled for a minute.
  // Let atlas decoding settle using the external Node clock, not a page timer.
  await new Promise(resolve=>setTimeout(resolve,500));
  const info=await run(`(()=>{Canvas.updateAll();return {name:Project.name,cubes:Cube.all.length,meshes:Mesh.all.length,bones:Group.all.length,animations:Animation.all.length,euler_order:Format.euler_order,textures:Texture.all.map(t=>({name:t.name,width:t.width,height:t.height,error:t.error}))}})()`);
  assert.equal(info.cubes,model.elements.filter(e=>e.type==='cube').length);assert.equal(info.meshes,1);assert.equal(info.bones,20);assert.equal(info.animations,9);
  assert(info.textures.every(t=>!t.error&&t.width===64&&t.height===32));
  writeFileSync(path.join(out,'blockbench-validation.json'),JSON.stringify(info,null,2)+'\n');console.log('Native import',info);
 }
 const guard=`if(Project.name!==${JSON.stringify(model.name)})throw new Error('Wrong preview project');`;
 if(['all','verify'].includes(mode)){
  const report=await run(`(()=>{${guard}const report=[];Animation.all.forEach(a=>a.playing=false);for(const a of Animation.all){a.playing=true;let low=Infinity,feet=Infinity,high=-Infinity;const frames=Math.ceil(a.length*240);for(let i=0;i<=frames;i++){Timeline.time=a.length*i/frames;Animator.preview();scene.updateMatrixWorld(true);for(const c of [...Cube.all,...Mesh.all]){const p=c.mesh.geometry.attributes.position;for(let j=0;j<p.count;j++){const v=new THREE.Vector3().fromBufferAttribute(p,j).applyMatrix4(c.mesh.matrixWorld);low=Math.min(low,v.y);high=Math.max(high,v.y);if(c.name.endsWith('shin'))feet=Math.min(feet,v.y)}}}report.push({name:a.name,frames:frames+1,minimum_y:low,foot_minimum_y:feet,maximum_y:high});a.playing=false}Animator.showDefaultPose();return report})()`);
  writeFileSync(path.join(out,'animation-contact-validation.json'),JSON.stringify(report,null,2)+'\n');console.log('Native contact samples',JSON.stringify(report));
  assert(report.every(r=>r.minimum_y>-.03),'Native animation penetrates floor');
 }
 if(['all','capture','hero','combo-views'].includes(mode)){
  mkdirSync(path.join(out,'previews'),{recursive:true});
  await run(`(()=>{${guard}window.maodiePreview||=new Preview({id:'maodie_fighter_art',offscreen:true});const p=window.maodiePreview;p.resize(720,720);p.setProjectionMode(true);p.controls.enableDamping=false;Cube.all.forEach(c=>c.unselect());return true})()`);
  async function capture(clip,time,view='hero'){
   return await run(`(async()=>{${guard}Animation.all.forEach(a=>a.playing=false);const a=Animation.all.find(a=>a.name==='animation.maodie.${clip}');a.playing=true;Timeline.time=${time};Animator.preview();const p=window.maodiePreview;const view=${JSON.stringify(view)};const jump=${['jump_slam','combat_flow'].includes(clip)},span=jump?25:${clip==='combo'?18:clip==='guard'?20:24};const pos=view==='front'?[0,9,-90]:view==='side'?[90,9,0]:view==='rear'?[65,35,75]:[65,30,-80];const target=jump?[0,15,-4]:[0,9,0];p.loadAnglePreset({projection:'orthographic',position:pos,target,zoom:1});Object.assign(p.camera,{zoom:1,left:-span,right:span,top:span,bottom:-span});p.camera.updateProjectionMatrix();p.camera.position.fromArray(pos);p.controls.target.fromArray(target);p.controls.update();p.render();return await new Promise(resolve=>p.screenshot({crop:false},resolve))})()`);
  }
  const views=mode==='hero'?['hero']:['hero','front','side','rear'];
  for(const view of views){const b64=await capture('guard',0,view);writeFileSync(path.join(out,'previews','guard_'+view+'.png'),Buffer.from(b64.split(',')[1],'base64'));console.log('Rendered guard',view)}
  for(const view of ['front','side']){const b64=await capture('jab',clips.find(c=>c.name==='jab').peak,view);writeFileSync(path.join(out,'previews','jab_'+view+'.png'),Buffer.from(b64.split(',')[1],'base64'))}
  for(const view of ['front','side']){const b64=await capture('roundhouse',clips.find(c=>c.name==='roundhouse').peak,view);writeFileSync(path.join(out,'previews','roundhouse_'+view+'.png'),Buffer.from(b64.split(',')[1],'base64'))}
  for(const [phase,time] of [['wind',.0528],['impact',.0864],['follow',.0984]])for(const view of ['hero','side']){const b64=await capture('combo',time,view);writeFileSync(path.join(out,'previews','combo_'+phase+'_'+view+'.png'),Buffer.from(b64.split(',')[1],'base64'))}
  if(['all','capture','combo-views'].includes(mode))for(const view of ['front','side']){
   const clip=clips.find(c=>c.name==='combo'),dir=path.join(out,'previews','combo_'+view);mkdirSync(dir,{recursive:true});const frames=Math.ceil(clip.length*120);
   for(let i=0;i<=frames;i++){const b64=await capture('combo',clip.length*i/frames,view);writeFileSync(path.join(dir,String(i).padStart(3,'0')+'.png'),Buffer.from(b64.split(',')[1],'base64'))}
   console.log('Rendered combo view',view,frames+1,'frames');
  }
  if(!['hero','combo-views'].includes(mode))for(const clip of clips){
   const b64=await capture(clip.name,clip.peak);writeFileSync(path.join(out,'previews',clip.name+'.png'),Buffer.from(b64.split(',')[1],'base64'));
   const dir=path.join(out,'previews',clip.name);mkdirSync(dir,{recursive:true});
   const frames=Math.ceil(clip.length*120);
   for(let i=0;i<=frames;i++){const b64=await capture(clip.name,clip.length*i/frames);writeFileSync(path.join(dir,String(i).padStart(3,'0')+'.png'),Buffer.from(b64.split(',')[1],'base64'))}
   console.log('Rendered clip',clip.name,frames+1,'frames');
  }
  await run(`(()=>{Animation.all.forEach(a=>a.playing=false);const a=Animation.all.find(a=>a.name==='animation.maodie.guard');a.select();a.playing=true;Timeline.time=0;Animator.preview();Preview.selected.loadAnglePreset({projection:'orthographic',position:[65,38,-80],target:[0,15,0],zoom:.9});Preview.selected.controls.update();Project.saved=true;return true})()`);
 }
}finally{socket.close()}
