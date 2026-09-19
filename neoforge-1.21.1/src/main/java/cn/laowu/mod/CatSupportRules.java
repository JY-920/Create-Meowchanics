package cn.laowu.mod;

import cn.laowu.mod.genetics.CatAttributeEffects;
import cn.laowu.mod.genetics.CatStat;
import net.minecraft.world.entity.animal.Cat;

/** Shared support pacing, eligibility and intelligence triage. */
public final class CatSupportRules {
    public static final double MOVEMENT_BONUS = 1.20D;
    public static final int LOGISTICS_CAST_TICKS = 20;
    public static final int LOGISTICS_TARGET_TICKS = 100;
    public static final double MEDICAL_SEARCH_RANGE = 16;
    public static final double MEDICAL_APPROACH_RANGE = 2.5;
    public static final int MEDICAL_WINDUP_TICKS = 20;
    public static final int HEAL_TICKS = 5;
    public static final double MEDICAL_WORK_SIZE = 3;

    public static double medicalIntelligence(Cat cat) {
        return ServerConfig.scale(CatStat.INTELLIGENCE,
                CatAttributeEffects.effectiveValue(cat, CatStat.INTELLIGENCE));
    }
    /** Four small, synchronized pulses per second; 100 intelligence = 4 HP/second. */
    public static double healingPerSecond(double intelligence) {
        return 1 + .03 * finiteIntelligence(intelligence);
    }
    /** A 3D radius, not a ground-only circle. Bound scans under extreme admin multipliers. */
    public static double medicalRadius(double intelligence) {
        return Math.min(32, 3 + .03 * finiteIntelligence(intelligence));
    }
    public static double medicalRadius(Cat cat) { return medicalRadius(medicalIntelligence(cat)); }
    public static double medicalSearchRange(Cat cat) { return Math.max(MEDICAL_SEARCH_RANGE, medicalRadius(cat)); }
    private static double finiteIntelligence(double intelligence) {
        return Double.isFinite(intelligence) ? Math.max(0, intelligence) : 0;
    }
    public static boolean canWork(Cat cat) {
        return canAssist(cat) && !cat.isPassenger() && !CareerCatBehavior.isCombatResting(cat);
    }
    /** Stationed medics intentionally retain their sit command and Create Seat passenger state. */
    public static boolean canAssist(Cat cat) {
        return cat.isAlive() && cat.isTame() && !cat.isNoAi() && cat.getOwnerUUID() != null
                && !CatProfileData.isBeingViewed(cat) && !CatPoseData.isPancake(cat)
                && !CatPoseData.isHissing(cat) && !CatLogisticsBehavior.isActive(cat)
                && !CatLaserCommands.hasOrder(cat);
    }
    public static double triageScore(int intelligence, double distanceSquared,
                                     double rangeSquared, double healthFraction) {
        double urgency = Math.max(0, Math.min(100, intelligence)) / 100.0;
        return (1 - urgency) * distanceSquared / rangeSquared + urgency * healthFraction;
    }
    private CatSupportRules() {}
}
