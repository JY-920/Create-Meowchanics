package cn.laowu.mod.entity;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Server-owned, non-persistent charge budget and swept-contact math. */
final class ButterCatCharge {
    static final double BASE_DISTANCE = 12.0D;
    static final double SPEED = 1.05D;
    static final int BASE_TICKS = 14;
    static final int HIT_BONUS_TICKS = 6;
    static final double HORIZONTAL_REACH = 0.45D;
    static final double VERTICAL_REACH = 0.25D;
    private final Set<UUID> hitVictims = new HashSet<>();
    private Vec3 direction = Vec3.ZERO;
    private double remainingDistance;
    private long remainingTicks;
    private boolean started;

    void start(Vec3 origin, Vec3 target, Vec3 fallback) {
        stop();
        direction = directionTo(origin, target, fallback);
        remainingDistance = BASE_DISTANCE;
        remainingTicks = BASE_TICKS;
        started = true;
    }

    Vec3 direction() { return direction; }
    long remainingTicks() { return remainingTicks; }
    double remainingDistance() { return remainingDistance; }
    boolean active() { return started && remainingTicks > 0 && remainingDistance > 1.0E-6D; }
    boolean hasHit(UUID victim) { return hitVictims.contains(victim); }

    Vec3 velocity() {
        return active() ? direction.scale(Math.min(SPEED, remainingDistance)) : Vec3.ZERO;
    }

    void advance(Vec3 actualMovement) {
        if (!started) return;
        remainingTicks = Math.max(0L, remainingTicks - 1L);
        remainingDistance = Math.max(0.0D, remainingDistance - actualMovement.length());
    }

    /** First hit starts a 0.3s tail; each later unique hit adds another 0.3s. */
    boolean hit(UUID victim) {
        if (!started || !hitVictims.add(victim)) return false;
        if (hitVictims.size() == 1) {
            // Discard the unspent base charge, otherwise an early hit still runs too long.
            remainingTicks = HIT_BONUS_TICKS;
            remainingDistance = SPEED * HIT_BONUS_TICKS;
        } else {
            remainingTicks += HIT_BONUS_TICKS;
            remainingDistance += SPEED * HIT_BONUS_TICKS;
        }
        return true;
    }

    void stop() {
        started = false;
        remainingDistance = 0.0D;
        remainingTicks = 0L;
        direction = Vec3.ZERO;
        hitVictims.clear();
    }

    static Vec3 directionTo(Vec3 origin, Vec3 target, Vec3 fallback) {
        Vec3 difference = target.subtract(origin);
        if (usable(difference)) return difference.normalize();
        if (usable(fallback)) return fallback.normalize();
        return new Vec3(0.0D, 0.0D, 1.0D);
    }

    private static boolean usable(Vec3 vector) {
        double length = vector.lengthSqr();
        return Double.isFinite(length) && length > 1.0E-8D;
    }

    static AABB sweptBox(AABB start, Vec3 movement) {
        return start.expandTowards(movement).inflate(HORIZONTAL_REACH, VERTICAL_REACH, HORIZONTAL_REACH);
    }

    /** Minkowski-expanded target + segment test avoids diagonal broad-phase false hits. */
    static boolean touches(AABB body, Vec3 movement, AABB target) {
        AABB expanded = target.inflate(body.getXsize() * 0.5D + HORIZONTAL_REACH,
                body.getYsize() * 0.5D + VERTICAL_REACH,
                body.getZsize() * 0.5D + HORIZONTAL_REACH);
        Vec3 start = body.getCenter(), end = start.add(movement);
        return expanded.contains(start) || expanded.contains(end) || expanded.clip(start, end).isPresent();
    }

    static boolean blocked(boolean horizontalCollision, boolean verticalCollision, Vec3 requested) {
        // Ground contact alone must not cancel an ordinary horizontal charge.
        return horizontalCollision || (verticalCollision && Math.abs(requested.y) > 1.0E-6D);
    }

    static float reduceDamage(float amount, boolean dashing, boolean bypassesInvulnerability) {
        return dashing && !bypassesInvulnerability ? amount * 0.5F : amount;
    }
}
