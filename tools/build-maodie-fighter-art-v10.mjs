import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import {createHash} from 'node:crypto';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
const Zip=require('C:/Users/16611/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/jszip');
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const out=path.join(root,'art/maodie-fighter-v10');
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
bone('hips',[0,5,1],[0,0,0],'root');
bone('lower_spine',[0,5,1],[0,0,0],'hips');
bone('upper_spine',[0,12,1],[0,0,0],'lower_spine');
bone('head',[0,15.4,-1.6],[0,0,0],'upper_spine');
// One watertight surface, with shared vertices along the entire curved waist.
// No overlapping cube caps, so leaning cannot open a white seam at the waist.
const torso={type:'mesh',uuid:id('continuous_curved_torso'),name:'continuous_curved_torso',origin:[0,0,0],rotation:[0,0,0],vertices:{},faces:{},visibility:true,export:true,shading:true};
const rings=12,bodyFaces=stock('body_cube');
for(let r=0;r<=rings;r++){
 const t=r/rings,y=2.6+12.6*t,w=2,d=3,z=1-2.6*t*t;
 // Original 4×6 rectangular section: only the centerline bends, no bevels,
 // taper or rounded barrel. Each original UV side is used exactly once.
 const section=[[-w,-d],[w,-d],[w,d],[-w,d]];
 section.forEach(([x,dz],i)=>torso.vertices[`r${r}_${i}`]=[x,y,z+dz]);
}
const sideUV=['north','east','south','west'];
for(let r=0;r<rings;r++)for(let s=0;s<4;s++){
 const n=(s+1)%4,keys=[`r${r}_${n}`,`r${r}_${s}`,`r${r+1}_${s}`,`r${r+1}_${n}`],u=bodyFaces[sideUV[s]].uv;
 const v0=u[3]-(u[3]-u[1])*r/rings,v1=u[3]-(u[3]-u[1])*(r+1)/rings;
 const x0=u[0],x1=u[2];
 torso.faces[`side_${r}_${s}`]={vertices:keys,texture:0,uv:{[keys[0]]:[x0,v0],[keys[1]]:[x1,v0],[keys[2]]:[x1,v1],[keys[3]]:[x0,v1]}};
}
for(const r of [0,rings]){
 const [u0,v0,u1,v1]=bodyFaces[r?'up':'down'].uv;
 const keys=[0,1,2,3].map(i=>`r${r}_${i}`);
 const uv=r
  ?{ [keys[0]]:[u0,v0], [keys[1]]:[u1,v0], [keys[2]]:[u1,v1], [keys[3]]:[u0,v1] }
  :{ [keys[0]]:[u0,v1], [keys[1]]:[u1,v1], [keys[2]]:[u1,v0], [keys[3]]:[u0,v0] };
 if(!r)keys.reverse();
 torso.faces[`cap_${r}`]={vertices:keys,texture:0,uv};
}
elements.push(torso);children.get('lower_spine').push(torso.uuid);
cube('cat_head','head',[-2.5,13.4,-4.1],[2.5,17.4,.9],stock('head_main'));
const muzzle=stock('nose');
// Vanilla hides the unpainted rear cap inside the head; close it for the articulated preview.
muzzle.south={...muzzle.south,uv:[6,5,9,7]};
cube('cat_muzzle','head',[-1.5,13.401,-5.1],[1.5,15.401,-3.1],muzzle);
cube('left_ear','head',[-2,17.4,-1.1],[-1,18.4,.9],stock('left_ear'));
cube('right_ear','head',[1,17.4,-1.1],[2,18.4,.9],stock('right_ear'));
function limbFaces(top,bottom){
 const f=stock('left_hind_leg_cube');
 for(const d of ['north','south','east','west']){f[d].uv[1]=top;f[d].uv[3]=bottom;}
 return f;
}
const paw=limbFaces(19,21);
paw.up.uv=[12,21,10,19];paw.down.uv=[14,19,12,21];
for(const side of ['left','right']){
 const x=side==='left'?2.8:-2.8,s=side==='left'?1:-1;
 bone(side+'_arm',[x,11.8,-.9],[22,0,-8*s],'upper_spine');
 bone(side+'_elbow',[x,8.6,-.9],[115,0,0],side+'_arm');
 bone(side+'_wrist',[x,5.4,-.9],[0,0,0],side+'_elbow');
 cube(side+'_upper_arm',side+'_arm',[x-1,8.6,-1.9],[x+1,11.8,.1],limbFaces(15,19));
 cube(side+'_forearm',side+'_elbow',[x-1,5.4,-1.9],[x+1,8.6,.1],limbFaces(15,19));
 cube(side+'_fist',side+'_wrist',[x-1.3,3.1,-2.2],[x+1.3,5.7,.4],structuredClone(paw));
 const lx=side==='left'?1.5:-1.5,turn=side==='left'?5:-4;
 bone(side+'_thigh',[lx,5,1],[turn,0,0],'hips');
 bone(side+'_knee',[lx,2.5,1],[0,0,0],side+'_thigh');
 bone(side+'_ankle',[lx,0,1],[-turn,0,0],side+'_knee');
 cube(side+'_thigh_cube',side+'_thigh',[lx-1,2.5,0],[lx+1,5,2],limbFaces(15,19));
 cube(side+'_shin',side+'_knee',[lx-1,0,0],[lx+1,2.5,2],structuredClone(paw));
}
for(let n=0;n<3;n++){
 const name=['tail_base','tail_middle','tail_tip'][n],parent=n?['tail_base','tail_middle'][n-1]:'hips',z=3.2+n*4;
 bone(name,[0,5,z],[n===0?8:n===1?-16:-12,0,0],parent);
 // Stock vertical tail strips are rotated per horizontal face; caps never sample atlas gaps.
 const u=n===2?5:1,v=n===1?20:16;
 const faces={};
 for(const d of ['east','west','up','down'])faces[d]={uv:[u,v,u+1,v+4],texture:0,rotation:90};
 for(const d of ['north','south'])faces[d]={uv:[u,15,u+1,16],texture:0};
 cube(name+'_cube',name,[-.5,4.5,z],[.5,5.5,z+4],faces);
}
// Lengthen both leg segments from 2.5 to 3.5px. Raise the existing upper
// rig as one unit, preserving compact torso length, UVs and low shoulders.
for(const g of groups){
 if(g.name==='root'||g.name.endsWith('_ankle'))continue;
 g.origin[1]+=g.name.endsWith('_knee')?1:2;
 if(g.name==='head'){g.origin[1]+=1;g.origin[2]-=1;}
}
for(const e of elements){
 if(e.type==='mesh'){for(const v of Object.values(e.vertices))v[1]+=2;continue;}
 if(e.name.endsWith('_thigh_cube')){e.from[1]+=1;e.to[1]+=2;continue;}
 if(e.name.endsWith('_shin')){e.to[1]+=1;continue;}
 e.from[1]+=2;e.to[1]+=2;
 if(['cat_head','cat_muzzle','left_ear','right_ear'].includes(e.name)){
  e.from[1]+=1;e.to[1]+=1;e.from[2]-=1;e.to[2]-=1;
 }
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
  p=rotate(p.map((v,i)=>(v-g.origin[i])*q.scale[i]),r).map((v,i)=>v+g.origin[i]+q.position[i]);
 }
 return p;
}
const smooth=x=>{x=Math.max(0,Math.min(1,x));return x*x*(3-2*x)};
const pulse=(t,a,b,c)=>t<b?smooth((t-a)/(b-a)):1-smooth((t-b)/(c-b));
function applyPunch(p,side,weight,planted=false){
 const s=side==='left'?1:-1;
 p[side+'_arm'].rotation[0]+=(planted?63:83)*weight;
 // Cancel the inward rest roll during extension so the shoulder/forearm
 // plane faces actor -Z. Power comes from pitch and forward lunge, not yaw.
 p[side+'_arm'].rotation[2]+=s*8*weight;
 p[side+'_elbow'].rotation[0]-=108*weight;
 p[side+'_wrist'].rotation[0]+=5*weight;
 p[side+'_arm'].scale[1]+= .28*weight;
 p[side+'_elbow'].scale[1]+= .24*weight;
 p[side+'_wrist'].scale=p[side+'_wrist'].scale.map(v=>v+.18*weight);
 if(!planted)p.upper_spine.rotation[0]-=8*weight;
}
const snap=(t,start,impact,hold,end)=>t<impact?smooth((t-start)/(impact-start)):t<=hold?1:1-smooth((t-hold)/(end-hold));
// Continuous cubic tangents carry momentum through impact, unlike isolated
// eased pulses which stop/reverse at every punch. These are authored poses.
function comboCurve(keys,t){
 keys.sort((a,b)=>a[0]-b[0]);
 if(t<=keys[0][0])return [...keys[0][1]];
 if(t>=keys.at(-1)[0])return [...keys.at(-1)[1]];
 const j=keys.findIndex(k=>k[0]>t),a=keys[j-1],b=keys[j],prev=keys[Math.max(0,j-2)],next=keys[Math.min(keys.length-1,j+1)];
 const dt=b[0]-a[0],u=(t-a[0])/dt;
 return a[1].map((v,n)=>{
  const va=j===1?0:(b[1][n]-prev[1][n])/(b[0]-prev[0]);
  const vb=j===keys.length-1?0:(next[1][n]-a[1][n])/(next[0]-a[0]);
  return (2*u**3-3*u*u+1)*v+(u**3-2*u*u+u)*dt*va+(-2*u**3+3*u*u)*b[1][n]+(u**3-u*u)*dt*vb;
 });
}
const dot=(a,b)=>a.reduce((s,v,i)=>s+v*b[i],0);
const unit=a=>{const d=Math.hypot(...a);return a.map(v=>v/d)};
const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
function inversePoint(v,name,p){
 const chain=[];for(let b=name;b;b=parents.get(b))chain.unshift(b);
 for(const b of chain){const g=named.get(b),q=p[b];v=v.map((n,i)=>n-g.origin[i]-q.position[i]);
  for(let axis=2;axis>=0;axis--){const r=[0,0,0];r[axis]=-g.rotation[axis]-q.rotation[axis];v=rotate(v,r)}
  v=v.map((n,i)=>n/q.scale[i]+g.origin[i]);
 }return v;
}
function aimComboArm(p,side,target,poleDown){
 const shoulder=named.get(side+'_arm'),local=inversePoint(target,'upper_spine',p).map((v,i)=>v-shoulder.origin[i]);
 const length=Math.hypot(...local),stretch=Math.max(1,Math.min(1.85,length/6.25));
 const direction=unit(local),pole=[side==='left'?1:-1,poleDown,.12];
 const bend=unit(pole.map((v,i)=>v-direction[i]*dot(pole,direction)));
 const cos=Math.min(.999,length/(6.4*stretch)),sin=Math.sqrt(1-cos*cos);
 const upper=direction.map((v,i)=>v*cos+bend[i]*sin),lower=direction.map((v,i)=>v*cos-bend[i]*sin);
 const y=upper.map(v=>-v),z=unit(lower.map((v,i)=>-v+y[i]*dot(lower,y))),x=cross(y,z);
 // Bone-local -Y follows the upper arm; +X is the elbow hinge axis.
 const angles=[Math.atan2(y[2],z[2]),Math.asin(Math.max(-1,Math.min(1,-x[2]))),Math.atan2(x[1],x[0])].map(v=>v*180/Math.PI);
 p[side+'_arm'].rotation=angles.map((v,i)=>v-shoulder.rotation[i]);
 p[side+'_arm'].scale=[stretch,stretch,stretch];
 p[side+'_elbow'].rotation=[Math.acos(Math.max(-1,Math.min(1,dot(upper,lower))))*180/Math.PI-115,0,0];
 p[side+'_wrist'].scale=[1/stretch,1/stretch,1/stretch];
}
const comboStrikes=[
 {time:.18,side:'left',label:'左直拳',wind:[5.4,15.2,1.1],arc:[4.7,15.5,-4.0],hit:[1.4,15.8,-11.2],follow:[.6,15.3,-11.7],pole:-.8},
 {time:.43,side:'right',label:'右直拳',wind:[-5.6,14.8,1.4],arc:[-4.6,15.3,-4.1],hit:[-.5,15.8,-11.5],follow:[.9,15.1,-11.9],pole:-.8},
 {time:.72,side:'left',label:'左横勾拳',wind:[6.5,15.6,1.4],arc:[7.6,15.5,-4.8],hit:[-.8,15.3,-10.1],follow:[-3.4,14.6,-8.9],pole:-.25},
 {time:.99,side:'right',label:'右腹部勾拳',wind:[-6.4,12.7,1.5],arc:[-7.2,11.8,-4.4],hit:[.8,11.8,-9.9],follow:[3.4,12.3,-8.5],pole:-.4},
 {time:1.27,side:'left',label:'左上挑拳',wind:[5.8,12.6,1.3],arc:[5.6,11.8,-4.5],hit:[.2,16.4,-10.1],follow:[-1.6,17.6,-8.9],pole:-.7},
 {time:1.55,side:'right',label:'右重拳收尾',wind:[-6.2,16.1,1.5],arc:[-6.9,17.9,-4.3],hit:[-.6,15.8,-11.1],follow:[1.4,13.9,-10.0],pole:-.45}
];
function supportComboLegs(p){
 // Two 3.5px links solve toward the original planted ankle targets. The
 // pelvis can shift/yaw and the knees yield without dragging both soles.
 const rest=neutral(),hip=named.get('hips');
 for(const side of ['left','right']){
  const thigh=named.get(side+'_thigh'),ankle=named.get(side+'_ankle');
  const target=point([...ankle.origin],side+'_knee',rest);
  for(let solve=0;solve<5;solve++){
  const local=rotate(target.map((v,i)=>v-hip.origin[i]-p.hips.position[i]),[0,-p.hips.rotation[1],0]).map((v,i)=>v-(thigh.origin[i]-hip.origin[i]));
  const d=Math.min(7,Math.hypot(...local)),bend=Math.acos(d/7)*180/Math.PI;
  const pitch=Math.asin(Math.max(-1,Math.min(1,-local[2]/Math.hypot(...local))))*180/Math.PI;
  const roll=Math.atan2(local[0],-local[1])*180/Math.PI;
  p[side+'_thigh'].rotation=[pitch+bend-thigh.rotation[0],0,roll];
  p[side+'_knee'].rotation=[-2*bend,0,0];
  // A foot's cube corners, not merely its ankle joint, must contact the
  // ground. Correct each sole independently as its pivot angle changes.
  const shin=elements.find(e=>e.name===side+'_shin');let low=Infinity;
  for(const x of [shin.from[0],shin.to[0]])for(const y of [shin.from[1],shin.to[1]])for(const z of [shin.from[2],shin.to[2]])low=Math.min(low,point([x,y,z],side+'_knee',p)[1]);
  target[1]-=low;
  }
 }
}
const clips=[
 {name:'guard',label:'灵动架势',length:.8,loop:'loop',peak:0,pose(p,t){
  const w=Math.sin(t/.8*Math.PI*2),bounce=(1-Math.cos(t/.4*Math.PI*2))*.5;
  p.lower_spine.rotation=[-4*bounce,9*w,5*w];p.head.rotation=[3*bounce,-6*w,-3*w];
  p.left_arm.rotation[0]=5*w;p.right_arm.rotation[0]=-5*w;p.root.position[0]=.6*w;
  p.tail_base.rotation[1]=-18*w;p.tail_tip.rotation[1]=Math.sin(t*8-1)*16;
  return .22*bounce;
 }},
 {name:'jab',label:'爆发直拳',length:.24,peak:.075,pose(p,t){
  const wind=pulse(t,0,.035,.07),w=snap(t,.04,.075,.09,.17),recoil=pulse(t,.13,.185,.235);
  applyPunch(p,'left',w);p.left_arm.rotation[0]-=24*wind+10*recoil;
  p.upper_spine.rotation[1]+=23*wind+9*recoil;p.lower_spine.rotation[0]=-12*w+5*wind;
  p.root.position[2]=-5.5*w+.5*wind;
  p.tail_base.rotation[1]=32*w;p.tail_tip.rotation[1]=24*recoil;
 }},
 {name:'uppercut',label:'上勾拳',length:.34,peak:.14,pose(p,t){
  const dip=pulse(t,0,.075,.14),hit=snap(t,.10,.14,.165,.29);
  p.left_thigh.rotation[0]=p.right_thigh.rotation[0]=-32*dip;
  p.left_knee.rotation[0]=p.right_knee.rotation[0]=65*dip;
  p.lower_spine.scale=[1+.1*dip,1-.16*dip+.1*hit,1+.06*dip];
  p.lower_spine.rotation[0]=-32*dip-8*hit;p.upper_spine.rotation[1]=-45*dip+58*hit;
  p.right_arm.rotation[0]=-30*dip+130*hit;p.right_elbow.rotation[0]=-95*hit-45*dip;
  p.right_arm.rotation[2]=-16*hit;p.right_elbow.scale[1]+= .3*hit;
  p.head.rotation[0]=18*dip-16*hit;p.head.rotation[1]=-24*hit;
  p.root.position[2]=-4.5*hit;p.root.position[0]=2.2*(hit-dip);p.tail_base.rotation[1]=-45*hit;
  return 1.2*pulse(t,.10,.15,.25);
 }},
 {name:'combo',label:'爆发六连拳',length:1.3104,peak:.5184,pose(p,t){
  // Author readable windups, then compress the source timeline (not just playback).
  t/=.72;
  const body=[[0,[0,0,0]],[1.82,[0,0,0]]],head=[[0,[0,0,0]],[1.82,[0,0,0]]],hips=[[0,[0,0,0,0]],[1.82,[0,0,0,0]]],arm={};
  for(const side of ['left','right']){
   const s=side==='left'?1:-1,guard=[s*3.1,15,-4.8,-.8],rest=point([...named.get(side+'_wrist').origin],side+'_elbow',neutral());
   arm[side]=[[0,[...rest,-.8]],[.04,guard],[1.74,guard],[1.82,[...rest,-.8]]];
  }
  for(const strike of comboStrikes){
   const {time:hit,side,wind,arc,hit:target,follow,pole}=strike,s=side==='left'?1:-1;
   const guard=[s*3.1,15,-4.8,-.8];
   arm[side].push([hit-.13,guard],[hit-.070,[...wind,-.55]],[hit-.025,[...arc,pole]],[hit,[...target,pole]],[hit+.025,[...follow,pole]],[hit+.14,guard]);
   body.push([hit-.10,[4,-s*23,-s*11]],[hit-.005,[-13,s*24,s*11]],[hit+.045,[-10,s*31,s*15]]);
   // The head lags the shoulder reversal, then overshoots: no gaze-lock counter-rotation.
   head.push([hit-.085,[-9,-s*9,-s*10]],[hit+.018,[13,s*7,s*9]],[hit+.065,[7,s*12,s*13]]);
   hips.push([hit-.12,[-s*.28,-.27,.05,-s*7]],[hit-.015,[s*.32,-.43,-.1,s*8]],[hit+.04,[s*.38,-.3,-.1,s*10]]);
  }
  p.lower_spine.rotation=comboCurve(body,t);
  const h=comboCurve(hips,t);p.hips.position=h.slice(0,3);p.hips.rotation[1]=h[3];
  p.head.rotation=comboCurve(head,t);
  p.tail_base.rotation[1]=-comboCurve(hips,Math.max(0,t-.035))[3]*.7;
  const headShift=point([...named.get('head').origin],'head',p).map((v,i)=>v-named.get('head').origin[i]);
  for(const side of ['left','right']){
   const a=comboCurve(arm[side],t),strikes=comboStrikes.filter(s=>s.side===side);
   const active=Math.max(...strikes.map(s=>snap(t,s.time-.13,s.time-.070,s.time+.025,s.time+.14)));
   // Guard follows the moving face; the actual strike still aims at the opponent.
   const target=a.slice(0,3).map((v,i)=>v+headShift[i]*.55*(1-active));
   aimComboArm(p,side,target,a[3]);
   const impact=Math.max(...strikes.map(s=>pulse(t,s.time-.035,s.time+.005,s.time+.075)));
   p[side+'_wrist'].scale=p[side+'_wrist'].scale.map(v=>v*(1+.26*impact));
  }
  supportComboLegs(p);
 }},
 {name:'block',label:'急停格挡',length:.26,peak:.09,pose(p,t){
  const w=snap(t,0,.028,.12,.24),impact=pulse(t,.055,.09,.17);
  for(const side of ['left','right']){p[side+'_arm'].rotation[0]=32*w;p[side+'_elbow'].rotation[0]=20*w;}
  p.left_arm.rotation[2]=12*w;p.right_arm.rotation[2]=-12*w;
  p.head.rotation[0]=-15*w;p.upper_spine.rotation[0]=-16*w+27*impact;
  p.upper_spine.rotation[1]=-12*impact;p.lower_spine.scale[1]=1-.1*impact;
  p.root.position[2]=3.4*impact;p.tail_middle.rotation[0]=-25*impact;
 }},
 {name:'backstep',label:'闪身后撤',length:.28,peak:.105,pose(p,t){
  const w=pulse(t,0,.08,.25),lift=pulse(t,.035,.105,.19);
  p.root.position[2]=9*smooth(t/.11);p.root.position[0]=-3*pulse(t,0,.10,.27);
  p.lower_spine.rotation=[-14*w,24*w,-20*w];p.head.rotation[2]=12*w;
  p.left_thigh.rotation[0]=-44*w;p.right_thigh.rotation[0]=34*w;
  p.left_knee.rotation[0]=45*lift;p.right_knee.rotation[0]=38*lift;
  p.tail_base.rotation[1]=40*w;return 1.5*lift;
 }},
 {name:'roundhouse',label:'爆旋回旋踢',length:.46,peak:.20,pose(p,t){
  const wind=pulse(t,0,.065,.15),kick=snap(t,.085,.18,.225,.34);
  p.root.rotation[1]=t<.065?-65*smooth(t/.065):t<.28?-65+425*smooth((t-.065)/.215):360;
  p.root.position[2]=-3*kick;p.right_thigh.rotation[2]=-102*kick;p.right_thigh.rotation[0]=-52*kick;
  p.right_knee.rotation[0]=60*wind-4*kick;p.right_thigh.scale[1]=1+.55*kick;
  p.right_knee.scale[1]=1+.35*kick;p.upper_spine.rotation[0]=15*kick;p.upper_spine.rotation[2]=28*kick;
  p.left_arm.rotation[0]=60*kick;p.right_arm.rotation[0]=-35*kick;
  p.left_arm.rotation[2]=30*kick;p.right_arm.rotation[2]=-35*kick;
  p.tail_base.rotation[1]=-55*kick;p.tail_middle.rotation[1]=-35*kick;
 }},
 {name:'jump_slam',label:'腾空重砸',length:.62,peak:.40,pose(p,t){
  const prep=pulse(t,0,.07,.14),flight=t<.20?smooth((t-.085)/.115):t<.28?1:1-smooth((t-.28)/.12),impact=snap(t,.355,.40,.425,.57);
  p.root.position[2]=-11*smooth((t-.11)/.28);
  p.left_thigh.rotation[0]=p.right_thigh.rotation[0]=-35*prep+45*flight-40*impact;
  p.left_knee.rotation[0]=p.right_knee.rotation[0]=70*prep+90*flight+80*impact;
  p.lower_spine.scale=[1+.16*impact,1-.18*prep-.22*impact+.12*flight,1+.1*impact];
  p.lower_spine.rotation[0]=-18*prep-32*impact;p.upper_spine.rotation[0]=20*flight-20*impact;
  for(const side of ['left','right']){p[side+'_arm'].rotation[0]=145*flight+90*impact;p[side+'_elbow'].rotation[0]=-108*flight-100*impact;p[side+'_wrist'].scale=[1+.25*impact,1+.25*impact,1+.25*impact];}
  p.head.rotation[0]=-20*flight+22*impact;p.tail_base.rotation[0]=30*flight-35*impact;
  return 15*flight;
 }}
];
// A connected demonstration makes the reference's rhythm visible at real speed.
// Short bridges recover from root displacement instead of teleporting between clips.
const sequence=['jab','combo','block','backstep','uppercut','roundhouse','jump_slam'];
const bridge=.07,sequenceClips=sequence.map(name=>clips.find(c=>c.name===name));
const neutral=()=>Object.fromEntries(groups.map(g=>[g.name,{rotation:[0,0,0],position:[0,0,0],scale:[1,1,1]}]));
const flowLength=sequenceClips.reduce((sum,c)=>sum+c.length+bridge,0);
clips.push({name:'combat_flow',label:'连贯实战演示',length:flowLength,loop:'loop',peak:.50,pose(p,t){
 let start=0;
 for(let n=0;n<sequenceClips.length;n++){
  const c=sequenceClips[n],end=start+c.length;
  if(t<=end)return c.pose(p,Math.max(0,t-start));
  if(t<=end+bridge){
   const next=sequenceClips[(n+1)%sequenceClips.length],a=neutral(),b=neutral(),la=c.pose(a,c.length)||0,lb=next.pose(b,0)||0,w=smooth((t-end)/bridge);
   for(const g of groups)for(const channel of ['rotation','position','scale'])p[g.name][channel]=a[g.name][channel].map((v,i)=>{
    let delta=b[g.name][channel][i]-v;if(channel==='rotation')delta=((delta+180)%360+360)%360-180;
    return v+delta*w;
   });
   return la*(1-w)+lb*w;
  }
  start=end+bridge;
 }
 return sequenceClips[0].pose(p,0);
}});
const animations=[];
const outliner=name=>({uuid:named.get(name).uuid,isOpen:true,children:children.get(name).map(v=>typeof v==='string'?v:outliner(v.bone))});
for(const clip of clips){
 const sampling=['combo','combat_flow'].includes(clip.name)?240:120;
 const frames=Math.ceil(clip.length*sampling),animators={},lastRotation=new Map();
 const times=Array.from({length:frames+1},(_,i)=>clip.length*i/frames);
 if(['combo','combat_flow'].includes(clip.name)){
  const start=clip.name==='combo'?0:clips.find(c=>c.name==='jab').length+bridge;
  // Preserve authored hits exactly; a fast strike can land between regular samples.
  for(const s of comboStrikes)for(const offset of [-.13,-.07,-.025,0,.025,.14])times.push(start+(s.time+offset)*.72);
 }
 const sampleTimes=[...new Set(times.map(t=>Math.round(t*1e6)/1e6))].sort((a,b)=>a-b);
 for(const g of groups)animators[g.uuid]={name:g.name,type:'bone',rotation_global:false,quaternion_interpolation:false,keyframes:[]};
 for(let i=0;i<sampleTimes.length;i++){
  const t=['combo','combat_flow'].includes(clip.name)?sampleTimes[i]:clip.length*i/frames;
  const p=neutral();
  const lift=clip.pose(p,t)||0;
  // The continuous curved torso, neck and shoulders move as one surface.
  // Combining chest/waist intent avoids tearing an unskinned Blockbench mesh.
  p.lower_spine.rotation=p.lower_spine.rotation.map((v,n)=>v+p.upper_spine.rotation[n]);
  p.upper_spine.rotation=[0,0,0];
  let lowest=Infinity;
  for(const side of ['left','right']){
   const e=elements.find(e=>e.name===side+'_shin');
   for(const x of [e.from[0],e.to[0]])for(const y of [e.from[1],e.to[1]])for(const z of [e.from[2],e.to[2]])lowest=Math.min(lowest,point([x,y,z],side+'_knee',p)[1]);
  }
  // Sub-keyframe linear interpolation can lower a rotating sole by <0.06 pixels.
  p.root.position[1]=-lowest+lift+.07;
  for(const g of groups)for(const channel of ['rotation','position','scale']){
   let values=p[g.name][channel].map(v=>Math.round(v*1e5)/1e5);
   if(channel==='rotation'&&['combo','combat_flow'].includes(clip.name)&&/_(arm|elbow|wrist)$/.test(g.name)){
    const previous=lastRotation.get(g.name);
    if(previous){
     const close=a=>a.map((v,n)=>{while(v-previous[n]>180)v-=360;while(v-previous[n]<-180)v+=360;return v});
     const first=close(values),total=values.map((v,n)=>v+g.rotation[n]);
     const alternate=close([total[0]+180,180-total[1],total[2]+180].map((v,n)=>v-g.rotation[n]));
     const distance=a=>a.reduce((sum,v,n)=>sum+(v-previous[n])**2,0);
     values=distance(alternate)<distance(first)?alternate:first;
    }
    lastRotation.set(g.name,values);
   }
   animators[g.uuid].keyframes.push({channel,data_points:[{x:String(values[0]),y:String(values[1]),z:String(values[2])}],uuid:id(clip.name+'/'+g.name+'/'+channel+'/'+i),time:Math.round(t*1e6)/1e6,color:-1,interpolation:'linear'});
  }
 }
 animations.push({uuid:id('clip/'+clip.name),name:'animation.maodie.'+clip.name,loop:clip.loop||'once',length:clip.length,snapping:sampling,override:false,anim_time_update:'',blend_weight:'',start_delay:'',loop_delay:'',animators});
}
const model={meta:{format_version:'5.0',model_format:'free',box_uv:false},name:'耄耋格斗手 · 动作预览 v10',model_identifier:'maodie_fighter_v10',resolution:{width:64,height:32},elements,groups,outliner:[outliner('root')],textures:[{...original.textures[0],name:'original_yellow_cat.png',id:'0',uuid:id('texture'),path:'',relative_path:'original_yellow_cat.png',folder:'',namespace:'',internal:true,saved:true}],animations};
mkdirSync(out,{recursive:true});
writeFileSync(path.join(out,'maodie_fighter_v10.bbmodel'),JSON.stringify(model)+'\n');
writeFileSync(path.join(out,'original_yellow_cat.png'),Buffer.from(model.textures[0].source.split(',')[1],'base64'));
writeFileSync(path.join(out,'clips.json'),JSON.stringify(clips.map(({name,label,length,peak})=>({name,label,length,peak})),null,2)+'\n');
writeFileSync(path.join(out,'combo-choreography.json'),JSON.stringify(comboStrikes,null,2)+'\n');
console.log('Authored Blockbench preview:',elements.filter(e=>e.type==='cube').length,'closed cubes + continuous mesh,',groups.length,'bones,',animations.length,'clips');
