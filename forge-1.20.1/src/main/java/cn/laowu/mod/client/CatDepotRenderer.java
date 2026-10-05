package cn.laowu.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Rotate Create's item rendering with the authored 13-pixel tabletop. */
public final class CatDepotRenderer extends DepotRenderer {
    public CatDepotRenderer(BlockEntityRendererProvider.Context context) { super(context); }

    @Override protected void renderSafe(DepotBlockEntity depot, float partialTicks, PoseStack pose,
                                        MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        try {
            pose.translate(.5,.5,.5);
            cn.laowu.mod.create.CatMachineOrientation.rotatePose(pose,cn.laowu.mod.create.CatMachineOrientation.bottom(depot.getBlockState()));
            pose.translate(-.5,-.5,-.5);
            super.renderSafe(depot, partialTicks, pose, buffers, light, overlay);
        } finally {
            pose.popPose();
        }
    }
}
