package cn.laowu.mod.create;

import cn.laowu.mod.CatClothesData;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.item.CatPancakeItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Bounded world-preview payload; never copies a captured entity or its inventory. */
public final class AdoptionPancakeDisplay {
    public static final int MAX_DISPLAYS = 9;
    private static final String TAG = "InputDisplays";
    private static final String LEGACY_TAG = "InputDisplay";
    private static final CatTrait[] VISUAL_TRAITS = {CatTrait.BIG_CHONKY_CAT, CatTrait.LOLI,
            CatTrait.RAINBOW_CAT, CatTrait.NEKOMATA, CatTrait.HIM, CatTrait.ISAAC, CatTrait.PUSS_IN_BOOTS};

    public static void write(CompoundTag update, IItemHandler inventory, int inputCount) {
        ListTag displays = new ListTag();
        int slots = Math.min(MAX_DISPLAYS, Math.min(inputCount, inventory.getSlots()));
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof CatPancakeItem)) continue;
            CompoundTag appearance = new CompoundTag();
            appearance.putByte("Slot", (byte) slot);
            appearance.putString(CatPancakeItem.CAT_TEXTURE_TAG, CatPancakeItem.texture(stack).toString());
            appearance.putBoolean(CatPancakeItem.BABY_TAG, CatPancakeItem.isBaby(stack));
            appearance.putBoolean("LaoWuTamedPancake", CatPancakeItem.isTamed(stack));
            appearance.putString(CatClothesData.OUTFIT_TAG, CatPancakeItem.getOutfit(stack).id());
            CatGenomeData.read(stack).ifPresent(genome -> appearance.put(CatGenomeData.TAG, genome.save()));
            var traits = CatTraitData.read(stack).orElse(CatTraitProfile.EMPTY);
            var visual = CatTraitProfile.EMPTY;
            for (CatTrait trait : VISUAL_TRAITS) visual = visual.withLevel(trait, traits.level(trait));
            appearance.put(CatTraitData.TAG, visual.save());
            displays.add(appearance);
        }
        update.remove(LEGACY_TAG);
        // An explicit empty list also wins over any legacy preview when decoding.
        update.put(TAG, displays);
    }

    /** Fixed input-slot positions: removing one cat never moves its neighbours. */
    public static List<ItemStack> readAll(CompoundTag update) {
        List<ItemStack> displays = new ArrayList<>(Collections.nCopies(MAX_DISPLAYS, ItemStack.EMPTY));
        if (update.contains(TAG, Tag.TAG_LIST)) {
            ListTag entries = update.getList(TAG, Tag.TAG_COMPOUND);
            for (int index = 0; index < Math.min(entries.size(), MAX_DISPLAYS); index++) {
                CompoundTag appearance = entries.getCompound(index);
                int slot = appearance.getInt("Slot");
                if (slot >= 0 && slot < MAX_DISPLAYS && displays.get(slot).isEmpty())
                    displays.set(slot, displayStack(appearance));
            }
        } else if (update.contains(LEGACY_TAG, Tag.TAG_COMPOUND)) {
            // The original single preview had no slot information and was centred.
            displays.set(4, displayStack(update.getCompound(LEGACY_TAG)));
        }
        return List.copyOf(displays);
    }

    /** Kept for callers expecting the first occupied input preview. */
    public static ItemStack read(CompoundTag update) {
        return first(readAll(update));
    }

    public static ItemStack first(List<ItemStack> displays) {
        for (ItemStack display : displays) if (!display.isEmpty()) return display;
        return ItemStack.EMPTY;
    }

    private static ItemStack displayStack(CompoundTag appearance) {
        ItemStack display = new ItemStack(LaoWuMod.CAT_PANCAKE.get());
        CompoundTag data = appearance.copy();
        data.remove("Slot");
        cn.laowu.mod.item.ItemCustomData.set(display, data);
        return display;
    }

    private AdoptionPancakeDisplay() {}
}
