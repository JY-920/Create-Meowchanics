package cn.laowu.mod.client;

import cn.laowu.mod.CatClothesData;
import cn.laowu.mod.CatPoseData;
import cn.laowu.mod.CatOutfitType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.animal.Cat;

/** Draws the wearable texture layers from the supplied cat-clothes Blockbench project. */
public final class CatClothesLayer extends RenderLayer<Cat, CatModel<Cat>> {
    public CatClothesLayer(RenderLayerParent<Cat, CatModel<Cat>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Cat cat,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        CatOutfitType outfit = CatClothesData.getOutfit(cat);
        CatOutfitModels.Definition definition = CatOutfitModels.get(outfit);
        if (definition == null || CatPoseData.isPancake(cat)
                || outfit == CatOutfitType.TRANSPORT
                || !(getParentModel() instanceof HissingCatModel model)) return;

        int overlay = LivingEntityRenderer.getOverlayCoords(cat, 0.0F);
        var transforms = model.catOutfitTransforms(outfit);
        renderTextureLayer(poseStack, buffer, packedLight, overlay, cat, model, outfit,
                definition, transforms, definition.texture(), 1);
        if (definition.translucentTexture() != null) {
            renderTextureLayer(poseStack, buffer, packedLight, overlay, cat, model, outfit,
                    definition, transforms, definition.translucentTexture(), 2);
        }
    }

    private static void renderTextureLayer(
            PoseStack poseStack, MultiBufferSource buffer, int packedLight, int overlay,
            Cat cat, HissingCatModel model, CatOutfitType outfit,
            CatOutfitModels.Definition definition,
            java.util.Map<String, RuntimeBlockbenchModel.GroupTransform> transforms,
            net.minecraft.resources.ResourceLocation texture, int textureIndex) {
        var consumer = buffer.getBuffer(outfit == CatOutfitType.HONEY
                ? RenderType.entityTranslucent(texture)
                : RenderType.entityCutoutNoCull(texture));

        if (outfit == CatOutfitType.FLIGHT) {
            var runtime = RuntimeBlockbenchModel.getCatOutfit(definition.model());
            java.util.Map<String, RuntimeBlockbenchModel.GroupTransform> helmetTransforms =
                    new java.util.HashMap<>(transforms);
            helmetTransforms.put("group", model.flightHelmetTransform());

            if (!cat.isBaby()) {
                // Aircraft/body section follows the body; only the exact
                // helmet mesh follows the live head pivot.
                runtime.renderFlightTexture(poseStack, consumer, packedLight, overlay,
                        RuntimeBlockbenchModel.HeadMotion.NONE, transforms, false);
                runtime.renderFlightTexture(poseStack, consumer, packedLight, overlay,
                        RuntimeBlockbenchModel.HeadMotion.NONE,
                        java.util.Map.copyOf(helmetTransforms), true);
                return;
            }

            poseStack.pushPose();
            poseStack.scale(0.75F, 0.75F, 0.75F);
            poseStack.translate(0.0D, 10.0D / 16.0D, 4.0D / 16.0D);
            runtime.renderFlightTexture(poseStack, consumer, packedLight, overlay,
                    RuntimeBlockbenchModel.HeadMotion.NONE,
                    java.util.Map.copyOf(helmetTransforms), true);
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.translate(0.0D, 1.5D, 0.0D);
            runtime.renderFlightTexture(poseStack, consumer, packedLight, overlay,
                    RuntimeBlockbenchModel.HeadMotion.NONE, transforms, false);
            poseStack.popPose();
            return;
        }

        if (!cat.isBaby()) {
            RuntimeBlockbenchModel.getCatOutfit(definition.model()).renderTexture(
                    poseStack, consumer, packedLight, overlay,
                    RuntimeBlockbenchModel.GroupSelection.ALL_GROUPS,
                    RuntimeBlockbenchModel.HeadMotion.NONE, transforms, textureIndex);
            return;
        }

        // Vanilla OcelotModel renders a kitten's head at 3/4 scale and its
        // body at 1/2 scale. Mirror those transforms for the outfit only; the
        // parent renderer remains solely responsible for the vanilla cat.
        poseStack.pushPose();
        poseStack.scale(0.75F, 0.75F, 0.75F);
        poseStack.translate(0.0D, 10.0D / 16.0D, 4.0D / 16.0D);
        RuntimeBlockbenchModel.getCatOutfit(definition.model()).renderTexture(
                poseStack, consumer, packedLight, overlay,
                definition.genericRootFollowsHead()
                        ? RuntimeBlockbenchModel.GroupSelection.CAT_HEAD_ONLY
                        : RuntimeBlockbenchModel.GroupSelection.CAT_HEAD_ONLY_PLAIN,
                RuntimeBlockbenchModel.HeadMotion.NONE, transforms, textureIndex);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(0.0D, 1.5D, 0.0D);
        RuntimeBlockbenchModel.getCatOutfit(definition.model()).renderTexture(
                poseStack, consumer, packedLight, overlay,
                definition.genericRootFollowsHead()
                        ? RuntimeBlockbenchModel.GroupSelection.CAT_BODY_ONLY
                        : RuntimeBlockbenchModel.GroupSelection.CAT_BODY_WITH_GENERIC,
                RuntimeBlockbenchModel.HeadMotion.NONE, transforms, textureIndex);
        poseStack.popPose();
    }
}
