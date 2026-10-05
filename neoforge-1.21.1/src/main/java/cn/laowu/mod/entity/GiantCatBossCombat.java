package cn.laowu.mod.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Shared deterministic attack timing and kinematics; independent of loader/world APIs. */
public final class GiantCatBossCombat {
    public static final byte SUMMON = 0, IDLE = 1, MELEE = 2, ROLL_WINDUP = 3,
            ROLL = 4, ROLL_RECOVERY = 5, JUMP_WINDUP = 6, JUMP = 7, SLAM_RECOVERY = 8;
    public static final double GRAVITY = 0.08D;
    private final Map<UUID, Integer> lastHits = new HashMap<>();

    public static byte nextPhase(byte phase, int ticks, boolean grounded,
                                 boolean blocked, boolean targetValid) {
        return switch (phase) {
            case SUMMON -> ticks >= 60 ? IDLE : SUMMON;
            // Numeric phase 2 remains reserved for save/network compatibility.
            case MELEE -> IDLE;
            case ROLL_WINDUP -> !targetValid ? ROLL_RECOVERY : ticks >= 16 ? ROLL : ROLL_WINDUP;
            case ROLL -> blocked || ticks >= 60 ? ROLL_RECOVERY : ROLL;
            case ROLL_RECOVERY -> ticks >= 30 ? IDLE : ROLL_RECOVERY;
            case JUMP_WINDUP -> !targetValid ? SLAM_RECOVERY : ticks >= 24 ? JUMP : JUMP_WINDUP;
            case JUMP -> (grounded && ticks > 1) || ticks >= 100 ? SLAM_RECOVERY : JUMP;
            case SLAM_RECOVERY -> ticks >= 40 ? IDLE : SLAM_RECOVERY;
            default -> IDLE;
        };
    }

    public static float steer(float current, float target) {
        float difference = ((target - current + 180.0F) % 360.0F + 360.0F) % 360.0F - 180.0F;
        return current + Math.max(-3.0F, Math.min(3.0F, difference));
    }

    /** Turn perpendicular to the movement heading locked when anticipation began. */
    public static float rollWindupYaw(float entryBodyYaw, float movementYaw, float ticks) {
        return easedYaw(entryBodyYaw, movementYaw + 90.0F, ticks / 16.0F);
    }

    /** Standing and cancelled recoveries retain their real entry heading. */
    public static float rollRecoveryYaw(float entryBodyYaw, float movementYaw, float ticks) {
        return entryBodyYaw;
    }

    /**
     * Artist head and muzzle at scale four, in blocks relative to the feet.
     * Neutral cubes are x ±.625 / y 1.5..2.5 / forward 1.75..3,
     * and x ±.375 / y 1.50025..2.00025 / forward 2.75..3.25.
     * A .15-block allowance covers the small authored gait/idle deformation.
     * Horizontal SAT keeps diagonal corners outside the rotated head harmless.
     */
    public static boolean headTouches(float bodyYaw, double minX, double minY, double minZ,
                                      double maxX, double maxY, double maxZ) {
        if (!Float.isFinite(bodyYaw) || !Double.isFinite(minX) || !Double.isFinite(minY)
                || !Double.isFinite(minZ) || !Double.isFinite(maxX) || !Double.isFinite(maxY)
                || !Double.isFinite(maxZ)) return false;
        // Small victims can be bitten below the neutral head pose. Keep this
        // reach in the same forward footprint and above the ground plane.
        double low = maxY - minY <= 1.35D ? 0.0D : 1.35D;
        return headBoxTouches(bodyYaw, minX, minY, minZ, maxX, maxY, maxZ, .775, low, 2.65, 1.6, 3.15)
                || headBoxTouches(bodyYaw, minX, minY, minZ, maxX, maxY, maxZ, .525, low, 2.15, 2.6, 3.4);
    }

    private static boolean headBoxTouches(float yaw, double minX, double minY, double minZ,
                                          double maxX, double maxY, double maxZ,
                                          double halfWidth, double low, double high,
                                          double back, double front) {
        if (maxY <= low || minY >= high) return false;
        double radians = Math.toRadians(yaw), sin = Math.sin(radians), cos = Math.cos(radians);
        double center = (back + front) * .5, halfDepth = (front - back) * .5;
        double dx = (minX + maxX) * .5 + sin * center;
        double dz = (minZ + maxZ) * .5 - cos * center;
        double hx = (maxX - minX) * .5, hz = (maxZ - minZ) * .5;
        double ac = Math.abs(cos), as = Math.abs(sin);
        return Math.abs(dx) < hx + ac * halfWidth + as * halfDepth
                && Math.abs(dz) < hz + as * halfWidth + ac * halfDepth
                && Math.abs(dx * cos + dz * sin) < halfWidth + hx * ac + hz * as
                && Math.abs(-dx * sin + dz * cos) < halfDepth + hx * as + hz * ac;
    }

    private static float easedYaw(float from, float to, float progress) {
        float t = Math.max(0.0F, Math.min(1.0F, progress));
        float difference = ((to - from + 180.0F) % 360.0F + 360.0F) % 360.0F - 180.0F;
        return from + difference * t * t * (3.0F - 2.0F * t);
    }

    /** Upgrade a saved maximum without healing the boss's wounded fraction or reviving it. */
    public static float healthAfterMaxChange(float health, float previousMaximum, float nextMaximum) {
        if (!Float.isFinite(health) || !Float.isFinite(previousMaximum) || !Float.isFinite(nextMaximum)
                || health <= 0.0F || previousMaximum <= 0.0F || nextMaximum <= 0.0F) return 0.0F;
        return Math.min(1.0F, health / previousMaximum) * nextMaximum;
    }

    /** Discrete, drag-free ballistic launch: move first, then subtract gravity each tick. */
    public static double[] leap(double dx, double dy, double dz) {
        double length = Math.hypot(dx, dz);
        double scale = length > 16.0D ? 16.0D / length : 1.0D;
        double height = Math.max(-6.0D, Math.min(6.0D, dy));
        return new double[] {dx * scale / 24.0D,
                (height + GRAVITY * 24.0D * 23.0D / 2.0D) / 24.0D,
                dz * scale / 24.0D};
    }

    public boolean canHit(UUID victim, int tick) {
        Integer previous = lastHits.get(victim);
        return previous == null || tick - previous >= 20;
    }

    public void recordHit(UUID victim, int tick) {
        lastHits.put(victim, tick);
    }

    public void clearHits() {
        lastHits.clear();
    }
}
