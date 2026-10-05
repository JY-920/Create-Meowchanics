package cn.laowu.mod.client;

import cn.laowu.mod.entity.CatGiantCarrier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Cat;
import java.util.Map;
import java.util.WeakHashMap;

/** One per-cat visual body heading shared by cat and rider, independent of render order. */
public final class GiantCatVisualHeading {
    private static final Map<Cat, State> STATES = new WeakHashMap<>();

    public static float yaw(Cat cat, float partialTick, float restWeight) {
        boolean riding = cat.getVehicle() instanceof CatGiantCarrier;
        float target = riding
                ? Mth.rotLerp(partialTick, cat.getVehicle().yRotO, cat.getVehicle().getYRot())
                : Mth.rotLerp(partialTick, cat.yBodyRotO, cat.yBodyRot);
        boolean resting = !riding && cat.onGround() && cat.isInSittingPose();
        if (!riding && !cat.isInSittingPose()) {
            double dx = cat.getX() - cat.xo, dz = cat.getZ() - cat.zo;
            if (dx * dx + dz * dz > .0004) target = (float)Math.toDegrees(Math.atan2(-dx, dz));
        }
        float age = cat.tickCount + partialTick;
        State state = STATES.get(cat);
        if (state == null || age < state.age) {
            state = new State(age, target); STATES.put(cat, state);
        }
        float ticks = Math.max(0, Math.min(2, age - state.age));
        state.age = age;
        state.yaw = GiantCatFacing.bodyYaw(state.yaw, target, ticks, riding, resting, restWeight);
        return state.yaw;
    }

    public static void clearCache() { STATES.clear(); }
    private static final class State {
        float age, yaw;
        State(float age, float yaw) { this.age = age; this.yaw = yaw; }
    }
    private GiantCatVisualHeading() {}
}
