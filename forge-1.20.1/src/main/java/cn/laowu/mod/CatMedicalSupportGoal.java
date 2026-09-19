package cn.laowu.mod;

import cn.laowu.mod.genetics.CatAttributeEffects;
import cn.laowu.mod.genetics.CatStat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cat;
import java.util.EnumSet;

/** Approach an injured career ally (or self), then channel a stationary 3D group-healing sphere. */
public final class CatMedicalSupportGoal extends Goal {
    private final Cat cat;
    private Cat recipient;
    private int nextSearch, nextPath, retarget;
    private long channelStart = -1;

    public CatMedicalSupportGoal(Cat cat) {
        this.cat = cat;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    private boolean available() {
        return CatClothesData.getOutfit(cat) == CatOutfitType.MEDICAL && CatSupportRules.canWork(cat)
                && !cat.isOrderedToSit() && !cat.isInWaterOrBubble() && !cat.isInLava();
    }
    public static double distanceSquared(Cat healer, Cat ally) {
        return healer.getBoundingBox().getCenter().distanceToSqr(ally.getBoundingBox().getCenter());
    }
    public static boolean recipient(Cat healer, Cat ally) {
        CatOutfitType outfit = CatClothesData.getOutfit(ally);
        double range = CatSupportRules.medicalSearchRange(healer);
        return CatMedicalHealing.injured(ally) && ally.level() == healer.level()
                && !CatPoseData.isPancake(ally) && (ally == healer || CatTeamRules.friendly(healer, ally))
                && outfit != CatOutfitType.NONE && !outfit.isPreviewOnly()
                && distanceSquared(healer, ally) <= range * range;
    }
    public static Cat selectRecipient(Cat healer) {
        if (!(healer.level() instanceof ServerLevel level)) return null;
        double range = CatSupportRules.medicalSearchRange(healer);
        int intelligence = CatAttributeEffects.effectiveValue(healer, CatStat.INTELLIGENCE);
        Cat best = null;
        double bestScore = Double.MAX_VALUE;
        for (Cat ally : level.getEntitiesOfClass(Cat.class, healer.getBoundingBox().inflate(range),
                candidate -> recipient(healer, candidate))) {
            double score = CatSupportRules.triageScore(intelligence, distanceSquared(healer, ally),
                    range * range, ally.getHealth() / ally.getMaxHealth());
            if (score < bestScore) { bestScore = score; best = ally; }
        }
        return best;
    }
    @Override public boolean canUse() {
        if (!available() || cat.tickCount < nextSearch) return false;
        nextSearch = cat.tickCount + 5;
        recipient = selectRecipient(cat);
        return recipient != null;
    }
    @Override public boolean canContinueToUse() {
        if (!available()) return false;
        if (recipient == null || !recipient(cat, recipient)) recipient = selectRecipient(cat);
        return recipient != null;
    }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() { nextPath = retarget = 0; channelStart = -1; }
    private void stopChannel() { channelStart = -1; CatMedicalHealing.stop(cat); }
    @Override public void stop() { stopChannel(); recipient = null; cat.getNavigation().stop(); }
    @Override public void tick() {
        if (!(cat.level() instanceof ServerLevel level) || !available()) { stopChannel(); return; }
        if (--retarget <= 0) { retarget = 10; recipient = selectRecipient(cat); }
        if (recipient == null || !recipient(cat, recipient)) { stopChannel(); return; }
        cat.setTarget(null);
        if (recipient != cat) cat.getLookControl().setLookAt(recipient, 35, 35);
        double radius = CatSupportRules.medicalRadius(cat);
        boolean aerial = !recipient.onGround() || recipient.isPassenger()
                || Math.abs(recipient.getY() - cat.getY()) > 1.25;
        // A grounded medic cannot walk into the sky. For an airborne target, begin anywhere
        // inside the actual healing sphere instead of demanding the old 2.5-block approach.
        double reach = channelStart < 0 && !aerial && recipient != cat
                ? Math.min(CatSupportRules.MEDICAL_APPROACH_RANGE, radius) : radius;
        if (distanceSquared(cat, recipient) > reach * reach
                || recipient != cat && !cat.hasLineOfSight(recipient) || !cat.onGround()) {
            stopChannel();
            if (--nextPath <= 0 && recipient != cat) {
                nextPath = 8;
                if (aerial) cat.getNavigation().moveTo(recipient.getX(), cat.getY(), recipient.getZ(), 1.15);
                else cat.getNavigation().moveTo(recipient, 1.15);
            }
            return;
        }
        CatMedicalHealing.holdStill(cat);
        if (channelStart < 0) channelStart = level.getGameTime();
        CatMedicalHealing.cast(cat, radius, false);
        for (Cat ally : level.getEntitiesOfClass(Cat.class, cat.getBoundingBox().inflate(radius),
                candidate -> recipient(cat, candidate) && distanceSquared(cat, candidate) <= radius * radius
                        && (candidate == cat || cat.hasLineOfSight(candidate)))) {
            CatMedicalHealing.treat(cat, ally, channelStart);
        }
    }
}
