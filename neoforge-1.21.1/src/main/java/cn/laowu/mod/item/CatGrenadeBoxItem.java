package cn.laowu.mod.item;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/** Inventory-click container dedicated to as many as 1024 cat grenades. */
public final class CatGrenadeBoxItem extends Item {
    public static final int CAPACITY = 1024;
    private static final String COUNT_TAG = "CatGrenades";

    public CatGrenadeBoxItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int count(ItemStack box) {
        CompoundTag tag = ItemCustomData.copy(box);
        return Math.max(0, Math.min(CAPACITY, tag.getInt(COUNT_TAG)));
    }

    private static int insert(ItemStack box, ItemStack grenades) {
        if (grenades.isEmpty() || !grenades.is(LaoWuMod.CAT_GRENADE.get())) return 0;
        int inserted = Math.min(grenades.getCount(), CAPACITY - count(box));
        if (inserted <= 0) return 0;
        ItemCustomData.update(box, tag -> tag.putInt(COUNT_TAG, count(box) + inserted));
        grenades.shrink(inserted);
        return inserted;
    }

    private static ItemStack extractStack(ItemStack box) {
        int stored = count(box);
        if (stored <= 0) return ItemStack.EMPTY;
        int extracted = Math.min(stored, LaoWuMod.CAT_GRENADE.get().getDefaultInstance().getMaxStackSize());
        setCount(box, stored - extracted);
        return new ItemStack(LaoWuMod.CAT_GRENADE.get(), extracted);
    }

    private static void setCount(ItemStack box, int count) {
        if (count <= 0) {
            ItemCustomData.update(box, tag -> tag.remove(COUNT_TAG));
            return;
        }
        ItemCustomData.update(box, tag -> tag.putInt(COUNT_TAG, Math.min(CAPACITY, count)));
    }

    private static NonNullList<ItemStack> contents(ItemStack box) {
        NonNullList<ItemStack> result = NonNullList.create();
        int remaining = count(box);
        int stackLimit = LaoWuMod.CAT_GRENADE.get().getDefaultInstance().getMaxStackSize();
        while (remaining > 0) {
            int amount = Math.min(remaining, stackLimit);
            result.add(new ItemStack(LaoWuMod.CAT_GRENADE.get(), amount));
            remaining -= amount;
        }
        return result;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        NonNullList<ItemStack> grenades = contents(stack);
        return Optional.of(new BundleTooltip(new BundleContents(grenades)));
    }

    @Override
    public void onDestroyed(ItemEntity entity) {
        ItemUtils.onContainerDestroyed(entity, contents(entity.getItem()));
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack box, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack target = slot.getItem();
        if (target.isEmpty()) {
            ItemStack extracted = extractStack(box);
            if (!extracted.isEmpty()) slot.safeInsert(extracted);
        } else {
            insert(box, target);
        }
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack box, ItemStack carried, Slot slot,
                                            ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY) return false;
        if (carried.isEmpty()) {
            ItemStack extracted = extractStack(box);
            if (!extracted.isEmpty()) access.set(extracted);
        } else {
            insert(box, carried);
        }
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return count(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.min(13, 1 + 12 * count(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x9A673E;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.laowu.cat_box.count", count(stack), CAPACITY)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.laowu.cat_box.control")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
