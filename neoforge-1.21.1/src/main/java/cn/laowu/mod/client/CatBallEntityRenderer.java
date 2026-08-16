package cn.laowu.mod.client;

import cn.laowu.mod.entity.CatBallEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

/** Renders the cat ball with its stored yaw instead of camera billboarding. */
public final class CatBallEntityRenderer extends EntityRenderer<CatBallEntity> {
    private final ItemRenderer itemRenderer;

    public CatBallEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemRenderer = context.getItemRenderer();
        shadowRadius = 0.32F;
    }

    @Override
    public void render(CatBallEntity ball, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        pose.pushPose();
        // The model's face points along local -Z. Minecraft yaw 0 points +Z.
        // Reading the current value directly makes a kick turn instantaneous.
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - ball.getYRot()));
        // NONE is reserved here for the full-sized, kickable entity. Ordinary
        // dropped item stacks use GROUND and are intentionally rendered smaller.
        itemRenderer.renderStatic(ball.getItem(), ItemDisplayContext.NONE,
                packedLight, OverlayTexture.NO_OVERLAY, pose, buffers,
                ball.level(), ball.getId());
        pose.popPose();
        super.render(ball, entityYaw, partialTick, pose, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CatBallEntity ball) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
