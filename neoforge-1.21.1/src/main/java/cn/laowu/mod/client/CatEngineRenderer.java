package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.create.CatEngineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public final class CatEngineRenderer extends ShaftRenderer<CatEngineBlockEntity> {
    static final ResourceLocation MODEL = LaoWuMod.id("models/block/cat_engine.bbmodel");
    static final ResourceLocation TEXTURE = LaoWuMod.id("textures/block/cat_engine.png");

    public CatEngineRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(CatEngineBlockEntity engine, float partialTicks, PoseStack pose,
                              MultiBufferSource buffers, int light, int overlay) {
        float seconds = 0.0F;
        if (engine.isActive() && engine.getLevel() != null) {
            float animationTicks = ((engine.getLevel().getGameTime() % 40L) * 5.0F
                    + partialTicks * 5.0F) % 40.0F;
            seconds = animationTicks / 20.0F;
        }

        pose.pushPose();
        Direction facing = engine.getBlockState().getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
        pose.translate(0.5D, 1.5D, 0.5D);
        // Blockbench's Z axis is mirrored by this runtime renderer. Negating
        // the yaw preserves north/south and fixes east/west front orientation.
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        pose.scale(1.0F, -1.0F, 1.0F);
        RuntimeBlockbenchModel.get(MODEL).render(pose,
                buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay,
                RuntimeBlockbenchModel.GroupSelection.ALL,
                RuntimeBlockbenchModel.HeadMotion.NONE, animationTransforms(seconds));
        pose.popPose();

        super.renderSafe(engine, partialTicks, pose, buffers, light, overlay);
    }

    static Map<String, RuntimeBlockbenchModel.GroupTransform> animationTransforms(float seconds) {
        float triangle = seconds <= 1.0F ? seconds : 2.0F - seconds;
        return Map.of(
                "bone5", RuntimeBlockbenchModel.GroupTransform.position(-2.0F * triangle, 0.0F, 0.0F),
                "bone6", RuntimeBlockbenchModel.GroupTransform.position(-2.0F + 2.0F * triangle, 0.0F, 0.0F),
                "bone7", RuntimeBlockbenchModel.GroupTransform.position(2.0F - 2.0F * triangle, 0.0F, 0.0F),
                "bone8", RuntimeBlockbenchModel.GroupTransform.position(2.0F * triangle, 0.0F, 0.0F),
                // The model's own rod overlaps Create's RPM-driven shaft renderer.
                "bone10", RuntimeBlockbenchModel.GroupTransform.HIDDEN);
    }
}
