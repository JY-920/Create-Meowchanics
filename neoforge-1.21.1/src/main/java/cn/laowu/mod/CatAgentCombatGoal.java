package cn.laowu.mod;

import cn.laowu.mod.genetics.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

/** High damage/low survival agent: positional criticals, intelligent flanks and ally-backed smoke retreat. */
public final class CatAgentCombatGoal extends Goal {
    public static final String NEXT_SMOKE = "LaoWuAgentNextSmoke", NEXT_ATTACK = "LaoWuAgentNextAttack";
    public static final int SMART = 60, RETREAT_TICKS = 60, SMOKE_COOLDOWN = 200;
    private final Cat cat;
    private long retreatUntil, nextPath;
    private Vec3 retreat;
    public CatAgentCombatGoal(Cat cat) { this.cat = cat; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
    private boolean available() {
        return cat.isAlive() && cat.isTame() && !cat.isNoAi() && !cat.isPassenger()
                && CatClothesData.getOutfit(cat) == CatOutfitType.AGENT
                && !CatPoseData.isPancake(cat) && !CatProfileData.isBeingViewed(cat)
                && !CareerCatBehavior.isCombatResting(cat);
    }
    private boolean target(LivingEntity target) {
        return target != null && target.isAlive() && !target.isRemoved()
                && CatTeamRules.canHarm(cat, target) && cat.distanceToSqr(target) <= 32 * 32;
    }
    @Override public boolean canUse() { return available() && (target(cat.getTarget()) || readyToRetreat() && threat() != null); }
    private boolean readyToRetreat() {
        // Smoke concealment is available at every Intelligence; only flanking uses SMART.
        return cat.level().getGameTime() >= cat.getPersistentData().getLong(NEXT_SMOKE) && hasNearbyAlly(cat);
    }
    @Override public boolean canContinueToUse() { return available() && (retreating() || target(cat.getTarget())); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void stop() { cat.getNavigation().stop(); retreatUntil = 0; retreat = null; }
    public boolean retreating() { return retreat != null && cat.level().getGameTime() < retreatUntil; }
    public static boolean hasNearbyAlly(Cat cat) {
        return !cat.level().getEntitiesOfClass(Cat.class, cat.getBoundingBox().inflate(12), other ->
                other != cat && other.isAlive() && other.isTame() && CatTeamRules.friendly(cat, other)
                && !CatClothesData.getOutfit(other).isSupport()
                && cat.distanceToSqr(other) <= 144).isEmpty();
    }
    /** Target's rear 120-degree sector; yaw is the body heading, not a temporary head glance. */
    public static boolean behind(Cat attacker, LivingEntity target) {
        Vec3 fromTarget = attacker.position().subtract(target.position()).multiply(1, 0, 1);
        if (fromTarget.lengthSqr() < .04 || Math.abs(attacker.getY() - target.getY()) > 2.5) return false;
        Vec3 facing = Vec3.directionFromRotation(0, target.yBodyRot);
        return fromTarget.normalize().dot(facing) < -.5;
    }
    public static boolean guaranteedCritical(Cat attacker, LivingEntity target, Entity direct) {
        return CatClothesData.getOutfit(attacker) == CatOutfitType.AGENT && direct == attacker
                && attacker.distanceToSqr(target) <= 16 && behind(attacker, target);
    }
    public static Vec3 rearPosition(LivingEntity target) {
        return target.position().subtract(Vec3.directionFromRotation(0, target.yBodyRot)
                .scale(1.25 + target.getBbWidth() * .5));
    }
    private Vec3 flankPosition(LivingEntity target) {
        Vec3 facing=Vec3.directionFromRotation(0,target.yBodyRot);
        Vec3 side=new Vec3(-facing.z,0,facing.x);
        Vec3 offset=cat.position().subtract(target.position()).multiply(1,0,1);
        double lateral=offset.dot(side), clearance=1.65+target.getBbWidth();
        // First go around the shoulder; a path straight to the rear would run through the enemy.
        if(offset.dot(facing)>-.25 && Math.abs(lateral)<clearance*.8) {
            double sign=Math.abs(lateral)>.15?Math.signum(lateral):(cat.getId()%2==0?1:-1);
            return target.position().add(side.scale(sign*clearance)).add(facing.scale(.15));
        }
        return rearPosition(target);
    }
    private Mob threat() {
        return cat.level().getEntitiesOfClass(Mob.class, cat.getBoundingBox().inflate(12),
                mob -> mob != cat && mob.isAlive() && mob.getTarget() == cat && cat.distanceToSqr(mob) <= 144)
                .stream().min(java.util.Comparator.comparingDouble(cat::distanceToSqr)).orElse(null);
    }
    private void retreat(Mob threat, long now) {
        net.minecraft.world.level.pathfinder.Path path = null;
        Vec3 point = cat.position();
        for (int attempt = 0; attempt < 10; attempt++) {
            Vec3 candidate = DefaultRandomPos.getPosAway(cat, 7, 3, threat.position());
            if (candidate == null || !cat.level().hasChunkAt(BlockPos.containing(candidate))) continue;
            var candidatePath = cat.getNavigation().createPath(BlockPos.containing(candidate), 0);
            if (candidatePath != null && candidatePath.canReach() && candidate.distanceToSqr(threat.position()) > cat.distanceToSqr(threat)) {
                point = candidate; path = candidatePath; break;
            }
        }
        // Even in a cramped room, smoke breaks aggro; a failed first random path must not cancel it.
        retreat = point; retreatUntil = now + RETREAT_TICKS; nextPath = now + 8;
        cat.getPersistentData().putLong(NEXT_SMOKE, now + SMOKE_COOLDOWN);
        if (path != null) cat.getNavigation().moveTo(path, 1.55);
        else cat.getNavigation().stop();
        CatAgentSmoke.burst(cat, cat.position());
    }
    @Override public void tick() {
        long now = cat.level().getGameTime();
        if (!retreating() && readyToRetreat()) {
            Mob threat = threat(); if (threat != null) retreat(threat, now);
        }
        if (retreating()) {
            if (now >= nextPath) { nextPath = now + 8; cat.getNavigation().moveTo(retreat.x, retreat.y, retreat.z, 1.55); }
            return;
        }
        LivingEntity target = cat.getTarget(); if (!target(target)) return;
        cat.getLookControl().setLookAt(target, 30, 30);
        if (now >= nextPath) {
            int intelligence = CatAttributeEffects.effectiveValue(cat, CatStat.INTELLIGENCE);
            nextPath = now + (intelligence >= SMART ? 5 : 12);
            Vec3 desired = intelligence >= SMART && !behind(cat, target) ? flankPosition(target) : target.position();
            var path = cat.getNavigation().createPath(BlockPos.containing(desired), 0);
            if (path != null && path.canReach()) cat.getNavigation().moveTo(path, 1.3);
            else cat.getNavigation().moveTo(target, 1.3);
        }
        double reach = Math.max(1.3, cat.getBbWidth() * 2 + target.getBbWidth() * .5);
        if (cat.distanceToSqr(target) <= reach * reach && cat.hasLineOfSight(target)
                && now >= cat.getPersistentData().getLong(NEXT_ATTACK)) {
            cat.getPersistentData().putLong(NEXT_ATTACK, now + CareerCatBehavior.careerAttackIntervalTicks(cat));
            CatAgentMeleeMotion.begin(cat);
            cat.swing(InteractionHand.MAIN_HAND); cat.doHurtTarget(target);
        }
    }
}
