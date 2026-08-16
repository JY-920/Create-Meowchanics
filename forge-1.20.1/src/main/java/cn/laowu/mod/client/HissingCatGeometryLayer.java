package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Cat;

public final class HissingCatGeometryLayer extends RenderLayer<Cat, CatModel<Cat>> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            LaoWuMod.MOD_ID, "models/entity/hissing_cat.bbmodel");

    public HissingCatGeometryLayer(RenderLayerParent<Cat, CatModel<Cat>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Cat cat,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!(getParentModel() instanceof HissingCatModel model) || !model.isHissing()) return;

        poseStack.pushPose();
        if (cat.isBaby()) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.translate(0.0D, 1.5D, 0.0D);
        }

        // Keep the authored head pitch, turn the face 30 degrees by default, and
        // visibly sway only the head around that angle. Vanilla yaw tracking remains.
        RuntimeBlockbenchModel.HeadMotion headMotion = headMotion(cat, model, ageInTicks);
        var vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(cat.getVariant().texture()));
        RuntimeBlockbenchModel.get(MODEL).render(
                poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY,
                RuntimeBlockbenchModel.GroupSelection.ALL, headMotion);
        poseStack.popPose();
    }

    static RuntimeBlockbenchModel.HeadMotion headMotion(Cat cat, HissingCatModel model, float ageInTicks) {
        float roll = (30.0F + Mth.sin(ageInTicks * 0.12F + cat.getId() * 0.73F) * 6.0F)
                * Mth.DEG_TO_RAD;
        return new RuntimeBlockbenchModel.HeadMotion(0.0F, model.liveHeadYRot(), roll);
    }
}
