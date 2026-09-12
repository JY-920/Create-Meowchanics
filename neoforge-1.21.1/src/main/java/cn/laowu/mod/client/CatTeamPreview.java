package cn.laowu.mod.client;

import cn.laowu.mod.CatTeamRules;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Local preview only: no scoreboard edits, shared glowing flags or owner highlights. */
public final class CatTeamPreview {
    public static boolean enabled() { return cn.laowu.mod.ClientConfig.CAT_TEAM_PREVIEW.get(); }
    public static void toggle() {
        cn.laowu.mod.ClientConfig.CAT_TEAM_PREVIEW.set(!enabled());
        cn.laowu.mod.ClientConfig.SPEC.save();
    }
    public static boolean visible(Entity entity) {
        var mc = Minecraft.getInstance();
        return enabled() && !mc.options.hideGui && mc.player != null
                && entity instanceof Cat cat && cat.isTame() && cat.isAlive()
                && cat.level() == mc.level && cat.distanceToSqr(mc.player) <= 4096;
    }
    public static void render(PoseStack pose, MultiBufferSource.BufferSource buffers,
                              net.minecraft.client.Camera camera, float partial) {
        var mc = Minecraft.getInstance();
        if (!enabled() || mc.options.hideGui || mc.level == null || mc.player == null) return;
        var previewTypes = new java.util.LinkedHashSet<RenderType>();
        for (Cat cat : mc.level.getEntitiesOfClass(Cat.class, mc.player.getBoundingBox().inflate(64),
                CatTeamPreview::visible)) {
            var owner = cat.getOwnerUUID();
            if (owner == null) continue;
            var info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(owner);
            var skin = info != null ? info.getSkin().texture()
                    : net.minecraft.client.resources.DefaultPlayerSkin.get(owner).texture();
            var position = cat.getPosition(partial);
            pose.pushPose();
            // Raise the badge by 16 pixels at the portrait's 8-pixel / 0.38-block scale.
            pose.translate(position.x, position.y + cat.getBbHeight() + .60 + 16 * (.38 / 8), position.z);
            pose.mulPose(camera.rotation());
            int colour = CatTeamRules.rgb(cat);
            var frameType = RenderType.debugQuads();
            previewTypes.add(frameType);
            var frame = buffers.getBuffer(frameType);
            colourQuad(pose, frame, -.24F, -.24F, .24F, .24F, 0, colour);
            // Face plus hat overlay using the owner's skin, full brightness.
            var faceType = RenderType.entityCutoutNoCull(skin);
            previewTypes.add(faceType);
            var face = buffers.getBuffer(faceType);
            skinQuad(pose, face, .002F, 8F/64, 8F/64, 16F/64, 16F/64);
            skinQuad(pose, face, .004F, 40F/64, 8F/64, 48F/64, 16F/64);
            pose.popPose();
        }
        // AFTER_PARTICLES has no later entity pass to flush the final owner's skin.
        // Submit only our types; endBatch(lines) in the caller cannot submit faces.
        for (var type : previewTypes) buffers.endBatch(type);
    }
    private static void colourQuad(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer out,
                                    float left, float bottom, float right, float top, float z, int rgb) {
        for (float[] p : new float[][]{{left,bottom},{right,bottom},{right,top},{left,top}}) {
            out.addVertex(pose.last().pose(), p[0], p[1], z).setColor(0xFF000000 | rgb);
        }
    }
    private static void skinQuad(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer out,
                                  float z, float u0, float v0, float u1, float v1) {
        for (float[] p : new float[][]{{-.19F,-.19F,u0,v1},{.19F,-.19F,u1,v1},
                {.19F,.19F,u1,v0},{-.19F,.19F,u0,v0}}) {
            out.addVertex(pose.last().pose(), p[0], p[1], z).setColor(-1).setUv(p[2],p[3])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0,0,1);
        }
    }
    private CatTeamPreview() {}
}
