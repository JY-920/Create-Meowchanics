import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const out = path.join(root, 'art/giant-tabby-boss');
const endpoint = process.env.BLOCKBENCH_MCP_URL || 'http://localhost:3000/bb-mcp';
let session, seq = 0;
async function rpc(method, params, notification = false) {
  const response = await fetch(endpoint, { method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json, text/event-stream',
      ...(session ? { 'mcp-session-id': session } : {}) },
    body: JSON.stringify({ jsonrpc: '2.0', ...(notification ? {} : { id: ++seq }), method, params }),
    signal: AbortSignal.timeout(60000) });
  assert(response.ok, 'Blockbench MCP HTTP ' + response.status);
  session = response.headers.get('mcp-session-id') || session;
  const source = await response.text();
  if (!source) return {};
  const message = source.startsWith('event:') || source.startsWith('data:')
    ? JSON.parse(source.split('\n').find(l => l.startsWith('data:')).slice(5)) : JSON.parse(source);
  if (message.error || message.result?.isError) throw new Error(JSON.stringify(message.error || message.result));
  return message.result;
}
async function call(name, args) {
  const result = await rpc('tools/call', { name, arguments: args });
  let value = result.content?.find(c => c.type === 'text')?.text;
  for (let i = 0; i < 4 && typeof value === 'string'; i++) {
    try { value = JSON.parse(value); } catch { break; }
  }
  if (typeof value === 'string' && value.startsWith('Error executing')) throw new Error(value);
  return value;
}
const run = code => call('risky_eval', { code });
await rpc('initialize', { protocolVersion: '2024-11-05', capabilities: {},
  clientInfo: { name: 'meowchanics-giant-tabby-art', version: '1.0' } });
await rpc('notifications/initialized', {}, true);
const model = JSON.parse(readFileSync(path.join(out, 'giant_tabby_boss.bbmodel'), 'utf8'));
const action = process.argv[2] || 'all';
if (action === 'all' || action === 'import') {
  // A fresh project tab preserves any other open/unsaved artist projects.
  await call('create_project', { name: model.name, format: 'free' });
  const source = JSON.stringify(model).replaceAll('/', '\\u002f');
  const info = await run(`(async()=>{Codecs.project.parse(${source});Project.name=${JSON.stringify(model.name)};Project.save_path=${JSON.stringify(path.join(out, 'giant_tabby_boss.bbmodel')).replaceAll('\\', '\\')};Project.saved=true;await new Promise(r=>setTimeout(r,250));Canvas.updateAll();return JSON.stringify({name:Project.name,cubes:Cube.all.length,bones:Group.all.length,animations:Animation.all.length,textures:Texture.all.map(t=>({name:t.name,width:t.width,height:t.height,error:t.error})),parents:Group.all.map(g=>({name:g.name,parent:g.parent?.name||null}))})})()`);
  assert.equal(info.cubes, model.elements.length);
  assert.equal(info.bones, model.groups.length);
  assert.equal(info.animations, model.animations.length);
  assert(info.textures.every(t => !t.error && t.width === 128 && t.height === 128));
  writeFileSync(path.join(out, 'blockbench-validation.json'), JSON.stringify(info, null, 2) + '\n');
  console.log('Native import passed:', JSON.stringify(info));
}
if (action === 'import') process.exit(0);
const guard = `if(Project.name!==${JSON.stringify(model.name)})throw new Error('Select the giant tabby project first');`;
if (action === 'all' || action === 'verify') {
  const report = await run(`(()=>{${guard}const report=[];Animation.all.forEach(a=>a.playing=false);for(const a of Animation.all){a.playing=true;let low=Infinity,frames=0;for(let i=0;i<=Math.round(a.length*30);i++){Timeline.time=Math.min(a.length,i/30);Animator.preview();scene.updateMatrixWorld(true);for(const c of Cube.all){const p=c.mesh.geometry.attributes.position;for(let j=0;j<p.count;j++){const v=new THREE.Vector3().fromBufferAttribute(p,j).applyMatrix4(c.mesh.matrixWorld);low=Math.min(low,v.y)}}frames++}report.push({name:a.name,frames,minimum_y:low});a.playing=false}Animator.showDefaultPose();return JSON.stringify(report)})()`);
  assert(report.every(a => a.minimum_y > -.03), 'Native animation floor clipping: ' + JSON.stringify(report));
  writeFileSync(path.join(out, 'animation-contact-validation.json'), JSON.stringify(report, null, 2) + '\n');
  console.log('Native contact sampling passed:', JSON.stringify(report));
}
if (action === 'all' || action === 'capture') {
  const views = [
    { name: 'hero', position: [160, 88, -125], target: [0, 29, 8], zoom: .255, pose: 'idle', time: 0 },
    { name: 'side', position: [-200, 30, 10], target: [0, 30, 10], zoom: .25 },
    { name: 'front', position: [0, 30, -200], target: [0, 30, 0], zoom: .33 },
    { name: 'rear', position: [115, 88, 160], target: [0, 30, 8], zoom: .255 },
    { name: 'walk', position: [-160, 88, -125], target: [0, 29, 8], zoom: .255, pose: 'walk', time: .4 },
    { name: 'swipe', position: [125, 80, -160], target: [0, 29, 0], zoom: .255, pose: 'paw_swipe', time: .45 },
    { name: 'bite', position: [-120, 80, -175], target: [0, 29, 4], zoom: .255, pose: 'bite', time: .35 },
    { name: 'pounce', position: [-160, 88, -125], target: [0, 35, 8], zoom: .255, pose: 'pounce', time: .85 },
  ];
  mkdirSync(path.join(out, 'previews'), { recursive: true });
  for (const view of views) {
    const b64 = await run(`(async()=>{${guard}Animator.showDefaultPose();Animation.all.forEach(a=>a.playing=false);const v=${JSON.stringify(view)};if(v.pose){const a=Animation.all.find(a=>a.name==='animation.giant_tabby.'+v.pose);a.playing=true;Timeline.time=v.time;Animator.preview()}window.meowTabbyPreview||=(new Preview({id:'giant_tabby_art',offscreen:true}));const p=window.meowTabbyPreview;p.resize(1400,1000);p.setProjectionMode(true);p.controls.enableDamping=false;p.loadAnglePreset({projection:'orthographic',position:v.position,target:v.target,zoom:v.zoom});p.controls.update();await new Promise(r=>setTimeout(r,50));p.camera.zoom=v.zoom;p.camera.updateProjectionMatrix();p.controls.target.fromArray(v.target);p.camera.position.fromArray(v.position);p.controls.update();Cube.all.forEach(c=>c.unselect());return await new Promise(resolve=>p.screenshot({crop:false},resolve))})()`);
    assert(typeof b64 === 'string' && b64.startsWith('data:image/png;base64,'));
    const bytes = Buffer.from(b64.split(',')[1], 'base64');
    writeFileSync(path.join(out, 'previews', view.name + '.png'), bytes);
    console.log('Rendered', view.name, bytes.length, 'bytes');
  }
  await run(`(()=>{${guard}Animation.all.forEach(a=>a.playing=false);Animator.showDefaultPose();Preview.selected.loadAnglePreset({projection:'orthographic',position:[160,88,-125],target:[0,29,8],zoom:.255});Preview.selected.controls.update();return 'ready'})()`);
}
if (action === 'all' || action === 'export') {
  const bedrock = await run(`(()=>{${guard}Animation.all.forEach(a=>a.playing=false);Animator.showDefaultPose();return Codecs.bedrock.compile({raw:true})})()`);
  const animations = await run(`(()=>{${guard}return AnimationCodec.codecs.bedrock.compileFile(Animation.all)})()`);
  assert.equal(bedrock['minecraft:geometry'][0].bones.length, model.groups.length);
  assert.equal(Object.keys(animations.animations).length, model.animations.length);
  writeFileSync(path.join(out, 'giant_tabby_boss.geo.json'), JSON.stringify(bedrock, null, 2) + '\n');
  writeFileSync(path.join(out, 'giant_tabby_boss.animation.json'), JSON.stringify(animations, null, 2) + '\n');
  const base64 = await run(`(async()=>{${guard}Animator.showDefaultPose();const bytes=new Uint8Array(await Codecs.gltf.compile({encoding:'binary',scale:16,embed_textures:true,armature:true,animations:true}));let s='';for(let i=0;i<bytes.length;i+=4096)s+=String.fromCharCode(...bytes.subarray(i,i+4096));return btoa(s)})()`);
  const glb = Buffer.from(base64, 'base64');
  assert.equal(glb.toString('ascii', 0, 4), 'glTF');
  const jsonLength = glb.readUInt32LE(12);
  const gltf = JSON.parse(glb.subarray(20, 20 + jsonLength).toString('utf8'));
  assert.equal(gltf.animations.length, model.animations.length);
  assert(gltf.images?.length > 0 && gltf.buffers[0].uri === undefined);
  writeFileSync(path.join(out, 'giant_tabby_boss.glb'), glb);
  console.log('Native exports passed:', JSON.stringify({ bedrock_bones: bedrock['minecraft:geometry'][0].bones.length,
    animations: gltf.animations.length, glb_bytes: glb.length, images: gltf.images.length }));
}
