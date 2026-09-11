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
    private static boolean enabled;
    private static java.lang.ref.WeakReference<net.minecraft.client.multiplayer.ClientLevel> world = new java.lang.ref.WeakReference<>(null);
    private static long lastToggle;
    private static boolean toggleHeld;
    public static void toggle() {
        var mc = Minecraft.getInstance();
        long now = net.minecraft.Util.getMillis();
        if (toggleHeld || now - lastToggle < 250) return;
        toggleHeld = true;
        lastToggle = now;
        enabled = world.get() != mc.level || !enabled;
        world = new java.lang.ref.WeakReference<>(mc.level);
        if (mc.player != null) mc.player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(enabled
                ? "message.laowu.laser.preview_on" : "message.laowu.laser.preview_off"), true);
    }
    public static boolean visible(Entity entity) {
        var mc = Minecraft.getInstance();
        return enabled && world.get() == mc.level && mc.player != null
                && entity instanceof Cat cat && cat.isTame() && cat.isAlive()
                && cat.level() == mc.level && cat.distanceToSqr(mc.player) <= 4096;
    }
    public static void render(PoseStack pose, MultiBufferSource.BufferSource buffers,
                              net.minecraft.client.Camera camera, float partial) {
        var mc = Minecraft.getInstance();
        if (!mc.options.keyUse.isDown()) toggleHeld = false;
        if (!enabled || world.get() != mc.level || mc.player == null) return;
        var previewTypes = new java.util.LinkedHashSet<RenderType>();
        for (Cat cat : mc.level.getEntitiesOfClass(Cat.class, mc.player.getBoundingBox().inflate(64),
                CatTeamPreview::visible)) {
            var owner = cat.getOwnerUUID();
            if (owner == null) continue;
            var info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(owner);
            var skin = info != null ? info.getSkinLocation()
                    : net.minecraft.client.resources.DefaultPlayerSkin.getDefaultSkin(owner);
            var position = cat.getPosition(partial);
            pose.pushPose();
            // Raise the badge by 16 pixels at the portrait's 8-pixel / 0.38-block scale.
            pose.translate(position.x, position.y + cat.getBbHeight() + .60 + 16 * (.38 / 8), position.z);
            pose.mulPose(CatTeamPreviewFrame.facingCamera(camera.rotation()));
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
            out.vertex(pose.last().pose(), p[0], p[1], z).color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 255).endVertex();
        }
    }
    private static void skinQuad(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer out,
                                  float z, float u0, float v0, float u1, float v1) {
        for (float[] p : new float[][]{{-.19F,-.19F,u0,v1},{.19F,-.19F,u1,v1},
                {.19F,.19F,u1,v0},{-.19F,.19F,u0,v0}}) {
            out.vertex(pose.last().pose(), p[0], p[1], z).color(255,255,255,255).uv(p[2],p[3])
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(0,0,1).endVertex();
        }
    }
    private CatTeamPreview() {}
}
