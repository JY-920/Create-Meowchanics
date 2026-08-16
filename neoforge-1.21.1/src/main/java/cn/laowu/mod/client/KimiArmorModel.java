package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/** Animated humanoid armor geometry reconstructed from the supplied Blockbench project. */
public final class KimiArmorModel extends HumanoidModel<LivingEntity> {
    private static final ResourceLocation HELMET_MODEL =
            LaoWuMod.id("models/entity/kimi_helmet.bbmodel");
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            LaoWuMod.id("kimi_armor"), "main");
    public static final ModelLayerLocation SLIM_LAYER = new ModelLayerLocation(
            LaoWuMod.id("kimi_armor"), "slim");

    private final ModelPart tailRoot;
    private final ModelPart tailTip;
    private final ModelPart rightLegging;
    private final ModelPart leftLegging;
    private final ModelPart rightBoot;
    private final ModelPart leftBoot;

    public KimiArmorModel(ModelPart root) {
        super(root);
        tailRoot = body.getChild("tail_root");
        tailTip = body.getChild("tail_tip");
        rightLegging = rightLeg.getChild("legging");
        leftLegging = leftLeg.getChild("legging");
        rightBoot = rightLeg.getChild("boot");
        leftBoot = leftLeg.getChild("boot");
    }

    public static LayerDefinition createLayer() {
        return createLayer(false);
    }

    public static LayerDefinition createSlimLayer() {
        return createLayer(true);
    }

    private static LayerDefinition createLayer(boolean slimArms) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Exact coordinates from the revised Blockbench model. Blockbench's
        // humanoid Y axis is inverted around y=24 in ModelPart coordinates.
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3.85F, -7.85F, -3.85F, 7.7F, 7.7F, 7.7F,
                                new CubeDeformation(0.75F))
                        .texOffs(16, 36)
                        .addBox(-1.15F, -6.55F, -5.15F, 2.3F, 1.5F, 0.7F,
                                new CubeDeformation(0.25F))
                        .texOffs(48, 48)
                        .addBox(-3.55F, -10.35F, 0.25F, 1.5F, 1.5F, 2.3F,
                                new CubeDeformation(0.25F))
                        .texOffs(0, 52)
                        .addBox(2.05F, -10.35F, 0.25F, 1.5F, 1.5F, 2.3F,
                                new CubeDeformation(0.25F)), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 20)
                        .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F,
                                new CubeDeformation(0.75F)),
                PartPose.ZERO);
        body.addOrReplaceChild("tail_root", CubeListBuilder.create().texOffs(24, 20)
                        .addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 9.0F),
                PartPose.offsetAndRotation(0.0F, 7.0F, 2.0F,
                        -(float) Math.toRadians(57.5F), 0.0F, 0.0F));
        body.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(24, 31)
                        .addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 9.0F),
                PartPose.offsetAndRotation(0.0F, 13.8F, 6.3F,
                        -(float) Math.toRadians(32.5F), 0.0F, 0.0F));

        if (slimArms) {
            root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 36)
                            .addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F,
                                    new CubeDeformation(0.5F)),
                    PartPose.offset(-5.0F, 2.0F, 0.0F));
            root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 0)
                            .addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F,
                                    new CubeDeformation(0.5F)),
                    PartPose.offset(5.0F, 2.0F, 0.0F));
        } else {
            root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 36)
                            .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                                    new CubeDeformation(0.5F)),
                    PartPose.offset(-5.0F, 2.0F, 0.0F));
            root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 0)
                            .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                                    new CubeDeformation(0.5F)),
                    PartPose.offset(5.0F, 2.0F, 0.0F));
        }
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        rightLeg.addOrReplaceChild("legging", CubeListBuilder.create().texOffs(46, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                                new CubeDeformation(0.5F)), PartPose.ZERO);
        rightLeg.addOrReplaceChild("boot", CubeListBuilder.create().texOffs(48, 32)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                                new CubeDeformation(0.75F)), PartPose.ZERO);
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        leftLeg.addOrReplaceChild("legging", CubeListBuilder.create().texOffs(16, 42)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                                new CubeDeformation(0.5F)), PartPose.ZERO);
        leftLeg.addOrReplaceChild("boot", CubeListBuilder.create().texOffs(32, 42)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                                new CubeDeformation(0.75F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    public void setVisibleFor(EquipmentSlot slot) {
        setAllVisible(false);
        tailRoot.visible = false;
        tailTip.visible = false;
        rightLegging.visible = false;
        leftLegging.visible = false;
        rightBoot.visible = false;
        leftBoot.visible = false;
        switch (slot) {
            case HEAD -> head.getAllParts().forEach(part -> part.visible = true);
            case CHEST -> {
                body.visible = true;
                rightArm.visible = true;
                leftArm.visible = true;
                tailRoot.getAllParts().forEach(part -> part.visible = true);
                tailTip.getAllParts().forEach(part -> part.visible = true);
            }
            case LEGS -> {
                rightLeg.visible = true;
                leftLeg.visible = true;
                rightLegging.visible = true;
                leftLegging.visible = true;
            }
            case FEET -> {
                rightLeg.visible = true;
                leftLeg.visible = true;
                rightBoot.visible = true;
                leftBoot.visible = true;
            }
            default -> { }
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                               int packedOverlay, int packedColor) {
        // The supplied helmet uses independent per-face UV rectangles. A
        // vanilla ModelPart cube cannot express those UV islands exactly, so
        // render the authored Blockbench head in the copied humanoid transform.
        boolean renderExactHelmet = head.visible;
        if (renderExactHelmet) head.visible = false;
        super.renderToBuffer(poseStack, consumer, packedLight, packedOverlay, packedColor);
        if (!renderExactHelmet) return;

        head.visible = true;
        poseStack.pushPose();
        head.translateAndRotate(poseStack);
        RuntimeBlockbenchModel.getInflated(HELMET_MODEL).render(
                poseStack, consumer, packedLight, packedOverlay,
                RuntimeBlockbenchModel.GroupSelection.ALL,
                RuntimeBlockbenchModel.HeadMotion.NONE,
                FastColor.ARGB32.red(packedColor),
                FastColor.ARGB32.green(packedColor),
                FastColor.ARGB32.blue(packedColor),
                FastColor.ARGB32.alpha(packedColor));
        poseStack.popPose();
    }
}
