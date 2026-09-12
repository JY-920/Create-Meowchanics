package cn.laowu.mod.client;

import cn.laowu.mod.ClientConfig;
import cn.laowu.mod.LaoWuMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;

/** Pet-only billboard art + atlas digits. Uses synchronised vanilla health; sends no render packets. */
public final class CatHealthBarRenderer {
    private static final ResourceLocation TEXTURE = LaoWuMod.id("textures/gui/cat_health_bar.png");

    public static void render(PoseStack pose, MultiBufferSource.BufferSource buffers, Camera camera, float partial) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || !ClientConfig.CAT_HEALTH_BARS.get()) return;
        RenderType type = RenderType.entityCutoutNoCull(TEXTURE);
        VertexConsumer out = buffers.getBuffer(type);
        for (Cat cat : mc.level.getEntitiesOfClass(Cat.class, mc.player.getBoundingBox().inflate(48),
                cat -> cat.isTame() && cat.isAlive() && !cat.isInvisibleTo(mc.player)
                        && cat.distanceToSqr(mc.player) <= 48 * 48)) {
            var position = cat.getPosition(partial);
            pose.pushPose();
            pose.translate(position.x, position.y + cat.getBbHeight() + .62, position.z);
            pose.mulPose(camera.rotation());
            pose.scale(.022F, .022F, .022F);
            draw(pose, out, cat.getHealth(), cat.getMaxHealth());
            pose.popPose();
        }
        buffers.endBatch(type);
    }

    private static void draw(PoseStack pose, VertexConsumer out, float health, float maximum) {
        float center = CatHealthBarLayout.width(health, maximum) / 2.0F;
        CatHealthBarLayout.draw(health, maximum, (x, y, width, height, u, v, sw, sh, z, tint) ->
                sprite(pose, out, x, y, width, height, u, v, sw, sh, z, center, tint));
    }

    private static void sprite(PoseStack pose, VertexConsumer out, float x, float y, float width, float height,
                               float u, float v, float sourceWidth, float sourceHeight, float z, float center, int tint) {
        float left = x - center, right = left + width, top = 6 - y, bottom = top - height;
        float u0 = u / 64, v0 = v / 64, u1 = (u + sourceWidth) / 64, v1 = (v + sourceHeight) / 64;
        vertex(pose, out, left, bottom, z, u0, v1, tint);
        vertex(pose, out, right, bottom, z, u1, v1, tint);
        vertex(pose, out, right, top, z, u1, v0, tint);
        vertex(pose, out, left, top, z, u0, v0, tint);
    }

    private static void vertex(PoseStack pose, VertexConsumer out, float x, float y, float z, float u, float v, int tint) {
        out.addVertex(pose.last().pose(), x, y, z).setColor(tint).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
    }
    private CatHealthBarRenderer() {}
}
