import fs from 'node:fs/promises';
import path from 'node:path';
import assert from 'node:assert/strict';
import {createHash} from 'node:crypto';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url),sharp=require('sharp'),JSZip=require('jszip');
const root=path.resolve(import.meta.dirname,'..');
const {entries}=JSON.parse(await fs.readFile(path.join(root,'art/cat-painting-gallery/manifest.json'),'utf8'));
const {entries:named}=JSON.parse(await fs.readFile(path.join(root,'art/cat-painting-named/manifest.json'),'utf8'));
const allEntries=[...entries.map(e=>({...e,pngPixels:[e.width*16,e.height*16]})),...named];
assert.equal(entries.length,12,'Random gallery must remain exactly twelve');
assert.equal(named.length,2,'Two name-only portraits');
assert.equal(new Set(allEntries.map(e=>e.id)).size,14,'Named identities cannot collide with random paintings');
const version=process.argv[2];
let count=0,total=0;
for(const port of ['forge-1.20.1','neoforge-1.21.1']){
 const base=path.join(root,port,'src/main');
 const java=await fs.readFile(path.join(base,'java/cn/laowu/mod/painting/PaintingBuiltinCatalog.java'),'utf8');
 const actual=[...java.matchAll(/new Entry\("([^"]+)",\s*(\d+),\s*(\d+)(?:,\s*PaintingSpec\.Crop\.(KEEP_WIDTH|KEEP_HEIGHT))?\)/g)].map(m=>[m[1],+m[2],+m[3],m[4]||'KEEP_WIDTH']);
 assert.deepEqual(actual,entries.map(e=>[e.id,e.width,e.height,e.crop]),'catalog vs artwork manifest');
 const actualNamed=[...java.matchAll(/new Entry\("([^"]+)",\s*(\d+),\s*(\d+),\s*PaintingSpec\.Crop\.(KEEP_WIDTH|KEEP_HEIGHT),\s*PaintingSpec\.Mode\.ORIGINAL,"([^"]+)"\)/g)].map(m=>[m[1],+m[2],+m[3],m[4],m[5]]);
 assert.deepEqual(actualNamed,named.map(e=>[e.id,...e.blocks,e.crop,e.name]),'Named catalog vs source photo manifest');
 let zip;
 if(version)zip=await JSZip.loadAsync(await fs.readFile(path.join(root,port,'build/libs/create-meowchanics-'+version+'-'+port+'.jar')));
 for(const lang of ['zh_cn','en_us']){
  const json=JSON.parse(await fs.readFile(path.join(base,'resources/assets/laowu/lang/'+lang+'.json'),'utf8'));
  for(const e of allEntries)assert.ok(json['painting.laowu.builtin.'+e.id]);
  assert.ok(json['painting.laowu.blank.edit']);
 }
 const dir=path.join(base,'resources/assets/laowu/textures/painting/builtin');
 assert.deepEqual((await fs.readdir(dir)).sort(),allEntries.map(e=>e.id+'.png').sort());
 for(const e of allEntries){
  const resource='assets/laowu/textures/painting/builtin/'+e.id+'.png';
  const bytes=await fs.readFile(path.join(base,'resources',resource));
  const meta=await sharp(bytes).metadata();assert.equal(meta.width,e.pngPixels[0]);assert.equal(meta.height,e.pngPixels[1]);
  if(e.pngSha256)assert.equal(createHash('sha256').update(bytes).digest('hex'),e.pngSha256,'Supplied photo must not drift');
  const other=await fs.readFile(path.join(root,'forge-1.20.1/src/main/resources',resource));assert.ok(bytes.equals(other));
  if(zip){assert.ok(zip.file(resource),'packaged '+resource);assert.ok((await zip.file(resource).async('nodebuffer')).equals(bytes));}
  count++;total+=bytes.length;
 }
 if(zip)assert.equal(Object.keys(zip.files).filter(x=>x.includes('/painting/builtin/')&&x.endsWith('.png')).length,allEntries.length);
}
console.log('PASS: '+count+' exact-size textures, both port catalogs + languages + byte parity'+(version?' + runtime jars':'')+'; '+total+' bytes total');

