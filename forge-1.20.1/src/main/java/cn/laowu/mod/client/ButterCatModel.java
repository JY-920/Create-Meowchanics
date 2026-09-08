package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.entity.ButterCatBoss;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.util.Mth;

import java.util.LinkedHashMap;
import java.util.Map;

/** Renders the supplied two-texture Blockbench model and its charge keyframes. */
public final class ButterCatModel extends EntityModel<ButterCatBoss> {
    static final net.minecraft.resources.ResourceLocation MODEL =
            LaoWuMod.id("models/entity/butter_cat.bbmodel");

    private RuntimeBlockbenchModel.HeadMotion headMotion =
            RuntimeBlockbenchModel.HeadMotion.NONE;
    private Map<String, RuntimeBlockbenchModel.GroupTransform> transforms = Map.of();

    @Override
    public void setupAnim(ButterCatBoss cat, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float partialTick = Mth.clamp(ageInTicks - cat.tickCount, 0.0F, 1.0F);
        if (cat.isWindingUp()) {
            this.headMotion = RuntimeBlockbenchModel.HeadMotion.NONE;
            this.transforms = windupTransforms(cat.getWindupAnimationSeconds(partialTick));
            return;
        }
        if (cat.isDashing()) {
            this.headMotion = RuntimeBlockbenchModel.HeadMotion.NONE;
            this.transforms = dashTransforms(cat.getDashAnimationProgress(partialTick));
            return;
        }

        this.headMotion = new RuntimeBlockbenchModel.HeadMotion(
                headPitch * Mth.DEG_TO_RAD,
                netHeadYaw * Mth.DEG_TO_RAD,
                0.0F);
        this.transforms = walkingTransforms(limbSwing, limbSwingAmount, ageInTicks);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int light,
                               int overlay, float red, float green, float blue, float alpha) {
        RuntimeBlockbenchModel.get(MODEL).renderTexture(poseStack, consumer, light, overlay,
                RuntimeBlockbenchModel.GroupSelection.ALL, this.headMotion,
                this.transforms, 0);
    }

    void renderButter(PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        RuntimeBlockbenchModel.get(MODEL).renderTexture(poseStack, consumer, light, overlay,
                RuntimeBlockbenchModel.GroupSelection.ALL, this.headMotion,
                this.transforms, 1);
    }

    private static Map<String, RuntimeBlockbenchModel.GroupTransform> walkingTransforms(
            float limbSwing, float limbSwingAmount, float ageInTicks) {
        float stride = Math.min(limbSwingAmount, 1.0F);
        float leftFront = Mth.cos(limbSwing * 0.6662F) * 1.15F * stride;
        float rightFront = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.15F * stride;
        float tailSway = Mth.sin(ageInTicks * 0.12F) * 0.09F;
        return Map.of(
                "left_front_leg", RuntimeBlockbenchModel.GroupTransform.rotation(
                        leftFront, 0.0F, 0.0F),
                "right_front_leg", RuntimeBlockbenchModel.GroupTransform.rotation(
                        rightFront, 0.0F, 0.0F),
                "left_hind_leg", RuntimeBlockbenchModel.GroupTransform.rotation(
                        rightFront, 0.0F, 0.0F),
                "right_hind_leg", RuntimeBlockbenchModel.GroupTransform.rotation(
                        leftFront, 0.0F, 0.0F),
                "tail1", RuntimeBlockbenchModel.GroupTransform.rotation(
                        0.0F, tailSway, 0.0F),
                "tail2", RuntimeBlockbenchModel.GroupTransform.rotation(
                        0.0F, tailSway * 1.4F, 0.0F));
    }

    /** Exact animation1 transforms, with timing supplied at 2x by the entity. */
    private static Map<String, RuntimeBlockbenchModel.GroupTransform> dashTransforms(
            float progress) {
        Map<String, RuntimeBlockbenchModel.GroupTransform> result = new LinkedHashMap<>();
        result.put("group2", RuntimeBlockbenchModel.GroupTransform.rotation(
                0.0F, 0.0F, Mth.TWO_PI * progress));
        result.put("left_hind_leg", transform(0.25F, -2.0F, 3.0F,
                -90.0F, 0.0F, 0.0F));
        result.put("right_hind_leg", transform(-0.25F, -2.0F, 3.0F,
                -90.0F, 0.0F, 0.0F));
        result.put("left_front_leg", transform(0.5F, -3.0F, 0.0F,
                90.0F, 0.0F, 0.0F));
        result.put("right_front_leg", transform(-0.5F, -3.0F, 0.0F,
                90.0F, 0.0F, 0.0F));
        result.put("group3", RuntimeBlockbenchModel.GroupTransform.rotation(
                -38.5F * Mth.DEG_TO_RAD, 0.0F, 0.0F));
        result.put("group4", RuntimeBlockbenchModel.GroupTransform.rotation(
                47.5F * Mth.DEG_TO_RAD, 0.0F, 0.0F));
        return Map.copyOf(result);
    }

    /**
     * Exact animation2 channels sampled with Blockbench's Catmull-Rom curve.
     * Time is in authored seconds and is clamped by the entity to [0, 5].
     */
    private static Map<String, RuntimeBlockbenchModel.GroupTransform> windupTransforms(
            float seconds) {
        Map<String, RuntimeBlockbenchModel.GroupTransform> result = new LinkedHashMap<>();

        AnimVec butterPosition = catmull(seconds,
                key(0.0F, 0.0F, 0.0F, 0.0F),
                key(0.25F, 0.0F, 0.0F, 0.0F),
                key(0.5F, 0.0F, 0.0F, 0.0F),
                key(2.5F, 0.0F, 13.0F, 0.0F),
                key(5.0F, 0.0F, 0.0F, 0.0F));
        AnimVec butterScale = catmull(seconds,
                key(0.0F, 1.0F, 1.0F, 1.0F),
                key(0.25F, 1.2F, 0.7F, 1.1F),
                key(0.5F, 1.0F, 1.0F, 1.0F));
        AnimVec butterRotation = catmull(seconds,
                key(0.5F, 0.0F, 0.0F, 0.0F),
                key(4.5F, 0.0F, 0.0F, 3600.0F),
                key(5.0F, 0.0F, 0.0F, 3600.0F));
        result.put("group", RuntimeBlockbenchModel.GroupTransform.scaled(
                butterPosition.x, butterPosition.y, butterPosition.z,
                butterRotation.x * Mth.DEG_TO_RAD,
                butterRotation.y * Mth.DEG_TO_RAD,
                butterRotation.z * Mth.DEG_TO_RAD,
                butterScale.x, butterScale.y, butterScale.z));

        AnimVec bodyPosition = catmull(seconds,
                key(4.5F, 0.0F, 0.0F, 0.0F),
                key(4.75F, 0.0F, -2.0F, 0.0F),
                key(5.0F, 0.0F, 0.0F, 0.0F));
        AnimVec bodyScale = catmull(seconds,
                key(4.5F, 1.0F, 1.0F, 1.0F),
                key(4.75F, 1.8F, 0.7F, 1.0F),
                key(5.0F, 1.0F, 1.0F, 1.0F));
        result.put("group2", RuntimeBlockbenchModel.GroupTransform.scaled(
                bodyPosition.x, bodyPosition.y, bodyPosition.z,
                0.0F, 0.0F, 0.0F,
                bodyScale.x, bodyScale.y, bodyScale.z));
        return Map.copyOf(result);
    }

    private static AnimKey key(float time, float x, float y, float z) {
        return new AnimKey(time, new AnimVec(x, y, z));
    }

    private static AnimVec catmull(float time, AnimKey... keys) {
        if (keys.length == 0) return AnimVec.ZERO;
        if (keys.length == 1 || time <= keys[0].time) return keys[0].value;
        if (time >= keys[keys.length - 1].time) return keys[keys.length - 1].value;

        int index = 0;
        while (index + 1 < keys.length && time > keys[index + 1].time) index++;
        AnimKey first = keys[index];
        AnimKey second = keys[index + 1];
        AnimVec before = index > 0 ? keys[index - 1].value : first.value;
        AnimVec after = index + 2 < keys.length ? keys[index + 2].value : second.value;
        float span = Math.max(1.0E-6F, second.time - first.time);
        float t = Mth.clamp((time - first.time) / span, 0.0F, 1.0F);
        return new AnimVec(
                catmull(before.x, first.value.x, second.value.x, after.x, t),
                catmull(before.y, first.value.y, second.value.y, after.y, t),
                catmull(before.z, first.value.z, second.value.z, after.z, t));
    }

    private static float catmull(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return 0.5F * ((2.0F * p1)
                + (-p0 + p2) * t
                + (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * t2
                + (-p0 + 3.0F * p1 - 3.0F * p2 + p3) * t3);
    }

    private static RuntimeBlockbenchModel.GroupTransform transform(
            float x, float y, float z, float xDegrees, float yDegrees, float zDegrees) {
        return new RuntimeBlockbenchModel.GroupTransform(x, y, z,
                xDegrees * Mth.DEG_TO_RAD,
                yDegrees * Mth.DEG_TO_RAD,
                zDegrees * Mth.DEG_TO_RAD);
    }

    private record AnimKey(float time, AnimVec value) { }

    private record AnimVec(float x, float y, float z) {
        private static final AnimVec ZERO = new AnimVec(0.0F, 0.0F, 0.0F);
    }
}
