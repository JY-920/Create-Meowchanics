package cn.laowu.mod.accessory;

import cn.laowu.mod.*;
import cn.laowu.mod.genetics.CatAttributeEffects;
import cn.laowu.mod.genetics.CatStat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import java.util.function.BooleanSupplier;

/** General accessories. Cooldowns survive saving; genes, vanilla absorption and cargo size are untouched. */
public final class CatCommonAccessories {
    public static final String DATA = "LaoWuGeneralAccessories";
    private static final Map<LivingEntity, Hit> HITS = new WeakHashMap<>();
    private static final Map<Cat, State> STATES = new WeakHashMap<>();
    private static final ThreadLocal<Boolean> REFLECTING = ThreadLocal.withInitial(() -> false);
    private static final class State {
        UUID target;
        int stacks, ownerBonus;
        long lastHit = Long.MIN_VALUE / 2, lastStack = Long.MIN_VALUE / 2;
        long lastCombat, nextRestHeal;
        boolean mint;
        State(long time) { lastCombat = time; nextRestHeal = time + 200; }
    }
    private static final class Hit {
        final DamageSource source;
        Hit previous;
        final long tick;
        float beforeHealth, proposed, shieldSpent;
        boolean critical, capped, prepared, prevented, eligible;
        BooleanSupplier allowed = () -> true;
        Hit(LivingEntity victim, DamageSource source) {
            this.source = source; tick = victim.level().getGameTime(); beforeHealth = victim.getHealth();
        }
    }
    private static long now(Cat cat) { return cat.level().getGameTime(); }
    private static State state(Cat cat) { return STATES.computeIfAbsent(cat, c -> new State(now(c))); }
    private static CompoundTag data(Cat cat) {
        var root = cat.getPersistentData();
        if (!root.contains(DATA, net.minecraft.nbt.Tag.TAG_COMPOUND)) root.put(DATA, new CompoundTag());
        return root.getCompound(DATA);
    }
    private static boolean ready(Cat cat, String key) { return now(cat) >= data(cat).getLong(key); }
    public static boolean isReflecting() { return REFLECTING.get(); }
    private static boolean auxiliary(DamageSource source) {
        return REFLECTING.get() || CatAccessories.isExtraStrike() || CatAccessoryHooks.isDispatching()
                || source.is(DamageTypes.THORNS);
    }
    private static Cat attacker(LivingEntity victim, DamageSource source) {
        return !auxiliary(source) && source.getEntity() instanceof Cat cat && cat != victim && cat.isAlive()
                && !cat.level().isClientSide && !DynamiteCatLastStand.isFinishing(cat)
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && (source.getDirectEntity() == cat || source.getDirectEntity() instanceof Projectile) ? cat : null;
    }
    private static Hit hit(LivingEntity victim, DamageSource source) {
        Hit current = HITS.get(victim);
        for (int depth = 0; current != null && depth < 8; depth++, current = current.previous)
            if (current.source == source && current.tick == victim.level().getGameTime()) return current;
        return null;
    }
    /** Pre-armor outgoing modifiers. A later cancellation must not grant stacks or consume the mint cooldown. */
    public static float beforeDamage(LivingEntity victim, DamageSource source, float amount) {
        if (victim.level().isClientSide || !(amount > 0) || !Float.isFinite(amount)) return amount;
        Hit hit = new Hit(victim, source);
        if (victim instanceof Cat || source.getEntity() instanceof Cat) {
            Hit outer = HITS.get(victim);
            if (auxiliary(source) && outer != null && outer.tick == hit.tick) hit.previous = outer;
            HITS.put(victim, hit);
        }
        Cat cat = attacker(victim, source);
        if (cat == null || !CatTeamRules.canHarm(cat, victim)) return amount;
        hit.eligible = true; // Preserve pre-hit team eligibility even when this hit kills the target.
        State s = state(cat);
        double bonus = 0;
        if (CatAccessories.value(cat, "damage_combo") > 0) {
            if (!victim.getUUID().equals(s.target) || now(cat) - s.lastHit >= 80) {
                s.target = victim.getUUID(); s.stacks = 0; s.lastStack = Long.MIN_VALUE / 2;
            }
            bonus += s.stacks * CatAccessories.value(cat, "damage_combo") / 100;
        }
        float result = (float)(amount * (1 + bonus));
        if (victim.getHealth() > victim.getMaxHealth() * .90F
                && CatAccessories.value(cat, "opening_damage") > 0) {
            result *= (float)(1 + CatAccessories.value(cat, "opening_damage") / 100);
        }
        return result;
    }
    public static void critical(Cat cat, LivingEntity victim, DamageSource source) {
        Hit hit = hit(victim, source);
        if (hit != null && hit.eligible && hit.source == source && attacker(victim, source) == cat) hit.critical = true;
    }
    /** Loader adapter passes health-bound damage, AFTER vanilla armor and absorption. */
    public static float finalDamage(LivingEntity victim, DamageSource source, float amount, BooleanSupplier allowed) {
        if (victim.level().isClientSide) return amount;
        Hit hit = hit(victim, source);
        if (hit == null || hit.source != source || hit.tick != victim.level().getGameTime()) return amount;
        hit.beforeHealth = victim.getHealth(); hit.prepared = true; hit.allowed = allowed;
        float result = amount;
        if (victim instanceof Cat cat && result > 0
                && !DynamiteCatLastStand.isFinishing(cat)
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (source.getEntity() instanceof LivingEntity && source.getEntity() != cat
                    && CatAccessories.value(cat, "heavy_hit_cap") > 0 && ready(cat, "RolyReady")) {
                float cap = (float)(cat.getMaxHealth() * CatAccessories.value(cat, "heavy_hit_cap") / 100);
                if (result > cap) { result = cap; hit.capped = true; }
            }
            float shield = shield(cat);
            hit.shieldSpent = Math.min(result, shield);
            result -= hit.shieldSpent;
        }
        hit.proposed = result;
        return result;
    }
    /** Death-prevention traits can change health without accepting this attack. */
    public static void prevented(LivingEntity victim) {
        Hit hit = HITS.get(victim); if (hit != null) hit.prevented = true;
    }
    /** Called at actuallyHurt RETURN on both loaders, not the misleading Forge pre-health Damage event. */
    public static void finishDamage(LivingEntity victim, DamageSource source) {
        if (victim.level().isClientSide) return; // Integrated-client entities can share server runtime IDs.
        Hit hit = hit(victim, source);
        if (hit == null) return;
        if (hit.previous != null && hit.previous.tick == victim.level().getGameTime()) HITS.put(victim, hit.previous);
        else HITS.remove(victim);
        if (hit.source != source || !hit.prepared || hit.prevented || !hit.allowed.getAsBoolean()
                || victim.level().isClientSide || hit.tick != victim.level().getGameTime()) return;
        float taken = Math.max(0, hit.beforeHealth - victim.getHealth());
        if (victim instanceof Cat cat) {
            if (hit.shieldSpent > 0 && (taken > 0 || hit.proposed == 0)) {
                data(cat).putFloat("Shield", Math.max(0, shield(cat) - hit.shieldSpent));
                combat(cat); particles(cat, ParticleTypes.ENCHANTED_HIT);
            }
            if (hit.capped && (taken > 0 || hit.shieldSpent > 0 && hit.proposed == 0)) {
                data(cat).putLong("RolyReady", now(cat) + 300);
                CatAccessoryDurability.spendEffect(cat,"heavy_hit_cap");
            }
            if (taken > 0) {
                combat(cat);
                if (cat.isAlive() && CatAccessories.value(cat, "emergency_shield") > 0
                        && hit.beforeHealth > cat.getMaxHealth() * .35F
                        && cat.getHealth() <= cat.getMaxHealth() * .35F && ready(cat, "ShieldReady")) {
                    data(cat).putFloat("Shield", (float)(cat.getMaxHealth() * CatAccessories.value(cat, "emergency_shield") / 100));
                    data(cat).putLong("ShieldUntil", now(cat) + 120);
                    data(cat).putLong("ShieldReady", now(cat) + 300);
                    // The fiftieth successful proc still grants its full six-second shield.
                    data(cat).putBoolean("ShieldLastUse",true);
                    if(!CatAccessoryDurability.spendEffect(cat,"emergency_shield"))data(cat).remove("ShieldLastUse");
                    particles(cat, ParticleTypes.ENCHANTED_HIT);
                }
                reflect(cat, source, taken);
                if(!auxiliary(source))CatAccessoryHooks.acceptedHurt(cat,source,taken);
            }
        }
        Cat cat = attacker(victim, source);
        if (cat == null || !hit.eligible || taken <= 0) return;
        combat(cat);
        State s = state(cat);
        if (CatAccessories.value(cat, "damage_combo") > 0) {
            if (!victim.getUUID().equals(s.target) || now(cat) - s.lastHit >= 80) {
                s.target = victim.getUUID(); s.stacks = 0; s.lastStack = Long.MIN_VALUE / 2;
            }
            if (now(cat) - s.lastStack >= 10) {
                s.stacks = Math.min(5, s.stacks + 1); s.lastStack = now(cat);
            }
            s.lastHit = now(cat);
        }
        if (hit.critical && CatAccessories.value(cat, "critical_haste") > 0 && ready(cat, "MintReady")) {
            data(cat).putLong("MintUntil", now(cat) + 80);
            data(cat).putLong("MintReady", now(cat) + 160);
            particles(cat, ParticleTypes.HAPPY_VILLAGER);
        }
        CatAccessoryHooks.acceptedAttack(cat,victim,source,taken);
    }
    private static void reflect(Cat cat, DamageSource source, float taken) {
        if(auxiliary(source))return;
        double percent=CatAccessories.value(cat,"melee_reflect");
        if(percent>0)reflectDamage(cat,source,(float)(taken*percent/100),20);
    }
    /** Low-level thorns operation; the script supplies actual reflected damage and the cooldown. */
    public static boolean reflectDamage(Cat cat,DamageSource source,float damage,int cooldownTicks) {
        if(source==null || !(damage>0) || REFLECTING.get() || CatAccessories.isExtraStrike()
                || source.is(DamageTypes.THORNS) || DynamiteCatLastStand.isFinishing(cat)
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || !(source.getEntity() instanceof LivingEntity enemy)
                || source.getDirectEntity() != enemy || cat.distanceToSqr(enemy) > 9
                || enemy == cat || enemy == cat.getOwner() || !enemy.isAlive()
                || enemy instanceof net.minecraft.world.entity.player.Player
                || CatTeamRules.friendly(cat, enemy) || cat.isAlliedTo(enemy) || !ready(cat,"ReflectReady"))return false;
        data(cat).putLong("ReflectReady",now(cat)+cooldownTicks);
        REFLECTING.set(true);
        try{return CatAccessoryHooks.withoutEvents(()->enemy.hurt(cat.damageSources().thorns(cat),damage));}
        finally{REFLECTING.remove();}
    }
    public static float shield(Cat cat) {
        if (CatAccessories.value(cat, "emergency_shield") <= 0 && !data(cat).getBoolean("ShieldLastUse")
                || ready(cat, "ShieldUntil")) return 0;
        return Math.max(0, data(cat).getFloat("Shield"));
    }
    public static double haste(Cat cat) {
        if (cat.level().isClientSide)
            return cat.getPersistentData().getCompound(CatAccessories.CLIENT_STATE).getDouble("MintHaste");
        return CatAccessories.value(cat, "critical_haste") > 0 && !ready(cat, "MintUntil")
                ? CatAccessories.value(cat, "critical_haste") / 100 : 0;
    }
    public static int ownerBonus(Cat cat) {
        if (cat.level().isClientSide)
            return cat.getPersistentData().getCompound(CatAccessories.CLIENT_STATE).getInt("OwnerAttackBonus");
        double bonus = CatAccessories.value(cat, "owner_attack_bonus");
        if (bonus <= 0 || !cat.isAlive() || CatPoseData.isPancake(cat)) return 0;
        var owner = cat.getOwner();
        return owner != null && owner.isAlive() && owner.level() == cat.level()
                && owner.distanceToSqr(cat) <= 16 ? (int)bonus : 0;
    }
    public static float receivedHealing(LivingEntity patient, float amount) {
        if (!(patient instanceof Cat cat) || cat.level().isClientSide || !(amount > 0) || !Float.isFinite(amount)) return amount;
        return CatAccessoryHooks.beforeHeal(cat,(float)(amount * (1 + CatAccessories.value(cat, "healing_received") / 100)));
    }
    public static boolean allowsPickup(Cat cat, Container inventory, ItemStack stack) {
        if (CatAccessories.value(cat, "sample_pickup") <= 0) return true;
        for (int slot = CatProfileData.ACCESSORY_SLOTS; slot < CatProfileData.SLOT_COUNT; slot++)
            if (!inventory.getItem(slot).isEmpty() && inventory.getItem(slot).is(stack.getItem())) return true;
        return false;
    }
    private static void combat(Cat cat) {
        State s = state(cat); s.lastCombat = now(cat); s.nextRestHeal = now(cat) + 200;
        data(cat).putLong("LastCombat", now(cat));
    }
    public static void tick(Cat cat) {
        if (cat.level().isClientSide || !cat.isAlive()) return;
        State s = state(cat);
        if (cat.getTarget() != null && cat.getTarget().isAlive() || HissingCatBehavior.isFighting(cat)
                || DynamiteCatLastStand.isActive(cat) || CatMusicSupport.performing(cat)
                || CatMedicalHealing.casting(cat) && !CatMedicalHealing.stationed(cat)) combat(cat);
        if (CatAccessories.value(cat, "rest_heal") > 0 && !CatPoseData.isPancake(cat)) {
            long last = Math.max(s.lastCombat, data(cat).getLong("LastCombat"));
            if (now(cat) - last >= 200 && now(cat) >= s.nextRestHeal) {
                s.nextRestHeal = now(cat) + 40;
                if (cat.getHealth() < cat.getMaxHealth()) {
                    cat.heal((float)CatAccessories.value(cat, "rest_heal"));
                    particles(cat, ParticleTypes.HEART);
                }
            }
        }
        if (CatAccessories.value(cat, "damage_combo") <= 0) { s.stacks = 0; s.target = null; }
        if (CatAccessories.value(cat, "emergency_shield") <= 0 && !data(cat).getBoolean("ShieldLastUse")
                || ready(cat,"ShieldUntil")) {data(cat).remove("Shield");data(cat).remove("ShieldLastUse");}
        if (CatAccessories.value(cat, "critical_haste") <= 0) data(cat).remove("MintUntil");
        int owner = ownerBonus(cat); boolean mint = haste(cat) > 0;
        if (owner != s.ownerBonus || mint != s.mint) {
            s.ownerBonus = owner; s.mint = mint;
            CatAttributeEffects.refresh(cat);
            CatAccessories.syncDynamic(cat);
        }
    }
    private static void particles(Cat cat, net.minecraft.core.particles.SimpleParticleType type) {
        if (cat.level() instanceof ServerLevel level)
            level.sendParticles(type, cat.getX(), cat.getY(.6), cat.getZ(), 5, .2, .15, .2, .02);
    }
    public static void equipmentChanged(Cat cat) {
        // Taking an accessory off cannot preserve its temporary proc, but never resets its saved cooldown.
        State s = state(cat);
        if (CatAccessories.value(cat, "damage_combo") <= 0) { s.stacks = 0; s.target = null; }
        if (CatAccessories.value(cat, "critical_haste") <= 0) data(cat).remove("MintUntil");
        if (CatAccessories.value(cat, "emergency_shield") <= 0 && !data(cat).getBoolean("ShieldLastUse")
                || ready(cat,"ShieldUntil")) {data(cat).remove("Shield");data(cat).remove("ShieldLastUse");}
    }
    public static void reset() { HITS.clear(); STATES.clear(); REFLECTING.remove(); }
    private CatCommonAccessories() {}
}
