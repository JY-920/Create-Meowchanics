// Use the editor's real Java pixel pipeline on ORIGINAL photos. Sharp only renders the contact sheet.
import fs from 'node:fs/promises';
import path from 'node:path';
import {createRequire} from 'node:module';
import {execFileSync} from 'node:child_process';
import {createHash} from 'node:crypto';
import assert from 'node:assert/strict';
const require=createRequire(import.meta.url),sharp=require('sharp');
const root=path.resolve(import.meta.dirname,'../..'),art=path.join(root,'art/cat-painting-gallery');
const {entries,sourceDirectory}=JSON.parse(await fs.readFile(path.join(art,'manifest.json'),'utf8'));
const sources=path.join(root,sourceDirectory),checkOnly=process.argv.includes('--check');
const ports=[['forge-1.20.1',17],['neoforge-1.21.1',21]];
const compiled=[];
function run(binary,args){const output=execFileSync(binary,args,{encoding:'utf8',cwd:root,stdio:['ignore','pipe','pipe']});if(output.trim())console.log(output.trim());}
for(const [port,version] of ports){
 const bin=path.join(process.env['JAVA'+version+'_HOME']||'C:/Program Files/Zulu/zulu-'+version,'bin');
 const build=path.join(root,'build/painting-gallery-native',port),classes=path.join(build,'classes');
 await fs.mkdir(classes,{recursive:true});
 const domain=path.join(root,port,'src/main/java/cn/laowu/mod/painting');
 run(path.join(bin,'javac.exe'),['-encoding','UTF-8','-d',classes,path.join(domain,'PaintingSpec.java'),path.join(domain,'PaintingImages.java'),path.join(root,'tools/painting/ExportBuiltinGallery.java'),path.join(root,'tests/painting/PaintingBuiltinPixelsCheck.java')]);
 const java=path.join(bin,'java.exe'),prefix=['-Djava.awt.headless=true','-Xmx256m','-cp',classes];
 if(!checkOnly)run(java,[...prefix,'ExportBuiltinGallery',sources,path.join(build,'textures'),...entries.flatMap(e=>[e.id,String(e.width),String(e.height),e.crop,e.source])]);
 compiled.push({port,build,java,prefix});
}
if(!checkOnly){
 for(const e of entries){
  const data=await Promise.all(compiled.map(p=>fs.readFile(path.join(p.build,'textures',e.id+'.png'))));
  assert.ok(data[0].equals(data[1]),'Forge/NeoForge native conversion mismatch: '+e.id);
  for(const p of compiled){
   const dest=path.join(root,p.port,'src/main/resources/assets/laowu/textures/painting/builtin');
   await fs.mkdir(dest,{recursive:true});await fs.writeFile(path.join(dest,e.id+'.png'),data[0]);
  }
 }
}
for(const p of compiled)run(p.java,[...p.prefix,'PaintingBuiltinPixelsCheck',root,p.port]);
if(checkOnly)process.exit(0);
const layers=[],labels=[],report=[];
for(let i=0;i<entries.length;i++){
 const e=entries[i],file=path.join(root,ports[0][0],'src/main/resources/assets/laowu/textures/painting/builtin',e.id+'.png');
 const bytes=await fs.readFile(file),meta=await sharp(bytes).metadata();
 assert.equal(meta.width,e.width*16);assert.equal(meta.height,e.height*16);
 const x=24+(i%4)*270,y=48+Math.floor(i/4)*286;
 layers.push({input:await sharp(bytes).resize(224,e.height/e.width*224,{kernel:'nearest'}).toBuffer(),left:x,top:y+(e.width>e.height?56:0)});
 labels.push('<text x="'+x+'" y="'+(y+248)+'">'+e.id+' · '+e.width+'×'+e.height+'</text>');
 report.push({id:e.id,width:meta.width,height:meta.height,bytes:bytes.length,sourceSha256:createHash('sha256').update(await fs.readFile(path.join(sources,e.source))).digest('hex'),textureSha256:createHash('sha256').update(bytes).digest('hex')});
}
const svg='<svg width="1100" height="900"><g font-family="Arial" font-size="17" fill="#e8dfcf"><text x="24" y="28">ORIGINAL PHOTOS · IN-GAME PIXEL MODE · 16 PIXELS PER BLOCK</text>'+labels.join('')+'</g></svg>';
layers.push({input:Buffer.from(svg),left:0,top:0});
const sheet=await sharp({create:{width:1100,height:900,channels:3,background:'#242426'}}).composite(layers).png().toBuffer();
await fs.writeFile(path.join(art,'contact-sheet.png'),sheet);
await fs.writeFile(path.join(art,'native-pixel-contact-sheet.png'),sheet);
await fs.writeFile(path.join(art,'texture-report.json'),JSON.stringify(report,null,2)+'\n');
console.log('PASS: native game pixel conversion; '+report.reduce((n,e)=>n+e.bytes,0)+' bytes per port');

