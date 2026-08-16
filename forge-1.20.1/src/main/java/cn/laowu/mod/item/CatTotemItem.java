package cn.laowu.mod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** A reusable totem shell whose durability represents inserted vanilla totems. */
public final class CatTotemItem extends Item {
    public static final int CAPACITY = 9;
    private static final String CHARGES_TAG = "LaoWuTotemCharges";
    private static final int BAR_COLOUR = 0xFFD84A;

    public CatTotemItem(Properties properties) {
        // Charges are deliberately stored separately from vanilla Damage so
        // anvils and Mending cannot manufacture inserted Totems of Undying.
        super(properties.stacksTo(1));
    }

    /** New and recipe-created shells begin empty instead of at vanilla full durability. */
    @Override
    public ItemStack getDefaultInstance() {
        return emptyStack(this);
    }

    public static ItemStack emptyStack(Item item) {
        return new ItemStack(item);
    }

    public static int charges(ItemStack stack) {
        return stack.hasTag() ? Math.max(0,
                Math.min(CAPACITY, stack.getTag().getInt(CHARGES_TAG))) : 0;
    }

    public static boolean canLoad(ItemStack stack) {
        return charges(stack) < CAPACITY;
    }

    public static void addCharge(ItemStack stack) {
        if (canLoad(stack)) {
            stack.getOrCreateTag().putInt(CHARGES_TAG, charges(stack) + 1);
        }
    }

    public static boolean consumeCharge(ItemStack stack) {
        int remaining = charges(stack);
        if (remaining <= 0) return false;
        if (remaining == 1) {
            if (stack.hasTag()) stack.getTag().remove(CHARGES_TAG);
        } else {
            stack.getOrCreateTag().putInt(CHARGES_TAG, remaining - 1);
        }
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * charges(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOUR;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.laowu.cat_totem.charges",
                charges(stack), CAPACITY).withStyle(ChatFormatting.GRAY));
    }
}
