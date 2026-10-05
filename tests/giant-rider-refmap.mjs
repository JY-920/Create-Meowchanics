import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';

// Exercise Mixin's actual ReferenceMapper, not a string-presence check.
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const cache = path.join(os.homedir(), '.gradle/caches/modules-2/files-2.1');
function jar(dir) {
  for (const entry of fs.readdirSync(dir, {withFileTypes: true})) {
    const candidate = path.join(dir, entry.name);
    if (entry.isDirectory()) { const found = jar(candidate); if(found) return found; }
    else if (entry.name.endsWith('.jar') && !/sources|javadoc/.test(entry.name)) return candidate;
  }
}
const cp = [jar(path.join(cache, 'org.spongepowered/mixin/0.8.5')),
  jar(path.join(cache, 'com.google.code.gson/gson/2.10.1')),
  jar(path.join(cache, 'com.google.guava/guava'))].join(path.delimiter);
const temp = fs.mkdtempSync(path.join(os.tmpdir(), 'giant-rider-map-'));
try {
  const source = path.join(temp, 'GiantRiderRefmapProbe.java');
  fs.writeFileSync(source, `import java.io.*;
import org.spongepowered.asm.mixin.refmap.ReferenceMapper;
public class GiantRiderRefmapProbe {
  public static void main(String[] args) throws Exception {
    var mapper = ReferenceMapper.read(new FileReader(args[0]), "laowu-performance.refmap.json");
    String[][] cases = {
      {"CatBasinOrientationMixin", "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;", "Lnet/minecraft/world/level/Level;m_7702_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"},
      {"CatBasinOrientationMixin", "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", "Lnet/minecraft/world/level/Level;m_8055_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"},
      {"CatBasinParticlesMixin", "Lnet/minecraft/world/level/Level;addAlwaysVisibleParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V", "Lnet/minecraft/world/level/Level;m_7107_(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"},
      {"CatDepotProcessingMixin", "Lnet/minecraft/core/BlockPos;above(I)Lnet/minecraft/core/BlockPos;", "Lnet/minecraft/core/BlockPos;m_6630_(I)Lnet/minecraft/core/BlockPos;"},
      {"DivingCapeLayerMixin", "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V", "Lnet/minecraft/client/renderer/entity/layers/CapeLayer;m_6494_(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V"},
      {"DivingCapeLayerMixin", "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", "Lcom/mojang/blaze3d/vertex/PoseStack;m_85836_()V"},
      {"GiantCatRiderInventoryMixin", "renderEntityInInventory(Lnet/minecraft/client/gui/GuiGraphics;IIILorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/world/entity/LivingEntity;)V", "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;m_280432_(Lnet/minecraft/client/gui/GuiGraphics;IIILorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/world/entity/LivingEntity;)V"},
      {"GiantCatRiderFacingMixin", "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;m_7392_(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      {"GiantCatRiderOffsetMixin", "getRenderOffset(Lnet/minecraft/client/player/AbstractClientPlayer;F)Lnet/minecraft/world/phys/Vec3;", "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;m_7860_(Lnet/minecraft/client/player/AbstractClientPlayer;F)Lnet/minecraft/world/phys/Vec3;"},
      {"GiantCatRiderCameraMixin", "setup", "Lnet/minecraft/client/Camera;m_90575_(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V"},
      {"GiantCatRiderCameraMixin", "Lnet/minecraft/client/Camera;setPosition(DDD)V", "Lnet/minecraft/client/Camera;m_90584_(DDD)V"}
    };
    for (String context : new String[]{null, "searge"}) for (String[] c : cases) {
      String got = mapper.remapWithContext(context, "cn/laowu/mod/mixin/"+c[0], c[1]);
      if (!got.equals(c[2])) throw new AssertionError("Forge release mapping: "+c[0]+" / "+c[1]+" -> "+got);
    }
    System.out.println("PASS: real Mixin ReferenceMapper resolves rider, cape and cat-machine selectors to Forge SRG targets");
  }
}`);
  execFileSync('javac', ['-proc:none', '-cp', cp, '-d', temp, source], {stdio: 'pipe'});
  process.stdout.write(execFileSync('java', ['-cp', cp+path.delimiter+temp, 'GiantRiderRefmapProbe',
    path.join(root, 'forge-1.20.1/src/main/resources/laowu-performance.refmap.json')], {encoding: 'utf8'}));
} finally { fs.rmSync(temp, {recursive: true, force: true}); }
