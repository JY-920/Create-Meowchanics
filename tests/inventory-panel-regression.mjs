import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

// Compile the real renderer. Only the external drawing boundary is recorded.
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const dir=fs.mkdtempSync(path.join(os.tmpdir(),'meow-inventory-'));
const executable=name=>process.env.JAVA_HOME?path.join(process.env.JAVA_HOME,'bin',name+'.exe'):name;
const fixtures={
  'cn/laowu/mod/compat/create/CreateIntegration.java':`package cn.laowu.mod.compat.create; public class CreateIntegration { public static boolean loaded; public static boolean isLoaded(){return loaded;} }`,
  'cn/laowu/mod/compat/create/CreateClientEvents.java':`package cn.laowu.mod.compat.create; public class CreateClientEvents { public static void renderPlayerInventory(net.minecraft.client.gui.GuiGraphics g,int x,int y){g.calls.add("create:"+x+":"+y);} }`,
  'net/minecraft/resources/ResourceLocation.java':`package net.minecraft.resources; public record ResourceLocation(String namespace,String path) { public static ResourceLocation fromNamespaceAndPath(String n,String p){return new ResourceLocation(n,p);} }`,
  'net/minecraft/client/gui/GuiGraphics.java':`package net.minecraft.client.gui; public class GuiGraphics { public final java.util.List<String> calls=new java.util.ArrayList<>(); public void fill(int x,int y,int a,int b,int c){calls.add("fill");} public void blit(net.minecraft.resources.ResourceLocation r,int x,int y,float u,float v,int w,int h,int tw,int th){calls.add(r.namespace()+":"+r.path()+":"+x+":"+y+":"+(int)u+":"+(int)v+":"+w+":"+h+":"+tw+":"+th);} }`,
  'InventoryRegression.java':`import cn.laowu.mod.client.CatClientTextures; import cn.laowu.mod.compat.create.CreateIntegration; import net.minecraft.client.gui.GuiGraphics; public class InventoryRegression { public static void main(String[] args) { var g=new GuiGraphics(); CatClientTextures.renderPlayerInventory(g,31,94); var expected=java.util.List.of("minecraft:textures/gui/container/generic_54.png:31:94:0:0:176:4:256:256","minecraft:textures/gui/container/generic_54.png:31:98:0:126:176:96:256:256"); if(!g.calls.equals(expected))throw new AssertionError("Standalone panel must keep the vanilla top border/corners and slot alignment: "+g.calls); g.calls.clear(); CreateIntegration.loaded=true; CatClientTextures.renderPlayerInventory(g,31,94); if(!g.calls.equals(java.util.List.of("create:31:94")))throw new AssertionError("Create inventory path changed"); System.out.println("PASS: complete inventory cap, unchanged slots and original Create renderer"); } }`
};
for(const [name,source] of Object.entries(fixtures)) {const file=path.join(dir,name);fs.mkdirSync(path.dirname(file),{recursive:true});fs.writeFileSync(file,source);}
for(const port of ['forge-1.20.1','neoforge-1.21.1']) {
  execFileSync(executable('javac'),['-d',dir,...Object.keys(fixtures).map(name=>path.join(dir,name)),path.join(root,port,'src/main/java/cn/laowu/mod/client/CatClientTextures.java')]);
  const result=execFileSync(executable('java'),['-cp',dir,'InventoryRegression'],{encoding:'utf8'});
  assert.match(result,/PASS:/); console.log(port+': '+result.trim());
}
