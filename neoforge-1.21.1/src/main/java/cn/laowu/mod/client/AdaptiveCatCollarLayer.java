package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;

/**
 * Uses vanilla's inflated collar mesh with the cat's live pose. While hissing, the collar
 * texture is instead drawn on the same Blockbench geometry as the cat, so an
 * entity's remembered sitting pose cannot pull the collar away from the body.
 */
public final class AdaptiveCatCollarLayer extends RenderLayer<Cat, CatModel<Cat>> {
    private static final ResourceLocation HISSING_MODEL = ResourceLocation.fromNamespaceAndPath(
            LaoWuMod.MOD_ID, "models/entity/hissing_cat.bbmodel");
    private static final ResourceLocation COLLAR_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/entity/cat/cat_collar.png");

    private final ModelPart collarRoot;
    private final CatModel<Cat> collarModel;

    public AdaptiveCatCollarLayer(RenderLayerParent<Cat, CatModel<Cat>> parent, EntityModelSet modelSet) {
        super(parent);
        collarRoot = modelSet.bakeLayer(ModelLayers.CAT_COLLAR);
        collarModel = new CatModel<>(collarRoot);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Cat cat,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (cn.laowu.mod.CatPoseData.isPancake(cat) || !cat.isTame() || cat.isInvisible()) return;
        if (!(getParentModel() instanceof HissingCatModel model)) return;
        if (!model.isHissing()) {
            // Copy AFTER the parent has evaluated vanilla/custom animation, without
            // calling prepareMobModel/setupAnim again on the separate collar model.
            model.copyPoseTo(collarModel, collarRoot);
            renderColoredCutoutModel(collarModel, COLLAR_TEXTURE, poseStack, buffer,
                    packedLight, cat, cat.getCollarColor().getTextureDiffuseColor());
            return;
        }

        poseStack.pushPose();
        if (cat.isBaby()) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.translate(0.0D, 1.5D, 0.0D);
        }

        int color = cat.getCollarColor().getTextureDiffuseColor();
        int red = color >> 16 & 255;
        int green = color >> 8 & 255;
        int blue = color & 255;
        var vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(COLLAR_TEXTURE));
        RuntimeBlockbenchModel.get(HISSING_MODEL).render(
                poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY,
                RuntimeBlockbenchModel.GroupSelection.FRONT_BODY_ONLY,
                HissingCatGeometryLayer.headMotion(cat, model, ageInTicks),
                red, green, blue, 255);
        poseStack.popPose();
    }
}
