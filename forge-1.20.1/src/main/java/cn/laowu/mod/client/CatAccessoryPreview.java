package cn.laowu.mod.client;

import cn.laowu.mod.CatClothesData;
import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.CatPoseData;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.item.CatPancakeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;

/** One cached, non-world entity so all pancake panels can reuse live attribute calculations. */
public final class CatAccessoryPreview {
    private static ItemStack previous = ItemStack.EMPTY;
    private static Cat preview;
    private static int nextPreviewId = -1_000_000;
    public static int nextEntityId() { return nextPreviewId--; }
    public static Cat forStack(ItemStack stack) {
        var level = Minecraft.getInstance().level;
        if (level == null || stack.isEmpty() || !(stack.getItem() instanceof CatPancakeItem)) return null;
        if (preview != null && preview.level() == level && ItemStack.isSameItemSameTags(stack, previous))
            return preview;
        Cat cat = EntityType.CAT.create(level);
        if (cat == null) return null;
        // Entity.equals compares runtime IDs; keep UI previews out of real client/server cat caches.
        cat.setId(nextEntityId());
        CompoundTag root = stack.getTag();
        if (root != null && root.contains(CatPancakeItem.CAT_DATA_TAG))
            cat.load(root.getCompound(CatPancakeItem.CAT_DATA_TAG).copy());
        CatAttributeData.read(stack).ifPresent(profile -> CatAttributeData.set(cat, profile));
        CatTraitData.read(stack).ifPresent(profile -> CatTraitData.set(cat, profile));
        CatOutfitType outfit = CatPancakeItem.getOutfit(stack);
        if (outfit != CatOutfitType.NONE) {
            cat.getPersistentData().putBoolean(CatClothesData.EQUIPPED_TAG, true);
            cat.getPersistentData().putString(CatClothesData.OUTFIT_TAG, outfit.id());
        }
        CatPoseData.setPose(cat, CatPoseData.NORMAL);
        previous = stack.copy();
        preview = cat;
        return cat;
    }
    public static void reset() { previous = ItemStack.EMPTY; preview = null; }
    private CatAccessoryPreview() {}
}
