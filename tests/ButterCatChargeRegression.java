package cn.laowu.mod.entity;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Real shared charge logic and Minecraft geometry; movement steps are simulated without a world. */
public final class ButterCatChargeRegression {
    private static int checks;
    public static void main(String[] args) {
        check(ButterCatCharge.HIT_BONUS_TICKS == 6, "Post-hit tail is 0.3 seconds at 20 TPS");
        Vec3 origin = new Vec3(12.5, 70.25, -8.75);
        int directions = 0;
        for (int x : new int[] {-20, -3, 0, 3, 20})
            for (int y : new int[] {-20, -3, 0, 3, 20})
                for (int z : new int[] {-20, -3, 0, 3, 20}) {
                    Vec3 offset = new Vec3(x, y, z);
                    if (offset.lengthSqr() == 0) continue;
                    directions++;
                    ButterCatCharge charge = new ButterCatCharge();
                    charge.start(origin, origin.add(offset), new Vec3(0, 0, 1));
                    near(charge.direction().length(), 1, "Unit direction");
                    near(charge.direction().dot(offset.normalize()), 1, "Full 3-D aim");
                    check(Math.signum(charge.velocity().y) == Math.signum(y), "Up/down component retained");
                    Result base = run(charge);
                    near(base.movement.length(), 12, "Original unextended distance");
                    check(base.ticks == 12, "Baseline consumes twelve movement steps");
                    near(base.movement.dot(offset.normalize()), 12, "Straight line without gravity drift");

                    charge.start(origin, origin.add(offset), offset);
                    Vec3 firstStep = charge.velocity();
                    charge.advance(firstStep);
                    for (int id = 0; id < 3; id++) check(charge.hit(new UUID(0, id)), "New victim extends");
                    near(charge.remainingDistance(), 18.9, "First hit discards base remainder; three victims yield 18 ticks of distance");
                    check(charge.remainingTicks() == 18, "Three same-step victims yield 0.9 seconds total");
                    for (int repeat = 0; repeat < 20; repeat++)
                        check(!charge.hit(new UUID(0, 1)), "Same victim cannot extend each overlapping tick");
                    check(charge.remainingTicks() == 18, "Duplicate contacts preserve budget");
                    Result extended = run(charge);
                    near(extended.movement.add(firstStep).length(), 19.95, "Tail retains speed without adding the original base distance");
                    check(extended.ticks == 18, "Three unique hits give exactly eighteen post-hit steps");
                }
        check(directions == 124, "All signed axes and diagonals covered");

        ButterCatCharge charge = new ButterCatCharge();
        for (int hitStep = 1; hitStep <= 12; hitStep++) {
            charge.start(Vec3.ZERO, new Vec3(10, 0, 0), Vec3.ZERO);
            for (int step = 0; step < hitStep; step++) charge.advance(charge.velocity());
            check(charge.hit(new UUID(3, hitStep)), "First hit accepted at each point of the base dash");
            check(charge.remainingTicks() == 6, "First hit replaces, not extends, the old timer");
            near(charge.remainingDistance(), 6.3, "First hit replaces the old distance budget");
            Result tail = run(charge);
            check(tail.ticks == 6, "First hit always stops after six further steps");
            near(tail.movement.length(), 6.3, "Six-step post-hit distance");
        }
        charge.start(Vec3.ZERO, new Vec3(10, 0, 0), Vec3.ZERO);
        charge.advance(charge.velocity());
        check(charge.hit(new UUID(4, 0)), "Staggered first hit starts tail");
        for (int step = 0; step < 2; step++) charge.advance(charge.velocity());
        check(!charge.hit(new UUID(4, 0)) && charge.remainingTicks() == 4, "Same victim neither resets nor extends a shrinking timer");
        check(charge.hit(new UUID(4, 1)) && charge.remainingTicks() == 10, "Second victim adds six ticks to the remaining four");
        for (int step = 0; step < 3; step++) charge.advance(charge.velocity());
        check(charge.hit(new UUID(4, 2)) && charge.remainingTicks() == 13, "Third victim adds six to seven; no timer overwrite");
        Result staggered = run(charge);
        check(staggered.ticks + 2 + 3 == 18, "Staggered hits total 0.9 seconds after first contact");
        near(staggered.movement.length(), 13 * 1.05, "Staggered time and distance budgets stay aligned");
        charge.start(Vec3.ZERO, new Vec3(10, 0, 0), Vec3.ZERO);
        Result exhausted = run(charge);
        check(!charge.active(), "Natural expiry");
        check(charge.hit(new UUID(1, 1)) && charge.active(), "A last-step hit extends before finish");
        Result late = run(charge);
        check(late.ticks == 6 && exhausted.ticks == 12, "Last-step hit gets the full 0.3 seconds");
        near(late.movement.length(), 6.3, "Full-speed final-step tail");
        check(charge.hit(new UUID(1, 2)) && charge.active(), "A new victim on the tail's final step can chain");
        check(run(charge).ticks == 6, "Final-step chained victim gets exactly six further steps");
        charge.stop();
        check(!charge.hit(new UUID(1, 3)), "Stopped attack cannot extend");
        check(charge.velocity().equals(Vec3.ZERO), "No stale velocity after stop");
        charge.start(Vec3.ZERO, new Vec3(0, 10, 0), Vec3.ZERO);
        check(charge.hit(new UUID(1, 1)), "Victims become eligible in a new charge");
        charge.stop();
        check(charge.remainingTicks() == 0 && charge.remainingDistance() == 0, "Cancellation clears budgets");

        charge.start(Vec3.ZERO, Vec3.ZERO, new Vec3(0, -8, 0));
        check(charge.direction().equals(new Vec3(0, -1, 0)), "Coincident target uses normalized fallback");
        charge.start(Vec3.ZERO, Vec3.ZERO, Vec3.ZERO);
        check(charge.direction().equals(new Vec3(0, 0, 1)), "Zero fallback is safe");
        charge.start(Vec3.ZERO, new Vec3(Double.NaN, 0, 0), new Vec3(Double.POSITIVE_INFINITY, 0, 0));
        check(charge.direction().equals(new Vec3(0, 0, 1)), "Invalid directions never create NaN motion");
        for (int i = 0; i < ButterCatCharge.BASE_TICKS; i++) charge.advance(Vec3.ZERO);
        check(!charge.active(), "Stalled charge has a bounded timeout");

        AABB body = new AABB(-.45, 0, -.45, .45, 1.05, .45);
        for (Vec3 direction : new Vec3[] {new Vec3(1, 0, 0), new Vec3(0, 1, 0),
                new Vec3(0, -1, 0), new Vec3(-1, 1, -1).normalize()}) {
            Vec3 movement = direction.scale(5);
            Vec3 midpoint = body.getCenter().add(movement.scale(.5));
            AABB thinVictim = new AABB(midpoint, midpoint).inflate(.03);
            check(ButterCatCharge.sweptBox(body, movement).intersects(thinVictim), "Sweep broad phase contains contact");
            check(ButterCatCharge.touches(body, movement, thinVictim), "No tunneling through a thin target");
            check(ButterCatCharge.touches(body.move(movement), movement.scale(-1), thinVictim), "Symmetric reverse sweep");
        }
        AABB diagonalCorner = new AABB(3.5, .1, .1, 3.7, .3, .3);
        Vec3 diagonal = new Vec3(5, 5, 5);
        check(ButterCatCharge.sweptBox(body, diagonal).intersects(diagonalCorner), "Broad-phase false-positive fixture");
        check(!ButterCatCharge.touches(body, diagonal, diagonalCorner), "No false hit in diagonal box corner");
        check(ButterCatCharge.touches(body, Vec3.ZERO, body), "Initial overlap counts");
        check(!ButterCatCharge.touches(body, Vec3.ZERO, body.move(4, 0, 0)), "Distant stationary target not hit");

        check(!ButterCatCharge.blocked(false, true, new Vec3(1, 0, 0)), "Ground does not stop horizontal movement");
        check(ButterCatCharge.blocked(true, false, new Vec3(1, 0, 0)), "Wall stops dash");
        check(ButterCatCharge.blocked(false, true, new Vec3(0, 1, 0)), "Ceiling stops ascent");
        check(ButterCatCharge.blocked(false, true, new Vec3(0, -1, 0)), "Floor stops descent");
        check(!ButterCatCharge.blocked(false, false, new Vec3(1, 1, 1)), "Unobstructed 3-D movement");
        for (float amount : new float[] {0, .5F, 1, 2, 14, 20, 999999, Float.MAX_VALUE}) {
            check(ButterCatCharge.reduceDamage(amount, true, false) == amount * .5F, "Dash halves damage once");
            check(ButterCatCharge.reduceDamage(amount, false, false) == amount, "Idle/windup unchanged");
            check(ButterCatCharge.reduceDamage(amount, true, true) == amount, "Void/kill bypass preserved");
        }
        System.out.println("PASS: " + checks + " butter-cat charge checks: 3-D directions, first-hit 0.3s reset, stacked/last-step hits, per-victim dedup, swept geometry, collisions and 50% reduction");
    }

    private static Result run(ButterCatCharge charge) {
        Vec3 movement = Vec3.ZERO;
        int ticks = 0;
        while (charge.active()) {
            Vec3 step = charge.velocity();
            check(step.length() <= ButterCatCharge.SPEED + 1e-8, "Speed remains bounded");
            movement = movement.add(step);
            charge.advance(step);
            if (++ticks > 1000) throw new AssertionError("Unexpected infinite dash");
        }
        return new Result(movement, ticks);
    }
    private static void near(double a, double b, String message) { check(Math.abs(a - b) < 1e-7, message); }
    private static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    private record Result(Vec3 movement, int ticks) {}
}
