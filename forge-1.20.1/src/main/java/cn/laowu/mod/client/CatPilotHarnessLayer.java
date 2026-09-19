package cn.laowu.mod.client;

import cn.laowu.mod.CatClothesData;
import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.CatPoseData;
import cn.laowu.mod.entity.CatFlightCarrier;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;

/** One torso band, with a small belly-to-rail gap for Create's raised wrench jaws. */
public final class CatPilotHarnessLayer extends RenderLayer<Cat, CatModel<Cat>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.tryParse("minecraft:textures/block/iron_block.png");
    private final ModelPart harness = createRoot();

    public CatPilotHarnessLayer(RenderLayerParent<Cat, CatModel<Cat>> parent) { super(parent); }

    public static ModelPart createRoot() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        // Ocelot body: pivot (0,12,-10), X rotation PI/2, cube (-2,3,-8)
        // sized (4,16,6). Its standing center is (0,17,1), NOT the front
        // legs at Z=-5. The torso spans X=-2..2 / Y=14..20; the upper
        // band wraps the back and sides. Its bottom rail starts at Y=23,
        // leaving exactly 3 pixels below the belly for the raised wrench.
        // Back/side inner edges overlap the skin by 1/8 pixel to avoid seams.
        // Keep the same X=0/Z=1 center; no second ring or dangling connector.
        root.addOrReplaceChild("body_band", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3, 13, 0.25F, 6, 1.125F, 1.5F)
                .addBox(-3, 14.125F, 0.25F, 1.125F, 8.875F, 1.5F)
                .addBox(1.875F, 14.125F, 0.25F, 1.125F, 8.875F, 1.5F)
                .addBox(-3, 23, 0.25F, 6, 1.125F, 1.5F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot();
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, Cat cat,
                       float limbSwing, float limbSwingAmount, float partialTick, float age,
                       float headYaw, float headPitch) {
        if (!(cat.getVehicle() instanceof CatFlightCarrier) || cat.isBaby() || cat.isInvisible()
                || CatPoseData.isPancake(cat) || CatClothesData.getOutfit(cat) != CatOutfitType.FLIGHT
                || !(getParentModel() instanceof HissingCatModel model)) return;
        // Share predicted yaw AND the live torso's pose/scale, not the entity root.
        pose.pushPose();
        model.applyBodyPoseDelta(pose);
        harness.render(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                light, LivingEntityRenderer.getOverlayCoords(cat, 0));
        pose.popPose();
    }
}
