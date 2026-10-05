// Standalone art only: preserve the supplied vanilla cat rig, not the old bulky v1 mesh.
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {createRequire} from 'node:module';
import assert from 'node:assert/strict';
import {pixelMaterial} from './giant-tabby-pixel-material.mjs';
const require=createRequire(import.meta.url);
const {PNG}=require('C:/Users/16611/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/pngjs');
const root=path.resolve(import.meta.dirname,'..'),out=path.join(root,'art/giant-tabby-boss-v2');
const input=path.join(out,'source');
const source=JSON.parse(fs.readFileSync(path.join(input,'vanilla_yellow_cat.bbmodel'),'utf8'));
const S=4,up=.1;
const uid=name=>{const h=createHash('sha256').update('giant-tabby-v2/'+name).digest('hex');return `${h.slice(0,8)}-${h.slice(8,12)}-4${h.slice(13,16)}-a${h.slice(17,20)}-${h.slice(20,32)}`};
const round=n=>Math.round(n*1e6)/1e6;
const point=p=>p.map((n,i)=>round((n+(i===1?up:0))*S));
const groups=source.groups.map(g=>({name:g.name,uuid:g.uuid,origin:point(g.origin),rotation:g.rotation||[0,0,0],export:true,visibility:true,autouv:0}));
const elements=source.elements.map(e=>({...e,from:point(e.from),to:point(e.to),origin:point(e.origin),box_uv:false,autouv:0,
 faces:Object.fromEntries(Object.entries(e.faces).map(([name,f])=>[name,{uv:[...f.uv],texture:0}]))}));
// Vanilla already contains the projecting muzzle. Emphasize its depth only; keep 3:2 face size.
elements.find(e=>e.name==='nose').from[2]=-13.5*S;
const rootId=uid('root');
groups.unshift({name:'root',uuid:rootId,origin:[0,0,0],rotation:[0,0,0],export:true,visibility:true,autouv:0});
const outliner=[{uuid:rootId,isOpen:true,children:structuredClone(source.outliner)}];
const groupByName=new Map(groups.map(g=>[g.name,g]));
const nodeById=new Map(outliner[0].children.map(g=>[g.uuid,g]));
const solidFaces=uv=>Object.fromEntries(['north','east','south','west','up','down'].map(f=>[f,{uv:[...uv],texture:0}]));
function addCube(name,owner,from,to,uv){
 const g=groupByName.get(owner),e={name,uuid:uid(name),type:'cube',box_uv:false,autouv:0,from:point(from),to:point(to),origin:[...g.origin],rotation:[0,0,0],export:true,visibility:true,faces:solidFaces(uv)};
 elements.push(e);nodeById.get(g.uuid).children.push(e.uuid);return e;
}
// Inverse of vanilla body's -90-degree X rest rotation. World bounds before scaling:
// x +/-1.3, y 3..8, z -7.5..-3.5. One small front-belly piece, not another broad torso.
const chest=addCube('front_belly','body',[-1.3,5.5,-19],[1.3,9.5,-14],[50.4,10.4,61.6,19.6]);
// A single thin reference collar, largely inside the rear of the head. No separate frame/armor.
const collar=addCube('collar','head',[-2.55,6.95,-7.4],[2.55,11.05,-6.9],[50.4,2.4,61.6,5.6]);
const {png,texture,layout}=pixelMaterial(elements,source,fs.readFileSync(path.join(input,'yellow_cat.png')));

const animations=[];
function animation(name,length,loop='once'){
 const a={uuid:uid('animation/'+name),name:'animation.giant_tabby.'+name,loop,length,snapping:30,override:false,animators:{}};
 animations.push(a);return a;
}
function track(a,name,channel,frames){
 const g=groupByName.get(name);assert(g,name);
 const animator=a.animators[g.uuid]||={name,type:'bone',rotation_global:false,keyframes:[]};
 for(const [time,value]of frames)animator.keyframes.push({uuid:uid(`${a.name}/${name}/${channel}/${time}`),channel,time:round(time),color:-1,interpolation:'linear',data_points:[Object.fromEntries(['x','y','z'].map((k,i)=>[k,String(round(value[i]))]))]});
}
function sampled(a,name,channel,fn){
 const n=Math.round(a.length*30);
 track(a,name,channel,Array.from({length:n+1},(_,i)=>[a.length*i/n,fn(i/n)]));
}
const tau=2*Math.PI;
const idle=animation('idle',4,'loop');
sampled(idle,'body','scale',p=>[1+.004*Math.sin(p*tau),1,1+.004*Math.sin(p*tau)]);
sampled(idle,'head','rotation',p=>[.8*Math.sin(p*tau),1.8*Math.sin(p*tau),0]);
for(const fast of [false,true]){
 const a=animation(fast?'run':'walk',fast?.8:1.6,'loop');
 for(const front of [false,true])for(const left of [false,true]){
  const name=`${left?'left':'right'}_${front?'front':'hind'}_leg`;
  sampled(a,name,'rotation',p=>[(fast?38:24)*Math.sin(tau*p+(front===left?0:Math.PI)),0,0]);
 }
 sampled(a,'head','rotation',p=>[(fast?2:1)*Math.sin(tau*p*2),0,0]);
}
const bite=animation('bite',1.2);
track(bite,'head','rotation',[[0,[0,0,0]],[.3,[14,0,0]],[.52,[-18,0,0]],[.75,[-7,0,0]],[1.2,[0,0,0]]]);
track(bite,'head','position',[[0,[0,0,0]],[.3,[0,0,1]],[.52,[0,0,-2]],[1.2,[0,0,0]]]);
const swipe=animation('paw_swipe',1.5);
track(swipe,'left_front_leg','rotation',[[0,[0,0,0]],[.4,[65,-12,15]],[.65,[45,25,-25]],[.85,[-15,10,-8]],[1.5,[0,0,0]]]);
track(swipe,'head','rotation',[[0,[0,0,0]],[.4,[0,-8,0]],[.8,[0,8,0]],[1.5,[0,0,0]]]);
const leap=animation('pounce',1.8);
track(leap,'root','position',[[0,[0,0,0]],[.4,[0,-2,0]],[.85,[0,12,0]],[1.1,[0,10,0]],[1.4,[0,-2,0]],[1.8,[0,0,0]]]);
for(const front of [false,true])for(const left of [false,true])track(leap,`${left?'left':'right'}_${front?'front':'hind'}_leg`,'rotation',[[0,[0,0,0]],[.4,[front?-20:25,0,0]],[.85,[front?65:-55,0,0]],[1.35,[front?20:-15,0,0]],[1.8,[0,0,0]]]);
const hurt=animation('hurt',.6);
track(hurt,'head','rotation',[[0,[0,0,0]],[.12,[8,9,-4]],[.35,[-3,-3,1]],[.6,[0,0,0]]]);
const death=animation('death',2.4,'hold');
track(death,'root','rotation',[[0,[0,0,0]],[.4,[0,0,6]],[1.3,[0,0,90]],[1.6,[0,0,86]],[2,[0,0,90]],[2.4,[0,0,90]]]);

const m={meta:{format_version:'5.0',model_format:'free',box_uv:false},name:'大猫猫 · 原版像素 16px每格',model_identifier:'giant_tabby_boss',geometry_name:'giant_tabby_boss',resolution:{width:png.width,height:png.height},elements,groups,outliner,animations,textures:[{name:'giant_tabby_boss.png',id:'0',uuid:uid('material'),path:'',relative_path:'giant_tabby_boss.png',width:png.width,height:png.height,uv_width:png.width,uv_height:png.height,render_mode:'default',render_sides:'auto',visible:true,internal:true,source:'data:image/png;base64,'+texture.toString('base64')}],animation_variable_placeholders:''};
fs.mkdirSync(out,{recursive:true});
fs.writeFileSync(path.join(out,'giant_tabby_boss.bbmodel'),JSON.stringify(m)+'\n');
fs.writeFileSync(path.join(out,'giant_tabby_boss.png'),texture);
fs.writeFileSync(path.join(out,'uv-density.json'),JSON.stringify({texels_per_block:16,faces:layout},null,2)+'\n');
fs.writeFileSync(path.join(out,'manifest.json'),JSON.stringify({schema:3,kind:'standalone-boss-art-not-gameplay',id:'giant_tabby_boss',scale:S,units_per_block:16,neutral_ear_height_blocks:(12+up)*S/16,body_width_blocks:4*S/16,source_model:'source/vanilla_yellow_cat.bbmodel',texture:{width:png.width,height:png.height,texels_per_block:16,base_uv:[64,32],source:'source/yellow_cat.png',filtering:'nearest',detail:'vanilla palette with sparse discrete 1-2 texel colour clusters; no fur, gradients or subtexel detail'},counts:{cubes:elements.length,bones:groups.length,animations:animations.length},notes:['Base head/body/limbs retain the supplied vanilla proportions.','One modest front belly and a projecting vanilla muzzle.','Standalone art only; no runtime entity or JAR changes.','Model is already 4x vanilla size; additional runtime scale changes texel density.']},null,2)+'\n');
console.log(JSON.stringify({output:out,cubes:elements.length,bones:groups.length,animations:animations.length,texture:[png.width,png.height]}));
