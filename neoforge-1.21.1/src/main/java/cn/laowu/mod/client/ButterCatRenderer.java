package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.entity.ButterCatBoss;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class ButterCatRenderer extends MobRenderer<ButterCatBoss, ButterCatModel> {
    private static final ResourceLocation CAT_TEXTURE =
            LaoWuMod.id("textures/entity/butter_cat.png");
    private static final ResourceLocation BUTTER_TEXTURE =
            LaoWuMod.id("textures/entity/butter_cat_butter.png");
    private final ItemRenderer itemRenderer;

    public ButterCatRenderer(EntityRendererProvider.Context context) {
        super(context, new ButterCatModel(), 0.65F);
        this.itemRenderer = context.getItemRenderer();
        this.addLayer(new ButterLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(ButterCatBoss entity) {
        return entity.getInheritedGenome()
                .map(genome -> CatGenomeTextureManager.resolve(genome, CAT_TEXTURE))
                .orElse(CAT_TEXTURE);
    }

    @Override
    protected void scale(ButterCatBoss entity, PoseStack poseStack, float partialTickTime) {
        float scale = entity.isSummoning() ? 1.0F : ButterCatBoss.MODEL_SCALE;
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public void render(ButterCatBoss entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
        if (!entity.isSummoning()) return;

        float progress = entity.getSummonProgress(partialTick);
        float eased = progress * progress * (3.0F - 2.0F * progress);
        double height = Mth.lerp(eased, 5.2D, 0.72D);
        float pulse = 2.35F
                + Mth.sin((entity.tickCount + partialTick) * 0.18F) * 0.06F;

        poseStack.pushPose();
        poseStack.translate(0.0D, height, 0.0D);
        // The summon prop belongs to the world rather than the camera: keep a
        // small fixed tilt and let it spin continuously around its own Y axis.
        poseStack.mulPose(Axis.YP.rotationDegrees(
                (entity.tickCount + partialTick) * 5.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(18.0F));
        poseStack.scale(pulse, pulse, pulse);
        this.itemRenderer.renderStatic(new ItemStack(LaoWuMod.BUTTER_BREAD.get()),
                ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, buffers, entity.level(), entity.getId());
        poseStack.popPose();
    }

    private static final class ButterLayer extends RenderLayer<ButterCatBoss, ButterCatModel> {
        private ButterLayer(RenderLayerParent<ButterCatBoss, ButterCatModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light,
                           ButterCatBoss entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw,
                           float headPitch) {
            if (entity.isSummoning()) return;
            int overlay = OverlayTexture.NO_OVERLAY;
            if (entity.hurtTime > 0 || entity.deathTime > 0) {
                overlay = net.minecraft.client.renderer.entity.LivingEntityRenderer
                        .getOverlayCoords(entity, 0.0F);
            }
            this.getParentModel().renderButter(poseStack,
                    buffers.getBuffer(RenderType.entityCutoutNoCull(BUTTER_TEXTURE)),
                    light, overlay);
        }
    }
}

