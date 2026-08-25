package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.entity.ButterCatBoss;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class ButterCatRenderer extends MobRenderer<ButterCatBoss, ButterCatModel> {
    private static final ResourceLocation CAT_TEXTURE =
            LaoWuMod.id("textures/entity/butter_cat.png");
    private static final ResourceLocation BUTTER_TEXTURE =
            LaoWuMod.id("textures/entity/butter_cat_butter.png");

    public ButterCatRenderer(EntityRendererProvider.Context context) {
        super(context, new ButterCatModel(), 0.65F);
        this.addLayer(new ButterLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(ButterCatBoss entity) {
        return CAT_TEXTURE;
    }

    @Override
    protected void scale(ButterCatBoss entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(ButterCatBoss.MODEL_SCALE, ButterCatBoss.MODEL_SCALE,
                ButterCatBoss.MODEL_SCALE);
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
