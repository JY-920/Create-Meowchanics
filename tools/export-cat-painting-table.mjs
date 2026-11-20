import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import {faceVertices} from './export-cat-editor.mjs';
const root=path.resolve(import.meta.dirname,'..');
const source=path.join(root,'assets-source/cat-painting-table/model.bbmodel');
const model=JSON.parse(fs.readFileSync(source,'utf8'));
assert(model.outliner.every(x=>typeof x==='string'),'Group transforms need explicit baking');
const obj=['mtllib cat_painting_table.mtl'];let n=0;
function transform(p,e){
 const origin=e.origin||[0,0,0];
 let [x,y,z]=p.map((v,i)=>v-(e.type==='mesh'?0:origin[i]));
 const [a,b,c]=(e.rotation||[0,0,0]).map(v=>v*Math.PI/180);
 if(a)[y,z]=[y*Math.cos(a)-z*Math.sin(a),y*Math.sin(a)+z*Math.cos(a)];
 if(b)[x,z]=[x*Math.cos(b)+z*Math.sin(b),-x*Math.sin(b)+z*Math.cos(b)];
 if(c)[x,y]=[x*Math.cos(c)-y*Math.sin(c),x*Math.sin(c)+y*Math.cos(c)];
 return [x,y,z].map((v,i)=>(v+origin[i])/16);
}
for(const e of model.elements){
 if(e.export===false)continue;
 obj.push('o part_'+e.uuid);
 for(const [dir,f]of Object.entries(e.faces)){
  if(f.texture==null)continue;
  const tex=model.textures[f.texture];
  let vertices,uv;
  if(e.type==='mesh'){vertices=f.vertices.map(k=>e.vertices[k]);uv=f.vertices.map(k=>f.uv[k]);}
  else{const raw=faceVertices(dir,e.from,e.to,f.uv);vertices=raw.map(v=>v.slice(0,3));uv=raw.map(v=>v.slice(3));}
  assert(vertices.length===4,'Expected authored quads');
  obj.push('usemtl tex'+f.texture);
  vertices.map(v=>transform(v,e)).forEach(v=>obj.push('v '+v.join(' ')));
  uv.forEach(v=>obj.push('vt '+v[0]/tex.uv_width+' '+(1-v[1]/tex.uv_height)));
  obj.push('f '+[1,2,3,4].map(i=>(n+i)+'/'+(n+i)).join(' '));n+=4;
 }
}
for(const port of ['forge-1.20.1','neoforge-1.21.1']){
 const base=path.join(root,port,'src/main/resources/assets/laowu');
 const write=(file,data)=>{fs.mkdirSync(path.dirname(path.join(base,file)),{recursive:true});fs.writeFileSync(path.join(base,file),data);};
 write('models/block/cat_painting_table.obj',obj.join('\n')+'\n');
 write('models/block/cat_painting_table.mtl',model.textures.map((_,i)=>'newmtl tex'+i+'\nmap_Kd #'+i).join('\n')+'\n');
 const textures={particle:'laowu:block/cat_painting_table_0'};
 model.textures.forEach((t,i)=>{assert(t.source.startsWith('data:image/png;base64,'));write('textures/block/cat_painting_table_'+i+'.png',Buffer.from(t.source.split(',')[1],'base64'));textures[i]='laowu:block/cat_painting_table_'+i;});
 write('models/block/cat_painting_table.json',JSON.stringify({parent:'minecraft:block/block',loader:(port.startsWith('neo')?'neoforge':'forge')+':obj',model:'laowu:models/block/cat_painting_table.obj',automatic_culling:false,shade_quads:true,flip_v:true,render_type:'minecraft:cutout',textures},null,2)+'\n');
}
console.log('Exported exact '+model.elements.length+' parts / '+n/4+' quads and three embedded original textures per loader');

