package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CatCollarLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Cat;

/**
 * Uses vanilla's collar layer for normal cats. While hissing, the collar
 * texture is instead drawn on the same Blockbench geometry as the cat, so an
 * entity's remembered sitting pose cannot pull the collar away from the body.
 */
public final class AdaptiveCatCollarLayer extends RenderLayer<Cat, CatModel<Cat>> {
    private static final ResourceLocation HISSING_MODEL = ResourceLocation.fromNamespaceAndPath(
            LaoWuMod.MOD_ID, "models/entity/hissing_cat.bbmodel");
    private static final ResourceLocation COLLAR_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/entity/cat/cat_collar.png");

    private final CatCollarLayer vanillaLayer;

    public AdaptiveCatCollarLayer(RenderLayerParent<Cat, CatModel<Cat>> parent, EntityModelSet modelSet) {
        super(parent);
        vanillaLayer = new CatCollarLayer(parent, modelSet);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Cat cat,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (cn.laowu.mod.CatPoseData.isPancake(cat)) return;
        if (!(getParentModel() instanceof HissingCatModel model) || !model.isHissing()) {
            vanillaLayer.render(poseStack, buffer, packedLight, cat, limbSwing, limbSwingAmount,
                    partialTick, ageInTicks, netHeadYaw, headPitch);
            return;
        }
        if (!cat.isTame()) return;

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
