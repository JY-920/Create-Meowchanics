import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const curves = JSON.parse(fs.readFileSync(path.join(root,
    'forge-1.20.1/src/main/resources/assets/laowu/cat_animation_clips/giant_cat_mount.json')));
const walkFront = curves.clips.walk.bones.left_front_leg.rotation
    .map(frame => `{${frame.map(value => `${value}f`).join(',')}}`).join(',');
const javaFrames = frames => frames ? `new float[][]{${frames.map(frame => `{${frame.map(value => `${value}f`).join(',')}}`).join(',')}}` : 'null';
// Dense contact curves exceed one JVM method's 64K limit; keep fixture loading
// separate from the real sampler assertions and split it by authored bone.
const installMethods = Object.entries(curves.clips).map(([name, clip], clipIndex) => `
  ${Object.entries(clip.bones).map(([bone, channels], boneIndex) => `
  static Object channels${clipIndex}_${boneIndex}(java.lang.reflect.Constructor<?> constructor) throws Exception {
    return constructor.newInstance(${['position','rotation','scale'].map(channel => javaFrames(channels[channel])).join(',')});
  }`).join('\n')}
  static void install${clipIndex}(java.util.Map<String,Object> authored,
      java.lang.reflect.Constructor<?> channelsConstructor, java.lang.reflect.Constructor<?> clipConstructor) throws Exception {
    java.util.Map<String,Object> bones = new java.util.HashMap<>();
    ${Object.keys(clip.bones).map((bone, boneIndex) => `bones.put("${bone}", channels${clipIndex}_${boneIndex}(channelsConstructor));`).join('\n')}
    authored.put("${name}", clipConstructor.newInstance(${clip.length}f, ${clip.loop}, bones));
  }`).join('\n');
const installClips = Object.keys(curves.clips).map((_, index) =>
    `install${index}(authored, channelsConstructor, clipConstructor);`).join('\n');
const temporary = fs.mkdtempSync(path.join(os.tmpdir(), 'giant-cat-sampler-'));
const sources = {
  'com/google/gson/JsonElement.java': 'package com.google.gson; public class JsonElement { public JsonObject getAsJsonObject(){return null;} public JsonArray getAsJsonArray(){return null;} public float getAsFloat(){return 0;} public boolean getAsBoolean(){return false;} }',
  'com/google/gson/JsonArray.java': 'package com.google.gson; public class JsonArray extends JsonElement { public int size(){return 0;} public JsonElement get(int i){return null;} }',
  'com/google/gson/JsonObject.java': 'package com.google.gson; public class JsonObject extends JsonElement { public JsonObject getAsJsonObject(String k){return null;} public JsonArray getAsJsonArray(String k){return null;} public JsonElement get(String k){return null;} public boolean has(String k){return false;} public java.util.Set<java.util.Map.Entry<String,JsonElement>> entrySet(){return java.util.Set.of();} }',
  'com/google/gson/JsonParser.java': 'package com.google.gson; public class JsonParser { public static JsonElement parseReader(java.io.Reader r){return null;} }',
  'net/minecraft/resources/ResourceLocation.java': 'package net.minecraft.resources; public class ResourceLocation { public ResourceLocation(){} public ResourceLocation(String n,String p){} public static ResourceLocation fromNamespaceAndPath(String n,String p){return new ResourceLocation();} }',
  'net/minecraft/world/phys/Vec3.java': 'package net.minecraft.world.phys; public class Vec3 { public double x,z; }',
  'net/minecraft/world/entity/Entity.java': 'package net.minecraft.world.entity; public class Entity { public double xo,zo; public boolean ground=true; public net.minecraft.world.phys.Vec3 velocity=new net.minecraft.world.phys.Vec3(); public double getX(){return 0;} public double getZ(){return 0;} public net.minecraft.world.phys.Vec3 getDeltaMovement(){return velocity;} public boolean onGround(){return ground;} }',
  'net/minecraft/world/entity/animal/Cat.java': 'package net.minecraft.world.entity.animal; public class Cat extends net.minecraft.world.entity.Entity { public int tickCount; public float yRotO; public boolean sitting,ordered; public net.minecraft.world.entity.Entity getVehicle(){return null;} public float getYRot(){return 0;} public boolean isInSittingPose(){return sitting;} public boolean isOrderedToSit(){return ordered;} }',
  'cn/laowu/mod/entity/CatGiantCarrier.java': 'package cn.laowu.mod.entity; public class CatGiantCarrier extends net.minecraft.world.entity.Entity { public boolean grounded(){return true;} public float horizontalSpeed(){return .22f;} public int jumpWindup(){return 0;} }',
  'net/minecraft/client/Minecraft.java': 'package net.minecraft.client; public class Minecraft { public static Minecraft getInstance(){return new Minecraft();} public ResourceManager getResourceManager(){return new ResourceManager();} public static class ResourceManager { public Resource getResourceOrThrow(net.minecraft.resources.ResourceLocation l){return new Resource();} } public static class Resource { public java.io.Reader openAsReader(){return new java.io.StringReader("");} } }',
  'cn/laowu/mod/client/RuntimeBlockbenchModel.java': 'package cn.laowu.mod.client; public class RuntimeBlockbenchModel { public record GroupTransform(float x,float y,float z,float xr,float yr,float zr,float sx,float sy,float sz) { public static GroupTransform scaled(float x,float y,float z,float xr,float yr,float zr,float sx,float sy,float sz){return new GroupTransform(x,y,z,xr,yr,zr,sx,sy,sz);} } }',
  'cn/laowu/mod/client/GiantCatAnimationCurveProbe.java': `package cn.laowu.mod.client;
public class GiantCatAnimationCurveProbe {
  INSTALL_METHODS
  static void near(float actual,float wanted,String label) { if(Math.abs(actual-wanted)>.0001f) throw new AssertionError(label+": "+actual+" != "+wanted); }
  public static void main(String[] args) throws Exception {
    float[][] keys={{0,0,1,0},{1,1,1,0}};
    near(GiantCatAnimation.interpolate(keys,.25f)[0],.15625f,"Hermite quarter");
    near(GiantCatAnimation.interpolate(keys,.5f)[0],.5f,"Hermite middle");
    near(GiantCatAnimation.interpolate(keys,1)[0],1,"end key");
    float[][] authoredWalk={WALK_FRONT_CURVE};
    near(GiantCatAnimation.interpolate(authoredWalk,.05f)[0],21.9375f,"authored walk curve quarter-segment");
    float[] rotation=GiantCatAnimation.rotationRadians(30,40,50);
    near(rotation[0],-(float)Math.toRadians(30),"Blockbench X");
    near(rotation[1],(float)Math.toRadians(40),"Blockbench Y");
    near(rotation[2],-(float)Math.toRadians(50),"Blockbench Z");
    near(GiantCatAnimation.runBlend(.22f),0,"walking speed");
    near(GiantCatAnimation.runBlend(.32f),1,"sprinting speed");
    near(GiantCatAnimation.locomotionWeight(false,false,.7f),0,"no stride airborne");
    near(GiantCatAnimation.locomotionWeight(true,true,.7f),0,"no stride during windup");
    near(GiantCatAnimation.locomotionWeight(true,false,.7f),.7f,"stride grounded");
    near(GiantCatAnimation.advanceJumpTime(0,.05f,true,true,true,false),.05f,"windup begins at first key");
    near(GiantCatAnimation.advanceJumpTime(.15f,.05f,false,false,true,true),.25f,"takeoff continues after anticipation");
    var channelsConstructor=Class.forName("cn.laowu.mod.client.GiantCatAnimation$Channels").getDeclaredConstructors()[0];
    var clipConstructor=Class.forName("cn.laowu.mod.client.GiantCatAnimation$Clip").getDeclaredConstructors()[0];
    channelsConstructor.setAccessible(true); clipConstructor.setAccessible(true);
    java.util.Map<String,Object> authored=new java.util.HashMap<>();
    INSTALL_CLIPS
    var clipsField=GiantCatAnimation.class.getDeclaredField("clips"); clipsField.setAccessible(true);
    clipsField.set(null,authored);
    var cat=new net.minecraft.world.entity.animal.Cat();
    GiantCatAnimation.sample(cat,0);
    cat.sitting=true;
    float lastY=0;
    for(int frame=1;frame<=180;frame++) {
      float age=frame/3f; cat.tickCount=(int)age;
      var pose=GiantCatAnimation.sample(cat,age-cat.tickCount);
      float y=pose.get("group2").y();
      if(Math.abs(y-lastY)>.28f) throw new AssertionError("rest transition snaps at age "+age+": "+(y-lastY));
      lastY=y;
    }
    if(lastY>-3f) throw new AssertionError("sitting cat must lower its side to the floor, got "+lastY);
    var resting=GiantCatAnimation.sample(cat,0);
    if(Math.abs(resting.get("group2").zr())<1.5f) throw new AssertionError("rest rolls onto its side, not its belly");
    if(Math.abs(resting.get("left_front_leg").xr()-resting.get("right_front_leg").xr())<.1f)
      throw new AssertionError("side-rest front legs must be staggered, not identically tucked");
    if(Math.abs(resting.get("left_hind_leg").xr()-resting.get("right_hind_leg").xr())<.15f)
      throw new AssertionError("side-rest hind legs need different relaxed bends");
    near(resting.get("body").y(),0,"idle body sway fades out for floor contact");
    cat.sitting=false;
    for(int frame=181;frame<=360;frame++) {
      float age=frame/3f; cat.tickCount=(int)age;
      float y=GiantCatAnimation.sample(cat,age-cat.tickCount).get("group2").y();
      if(Math.abs(y-lastY)>.28f) throw new AssertionError("standing transition snaps");
      lastY=y;
    }
    if(lastY<-.02f) throw new AssertionError("cat must stand back up");
    // The synced pose is authoritative; local predicted commands must not activate it.
    var predicted=new net.minecraft.world.entity.animal.Cat(); predicted.ordered=true;
    for(int tick=0;tick<=60;tick++) { predicted.tickCount=tick; GiantCatAnimation.sample(predicted,0); }
    near(GiantCatAnimation.restWeight(predicted),0,"local-only command cannot revive rest");
    var lowFps=new net.minecraft.world.entity.animal.Cat(); lowFps.sitting=true;
    var highFps=new net.minecraft.world.entity.animal.Cat(); highFps.sitting=true;
    GiantCatAnimation.sample(lowFps,0); GiantCatAnimation.sample(highFps,0);
    for(int tick=1;tick<=10;tick++) { lowFps.tickCount=tick; GiantCatAnimation.sample(lowFps,0); }
    for(int frame=1;frame<=60;frame++) {
      float age=frame/6f; highFps.tickCount=(int)age; GiantCatAnimation.sample(highFps,age-highFps.tickCount);
    }
    float lowY=GiantCatAnimation.sample(lowFps,0).get("group2").y();
    float lowRoll=Math.abs(GiantCatAnimation.sample(lowFps,0).get("group2").zr());
    near(GiantCatAnimation.sample(highFps,0).get("group2").y(),lowY,"frame independent lie-down");
    if(lowY>-.9f) throw new AssertionError("synced pose starts lying down");
    lowFps.sitting=false;
    near(GiantCatAnimation.sample(lowFps,0).get("group2").y(),lowY,"reversal preserves pose on same frame");
    lowFps.tickCount++;
    float reverseY=GiantCatAnimation.sample(lowFps,0).get("group2").y();
    if(Math.abs(reverseY-lowY)>.3f) throw new AssertionError("reversal root snaps from "+lowY+" to "+reverseY+": "+(reverseY-lowY));
    if(Math.abs(GiantCatAnimation.sample(lowFps,0).get("group2").zr())>=lowRoll)
      throw new AssertionError("reversal smoothly unrolls toward standing");
    var idleCat=new net.minecraft.world.entity.animal.Cat(); idleCat.yRotO=-30;
    for(int tick=0;tick<=40;tick++) {
      idleCat.tickCount=tick;
      var idlePose=GiantCatAnimation.sample(idleCat,0);
      near(idlePose.get("group2").y(),0,"idle root remains on floor");
      near(idlePose.get("group2").zr(),0,"idle root does not roll paws");
      near(idlePose.get("group5").zr(),0,"stationary facing change cannot lean planted paws");
    }
    var runner=new net.minecraft.world.entity.animal.Cat(); runner.velocity.x=.32;
    float minBody=2,maxBody=0,minLeg=2,maxLeg=0;
    for(int frame=0;frame<600;frame++) {
      float age=frame/3f; runner.tickCount=(int)age;
      var pose=GiantCatAnimation.sample(runner,age-runner.tickCount);
      if(frame<300) continue;
      var body=pose.get("group2"); var leg=pose.get("left_front_leg");
      minBody=Math.min(minBody,body.sy()); maxBody=Math.max(maxBody,body.sy());
      minLeg=Math.min(minLeg,leg.sy()); maxLeg=Math.max(maxLeg,leg.sy());
      if(Math.abs(pose.get("group5").xr())>Math.toRadians(3)) throw new AssertionError("run body pitches excessively");
      if(Math.abs(pose.get("head").yr())>.001f) throw new AssertionError("run head wags sideways");
    }
    if(maxBody-minBody<.15f || maxLeg-minLeg<.25f) throw new AssertionError("run must deform body AND legs");
    System.out.println("giant cat animation sampler: OK");
  }
}`.replace('WALK_FRONT_CURVE', walkFront).replace('INSTALL_CLIPS', installClips).replace('INSTALL_METHODS', installMethods),
};
try {
  const files = [];
  for (const [relative, content] of Object.entries(sources)) {
    const target = path.join(temporary, relative);
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.writeFileSync(target, content);
    files.push(target);
  }
  for (const loader of ['forge-1.20.1', 'neoforge-1.21.1']) {
    const java = path.join(root, loader, 'src/main/java/cn/laowu/mod/client/GiantCatAnimation.java');
    execFileSync('javac', ['-d', temporary, java, ...files], { stdio: 'pipe' });
    const output = execFileSync('java', ['-cp', temporary, 'cn.laowu.mod.client.GiantCatAnimationCurveProbe'], { encoding: 'utf8' });
    process.stdout.write(`${loader}: ${output}`);
  }
} finally {
  fs.rmSync(temporary, { recursive: true, force: true });
}
