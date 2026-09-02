package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.create.AdoptionBoxBlock;
import cn.laowu.mod.create.AdoptionBoxBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** Static renderer preserving the supplied Blockbench hierarchy, UVs and decals. */
public final class AdoptionBoxRenderer implements BlockEntityRenderer<AdoptionBoxBlockEntity> {
    public static final ResourceLocation MODEL =
            LaoWuMod.id("models/block/adoption_box.bbmodel");
    public static final ResourceLocation TEXTURE =
            LaoWuMod.id("textures/block/adoption_box.png");

    public AdoptionBoxRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(AdoptionBoxBlockEntity box, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        Direction facing = box.getBlockState().getValue(AdoptionBoxBlock.FACING);
        pose.pushPose();
        pose.translate(0.5D, 1.5D, 0.5D);
        // The authored front is NORTH; rotate it to the state's outward face.
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        pose.scale(1.0F, -1.0F, 1.0F);
        RuntimeBlockbenchModel.get(MODEL).render(pose,
                buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                light, overlay, RuntimeBlockbenchModel.GroupSelection.ALL,
                RuntimeBlockbenchModel.HeadMotion.NONE);
        pose.popPose();
    }
}

