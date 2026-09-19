package cn.laowu.mod.client;

import cn.laowu.mod.CatPoseData;
import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatTraitEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.OcelotModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
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
    private boolean playingPipa;
    private boolean playingStreetDance;
    private int cockroachMode;
    private float cockroachAge;
    private float cockroachWingWeight;
    private final ModelPart pipa = new ModelPart(List.of(), Map.of());
    private final ModelPart plectrum = new ModelPart(List.of(), Map.of());
    private float liveHeadXRot;
    private float liveHeadYRot;
    private final ModelPart leftEar;
    private final ModelPart rightEar;

    public HissingCatModel(ModelPart root) {
        super(root);
        leftEar = head.getChild("left_ear");
        rightEar = head.getChild("right_ear");
    }

    public static LayerDefinition createLayer() {
        CubeDeformation deformation = CubeDeformation.NONE;
        MeshDefinition mesh = OcelotModel.createBodyMesh(deformation);
        PartDefinition root = mesh.getRoot();
        PartDefinition separatedHead = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.5F, -2.0F, -3.0F,
                                5.0F, 4.0F, 5.0F, deformation)
                        .texOffs(0, 24).addBox(-1.5F, -0.001F, -4.0F,
                                3.0F, 2.0F, 2.0F, deformation),
                PartPose.offset(0.0F, 15.0F, -9.0F));
        separatedHead.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(0, 10)
                        .addBox(-2.0F, -3.0F, 0.0F,
                                1.0F, 1.0F, 2.0F, deformation),
                PartPose.ZERO);
        separatedHead.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(6, 10)
                        .addBox(1.0F, -3.0F, 0.0F,
                                1.0F, 1.0F, 2.0F, deformation),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void prepareMobModel(Cat cat, float limbSwing, float limbSwingAmount, float partialTick) {
        // The renderer shares a model between cats. Clear custom rotations/scales
        // before vanilla prepares its standing, sitting and sleeping poses.
        for (ModelPart part : new ModelPart[]{head, body, leftHindLeg, rightHindLeg,
                leftFrontLeg, rightFrontLeg, tail1, tail2}) {
            part.resetPose();
            part.xScale = part.yScale = part.zScale = 1.0F;
        }
        super.prepareMobModel(cat, limbSwing, limbSwingAmount, partialTick);
        if (cn.laowu.mod.CatEngineeringCombat.deployed(cat) && !cat.isInSittingPose()) {
            // Vanilla seated geometry only: never set the server's stay/sit command flag.
            body.xRot = 0.7853982F; body.y -= 4; body.z += 5;
            head.y -= 3.3F; head.z += 1;
            tail1.y += 8; tail1.z -= 2; tail1.xRot = 1.7278761F;
            tail2.y += 2; tail2.z -= 0.8F; tail2.xRot = 2.670354F;
            leftFrontLeg.xRot = rightFrontLeg.xRot = -0.15707964F;
            leftFrontLeg.y = rightFrontLeg.y = 16.1F;
            leftFrontLeg.z = rightFrontLeg.z = -7;
            leftHindLeg.xRot = rightHindLeg.xRot = -1.5707964F;
            leftHindLeg.y = rightHindLeg.y = 21;
            leftHindLeg.z = rightHindLeg.z = 1;
            state = 3;
        }
    }

    @Override
    public void setupAnim(Cat cat, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        boolean healing = cn.laowu.mod.CatMedicalHealing.casting(cat);
        boolean combatMusic = cn.laowu.mod.CatMusicSupport.performing(cat);
        boolean music = combatMusic || CatMusicRecordClient.performing(cat);
        if (healing || music) { limbSwing = 0; limbSwingAmount = 0; }
        super.setupAnim(cat, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (cn.laowu.mod.CatPilotFlight.carried(cat) || cn.laowu.mod.CatDivingMount.carried(cat)) {
            // The complete pilot follows the locally predicted body heading; no old network head yaw.
            head.xRot = head.yRot = 0;
        }
        liveHeadXRot = head.xRot;
        liveHeadYRot = head.yRot;
        hissing = CatPoseData.isHissing(cat);
        pancake = CatPoseData.isPancake(cat);
        playingPipa = false;
        playingStreetDance = false;
        cockroachMode = cn.laowu.mod.CatCockroachSwarm.mode(cat);
        cockroachAge = cn.laowu.mod.CatCockroachSwarm.age(cat, ageInTicks - cat.tickCount);
        boolean vanillaVisible = !hissing && !pancake;
        setVanillaGeometryVisible(vanillaVisible);
        var traits = CatTraitData.read(cat).orElse(null);
        boolean roundHeadInCombat = traits != null
                && traits.has(CatTrait.ROUND_HEAD)
                && CatTraitEffects.isCombatActive(cat);
        leftEar.visible = vanillaVisible && !roundHeadInCombat;
        rightEar.visible = vanillaVisible && !roundHeadInCombat;
        boolean splitTail = traits != null && traits.has(CatTrait.NEKOMATA);
        tail1.visible = vanillaVisible && !splitTail;
        tail2.visible = vanillaVisible && !splitTail;
        if (vanillaVisible && !healing && !music && traits != null && traits.has(CatTrait.STREET_DANCE)
                && cat.isAlive() && cat.onGround() && !cat.isInWater()
                && !cat.isPassenger() && limbSwingAmount < 0.08F
                && cat.getLieDownAmount(0.0F) <= 0.0F
                && !CatTraitEffects.isCombatActive(cat)) {
            CatStreetDanceAnimation.apply(ageInTicks, head, body, leftHindLeg,
                    rightHindLeg, leftFrontLeg, rightFrontLeg, tail1, tail2);
            playingStreetDance = CatStreetDanceAnimation.isAvailable() && Float.isFinite(ageInTicks);
        }
        if (vanillaVisible && !healing && !music && traits != null && traits.has(CatTrait.PIPA_PERFORMANCE)
                && cat.isAlive() && cat.onGround() && !cat.isInWater()
                && !cat.isPassenger() && limbSwingAmount < 0.08F
                && cat.getLieDownAmount(0.0F) <= 0.0F
                && !CatTraitEffects.isCombatActive(cat)
                && CatPipaAnimation.isAvailable() && Float.isFinite(ageInTicks)) {
            CatPipaAnimation.apply(ageInTicks, head, body, leftHindLeg,
                    rightHindLeg, leftFrontLeg, rightFrontLeg, tail1, tail2, pipa, plectrum);
            playingPipa = true;
        }
        if (vanillaVisible && music) {
            float musicAge = combatMusic ? cn.laowu.mod.CatMusicSupport.age(cat, ageInTicks - cat.tickCount)
                    : CatMusicRecordClient.age(cat, ageInTicks - cat.tickCount);
            int musicPose = combatMusic ? cn.laowu.mod.CatMusicSupport.pose(cat) : CatMusicRecordClient.pose(cat);
            if (musicPose == 0) {
                CatPipaAnimation.apply(musicAge, head, body, leftHindLeg,
                        rightHindLeg, leftFrontLeg, rightFrontLeg, tail1, tail2, pipa, plectrum);
                playingPipa = CatPipaAnimation.isAvailable();
            } else {
                CatStreetDanceAnimation.apply(musicAge, head, body, leftHindLeg,
                        rightHindLeg, leftFrontLeg, rightFrontLeg, tail1, tail2);
                playingStreetDance = CatStreetDanceAnimation.isAvailable();
            }
        }
        if (vanillaVisible && healing) {
            float castAge = cn.laowu.mod.CatMedicalHealing.castAge(cat, ageInTicks - cat.tickCount);
            if (cn.laowu.mod.CatMedicalHealing.stationed(cat))
                CatMedicalAnimation.applyStationed(castAge, head, body, leftHindLeg, rightHindLeg,
                        leftFrontLeg, rightFrontLeg, tail1, tail2);
            else CatMedicalAnimation.apply(castAge, head, body, leftHindLeg, rightHindLeg,
                        leftFrontLeg, rightFrontLeg, tail1, tail2);
        }
        boolean riding=vanillaVisible && (cn.laowu.mod.CatPilotFlight.carried(cat) || cn.laowu.mod.CatDivingMount.swimming(cat));
        var transition=CatPoseTransitions.sample(cat,riding,vanillaVisible&&!healing&&!music?cockroachMode:0,ageInTicks-cat.tickCount);
        cockroachWingWeight=transition.wings();
        cockroachAge=transition.age();
        cockroachMode=transition.mode();
        if(vanillaVisible && !healing && !music) {
            CatPoseTransitions.apply(transition.ride(),()->CatRideAnimation.apply(ageInTicks,head,body,leftHindLeg,rightHindLeg,
                    leftFrontLeg,rightFrontLeg,tail1,tail2),head,body,leftHindLeg,rightHindLeg,leftFrontLeg,rightFrontLeg,tail1,tail2);
            CatPoseTransitions.apply(transition.dash(),()->CatCockroachAnimation.dash(cockroachAge,leftHindLeg,rightHindLeg,
                    leftFrontLeg,rightFrontLeg),leftHindLeg,rightHindLeg,leftFrontLeg,rightFrontLeg);
            if(riding)liveHeadXRot=liveHeadYRot=0;
        }
        var strike = cn.laowu.mod.CatAgentMeleeMotion.current(cat);
        if (vanillaVisible && strike != null && !healing && !music) {
            float strikeAge = cat.level().getGameTime() - strike.started() + ageInTicks - cat.tickCount;
            CatAgentAttackAnimation.apply(strike.move(), strikeAge / strike.duration(), head, body,
                    leftHindLeg, rightHindLeg, leftFrontLeg, rightFrontLeg, tail1, tail2);
            liveHeadXRot = liveHeadYRot = 0;
            playingStreetDance = playingPipa = false;
        }
        if (vanillaVisible && CatEngineeringAnimation.isPosing(cat)
                && !cn.laowu.mod.CatEngineeringCombat.deployed(cat)) {
            CatEngineeringAnimation.apply(cat, head, body, leftHindLeg, rightHindLeg,
                    leftFrontLeg, rightFrontLeg, tail1, tail2);
            playingStreetDance = false;
            playingPipa = false;
        }
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
        leftEar.visible = visible;
        rightEar.visible = visible;
    }

    public boolean isHissing() {
        return hissing;
    }

    public boolean isPancake() {
        return pancake;
    }

    /** True only after this cat has actually received a performance pose this frame. */
    public boolean isPlayingPerformance() {
        return playingStreetDance || playingPipa;
    }

    public boolean isPlayingPipa() {
        return playingPipa;
    }

    public Map<String, RuntimeBlockbenchModel.GroupTransform> pipaTransforms() {
        // Runtime props mirror BB X once, matching the cat's ModelPart space.
        return Map.of("pipa", boneDelta(pipa, 0.0F, 21.5F, -5.4F, 0, 0, 0),
                "plectrum", boneDelta(plectrum, 0.0F, 24.0F, 0.0F, 0, 0, 0));
    }

    public float liveHeadXRot() {
        return liveHeadXRot;
    }

    public float liveHeadYRot() {
        return liveHeadYRot;
    }

    /**
     * Transfers the evaluated pose to the slightly inflated vanilla collar mesh.
     * Re-running vanilla animation on that mesh would discard custom head motion.
     */
    void copyPoseTo(CatModel<Cat> target, ModelPart targetRoot) {
        copyPropertiesTo(target);
        copyPartPose(head, targetRoot.getChild("head"));
        copyPartPose(body, targetRoot.getChild("body"));
        copyPartPose(leftHindLeg, targetRoot.getChild("left_hind_leg"));
        copyPartPose(rightHindLeg, targetRoot.getChild("right_hind_leg"));
        copyPartPose(leftFrontLeg, targetRoot.getChild("left_front_leg"));
        copyPartPose(rightFrontLeg, targetRoot.getChild("right_front_leg"));
        copyPartPose(tail1, targetRoot.getChild("tail1"));
        copyPartPose(tail2, targetRoot.getChild("tail2"));
    }

    private static void copyPartPose(ModelPart source, ModelPart target) {
        target.copyFrom(source);
        target.visible = source.visible;
        target.skipDraw = source.skipDraw;
    }

    ModelPart headPart() {
        return head;
    }

    ModelPart leftHindLegPart() {
        return leftHindLeg;
    }

    ModelPart rightHindLegPart() {
        return rightHindLeg;
    }

    ModelPart leftFrontLegPart() {
        return leftFrontLeg;
    }

    ModelPart rightFrontLegPart() {
        return rightFrontLeg;
    }

    ModelPart tailBasePart() {
        return tail1;
    }

    ModelPart tailTipPart() {
        return tail2;
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
            // Preserve supplied pivots. Diving has a tilted root at model (-2.25, 6.5, 1.25).
            boolean diving = outfit == CatOutfitType.DIVING;
            float pivotX = diving ? 2.25F : modelOriginRoot || outfit.hasImportedModel() ? 0.0F : 1.6F;
            float pivotY = diving ? 17.5F : modelOriginRoot ? 24.0F : outfit.hasImportedModel() ? 18.6F : 14.5F;
            float pivotZ = diving ? 1.25F : modelOriginRoot ? 0.0F : outfit.hasImportedModel() ? -9.5F : -10.1F;
            transforms.put("group", bodyAttachedAccessoryDelta(pivotX, pivotY, pivotZ));
        }
        if (outfit == CatOutfitType.COCKROACH) CatCockroachAnimation.wings(transforms, cockroachMode, cockroachAge, cockroachWingWeight);
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
        // Convert the authored offset into the bone's local space before
        // applying its animation scale, then follow the live bone rotation.
        offset.rotate(baseInverse).mul(body.xScale, body.yScale, body.zScale)
                .rotate(new Quaternionf().rotationZYX(body.zRot, body.yRot, body.xRot));
        float liveX = body.x + offset.x;
        float liveY = body.y + offset.y;
        float liveZ = body.z + offset.z;
        return new RuntimeBlockbenchModel.GroupTransform(
                liveX - accessoryX,
                accessoryY - liveY,
                liveZ - accessoryZ,
                body.xRot - baseXRot,
                body.yRot,
                body.zRot,
                body.xScale, body.zScale, body.yScale);
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
                part.zRot - baseZRot,
                part.xScale, part.yScale, part.zScale);
    }

    /**
     * Converts geometry authored against the vanilla standing body transform so
     * it follows the live body transform (most visibly the 45-degree sitting pose).
     */
    public void applyBodyPoseDelta(PoseStack poseStack) {
        poseStack.translate(body.x / 16.0F, body.y / 16.0F, body.z / 16.0F);
        poseStack.mulPose(new Quaternionf().rotationZYX(body.zRot, body.yRot, body.xRot));
        poseStack.scale(body.xScale, body.yScale, body.zScale);
        poseStack.mulPose(new Quaternionf().rotationX(-((float) Math.PI / 2.0F)));
        poseStack.translate(0.0F, -12.0F / 16.0F, 10.0F / 16.0F);
    }
}
