package cn.laowu.mod.client;

import cn.laowu.mod.entity.FishingRodProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.phys.Vec3;

/** Renders the fishing cat's attack as a vanilla bobber with a curved line. */
public final class FishingRodProjectileRenderer
        extends EntityRenderer<FishingRodProjectile> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "textures/entity/fishing_hook.png");
    private static final RenderType HOOK_RENDER_TYPE = RenderType.entityCutout(TEXTURE);
    private static final int LINE_SEGMENTS = 16;

    public FishingRodProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FishingRodProjectile hook, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        PoseStack.Pose hookPose = poseStack.last();
        VertexConsumer hookBuffer = buffers.getBuffer(HOOK_RENDER_TYPE);
        vertex(hookBuffer, hookPose, packedLight, 0.0F, 0, 0, 1);
        vertex(hookBuffer, hookPose, packedLight, 1.0F, 0, 1, 1);
        vertex(hookBuffer, hookPose, packedLight, 1.0F, 1, 1, 0);
        vertex(hookBuffer, hookPose, packedLight, 0.0F, 1, 0, 0);
        poseStack.popPose();

        Cat owner = hook.getCatOwnerForRender();
        if (owner != null) {
            Vec3 look = owner.getViewVector(partialTick);
            double ownerX = Mth.lerp(partialTick, owner.xo, owner.getX())
                    + look.x * 0.35D;
            double ownerY = Mth.lerp(partialTick, owner.yo, owner.getY())
                    + owner.getEyeHeight() * 0.82D + look.y * 0.15D;
            double ownerZ = Mth.lerp(partialTick, owner.zo, owner.getZ())
                    + look.z * 0.35D;

            double hookX = Mth.lerp(partialTick, hook.xo, hook.getX());
            double hookY = Mth.lerp(partialTick, hook.yo, hook.getY()) + 0.25D;
            double hookZ = Mth.lerp(partialTick, hook.zo, hook.getZ());
            float lineX = (float) (ownerX - hookX);
            float lineY = (float) (ownerY - hookY);
            float lineZ = (float) (ownerZ - hookZ);

            VertexConsumer lineBuffer = buffers.getBuffer(RenderType.lineStrip());
            PoseStack.Pose linePose = poseStack.last();
            for (int segment = 0; segment <= LINE_SEGMENTS; segment++) {
                stringVertex(lineX, lineY, lineZ, lineBuffer, linePose,
                        fraction(segment, LINE_SEGMENTS),
                        fraction(segment + 1, LINE_SEGMENTS));
            }
        }

        poseStack.popPose();
        super.render(hook, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    private static float fraction(int numerator, int denominator) {
        return (float) numerator / denominator;
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               int packedLight, float x, int y, int u, int v) {
        consumer.addVertex(pose, x - 0.5F, y - 0.5F, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    /** Matches vanilla FishingHookRenderer's slightly sagging line curve. */
    private static void stringVertex(float x, float y, float z,
                                     VertexConsumer consumer, PoseStack.Pose pose,
                                     float start, float end) {
        float px = x * start;
        float py = y * (start * start + start) * 0.5F + 0.25F;
        float pz = z * start;
        float nx = x * end - px;
        float ny = y * (end * end + end) * 0.5F + 0.25F - py;
        float nz = z * end - pz;
        float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 1.0E-5F) {
            nx /= length;
            ny /= length;
            nz /= length;
        }
        consumer.addVertex(pose, px, py, pz)
                .setColor(0, 0, 0, 255)
                .setNormal(pose, nx, ny, nz);
    }

    @Override
    public ResourceLocation getTextureLocation(FishingRodProjectile entity) {
        return TEXTURE;
    }
}
