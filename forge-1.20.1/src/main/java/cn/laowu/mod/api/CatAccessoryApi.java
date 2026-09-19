package cn.laowu.mod.api;

import cn.laowu.mod.accessory.CatAccessories;
import cn.laowu.mod.accessory.*;
import cn.laowu.mod.*;
import cn.laowu.mod.genetics.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Stable additive script facade; v1-v3 contracts remain supported without a KubeJS dependency. */
public final class CatAccessoryApi {
    public static int schemaVersion() { return 1; }
    public static boolean isEquipped(Cat cat, String accessoryOrItemId) {
        return cat != null && CatAccessories.has(cat, accessoryOrItemId);
    }
    public static double effectValue(Cat cat, String effectId) {
        return cat == null ? 0 : CatAccessories.value(cat, effectId);
    }
    /** Copies only. Modify equipment through the server menu, not these returned stacks. */
    public static List<ItemStack> equippedItems(Cat cat) {
        return cat == null ? List.of() : CatAccessories.equipment(cat);
    }

    public static int apiVersion() { return 3; }
    /** Explicit compatibility promise, independent of the mod's release/build number.
     * Future APIs must retain these contracts or provide adapters before claiming support.
     */
    public static boolean supportsApi(int version) { return version >= 1 && version <= 3; }
    public static CatAccessoryHandle accessory(Cat cat, String id) {
        requireServer(cat);
        var inventory = CatProfileData.openContainer(cat);
        java.util.Set<String> ids = new java.util.HashSet<>(), groups = new java.util.HashSet<>();
        for (int slot = 0; slot < CatProfileData.ACCESSORY_SLOTS; slot++) {
            ItemStack stack = inventory.getItem(slot);
            CatAccessoryDefinition def = CatAccessoryRegistry.find(stack, false);
            if (def == null || !def.enabled() || !ids.add(def.id())) continue;
            if (!def.exclusiveGroup().isEmpty() && !groups.add(def.exclusiveGroup())) continue;
            if (def.activeFor(CatClothesData.getOutfit(cat).id()) && (id.equals(def.id()) || id.equals(def.item())))
                return new CatAccessoryHandle(cat, slot, inventory, stack, def.id());
        }
        return null;
    }
    public static boolean isPancake(Cat cat) { return CatPoseData.isPancake(cat); }
    public static boolean isFinishing(Cat cat) { return DynamiteCatLastStand.isFinishing(cat); }
    public static String outfit(Cat cat) { return CatClothesData.getOutfit(cat).id(); }
    public static int stat(Cat cat, String name) {
        for (CatStat stat : CatStat.values()) if (stat.serializedName().equals(name))
            return CatAttributeEffects.effectiveValue(cat, stat);
        throw new IllegalArgumentException("Unknown cat stat: " + name);
    }
    public static boolean friendly(Cat cat, LivingEntity target) {
        if (target == null || target.level() != cat.level()) return false;
        if (target instanceof net.minecraft.world.entity.TamableAnimal pet && cat.isTame() && pet.isTame())
            return target == cat || CatTeamRules.friendly(cat, pet);
        return target == cat || target == cat.getOwner() || cat.isAlliedTo(target);
    }
    public static boolean canHarm(Cat cat, LivingEntity target) {
        return target != null && target.level() == cat.level() && CatTeamRules.canHarm(cat, target);
    }
    public static List<Cat> nearbyAllies(Cat cat, double radius) {
        requireServer(cat); radius(radius);
        return cat.level().getEntitiesOfClass(Cat.class, cat.getBoundingBox().inflate(radius),
                other -> other.isAlive() && friendly(cat, other) && cat.distanceToSqr(other) <= radius * radius
                        && cat.hasLineOfSight(other)).stream().limit(32).toList();
    }
    public static List<LivingEntity> nearbyEnemies(Cat cat, double radius) {
        requireServer(cat); radius(radius);
        return cat.level().getEntitiesOfClass(LivingEntity.class, cat.getBoundingBox().inflate(radius),
                other -> canHarm(cat, other) && cat.distanceToSqr(other) <= radius * radius
                        && cat.hasLineOfSight(other)).stream().limit(32).toList();
    }
    /** Team-safe, bounded and non-recursive. Respects vanilla invulnerability frames. */
    public static boolean damage(Cat cat, LivingEntity target, double amount) {
        requireServer(cat); CatAccessoryScriptRules.damage(amount);
        if (!canHarm(cat, target) || cat.distanceToSqr(target) > 1024 || !cat.hasLineOfSight(target)) return false;
        return CatAccessoryHooks.withoutEvents(() -> target.hurt(cat.damageSources().mobAttack(cat), (float)amount));
    }
    public static boolean heal(Cat cat, LivingEntity target, double amount) {
        requireServer(cat); CatAccessoryScriptRules.damage(amount);
        if (target == null || !target.isAlive() || !friendly(cat, target) || cat.distanceToSqr(target) > 1024) return false;
        target.heal((float)amount); return true;
    }
    /** Team-safe passive thorns primitive; works for support cats without giving them an attack AI. */
    public static boolean reflectDamage(Cat cat,net.minecraft.world.damagesource.DamageSource source,double amount,int cooldownTicks) {
        requireServer(cat);CatAccessoryScriptRules.damage(amount);CatAccessoryScriptRules.ticks(cooldownTicks);
        return CatCommonAccessories.reflectDamage(cat,source,(float)amount,cooldownTicks);
    }
    public static boolean addEffect(Cat cat, LivingEntity target, String effectId, int ticks, int amplifier, boolean allies) {
        requireServer(cat); CatAccessoryScriptRules.ticks(ticks);
        if (amplifier < 0 || amplifier > 255) throw new IllegalArgumentException("Invalid effect amplifier");
        if (target == null || !target.isAlive() || cat.distanceToSqr(target) > 1024
                || !(allies ? friendly(cat, target) : canHarm(cat, target))) return false;
        ResourceLocation id = new ResourceLocation(effectId);
        var effect = BuiltInRegistries.MOB_EFFECT.get(id);
        if (effect == null) throw new IllegalArgumentException("Unknown mob effect " + effectId);
        return target.addEffect(new MobEffectInstance(effect, ticks, amplifier), cat);
    }
    private static void radius(double radius) {
        if (!Double.isFinite(radius) || radius < 0 || radius > 32) throw new IllegalArgumentException("Radius must be 0..32");
    }
    public static void requireServer(Cat cat) {
        if (cat == null || cat.level().isClientSide || cat.getServer() == null || !cat.getServer().isSameThread() || cat.isRemoved())
            throw new IllegalStateException("Cat accessory mutation requires a loaded cat on the server thread");
    }
    private CatAccessoryApi() {}
}
