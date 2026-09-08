package cn.laowu.mod.genetics;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Server-owned cat genome persistence plus the matching cat-pancake NBT bridge. */
public final class CatGenomeData {
    public static final String TAG = "LaoWuCatGenome";

    public static boolean has(Cat cat) {
        return read(cat).isPresent();
    }

    public static Optional<CatGenome> read(Cat cat) {
        CompoundTag data = cat.getPersistentData();
        if (!data.contains(TAG, Tag.TAG_COMPOUND)) return Optional.empty();
        return CatGenome.load(data.getCompound(TAG));
    }

    public static Optional<CatGenome> read(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(TAG, Tag.TAG_COMPOUND)) return Optional.empty();
        return CatGenome.load(root.getCompound(TAG));
    }

    public static CatGenome getOrFallback(Cat cat) {
        return read(cat).orElseGet(() -> CatGenome.uniform(variantId(cat)));
    }

    /** Initializes legacy/ordinary cats only when the genetics feature touches them. */
    public static CatGenome ensure(Cat cat) {
        CatGenome genome = getOrFallback(cat);
        if (!has(cat)) set(cat, genome);
        return genome;
    }

    public static void set(Cat cat, CatGenome genome) {
        cat.getPersistentData().put(TAG, genome.save());
    }

    public static void set(ItemStack stack, CatGenome genome) {
        stack.getOrCreateTag().put(TAG, genome.save());
    }

    public static CompoundTag serialized(Cat cat) {
        return getOrFallback(cat).save();
    }

    public static void setSerialized(Cat cat, CompoundTag serialized) {
        CatGenome.load(serialized).ifPresent(genome -> set(cat, genome));
    }

    public static void copyToStack(Cat cat, ItemStack stack) {
        read(cat).ifPresent(genome -> stack.getOrCreateTag().put(TAG, genome.save()));
    }

    public static void applyFromStack(ItemStack stack, Cat cat) {
        read(stack).ifPresent(genome -> set(cat, genome));
    }

    private static ResourceLocation variantId(Cat cat) {
        ResourceLocation id = BuiltInRegistries.CAT_VARIANT.getKey(cat.getVariant());
        return id == null ? CatVariant.RED.location() : id;
    }

    private CatGenomeData() {}
}
