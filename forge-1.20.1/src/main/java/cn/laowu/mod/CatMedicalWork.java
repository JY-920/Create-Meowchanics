package cn.laowu.mod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.phys.AABB;
import java.util.Map;
import java.util.WeakHashMap;

/** A Create Seat makes the medic a stationary 3x3x3 clinic, including players and ordinary mobs. */
public final class CatMedicalWork {
    private static final Map<Cat, Long> STARTED = new WeakHashMap<>();
    public static AABB area(Cat cat) {
        return AABB.ofSize(cat.getBoundingBox().getCenter(), CatSupportRules.MEDICAL_WORK_SIZE,
                CatSupportRules.MEDICAL_WORK_SIZE, CatSupportRules.MEDICAL_WORK_SIZE);
    }
    public static boolean available(Cat cat) {
        return CatClothesData.getOutfit(cat) == CatOutfitType.MEDICAL && CatSupportRules.canAssist(cat)
                && CareerCatBehavior.findSeat(cat) != null && !cat.isInWaterOrBubble() && !cat.isInLava();
    }
    public static void tick(Cat cat) {
        if (!(cat.level() instanceof ServerLevel level)) return;
        if (!available(cat)) { stop(cat); return; }
        AABB box = area(cat);
        var patients = level.getEntitiesOfClass(LivingEntity.class, box,
                patient -> CatMedicalHealing.injured(patient) && box.contains(patient.getBoundingBox().getCenter())
                        && (patient == cat || cat.hasLineOfSight(patient)));
        if (patients.isEmpty()) { stop(cat); return; }
        long started = STARTED.computeIfAbsent(cat, ignored -> level.getGameTime());
        CatMedicalHealing.holdStill(cat);
        CatMedicalHealing.cast(cat, CatSupportRules.MEDICAL_WORK_SIZE / 2, true);
        for (LivingEntity patient : patients) CatMedicalHealing.treat(cat, patient, started);
    }
    public static void stop(Cat cat) {
        if (STARTED.remove(cat) != null) CatMedicalHealing.stop(cat);
    }
    private CatMedicalWork() {}
}
