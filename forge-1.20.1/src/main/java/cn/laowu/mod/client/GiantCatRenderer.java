package cn.laowu.mod.client;

import cn.laowu.mod.CatGiantMount;
import cn.laowu.mod.LaoWuMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Draws the artist's unmodified silhouette using the actual cat's resolved coat. */
public final class GiantCatRenderer extends EntityRenderer<Cat> {
    private static final ResourceLocation MODEL=LaoWuMod.id("models/entity/giant_cat_mount.bbmodel");
    private final java.util.Map<Cat,Facing> facing=new java.util.WeakHashMap<>();
    private static final class Facing {
        float age,yaw,headYaw,pitch;
        final GiantCatLook.State targets = new GiantCatLook.State();
        Facing(float age,float yaw){this.age=age;this.yaw=yaw;}
    }
    public GiantCatRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(Cat cat) { return CatGenomeTextureManager.resolve(cat); }
    @Override public void render(Cat cat,float yaw,float partialTick,PoseStack pose,MultiBufferSource buffers,int light) {
        var mc=Minecraft.getInstance();
        if (!cat.isInvisible() || mc.player!=null && !cat.isInvisibleTo(mc.player) || mc.shouldEntityAppearGlowing(cat)) {
            pose.pushPose();
            boolean riding=cat.getVehicle() instanceof cn.laowu.mod.entity.CatGiantCarrier;
            var animation=GiantCatAnimation.sample(cat,partialTick);
            float restWeight=GiantCatAnimation.restWeight(cat);
            float targetYaw=GiantCatVisualHeading.yaw(cat,partialTick,restWeight);
            float age=cat.tickCount+partialTick;
            Facing look=facing.get(cat);
            if(look==null){look=new Facing(age,targetYaw);facing.put(cat,look);}
            float ticks=Math.max(0,Math.min(2,age-look.age));look.age=age;
            look.yaw=targetYaw;
            float desiredHead=riding?0:Mth.clamp(Mth.wrapDegrees(Mth.rotLerp(partialTick,cat.yHeadRotO,cat.yHeadRot)-look.yaw),-35,35);
            float desiredPitch=riding?0:Mth.clamp(cat.getXRot(),-20,20);
            HeadFrame restingHead = !riding && restWeight > .002F
                    ? headFrame(cat, partialTick, look.yaw, animation) : null;
            float restYaw = 0.0F, restPitch = 0.0F;
            if (restingHead != null) {
                var target = look.targets.select(nearbyTargets(cat, partialTick, restingHead.position),
                        look.yaw, ticks);
                if (target != null) {
                    float[] angles = GiantCatLook.boundedAngles(
                            target.dx(), target.dy(), target.dz(), look.yaw);
                    restYaw = angles[0];
                    restPitch = angles[1];
                }
            }
            desiredHead = Mth.lerp(restWeight, desiredHead, restYaw);
            desiredPitch = Mth.lerp(restWeight, desiredPitch, restPitch);
            look.headYaw=GiantCatFacing.approach(look.headYaw,desiredHead,ticks,
                    Mth.lerp(restWeight, 6.0F, 2.0F));
            look.pitch=GiantCatFacing.approach(look.pitch,desiredPitch,ticks,
                    Mth.lerp(restWeight, 5.0F, 1.5F));
            float bodyYaw=look.yaw;
            pose.mulPose(Axis.YP.rotationDegrees(180-bodyYaw));
            float scale=CatGiantMount.SCALE*CatGiantMount.sizeFactor(cat);
            pose.scale(-scale,-scale,scale);
            // RuntimeBlockbenchModel's root pivot is Java-model Y=24; source floor is Y=0.
            pose.translate(0,-1.5,0);
            RenderType type=cat.isInvisible()
                    ?(mc.shouldEntityAppearGlowing(cat)?RenderType.outline(getTextureLocation(cat)):RenderType.itemEntityTranslucentCull(getTextureLocation(cat)))
                    :RenderType.entityCutoutNoCull(getTextureLocation(cat));
            var headMotion=new RuntimeBlockbenchModel.HeadMotion(look.pitch*Mth.DEG_TO_RAD,look.headYaw*Mth.DEG_TO_RAD,0);
            if (restingHead != null) {
                var baseline = restingHead.baseline;
                float[] delta = GiantCatLook.restEulerDelta(bodyYaw, look.headYaw, look.pitch,
                        restingHead.parentWorld, baseline.xRot(), baseline.yRot(), baseline.zRot());
                headMotion = new RuntimeBlockbenchModel.HeadMotion(delta[0], delta[1], delta[2]);
            }
            RuntimeBlockbenchModel.get(MODEL).render(pose,buffers.getBuffer(type),light,
                    LivingEntityRenderer.getOverlayCoords(cat,0),RuntimeBlockbenchModel.GroupSelection.ALL_GROUPS,
                    headMotion,animation);
            GiantCatHeadOutfit.render(cat,partialTick,pose,buffers,light,headMotion,animation);
            pose.popPose();
        }
        super.render(cat,yaw,partialTick,pose,buffers,light);
    }

    /** Camera-independent world pose of the head, before visual target correction. */
    private static HeadFrame headFrame(Cat cat, float partialTick, float bodyYaw,
                                        java.util.Map<String, RuntimeBlockbenchModel.GroupTransform> animation) {
        PoseStack world = new PoseStack();
        world.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        float scale = CatGiantMount.SCALE*CatGiantMount.sizeFactor(cat);
        world.scale(-scale, -scale, scale);
        world.translate(0.0D, -1.5D, 0.0D);
        if (!RuntimeBlockbenchModel.get(MODEL).translateToGroup(world, "head",
                RuntimeBlockbenchModel.HeadMotion.NONE, animation)) return null;
        Vector3f offset = world.last().pose().transformPosition(new Vector3f());
        Vec3 position = new Vec3(Mth.lerp(partialTick, cat.xo, cat.getX()) + offset.x,
                Mth.lerp(partialTick, cat.yo, cat.getY()) + offset.y,
                Mth.lerp(partialTick, cat.zo, cat.getZ()) + offset.z);
        var baseline = animation.getOrDefault("head", RuntimeBlockbenchModel.GroupTransform.IDENTITY);
        Quaternionf parent = world.last().pose().getUnnormalizedRotation(new Quaternionf()).normalize()
                .mul(new Quaternionf().rotationZYX(baseline.zRot(), baseline.yRot(), baseline.xRot()).conjugate());
        return new HeadFrame(position, parent, baseline);
    }

    private static java.util.List<GiantCatLook.Candidate> nearbyTargets(
            Cat cat, float partialTick, Vec3 head) {
        java.util.List<GiantCatLook.Candidate> candidates = new java.util.ArrayList<>();
        for (LivingEntity entity : cat.level().getEntitiesOfClass(
                LivingEntity.class, new net.minecraft.world.phys.AABB(head, head).inflate(8.0D))) {
            Vec3 eyes = entity.getEyePosition(partialTick);
            candidates.add(new GiantCatLook.Candidate(entity.getId(), entity instanceof Player,
                    entity.isAlive(), entity.isInvisible(), entity.isSpectator(),
                    entity == cat || entity.getRootVehicle() == cat.getRootVehicle(),
                    eyes.x - head.x, eyes.y - head.y, eyes.z - head.z));
        }
        return candidates;
    }

    private record HeadFrame(Vec3 position, Quaternionf parentWorld,
                             RuntimeBlockbenchModel.GroupTransform baseline) {
    }
}
