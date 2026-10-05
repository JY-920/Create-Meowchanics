import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';

const root = path.dirname(path.dirname(fileURLToPath(import.meta.url)));
const temp = fs.mkdtempSync(path.join(os.tmpdir(), 'giant-boss-combat-'));
try {
  const probe = path.join(temp, 'GiantBossCombatProbe.java');
  fs.writeFileSync(probe, `import cn.laowu.mod.entity.GiantCatBossCombat;
import java.util.UUID;
public class GiantBossCombatProbe {
  static void check(boolean pass, String label) { if (!pass) throw new AssertionError(label); }
  static void near(double actual, double expected, String label) { check(Math.abs(actual-expected)<1e-6,label+": "+actual); }
  static float facing(String method, float start, float movement, float ticks) {
    try { return (float) GiantCatBossCombat.class.getMethod(method,float.class,float.class,float.class).invoke(null,start,movement,ticks); }
    catch (ReflectiveOperationException missing) { throw new AssertionError("Missing real smooth roll-facing implementation: "+method,missing); }
  }
  static float migratedHealth(float health, float oldMaximum, float newMaximum) {
    try { return (float) GiantCatBossCombat.class.getMethod("healthAfterMaxChange",float.class,float.class,float.class).invoke(null,health,oldMaximum,newMaximum); }
    catch (ReflectiveOperationException missing) { throw new AssertionError("Missing real damage-preserving health migration",missing); }
  }
  public static void main(String[] args) {
    try {
      var head=GiantCatBossCombat.class.getMethod("headTouches",float.class,double.class,double.class,double.class,double.class,double.class,double.class);
      check((boolean)head.invoke(null,0f,-.3,0d,2.9,.3,1.8,3.5),"nose can touch a standing player outside body box");
      check((boolean)head.invoke(null,0f,-.3,0d,2.3,.3,1d,2.9),"forward bite can reach an adult cat below the giant head");
      check((boolean)head.invoke(null,0f,-.15,0d,2.35,.15,.5,2.65),"forward bite can reach a kitten");
      check(!(boolean)head.invoke(null,0f,-.3,-2d,2.3,.3,-1d,2.9),"bite cannot reach a target below the ground");
      check(!(boolean)head.invoke(null,0f,-.3,0d,-.3,.3,1.8,.3),"torso overlap cannot bite");
      check(!(boolean)head.invoke(null,0f,-.3,0d,-3d,.3,1.8,-2.4),"rear contact cannot bite");
      check(!(boolean)head.invoke(null,0f,-.3,3d,2.9,.3,4.8,3.5),"above-head player cannot bite");
      check((boolean)head.invoke(null,-90f,2.9,0d,-.3,3.5,1.8,.3),"head region rotates with actual body heading");
      check(!(boolean)head.invoke(null,45f,-.95,1.5,2.4,-.85,1.7,2.5),"empty diagonal envelope corner cannot bite");
    } catch(ReflectiveOperationException error) {throw new AssertionError("Missing oriented real head-contact geometry",error);}
    // Break caught: old saved MELEE phase can still hold/play an active attack.
    check(GiantCatBossCombat.nextPhase((byte)2,0,true,false,true)==1,"legacy melee immediately returns to contact-only idle");
    // Break caught: side-facing yaw snaps at windup/roll/recovery seams or takes a full turn across wrap.
    near(facing("rollWindupYaw",0,0,0),0,"windup keeps entry body heading");
    near(facing("rollWindupYaw",0,0,8),45,"windup smoothly turns halfway sideways");
    near(facing("rollWindupYaw",0,0,16),90,"rolling body is perpendicular to forward movement");
    near(facing("rollWindupYaw",179,91,8),180,"sideways transition crosses wrap by shortest path");
    near(facing("rollRecoveryYaw",90,0,0),90,"recovery keeps rolled side-facing entry");
    near(facing("rollRecoveryYaw",90,0,15),90,"standing recovery retains its actual sideways heading");
    near(facing("rollRecoveryYaw",90,0,30),90,"recovery cannot add a compulsory quarter turn");
    near(facing("rollRecoveryYaw",35,0,0),35,"cancelled windup cannot snap to a full sideways heading");
    near(facing("rollRecoveryYaw",35,0,15),35,"cancelled windup retains actual partial heading");
    near(facing("rollRecoveryYaw",179,-179,15),179,"recovery has no yaw-wrap rotation");
    float last=0;
    for(int tick=1;tick<=16;tick++) {float yaw=facing("rollWindupYaw",0,0,tick);check(yaw>=last&&yaw-last<8.5,"no abrupt ninety-degree windup turn");last=yaw;}
    // Break caught: ending the summon or vulnerable recoveries one tick early.
    check(GiantCatBossCombat.nextPhase((byte)0,59,true,false,true)==0,"summon lasts 60 ticks");
    check(GiantCatBossCombat.nextPhase((byte)0,60,true,false,true)==1,"summon completes");
    check(GiantCatBossCombat.nextPhase((byte)5,29,true,false,true)==5,"roll recovery cannot attack early");
    check(GiantCatBossCombat.nextPhase((byte)5,30,true,false,true)==1,"roll recovery ends");
    check(GiantCatBossCombat.nextPhase((byte)8,39,true,false,true)==8,"slam recovery cannot attack early");
    check(GiantCatBossCombat.nextPhase((byte)8,40,true,false,true)==1,"slam recovery ends");
    // Break caught: launching before the telegraph or continuing a cancelled windup.
    check(GiantCatBossCombat.nextPhase((byte)3,15,true,false,true)==3,"roll telegraph");
    check(GiantCatBossCombat.nextPhase((byte)3,16,true,false,true)==4,"roll launches");
    check(GiantCatBossCombat.nextPhase((byte)6,23,true,false,true)==6,"jump telegraph");
    check(GiantCatBossCombat.nextPhase((byte)6,24,true,false,true)==7,"jump launches");
    check(GiantCatBossCombat.nextPhase((byte)3,9,true,false,false)==5,"lost target recovers from partial roll windup");
    check(GiantCatBossCombat.nextPhase((byte)6,12,true,false,false)==8,"lost target recovers from partial jump windup");
    // Break caught: wall tunneling phase, early slam, or airborne attack stuck forever.
    check(GiantCatBossCombat.nextPhase((byte)4,12,true,true,true)==5,"wall terminates roll");
    check(GiantCatBossCombat.nextPhase((byte)4,59,true,false,false)==4,"committed roll survives lost target");
    check(GiantCatBossCombat.nextPhase((byte)4,60,true,false,true)==5,"roll expires");
    check(GiantCatBossCombat.nextPhase((byte)7,1,true,false,true)==7,"launch ground flag cannot slam");
    check(GiantCatBossCombat.nextPhase((byte)7,24,false,false,true)==7,"airborne cannot slam");
    check(GiantCatBossCombat.nextPhase((byte)7,24,true,false,true)==8,"landing recovers");
    check(GiantCatBossCombat.nextPhase((byte)7,100,false,false,true)==8,"airborne timeout aborts");
    // Break caught: full-speed homing instead of a slow, shortest-path turn.
    near(GiantCatBossCombat.steer(0,180),-3,"turn limited to three degrees");
    near(GiantCatBossCombat.steer(179,-179),181,"shortest turn across wrap");
    // Break caught: chasing target after launch, wrong discrete gravity, unbounded leap.
    double[] launch=GiantCatBossCombat.leap(12,0,0);
    double x=0,y=0,vy=launch[1];
    for(int i=0;i<24;i++){x+=launch[0];y+=vy;vy-=0.08;}
    near(x,12,"lands at locked x"); near(y,0,"lands at locked height");
    double[] far=GiantCatBossCombat.leap(30,0,40);
    near(Math.hypot(far[0],far[2])*24,16,"leap capped at sixteen blocks");
    double[] above=GiantCatBossCombat.leap(0,40,0);
    near(above[1]*24-0.08*24*23/2,6,"vertical aim bounded");
    // Break caught: repeated collision damage each tick or shared cooldown across victims.
    GiantCatBossCombat contacts=new GiantCatBossCombat();
    UUID a=UUID.randomUUID(),b=UUID.randomUUID();
    check(contacts.canHit(a,0),"first contact permitted");contacts.recordHit(a,0);
    check(!contacts.canHit(a,19),"no repeated damage in cooldown");
    check(contacts.canHit(b,1),"different victim independent");
    check(contacts.canHit(a,20),"same victim allowed after cooldown");
    contacts.clearHits();check(contacts.canHit(a,1),"new attack clears contacts");
    // Break caught: upgrading max health restores a wounded/dead boss to full health,
    // ignores existing max-health modifiers, or applies its ratio twice on reload.
    near(migratedHealth(275,550,800),400,"half-health legacy boss remains half-health after upgrade");
    near(migratedHealth(0,550,800),0,"migration must not revive a dead boss");
    near(migratedHealth(550,550,800),800,"healthy legacy boss receives the new maximum");
    near(migratedHealth(375,750,1000),500,"existing max-health modifiers preserve the wounded fraction");
    near(migratedHealth(400,800,800),400,"reloading migrated health is idempotent");
    near(migratedHealth(900,550,800),800,"malformed over-max legacy health cannot exceed the new maximum");
    near(migratedHealth(-1,550,800),0,"negative saved health never becomes positive");
    near(migratedHealth(10,0,800),0,"invalid old maximum cannot resurrect a saved entity");
    System.out.println("PASS: giant boss phase, turn, ballistic landing, per-victim cooldown");
  }
}`);
  for (const loader of ['forge-1.20.1', 'neoforge-1.21.1']) {
    const helper = path.join(root, loader, 'src/main/java/cn/laowu/mod/entity/GiantCatBossCombat.java');
    assert.ok(fs.existsSync(helper), `${loader}: real giant boss combat implementation is missing`);
    execFileSync('javac', ['-d', temp, helper, probe], {stdio: 'inherit'});
    execFileSync('java', ['-cp', temp, 'GiantBossCombatProbe'], {stdio: 'inherit'});
  }
} finally {
  fs.rmSync(temp, {recursive: true, force: true});
}
