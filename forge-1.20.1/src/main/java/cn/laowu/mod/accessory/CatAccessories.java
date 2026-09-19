package cn.laowu.mod.accessory;

import cn.laowu.mod.*;
import cn.laowu.mod.genetics.CatAttributeEffects;
import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.nbt.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import java.lang.ref.WeakReference;
import java.util.*;

/** Server-authoritative effects. Equipment NBT stays in CatProfileData, including unknown items. */
public final class CatAccessories {
    public static final String CLIENT_STATE = "LaoWuAccessoryState";
    private record Cached(Tag source, long revision, String outfit, Map<String, Double> effects,
                          Set<String> ids) {}
    private record Strike(WeakReference<LivingEntity> target, long due, boolean projectile) {}
    private static final Map<Cat, Cached> CACHE = new WeakHashMap<>();
    private static final Map<Cat, Cached> CLIENT_CACHE = new WeakHashMap<>();
    private static final Map<Cat, CompoundTag> LAST_SYNC = new WeakHashMap<>();
    private static final Map<Cat, List<Strike>> STRIKES = new WeakHashMap<>();
    private static final Map<Cat, Long> LAST_ROLL = new WeakHashMap<>();
    private static final ThreadLocal<Boolean> EXTRA_STRIKE = ThreadLocal.withInitial(() -> false);

    public static boolean isAccessory(ItemStack stack, boolean client) {
        CatAccessoryDefinition def = CatAccessoryRegistry.find(stack, client);
        return def != null && def.enabled();
    }
    public static boolean mayEquip(Container container, int slot, ItemStack stack, boolean client) {
        CatAccessoryDefinition def = CatAccessoryRegistry.find(stack, client);
        if (def == null || !def.enabled()) return false;
        for (int i = 0; i < CatProfileData.ACCESSORY_SLOTS; i++) {
            if (i == slot) continue;
            CatAccessoryDefinition other = CatAccessoryRegistry.find(container.getItem(i), client);
            if (other != null && (other.id().equals(def.id()) ||
                    !def.exclusiveGroup().isEmpty() && def.exclusiveGroup().equals(other.exclusiveGroup())))
                return false;
        }
        return true;
    }
    public static double value(Cat cat, String effect) {
        if (cat.level().isClientSide && cat.getPersistentData().contains(CLIENT_STATE, Tag.TAG_COMPOUND))
            return cat.getPersistentData().getCompound(CLIENT_STATE).getCompound("Effects").getDouble(effect);
        return cached(cat).effects.getOrDefault(effect, 0.0D);
    }
    public static int statBonus(Cat cat, CatStat stat) {
        return (int)value(cat, stat.serializedName()) + (stat == CatStat.ATTACK ? CatCommonAccessories.ownerBonus(cat) : 0)
                + (stat == CatStat.SPEED ? CatAccessoryAuras.speedBonus(cat) : 0);
    }
    public static boolean isExtraStrike() { return EXTRA_STRIKE.get(); }
    public static void syncDynamic(Cat cat) { if (!cat.level().isClientSide) syncIfChanged(cat); }
    public static boolean has(Cat cat, String id) {
        if (!cat.level().isClientSide) return cached(cat).ids.contains(id);
        ListTag ids = cat.getPersistentData().getCompound(CLIENT_STATE).getList("Ids", Tag.TAG_STRING);
        return ids.contains(StringTag.valueOf(id));
    }
    private static Cached cached(Cat cat) {
        Tag source = cat.getPersistentData().get(CatProfileData.ITEMS_TAG);
        String outfit = CatClothesData.getOutfit(cat).id();
        boolean clientSide = cat.level().isClientSide;
        Map<Cat, Cached> cache = clientSide ? CLIENT_CACHE : CACHE;
        Cached old = cache.get(cat);
        long revision = CatAccessoryRegistry.revision(clientSide);
        if (old != null && old.source == source && old.revision == revision && old.outfit.equals(outfit))
            return old;
        var loadout = CatAccessoryLoadout.resolve(equipment(cat).stream()
                .map(stack -> CatAccessoryRegistry.find(stack, clientSide)).toList(), outfit);
        Map<String, Double> effects = new HashMap<>(loadout.effects());
        Set<String> scripted = new HashSet<>();
        for (ItemStack stack : equipment(cat)) {
            var def = CatAccessoryRegistry.find(stack, clientSide);
            if (def == null || !loadout.ids().contains(def.id()) || !def.activeFor(outfit) || !scripted.add(def.id())) continue;
            CompoundTag bonuses = CatAccessoryStackData.read(stack).getCompound("StatBonuses");
            for (String stat : CatAccessoryDefinition.STATS) {
                int bonus = Math.max(-300, Math.min(300, bonuses.getInt(stat)));
                if (bonus != 0) effects.merge(stat, (double)bonus, Double::sum);
            }
        }
        Cached result = new Cached(source, revision, outfit, Map.copyOf(effects), loadout.ids());
        cache.put(cat, result);
        return result;
    }
    /** Copy-only API: callers cannot bypass the menu or mutate the live container. */
    public static List<ItemStack> equipment(Cat cat) {
        List<ItemStack> items = new ArrayList<>(Collections.nCopies(CatProfileData.ACCESSORY_SLOTS, ItemStack.EMPTY));
        ListTag saved = cat.getPersistentData().getList(CatProfileData.ITEMS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < saved.size(); i++) {
            CompoundTag entry = saved.getCompound(i);
            if (!entry.contains("Slot", Tag.TAG_ANY_NUMERIC)) continue;
            int slot = entry.getByte("Slot") & 255;
            if (slot < CatProfileData.ACCESSORY_SLOTS) items.set(slot, decode(cat, entry));
        }
        return items;
    }
    private static ItemStack decode(Cat cat, CompoundTag entry) { return ItemStack.of(entry); }

    public static CompoundTag state(Cat cat) {
        Cached cached = cached(cat);
        CompoundTag state = new CompoundTag(), effects = new CompoundTag();
        cached.effects.forEach(effects::putDouble);
        state.put("Effects", effects);
        ListTag ids = new ListTag();
        cached.ids.stream().sorted().forEach(id -> ids.add(StringTag.valueOf(id)));
        state.put("Ids", ids);
        state.putInt("OwnerAttackBonus", CatCommonAccessories.ownerBonus(cat));
        state.putDouble("MintHaste", CatCommonAccessories.haste(cat));
        state.putInt("MusicSpeedBonus", CatAccessoryAuras.speedBonus(cat));
        return state;
    }
    public static void equipmentChanged(Cat cat) {
        if (cat.level().isClientSide) return;
        CACHE.remove(cat);
        if (!cat.isAlive()) return;
        CatCommonAccessories.equipmentChanged(cat);
        CatAttributeEffects.refresh(cat);
        syncIfChanged(cat);
        CatAccessoryHooks.equipmentChanged(cat);
    }
    private static void syncIfChanged(Cat cat) {
        CompoundTag state = state(cat);
        if (!state.equals(LAST_SYNC.get(cat))) {
            LAST_SYNC.put(cat, state);
            ModNetwork.syncCatAccessories(cat, null, state);
        }
    }
    public static boolean fireImmune(Cat cat) { return value(cat, "fire_immune") > 0; }
    public static boolean knockbackImmune(Cat cat) { return value(cat, "knockback_resistance") > 0; }
    public static boolean projectileKnockback(Cat cat) { return value(cat, "projectile_knockback") > 0; }
    public static double explosionMultiplier(Cat cat, double fallback) {
        return CatClothesData.getOutfit(cat) == CatOutfitType.DYNAMITE
                ? Math.max(fallback, value(cat, "self_destruct_multiplier")) : fallback;
    }
    /** Butter mitigation only applies while moving horizontally and to attributed living attacks. */
    public static float mitigateMovingDamage(LivingEntity victim, DamageSource source, float amount) {
        amount = CatAccessoryAuras.mitigate(victim, source, amount);
        if (!(victim instanceof Cat cat) || cat.level().isClientSide || amount <= 0
                || !(source.getEntity() instanceof LivingEntity) || source.getEntity() == cat
                || CatPoseData.isPancake(cat) || cat.isPassenger()
                || cat.isOrderedToSit() || cat.isInSittingPose()
                || DynamiteCatLastStand.isFinishing(cat)
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || source.is(net.minecraft.world.damagesource.DamageTypes.THORNS)
                || cat.getDeltaMovement().horizontalDistanceSqr() <= 1.0E-4D) return amount;
        double reduction = Math.max(0, Math.min(80, value(cat, "moving_damage_reduction")));
        return (float) (amount * (1.0D - reduction / 100.0D));
    }
    public static boolean avoidDamage(Cat cat, DamageSource source) {
        if (DynamiteCatLastStand.isFinishing(cat) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))
            return false;
        if(CatAccessoryHooks.beforeAvoid(cat,source))return true;
        if (source.is(DamageTypeTags.IS_FIRE) && fireImmune(cat)) {
            cat.clearFire();
            return true;
        }
        // Flight dodge applies to attacks, not void, starvation, self-destruction or reflected thorns.
        if (CatClothesData.getOutfit(cat) != CatOutfitType.FLIGHT
                || source.getEntity() == null || source.getEntity() == cat
                || source.is(net.minecraft.world.damagesource.DamageTypes.THORNS)) return false;
        double chance = CatAccessoryDefinition.dodgeChance(
                CatAttributeEffects.effectiveValue(cat, CatStat.SPEED), value(cat, "pilot_dodge_per_speed"));
        return chance > 0 && cat.getRandom().nextDouble() < chance;
    }
    public static void acceptedAttack(LivingEntity victim, DamageSource source, float damage) {
        if (CatAccessoryHooks.isDispatching() || EXTRA_STRIKE.get() || damage <= 0 || !(source.getEntity() instanceof Cat cat)
                || cat.level().isClientSide || !cat.isAlive() || !CatTeamRules.canHarm(cat, victim)
                || source.is(DamageTypeTags.IS_EXPLOSION) && !(source.getDirectEntity() instanceof Projectile)
                || source.is(net.minecraft.world.damagesource.DamageTypes.THORNS)
                || source.getDirectEntity() != cat && !(source.getDirectEntity() instanceof Projectile)) return;
        double chance = value(cat, "extra_strike_chance") / 100.0D;
        if (chance <= 0) return;
        long now = cat.level().getGameTime();
        if (Objects.equals(LAST_ROLL.get(cat), now)) return; // One roll per attack tick, not per splash victim.
        LAST_ROLL.put(cat, now);
        if (cat.getRandom().nextDouble() >= chance) return;
        List<Strike> pending = STRIKES.computeIfAbsent(cat, ignored -> new ArrayList<>());
        if (pending.size() < 4) pending.add(new Strike(new WeakReference<>(victim), now + 2,
                source.getDirectEntity() instanceof Projectile));
    }
    public static void tick(Cat cat) {
        if (cat.level().isClientSide || !cat.isAlive()) return;
        CatAccessoryHooks.tick(cat);
        CatCommonAccessories.tick(cat);
        List<Strike> pending = STRIKES.get(cat);
        if (pending != null) {
            List<Strike> due = pending.stream().filter(s -> s.due <= cat.level().getGameTime()).toList();
            pending.removeAll(due);
            if (pending.isEmpty()) STRIKES.remove(cat);
            for (Strike strike : due) extraStrike(cat, strike);
        }
        CatAccessoryAuras.tick(cat);
        // Pull every other tick so floor friction cannot stop items between half-second impulses.
        if (Math.floorMod(cat.tickCount + cat.getId(), 2) == 0) {
            double magnet = value(cat, "loot_magnet_radius");
            if (magnet <= 0 && value(cat, "sample_pickup") > 0) magnet = 1;
            if (magnet > 0 && cat.isTame() && !CatPoseData.isPancake(cat)) attractLoot(cat, magnet);
        }
        if (Math.floorMod(cat.tickCount + cat.getId(), 10) != 0) return;
        // Also notice in-place NBT edits made by commands/scripts, without deserializing on every hit.
        CACHE.remove(cat);
        syncIfChanged(cat);
        if (fireImmune(cat)) cat.clearFire();
        if (value(cat, "aggro_bias") > 0 && !CatPoseData.isPancake(cat)
                && Math.floorMod(cat.tickCount + cat.getId(), 20) == 0) {
            int visited = 0;
            for (Mob mob : cat.level().getEntitiesOfClass(Mob.class, cat.getBoundingBox().inflate(8),
                    m -> m != cat && m.getTarget() != null && ally(cat, m.getTarget()))) {
                if (++visited > 32) break;
                LivingEntity preferred = preferTarget(mob, mob.getTarget());
                if (preferred != mob.getTarget()) mob.setTarget(preferred);
            }
        }
    }
    private static void extraStrike(Cat cat, Strike strike) {
        LivingEntity target = strike.target.get();
        if (target == null || !cat.isAlive() || target.level() != cat.level()
                || !CatTeamRules.canHarm(cat, target) || cat.distanceToSqr(target) > 1024
                || !cat.hasLineOfSight(target) || DynamiteCatLastStand.isActive(cat)) return;
        int invulnerability = target.invulnerableTime;
        EXTRA_STRIKE.set(true);
        try {
            target.invulnerableTime = 0;
            float damage = (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE);
            CatAccessoryHooks.withoutEvents(() -> strike.projectile
                    ? CatProjectileDamage.hurt(target, cat.damageSources().mobAttack(cat), damage)
                    : target.hurt(cat.damageSources().mobAttack(cat), damage));
        } finally {
            target.invulnerableTime = Math.max(invulnerability, target.invulnerableTime);
            EXTRA_STRIKE.remove();
        }
    }
    private static boolean ally(Cat cat, LivingEntity entity) {
        if (entity instanceof net.minecraft.world.entity.TamableAnimal pet && pet.isTame() && cat.isTame())
            return cat == pet || CatTeamRules.friendly(cat, pet);
        return entity == cat || entity == cat.getOwner() || CatTeamRules.friendly(cat, entity)
                || entity != null && cat.isAlliedTo(entity);
    }
    private static boolean validEnemy(Mob mob, Cat candidate) {
        return candidate.isAlive() && !CatPoseData.isPancake(candidate) && mob != candidate
                && mob.distanceToSqr(candidate) <= 64.0D
                && !CatTeamRules.friendly(mob, candidate) && mob != candidate.getOwner()
                && mob.hasLineOfSight(candidate)
                && (mob instanceof Cat attacker ? CatTeamRules.canHarm(attacker, candidate)
                    : !mob.isAlliedTo(candidate) && mob.canAttack(candidate));
    }
    public static LivingEntity preferTarget(Mob mob, LivingEntity current) {
        if (current == null || !current.isAlive() || mob.level().isClientSide) return current;
        if (CatTeamRules.friendly(mob, current)) return null;
        LivingEntity best = current;
        double bias = current instanceof Cat cat ? value(cat, "aggro_bias") : 0;
        // Being quiet reduces attention, but does not erase direct retaliation.
        if (bias < 0 && mob.getLastHurtByMob() == current) return current;
        double bestScore = mob.distanceToSqr(current) / CatAccessoryDefinition.threatWeight(bias);
        for (Cat candidate : mob.level().getEntitiesOfClass(Cat.class, mob.getBoundingBox().inflate(8),
                c -> ally(c, current) && validEnemy(mob, c))) {
            double candidateBias = value(candidate, "aggro_bias");
            if (candidateBias == 0 && bias >= 0) continue;
            double score = mob.distanceToSqr(candidate) / CatAccessoryDefinition.threatWeight(candidateBias);
            if (score < bestScore) { best = candidate; bestScore = score; }
        }
        if (bias < 0 && current instanceof Cat cat && cat.getOwner() instanceof Player owner
                && !owner.isCreative() && !owner.isSpectator() && mob.canAttack(owner)
                && !mob.isAlliedTo(owner) && mob.hasLineOfSight(owner)
                && !(mob instanceof Cat) && mob.distanceToSqr(owner) <= 64
                && mob.distanceToSqr(owner) < bestScore) best = owner;
        return best;
    }
    private static void attractLoot(Cat cat, double radius) {
        Container inventory = null;
        int processed = 0;
        for (ItemEntity item : cat.level().getEntitiesOfClass(ItemEntity.class,
                cat.getBoundingBox().inflate(radius), ItemEntity::isAlive)) {
            if (++processed > 32) break;
            if (cat.distanceToSqr(item) > radius * radius || item.hasPickUpDelay()
                    || !canPickUp(cat, item)
                    || !cat.hasLineOfSight(item)) continue;
            if (inventory == null) inventory = CatProfileData.openContainer(cat);
            ItemStack stack = item.getItem();
            if (!CatCommonAccessories.allowsPickup(cat, inventory, stack)) continue;
            if (!canStore(inventory, stack)) continue;
            Vec3 delta = cat.position().add(0, 0.3, 0).subtract(item.position());
            if (delta.lengthSqr() > 1) {
                item.setDeltaMovement(delta.normalize().scale(Math.min(.65, .25 + delta.length()*.15)));
                item.hasImpulse = true; item.hurtMarked = true;
                continue;
            }
            ItemStack remaining = stack.copy();
            for (int slot = CatProfileData.ACCESSORY_SLOTS;
                    slot < CatProfileData.SLOT_COUNT && !remaining.isEmpty(); slot++) {
                ItemStack stored = inventory.getItem(slot);
                if (stored.isEmpty()) {
                    int count = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                    inventory.setItem(slot, remaining.copyWithCount(count));
                    remaining.shrink(count);
                } else if (ItemStack.isSameItemSameTags(stored, remaining)) {
                    int count = Math.min(remaining.getCount(), stored.getMaxStackSize() - stored.getCount());
                    if (count > 0) { stored.grow(count); remaining.shrink(count); }
                }
            }
            if (remaining.getCount() == stack.getCount()) continue;
            inventory.setChanged();
            if (remaining.isEmpty()) item.discard();
            else item.setItem(remaining);
        }
    }
    private static boolean canPickUp(Cat cat, ItemEntity item) {
        // getOwner() is the thrower, NOT the pickup restriction. Forge lacks NeoForge's getTarget().
        CompoundTag saved = new CompoundTag();
        item.addAdditionalSaveData(saved);
        return !saved.hasUUID("Owner") || saved.getUUID("Owner").equals(cat.getOwnerUUID());
    }

    private static boolean canStore(Container inventory, ItemStack stack) {
        for (int slot = CatProfileData.ACCESSORY_SLOTS; slot < CatProfileData.SLOT_COUNT; slot++) {
            ItemStack stored = inventory.getItem(slot);
            if (stored.isEmpty() || ItemStack.isSameItemSameTags(stored, stack)
                    && stored.getCount() < stored.getMaxStackSize()) return true;
        }
        return false;
    }
    public static void reset() {
        CatAccessoryHooks.resetWorld();
        CatCommonAccessories.reset();
        CACHE.clear(); LAST_SYNC.clear(); STRIKES.clear(); LAST_ROLL.clear();
        CatAccessoryRegistry.resetServer();
    }
    private CatAccessories() {}
}
