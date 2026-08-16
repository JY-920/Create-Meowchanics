package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.create.DevouringCatBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

public final class DevouringCatRenderer implements BlockEntityRenderer<DevouringCatBlockEntity> {
    static final ResourceLocation MODEL = LaoWuMod.id("models/block/devouring_cat.geo.json");
    static final ResourceLocation TEXTURE = LaoWuMod.id("textures/block/devouring_cat.png");

    public DevouringCatRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(DevouringCatBlockEntity blockEntity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        Direction facing = blockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        pose.pushPose();
        pose.translate(0.5D, 1.5D, 0.5D);
        // The supplied Bedrock model's face points toward local north.
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        // Bedrock geometry already uses Minecraft's left/right convention.
        // Mirroring X here swapped the two arms and mirrored every face UV.
        pose.scale(1.0F, -1.0F, 1.0F);
        RuntimeBlockbenchModel.get(MODEL).render(pose,
                buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay,
                RuntimeBlockbenchModel.GroupSelection.ALL, RuntimeBlockbenchModel.HeadMotion.NONE);
        pose.popPose();
    }
}
