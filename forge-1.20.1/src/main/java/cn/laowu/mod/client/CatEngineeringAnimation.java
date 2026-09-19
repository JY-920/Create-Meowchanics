package cn.laowu.mod.client;

import cn.laowu.mod.CatEngineeringBehavior;
import cn.laowu.mod.CatPoseData;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitData;
import com.simibubi.create.content.kinetics.crank.HandCrankBlock;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Cat;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.WeakHashMap;

/** One render-thread state per cat; the crank itself supplies the interpolated clock. */
public final class CatEngineeringAnimation {
    private static final Map<Cat, State> STATES = new WeakHashMap<>();

    private static final class State {
        double time, phase;
        float blend, yaw, scale, seatY;
        boolean baby;
        Vector3f left = new Vector3f(), right = new Vector3f();
        final float[] pose = new float[CatCrankPose.VALUES];
    }

    public static float prepare(Cat cat, float ordinaryYaw, float partialTick) {
        if (!CatCrankPose.available() || !cat.isAlive()
                || CatPoseData.isPancake(cat) || CatPoseData.isHissing(cat)) {
            STATES.remove(cat);
            return ordinaryYaw;
        }
        var crank = CatEngineeringBehavior.findCrank(cat);
        State state = STATES.get(cat);
        if (crank == null && state == null) return ordinaryYaw;
        double now = cat.level().getGameTime() + partialTick;
        if (state == null) {
            state = new State();
            state.time = now;
            STATES.put(cat, state);
        }
        float elapsed = (float) Math.max(0, Math.min(3, now - state.time));
        state.time = now;
        state.blend = Mth.clamp(state.blend + elapsed / (crank != null ? 10 : -8), 0, 1);
        if (crank == null && state.blend <= 0) {
            STATES.remove(cat);
            return ordinaryYaw;
        }
        if (crank != null) state.yaw = CatEngineeringBehavior.facingYaw(crank);
        float renderYaw = Mth.rotLerp(ease(state.blend), ordinaryYaw, state.yaw);
        if (crank != null) {
            var direction = crank.getBlockState().getValue(HandCrankBlock.FACING);
            double angle = crank.getIndependentAngle(partialTick);
            Vector3f left = CatCrankPose.handleOffset(direction.getStepX(), direction.getStepZ(), angle, 4);
            Vector3f right = CatCrankPose.handleOffset(direction.getStepX(), direction.getStepZ(), angle, 7);
            Vector3f canonical = CatCrankPose.toLocal(new Vector3f(left), state.yaw);
            state.phase = Math.atan2(canonical.y, canonical.z) / (Math.PI * 2);
            var pos = crank.getBlockPos();
            Vector3f center = new Vector3f(
                    (float) ((pos.getX() + 0.5 - Mth.lerp(partialTick, cat.xo, cat.getX())) * 16),
                    (float) ((pos.getY() + 0.5 - Mth.lerp(partialTick, cat.yo, cat.getY())) * 16),
                    (float) ((pos.getZ() + 0.5 - Mth.lerp(partialTick, cat.zo, cat.getZ())) * 16));
            state.left = CatCrankPose.toLocal(left.add(center), renderYaw);
            state.right = CatCrankPose.toLocal(right.add(center), renderYaw);
            state.seatY = center.y - 16;
            int level = CatTraitData.read(cat).map(t -> t.level(CatTrait.BIG_CHONKY_CAT)).orElse(0);
            state.scale = level > 0 ? CatTrait.BIG_CHONKY_CAT.bigCatScalePercent(level) / 100.0F : 1;
            state.baby = cat.isBaby();
        }
        return renderYaw;
    }

    public static boolean isPosing(Cat cat) {
        State state = STATES.get(cat);
        return state != null && state.blend > 0;
    }

    public static void apply(Cat cat, ModelPart... bones) {
        State state = STATES.get(cat);
        if (state == null || state.blend <= 0 || bones.length != CatCrankPose.BONES) return;
        CatCrankPose.evaluate(state.phase, state.scale, state.baby, state.seatY,
                state.left, state.right, state.pose);
        float alpha = ease(state.blend);
        for (int b = 0; b < bones.length; b++) {
            ModelPart part = bones[b];
            int i = b * CatCrankPose.STRIDE;
            part.setPos(Mth.lerp(alpha, part.x, state.pose[i]),
                    Mth.lerp(alpha, part.y, state.pose[i + 1]),
                    Mth.lerp(alpha, part.z, state.pose[i + 2]));
            Quaternionf q = new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot);
            q.slerp(new Quaternionf().rotationZYX(state.pose[i + 5], state.pose[i + 4], state.pose[i + 3]), alpha);
            Vector3f r = q.getEulerAnglesZYX(new Vector3f());
            part.xRot = r.x; part.yRot = r.y; part.zRot = r.z;
            part.xScale = Mth.lerp(alpha, part.xScale, state.pose[i + 6]);
            part.yScale = Mth.lerp(alpha, part.yScale, state.pose[i + 7]);
            part.zScale = Mth.lerp(alpha, part.zScale, state.pose[i + 8]);
        }
    }

    private static float ease(float value) { return value * value * (3 - 2 * value); }
    private CatEngineeringAnimation() {}
}
