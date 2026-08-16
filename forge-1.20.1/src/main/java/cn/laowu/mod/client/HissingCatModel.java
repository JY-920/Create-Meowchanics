package cn.laowu.mod.client;

import cn.laowu.mod.CatPoseData;
import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.LaoWuMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.OcelotModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps the vanilla cat model for the normal state. The hissing geometry is
 * rendered from the user's Blockbench project by {@link HissingCatGeometryLayer}.
 */
public final class HissingCatModel extends CatModel<Cat> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LaoWuMod.MOD_ID, "hissing_cat"), "main");

    private boolean hissing;
    private boolean pancake;
    private float liveHeadXRot;
    private float liveHeadYRot;

    public HissingCatModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        return LayerDefinition.create(OcelotModel.createBodyMesh(CubeDeformation.NONE), 64, 32);
    }

    @Override
    public void setupAnim(Cat cat, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(cat, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        liveHeadXRot = head.xRot;
        liveHeadYRot = head.yRot;
        hissing = CatPoseData.isHissing(cat);
        pancake = CatPoseData.isPancake(cat);
        setVanillaGeometryVisible(!hissing && !pancake);
    }

    private void setVanillaGeometryVisible(boolean visible) {
        head.visible = visible;
        body.visible = visible;
        leftHindLeg.visible = visible;
        rightHindLeg.visible = visible;
        leftFrontLeg.visible = visible;
        rightFrontLeg.visible = visible;
        tail1.visible = visible;
        tail2.visible = visible;
    }

    public boolean isHissing() {
        return hissing;
    }

    public boolean isPancake() {
        return pancake;
    }

    public float liveHeadXRot() {
        return liveHeadXRot;
    }

    public float liveHeadYRot() {
        return liveHeadYRot;
    }

    /**
     * Converts the live vanilla cat bones into additive transforms understood
     * by {@link RuntimeBlockbenchModel}. The supplied clothing project was
     * authored over these exact vanilla standing pivots, so only the deltas
     * need to be applied while walking, sitting and looking around.
     */
    public Map<String, RuntimeBlockbenchModel.GroupTransform> catClothesTransforms() {
        return catOutfitTransforms(CatOutfitType.TERMINATOR);
    }

    public Map<String, RuntimeBlockbenchModel.GroupTransform> catOutfitTransforms(
            CatOutfitType outfit) {
        Map<String, RuntimeBlockbenchModel.GroupTransform> transforms = new HashMap<>();
        RuntimeBlockbenchModel.GroupTransform headTransform = boneDelta(
                head, 0.0F, 15.0F, -9.0F, 0.0F, 0.0F, 0.0F);
        transforms.put("head", headTransform);
        transforms.put("body", boneDelta(body, 0.0F, 12.0F, -10.0F,
                (float) Math.PI / 2.0F, 0.0F, 0.0F));
        transforms.put("left_hind_leg", boneDelta(leftHindLeg, 1.1F, 18.0F, 5.0F,
                0.0F, 0.0F, 0.0F));
        transforms.put("right_hind_leg", boneDelta(rightHindLeg, -1.1F, 18.0F, 5.0F,
                0.0F, 0.0F, 0.0F));
        transforms.put("left_front_leg", boneDelta(leftFrontLeg, 1.2F, 14.1F, -5.0F,
                0.0F, 0.0F, 0.0F));
        transforms.put("right_front_leg", boneDelta(rightFrontLeg, -1.2F, 14.1F, -5.0F,
                0.0F, 0.0F, 0.0F));
        transforms.put("tail1", boneDelta(tail1, 0.0F, 15.0F, 8.0F,
                0.9F, 0.0F, 0.0F));
        transforms.put("tail2", boneDelta(tail2, 0.0F, 20.0F, 14.0F,
                1.7278761F, 0.0F, 0.0F));

        if (outfit == CatOutfitType.TERMINATOR) {
            // The Terminator project's generic root is a facial accessory.
            transforms.put("group", headAttachedAccessoryDelta());
        } else if (outfit != CatOutfitType.NONE) {
            // The four specialist projects put their body/back attachments in
            // a generic root. Orbit that root around the live body pivot so it
            // follows sitting and walking without treating it as headwear.
            // The outfit loader reflects the free-format projects across X.
            // These pivots are expressed in ModelPart pose pixels, where
            // Blockbench model Y is converted with poseY = 24 - modelY.
            boolean modelOriginRoot = outfit == CatOutfitType.FLIGHT
                    || outfit == CatOutfitType.TRANSPORT;
            float pivotX = modelOriginRoot ? 0.0F : 1.6F;
            float pivotY = modelOriginRoot ? 24.0F : 14.5F;
            float pivotZ = modelOriginRoot ? 0.0F : -10.1F;
            transforms.put("group", bodyAttachedAccessoryDelta(pivotX, pivotY, pivotZ));
        }
        return Map.copyOf(transforms);
    }

    private RuntimeBlockbenchModel.GroupTransform headAttachedAccessoryDelta() {
        return headAttachedAccessoryDelta(1.6F, 14.5F, -10.1F);
    }

    /** Flight's source puts its helmet beneath a generic root at model origin. */
    public RuntimeBlockbenchModel.GroupTransform flightHelmetTransform() {
        return headAttachedAccessoryDelta(0.0F, 24.0F, 0.0F);
    }

    private RuntimeBlockbenchModel.GroupTransform headAttachedAccessoryDelta(
            float accessoryX, float accessoryY, float accessoryZ) {
        Vector3f offset = new Vector3f(
                accessoryX,
                accessoryY - 15.0F,
                accessoryZ + 9.0F);
        offset.rotate(new Quaternionf().rotationZYX(head.zRot, head.yRot, head.xRot));
        float liveX = head.x + offset.x;
        float liveY = head.y + offset.y;
        float liveZ = head.z + offset.z;
        return new RuntimeBlockbenchModel.GroupTransform(
                liveX - accessoryX,
                accessoryY - liveY,
                liveZ - accessoryZ,
                head.xRot, head.yRot, head.zRot);
    }

    private RuntimeBlockbenchModel.GroupTransform bodyAttachedAccessoryDelta(
            float accessoryX, float accessoryY, float accessoryZ) {
        final float baseX = 0.0F;
        final float baseY = 12.0F;
        final float baseZ = -10.0F;
        final float baseXRot = (float) Math.PI / 2.0F;

        Vector3f offset = new Vector3f(
                accessoryX - baseX,
                accessoryY - baseY,
                accessoryZ - baseZ);
        Quaternionf baseInverse = new Quaternionf()
                .rotationZYX(0.0F, 0.0F, baseXRot).invert();
        Quaternionf delta = new Quaternionf()
                .rotationZYX(body.zRot, body.yRot, body.xRot)
                .mul(baseInverse);
        offset.rotate(delta);
        float liveX = body.x + offset.x;
        float liveY = body.y + offset.y;
        float liveZ = body.z + offset.z;
        return new RuntimeBlockbenchModel.GroupTransform(
                liveX - accessoryX,
                accessoryY - liveY,
                liveZ - accessoryZ,
                body.xRot - baseXRot,
                body.yRot,
                body.zRot);
    }

    private static RuntimeBlockbenchModel.GroupTransform boneDelta(
            ModelPart part, float baseX, float baseY, float baseZ,
            float baseXRot, float baseYRot, float baseZRot) {
        return new RuntimeBlockbenchModel.GroupTransform(
                part.x - baseX,
                baseY - part.y,
                part.z - baseZ,
                part.xRot - baseXRot,
                part.yRot - baseYRot,
                part.zRot - baseZRot);
    }

    /**
     * Converts geometry authored against the vanilla standing body transform so
     * it follows the live body transform (most visibly the 45-degree sitting pose).
     */
    public void applyBodyPoseDelta(PoseStack poseStack) {
        poseStack.translate(body.x / 16.0F, body.y / 16.0F, body.z / 16.0F);
        poseStack.mulPose(new Quaternionf().rotationZYX(body.zRot, body.yRot, body.xRot));
        poseStack.mulPose(new Quaternionf().rotationX(-((float) Math.PI / 2.0F)));
        poseStack.translate(0.0F, -12.0F / 16.0F, 10.0F / 16.0F);
    }
}
