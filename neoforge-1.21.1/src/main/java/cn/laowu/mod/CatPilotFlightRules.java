package cn.laowu.mod;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Uncapped stat formulas; clients send validated controls, never position or speed. */
public final class CatPilotFlightRules {
    public static long durationTicks(double stamina) {
        return Math.round((5 + 0.25 * finiteStat(stamina)) * 20);
    }
    public static double speedPerTick(double speed) {
        return (4 + 0.06 * finiteStat(speed)) / 20;
    }
    private static double finiteStat(double value) { return Double.isFinite(value) ? Math.max(0, value) : 0; }
    public static boolean validInput(float forward, float side, float yaw, float pitch) {
        return Float.isFinite(forward) && Float.isFinite(side) && Float.isFinite(yaw) && Float.isFinite(pitch)
                && Math.abs(forward) <= 1 && Math.abs(side) <= 1 && Math.abs(pitch) <= 90;
    }
    public static Vec3 step(Vec3 previous, float forward, float side, float yaw, float pitch,
                            boolean up, boolean down, boolean gliding, double speed) {
        if (!validInput(forward, side, yaw, pitch) || !Double.isFinite(speed) || speed < 0) return Vec3.ZERO;
        yaw = Mth.wrapDegrees(yaw);
        if (gliding && Math.abs(forward) + Math.abs(side) < 0.01) forward = 1;
        Vec3 desired = Vec3.directionFromRotation(gliding ? 0 : pitch, yaw).scale(forward)
                .add(Vec3.directionFromRotation(0, yaw - 90).scale(side));
        // Explicit altitude controls override camera pitch; Space + Ctrl cancels vertically.
        if (!gliding && (up || down)) desired = new Vec3(desired.x, (up ? 0.8 : 0) - (down ? 0.8 : 0), desired.z);
        if (desired.lengthSqr() > 1) desired = desired.normalize();
        desired = desired.scale(speed * (gliding ? 0.65 : 1));
        // Ease speed, not heading: turning must not retain the previous direction.
        double desiredSpeed = desired.length();
        Vec3 blended = desiredSpeed > 1.0E-6
                ? desired.scale(Mth.lerp(0.65, previous.length(), desiredSpeed) / desiredSpeed)
                : previous.scale(0.35);
        if (gliding) {
            double descent = -0.1 - 0.2 * Math.max(0, pitch) / 90 - (down ? 0.25 : 0);
            blended = new Vec3(blended.x, Math.min(-0.035, Mth.lerp(0.15, previous.y, descent)), blended.z);
        }
        return blended;
    }
    private CatPilotFlightRules() {}
}
