package cn.laowu.mod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cat;
import java.util.*;

/** Smart musicians favour actual recent DPS; with no samples they cover ranged clusters. */
public final class CatMusicSupportGoal extends Goal {
    private final Cat cat;
    private Cat anchor;
    private int searchDelay, pathDelay, retarget;
    public CatMusicSupportGoal(Cat cat) { this.cat = cat; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
    private boolean available() {
        return CatClothesData.getOutfit(cat) == CatOutfitType.MUSIC && CatSupportRules.canWork(cat)
                && !cat.isOrderedToSit() && !cat.isInWaterOrBubble() && !cat.isInLava();
    }
    private static double distance(Cat a, Cat b) { return a.getBoundingBox().getCenter().distanceToSqr(b.getBoundingBox().getCenter()); }
    public static boolean eligible(Cat musician, Cat ally) {
        return ally != musician && CatMusicSupport.recipient(ally) && CatTeamRules.friendly(musician, ally)
                && CareerCatBehavior.canParticipateInCombat(ally)
                && (ally.getTarget() != null && ally.getTarget().isAlive() || CatMusicDps.recent(ally) > 0);
    }
    public static Cat select(Cat musician) {
        if (!(musician.level() instanceof ServerLevel level)) return null;
        double range = Math.max(24, CatMusicSupport.radius(musician) + 8);
        var allies = level.getEntitiesOfClass(Cat.class, musician.getBoundingBox().inflate(range),
                ally -> eligible(musician, ally) && distance(musician, ally) <= range * range);
        return choose(musician, allies, CatMusicSupport.intelligence(musician), CatMusicSupport.radius(musician));
    }
    public static Cat choose(Cat musician, List<Cat> allies, double intelligence, double radius) {
        Cat best = null; double bestScore = -Double.MAX_VALUE; long bestCoverage = -1;
        double maximum = allies.stream().mapToDouble(CatMusicDps::recent).max().orElse(0);
        double smart = Math.max(0, Math.min(1, intelligence / 80));
        for (Cat ally : allies) {
            long ranged = allies.stream().filter(other -> CatClothesData.getOutfit(other).role() == CatCombatRole.RANGED
                    && distance(ally, other) <= radius * radius).count();
            double proximity = 1 / (1 + Math.sqrt(distance(musician, ally)));
            // Blend two normalized scores so small intelligence gains do not instantly erase proximity.
            // At 80+ Intelligence, highest recent DPS wins; ranged coverage breaks exact ties.
            double tactical = maximum > 0 ? CatMusicDps.recent(ally) / maximum
                    : ranged / (double)Math.max(1, allies.size());
            double score = smart * tactical + (1 - smart) * proximity;
            if (score > bestScore || score == bestScore && (ranged > bestCoverage
                    || ranged == bestCoverage && (best == null || distance(musician, ally) < distance(musician, best)))) {
                bestScore = score; bestCoverage = ranged; best = ally;
            }
        }
        return best;
    }
    @Override public boolean canUse() {
        if (!available() || cat.tickCount < searchDelay) return false;
        searchDelay = cat.tickCount + 5; anchor = select(cat); return anchor != null;
    }
    @Override public boolean canContinueToUse() { return available() && anchor != null && eligible(cat, anchor); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() { pathDelay = retarget = 0; }
    @Override public void stop() { CatMusicSupport.stop(cat); cat.getNavigation().stop(); anchor = null; }
    @Override public void tick() {
        if (!(cat.level() instanceof ServerLevel level) || !available()) { CatMusicSupport.stop(cat); return; }
        if (--retarget <= 0) { anchor = select(cat); retarget = 10; }
        if (anchor == null) { CatMusicSupport.stop(cat); return; }
        cat.setTarget(null); cat.getLookControl().setLookAt(anchor, 35, 35);
        double radius = CatMusicSupport.radius(cat);
        boolean aerial = !anchor.onGround() || anchor.isPassenger() && !CatEngineeringCombat.deployed(anchor);
        // Approach the selected cluster closely, but never demand that a ground musician walk into the sky.
        double reach = CatMusicSupport.performing(cat) || aerial ? radius : Math.min(3, radius);
        if (distance(cat, anchor) > reach * reach || !cat.hasLineOfSight(anchor) || !cat.onGround()) {
            CatMusicSupport.stop(cat);
            if (--pathDelay <= 0) {
                pathDelay = 8;
                if (aerial) cat.getNavigation().moveTo(anchor.getX(), cat.getY(), anchor.getZ(), 1.15);
                else cat.getNavigation().moveTo(anchor, 1.15);
            }
            return;
        }
        CatMedicalHealing.holdStill(cat);
        CatMusicSupport.perform(cat);
        double bonus = CatMusicSupport.strength(cat);
        for (Cat ally : level.getEntitiesOfClass(Cat.class, cat.getBoundingBox().inflate(radius),
                other -> other != cat && CatMusicSupport.recipient(other) && CatTeamRules.friendly(cat, other)
                        && distance(cat, other) <= radius * radius && cat.hasLineOfSight(other))) {
            CatMusicSupport.offer(ally, bonus);
            cn.laowu.mod.accessory.CatAccessoryAuras.accelerate(cat,ally);
        }
    }
}
