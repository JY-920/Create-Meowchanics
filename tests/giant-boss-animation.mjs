import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const temporary=fs.mkdtempSync(path.join(os.tmpdir(),'giant-boss-animation-'));
const stub=`package cn.laowu.mod.client;
public class RuntimeBlockbenchModel {
 public record GroupTransform(float x,float y,float z,float xRot,float yRot,float zRot,float scaleX,float scaleY,float scaleZ) {
 public static final GroupTransform IDENTITY=new GroupTransform(0,0,0,0,0,0,1,1,1);
 public static GroupTransform scaled(float x,float y,float z,float a,float b,float c,float sx,float sy,float sz){return new GroupTransform(x,y,z,a,b,c,sx,sy,sz);}
 }
}`;
const probe=`package cn.laowu.mod.client;
import java.util.*; public class BossAnimationProbe {
 public static void main(String[] args) throws Exception {
 Class<?> type; try {type=Class.forName("cn.laowu.mod.client.GiantCatBossAnimation");}
 catch(ClassNotFoundException missing){throw new AssertionError("Boss needs its own phase-driven pose sampler",missing);}
 var method=type.getMethod("sample",byte.class,float.class,float.class,Map.class);
 var walking=Map.of("left_front_leg",RuntimeBlockbenchModel.GroupTransform.scaled(0,0,0,.25f,0,0,1,1,1));
 for(float meleeTime:new float[]{0,4,8,12,18}) {
  var actual=(Map<String,RuntimeBlockbenchModel.GroupTransform>)method.invoke(null,(byte)2,meleeTime,60f,walking);
  for(var entry:actual.entrySet())if(!walking.getOrDefault(entry.getKey(),RuntimeBlockbenchModel.GroupTransform.IDENTITY).equals(entry.getValue()))
   throw new AssertionError("Melee must preserve authored locomotion without a separate strike animation at "+meleeTime);
 }
 for(byte phase=0;phase<=8;phase++)for(int frame=0;frame<=240;frame++) {
 float ticks=frame/4f;
 var pose=(Map<String,RuntimeBlockbenchModel.GroupTransform>)method.invoke(null,phase,ticks,27f,Map.of());
 StringJoiner bones=new StringJoiner(",");
 for(var e:pose.entrySet()){var v=e.getValue();bones.add("\\\""+e.getKey()+"\\\":["+v.x()+","+v.y()+","+v.z()+","+v.xRot()+","+v.yRot()+","+v.zRot()+","+v.scaleX()+","+v.scaleY()+","+v.scaleZ()+"]");}
 System.out.println("{\\\"phase\\\":"+phase+",\\\"ticks\\\":"+ticks+",\\\"bones\\\":{"+bones+"}}");
 }
 java.lang.reflect.Method recovery;
 try {recovery=type.getMethod("sample",byte.class,float.class,byte.class,float.class,Map.class);}
 catch(NoSuchMethodException missing){throw new AssertionError("Recovery needs the synchronized source phase and elapsed ticks",missing);}
 for(byte from:new byte[]{1,3,4,6,7})for(float interrupted:new float[]{0,2,3,5,12,15,23,27,40,48,55,59,60}) {
 if(from==3 && interrupted>16)continue;
 if(from!=4 && interrupted>23)continue;
 byte phase=(byte)(from==3||from==4?5:8);
 for(int frame=0;frame<=160;frame++){
 float ticks=frame/4f;
 var pose=(Map<String,RuntimeBlockbenchModel.GroupTransform>)recovery.invoke(null,phase,ticks,from,interrupted,Map.of());
 StringJoiner bones=new StringJoiner(",");
 for(var e:pose.entrySet()){var v=e.getValue();bones.add("\\\""+e.getKey()+"\\\":["+v.x()+","+v.y()+","+v.z()+","+v.xRot()+","+v.yRot()+","+v.zRot()+","+v.scaleX()+","+v.scaleY()+","+v.scaleZ()+"]");}
 System.out.println("{\\\"phase\\\":"+phase+",\\\"ticks\\\":"+ticks+",\\\"from\\\":"+from+",\\\"interrupted\\\":"+interrupted+",\\\"bones\\\":{"+bones+"}}");
 }
 }
 }
}`;
// Evaluate the actual authored cube corners through its complete hierarchy.
// Missing root rotation, reversed jump pitch, leg directions, or phase seams
// change these positions, rather than merely changing a source-code string.
function vertices(model,bones){
 const groups=new Map(model.groups.map(g=>[g.uuid,g]));
 const elements=new Map(model.elements.map(e=>[e.uuid,e])); const result={};
 function rotate(p,r){let [x,y,z]=p;let [a,b,c]=r;
  [y,z]=[y*Math.cos(a)-z*Math.sin(a),y*Math.sin(a)+z*Math.cos(a)];
  [x,z]=[x*Math.cos(b)+z*Math.sin(b),-x*Math.sin(b)+z*Math.cos(b)];
  return [x*Math.cos(c)-y*Math.sin(c),x*Math.sin(c)+y*Math.cos(c),z];}
 function visit(nodes,chain){for(const node of nodes){if(typeof node==='object'){visit(node.children,[...chain,groups.get(node.uuid)]);continue;}
  const e=elements.get(node);const points=[];
  for(const x of [e.from[0],e.to[0]])for(const y of [e.from[1],e.to[1]])for(const z of [e.from[2],e.to[2]]){
   let p=[x,y,z].map((v,i)=>v-e.origin[i]);p=rotate(p,(e.rotation??[0,0,0]).map(a=>a*Math.PI/180));
   p=p.map((v,i)=>v+e.origin[i]-chain.at(-1).origin[i]);
   for(let n=chain.length-1;n>=0;n--){const g=chain[n],t=bones[g.name]??[0,0,0,0,0,0,1,1,1];
    p=p.map((v,i)=>v*t[6+i]);p=rotate(p,g.rotation.map((a,i)=>a*Math.PI/180+t[3+i]*(i===1?1:-1)));
    p=p.map((v,i)=>v+g.origin[i]-(chain[n-1]?.origin[i]??0)+t[i]);}
   points.push(p);
  }result[e.name]=points;
 }}visit(model.outliner,[]);return result;
}
try {
 const pkg=path.join(temporary,'cn/laowu/mod/client');fs.mkdirSync(pkg,{recursive:true});
 const files=[['RuntimeBlockbenchModel.java',stub],['BossAnimationProbe.java',probe]].map(([name,contents])=>{const p=path.join(pkg,name);fs.writeFileSync(p,contents);return p;});
 for(const loader of ['forge-1.20.1','neoforge-1.21.1']){
  const java=path.join(root,loader,'src/main/java/cn/laowu/mod/client/GiantCatBossAnimation.java');
  execFileSync('javac',['-d',temporary,...files,...(fs.existsSync(java)?[java]:[])],{stdio:'pipe'});
  const rows=execFileSync('java',['-cp',temporary,'cn.laowu.mod.client.BossAnimationProbe'],{encoding:'utf8',maxBuffer:16e6}).trim().split('\n').map(JSON.parse);
  const model=JSON.parse(fs.readFileSync(path.join(root,loader,'src/main/resources/assets/laowu/models/entity/giant_cat_mount.bbmodel')));
  const row=(p,t)=>rows.find(r=>r.phase===p&&r.ticks===t);
  const points=(p,t)=>Object.values(vertices(model,row(p,t).bones)).flat();
  const distance=(a,b)=>Math.hypot(...a.map((v,i)=>v-b[i]));
  const seam=(a,b)=>{const av=points(...a),bv=points(...b);assert.ok(Math.max(...av.map((v,i)=>distance(v,bv[i])))<.02,`phase seam ${a} -> ${b} must not pop`);};
  seam([3,16],[4,0]);seam([4,27],[5,0]);seam([6,24],[7,0]);seam([7,20],[8,0]);seam([5,30],[1,0]);seam([8,40],[1,0]);
  assert.ok(Math.abs(row(3,16).bones.group2[5])>1.5,'windup visibly lies on side');
  assert.ok(Math.abs(row(4,20).bones.group2[5]-row(4,0).bones.group2[5])>6.2,'rolling body completes full revolution, not static prone slide');
  seam([4,60],[1,0]);
  let lastRoll=row(4,40).bones.group2[5];
  for(let tick=40.25;tick<=60;tick+=.25){
   const angle=row(4,tick).bones.group2[5];
   assert.ok(angle>=lastRoll-1e-5&&angle-lastRoll<.12,
    'last revolution continues forward smoothly while unfolding, never rewinds');lastRoll=angle;
  }
  assert.ok(row(4,60).bones.group2[5]-row(4,59.75).bones.group2[5]<.003,
   'final roll eases angular velocity to zero before standing recovery');
  const rollNow=vertices(model,row(4,0).bones).body_cube,rollNext=vertices(model,row(4,.25).bones).body_cube;
  const rollBottom=Math.min(...rollNow.map(p=>p[1]));
  const contact=rollNow.map((p,i)=>({p,i})).filter(({p})=>Math.abs(p[1]-rollBottom)<.001);
  assert.ok(contact.every(({p,i})=>rollNext[i][0]<p[0]),
   'with bodyYaw=travelYaw+90, bottom surface moves against forward travel (not backwards rolling)');
  for(const phase of [2,3,4,5,6,7,8])for(let t=.25;t<=({2:18,3:16,4:60,5:30,6:24,7:20,8:40}[phase]);t+=.25){
   const a=points(phase,t-.25),b=points(phase,t);assert.ok(Math.max(...a.map((v,i)=>distance(v,b[i])))<2.3,`smooth geometry ${phase}:${t}`);
  }
  const seated=vertices(model,row(8,0).bones);
  const mean=(name,axis)=>seated[name].reduce((s,p)=>s+p[axis],0)/8;
  assert.ok(mean('head_main',1)>mean('body_cube',1)+2,'slam keeps head above seated torso, butt towards ground');
  assert.ok(mean('left_hind_leg_cube',2)<mean('body_cube',2),'seated hind feet point forward rather than beneath rump');
  const bottom=name=>Math.min(...seated[name].map(p=>p[1]));
  const minimum=(name,axis)=>Math.min(...seated[name].map(p=>p[axis]));
  const maximum=(name,axis)=>Math.max(...seated[name].map(p=>p[axis]));
  const bodyAxis=seated.body_cube[2].map((v,i)=>v-seated.body_cube[0][i]);
  assert.ok(Math.abs(bodyAxis[1])>15.5&&Math.abs(bodyAxis[2])<1,
   'seated torso long axis must be upright, not a reclined whole-cat pose');
  assert.ok(Math.abs(seated.head_main[1][1]-seated.head_main[0][1])<.05,
   'head forward axis must be horizontal rather than pointing into sky');
  for(const side of ['left','right']) {
   assert.ok(Math.abs(bottom(`${side}_hind_leg_cube`)-bottom('body_cube'))<.05,
    `${side} hind paw and rump must share the support plane, not just whole-model minimum`);
   const chestHeight=maximum('body_cube',1)-bottom('body_cube');
   const forearm=seated[`${side}_front_leg_cube`];
   const pawHeight=bottom(`${side}_front_leg_cube`)-bottom('body_cube');
   assert.ok(pawHeight>chestHeight*.35&&pawHeight<chestHeight*.6,
    `${side} front paw must hang in mid-chest, not form a pillar reaching the floor`);
   assert.ok(distance(forearm[2],forearm[0])<=10.001,
    'folded forearms cannot lengthen the original leg geometry');
   const forwardReach=forearm[2][2]-forearm[0][2],naturalDrop=forearm[2][1]-forearm[0][1];
   assert.ok(forwardReach>3&&naturalDrop>1&&naturalDrop<forwardReach,
    `${side} arm reaches forward with a gentle downward slope instead of hanging vertically against chest`);
   assert.ok(maximum(`${side}_front_leg_cube`,1)>mean('body_cube',1)+3,
    'forearms attach high on the sides of the chest');
   const hind=seated[`${side}_hind_leg_cube`];
   assert.ok(Math.abs(hind[2][1]-hind[0][1])<.05,'extended hind-foot sole remains flat');
   assert.ok(minimum(`${side}_hind_leg_cube`,2)<minimum('body_cube',2)-1,'hind feet extend forward beyond belly');
   assert.ok(maximum(`${side}_front_leg_cube`,2)<minimum('body_cube',2)+.15,
    `${side} forearm must be exposed outside chest, not hidden inside torso`);
  }
  assert.ok(minimum('left_front_leg_cube',0)>.25&&maximum('right_front_leg_cube',0)<-.25,
   'inward folded forearms cannot cross or intersect each other');
  assert.ok(bottom('body_cube')<=Math.min(bottom('tail_base_cube'),bottom('tail_tip_cube')),
   'slam support belongs to rump, not a low tail suspending the seated body');
  assert.ok(row(6,24).bones.group2[7]<.85,'jump anticipation visibly squashes');
  for(const tick of [0,4,8,12,18])seam([2,tick],[1,0]);
  seam([2,18],[1,0]);
  const defaultRecovery=rows.find(r=>r.from===1&&r.interrupted===0&&r.ticks===0);
  const defaultPose=Object.values(vertices(model,defaultRecovery.bones)).flat(),fullSeat=points(8,0);
  assert.ok(Math.max(...defaultPose.map((v,i)=>distance(v,fullSeat[i])))<.02,
   'missing/invalid synchronized recovery source falls back to seated impact, not crouch');
  for(const from of [3,4,6,7])for(const interrupted of [0,2,3,5,12,15,23,27,40,48,55,59,60]){
   if(from===3&&interrupted>16)continue;
   if(from!==4&&interrupted>23)continue;
   const recovery=rows.filter(r=>r.from===from&&r.interrupted===interrupted);
   const source=points(from,interrupted),initial=Object.values(vertices(model,recovery[0].bones)).flat();
   assert.ok(Math.max(...source.map((v,i)=>distance(v,initial[i])))<.02,
    `interrupted phase${from} tick${interrupted} must not snap entering recovery`);
   const duration=from===3||from===4?30:40;
   let previous=initial;
   let previousAngle=from===4?row(4,interrupted).bones.group2[5]:null;
   if(from===4)assert.ok(Math.abs(recovery[0].bones.group2[5]-previousAngle)<1e-4,
    'collision recovery starts from the actual unwrapped roll angle, not its shortest reverse arc');
   for(const r of recovery.filter(r=>r.ticks>0&&r.ticks<=duration)){
    const current=Object.values(vertices(model,r.bones)).flat();
    assert.ok(Math.max(...previous.map((v,i)=>distance(v,current[i])))<2.3,
     `interrupted phase${from} tick${interrupted} smooth recovery at ${r.ticks}`);previous=current;
    if(from===4){const angle=r.bones.group2[5];
     assert.ok(angle>=previousAngle-1e-5&&angle-previousAngle<.18,
      `collision at ${interrupted} must continue forward to standing without reverse recovery`);previousAngle=angle;
     if(interrupted===60)assert.ok(Math.abs(angle-row(4,60).bones.group2[5])<1e-4,
      'normal recovery is an already-standing buffer, not another get-up animation');
    }
   }
   const standing=points(1,0);
   assert.ok(Math.max(...standing.map((v,i)=>distance(v,previous[i])))<.02,
    `interrupted phase${from} must finish standing at ${duration}ticks`);
  }
  console.log(`${loader}: giant boss pose geometry OK`);
 }
} finally {fs.rmSync(temporary,{recursive:true,force:true});}
