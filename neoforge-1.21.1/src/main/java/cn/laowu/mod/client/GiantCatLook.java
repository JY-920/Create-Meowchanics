package cn.laowu.mod.client;

import org.joml.Quaternionf;

import java.util.List;

/** Client-only target policy and gravity-aligned head math, with no world/AI side effects. */
public final class GiantCatLook {
    private static final double RANGE_SQUARED = 64.0D;
    private static final double FRONT_COSINE = Math.cos(Math.toRadians(70));
    private static final float HOLD_TICKS = 40.0F;
    private static final double SWITCH_DISTANCE_RATIO_SQUARED = .65D * .65D;

    /** Positions are eye offsets from the actual animated head pivot, in world axes. */
    public record Candidate(int id, boolean player, boolean alive, boolean invisible,
                            boolean spectator, boolean excluded,
                            double dx, double dy, double dz) {
        private double distanceSquared() { return dx * dx + dy * dy + dz * dz; }

        private boolean valid(float bodyYaw) {
            if (!alive || invisible || spectator || excluded
                    || !Double.isFinite(distanceSquared()) || distanceSquared() > RANGE_SQUARED)
                return false;
            double horizontal = Math.hypot(dx, dz);
            if (horizontal < 1.0E-6D) return true;
            double yaw = Math.toRadians(bodyYaw);
            return (-dx * Math.sin(yaw) + dz * Math.cos(yaw)) / horizontal >= FRONT_COSINE;
        }
    }

    /** One state belongs to one rendered cat, not the world or the server. */
    public static final class State {
        private int target = Integer.MIN_VALUE;
        private float heldTicks;

        public Candidate select(List<Candidate> candidates, float bodyYaw, float elapsedTicks) {
            heldTicks += Math.max(0.0F, Math.min(2.0F, elapsedTicks));
            Candidate current = null, best = null;
            for (Candidate candidate : candidates) {
                if (!candidate.valid(bodyYaw)) continue;
                if (candidate.id == target) current = candidate;
                if (best == null || candidate.player && !best.player
                        || candidate.player == best.player
                        && (candidate.distanceSquared() < best.distanceSquared()
                        || candidate.distanceSquared() == best.distanceSquared()
                        && candidate.id < best.id)) best = candidate;
            }
            Candidate next = current;
            if (current == null) next = best;
            else if (best != null && best.id != current.id
                    && (best.player && !current.player
                    || best.player == current.player && heldTicks >= HOLD_TICKS
                    && best.distanceSquared()
                    < current.distanceSquared() * SWITCH_DISTANCE_RATIO_SQUARED)) next = best;
            int id = next == null ? Integer.MIN_VALUE : next.id;
            if (id != target) {
                target = id;
                heldTicks = 0.0F;
            }
            return next;
        }
    }

    /** Minecraft relative yaw and pitch, bounded in gravity-aligned world axes. */
    public static float[] boundedAngles(double dx, double dy, double dz, float bodyYaw) {
        double horizontal = Math.hypot(dx, dz);
        float yaw = horizontal < 1.0E-6D ? 0.0F
                : wrapDegrees((float) Math.toDegrees(Math.atan2(-dx, dz)) - bodyYaw);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        return new float[]{Math.max(-25.0F, Math.min(25.0F, yaw)),
                Math.max(-10.0F, Math.min(10.0F, pitch))};
    }

    /**
     * Converts a world-up yaw and world-horizontal pitch into the runtime's
     * additive ZYX head Euler channels. Parent already includes the rendered
     * body heading, Java-model reflection and every animated ancestor.
     * Conjugating the correction prevents a rolled local Y axis from making
     * horizontal observation tip the head vertically.
     */
    public static float[] restEulerDelta(float bodyYaw, float yaw, float pitch,
                                          Quaternionf parentWorld,
                                          float baseX, float baseY, float baseZ) {
        Quaternionf correction = new Quaternionf()
                .rotationY(radians(-bodyYaw - yaw))
                .rotateX(radians(pitch))
                .rotateY(radians(bodyYaw));
        Quaternionf local = new Quaternionf(parentWorld).invert().mul(correction)
                .mul(parentWorld).mul(new Quaternionf().rotationZYX(baseZ, baseY, baseX));
        local.normalize();
        // Explicit ZYX extraction also avoids JOML 1.10.5's X-angle
        // denominator error when the quaternion has a non-zero Y component.
        float x = (float) Math.atan2(2.0F * (local.w * local.x + local.y * local.z),
                1.0F - 2.0F * (local.x * local.x + local.y * local.y));
        float y = (float) Math.asin(Math.max(-1.0F, Math.min(1.0F,
                2.0F * (local.w * local.y - local.z * local.x))));
        float z = (float) Math.atan2(2.0F * (local.w * local.z + local.x * local.y),
                1.0F - 2.0F * (local.y * local.y + local.z * local.z));
        return new float[]{wrapRadians(x - baseX),
                wrapRadians(y - baseY), wrapRadians(z - baseZ)};
    }

    private static float radians(float degrees) { return (float) Math.toRadians(degrees); }
    private static float wrapRadians(float value) { return radians(wrapDegrees((float) Math.toDegrees(value))); }
    private static float wrapDegrees(float value) {
        value %= 360.0F;
        if (value >= 180.0F) value -= 360.0F;
        if (value < -180.0F) value += 360.0F;
        return value;
    }

    private GiantCatLook() {
    }
}
