import assert from 'node:assert/strict';
import {copyFileSync,readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {exportEditorObj} from './export-cat-editor.mjs';
const root=path.resolve(import.meta.dirname,'..');
const sourceDir=path.join(root,'assets-source/bogu-shelf');
const round=x=>Math.round(x*1e9)/1e9;
function area(p) {
 let value=0;
 for(let i=1;i<p.length-1;i++) {const a=p[i].map((v,j)=>v-p[0][j]),b=p[i+1].map((v,j)=>v-p[0][j]);value+=Math.hypot(a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0])/2;}
 return value;
}
function clip(p,axis,value,sign) {
 const out=[];
 for(let i=0;i<p.length;i++) {const a=p[i],b=p[(i+1)%p.length],da=(a[axis]-value)*sign,db=(b[axis]-value)*sign;
  if(da>=-1e-10)out.push(a);
  if((da>1e-10&&db< -1e-10)||(da< -1e-10&&db>1e-10)){const t=(value-a[axis])/(b[axis]-a[axis]);out.push(a.map((v,j)=>v+(b[j]-v)*t));}
 }
 return out;
}
function obj(faces,name) {
 let n=0;const lines=[`# Authored shelf, exact UV; ${name}`, 'mtllib round_bogu_shelf.mtl','usemtl editor'];
 for(const p of faces) for(let i=1;i<p.length-1;i++) {
  const q=[p[0],p[i],p[i+1],p[i+1]];
  if(area(q)<1e-12)continue;
  lines.push(...q.map(v=>'v '+v.slice(0,3).map(round).join(' ')),...q.map(v=>'vt '+v.slice(3).map(round).join(' ')),`f ${[1,2,3,4].map(k=>`${n+k}/${n+k}`).join(' ')}`);n+=4;
 }
 return lines.join('\n')+'\n';
}
function merged(boxes) {
 let out=[...new Map(boxes.map(b=>[b.map(round).join(','),b])).values()];
 for(let axis=0;axis<3;axis++) {
  const groups=new Map();
  for(const b of out) {const key=b.filter((_,i)=>i!==axis&&i!==axis+3).map(round).join(',');if(!groups.has(key))groups.set(key,[]);groups.get(key).push(b);}
  out=[];for(const rows of groups.values()) {rows.sort((a,b)=>a[axis]-b[axis]);let current=null;for(const b of rows) {if(current&&b[axis]<=current[axis+3]+1e-9)current[axis+3]=Math.max(current[axis+3],b[axis+3]);else {current=b.slice();out.push(current);}}}
 }
 return out;
}
export function exportShelf(source) {
 const full=exportEditorObj(source).replace('mtllib cat_editor.mtl','mtllib round_bogu_shelf.mtl');const vertices=[],uvs=[],faces=[];
 for(const line of full.split('\n')) {
  if(line.startsWith('v '))vertices.push(line.slice(2).split(' ').map(Number));
  if(line.startsWith('vt '))uvs.push(line.slice(3).split(' ').map(Number));
  if(line.startsWith('f '))faces.push(line.slice(2).split(' ').map(s=>{const [v,t]=s.split('/').map(Number);return [...vertices[v-1],...uvs[t-1]];}));
 }
 const bounds=[0,1,2].map(i=>[Math.min(...vertices.map(v=>v[i])),Math.max(...vertices.map(v=>v[i]))]);
 assert.ok(bounds[0][0]>=-1&&bounds[0][1]<=2&&bounds[1][0]>=0&&bounds[1][1]<=3&&bounds[2][0]>=0&&bounds[2][1]<=1,'model fits 3x3x1');
 const boxes=[];for(const chunk of full.split('\no ').slice(1)) {const points=chunk.split('\n').filter(l=>l.startsWith('v ')).map(l=>l.slice(2).split(' ').map(Number));if(points.length)boxes.push([...Array.from({length:3},(_,i)=>Math.min(...points.map(v=>v[i]))),...Array.from({length:3},(_,i)=>Math.max(...points.map(v=>v[i])))]);}
 let partArea=0;const parts=[];
 for(let row=0;row<3;row++)for(let col=-1;col<=1;col++) {
  const polygons=[];
  for(const face of faces) {let p=clip(clip(clip(clip(face,0,col,1),0,col+1,-1),1,row,1),1,row+1,-1);if(p.length<3||area(p)<1e-12)continue;
   // Coplanar boundary faces belong to one cell only; avoid z-fighting duplicates.
   const cx=p.reduce((a,v)=>a+v[0],0)/p.length,cy=p.reduce((a,v)=>a+v[1],0)/p.length;
   if(Math.max(-1,Math.min(1,Math.floor(cx+1e-9)))!==col||Math.min(2,Math.floor(cy+1e-9))!==row)continue;
   partArea+=area(p);polygons.push(p.map(v=>[v[0]-col,v[1]-row,...v.slice(2)]));
  }
  const localBoxes=boxes.map(b=>[Math.max(0,b[0]-col),Math.max(0,b[1]-row),b[2],Math.min(1,b[3]-col),Math.min(1,b[4]-row),b[5]]).filter(b=>b[3]>b[0]+1e-9&&b[4]>b[1]+1e-9&&b[5]>b[2]+1e-9);
  parts.push({obj:obj(polygons,`part ${parts.length}`),boxes:merged(localBoxes)});
 }
 return {full,parts,bounds,faces:faces.length,sourceArea:faces.reduce((n,p)=>n+area(p),0),partArea};
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)) {
 const write=process.argv.includes('--write');
 if(process.argv.includes('--import')) {assert.ok(write);mkdirSync(sourceDir,{recursive:true});copyFileSync('C:/Users/16611/Downloads/round_bogu_shelf (5).bbmodel',path.join(sourceDir,'model.bbmodel'));}
 const source=JSON.parse(readFileSync(path.join(sourceDir,'model.bbmodel')));const result=exportShelf(source);
 assert.ok(Math.abs(result.sourceArea-result.partArea)<1e-7,'surface conservation');
 const texture=Buffer.from(source.textures[0].source.split(',')[1],'base64');
 for(const port of ['forge-1.20.1','neoforge-1.21.1']) {
  const assets=path.join(root,port,'src/main/resources/assets/laowu');
  const files=new Map([['models/block/round_bogu_shelf.obj',result.full],['models/block/round_bogu_shelf.mtl','newmtl editor\nmap_Kd #0\n'],['textures/block/round_bogu_shelf.png',texture]]);
  const model=n=>({parent:'minecraft:block/block',loader:`${port.startsWith('neo')?'neoforge':'forge'}:obj`,model:`laowu:models/block/${n}.obj`,automatic_culling:false,shade_quads:true,flip_v:true,render_type:'minecraft:cutout',textures:{'0':'laowu:block/round_bogu_shelf',particle:'laowu:block/round_bogu_shelf'}});
  const json=(name,value)=>files.set(name,JSON.stringify(value,null,2)+'\n');const variants={};
  for(let i=0;i<9;i++) {files.set(`models/block/round_bogu_shelf_${i}.obj`,result.parts[i].obj);json(`models/block/round_bogu_shelf_${i}.json`,model(`round_bogu_shelf_${i}`));
   for(const [facing,y] of Object.entries({north:0,east:90,south:180,west:270}))variants[`facing=${facing},part=${i}`]={model:`laowu:block/round_bogu_shelf_${i}`,...(y?{y}:{})};
  }
  json('blockstates/round_bogu_shelf.json',{variants});
  json('models/item/round_bogu_shelf.json',{...model('round_bogu_shelf'),display:{gui:{rotation:[15,165,0],translation:[0,-5,0],scale:[.27,.27,.27]},ground:{translation:[0,-2,0],scale:[.2,.2,.2]},fixed:{translation:[0,-6,0],scale:[.3,.3,.3]},thirdperson_righthand:{rotation:[0,90,0],translation:[0,-2,0],scale:[.25,.25,.25]},firstperson_righthand:{rotation:[0,90,0],translation:[0,-3,0],scale:[.25,.25,.25]},firstperson_lefthand:{rotation:[0,-90,0],translation:[0,-3,0],scale:[.25,.25,.25]}}});
  for(const [name,data] of files) {const target=path.join(assets,name);if(write){mkdirSync(path.dirname(target),{recursive:true});writeFileSync(target,data);}else assert.deepEqual(readFileSync(target),Buffer.from(data));}
  const shapes=`package cn.laowu.mod.block;\n/** Generated by tools/export-bogu-shelf.mjs; rotated-cube AABBs, merged exact neighbours. */\nfinal class BoguShelfShapeData {\n static final double[][][] BOXES = {\n${result.parts.map(p=>'  {\n'+p.boxes.map(b=>'   {'+b.map(round).join(',')+'}').join(',\n')+'\n  }').join(',\n')}\n };\n private BoguShelfShapeData() {}\n}\n`;
  const target=path.join(root,port,'src/main/java/cn/laowu/mod/block/BoguShelfShapeData.java');if(write){mkdirSync(path.dirname(target),{recursive:true});writeFileSync(target,shapes);}else assert.equal(readFileSync(target,'utf8'),shapes);
 }
 console.log('PASS exact shelf export:',JSON.stringify({faces:result.faces,bounds:result.bounds,boxes:result.parts.map(p=>p.boxes.length),area:result.partArea}));
}
