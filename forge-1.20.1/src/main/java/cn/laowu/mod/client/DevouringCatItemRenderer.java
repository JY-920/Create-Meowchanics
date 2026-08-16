package cn.laowu.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class DevouringCatItemRenderer extends BlockEntityWithoutLevelRenderer {
    public DevouringCatItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        // RuntimeBlockbenchModel's Bedrock root pivot contributes another
        // -0.21875 block after the Y-axis conversion. It is most noticeable in
        // the larger GUI projection (about two inventory pixels), so compensate
        // that context without moving the already-correct held presentation.
        double verticalOffset = context == ItemDisplayContext.GUI
                ? 1.359375D
                : 1.140625D;
        pose.translate(0.5D, verticalOffset, 0.5D);
        pose.scale(1.0F, -1.0F, 1.0F);
        RuntimeBlockbenchModel.get(DevouringCatRenderer.MODEL).render(pose,
                buffers.getBuffer(RenderType.entityCutoutNoCull(DevouringCatRenderer.TEXTURE)),
                light, overlay, RuntimeBlockbenchModel.GroupSelection.ALL,
                RuntimeBlockbenchModel.HeadMotion.NONE);
        pose.popPose();
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        RuntimeBlockbenchModel.clearCache();
    }
}
