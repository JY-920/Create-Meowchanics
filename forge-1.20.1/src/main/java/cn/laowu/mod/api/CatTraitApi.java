package cn.laowu.mod.api;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Stable optional script API v1. Definitions use data/<namespace>/cat_traits/<path>.json, schema 1. */
public final class CatTraitApi {
    public static int apiVersion() { return 1; }
    public static int schemaVersion() { return 1; }
    public static boolean supportsApi(int version) { return version == 1; }
    public static float health(Cat cat) { requireServer(cat); return cat.getHealth(); }
    public static float maxHealth(Cat cat) { requireServer(cat); return cat.getMaxHealth(); }
    public static List<String> registeredIds() { return CatTraitRegistry.values(false).stream().map(t -> t.id().toString()).toList(); }
    public static List<String> ids(Cat cat) {
        requireServer(cat);
        return CatTraitData.ensure(cat).traits().stream().map(t -> t.trait().id().toString()).toList();
    }
    public static int level(Cat cat, String id) { requireServer(cat); return CatTraitData.ensure(cat).level(type(id)); }
    public static int storedLevel(Cat cat, String id) { requireServer(cat); return CatTraitData.ensure(cat).rawLevel(type(id)); }
    public static boolean has(Cat cat, String id) { return level(cat, id) > 0; }
    public static boolean isRegistered(String id) { return type(id).available(); }
    public static boolean setLevel(Cat cat, String id, int level) {
        requireServer(cat);
        if (!cat.isAlive()) throw new IllegalStateException("Cannot edit a dead cat");
        validateLevel(level);
        CatTraitType type = type(id);
        CatTraitProfile before = CatTraitData.ensure(cat), after = before.withLevel(type, level);
        if (after != before) { CatTraitData.set(cat, after); ModNetwork.syncCatTraitsToTracking(cat); }
        return after.rawLevel(type) == (level == 0 ? 0 : type.clampLevel(level));
    }
    public static boolean remove(Cat cat, String id) { return setLevel(cat, id, 0); }
    public static CatTraitHandle trait(Cat cat, String id) {
        requireServer(cat); var type = type(id);
        return type.enabled() && CatTraitData.ensure(cat).has(type) ? new CatTraitHandle(cat, type) : null;
    }
    public static List<String> pancakeIds(ItemStack stack) {
        return pancake(stack).traits().stream().map(t -> t.trait().id().toString()).toList();
    }
    public static int pancakeLevel(ItemStack stack, String id) { return pancake(stack).rawLevel(type(id)); }
    /** Mutates the supplied live server stack; the caller must mark its owning container changed. */
    public static boolean setPancakeLevel(ItemStack stack, String id, int level) {
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isSameThread()) throw new IllegalStateException("Pancake edits require the server thread");
        validateLevel(level); var type = type(id); var before = pancake(stack); var after = before.withLevel(type, level);
        if (after != before) CatTraitData.set(stack, after);
        return after.rawLevel(type) == (level == 0 ? 0 : type.clampLevel(level));
    }
    private static CatTraitProfile pancake(ItemStack stack) {
        if (stack == null || !stack.is(LaoWuMod.CAT_PANCAKE.get())) throw new IllegalArgumentException("Expected cat pancake");
        return CatTraitData.read(stack).orElse(CatTraitProfile.EMPTY);
    }
    public static List<Cat> nearbyAllies(Cat cat, double radius) {
        requireServer(cat);
        return cat.tickCount <= 0 ? List.of() : CatAccessoryApi.nearbyAllies(cat, radius);
    }
    public static List<LivingEntity> nearbyEnemies(Cat cat, double radius) {
        requireServer(cat);
        return cat.tickCount <= 0 ? List.of() : CatAccessoryApi.nearbyEnemies(cat, radius);
    }
    public static boolean friendly(Cat cat, LivingEntity other) { return CatAccessoryApi.friendly(cat, other); }
    public static boolean damage(Cat cat, LivingEntity target, double amount) {
        requireServer(cat);
        return cat.tickCount > 0 && CatTraitHooks.withoutEvents(() -> CatAccessoryApi.damage(cat, target, amount));
    }
    public static boolean heal(Cat cat, LivingEntity target, double amount) {
        requireServer(cat);
        return CatTraitHooks.withoutEvents(() -> CatAccessoryApi.heal(cat, target, amount));
    }
    public static boolean addEffect(Cat cat, LivingEntity target, String effect, int ticks, int amplifier, boolean allies) {
        requireServer(cat);
        return CatTraitHooks.withoutEvents(() -> CatAccessoryApi.addEffect(cat, target, effect, ticks, amplifier, allies));
    }
    public static void requireServer(Cat cat) { CatAccessoryApi.requireServer(cat); }
    private static CatTraitType type(String id) {
        if (id == null || id.length() > 128) throw new IllegalArgumentException("Invalid trait ID");
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null) throw new IllegalArgumentException("Invalid trait ID");
        return CatTraitRegistry.resolve(parsed, false);
    }
    private static void validateLevel(int level) {
        if (level < 0 || level > 7) throw new IllegalArgumentException("Trait level must be 0..7; zero removes it");
    }
    private CatTraitApi() {}
}
