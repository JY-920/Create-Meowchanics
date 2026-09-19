package cn.laowu.mod.accessory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

/** Keeps arbitrary third-party item data intact; never uses durability for accessory charge. */
public final class CatAccessoryStackData {
    public static final String KEY = "LaoWuAccessory";
    public static CompoundTag read(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(KEY).copy();
    }
    public static void write(ItemStack stack, CompoundTag data) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(KEY, data.copy()));
    }
    public static boolean ensureIdentity(ItemStack stack) {
        CompoundTag data = read(stack);
        if (data.hasUUID("Instance")) return false;
        data.putUUID("Instance", java.util.UUID.randomUUID());
        write(stack, data); return true;
    }
    private CatAccessoryStackData() {}
}
