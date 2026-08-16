package cn.laowu.mod.client;

import cn.laowu.mod.CatClothesData;
import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.CatPoseData;
import cn.laowu.mod.LaoWuMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;

import java.util.Map;

/** Renders the authored normal and hissing transport-cat equipment. */
public final class CatChestLayer extends RenderLayer<Cat, CatModel<Cat>> {
    private static final ResourceLocation NORMAL_MODEL = ResourceLocation.fromNamespaceAndPath(
            LaoWuMod.MOD_ID, "models/entity/transport_suit.bbmodel");
    private static final ResourceLocation HISSING_MODEL = ResourceLocation.fromNamespaceAndPath(
            LaoWuMod.MOD_ID, "models/entity/transport_suit_hissing.bbmodel");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            LaoWuMod.MOD_ID, "textures/entity/transport_suit.png");
    private static final int OUTFIT_TEXTURE = 1;

    public CatChestLayer(RenderLayerParent<Cat, CatModel<Cat>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Cat cat,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (CatClothesData.getOutfit(cat) != CatOutfitType.TRANSPORT
                || CatPoseData.isPancake(cat)
                || !(getParentModel() instanceof HissingCatModel model)) return;

        var consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        if (CatPoseData.isHissing(cat)) {
            renderHissing(poseStack, consumer, packedLight, cat, model, ageInTicks);
        } else {
            renderNormal(poseStack, consumer, packedLight, cat, model);
        }
    }

    private static void renderHissing(PoseStack poseStack,
                                      com.mojang.blaze3d.vertex.VertexConsumer consumer,
                                      int packedLight, Cat cat, HissingCatModel model,
                                      float ageInTicks) {
        poseStack.pushPose();
        if (cat.isBaby()) {
            // The authored hissing kitten is uniformly half-sized, matching
            // HissingCatGeometryLayer rather than vanilla's large kitten head.
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.translate(0.0D, 1.5D, 0.0D);
        }
        RuntimeBlockbenchModel.getCatOutfit(HISSING_MODEL).renderTexture(
                poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                RuntimeBlockbenchModel.GroupSelection.ALL_GROUPS,
                HissingCatGeometryLayer.headMotion(cat, model, ageInTicks),
                Map.of(), OUTFIT_TEXTURE);
        poseStack.popPose();
    }

    private static void renderNormal(PoseStack poseStack,
                                     com.mojang.blaze3d.vertex.VertexConsumer consumer,
                                     int packedLight, Cat cat, HissingCatModel model) {
        RuntimeBlockbenchModel runtime = RuntimeBlockbenchModel.getCatOutfit(NORMAL_MODEL);
        Map<String, RuntimeBlockbenchModel.GroupTransform> transforms =
                model.catOutfitTransforms(CatOutfitType.TRANSPORT);
        if (!cat.isBaby()) {
            runtime.renderTexture(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                    RuntimeBlockbenchModel.GroupSelection.ALL_GROUPS,
                    RuntimeBlockbenchModel.HeadMotion.NONE, transforms, OUTFIT_TEXTURE);
            return;
        }

        // Match vanilla's separate kitten head/body scaling so the visor stays
        // on the enlarged head while both cargo boxes follow the smaller body.
        poseStack.pushPose();
        poseStack.scale(0.75F, 0.75F, 0.75F);
        poseStack.translate(0.0D, 10.0D / 16.0D, 4.0D / 16.0D);
        runtime.renderTexture(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                RuntimeBlockbenchModel.GroupSelection.CAT_HEAD_ONLY_PLAIN,
                RuntimeBlockbenchModel.HeadMotion.NONE, transforms, OUTFIT_TEXTURE);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(0.0D, 1.5D, 0.0D);
        runtime.renderTexture(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                RuntimeBlockbenchModel.GroupSelection.CAT_BODY_WITH_GENERIC,
                RuntimeBlockbenchModel.HeadMotion.NONE, transforms, OUTFIT_TEXTURE);
        poseStack.popPose();
    }
}
