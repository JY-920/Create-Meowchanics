package cn.laowu.mod.genetics;

import cn.laowu.mod.CatClothesData;
import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.CatProfileData;
import cn.laowu.mod.CatPoseData;
import cn.laowu.mod.HissingCatBehavior;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/** Event-driven trait effects that are not ordinary attribute conversions. */
public final class CatTraitEffects {
    private static final long NIGHT_START = 13_000L;
    private static final long NIGHT_END = 23_000L;
    private static final String RAGE_ACTIVE_UNTIL_TAG = "LaoWuTraitRageActiveUntil";
    private static final String RAGE_COOLDOWN_UNTIL_TAG = "LaoWuTraitRageCooldownUntil";
    private static final String RAGE_CLIENT_ACTIVE_TAG = "LaoWuTraitRageClientActive";
    private static final String LU_BU_OUTNUMBERED_TAG = "LaoWuTraitLuBuOutnumbered";
    private static final String TIMID_OUTNUMBERED_TAG = "LaoWuTraitTimidOutnumbered";
    private static final String COMBAT_ACTIVE_TAG = "LaoWuTraitCombatActive";
    private static final String NINE_LIVES_COOLDOWN_UNTIL_TAG =
            "LaoWuTraitNineLivesCooldownUntil";
    private static final String ENERGY_RECOVERY_COOLDOWN_UNTIL_TAG =
            "LaoWuTraitEnergyRecoveryCooldownUntil";
    private static final int RAGE_DURATION_TICKS = 20 * 8;
    private static final int RAGE_COOLDOWN_TICKS = 20 * 16;
    private static final int HEAL_INTERVAL_TICKS = 20 * 4;
    private static final int NINE_LIVES_COOLDOWN_TICKS = 20 * 180;
    private static final double LU_BU_SCAN_RANGE = 8.0D;

    /** Called from the existing per-cat tick; no world scan or NBT reparse is involved. */
    public static void tick(Cat cat) {
        if (cat.level().isClientSide) return;
        CatTraitProfile traits = CatTraitData.ensure(cat);
        // Reset only near the end of the growth timer. This keeps LOLI cats
        // permanently young without dirtying synchronized age data every tick.
        if (traits.has(CatTrait.LOLI) && cat.getAge() > -23_000) {
            cat.setAge(-24_000);
        }
        updateCombatState(cat, traits);
        if (traits.has(CatTrait.HEAT_RESISTANCE) && cat.isOnFire()) {
            cat.setRemainingFireTicks(0);
        }

        long now = cat.level().getGameTime();
        if (cat.getPersistentData().contains(RAGE_ACTIVE_UNTIL_TAG)
                && now >= cat.getPersistentData().getLong(RAGE_ACTIVE_UNTIL_TAG)) {
            cat.getPersistentData().remove(RAGE_ACTIVE_UNTIL_TAG);
            CatAttributeEffects.refresh(cat);
            ModNetwork.syncCatTraitStateToTracking(cat);
        }

        int healingLevel = traits.level(CatTrait.HEALING_PURR);
        if (healingLevel > 0 && !CatPoseData.isPancake(cat)
                && cat.tickCount % HEAL_INTERVAL_TICKS == 0
                && cat.getHealth() < cat.getMaxHealth()) {
            cat.heal(CatTrait.HEALING_PURR.healingPurrAmount(healingLevel));
        }

        if (cat.tickCount % 20 == 0) {
            updateHostileConditions(cat, traits);
            tryEnergyRecovery(cat, traits, now);
        }
    }

    /**
     * Shared client/server definition of night. An explicit day-time interval
     * avoids UI and server modifiers disagreeing around the dusk/dawn fade.
     */
    public static boolean isNight(Level level) {
        if (level == null || !level.dimensionType().hasSkyLight()) return false;
        long dayTime = Math.floorMod(level.getDayTime(), 24_000L);
        return dayTime >= NIGHT_START && dayTime < NIGHT_END;
    }

    public static boolean isDay(Level level) {
        return level != null && level.dimensionType().hasSkyLight() && !isNight(level);
    }

    public static boolean isHeatResistant(Cat cat) {
        return CatTraitData.ensure(cat).has(CatTrait.HEAT_RESISTANCE);
    }

    public static boolean isBristlingRageActive(Cat cat) {
        if (cat.level().isClientSide) {
            return cat.getPersistentData().getBoolean(RAGE_CLIENT_ACTIVE_TAG);
        }
        return cat.getPersistentData().getLong(RAGE_ACTIVE_UNTIL_TAG)
                > cat.level().getGameTime();
    }

    public static boolean isLuBuOutnumbered(Cat cat) {
        return cat.getPersistentData().getBoolean(LU_BU_OUTNUMBERED_TAG);
    }

    public static boolean isTimidOutnumbered(Cat cat) {
        return cat.getPersistentData().getBoolean(TIMID_OUTNUMBERED_TAG);
    }

    /** Synced combat flag used by the Round Head appearance renderer. */
    public static boolean isCombatActive(Cat cat) {
        return cat.getPersistentData().getBoolean(COMBAT_ACTIVE_TAG);
    }

    /** Starts the event-driven retaliation buff only after accepted final damage. */
    public static void onAcceptedDamage(Cat cat) {
        if (cat.level().isClientSide || CatPoseData.isPancake(cat)) return;
        int level = CatTraitData.ensure(cat).level(CatTrait.BRISTLING_RAGE);
        if (level <= 0) return;
        long now = cat.level().getGameTime();
        if (cat.getPersistentData().getLong(RAGE_COOLDOWN_UNTIL_TAG) > now) return;

        cat.getPersistentData().putLong(RAGE_ACTIVE_UNTIL_TAG,
                now + RAGE_DURATION_TICKS);
        cat.getPersistentData().putLong(RAGE_COOLDOWN_UNTIL_TAG,
                now + RAGE_COOLDOWN_TICKS);
        CatAttributeEffects.refresh(cat);
        ModNetwork.syncCatTraitStateToTracking(cat);
    }

    /** Applies a transition-only packet; no transient timer is written client-side. */
    public static void setClientState(Cat cat, boolean rageActive,
                                      boolean luBuOutnumbered,
                                      boolean timidOutnumbered,
                                      boolean combatActive) {
        if (rageActive) cat.getPersistentData().putBoolean(RAGE_CLIENT_ACTIVE_TAG, true);
        else cat.getPersistentData().remove(RAGE_CLIENT_ACTIVE_TAG);
        if (luBuOutnumbered) {
            cat.getPersistentData().putBoolean(LU_BU_OUTNUMBERED_TAG, true);
        } else {
            cat.getPersistentData().remove(LU_BU_OUTNUMBERED_TAG);
        }
        if (timidOutnumbered) {
            cat.getPersistentData().putBoolean(TIMID_OUTNUMBERED_TAG, true);
        } else {
            cat.getPersistentData().remove(TIMID_OUTNUMBERED_TAG);
        }
        if (combatActive) {
            cat.getPersistentData().putBoolean(COMBAT_ACTIVE_TAG, true);
        } else {
            cat.getPersistentData().remove(COMBAT_ACTIVE_TAG);
        }
    }

    private static void updateCombatState(Cat cat, CatTraitProfile traits) {
        boolean recentRetaliation = cat.getLastHurtByMob() != null
                && cat.tickCount - cat.getLastHurtByMobTimestamp() <= 100;
        boolean active = traits.has(CatTrait.ROUND_HEAD)
                && ((cat.getTarget() != null && cat.getTarget().isAlive())
                || HissingCatBehavior.isFighting(cat) || recentRetaliation);
        if (isCombatActive(cat) == active) return;
        if (active) cat.getPersistentData().putBoolean(COMBAT_ACTIVE_TAG, true);
        else cat.getPersistentData().remove(COMBAT_ACTIVE_TAG);
        ModNetwork.syncCatTraitStateToTracking(cat);
    }

    private static void updateHostileConditions(Cat cat, CatTraitProfile traits) {
        boolean activeBody = !CatPoseData.isPancake(cat);
        boolean checksLuBu = activeBody && traits.has(CatTrait.LU_BU_REBORN);
        boolean checksTimid = activeBody && traits.has(CatTrait.TIMID);
        int enemies = checksLuBu || checksTimid
                ? cat.level().getEntitiesOfClass(LivingEntity.class,
                cat.getBoundingBox().inflate(LU_BU_SCAN_RANGE),
                entity -> entity != cat && entity.isAlive() && entity instanceof Enemy)
                .size()
                : 0;
        boolean luBuActive = checksLuBu && enemies >= 3;
        boolean timidActive = checksTimid && enemies >= 2;
        if (isLuBuOutnumbered(cat) == luBuActive
                && isTimidOutnumbered(cat) == timidActive) return;
        if (luBuActive) cat.getPersistentData().putBoolean(LU_BU_OUTNUMBERED_TAG, true);
        else cat.getPersistentData().remove(LU_BU_OUTNUMBERED_TAG);
        if (timidActive) cat.getPersistentData().putBoolean(TIMID_OUTNUMBERED_TAG, true);
        else cat.getPersistentData().remove(TIMID_OUTNUMBERED_TAG);
        CatAttributeEffects.refresh(cat);
        ModNetwork.syncCatTraitStateToTracking(cat);
    }

    private static void tryEnergyRecovery(Cat cat, CatTraitProfile traits, long now) {
        if (!traits.has(CatTrait.ENERGY_RECOVERY) || traits.has(CatTrait.ANOREXIA)
                || CatPoseData.isPancake(cat)
                || cat.getHealth() > cat.getMaxHealth() * 0.5F
                || cat.getPersistentData().getLong(ENERGY_RECOVERY_COOLDOWN_UNTIL_TAG)
                > now) return;

        var inventory = CatProfileData.openContainer(cat);
        for (int slot = CatProfileData.ACCESSORY_SLOTS;
             slot < CatProfileData.SLOT_COUNT; slot++) {
            if (!inventory.getItem(slot).is(LaoWuMod.CAT_PANCAKE.get())) continue;
            if (inventory.removeItem(slot, 1).isEmpty()) return;
            inventory.setChanged();
            cat.heal(cat.getMaxHealth()
                    * CatTrait.ENERGY_RECOVERY.energyRecoveryPercent() / 100.0F);
            cat.getPersistentData().putLong(ENERGY_RECOVERY_COOLDOWN_UNTIL_TAG,
                    now + CatTrait.ENERGY_RECOVERY
                            .energyRecoveryCooldownSeconds() * 20L);
            cat.playSound(SoundEvents.GENERIC_EAT, 0.9F, 1.1F);
            cat.gameEvent(GameEvent.EAT);
            return;
        }
    }

    /** Luck contributes one point per 25 effective points, with a hard 15% cap. */
    public static boolean tryCainAvoid(Cat cat, DamageSource source) {
        if (cat.level().isClientSide || CatPoseData.isPancake(cat)
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        int level = CatTraitData.ensure(cat).level(CatTrait.CAIN_MARK);
        if (level <= 0) return false;
        int chance = Math.min(15, CatTrait.CAIN_MARK.cainBaseAvoidChance(level)
                + CatAttributeEffects.effectiveValue(cat, CatStat.LUCK) / 25);
        if (cat.getRandom().nextInt(100) >= chance) return false;
        cat.playSound(SoundEvents.SHIELD_BLOCK, 0.8F, 1.25F);
        return true;
    }

    /** Reflects accepted damage once; vanilla's thorns damage type prevents loops. */
    public static void tryReflectDamage(Cat cat, DamageSource source) {
        if (cat.level().isClientSide || source.is(DamageTypes.THORNS)) return;
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living
                ? living : null;
        if (attacker == null || attacker == cat || !attacker.isAlive()) return;

        CatTraitProfile traits = CatTraitData.ensure(cat);
        int level = traits.level(CatTrait.THORNS);
        if (level <= 0 || cat.getRandom().nextInt(100)
                >= CatTrait.THORNS.thornsChance(level)) return;

        CatOutfitType outfit = CatClothesData.getOutfit(cat);
        if (outfit == CatOutfitType.NONE || outfit == CatOutfitType.TRANSPORT) return;
        float damage = (float) (cat.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * CatTrait.THORNS.thornsDamagePercent(level) / 100.0D);
        if (damage <= 0.0F) return;
        if (attacker.hurt(cat.damageSources().thorns(cat), damage)) {
            cat.playSound(SoundEvents.THORNS_HIT, 0.8F, 1.0F);
        }
    }

    /**
     * Cancels one otherwise-lethal final hit. The saved cooldown makes relogging
     * unable to reset the 180-second limit; the ordinary totem entity event is
     * reused for its familiar sound and particles without consuming an item.
     */
    public static boolean tryNineLives(Cat cat, float finalDamage) {
        if (cat.level().isClientSide || CatPoseData.isPancake(cat)
                || finalDamage < cat.getHealth()) return false;
        CatTraitProfile traits = CatTraitData.ensure(cat);
        int level = traits.level(CatTrait.NINE_LIVES);
        if (level <= 0) return false;

        long now = cat.level().getGameTime();
        if (cat.getPersistentData().getLong(NINE_LIVES_COOLDOWN_UNTIL_TAG) > now
                || cat.getRandom().nextInt(100)
                >= CatTrait.NINE_LIVES.nineLivesChance(level)) return false;

        cat.getPersistentData().putLong(NINE_LIVES_COOLDOWN_UNTIL_TAG,
                now + NINE_LIVES_COOLDOWN_TICKS);
        cat.setRemainingFireTicks(0);
        cat.level().broadcastEntityEvent(cat, (byte) 35);
        return true;
    }

    private CatTraitEffects() {}
}
