package cn.laowu.mod.item;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/** A cat-only bundle. Every stored pancake costs one point and capacity is 128. */
public final class CatPouchItem extends Item {
    public static final int CAPACITY = 128;
    private static final String ITEMS_TAG = "Items";

    public CatPouchItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int count(ItemStack pouch) {
        CompoundTag tag = pouch.getTag();
        return tag == null ? 0 : Math.min(CAPACITY, tag.getList(ITEMS_TAG, Tag.TAG_COMPOUND).size());
    }

    public static boolean insertOne(ItemStack pouch, ItemStack cat) {
        if (!cat.is(LaoWuMod.CAT_PANCAKE.get()) || cat.isEmpty() || count(pouch) >= CAPACITY) return false;
        ListTag stored = pouch.getOrCreateTag().getList(ITEMS_TAG, Tag.TAG_COMPOUND);
        stored.add(cat.copyWithCount(1).save(new CompoundTag()));
        pouch.getOrCreateTag().put(ITEMS_TAG, stored);
        cat.shrink(1);
        return true;
    }

    public static ItemStack extractOne(ItemStack pouch) {
        CompoundTag tag = pouch.getTag();
        if (tag == null) return ItemStack.EMPTY;
        ListTag stored = tag.getList(ITEMS_TAG, Tag.TAG_COMPOUND);
        if (stored.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = ItemStack.of(stored.getCompound(stored.size() - 1));
        stored.remove(stored.size() - 1);
        if (stored.isEmpty()) tag.remove(ITEMS_TAG); else tag.put(ITEMS_TAG, stored);
        return result;
    }

    public static ItemStack peek(ItemStack pouch) {
        CompoundTag tag = pouch.getTag();
        if (tag == null) return ItemStack.EMPTY;
        ListTag stored = tag.getList(ITEMS_TAG, Tag.TAG_COMPOUND);
        return stored.isEmpty() ? ItemStack.EMPTY : ItemStack.of(stored.getCompound(stored.size() - 1));
    }

    private static NonNullList<ItemStack> contents(ItemStack pouch) {
        NonNullList<ItemStack> result = NonNullList.create();
        CompoundTag tag = pouch.getTag();
        if (tag == null) return result;
        ListTag stored = tag.getList(ITEMS_TAG, Tag.TAG_COMPOUND);
        for (int i = stored.size() - 1; i >= 0; i--) result.add(ItemStack.of(stored.getCompound(i)));
        return result;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        NonNullList<ItemStack> cats = contents(stack);
        int vanillaWeightScale = Math.min(64, (cats.size() + 1) / 2);
        return Optional.of(new BundleTooltip(cats, vanillaWeightScale));
    }

    @Override
    public void onDestroyed(ItemEntity entity) {
        ItemUtils.onContainerDestroyed(entity, contents(entity.getItem()).stream());
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack pouch, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack target = slot.getItem();
        if (target.isEmpty()) {
            ItemStack extracted = extractOne(pouch);
            if (!extracted.isEmpty()) slot.safeInsert(extracted);
        } else if (target.is(LaoWuMod.CAT_PANCAKE.get()) && count(pouch) < CAPACITY) {
            insertOne(pouch, target);
        }
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack pouch, ItemStack carried, Slot slot,
                                            ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY) return false;
        if (carried.isEmpty()) {
            ItemStack extracted = extractOne(pouch);
            if (!extracted.isEmpty()) access.set(extracted);
        } else if (carried.is(LaoWuMod.CAT_PANCAKE.get()) && count(pouch) < CAPACITY) {
            insertOne(pouch, carried);
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
        return 0xE5A648;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.laowu.cat_pouch.count", count(stack), CAPACITY)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.laowu.cat_pouch.control").withStyle(ChatFormatting.DARK_GRAY));
    }
}
