import assert from 'node:assert/strict';
import {readFileSync,existsSync} from 'node:fs';
import {createRequire} from 'node:module';
import {fileURLToPath,pathToFileURL} from 'node:url';
import path from 'node:path';
const require=createRequire(import.meta.url),deps='C:/Users/16611/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/';
const {chromium}=require(deps+'playwright'),sharp=require(deps+'sharp'),{PNG}=require(deps+'pngjs');
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..'),out=path.join(root,'art/maodie-fighter-v11');
const data=JSON.parse(readFileSync(path.join(out,'preview-data.js'),'utf8').replace('window.MAODIE_PREVIEW=','').trim().replace(/;$/,''));
for(const c of data){
 for(const src of c.frames){
  const file=path.join(out,src);assert(existsSync(file));const p=PNG.sync.read(readFileSync(file));
  let pixels=0,left=p.width,right=0,top=p.height,bottom=0;
  for(let y=0;y<p.height;y++)for(let x=0;x<p.width;x++)if(p.data[(y*p.width+x)*4+3]>20){pixels++;left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y)}
  assert(pixels>5000,'Blank native render: '+src);
  assert(left>2&&right<p.width-3&&top>2&&bottom<p.height-3,'Camera cuts off model: '+src);
 }
 // GIF encoders merge visually identical consecutive frames while retaining delay.
 if(c.name!=='guard'){const m=await sharp(path.join(out,'previews',c.name+'.gif'),{animated:true}).metadata();assert(m.pages>=c.frames.length*.6&&m.pages<=c.frames.length,'GIF lost animated motion');const expected=Math.round(c.length/(c.frames.length-1)*4000/10)*10*(c.frames.length-1)+350;assert(Math.abs(m.delay.reduce((a,b)=>a+b,0)-expected)<=10,'GIF discarded frame time');assert.equal(m.pageHeight,720)}
}
const browser=await chromium.launch({channel:'msedge',headless:true});
try{
 const page=await browser.newPage({viewport:{width:1200,height:1000}}),errors=[];
 page.on('pageerror',e=>errors.push(e.message));
 await page.goto(pathToFileURL(path.join(out,'preview.html')).href);
 await page.waitForFunction(()=>document.querySelector('#motion-video')?.readyState>=2);
 assert.equal(await page.locator('#clips button').count(),9);
 assert.equal(await page.locator('#speed').inputValue(),'1','Latest explosive revision must open at normal speed');
 assert.equal(await page.locator('#motion-video').evaluate(v=>v.playbackRate),1,'Video must actually play at normal speed on initial load');
 assert.equal(await page.locator('[data-clip="combo"]').getAttribute('aria-pressed'),'true');
 assert.equal(await page.locator('#motion-video').isVisible(),true,'Real-speed flow needs video playback rather than hundreds of decoded PNGs');
 await page.waitForFunction(()=>document.querySelector('#motion-video').readyState>=2);
 const videoTime=await page.locator('#motion-video').evaluate(v=>v.currentTime);await page.waitForTimeout(120);
 assert.notEqual(await page.locator('#motion-video').evaluate(v=>v.currentTime),videoTime,'Video preview failed to play');
 for(const c of data){
  await page.locator(`[data-clip="${c.name}"]`).click();
  // A playing 120 Hz image sequence changes src while decode() is pending.
  if(!['combat_flow','combo'].includes(c.name))await page.waitForFunction(name=>{const im=document.querySelector('#frame');return im.complete&&im.naturalWidth===720&&im.src.includes('/'+name+'/')},c.name);
  assert.equal(await page.locator(`[data-clip="${c.name}"]`).getAttribute('aria-pressed'),'true');
  assert.equal(await page.locator('#views').isVisible(),['guard','combo'].includes(c.name));
 }
 await page.locator('[data-clip="jump_slam"]').click();
 await page.locator('#seek').fill('500');
 assert.equal(await page.locator('#play').innerText(),'播放');
 await page.selectOption('#speed','1');
 await page.locator('#play').click();
 const before=await page.locator('#frame').getAttribute('src');await page.waitForTimeout(120);
 assert.notEqual(await page.locator('#frame').getAttribute('src'),before,'Playback stuck on one frame');
 await page.locator('[data-clip="guard"]').click();await page.getByRole('button',{name:'背面',exact:true}).click();
 assert((await page.locator('#frame').getAttribute('src')).endsWith('guard_rear.png'));
 await page.locator('[data-clip="jab"]').click();await page.locator('#seek').fill('250');await page.locator('#frame').evaluate(im=>im.decode());
 await page.screenshot({path:path.join(out,'previews','preview-page.png'),fullPage:true});
 assert.deepEqual(errors,[]);
 console.log('PASS: native frame coverage, complete GIF timing, nine clips, normal-speed combo, scrub/pause/speed and guard views');
}finally{await browser.close()}
