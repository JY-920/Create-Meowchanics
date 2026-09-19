package cn.laowu.mod;

import cn.laowu.mod.network.ModNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.WeakHashMap;

/** Server-owned healing: strongest simultaneous source wins, and patients' AI is untouched. */
public final class CatMedicalHealing {
    public static final String NEXT_HEAL = "LaoWuNextMedicalHeal";
    private static final int VISUAL_TTL = 30;
    private static final CatVisualStates<Visual> VISUALS = new CatVisualStates<>();
    private static final Map<LivingEntity, Offer> PENDING = new WeakHashMap<>();
    private static final class Visual {
        long castStart, castUntil, auraUntil, nextSync;
        float radius;
        boolean stationed;
    }
    private record Offer(long tick, float rate) {}
    private static Visual state(LivingEntity entity) { return VISUALS.getOrCreate(entity, Visual::new); }
    private static long now(LivingEntity entity) { return entity.level().getGameTime(); }
    public static boolean casting(Cat cat) {
        Visual v = VISUALS.get(cat);
        return v != null && v.castUntil > now(cat) && cat.isAlive() && !cat.isRemoved()
                && CatClothesData.getOutfit(cat) == CatOutfitType.MEDICAL && !CatPoseData.isPancake(cat)
                && (cat.level().isClientSide || v.stationed || !cat.isOrderedToSit() && !cat.isPassenger());
    }
    public static boolean glowing(LivingEntity entity) {
        Visual v = VISUALS.get(entity);
        return entity.isAlive() && !entity.isInvisible()
                && (!(entity instanceof Cat cat) || !CatPoseData.isPancake(cat))
                && (entity instanceof Cat cat && casting(cat) || v != null && v.auraUntil > now(entity));
    }
    public static float castAge(Cat cat, float partial) {
        Visual v = VISUALS.get(cat);
        return casting(cat) ? Math.max(0, now(cat) - v.castStart + partial) : 0;
    }
    public static float radius(LivingEntity entity) {
        Visual v = VISUALS.get(entity);
        return v == null ? 0 : v.radius;
    }
    public static boolean stationed(LivingEntity entity) {
        Visual v = VISUALS.get(entity);
        return v != null && v.stationed;
    }
    public static void cast(Cat cat, double radius, boolean stationed) {
        if (cat.level().isClientSide) return;
        Visual v = state(cat);
        boolean started = !casting(cat) || v.stationed != stationed;
        if (started) v.castStart = now(cat);
        v.castUntil = now(cat) + VISUAL_TTL;
        v.radius = (float)radius; v.stationed = stationed;
        sync(cat, v, started);
    }
    public static void stop(Cat cat) {
        Visual v = VISUALS.get(cat);
        if (v == null || v.castUntil == 0) return;
        v.castUntil = 0;
        if (!cat.level().isClientSide) sync(cat, v, true);
    }
    /** Only the caster is rooted. Never set sit/order/no-AI flags on any entity. */
    public static void holdStill(Cat cat) {
        cat.getNavigation().stop();
        cat.getMoveControl().setWantedPosition(cat.getX(), cat.getY(), cat.getZ(), 0);
        cat.setSpeed(0); cat.setXxa(0); cat.setZza(0);
        cat.setDeltaMovement(new Vec3(0, Math.min(0, cat.getDeltaMovement().y), 0));
    }
    public static void illuminate(LivingEntity entity) {
        if (entity.level().isClientSide) return;
        Visual v = state(entity);
        boolean started = v.auraUntil <= now(entity);
        v.auraUntil = now(entity) + VISUAL_TTL;
        sync(entity, v, started);
    }
    private static void sync(LivingEntity entity, Visual v, boolean force) {
        long time = now(entity);
        if (!force && time < v.nextSync) return;
        v.nextSync = time + 10;
        ModNetwork.syncMedical(entity, v.castUntil > time, v.auraUntil > time,
                (int)Math.min(1000, Math.max(0, time - v.castStart)), v.radius, v.stationed);
    }
    public static void receive(LivingEntity entity, boolean casting, boolean aura, int age,
                               float radius, boolean stationed) {
        if (!entity.level().isClientSide) return;
        Visual v = state(entity);
        v.castStart = now(entity) - Math.max(0, Math.min(1000, age));
        v.castUntil = casting ? now(entity) + VISUAL_TTL : 0;
        v.auraUntil = aura ? now(entity) + VISUAL_TTL : 0;
        v.radius = Float.isFinite(radius) ? Math.max(0, Math.min(32, radius)) : 0;
        v.stationed = stationed;
    }
    public static boolean injured(LivingEntity patient) {
        return patient.isAlive() && !patient.isRemoved() && !patient.isSpectator()
                && !(patient instanceof net.minecraft.world.entity.decoration.ArmorStand)
                && patient.getHealth() < patient.getMaxHealth() - .001F;
    }
    public static void treat(Cat healer, LivingEntity patient, long channelStart) {
        illuminate(patient);
        if (now(healer) - channelStart >= CatSupportRules.MEDICAL_WINDUP_TICKS) {
            offerHealing(patient, (float)CatSupportRules.healingPerSecond(CatSupportRules.medicalIntelligence(healer)));
            cn.laowu.mod.accessory.CatAccessoryAuras.protect(healer,patient);
        }
    }
    /** Collect for this tick before healing, so iteration order cannot let a weaker medic win. */
    public static void offerHealing(LivingEntity patient, float rate) {
        if (patient.level().isClientSide || !injured(patient) || !Float.isFinite(rate) || rate <= 0) return;
        long tick = now(patient);
        Offer previous = PENDING.get(patient);
        PENDING.put(patient, new Offer(tick, previous != null && previous.tick == tick
                ? Math.max(previous.rate, rate) : rate));
    }
    /** Called at server tick END after all goals and workstation ticks have offered healing. */
    public static void flush(ServerLevel level) {
        long tick = level.getGameTime();
        for (var iterator = PENDING.entrySet().iterator(); iterator.hasNext();) {
            var entry = iterator.next();
            LivingEntity patient = entry.getKey();
            if (patient.level() != level) continue;
            Offer offer = entry.getValue();
            iterator.remove();
            if (offer.tick != tick || !injured(patient)) continue;
            var data = patient.getPersistentData();
            if (data.getLong(NEXT_HEAL) > tick) continue;
            data.putLong(NEXT_HEAL, tick + CatSupportRules.HEAL_TICKS);
            patient.heal(Math.min(patient.getMaxHealth() - patient.getHealth(),
                    offer.rate * CatSupportRules.HEAL_TICKS / 20F));
        }
    }
    private CatMedicalHealing() {}
}
