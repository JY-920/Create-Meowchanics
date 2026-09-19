package cn.laowu.mod.client;

import cn.laowu.mod.entity.EngineeringCogwheelProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/** A spinning solid Create cogwheel, aligned along its trajectory rather than billboarded. */
public final class EngineeringCogwheelRenderer extends EntityRenderer<EngineeringCogwheelProjectile> {
    public EngineeringCogwheelRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = 0; }
    @Override public void render(EngineeringCogwheelProjectile gear, float entityYaw, float partialTick,
                                 PoseStack pose, MultiBufferSource buffers, int light) {
        var velocity = gear.getDeltaMovement();
        float yaw = (float)(Mth.atan2(-velocity.x, velocity.z) * Mth.RAD_TO_DEG);
        float pitch = (float)(-Mth.atan2(velocity.y, velocity.horizontalDistance()) * Mth.RAD_TO_DEG);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        pose.mulPose(Axis.XP.rotationDegrees(90 + pitch));
        pose.mulPose(Axis.YP.rotationDegrees((gear.tickCount + partialTick) * 40));
        float size = switch (gear.munition()) { case LARGE_COG -> 0.75F; case SHAFT -> 0.65F; default -> 0.45F; };
        pose.scale(size, size, size);
        Minecraft.getInstance().getItemRenderer().renderStatic(gear.getItem(),
                ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffers, gear.level(), gear.getId());
        pose.popPose();
        super.render(gear, entityYaw, partialTick, pose, buffers, light);
    }
    @Override public ResourceLocation getTextureLocation(EngineeringCogwheelProjectile gear) { return TextureAtlas.LOCATION_BLOCKS; }
}
