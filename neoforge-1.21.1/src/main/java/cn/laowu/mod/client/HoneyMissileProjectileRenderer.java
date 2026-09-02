package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.entity.HoneyMissileProjectile;
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

/** Renders the supplied honey missile model along its flight vector. */
public final class HoneyMissileProjectileRenderer
        extends EntityRenderer<HoneyMissileProjectile> {
    private static final ResourceLocation MODEL =
            LaoWuMod.id("models/entity/honey_missile.geo.json");
    private static final ResourceLocation TEXTURE =
            LaoWuMod.id("textures/entity/honey_missile.png");

    public HoneyMissileProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(HoneyMissileProjectile missile, float entityYaw,
                       float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        Vec3 motion = missile.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-7D) return;

        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float yaw = (float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (Mth.atan2(motion.y, horizontal) * Mth.RAD_TO_DEG);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.mulPose(Axis.XP.rotationDegrees(-pitch));
        // The Bedrock model is centred at Y=6 px and Z=-0.5 px after the
        // runtime's 24 px entity-root transform.
        pose.translate(0.0D, -18.0D / 16.0D, 0.5D / 16.0D);
        RuntimeBlockbenchModel.get(MODEL).render(pose,
                buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                packedLight, OverlayTexture.NO_OVERLAY,
                RuntimeBlockbenchModel.GroupSelection.ALL_GROUPS,
                RuntimeBlockbenchModel.HeadMotion.NONE);
        pose.popPose();

        super.render(missile, entityYaw, partialTick, pose, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(HoneyMissileProjectile entity) {
        return TEXTURE;
    }
}

