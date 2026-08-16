package cn.laowu.mod.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/** Small bridge for the mod's legacy per-stack data on Minecraft 1.21 components. */
public final class ItemCustomData {
    private static final RegistryAccess.Frozen BUILTIN_REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    public static CompoundTag copy(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static boolean has(ItemStack stack) {
        return !stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).isEmpty();
    }

    public static void update(ItemStack stack, Consumer<CompoundTag> operation) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, operation);
    }

    public static void set(ItemStack stack, CompoundTag tag) {
        if (tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static CompoundTag saveStack(ItemStack stack) {
        return (CompoundTag) stack.save(BUILTIN_REGISTRIES, new CompoundTag());
    }

    public static ItemStack loadStack(CompoundTag tag) {
        return ItemStack.parseOptional(BUILTIN_REGISTRIES, tag);
    }

    private ItemCustomData() {
    }
}
