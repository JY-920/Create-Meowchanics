package cn.laowu.mod;

import net.minecraft.world.phys.Vec3;

/** Bounded steering shared by commanded flight and its offline regression tests. */
public final class CatFlightMotion {
    public static Vec3 step(Vec3 position, Vec3 target, Vec3 velocity, double speed) {
        Vec3 delta = target.subtract(position);
        double distance = delta.length();
        double cappedSpeed = Math.max(0, Math.min(0.45D, speed));
        Vec3 desired = distance < 0.03D ? Vec3.ZERO
                : delta.scale(Math.min(cappedSpeed, distance * 0.5D) / distance);
        Vec3 next = velocity.scale(0.65D).add(desired.scale(0.35D));
        double length = next.length();
        if (length > 0.45D) next = next.scale(0.45D / length);
        return new Vec3(next.x, Math.max(-0.28D, Math.min(0.28D, next.y)), next.z);
    }
    private CatFlightMotion() {}
}
