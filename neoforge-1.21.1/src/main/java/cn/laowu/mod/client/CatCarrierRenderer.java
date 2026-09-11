package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.create.CatCarrierBlock;
import cn.laowu.mod.create.CatCarrierBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** Static renderer preserving the supplied Blockbench hierarchy, UVs and decals. */
public final class CatCarrierRenderer implements BlockEntityRenderer<CatCarrierBlockEntity> {
    public static final ResourceLocation MODEL =
            LaoWuMod.id("models/block/cat_carrier.bbmodel");
    public static final ResourceLocation TEXTURE =
            LaoWuMod.id("textures/block/cat_carrier.png");

    public CatCarrierRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(CatCarrierBlockEntity box, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        Direction facing = box.getBlockState().getValue(CatCarrierBlock.FACING);
        pose.pushPose();
        pose.translate(0.5D, 1.5D, 0.5D);
        // The authored front is NORTH; rotate it to the state's outward face.
        pose.mulPose(Axis.YP.rotationDegrees(90.0F - facing.toYRot()));
        pose.scale(1.0F, -1.0F, 1.0F);
        RuntimeBlockbenchModel.get(MODEL).render(pose,
                buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                light, overlay, RuntimeBlockbenchModel.GroupSelection.ALL,
                RuntimeBlockbenchModel.HeadMotion.NONE);
        pose.popPose();
        CarrierCatPreview.render(box, partialTick, pose, buffers, light);
    }
}
