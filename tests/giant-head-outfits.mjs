import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';

// Exercises the real runtime loader, mesh filtering and helper using real Gson/JOML.
// The Minecraft boundary doubles only supply resources and collect emitted vertices.
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const cache = path.join(process.env.USERPROFILE ?? os.homedir(), '.gradle/caches/forge_gradle/maven_downloader');
const jars = ['com/google/code/gson/gson/2.10/gson-2.10.jar',
    'org/joml/joml/1.10.5/joml-1.10.5.jar', 'org/slf4j/slf4j-api/2.0.1/slf4j-api-2.0.1.jar']
    .map(name => path.join(cache, name));
for (const jar of jars) if (!fs.existsSync(jar)) throw new Error(`Missing cached dependency: ${jar}`);
const temporary = fs.mkdtempSync(path.join(os.tmpdir(), 'giant-head-outfits-'));
const sources = {
    'net/minecraft/resources/ResourceLocation.java': `package net.minecraft.resources;
public record ResourceLocation(String namespace,String path) { public static ResourceLocation fromNamespaceAndPath(String n,String p){return new ResourceLocation(n,p);} }`,
    'cn/laowu/mod/LaoWuMod.java': `package cn.laowu.mod; public class LaoWuMod { public static net.minecraft.resources.ResourceLocation id(String p){return new net.minecraft.resources.ResourceLocation("laowu",p);} }`,
    'cn/laowu/mod/CatClothesData.java': `package cn.laowu.mod; public class CatClothesData { public static CatOutfitType getOutfit(net.minecraft.world.entity.animal.Cat c){return c.outfit;} }`,
    'cn/laowu/mod/DynamiteCatLastStand.java': `package cn.laowu.mod; public class DynamiteCatLastStand { public static float whiteOverlayProgress(net.minecraft.world.entity.animal.Cat c,float p){return 0;} }`,
    'cn/laowu/mod/client/GiantCatAnimation.java': `package cn.laowu.mod.client; public class GiantCatAnimation { public static void clearCache(){} }`,
    'cn/laowu/mod/client/GiantCatVisualHeading.java': `package cn.laowu.mod.client; public class GiantCatVisualHeading { public static void clearCache(){} }`,
    'net/minecraft/world/entity/animal/Cat.java': `package net.minecraft.world.entity.animal; public class Cat { public cn.laowu.mod.CatOutfitType outfit; public boolean invisible; public boolean isInvisible(){return invisible;} public boolean isInvisibleTo(Object p){return invisible;} }`,
    'net/minecraft/client/Minecraft.java': `package net.minecraft.client;
public class Minecraft { public static java.nio.file.Path resources; public Object player=new Object(); public boolean glowing;
static final Minecraft INSTANCE=new Minecraft(); public static Minecraft getInstance(){return INSTANCE;} public boolean shouldEntityAppearGlowing(Object c){return glowing;}
public ResourceManager getResourceManager(){return new ResourceManager();} public static class ResourceManager { public Resource getResourceOrThrow(net.minecraft.resources.ResourceLocation l){return new Resource(l);} }
public record Resource(net.minecraft.resources.ResourceLocation location) { public java.io.Reader openAsReader() throws java.io.IOException {return java.nio.file.Files.newBufferedReader(resources.resolve("assets").resolve(location.namespace()).resolve(location.path()));} } }`,
    'com/mojang/logging/LogUtils.java': `package com.mojang.logging; public class LogUtils { public static org.slf4j.Logger getLogger(){return (org.slf4j.Logger)java.lang.reflect.Proxy.newProxyInstance(LogUtils.class.getClassLoader(),new Class[]{org.slf4j.Logger.class},(proxy,method,args)->{if(method.getName().equals("error"))throw new AssertionError(java.util.Arrays.toString(args));return method.getReturnType()==boolean.class?false:null;});} }`,
    'com/mojang/blaze3d/vertex/PoseStack.java': `package com.mojang.blaze3d.vertex;
public class PoseStack { public record Pose(org.joml.Matrix4f pose,org.joml.Matrix3f normal){} private final java.util.Deque<Pose> stack=new java.util.ArrayDeque<>();
public PoseStack(){stack.push(new Pose(new org.joml.Matrix4f(),new org.joml.Matrix3f()));} public void pushPose(){stack.push(new Pose(new org.joml.Matrix4f(last().pose),new org.joml.Matrix3f(last().normal)));} public void popPose(){stack.pop();}
public void translate(double x,double y,double z){last().pose.translate((float)x,(float)y,(float)z);} public void scale(float x,float y,float z){last().pose.scale(x,y,z);} public void mulPose(org.joml.Quaternionf q){last().pose.rotate(q);} public Pose last(){return stack.peek();} public int depth(){return stack.size();} }`,
    'com/mojang/blaze3d/vertex/VertexConsumer.java': `package com.mojang.blaze3d.vertex;
public class VertexConsumer { public final java.util.List<org.joml.Vector3f> vertices=new java.util.ArrayList<>();
public VertexConsumer vertex(org.joml.Matrix4f m,float x,float y,float z){vertices.add(m.transformPosition(new org.joml.Vector3f(x,y,z)));return this;}
public VertexConsumer addVertex(PoseStack.Pose p,float x,float y,float z){return vertex(p.pose(),x,y,z);} public VertexConsumer color(int a,int b,int c,int d){return this;} public VertexConsumer setColor(int a,int b,int c,int d){return this;}
public VertexConsumer uv(float a,float b){return this;} public VertexConsumer setUv(float a,float b){return this;} public VertexConsumer overlayCoords(int a){return this;} public VertexConsumer setOverlay(int a){return this;}
public VertexConsumer uv2(int a){return this;} public VertexConsumer setLight(int a){return this;} public VertexConsumer normal(org.joml.Matrix3f m,float x,float y,float z){return this;} public VertexConsumer setNormal(PoseStack.Pose p,float x,float y,float z){return this;} public void endVertex(){} }`,
    'net/minecraft/client/renderer/MultiBufferSource.java': `package net.minecraft.client.renderer; public class MultiBufferSource { public final com.mojang.blaze3d.vertex.VertexConsumer consumer=new com.mojang.blaze3d.vertex.VertexConsumer(); public com.mojang.blaze3d.vertex.VertexConsumer getBuffer(RenderType t){return consumer;} }`,
    'net/minecraft/client/renderer/RenderType.java': `package net.minecraft.client.renderer; public class RenderType { public static RenderType entityTranslucent(net.minecraft.resources.ResourceLocation l){return new RenderType();} public static RenderType entityCutoutNoCull(net.minecraft.resources.ResourceLocation l){return new RenderType();} public static RenderType outline(net.minecraft.resources.ResourceLocation l){return new RenderType();} public static RenderType itemEntityTranslucentCull(net.minecraft.resources.ResourceLocation l){return new RenderType();} }`,
    'net/minecraft/client/renderer/entity/LivingEntityRenderer.java': `package net.minecraft.client.renderer.entity; public class LivingEntityRenderer { public static int getOverlayCoords(net.minecraft.world.entity.animal.Cat c,float f){return 0;} }`,
    'cn/laowu/mod/client/GiantHeadOutfitProbe.java': `package cn.laowu.mod.client;
import cn.laowu.mod.*; import com.mojang.blaze3d.vertex.*; import net.minecraft.client.renderer.*; import org.joml.*; import java.util.*;
public class GiantHeadOutfitProbe {
static void check(boolean b,String s){if(!b)throw new AssertionError(s);} static void near(Vector3f a,Vector3f b,String s){check(a.distance(b)<.00002f,s+": "+a+" != "+b);}
static java.lang.reflect.Method render;
static void draw(net.minecraft.world.entity.animal.Cat c,PoseStack p,MultiBufferSource b,RuntimeBlockbenchModel.HeadMotion h,Map<String,RuntimeBlockbenchModel.GroupTransform> a) throws Exception {render.invoke(null,c,.5f,p,b,15728880,h,a);}
public static void main(String[] args) throws Exception {
net.minecraft.client.Minecraft.resources=java.nio.file.Path.of(args[0]);
try {render=Class.forName("cn.laowu.mod.client.GiantCatHeadOutfit").getDeclaredMethod("render",net.minecraft.world.entity.animal.Cat.class,float.class,PoseStack.class,MultiBufferSource.class,int.class,RuntimeBlockbenchModel.HeadMotion.class,Map.class);render.setAccessible(true);} catch(ClassNotFoundException e){throw new AssertionError("Giant cat must emit career head outfit geometry",e);}
// Some authoring meshes are planes; zero-area side faces intentionally emit no vertices.
int[] counts={0,80,32,24,80,24,40,96,24,56,72,64,40,72};
var head=new RuntimeBlockbenchModel.HeadMotion(.17f,-.31f,.12f);
var animation=Map.of("group2",RuntimeBlockbenchModel.GroupTransform.scaled(1,2,3,.11f,.23f,-.12f,1.1f,.9f,1.2f),"group5",RuntimeBlockbenchModel.GroupTransform.scaled(-2,1,-1,-.2f,.1f,.08f,.8f,1.2f,1),"head",RuntimeBlockbenchModel.GroupTransform.scaled(.3f,.4f,.5f,.1f,.15f,.2f,1.1f,1,1));
// Literal authored chain: root Y24 -> group2 -> group5 -> giant head Y8.
Matrix4f attachment=new Matrix4f().translate(1/16f,(24-7.31111f-2)/16f,(.33333f+3)/16f).rotate(new Quaternionf().rotationZYX(-.12f,.23f,.11f)).scale(1.1f,.9f,1.2f)
.translate(-2/16f,(7.31111f-8.82857f-1)/16f,(-1.46429f-.33333f-1)/16f).rotate(new Quaternionf().rotationZYX(.08f,.1f,-.2f)).scale(.8f,1.2f,1)
.translate(.3f/16f,(8.82857f-8-.4f)/16f,(-9+1.46429f+.5f)/16f).rotate(new Quaternionf().rotationZYX(.32f,-.16f,.27f)).scale(1.1f,1,1).translate(0,-15/16f,9/16f);
for(var outfit:CatOutfitType.values()) {
var cat=new net.minecraft.world.entity.animal.Cat();cat.outfit=outfit;var pose=new PoseStack();var buffer=new MultiBufferSource();draw(cat,pose,buffer,RuntimeBlockbenchModel.HeadMotion.NONE,Map.of());
check(buffer.consumer.vertices.size()==counts[outfit.ordinal()],outfit+" must emit only exact head meshes, no body/vehicle/reference texture: "+buffer.consumer.vertices.size()+" != "+counts[outfit.ordinal()]);check(pose.depth()==1,outfit+" leaves caller pose intact");
if(outfit==CatOutfitType.NONE)continue;
var source=new VertexConsumer();var definition=CatOutfitModels.get(outfit);var runtime=RuntimeBlockbenchModel.getCatOutfit(definition.model());
if(outfit==CatOutfitType.FLIGHT)runtime.renderFlightTexture(new PoseStack(),source,0,0,RuntimeBlockbenchModel.HeadMotion.NONE,Map.of(),true);
else runtime.renderTexture(new PoseStack(),source,0,0,outfit==CatOutfitType.TERMINATOR?RuntimeBlockbenchModel.GroupSelection.CAT_HEAD_ONLY:RuntimeBlockbenchModel.GroupSelection.CAT_HEAD_ONLY_PLAIN,RuntimeBlockbenchModel.HeadMotion.NONE,Map.of(),1);
for(int i=0;i<source.vertices.size();i++)near(buffer.consumer.vertices.get(i),new Vector3f(source.vertices.get(i)).add(0,1/16f,0),outfit+" neutral attachment keeps unchanged head size and lowers one authored pixel");
buffer=new MultiBufferSource();draw(cat,pose,buffer,head,animation);check(buffer.consumer.vertices.size()==counts[outfit.ordinal()],outfit+" animated mesh count");
for(int i=0;i<source.vertices.size();i++)near(buffer.consumer.vertices.get(i),attachment.transformPosition(new Vector3f(source.vertices.get(i))),outfit+" inherits both animated ancestors and summed live head rotation exactly once");
}
// Missing target must leave the caller pose untouched, not apply a partial root chain.
var giant=RuntimeBlockbenchModel.get(LaoWuMod.id("models/entity/giant_cat_mount.bbmodel"));var p=new PoseStack();
var attach=RuntimeBlockbenchModel.class.getMethod("translateToGroup",PoseStack.class,String.class,RuntimeBlockbenchModel.HeadMotion.class,Map.class);
check(!(boolean)attach.invoke(giant,p,"missing_head",head,animation),"missing target reports false");check(p.depth()==1 && p.last().pose().equals(new Matrix4f()),"missing target leaves pose untouched");
System.out.println("PASS: all 13 career head meshes, exact flight/Terminator exceptions, no body/vehicle/reference meshes, neutral pivot, live head plus full animated ancestry, balanced pose");
} }`
};
try {
    const stubPaths = Object.entries(sources).map(([name, content]) => {
        const file = path.join(temporary, name); fs.mkdirSync(path.dirname(file), {recursive:true}); fs.writeFileSync(file, content); return file;
    });
    for (const loader of ['forge-1.20.1','neoforge-1.21.1']) {
        const main = path.join(root, loader, 'src/main/java/cn/laowu/mod');
        const production = ['CatOutfitType.java','CatCombatRole.java','client/CatOutfitModels.java','client/RuntimeBlockbenchModel.java','client/GiantCatHeadOutfit.java']
            .map(name => path.join(main,name)).filter(file => fs.existsSync(file));
        const classes = path.join(temporary, loader); fs.mkdirSync(classes);
        execFileSync('javac',['-encoding','UTF-8','-cp',jars.join(path.delimiter),'-d',classes,...stubPaths,...production],{stdio:'inherit'});
        execFileSync('java',['-cp',[classes,...jars].join(path.delimiter),'cn.laowu.mod.client.GiantHeadOutfitProbe',path.join(root,loader,'src/main/resources')],{stdio:'inherit'});
    }
} finally {fs.rmSync(temporary,{recursive:true,force:true});}
