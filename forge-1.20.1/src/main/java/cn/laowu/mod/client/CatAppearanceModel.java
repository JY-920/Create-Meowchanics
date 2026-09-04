package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Small trait-only geometry that follows the live vanilla cat bones. */
public final class CatAppearanceModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            LaoWuMod.id("cat_appearance"), "main");

    private final ModelPart eyes;
    private final ModelPart tears;
    private final ModelPart leftHindBoot;
    private final ModelPart rightHindBoot;
    private final ModelPart leftFrontBoot;
    private final ModelPart rightFrontBoot;

    public CatAppearanceModel(ModelPart root) {
        eyes = root.getChild("eyes");
        tears = root.getChild("tears");
        leftHindBoot = root.getChild("left_hind_boot");
        rightHindBoot = root.getChild("right_hind_boot");
        leftFrontBoot = root.getChild("left_front_boot");
        rightFrontBoot = root.getChild("right_front_boot");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation slightInflate = new CubeDeformation(0.015F);

        root.addOrReplaceChild("eyes", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-2.48F, -1.0F, -3.08F,
                                1.95F, 0.95F, 0.08F, slightInflate)
                        .addBox(0.53F, -1.0F, -3.08F,
                                1.95F, 0.95F, 0.08F, slightInflate),
                PartPose.ZERO);
        root.addOrReplaceChild("tears", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.95F, -0.05F, -3.10F,
                                0.40F, 3.0F, 0.08F, slightInflate)
                        .addBox(1.55F, -0.05F, -3.10F,
                                0.40F, 3.0F, 0.08F, slightInflate),
                PartPose.ZERO);

        CubeListBuilder hindBoot = CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.2F, 4.55F, 0.75F,
                        2.4F, 2.15F, 2.75F, new CubeDeformation(0.04F));
        CubeListBuilder frontBoot = CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.2F, 8.45F, -0.25F,
                        2.4F, 2.15F, 2.75F, new CubeDeformation(0.04F));
        root.addOrReplaceChild("left_hind_boot", hindBoot, PartPose.ZERO);
        root.addOrReplaceChild("right_hind_boot", hindBoot, PartPose.ZERO);
        root.addOrReplaceChild("left_front_boot", frontBoot, PartPose.ZERO);
        root.addOrReplaceChild("right_front_boot", frontBoot, PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16);
    }

    public void renderEyes(HissingCatModel catModel, PoseStack pose,
                           VertexConsumer consumer, int light, int overlay) {
        eyes.copyFrom(catModel.headPart());
        eyes.visible = true;
        eyes.render(pose, consumer, light, overlay);
    }

    public void renderTears(HissingCatModel catModel, PoseStack pose,
                            VertexConsumer consumer, int light, int overlay) {
        tears.copyFrom(catModel.headPart());
        tears.visible = true;
        tears.render(pose, consumer, light, overlay,
                0.42F, 0.78F, 1.0F, 0.82F);
    }

    public void renderBoots(HissingCatModel catModel, PoseStack pose,
                            VertexConsumer consumer, int light, int overlay) {
        leftHindBoot.copyFrom(catModel.leftHindLegPart());
        rightHindBoot.copyFrom(catModel.rightHindLegPart());
        leftFrontBoot.copyFrom(catModel.leftFrontLegPart());
        rightFrontBoot.copyFrom(catModel.rightFrontLegPart());
        leftHindBoot.visible = true;
        rightHindBoot.visible = true;
        leftFrontBoot.visible = true;
        rightFrontBoot.visible = true;
        leftHindBoot.render(pose, consumer, light, overlay);
        rightHindBoot.render(pose, consumer, light, overlay);
        leftFrontBoot.render(pose, consumer, light, overlay);
        rightFrontBoot.render(pose, consumer, light, overlay);
    }
}
