package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.create.WishAdoptionBoxBlock;
import cn.laowu.mod.create.WishAdoptionBoxBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Places a flat, atlas-backed reward sprite directly over both faces of the authored white card. */
public final class WishAdoptionBoxRenderer implements BlockEntityRenderer<WishAdoptionBoxBlockEntity> {
    public static final ResourceLocation MODEL = LaoWuMod.id("models/block/wish_adoption_box.bbmodel");
    public static final ResourceLocation TEXTURE = LaoWuMod.id("textures/block/wish_adoption_box.png");
    public WishAdoptionBoxRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(WishAdoptionBoxBlockEntity box, float partialTick, PoseStack pose,
                                  MultiBufferSource buffers, int light, int overlay) {
        Direction facing = box.getBlockState().getValue(WishAdoptionBoxBlock.FACING);
        pose.pushPose();
        try {
            pose.translate(.5, 1.5, .5);
            pose.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
            pose.scale(1, -1, 1);
            RuntimeBlockbenchModel.get(MODEL).render(pose,
                    buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay,
                    RuntimeBlockbenchModel.GroupSelection.ALL, RuntimeBlockbenchModel.HeadMotion.NONE);
            renderReward(box.rewardPreview(), pose, buffers, light, overlay);
        } finally { pose.popPose(); }
    }
    public static void renderReward(ItemStack reward, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (reward.isEmpty()) return;
        var mc = Minecraft.getInstance();
        var sprite = mc.getItemRenderer().getModel(reward, mc.level, null, 0).getParticleIcon();
        VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        float half = 2.5F / 16, top = (24 - 8.5F) / 16, bottom = (24 - 3.5F) / 16;
        // Model card: x=-6..6, y=2..10, z=7.5..8.5. Tiny offsets prevent z-fighting.
        for (int side : new int[]{-1, 1}) {
            float z = (side < 0 ? 7.48F : 8.52F) / 16;
            float left = -side * half, right = side * half;
            vertex(out, pose, left, top, z, sprite.getU0(), sprite.getV0(), light, overlay, side);
            vertex(out, pose, left, bottom, z, sprite.getU0(), sprite.getV1(), light, overlay, side);
            vertex(out, pose, right, bottom, z, sprite.getU1(), sprite.getV1(), light, overlay, side);
            vertex(out, pose, right, top, z, sprite.getU1(), sprite.getV0(), light, overlay, side);
        }
    }
    private static void vertex(VertexConsumer out, PoseStack pose, float x, float y, float z,
                               float u, float v, int light, int overlay, int side) {
        out.vertex(pose.last().pose(), x, y, z).color(255, 255, 255, 255)
                .uv(u, v).overlayCoords(overlay).uv2(light).normal(pose.last().normal(), 0, 0, side).endVertex();
    }
}
