package cn.laowu.mod;
import net.minecraft.world.phys.Vec3;
/** Same uncapped pilot stamina/speed formulas; +30% swimming, -80% movement on land. */
public final class CatDivingRules {
    public static final double WATER_MULTIPLIER = 1.30, LAND_MULTIPLIER = .20;
    public static double speed(double stat, boolean wet) {
        return CatPilotFlightRules.speedPerTick(stat) * WATER_MULTIPLIER * (wet ? 1 : LAND_MULTIPLIER);
    }
    public static Vec3 swim(Vec3 previous, float forward, float side, float yaw, float pitch,
                            boolean up, boolean down, boolean exhausted, double speed) {
        Vec3 motion = CatPilotFlightRules.step(previous, forward, side, yaw, exhausted ? 0 : pitch,
                up, !exhausted && down, false, speed * (exhausted ? .45 : 1));
        return exhausted ? new Vec3(motion.x, Math.max(.12, motion.y), motion.z) : motion;
    }
    private CatDivingRules() {}
}
