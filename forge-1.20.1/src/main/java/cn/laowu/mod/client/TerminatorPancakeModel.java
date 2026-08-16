package cn.laowu.mod.client;

import cn.laowu.mod.CatOutfitType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.Map;

/** Maps the standing Terminator outfit onto the authored flattened cat geometry. */
public final class TerminatorPancakeModel {
    private static final float QUARTER_TURN = (float) Math.PI / 2.0F;
    private static final float PANCAKE_BODY_COMPRESSION = 0.387F;

    /*
     * cat_pancake.bbmodel moves the cropped head up four pixels and rotates the
     * legs outward. Its missing head row is baked by getPancakeOutfit, while
     * the abdomen deliberately retains the earlier flattened compression.
     */
    private static final Map<String, RuntimeBlockbenchModel.GroupTransform> TRANSFORMS = Map.of(
            "head", RuntimeBlockbenchModel.GroupTransform.scaled(
                    0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                    1.0F, 1.0F, 1.0F),
            "group", RuntimeBlockbenchModel.GroupTransform.scaled(
                    0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                    1.0F, 1.0F, 1.0F),
            "body", RuntimeBlockbenchModel.GroupTransform.scaled(
                    0.0F, -5.05F, 0.0F, 0.0F, 0.0F, 0.0F,
                    1.0F, 1.0F, 0.387F),
            "left_hind_leg", RuntimeBlockbenchModel.GroupTransform.scaled(
                    0.0F, -1.2F, 0.0F, 0.0F, 0.0F, -QUARTER_TURN,
                    1.0F, 1.0F, 1.0F),
            "right_hind_leg", RuntimeBlockbenchModel.GroupTransform.scaled(
                    0.0F, -1.2F, 0.0F, 0.0F, 0.0F, QUARTER_TURN,
                    1.0F, 1.0F, 1.0F),
            "left_front_leg", RuntimeBlockbenchModel.GroupTransform.scaled(
                    0.0F, -5.0F, 0.0F, 0.0F, 0.0F, -QUARTER_TURN,
                    1.0F, 1.0F, 1.0F),
            "right_front_leg", RuntimeBlockbenchModel.GroupTransform.scaled(
                    0.0F, -5.0F, 0.0F, 0.0F, 0.0F, QUARTER_TURN,
                    1.0F, 1.0F, 1.0F));

    public static void render(PoseStack poseStack, MultiBufferSource buffer,
                              int packedLight, int packedOverlay) {
        render(poseStack, buffer, packedLight, packedOverlay,
                CatOutfitType.TERMINATOR,
                RuntimeBlockbenchModel.GroupSelection.ALL_GROUPS);
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffer,
                              int packedLight, int packedOverlay,
                              RuntimeBlockbenchModel.GroupSelection selection) {
        render(poseStack, buffer, packedLight, packedOverlay,
                CatOutfitType.TERMINATOR, selection);
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffer,
                              int packedLight, int packedOverlay, CatOutfitType outfit) {
        render(poseStack, buffer, packedLight, packedOverlay, outfit,
                RuntimeBlockbenchModel.GroupSelection.ALL_GROUPS);
    }

    private static void render(PoseStack poseStack, MultiBufferSource buffer,
                               int packedLight, int packedOverlay, CatOutfitType outfit,
                               RuntimeBlockbenchModel.GroupSelection selection) {
        CatOutfitModels.Definition definition = CatOutfitModels.get(outfit);
        if (definition == null) return;
        Map<String, RuntimeBlockbenchModel.GroupTransform> transforms =
                outfit == CatOutfitType.TERMINATOR ? TRANSFORMS : bodyOutfitTransforms(outfit);
        RuntimeBlockbenchModel runtime = RuntimeBlockbenchModel.getPancakeOutfit(definition.model());
        renderTextureLayer(poseStack, buffer, packedLight, packedOverlay, outfit, selection,
                definition, transforms, runtime, definition.texture(), 1);
        if (definition.translucentTexture() != null) {
            renderTextureLayer(poseStack, buffer, packedLight, packedOverlay, outfit, selection,
                    definition, transforms, runtime, definition.translucentTexture(), 2);
        }
    }

    private static void renderTextureLayer(
            PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay,
            CatOutfitType outfit, RuntimeBlockbenchModel.GroupSelection selection,
            CatOutfitModels.Definition definition,
            Map<String, RuntimeBlockbenchModel.GroupTransform> transforms,
            RuntimeBlockbenchModel runtime,
            net.minecraft.resources.ResourceLocation texture, int textureIndex) {
        var consumer = buffer.getBuffer(outfit == CatOutfitType.HONEY
                ? RenderType.entityTranslucent(texture)
                : RenderType.entityCutoutNoCull(texture));
        if (outfit == CatOutfitType.FLIGHT
                && selection == RuntimeBlockbenchModel.GroupSelection.ALL_GROUPS) {
            // The flight project stores the helmet and aircraft in one generic
            // root. The helmet belongs to the flattened head; only the aircraft
            // is compressed onto the pancake body.
            runtime.renderFlightTexture(poseStack, consumer, packedLight, packedOverlay,
                    RuntimeBlockbenchModel.HeadMotion.NONE, transforms, false);
            java.util.Map<String, RuntimeBlockbenchModel.GroupTransform> helmetTransforms =
                    new java.util.HashMap<>(transforms);
            helmetTransforms.put("group", TRANSFORMS.get("head"));
            runtime.renderFlightTexture(poseStack, consumer, packedLight, packedOverlay,
                    RuntimeBlockbenchModel.HeadMotion.NONE,
                    java.util.Map.copyOf(helmetTransforms), true);
            return;
        }
        runtime.renderTexture(poseStack, consumer, packedLight, packedOverlay,
                selection, RuntimeBlockbenchModel.HeadMotion.NONE, transforms, textureIndex);
    }

    private static Map<String, RuntimeBlockbenchModel.GroupTransform> bodyOutfitTransforms(
            CatOutfitType outfit) {
        // Generic roots are authored in global model coordinates. Express the
        // pivot in ModelPart pose pixels (poseY = 24 - modelY), then apply the
        // same global affine compression as the flattened body. Keeping the
        // body's +5.05 pose-space displacement here prevents specialist gear
        // from being left below the pancake.
        float genericPivotY = outfit == CatOutfitType.FLIGHT
                || outfit == CatOutfitType.TRANSPORT ? 24.0F : 14.5F;
        float genericOffsetY = -5.05F
                + (genericPivotY - 12.0F) * (1.0F - PANCAKE_BODY_COMPRESSION);
        return Map.of(
                "head", TRANSFORMS.get("head"),
                "group", RuntimeBlockbenchModel.GroupTransform.scaled(
                        0.0F, genericOffsetY, 0.0F, 0.0F, 0.0F, 0.0F,
                        1.0F, PANCAKE_BODY_COMPRESSION, 1.0F),
                "body", TRANSFORMS.get("body"),
                "left_hind_leg", TRANSFORMS.get("left_hind_leg"),
                "right_hind_leg", TRANSFORMS.get("right_hind_leg"),
                "left_front_leg", TRANSFORMS.get("left_front_leg"),
                "right_front_leg", TRANSFORMS.get("right_front_leg"));
    }

    private TerminatorPancakeModel() {
    }
}
