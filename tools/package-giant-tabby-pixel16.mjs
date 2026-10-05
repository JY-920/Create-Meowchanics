import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import {createHash} from 'node:crypto';
import {createRequire} from 'node:module';
import {execFileSync} from 'node:child_process';
const require=createRequire(import.meta.url);
const deps='C:/Users/16611/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules';
const {PNG}=require(deps+'/pngjs'),JSZip=require(deps+'/jszip');
const root=path.resolve(import.meta.dirname,'..'),out=path.join(root,'art/giant-tabby-boss-v2');
execFileSync(process.execPath,[path.join(root,'tests/giant-tabby-v2.mjs'),path.join(out,'giant_tabby_boss.bbmodel')],{stdio:'inherit'});
const read=name=>fs.readFileSync(path.join(out,name));
const model=JSON.parse(read('giant_tabby_boss.bbmodel'));
assert(Buffer.from(model.textures[0].source.split(',')[1],'base64').equals(read('giant_tabby_boss.png')),'Embedded model texture differs');
const native=JSON.parse(read('blockbench-validation.json'));
assert.equal(native.cubes,13);assert.equal(native.bones,9);assert.equal(native.animations,8);
assert(native.textures.every(t=>t.width===256&&t.height===256&&!t.error));
const contacts=JSON.parse(read('animation-contact-validation.json'));
assert.equal(contacts.length,8);assert(contacts.every(a=>a.minimum_y>=0));
const glb=read('giant_tabby_boss.glb');
assert.equal(glb.toString('ascii',0,4),'glTF');assert.equal(glb.readUInt32LE(4),2);assert.equal(glb.readUInt32LE(8),glb.length);
const json=JSON.parse(glb.subarray(20,20+glb.readUInt32LE(12)).toString());
assert.equal(json.animations.length,8);assert.equal(json.images.length,1);
assert(json.samplers.every(s=>s.magFilter===9728&&s.minFilter===9728));
const binaryStart=20+glb.readUInt32LE(12)+8,im=json.bufferViews[json.images[0].bufferView];
const embedded=glb.subarray(binaryStart+(im.byteOffset||0),binaryStart+(im.byteOffset||0)+im.byteLength);
// Blockbench's GLB bufferView includes its 0-3 byte alignment padding after PNG IEND.
let pngEnd=8;
while(pngEnd+12<=embedded.length){const n=embedded.readUInt32BE(pngEnd),type=embedded.toString('ascii',pngEnd+4,pngEnd+8);pngEnd+=n+12;if(type==='IEND')break;}
assert(pngEnd<=embedded.length&&embedded.length-pngEnd<=3);
assert(embedded.subarray(pngEnd).every(v=>v===0),'Unexpected trailing PNG data');
const glbPng=PNG.sync.read(embedded.subarray(0,pngEnd));
assert.equal(glbPng.width,256);assert.equal(glbPng.height,256);
assert(glbPng.data.equals(PNG.sync.read(read('giant_tabby_boss.png')).data),'GLB has stale texture');
const previews=['hero','side','front','rear','face-detail','material-detail'];
for(const name of previews){
 const p=PNG.sync.read(read('previews/'+name+'.png'));let visible=0;
 for(let i=3;i<p.data.length;i+=4)if(p.data[i]>128)visible++;
 assert(visible>p.width*p.height*.035,`${name}: empty or tiny render`);
}
// Explicit allowlist: never package the rejected source/material.png or stale references.
const files=['README.md','giant_tabby_boss.bbmodel','giant_tabby_boss.png','giant_tabby_boss.glb','giant_tabby_boss.geo.json','giant_tabby_boss.animation.json','manifest.json','uv-density.json','blockbench-validation.json','animation-contact-validation.json','source/vanilla_yellow_cat.bbmodel','source/yellow_cat.png',...previews.map(n=>'previews/'+n+'.png')];
const zip=new JSZip(),hashes={};
for(const file of files){const bytes=read(file);zip.file(file,bytes);hashes[file]=createHash('sha256').update(bytes).digest('hex');}
const output=path.join(root,'releases/giant-tabby-boss-pixel16.zip');
fs.mkdirSync(path.dirname(output),{recursive:true});
fs.writeFileSync(output,await zip.generateAsync({type:'nodebuffer',compression:'DEFLATE',compressionOptions:{level:9}}));
const reopened=await JSZip.loadAsync(fs.readFileSync(output),{checkCRC32:true});
for(const [name,hash]of Object.entries(hashes))assert.equal(createHash('sha256').update(await reopened.file(name).async('nodebuffer')).digest('hex'),hash);
const report={output,bytes:fs.statSync(output).size,files:hashes,animation_samples:contacts.reduce((s,a)=>s+a.frames,0),texels_per_block:16,mod_jars_changed:false};
fs.writeFileSync(path.join(root,'releases/giant-tabby-boss-pixel16.manifest.json'),JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify({...report,files:files.length},null,2));
