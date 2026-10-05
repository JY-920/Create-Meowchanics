import {createRequire} from 'node:module';
import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
const require=createRequire(import.meta.url);
const {chromium}=require('C:/Users/16611/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const browser=await chromium.connectOverCDP('http://127.0.0.1:9337');
const out=path.resolve(import.meta.dirname,'../art/giant-tabby-boss-v2');
const action=process.argv[2]||'inspect';
try{
 const pages=browser.contexts().flatMap(c=>c.pages());
 const page=pages.find(p=>p.url().includes('index.html'))||pages[0];
 await page.waitForFunction(()=>typeof Blockbench!=='undefined'&&typeof Codecs!=='undefined');
 console.log(await page.evaluate(()=>({version:Blockbench.version,projects:ModelProject.all.map(p=>({name:p.name,saved:p.saved})),codecs:Object.keys(Codecs),preview:!!Preview.selected})));
 if(action==='inspect')process.exitCode=0;
 else{
 const model=JSON.parse(fs.readFileSync(path.join(out,'giant_tabby_boss.bbmodel'),'utf8'));
 const info=await page.evaluate(async ({model,out})=>{
  newProject(Formats.free);Codecs.project.parse(model);Project.name=model.name;
  Project.save_path=out+'/giant_tabby_boss.bbmodel';Project.saved=true;
  await new Promise(r=>setTimeout(r,500));Canvas.updateAll();
  return {version:Blockbench.version,cubes:Cube.all.length,bones:Group.all.length,animations:Animation.all.length,textures:Texture.all.map(t=>({name:t.name,width:t.width,height:t.height,error:t.error}))};
 },{model,out});
 assert.equal(info.cubes,model.elements.length);assert.equal(info.bones,model.groups.length);
 assert.equal(info.animations,model.animations.length);assert(info.textures.every(t=>!t.error&&t.width===model.textures[0].width));
 console.log(info);fs.writeFileSync(path.join(out,'blockbench-validation.json'),JSON.stringify(info,null,2)+'\n');
 if(action==='import'){}else{
 if(action==='export'){
  // Preserve the authored movement; only lift poses whose transformed mesh clips the floor.
  const corrected=await page.evaluate(()=>{
   const root=Group.all.find(g=>g.name==='root');
   const minY=()=>{scene.updateMatrixWorld(true);let low=Infinity;for(const c of Cube.all){const p=c.mesh.geometry.attributes.position;for(let i=0;i<p.count;i++)low=Math.min(low,new THREE.Vector3().fromBufferAttribute(p,i).applyMatrix4(c.mesh.matrixWorld).y);}return low;};
   Animation.all.forEach(a=>a.playing=false);
   const report=[];
   for(const a of Animation.all){
    const animator=a.getBoneAnimator(root),old=[...animator.position].sort((a,b)=>a.time-b.time);
    const value=k=>['x','y','z'].map(v=>Number(k.data_points[0][v]));
    const at=t=>{if(!old.length)return [0,0,0];if(t<=old[0].time)return value(old[0]);for(let i=1;i<old.length;i++)if(t<=old[i].time){const f=(t-old[i-1].time)/(old[i].time-old[i-1].time),p=value(old[i-1]),q=value(old[i]);return p.map((v,j)=>v+(q[j]-v)*f);}return value(old.at(-1));};
    a.playing=true;const frames=[];
    for(let i=0;i<=Math.round(a.length*60);i++){
     const t=i/60;Timeline.time=t;Animator.preview();const low=minY(),pos=at(t);pos[1]+=Math.max(0,-low)+.08;frames.push({t,pos});
    }
    animator.position.splice(0);
    for(const {t,pos} of frames)animator.addKeyframe({channel:'position',time:t,interpolation:'linear',data_points:[Object.fromEntries(['x','y','z'].map((v,i)=>[v,String(pos[i])]))]});
    let low=Infinity;
    for(let i=0;i<=Math.round(a.length*120);i++){Timeline.time=i/120;Animator.preview();low=Math.min(low,minY());}
    report.push({name:a.name,frames:Math.round(a.length*120)+1,minimum_y:low});a.playing=false;
   }
   Animator.showDefaultPose();Project.saved=true;
   return {report,model:Codecs.project.compile({raw:true})};
  });
  console.log('Ground contacts',corrected.report);
  assert(corrected.report.every(a=>a.minimum_y>=0),'Animation floor clipping');
  const saved=typeof corrected.model==='string'?JSON.parse(corrected.model):corrected.model;
  saved.textures.forEach(t=>{t.path='';t.relative_path='giant_tabby_boss.png';});
  fs.writeFileSync(path.join(out,'giant_tabby_boss.bbmodel'),JSON.stringify(saved)+'\n');
  fs.writeFileSync(path.join(out,'animation-contact-validation.json'),JSON.stringify(corrected.report,null,2)+'\n');
  const exports=await page.evaluate(async()=>{
   Animator.showDefaultPose();
   const geo=Codecs.bedrock.compile({raw:true});
   const animations=AnimationCodec.codecs.bedrock.compileFile(Animation.all);
   const data=new Uint8Array(await Codecs.gltf.compile({encoding:'binary',scale:16,embed_textures:true,armature:true,animations:true}));
   let s='';for(let i=0;i<data.length;i+=4096)s+=String.fromCharCode(...data.subarray(i,i+4096));
   return {geo,animations,glb:btoa(s)};
  });
  for(const [name,data]of [['giant_tabby_boss.geo.json',exports.geo],['giant_tabby_boss.animation.json',exports.animations]])fs.writeFileSync(path.join(out,name),typeof data==='string'?data:JSON.stringify(data,null,2));
  fs.writeFileSync(path.join(out,'giant_tabby_boss.glb'),Buffer.from(exports.glb,'base64'));
 }
 const views=[
  {name:'hero',position:[150,76,-140],target:[0,24,12],zoom:.20},
  {name:'side',position:[-200,26,12],target:[0,26,12],zoom:.19},
  {name:'front',position:[0,26,-200],target:[0,26,0],zoom:.37},
  {name:'rear',position:[120,80,160],target:[0,26,12],zoom:.23},
  {name:'face-detail',position:[75,55,-160],target:[0,35,-39],zoom:.63},
  {name:'material-detail',position:[-150,50,0],target:[0,28,0],zoom:.57}
 ];
 fs.mkdirSync(path.join(out,'previews'),{recursive:true});
 // The first offscreen screenshot in Blockbench may precede scene attachment.
 // Capture hero again after the other views; packaging also checks nonempty coverage.
 for(const view of [...views,views[0]]){
  const b64=await page.evaluate(async v=>{
   Animation.all.forEach(a=>a.playing=false);Animator.showDefaultPose();Cube.all.forEach(c=>c.unselect());
   window.giantV2Preview||=new Preview({id:'giant_tabby_v2',offscreen:true});
   const p=window.giantV2Preview;p.resize(1400,1000);p.setProjectionMode(true);p.controls.enableDamping=false;
   p.loadAnglePreset({projection:'orthographic',position:v.position,target:v.target,zoom:v.zoom});
   p.camera.zoom=v.zoom;p.camera.updateProjectionMatrix();p.camera.position.fromArray(v.position);p.controls.target.fromArray(v.target);p.controls.update();
   p.render();await new Promise(r=>setTimeout(r,150));p.render();
   return await new Promise(resolve=>p.screenshot({crop:false},resolve));
  },view);
  fs.writeFileSync(path.join(out,'previews',view.name+'.png'),Buffer.from(b64.split(',')[1],'base64'));
  console.log('Rendered',view.name);
 }
 }
 }
}finally{await browser.close()}
