package cn.laowu.mod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Short-lived per-cat orders; no forced chunk loading and no commands to other owners' pets. */
public final class CatLaserCommands {
    private static final String UNTIL = "LaoWuLaserUntil", TARGET = "LaoWuLaserTarget";
    private static final String X = "LaoWuLaserX", Y = "LaoWuLaserY", Z = "LaoWuLaserZ";
    private static final Set<Cat> INSTALLED = Collections.newSetFromMap(new WeakHashMap<>());
    public static int issue(ServerPlayer player, CatLaserTargeting.Aim aim) {
        if (aim.target() != null && !CatLaserTargeting.allowed(player, aim.target())) return 0;
        int count = 0;
        for (Cat cat : player.level().getEntitiesOfClass(Cat.class, player.getBoundingBox().inflate(32),
                c -> c.isAlive() && c.isTame() && player.getUUID().equals(c.getOwnerUUID())
                        && !c.isPassenger() && !CatPoseData.isPancake(c)
                        && !CatProfileData.isBeingViewed(c) && c.distanceToSqr(player) <= 1024)) {
            if (aim.target() != null && CatClothesData.getOutfit(cat) == CatOutfitType.TRANSPORT) continue;
            if (aim.target() != null && !CatTeamRules.canHarm(cat, aim.target())) continue;
            install(cat);
            if (CatPoseData.isHissing(cat) || HissingCatBehavior.isFighting(cat))
                HissingCatBehavior.interruptPair(cat, 600);
            cat.setOrderedToSit(false);
            cat.setInSittingPose(false);
            var data = cat.getPersistentData();
            data.putLong(UNTIL, cat.level().getGameTime() + 600);
            data.putDouble(X, aim.point().x); data.putDouble(Y, aim.point().y); data.putDouble(Z, aim.point().z);
            data.remove(TARGET);
            if (aim.target() != null) {
                data.putUUID(TARGET, aim.target().getUUID());
                cat.setTarget(aim.target());
            } else cat.setTarget(null);
            cat.getNavigation().stop();
            CatCommandFlight.update(cat, true, aim.target() != null);
            count++;
        }
        return count;
    }
    private static boolean valid(Cat cat) {
        var owner = cat.getOwner();
        return owner != null && owner.isAlive() && cat.isAlive() && !cat.isOrderedToSit()
                && !cat.isPassenger() && !CatPoseData.isPancake(cat)
                && !CatProfileData.isBeingViewed(cat)
                && cat.distanceToSqr(owner) <= CareerCatBehavior.MAX_OWNER_DISTANCE_SQR
                && cat.getPersistentData().getLong(UNTIL) > cat.level().getGameTime();
    }
    private static LivingEntity target(Cat cat) {
        if (!(cat.level() instanceof ServerLevel level) || !cat.getPersistentData().hasUUID(TARGET)) return null;
        var entity = level.getEntity(cat.getPersistentData().getUUID(TARGET));
        return entity instanceof LivingEntity living && cat.getOwner() instanceof net.minecraft.world.entity.player.Player owner
                && CatLaserTargeting.allowed(owner, living) && CatTeamRules.canHarm(cat, living)
                && living.distanceToSqr(owner) <= CareerCatBehavior.MAX_OWNER_DISTANCE_SQR ? living : null;
    }
    public static boolean hasOrder(Cat cat) { return valid(cat); }
    public static boolean isAttackOrderTarget(Cat cat, LivingEntity proposed) {
        return proposed != null && valid(cat) && target(cat) == proposed;
    }
    private static void clear(Cat cat) {
        if (cat.getPersistentData().hasUUID(TARGET)) cat.setTarget(null);
        for (String key : List.of(UNTIL, TARGET, X, Y, Z)) cat.getPersistentData().remove(key);
    }
    public static void tick(Cat cat) {
        if (cat.getPersistentData().contains(UNTIL)) {
            install(cat);
            if (!valid(cat) || (cat.getPersistentData().hasUUID(TARGET) && target(cat) == null)) clear(cat);
        }
        // Also restore controllers after arrival, expiry, sitting or trait removal.
        CatCommandFlight.update(cat, cat.getPersistentData().contains(UNTIL) && valid(cat),
                cat.getPersistentData().hasUUID(TARGET));
    }
    private static void install(Cat cat) {
        if (!INSTALLED.add(cat)) return;
        cat.targetSelector.addGoal(0, new Goal() {
            { setFlags(EnumSet.of(Flag.TARGET)); }
            @Override public boolean canUse() { return valid(cat) && target(cat) != null; }
            @Override public boolean canContinueToUse() { return canUse(); }
            @Override public void start() { cat.setTarget(target(cat)); }
            @Override public void tick() { cat.setTarget(target(cat)); }
            @Override public void stop() { cat.setTarget(null); }
        });
        cat.goalSelector.addGoal(0, new Goal() {
            private int attackTicks;
            { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
            @Override public boolean canUse() {
                return valid(cat) && (!cat.getPersistentData().hasUUID(TARGET)
                        || CatClothesData.getOutfit(cat) == CatOutfitType.NONE && target(cat) != null);
            }
            @Override public boolean canContinueToUse() { return canUse(); }
            @Override public boolean requiresUpdateEveryTick() { return true; }
            @Override public void tick() {
                var t = target(cat);
                var data = cat.getPersistentData();
                Vec3 point = t == null ? new Vec3(data.getDouble(X), data.getDouble(Y), data.getDouble(Z)) : t.position();
                cat.getLookControl().setLookAt(point.x, point.y + (t == null ? 0 : t.getBbHeight()/2), point.z);
                if (cat.tickCount % 10 == 0) cat.getNavigation().moveTo(point.x, point.y, point.z, 1.2);
                if (t == null && cat.position().distanceToSqr(point) < 1.5) {
                    clear(cat);
                    cat.getNavigation().stop();
                    // Arrival completes only the move order; normal standing/follow/combat AI resumes.
                }
                if (attackTicks > 0) attackTicks--;
                if (t != null && attackTicks == 0 && cat.distanceToSqr(t) < 2.5 && cat.hasLineOfSight(t)) {
                    cat.doHurtTarget(t); // Unsuited cats keep their vanilla biological attack.
                    attackTicks = 20;
                }
            }
            @Override public void stop() { cat.getNavigation().stop(); }
        });
    }
    private CatLaserCommands() {}
}
