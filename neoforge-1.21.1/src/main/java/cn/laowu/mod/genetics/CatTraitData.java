package cn.laowu.mod.genetics;

import cn.laowu.mod.item.ItemCustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** Server-owned trait persistence, client sync target and cat-pancake bridge. */
public final class CatTraitData {
    public static final String TAG = "LaoWuCatTraits";
    private static final Map<Cat, CatTraitProfile> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static Optional<CatTraitProfile> read(Cat cat) {
        CatTraitProfile cached = CACHE.get(cat);
        if (cached != null) return Optional.of(cached);
        CompoundTag data = cat.getPersistentData();
        if (!data.contains(TAG, Tag.TAG_COMPOUND)) return Optional.empty();
        Optional<CatTraitProfile> loaded = CatTraitProfile.load(data.getCompound(TAG));
        loaded.ifPresent(profile -> CACHE.put(cat, profile));
        return loaded;
    }

    public static Optional<CatTraitProfile> read(ItemStack stack) {
        CompoundTag root = ItemCustomData.copy(stack);
        if (!root.contains(TAG, Tag.TAG_COMPOUND)) return Optional.empty();
        return CatTraitProfile.load(root.getCompound(TAG));
    }

    public static CatTraitProfile ensure(Cat cat) {
        Optional<CatTraitProfile> cached = read(cat);
        if (cached.isPresent()) return cached.get();
        CatTraitProfile generated = CatTraitProfile.founder(cat.getRandom());
        set(cat, generated);
        return generated;
    }

    public static CatTraitProfile ensure(ItemStack stack, RandomSource random) {
        Optional<CatTraitProfile> existing = read(stack);
        if (existing.isPresent()) return existing.get();
        CatTraitProfile generated = CatTraitProfile.founder(random);
        set(stack, generated);
        return generated;
    }

    public static void setInjected(ItemStack stack, RandomSource random) {
        set(stack, CatTraitProfile.injected(random));
    }

    public static void set(Cat cat, CatTraitProfile profile) {
        cat.getPersistentData().put(TAG, profile.save());
        CACHE.put(cat, profile);
        if (profile.has(CatTrait.LOLI) && cat.getAge() >= 0) {
            cat.setAge(-24_000);
        }
        // Appearance traits can alter the per-entity physical scale. Refresh
        // after updating the cache so Forge's Size event reads the new profile.
        cat.refreshDimensions();
        if (!cat.level().isClientSide) {
            CatAttributeData.read(cat).ifPresent(attributes ->
                    CatAttributeEffects.refresh(cat, attributes, profile));
        }
    }

    public static void set(ItemStack stack, CatTraitProfile profile) {
        ItemCustomData.update(stack, tag -> tag.put(TAG, profile.save()));
    }

    public static CompoundTag serialized(Cat cat) {
        return ensure(cat).save();
    }

    public static void setSerialized(Cat cat, CompoundTag serialized) {
        CatTraitProfile.load(serialized).ifPresent(profile -> set(cat, profile));
    }

    public static void copyToStack(Cat cat, ItemStack stack) {
        ItemCustomData.update(stack, tag -> tag.put(TAG, ensure(cat).save()));
    }

    public static void applyFromStack(ItemStack stack, Cat cat) {
        read(stack).ifPresent(profile -> set(cat, profile));
    }

    private CatTraitData() {}
}
