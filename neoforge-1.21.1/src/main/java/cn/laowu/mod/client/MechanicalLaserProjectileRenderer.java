package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.entity.MechanicalLaserProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Renders the supplied 3-D laser model along the projectile's flight vector. */
public final class MechanicalLaserProjectileRenderer
        extends EntityRenderer<MechanicalLaserProjectile> {
    private static final ResourceLocation MODEL =
            LaoWuMod.id("models/entity/mechanical_laser.bbmodel");
    private static final ResourceLocation TEXTURE =
            LaoWuMod.id("textures/entity/mechanical_laser.png");

    public MechanicalLaserProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(MechanicalLaserProjectile laser, float entityYaw,
                       float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        Vec3 motion = laser.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-7D) return;

        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float yaw = (float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (Mth.atan2(motion.y, horizontal) * Mth.RAD_TO_DEG);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.mulPose(Axis.XP.rotationDegrees(-pitch));

        // The authored laser is centred at X=0.5 px, Y=4.5 px. Compensate
        // RuntimeBlockbenchModel's 24 px entity root so it rotates about its centre.
        pose.translate(-0.5D / 16.0D, -19.5D / 16.0D, 0.0D);
        RuntimeBlockbenchModel.get(MODEL).render(pose,
                buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                RuntimeBlockbenchModel.GroupSelection.ALL_GROUPS,
                RuntimeBlockbenchModel.HeadMotion.NONE);
        pose.popPose();

        super.render(laser, entityYaw, partialTick, pose, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MechanicalLaserProjectile entity) {
        return TEXTURE;
    }
}

