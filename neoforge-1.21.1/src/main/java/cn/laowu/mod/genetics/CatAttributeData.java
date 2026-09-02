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

/** Server-owned attribute persistence plus the cat-pancake NBT bridge. */
public final class CatAttributeData {
    public static final String TAG = "LaoWuCatAttributes";
    private static final Map<Cat, CatAttributeProfile> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static boolean has(Cat cat) {
        return read(cat).isPresent();
    }

    public static Optional<CatAttributeProfile> read(Cat cat) {
        CatAttributeProfile cached = CACHE.get(cat);
        if (cached != null) return Optional.of(cached);
        CompoundTag data = cat.getPersistentData();
        if (!data.contains(TAG, Tag.TAG_COMPOUND)) return Optional.empty();
        Optional<CatAttributeProfile> loaded = CatAttributeProfile.load(data.getCompound(TAG));
        loaded.ifPresent(profile -> CACHE.put(cat, profile));
        return loaded;
    }

    public static Optional<CatAttributeProfile> read(ItemStack stack) {
        CompoundTag root = ItemCustomData.copy(stack);
        if (!root.contains(TAG, Tag.TAG_COMPOUND)) return Optional.empty();
        return CatAttributeProfile.load(root.getCompound(TAG));
    }

    /** Initializes a profile exactly once; callers must invoke this on the server. */
    public static CatAttributeProfile ensure(Cat cat) {
        CatAttributeProfile cached = CACHE.get(cat);
        if (cached != null) return cached;
        CompoundTag data = cat.getPersistentData();
        CompoundTag stored = data.contains(TAG, Tag.TAG_COMPOUND)
                ? data.getCompound(TAG) : null;
        Optional<CatAttributeProfile> existing = stored == null
                ? Optional.empty() : CatAttributeProfile.load(stored);
        CatAttributeProfile profile = existing.orElseGet(
                () -> CatAttributeProfile.founder(cat.getRandom()));
        if (existing.isEmpty() || !CatAttributeProfile.isCurrentVersion(stored)) {
            set(cat, profile);
        } else {
            CACHE.put(cat, profile);
        }
        return profile;
    }

    /** Initializes item-form cat-pancake attributes exactly once on the server. */
    public static CatAttributeProfile ensure(ItemStack stack, RandomSource random) {
        CompoundTag root = ItemCustomData.copy(stack);
        CompoundTag stored = root.contains(TAG, Tag.TAG_COMPOUND)
                ? root.getCompound(TAG) : null;
        Optional<CatAttributeProfile> existing = stored == null
                ? Optional.empty() : CatAttributeProfile.load(stored);
        CatAttributeProfile profile = existing.orElseGet(
                () -> CatAttributeProfile.founder(random));
        if (existing.isEmpty() || !CatAttributeProfile.isCurrentVersion(stored)) {
            set(stack, profile);
        }
        return profile;
    }

    public static void set(Cat cat, CatAttributeProfile profile) {
        cat.getPersistentData().put(TAG, profile.save());
        CACHE.put(cat, profile);
        if (!cat.level().isClientSide) {
            CatAttributeEffects.refresh(cat, profile,
                    CatTraitData.read(cat).orElse(CatTraitProfile.EMPTY));
        }
    }

    public static void set(ItemStack stack, CatAttributeProfile profile) {
        ItemCustomData.update(stack, tag -> tag.put(TAG, profile.save()));
    }

    public static CompoundTag serialized(Cat cat) {
        return ensure(cat).save();
    }

    public static void setSerialized(Cat cat, CompoundTag serialized) {
        CatAttributeProfile.load(serialized).ifPresent(profile -> set(cat, profile));
    }

    public static void copyToStack(Cat cat, ItemStack stack) {
        ItemCustomData.update(stack, tag -> tag.put(TAG, ensure(cat).save()));
    }

    public static void applyFromStack(ItemStack stack, Cat cat) {
        read(stack).ifPresent(profile -> set(cat, profile));
    }

    private CatAttributeData() {}
}
