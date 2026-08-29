package cn.laowu.mod.genetics;

import cn.laowu.mod.item.ItemCustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Server-owned attribute persistence plus the cat-pancake NBT bridge. */
public final class CatAttributeData {
    public static final String TAG = "LaoWuCatAttributes";

    public static boolean has(Cat cat) {
        return read(cat).isPresent();
    }

    public static Optional<CatAttributeProfile> read(Cat cat) {
        CompoundTag data = cat.getPersistentData();
        if (!data.contains(TAG, Tag.TAG_COMPOUND)) return Optional.empty();
        return CatAttributeProfile.load(data.getCompound(TAG));
    }

    public static Optional<CatAttributeProfile> read(ItemStack stack) {
        CompoundTag root = ItemCustomData.copy(stack);
        if (!root.contains(TAG, Tag.TAG_COMPOUND)) return Optional.empty();
        return CatAttributeProfile.load(root.getCompound(TAG));
    }

    /** Initializes a profile exactly once; callers must invoke this on the server. */
    public static CatAttributeProfile ensure(Cat cat) {
        CompoundTag data = cat.getPersistentData();
        CompoundTag stored = data.contains(TAG, Tag.TAG_COMPOUND)
                ? data.getCompound(TAG) : null;
        Optional<CatAttributeProfile> existing = stored == null
                ? Optional.empty() : CatAttributeProfile.load(stored);
        CatAttributeProfile profile = existing.orElseGet(
                () -> CatAttributeProfile.founder(cat.getRandom()));
        if (existing.isEmpty() || !CatAttributeProfile.isCurrentVersion(stored)) {
            set(cat, profile);
        }
        return profile;
    }

    /** Initializes a profile on a cat-pancake stack exactly once. */
    public static CatAttributeProfile ensure(ItemStack stack, RandomSource random) {
        Optional<CatAttributeProfile> existing = read(stack);
        if (existing.isPresent()) return existing.get();
        CatAttributeProfile generated = CatAttributeProfile.founder(random);
        set(stack, generated);
        return generated;
    }

    public static void set(Cat cat, CatAttributeProfile profile) {
        cat.getPersistentData().put(TAG, profile.save());
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
