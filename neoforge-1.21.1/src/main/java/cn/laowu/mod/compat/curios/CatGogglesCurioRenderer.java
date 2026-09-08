package cn.laowu.mod.compat.curios;

import cn.laowu.mod.LaoWuMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Renders the complete goggles model identically with or without head armour. */
@OnlyIn(Dist.CLIENT)
public final class CatGogglesCurioRenderer implements ICurioRenderer {
    private static final ModelResourceLocation WORN_MODEL =
            ModelResourceLocation.standalone(LaoWuMod.id("item/cat_engineer_goggles_worn"));

    private final HumanoidModel<LivingEntity> model;

    public CatGogglesCurioRenderer(ModelPart root) {
        model = new HumanoidModel<>(root);
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack, SlotContext slotContext, PoseStack poseStack,
            RenderLayerParent<T, M> renderLayerParent, MultiBufferSource buffers,
            int packedLight, float limbSwing, float limbSwingAmount,
            float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        LivingEntity wearer = slotContext.entity();
        model.setupAnim(wearer, limbSwing, limbSwingAmount, ageInTicks,
                netHeadYaw, headPitch);
        model.prepareMobModel(wearer, limbSwing, limbSwingAmount, partialTicks);
        ICurioRenderer.followHeadRotations(wearer, model.head);

        poseStack.pushPose();
        poseStack.translate(model.head.x / 16.0D, model.head.y / 16.0D,
                model.head.z / 16.0D);
        poseStack.mulPose(Axis.ZP.rotation(model.head.zRot));
        poseStack.mulPose(Axis.YP.rotation(model.head.yRot));
        poseStack.mulPose(Axis.XP.rotation(model.head.xRot));
        poseStack.translate(0.0D, -0.25D, 0.0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(0.625F, 0.625F, 0.625F);

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getItemRenderer().render(stack, ItemDisplayContext.HEAD, false,
                poseStack, buffers, packedLight, OverlayTexture.NO_OVERLAY,
                minecraft.getModelManager().getModel(WORN_MODEL));
        poseStack.popPose();
    }
}
