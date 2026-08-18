package cn.laowu.mod.client;

import cn.laowu.mod.CatClothesData;
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

/** Renders a living cat in the same flattened geometry used by the cat pancake item. */
public final class PancakeCatGeometryLayer extends RenderLayer<Cat, CatModel<Cat>> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            LaoWuMod.MOD_ID, "models/item/cat_pancake.bbmodel");

    public PancakeCatGeometryLayer(RenderLayerParent<Cat, CatModel<Cat>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Cat cat,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!(getParentModel() instanceof HissingCatModel model) || !model.isPancake()) return;

        poseStack.pushPose();
        // The item model is authored four pixels above the vanilla cat ground plane.
        poseStack.translate(0.0D, 4.0D / 16.0D, 0.0D);
        if (cat.isBaby()) {
            poseStack.translate(0.0D, -3.0D / 16.0D, 0.0D);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.translate(0.0D, 1.5D, 0.0D);
        }
        var consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(
                CatGenomeTextureManager.resolve(cat)));
        RuntimeBlockbenchModel.get(MODEL).render(
                poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                RuntimeBlockbenchModel.GroupSelection.ALL,
                RuntimeBlockbenchModel.HeadMotion.NONE);
        if (cat.isTame()) {
            PancakeCatCollarModel.render(
                    poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        }
        if (CatClothesData.isEquipped(cat)) {
            TerminatorPancakeModel.render(poseStack, buffer, packedLight,
                    OverlayTexture.NO_OVERLAY, CatClothesData.getOutfit(cat));
        }
        poseStack.popPose();
    }
}
