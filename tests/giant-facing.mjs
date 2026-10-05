import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {execFileSync} from 'node:child_process';
const temp=fs.mkdtempSync(path.join(os.tmpdir(),'giant-facing-'));
try {
  const probe=path.join(temp,'FacingProbe.java');
  fs.writeFileSync(probe,`import cn.laowu.mod.client.GiantCatFacing;
public class FacingProbe {
  static void check(boolean x,String name){if(!x)throw new AssertionError(name);}
  public static void main(String[] args){
    float angle=GiantCatFacing.approach(179,-179,1,10);
    check(angle>179&&angle<=181,"Crosses wrap by shortest path");
    check(Math.abs(GiantCatFacing.approach(0,180,1,10))<=10.001,"No instantaneous half-turn");
    float stable=0;for(int i=0;i<80;i++)stable=GiantCatFacing.approach(stable,i%2==0?3:-3,1,10);
    check(Math.abs(stable)<1,"Small noisy targets do not whip body");
    check(GiantCatFacing.approach(20,-80,0,10)==20,"Multiple renders in same tick cannot advance turn");
    float a=0,b=0;for(int i=0;i<20;i++)a=GiantCatFacing.approach(a,60,.5f,10);
    for(int i=0;i<10;i++)b=GiantCatFacing.approach(b,60,1,10);
    check(Math.abs(a-b)<1.5,"Frame-rate stable convergence");
    float rest=23;
    for(int i=0;i<40;i++)rest=GiantCatFacing.bodyYaw(rest,i%2==0?170:-170,1,false,true,1);
    check(rest==23,"Rest locks body regardless of look target");
    rest=GiantCatFacing.bodyYaw(rest,170,1,false,false,.25f);
    check(rest==23,"Getting up keeps body heading until pose is upright");
    rest=GiantCatFacing.bodyYaw(rest,170,1,false,false,0);
    check(rest>23&&rest<=33.001,"Upright cat smoothly resumes turning");
    for(float weight:new float[]{1,.8f,.25f,.003f,0}) {
      float mounted=GiantCatFacing.bodyYaw(23,170,1,true,false,weight);
      check(mounted>23&&mounted<=41.001,
          "Mounted get-up responds to rider heading on the first tick without snapping; restWeight="+weight+" yaw="+mounted);
    }
    float mounted=GiantCatFacing.bodyYaw(23,170,.25f,true,false,1);
    check(mounted>23&&mounted<=27.501,"First partial tick of mounted get-up already responds with bounded turning");
    check(GiantCatFacing.bodyYaw(mounted,-80,0,true,false,1)==mounted,
        "Cat and rider rendering in the same frame cannot advance mounted get-up twice");
    mounted=GiantCatFacing.bodyYaw(179,-179,1,true,false,1);
    check(mounted>179&&mounted<181,"Mounted get-up crosses angle wrap by the shortest arc");
    mounted=23;
    for(int tick=0;tick<20;tick++)mounted=GiantCatFacing.bodyYaw(mounted,170,1,true,false,.25f);
    check(mounted>169&&mounted<=170,"Mounted heading converges while the get-up pose still has rest weight");
    check(GiantCatFacing.bodyYaw(mounted,-80,1,false,true,1)==mounted,
        "Dismounting back into rest restores the unmounted body lock");
    System.out.println("PASS: giant facing shortest arc, bounded speed, jitter, frame-rate, immediate mounted get-up response, unmounted rest lock");
  }
}`);
  const failures=[];
  for(const loader of ['forge-1.20.1','neoforge-1.21.1']){
    try {
      console.log(loader);
      execFileSync('javac',['-d',temp,loader+'/src/main/java/cn/laowu/mod/client/GiantCatFacing.java',probe],{stdio:'inherit'});
      execFileSync('java',['-cp',temp,'FacingProbe'],{stdio:'inherit'});
    } catch(error) { failures.push(new Error(loader,{cause:error})); }
  }
  if(failures.length)throw new AggregateError(failures,'Giant facing regression failed');
}finally{fs.rmSync(temp,{recursive:true,force:true});}
