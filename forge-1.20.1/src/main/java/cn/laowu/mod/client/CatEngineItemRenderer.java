package cn.laowu.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class CatEngineItemRenderer extends BlockEntityWithoutLevelRenderer {
    public CatEngineItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5D, 0.5D, 0.5D);
        if (context == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(315.0F));
            pose.scale(0.68F, 0.68F, 0.68F);
        } else if (context == ItemDisplayContext.GROUND) {
            // Ground items use the same quarter-block scale as vanilla block
            // items. The previous 0.52 scale made this two-level model appear
            // more than twice as large as nearby dropped blocks.
            pose.scale(0.25F, 0.25F, 0.25F);
        } else if (context == ItemDisplayContext.FIXED) {
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            pose.scale(0.5F, 0.5F, 0.5F);
        } else {
            pose.mulPose(Axis.YP.rotationDegrees(215.0F));
            pose.scale(0.4F, 0.4F, 0.4F);
        }

        // The authored shaft is hidden to avoid overlap in-world. Render the
        // same native Create shaft model in the item icon as the block renderer.
        pose.pushPose();
        // CachedBuffers.block() is authored in block coordinates while the
        // imported Blockbench model is centred half a block lower on every
        // axis. Apply the missing Y compensation as well, otherwise the shaft
        // floats eight pixels above the opening in the inventory icon.
        pose.translate(-0.5D, -0.5D, -0.5D);
        CachedBuffers.block(KineticBlockEntityRenderer.shaft(Direction.Axis.Z))
                .light(light)
                .overlay(overlay)
                .renderInto(pose, buffers.getBuffer(RenderType.solid()));
        pose.popPose();

        pose.translate(0.0D, 1.0D, 0.0D);
        pose.scale(1.0F, -1.0F, 1.0F);
        RuntimeBlockbenchModel.get(CatEngineRenderer.MODEL).render(pose,
                buffers.getBuffer(RenderType.entityCutoutNoCull(CatEngineRenderer.TEXTURE)),
                light, overlay, RuntimeBlockbenchModel.GroupSelection.ALL,
                RuntimeBlockbenchModel.HeadMotion.NONE,
                CatEngineRenderer.animationTransforms(0.0F));
        pose.popPose();
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        RuntimeBlockbenchModel.clearCache();
    }
}
