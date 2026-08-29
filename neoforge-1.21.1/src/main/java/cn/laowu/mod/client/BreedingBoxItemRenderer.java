package cn.laowu.mod.client;

import cn.laowu.mod.create.BreedingBoxBlock;
import cn.laowu.mod.create.BreedingBoxTier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Item-context companion to {@link BreedingBoxRenderer}. */
public final class BreedingBoxItemRenderer extends BlockEntityWithoutLevelRenderer {
    public BreedingBoxItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        BreedingBoxTier tier = stack.getItem() instanceof BlockItem item
                && item.getBlock() instanceof BreedingBoxBlock box
                ? box.tier() : BreedingBoxTier.BASIC;

        pose.pushPose();
        pose.translate(0.5D, 0.5D, 0.5D);
        float scale;
        if (context == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            // Match the established GUI scale used by the other full-block
            // machinery items; centring remains geometry-derived below.
            scale = 0.63F;
        } else if (context == ItemDisplayContext.GROUND) {
            scale = 0.24F;
        } else if (context == ItemDisplayContext.FIXED) {
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            scale = 0.47F;
        } else {
            pose.mulPose(Axis.YP.rotationDegrees(215.0F));
            scale = 0.38F;
        }
        pose.scale(scale, scale, scale);
        // Centre item contexts against the ordinary 0..16 px block body, not
        // the decorative ears/flaps that extend to y=20. Direct-root projects
        // use vanilla's 24 px root pivot, so the body centre maps to
        // 1.5 - 8/16 = 1.0. Including the ears in the bounds would make the
        // solid box body appear slightly low even when the full mesh is drawn.
        pose.translate(0.0D, 1.0D, 0.0D);
        pose.scale(1.0F, -1.0F, 1.0F);
        RuntimeBlockbenchModel.get(BreedingBoxRenderer.model(tier)).render(pose,
                buffers.getBuffer(RenderType.entityCutoutNoCull(
                        BreedingBoxRenderer.texture(tier))),
                light, overlay, RuntimeBlockbenchModel.GroupSelection.ALL,
                RuntimeBlockbenchModel.HeadMotion.NONE);
        pose.popPose();
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        RuntimeBlockbenchModel.clearCache();
    }
}
