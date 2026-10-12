import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import {createHash} from 'node:crypto';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
const Zip=require('C:/Users/16611/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/jszip');
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const out=path.join(root,'art/maodie-fighter-v2');
const original=JSON.parse(readFileSync(path.join(root,'forge-1.20.1/src/main/resources/assets/laowu/models/entity/butter_cat.bbmodel'),'utf8'));
// The old bbmodel calls its embedded tuxedo atlas "yellow_cat". Use the actual
// vanilla orange cat atlas for the user's golden boxer, without painting it.
const vanilla=await Zip.loadAsync(readFileSync('C:/Users/16611/.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar'));
original.textures[0].source='data:image/png;base64,'+await vanilla.file('assets/minecraft/textures/entity/cat/red.png').async('base64');
const id=s=>{const h=createHash('sha256').update('maodie-fighter-v2/'+s).digest('hex');return h.slice(0,8)+'-'+h.slice(8,12)+'-4'+h.slice(13,16)+'-a'+h.slice(17,20)+'-'+h.slice(20,32)};
const groups=[],elements=[],parents=new Map(),children=new Map(),named=new Map();
function bone(name,origin,rotation=[0,0,0],parent=null){
 const b={name,uuid:id(name),origin,rotation,export:true,visibility:true,autouv:0,shade:true,mirror_uv:false,color:groups.length%8};
 groups.push(b);named.set(name,b);parents.set(name,parent);children.set(name,[]);
 if(parent)children.get(parent).push({bone:name});
 return b;
}
const stock=name=>structuredClone(original.elements.find(e=>e.name===name).faces);
function cube(name,b,from,to,faces){
 const e={name,uuid:id(name),type:'cube',box_uv:false,autouv:0,from,to,origin:[0,0,0],rotation:[0,0,0],export:true,visibility:true,shade:true,render_order:'default',faces};
 elements.push(e);children.get(b).push(e.uuid);return e;
}
bone('root',[0,0,0]);
bone('hips',[0,8,1],[0,0,0],'root');
bone('lower_spine',[0,8,1],[-8,0,0],'hips');
bone('upper_spine',[0,16,1],[-18,0,0],'lower_spine');
bone('head',[0,24,-.5],[15,0,0],'upper_spine');
const lower=stock('body_cube'),upper=stock('body_cube');
for(const dir of ['north','south','east','west']){lower[dir].uv[1]=14;upper[dir].uv[3]=14;}
cube('lower_spine_body','lower_spine',[-2,8,-2],[2,16,4],lower);
cube('upper_spine_body','upper_spine',[-2,15.6,-2],[2,23.6,4],upper);
cube('cat_head','head',[-2.5,22,-3.5],[2.5,26,1.5],stock('head_main'));
const muzzle=stock('nose');
// Vanilla hides the unpainted rear cap inside the head; close it for the articulated preview.
muzzle.south={...muzzle.south,uv:[6,5,9,7]};
cube('cat_muzzle','head',[-1.5,22.001,-4.5],[1.5,24.001,-2.5],muzzle);
cube('left_ear','head',[-2,26,-.5],[-1,27,1.5],stock('left_ear'));
cube('right_ear','head',[1,26,-.5],[2,27,1.5],stock('right_ear'));
function limbFaces(top,bottom){
 const f=stock('left_hind_leg_cube');
 for(const d of ['north','south','east','west']){f[d].uv[1]=top;f[d].uv[3]=bottom;}
 return f;
}
const paw=limbFaces(19,21);
paw.up.uv=[12,21,10,19];paw.down.uv=[14,19,12,21];
for(const side of ['left','right']){
 const x=side==='left'?3:-3,s=side==='left'?1:-1;
 bone(side+'_arm',[x,22,1],[25,0,-8*s],'upper_spine');
 bone(side+'_elbow',[x,18,1],[125,0,0],side+'_arm');
 bone(side+'_wrist',[x,14,1],[0,0,0],side+'_elbow');
 cube(side+'_upper_arm',side+'_arm',[x-1,18,0],[x+1,22,2],limbFaces(15,19));
 cube(side+'_forearm',side+'_elbow',[x-1,14,0],[x+1,18,2],limbFaces(15,19));
 cube(side+'_fist',side+'_wrist',[x-1.4,11.7,-.4],[x+1.4,14.5,2.4],structuredClone(paw));
 const lx=side==='left'?1.5:-1.5,turn=side==='left'?5:-4;
 bone(side+'_thigh',[lx,8,1],[turn,0,0],'hips');
 bone(side+'_knee',[lx,4,1],[0,0,0],side+'_thigh');
 bone(side+'_ankle',[lx,1,1],[-turn,0,0],side+'_knee');
 cube(side+'_thigh_cube',side+'_thigh',[lx-1,4,0],[lx+1,8,2],limbFaces(15,19));
 cube(side+'_shin',side+'_knee',[lx-1,1,0],[lx+1,4,2],limbFaces(18,21));
 cube(side+'_hind_paw',side+'_ankle',[lx-1,0,-.5],[lx+1,1,2.5],structuredClone(paw));
}
for(let n=0;n<3;n++){
 const name=['tail_base','tail_middle','tail_tip'][n],parent=n?['tail_base','tail_middle'][n-1]:'hips',z=4+n*4;
 bone(name,[0,9,z],[n===0?8:n===1?-16:-12,0,0],parent);
 // Stock vertical tail strips are rotated per horizontal face; caps never sample atlas gaps.
 const u=n===2?5:1,v=n===1?20:16;
 const faces={};
 for(const d of ['east','west','up','down'])faces[d]={uv:[u,v,u+1,v+4],texture:0,rotation:90};
 for(const d of ['north','south'])faces[d]={uv:[u,15,u+1,16],texture:0};
 cube(name+'_cube',name,[-.5,8.5,z],[.5,9.5,z+4],faces);
}
function rotate(p,degrees){
 let [x,y,z]=p;const [rx,ry,rz]=degrees.map(v=>v*Math.PI/180);
 [y,z]=[y*Math.cos(rx)-z*Math.sin(rx),y*Math.sin(rx)+z*Math.cos(rx)];
 [x,z]=[x*Math.cos(ry)+z*Math.sin(ry),-x*Math.sin(ry)+z*Math.cos(ry)];
 [x,y]=[x*Math.cos(rz)-y*Math.sin(rz),x*Math.sin(rz)+y*Math.cos(rz)];
 return [x,y,z];
}
function point(p,name,pose){
 for(let b=name;b;b=parents.get(b)){
  const g=named.get(b),q=pose[b],r=g.rotation.map((v,i)=>v+q.rotation[i]);
  p=rotate(p.map((v,i)=>v-g.origin[i]),r).map((v,i)=>v+g.origin[i]+q.position[i]);
 }
 return p;
}
const smooth=x=>{x=Math.max(0,Math.min(1,x));return x*x*(3-2*x)};
const pulse=(t,a,b,c)=>t<b?smooth((t-a)/(b-a)):1-smooth((t-b)/(c-b));
function applyPunch(p,side,weight){
 p[side+'_arm'].rotation[0]+=65*weight;
 p[side+'_elbow'].rotation[0]-=113*weight;
 p[side+'_wrist'].rotation[0]+=7*weight;
 p.upper_spine.rotation[1]+=(side==='left'?-23:23)*weight;
 p.head.rotation[1]+=(side==='left'?15:-15)*weight;
}
const clips=[
 {name:'guard',label:'护脸站姿',length:1.2,loop:'loop',peak:0,pose(p,t){
  const w=Math.sin(t/1.2*Math.PI*2);p.upper_spine.rotation[0]=w*1.5;p.head.rotation[0]=-w*1;
  p.tail_base.rotation[1]=Math.sin(t*5)*8;p.tail_tip.rotation[1]=Math.sin(t*5-1)*12;
 }},
 {name:'jab',label:'普通挥拳',length:.28,peak:.07,pose(p,t){
  const w=pulse(t,0,.07,.22);applyPunch(p,'left',w);p.root.position[2]=-2*w;p.lower_spine.rotation[0]=-3*w;
 }},
 {name:'uppercut',label:'上勾拳',length:.36,peak:.145,pose(p,t){
  const dip=pulse(t,0,.055,.17),hit=pulse(t,.055,.145,.29);
  p.left_thigh.rotation[0]=-23*dip;p.right_thigh.rotation[0]=-23*dip;
  p.left_knee.rotation[0]=p.right_knee.rotation[0]=48*dip;
  p.left_ankle.rotation[0]=p.right_ankle.rotation[0]=-25*dip;
  p.lower_spine.rotation[0]=-16*dip+16*hit;p.upper_spine.rotation[1]=-27*dip+35*hit;
  p.right_arm.rotation[0]=-20*dip+100*hit;p.right_elbow.rotation[0]=-70*hit-45*dip;
  p.right_arm.rotation[2]=-20*hit;p.head.rotation[0]=8*dip-12*hit;p.root.position[2]=-3*hit;
 }},
 {name:'combo',label:'连续出拳',length:.68,peak:.27,pose(p,t){
  for(let n=0;n<8;n++){
   const peak=.055+n*.07,w=pulse(t,peak-.04,peak,peak+.043);applyPunch(p,n%2?'right':'left',w);
   p.root.position[2]-=1.8*w;p.lower_spine.rotation[0]-=3*w;
  }
  p.tail_base.rotation[1]=Math.sin(t*45)*12;
 }},
 {name:'block',label:'格挡',length:.32,peak:.1,pose(p,t){
  const w=pulse(t,0,.065,.28),impact=pulse(t,.07,.105,.19);
  for(const side of ['left','right']){p[side+'_arm'].rotation[0]=12*w;p[side+'_elbow'].rotation[0]=15*w;}
  p.left_arm.rotation[2]=12*w;p.right_arm.rotation[2]=-12*w;
  p.head.rotation[0]=-10*w;p.upper_spine.rotation[0]=-9*w+10*impact;p.root.position[2]=1.2*impact;
 }},
 {name:'backstep',label:'后撤',length:.34,peak:.12,pose(p,t){
  const w=pulse(t,0,.1,.28),lift=pulse(t,.05,.13,.22);
  p.root.position[2]=10*smooth(t/.16);p.lower_spine.rotation[0]=-7*w;
  p.left_thigh.rotation[0]=-34*w;p.right_thigh.rotation[0]=23*w;
  p.left_knee.rotation[0]=30*lift;p.right_knee.rotation[0]=25*lift;
  p.left_ankle.rotation[0]=34*w-30*lift;p.right_ankle.rotation[0]=-23*w-25*lift;
 }},
 {name:'roundhouse',label:'回旋踢',length:.54,peak:.265,pose(p,t){
  const wind=pulse(t,0,.09,.2),kick=pulse(t,.1,.255,.39);
  p.root.rotation[1]=t<.09?-38*smooth(t/.09):t<.34?-38+398*smooth((t-.09)/.25):360;
  p.right_thigh.rotation[2]=-78*kick;p.right_thigh.rotation[0]=-45*kick;
  p.right_knee.rotation[0]=35*wind-18*kick;p.right_ankle.rotation[0]=-p.right_knee.rotation[0];
  p.upper_spine.rotation[0]=9*kick;p.upper_spine.rotation[2]=15*kick;
  p.left_arm.rotation[0]=25*kick;p.right_arm.rotation[0]=5*kick;
  p.tail_base.rotation[1]=-35*kick;p.tail_middle.rotation[1]=-25*kick;
 }},
 {name:'jump_slam',label:'腾空下砸',length:.72,peak:.535,pose(p,t){
  const prep=pulse(t,0,.075,.17),flight=pulse(t,.11,.285,.53),impact=pulse(t,.49,.545,.68);
  p.root.position[2]=-13*smooth((t-.12)/.4);
  p.left_thigh.rotation[0]=p.right_thigh.rotation[0]=-28*prep+30*flight-33*impact;
  p.left_knee.rotation[0]=p.right_knee.rotation[0]=55*prep+45*flight+65*impact;
  p.left_ankle.rotation[0]=p.right_ankle.rotation[0]=-27*prep-75*flight-32*impact;
  p.lower_spine.rotation[0]=-12*prep-24*impact;p.upper_spine.rotation[0]=30*flight-32*impact;
  for(const side of ['left','right']){p[side+'_arm'].rotation[0]=140*flight+110*impact;p[side+'_elbow'].rotation[0]=-115*flight-100*impact;}
  p.head.rotation[0]=-15*flight+15*impact;
  return 15*flight;
 }}
];
const animations=[];
const outliner=name=>({uuid:named.get(name).uuid,isOpen:true,children:children.get(name).map(v=>typeof v==='string'?v:outliner(v.bone))});
for(const clip of clips){
 const frames=Math.ceil(clip.length*60),animators={};
 for(const g of groups)animators[g.uuid]={name:g.name,type:'bone',rotation_global:false,quaternion_interpolation:false,keyframes:[]};
 for(let i=0;i<=frames;i++){
  const t=clip.length*i/frames;
  const p=Object.fromEntries(groups.map(g=>[g.name,{rotation:[0,0,0],position:[0,0,0]}]));
  const lift=clip.pose(p,t)||0;
  let lowest=Infinity;
  for(const side of ['left','right']){
   const e=elements.find(e=>e.name===side+'_hind_paw');
   for(const x of [e.from[0],e.to[0]])for(const y of [e.from[1],e.to[1]])for(const z of [e.from[2],e.to[2]])lowest=Math.min(lowest,point([x,y,z],side+'_ankle',p)[1]);
  }
  // Sub-keyframe linear interpolation can lower a rotating sole by <0.06 pixels.
  p.root.position[1]=-lowest+lift+.07;
  for(const g of groups)for(const channel of ['rotation','position']){
   const values=p[g.name][channel].map(v=>Math.round(v*1e5)/1e5);
   animators[g.uuid].keyframes.push({channel,data_points:[{x:String(values[0]),y:String(values[1]),z:String(values[2])}],uuid:id(clip.name+'/'+g.name+'/'+channel+'/'+i),time:Math.round(t*1e6)/1e6,color:-1,interpolation:'linear'});
  }
 }
 animations.push({uuid:id('clip/'+clip.name),name:'animation.maodie.'+clip.name,loop:clip.loop||'once',length:clip.length,snapping:60,override:false,anim_time_update:'',blend_weight:'',start_delay:'',loop_delay:'',animators});
}
const model={meta:{format_version:'5.0',model_format:'free',box_uv:false},name:'耄耋格斗手 · 动作预览 v2',model_identifier:'maodie_fighter_v2',resolution:{width:64,height:32},elements,groups,outliner:[outliner('root')],textures:[{...original.textures[0],name:'original_yellow_cat.png',id:'0',uuid:id('texture'),path:'',relative_path:'original_yellow_cat.png',folder:'',namespace:'',internal:true,saved:true}],animations};
mkdirSync(out,{recursive:true});
writeFileSync(path.join(out,'maodie_fighter_v2.bbmodel'),JSON.stringify(model)+'\n');
writeFileSync(path.join(out,'original_yellow_cat.png'),Buffer.from(model.textures[0].source.split(',')[1],'base64'));
writeFileSync(path.join(out,'clips.json'),JSON.stringify(clips.map(({name,label,length,peak})=>({name,label,length,peak})),null,2)+'\n');
console.log('Authored Blockbench preview:',elements.length,'closed cubes,',groups.length,'bones,',animations.length,'clips');
