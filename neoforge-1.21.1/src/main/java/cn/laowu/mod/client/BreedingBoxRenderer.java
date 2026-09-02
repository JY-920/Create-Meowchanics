package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.create.BreedingBoxBlock;
import cn.laowu.mod.create.BreedingBoxBlockEntity;
import cn.laowu.mod.create.BreedingBoxTier;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** Static BER preserving the complete supplied Blockbench geometry and exact UVs. */
public final class BreedingBoxRenderer
        extends SmartBlockEntityRenderer<BreedingBoxBlockEntity> {
    public BreedingBoxRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    public static ResourceLocation model(BreedingBoxTier tier) {
        return LaoWuMod.id("models/block/" + tier.serializedName() + ".bbmodel");
    }

    public static ResourceLocation texture(BreedingBoxTier tier) {
        return LaoWuMod.id("textures/block/" + tier.serializedName() + ".png");
    }

    @Override
    protected void renderSafe(BreedingBoxBlockEntity box, float partialTick,
                              PoseStack pose, MultiBufferSource buffers,
                              int light, int overlay) {
        Direction facing = box.getBlockState().getValue(BreedingBoxBlock.FACING);
        pose.pushPose();
        // Direct-root Blockbench projects are evaluated around the renderer's
        // vanilla 24 px root pivot. Their y=0..16 block therefore reaches
        // runtime y=.5..1.5; reflecting around world y=1.5 maps it exactly to
        // block y=0..1 rather than burying the model below the floor.
        pose.translate(0.5D, 1.5D, 0.5D);
        // The authored model's visible front is NORTH, whereas the placement
        // state records the outward face. Add the missing half-turn so that
        // every horizontal placement presents its front to the placer.
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        pose.scale(1.0F, -1.0F, 1.0F);
        RuntimeBlockbenchModel.get(model(box.tier())).render(pose,
                buffers.getBuffer(RenderType.entityCutoutNoCull(texture(box.tier()))),
                light, overlay, RuntimeBlockbenchModel.GroupSelection.ALL,
                RuntimeBlockbenchModel.HeadMotion.NONE);
        pose.popPose();

        // Keep Create's native SmartBlockEntity overlays. This renders the
        // configured Cat Filter item into each sided value box; the previous
        // standalone BER drew only the cardboard model and skipped this pass.
        super.renderSafe(box, partialTick, pose, buffers, light, overlay);
    }
}
