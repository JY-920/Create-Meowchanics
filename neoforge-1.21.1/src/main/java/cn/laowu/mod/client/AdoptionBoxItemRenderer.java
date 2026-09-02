package cn.laowu.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Item-context renderer for the exact adoption-box Blockbench model. */
public final class AdoptionBoxItemRenderer extends BlockEntityWithoutLevelRenderer {
    public AdoptionBoxItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5D, 0.5D, 0.5D);
        float scale;
        if (context == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
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
        // The complete authored geometry spans y=0..10 px, whose centre is 5 px.
        pose.translate(0.0D, 1.1875D, 0.0D);
        pose.scale(1.0F, -1.0F, 1.0F);
        RuntimeBlockbenchModel.get(AdoptionBoxRenderer.MODEL).render(pose,
                buffers.getBuffer(RenderType.entityCutoutNoCull(AdoptionBoxRenderer.TEXTURE)),
                light, overlay, RuntimeBlockbenchModel.GroupSelection.ALL,
                RuntimeBlockbenchModel.HeadMotion.NONE);
        pose.popPose();
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        RuntimeBlockbenchModel.clearCache();
    }
}

