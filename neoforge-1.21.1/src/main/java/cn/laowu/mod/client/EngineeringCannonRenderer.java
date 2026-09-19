package cn.laowu.mod.client;

import cn.laowu.mod.entity.EngineeringCannon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.DyeColor;

/** Uses Create's existing block and partial models; no fake block entity, shaders or copied textures. */
public final class EngineeringCannonRenderer extends EntityRenderer<EngineeringCannon> {
    public EngineeringCannonRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.6F;
    }
    @Override public void render(EngineeringCannon cannon, float entityYaw, float partialTick,
                                 PoseStack pose, MultiBufferSource buffers, int light) {
        float yaw = Mth.rotLerp(partialTick, cannon.yRotO, cannon.getYRot());
        float pitch = Mth.lerp(partialTick, cannon.xRotO, cannon.getXRot());
        var state = AllBlocks.SCHEMATICANNON.getDefaultState();
        var blocks = Minecraft.getInstance().getBlockRenderer();
        // Extend the thin floating deck behind the gun instead of seating the cat on its barrel.
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        pose.translate(-0.5, 0.045, -EngineeringCannon.SEAT_BACK - 0.45);
        pose.scale(1, 0.08F, (float)(EngineeringCannon.SEAT_BACK + 0.95));
        blocks.renderSingleBlock(AllBlocks.INDUSTRIAL_IRON_BLOCK.getDefaultState(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        pose.pushPose();
        pose.scale(EngineeringCannon.MODEL_SCALE, EngineeringCannon.MODEL_SCALE, EngineeringCannon.MODEL_SCALE);
        pose.translate(-0.5, 0, -0.5);
        blocks.renderSingleBlock(state, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-yaw - 90));
        pose.scale(EngineeringCannon.MODEL_SCALE, EngineeringCannon.MODEL_SCALE, EngineeringCannon.MODEL_SCALE);
        pose.pushPose();
        pose.translate(-0.5, 0, -0.5);
        CachedBuffers.partial(AllPartialModels.SCHEMATICANNON_CONNECTOR, state).light(light)
                .renderInto(pose, buffers.getBuffer(RenderType.cutoutMipped()));
        pose.popPose();
        // The original pipe points along +Y, pivots at (8,15,8), and rotates about its Z axle.
        pose.translate(0, 15.0 / 16.0, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(-90 - pitch));
        pose.translate(-0.5, -15.0 / 16.0 - 0.13 * cannon.recoil(partialTick), -0.5);
        CachedBuffers.partial(AllPartialModels.SCHEMATICANNON_PIPE, state).light(light)
                .renderInto(pose, buffers.getBuffer(RenderType.cutoutMipped()));
        pose.popPose();

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        pose.translate(-0.375, EngineeringCannon.SEAT_TOP - 0.125, -EngineeringCannon.SEAT_BACK - 0.375);
        pose.scale(0.75F, 0.25F, 0.75F);
        DyeColor color = cannon.getFirstPassenger() instanceof Cat cat ? cat.getCollarColor() : DyeColor.WHITE;
        blocks.renderSingleBlock(AllBlocks.SEATS.get(color).getDefaultState(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(cannon, entityYaw, partialTick, pose, buffers, light);
    }
    @Override public ResourceLocation getTextureLocation(EngineeringCannon cannon) { return TextureAtlas.LOCATION_BLOCKS; }
}
