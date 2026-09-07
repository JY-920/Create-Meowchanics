package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatTraitProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;

/** Renders fixed geometry belonging to appearance traits. */
public final class CatAppearanceLayer extends RenderLayer<Cat, CatModel<Cat>> {
    private static final ResourceLocation TEAR_BLUE = ResourceLocation.withDefaultNamespace(
            "textures/block/light_blue_concrete.png");
    private static final ResourceLocation BOOT_BROWN = ResourceLocation.withDefaultNamespace(
            "textures/block/brown_wool.png");
    private static final ResourceLocation HISSING_MODEL = LaoWuMod.id(
            "models/entity/hissing_cat.bbmodel");
    private static final ResourceLocation PANCAKE_MODEL = LaoWuMod.id(
            "models/item/cat_pancake.bbmodel");
    private static final float TAIL_SPREAD_DEGREES = 22.5F;
    private static final float RUNTIME_TAIL_PIVOT_Y = 15.0F / 16.0F;
    private static final float RUNTIME_TAIL_PIVOT_Z = 8.0F / 16.0F;

    private final CatAppearanceModel appearanceModel;

    public CatAppearanceLayer(RenderLayerParent<Cat, CatModel<Cat>> parent,
                              CatAppearanceModel appearanceModel) {
        super(parent);
        this.appearanceModel = appearanceModel;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int packedLight, Cat cat,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        CatTraitProfile traits = CatTraitData.read(cat).orElse(CatTraitProfile.EMPTY);
        if (traits.traits().isEmpty()
                || !(getParentModel() instanceof HissingCatModel model)) return;

        boolean ordinaryGeometry = !model.isHissing() && !model.isPancake();
        if (ordinaryGeometry && traits.has(CatTrait.HIM)) {
            getParentModel().renderToBuffer(pose,
                    buffers.getBuffer(RenderType.eyes(
                            CatAppearanceTextures.whiteEyes())),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    1.0F, 1.0F, 1.0F, 1.0F);
        }
        if (ordinaryGeometry && traits.has(CatTrait.ISAAC)) {
            pose.pushPose();
            applyBabyHeadTransform(pose, cat);
            appearanceModel.renderTears(model, pose,
                    buffers.getBuffer(RenderType.entityTranslucent(TEAR_BLUE)),
                    packedLight, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        if (ordinaryGeometry && traits.has(CatTrait.PUSS_IN_BOOTS)) {
            pose.pushPose();
            applyBabyBodyTransform(pose, cat);
            appearanceModel.renderBoots(model, pose,
                    buffers.getBuffer(RenderType.entityCutoutNoCull(BOOT_BROWN)),
                    packedLight, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        if (traits.has(CatTrait.NEKOMATA)) {
            renderTwinTails(pose, buffers, packedLight, cat, model, ageInTicks);
        }
    }

    private static void renderTwinTails(PoseStack pose, MultiBufferSource buffers,
                                        int packedLight, Cat cat,
                                        HissingCatModel model, float ageInTicks) {
        var consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(
                CatGenomeTextureManager.resolve(cat)));
        pose.pushPose();
        if (model.isPancake()) {
            pose.translate(0.0D, 4.0D / 16.0D, 0.0D);
            if (cat.isBaby()) pose.translate(0.0D, -3.0D / 16.0D, 0.0D);
            applyBabyBodyTransform(pose, cat);
            renderRuntimeTwinTails(RuntimeBlockbenchModel.get(PANCAKE_MODEL),
                    pose, consumer, packedLight,
                    RuntimeBlockbenchModel.HeadMotion.NONE);
        } else if (model.isHissing()) {
            applyBabyBodyTransform(pose, cat);
            renderRuntimeTwinTails(RuntimeBlockbenchModel.get(HISSING_MODEL),
                    pose, consumer, packedLight,
                    HissingCatGeometryLayer.headMotion(cat, model, ageInTicks));
        } else {
            applyBabyBodyTransform(pose, cat);
            var base = model.tailBasePart();
            var tip = model.tailTipPart();
            boolean baseVisible = base.visible;
            boolean tipVisible = tip.visible;
            base.visible = true;
            tip.visible = true;
            for (float angle : new float[] {-TAIL_SPREAD_DEGREES, TAIL_SPREAD_DEGREES}) {
                pose.pushPose();
                rotateAroundTailRoot(pose, base.x / 16.0F,
                        base.y / 16.0F, base.z / 16.0F, angle);
                base.render(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY);
                tip.render(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
            base.visible = baseVisible;
            tip.visible = tipVisible;
        }
        pose.popPose();
    }

    private static void renderRuntimeTwinTails(RuntimeBlockbenchModel runtimeModel,
                                               PoseStack pose,
                                               com.mojang.blaze3d.vertex.VertexConsumer consumer,
                                               int packedLight,
                                               RuntimeBlockbenchModel.HeadMotion headMotion) {
        for (float angle : new float[] {-TAIL_SPREAD_DEGREES, TAIL_SPREAD_DEGREES}) {
            pose.pushPose();
            rotateAroundTailRoot(pose, 0.0F, RUNTIME_TAIL_PIVOT_Y,
                    RUNTIME_TAIL_PIVOT_Z, angle);
            runtimeModel.render(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                    RuntimeBlockbenchModel.GroupSelection.CAT_TAIL_ONLY, headMotion);
            pose.popPose();
        }
    }

    private static void rotateAroundTailRoot(PoseStack pose, float x, float y,
                                             float z, float angle) {
        pose.translate(x, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate(-x, -y, -z);
    }

    private static void applyBabyBodyTransform(PoseStack pose, Cat cat) {
        if (!cat.isBaby()) return;
        pose.scale(0.5F, 0.5F, 0.5F);
        pose.translate(0.0D, 1.5D, 0.0D);
    }

    private static void applyBabyHeadTransform(PoseStack pose, Cat cat) {
        if (!cat.isBaby()) return;
        pose.scale(0.75F, 0.75F, 0.75F);
        pose.translate(0.0D, 10.0D / 16.0D, 4.0D / 16.0D);
    }

}
