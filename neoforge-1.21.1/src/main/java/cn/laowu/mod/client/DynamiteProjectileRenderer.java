package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.entity.DynamiteProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Renders the supplied dynamite model tumbling along its flight path. */
public final class DynamiteProjectileRenderer
        extends EntityRenderer<DynamiteProjectile> {
    private static final ResourceLocation MODEL =
            LaoWuMod.id("models/entity/dynamite_projectile.bbmodel");
    private static final ResourceLocation TEXTURE =
            LaoWuMod.id("textures/entity/dynamite_projectile.png");

    public DynamiteProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(DynamiteProjectile dynamite, float entityYaw,
                       float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        Vec3 motion = dynamite.getDeltaMovement();
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float yaw = motion.lengthSqr() < 1.0E-7D ? entityYaw
                : (float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG);
        float pitch = motion.lengthSqr() < 1.0E-7D ? 0.0F
                : (float) (Mth.atan2(motion.y, horizontal) * Mth.RAD_TO_DEG);
        float spin = (dynamite.tickCount + partialTick) * 28.0F;

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.mulPose(Axis.XP.rotationDegrees(-pitch));
        pose.mulPose(Axis.ZP.rotationDegrees(spin));
        // The authored model spans Y=1.2..6.8 px. The runtime uses a 24 px
        // entity root, so -20 px places its exact centre on the projectile.
        pose.translate(0.0D, -20.0D / 16.0D, 0.0D);
        RuntimeBlockbenchModel.get(MODEL).render(pose,
                buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                packedLight, OverlayTexture.NO_OVERLAY,
                RuntimeBlockbenchModel.GroupSelection.ALL_GROUPS,
                RuntimeBlockbenchModel.HeadMotion.NONE);
        pose.popPose();

        super.render(dynamite, entityYaw, partialTick, pose, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(DynamiteProjectile entity) {
        return TEXTURE;
    }
}

