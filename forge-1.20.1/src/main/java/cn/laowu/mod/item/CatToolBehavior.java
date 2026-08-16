package cn.laowu.mod.item;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Shared hiss-value semantics for cat tools and the cat armor set. */
public final class CatToolBehavior {
    public static final int REPAIR_FLUID_COST = 250;
    public static final int REPAIR_AMOUNT = 500;
    public static final int BAR_COLOR = 0xD9DEE3;
    public static final int EMPOWERED_HISS_MULTIPLIER = 3;
    private static final String EMPOWERED_TAG = "LaoWuEmpowered";
    private static final String SWORD_HITS_TAG = "LaoWuEmpoweredSwordHits";

    /** The five tools that can be toggled with Alt; armour keeps normal hiss use. */
    public static boolean isCatHandTool(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(LaoWuMod.CAT_SWORD.get())
                || stack.is(LaoWuMod.CAT_PICKAXE.get())
                || stack.is(LaoWuMod.CAT_AXE.get())
                || stack.is(LaoWuMod.CAT_SHOVEL.get())
                || stack.is(LaoWuMod.CAT_HOE.get()));
    }

    public static boolean isCatTool(ItemStack stack) {
        return isCatHandTool(stack) || (!stack.isEmpty() && (stack.is(LaoWuMod.CAT_HELMET.get())
                || stack.is(LaoWuMod.CAT_CHESTPLATE.get())
                || stack.is(LaoWuMod.CAT_LEGGINGS.get())
                || stack.is(LaoWuMod.CAT_BOOTS.get())));
    }

    /** Whether the player selected the enhanced layer, including while empty. */
    public static boolean isEmpowermentMarked(ItemStack stack) {
        return isCatHandTool(stack) && stack.hasTag() && stack.getTag().getBoolean(EMPOWERED_TAG);
    }

    /** Active enhancement additionally requires at least one point of hiss. */
    public static boolean isEmpowered(ItemStack stack) {
        return isEmpowermentMarked(stack) && !isExhausted(stack);
    }

    public static void setEmpowered(ItemStack stack, boolean empowered) {
        if (!isCatHandTool(stack)) return;
        if (empowered) {
            stack.getOrCreateTag().putBoolean(EMPOWERED_TAG, true);
        } else if (stack.hasTag()) {
            stack.getTag().remove(EMPOWERED_TAG);
            stack.getTag().remove(SWORD_HITS_TAG);
        }
    }

    /** Returns the new selected state. Enabling an empty tool is rejected. */
    public static boolean toggleEmpowered(ItemStack stack) {
        boolean enabled = !isEmpowermentMarked(stack);
        if (enabled && isExhausted(stack)) return false;
        setEmpowered(stack, enabled);
        return enabled;
    }

    /** Records one successful enhanced sword hit and reports each third hit. */
    public static boolean recordSwordHit(ItemStack stack) {
        if (!stack.is(LaoWuMod.CAT_SWORD.get()) || !isEmpowered(stack)) return false;
        int hits = stack.getOrCreateTag().getInt(SWORD_HITS_TAG) + 1;
        if (hits >= 3) {
            stack.getOrCreateTag().putInt(SWORD_HITS_TAG, 0);
            return true;
        }
        stack.getOrCreateTag().putInt(SWORD_HITS_TAG, hits);
        return false;
    }

    public static int hissCost(ItemStack stack, int normalCost) {
        if (normalCost <= 0) return normalCost;
        return isEmpowered(stack) ? normalCost * EMPOWERED_HISS_MULTIPLIER : normalCost;
    }

    public static boolean isExhausted(ItemStack stack) {
        return isCatTool(stack) && stack.getDamageValue() >= stack.getMaxDamage();
    }

    public static boolean canRepair(ItemStack stack) {
        return isCatTool(stack) && stack.getDamageValue() > 0;
    }

    public static int remaining(ItemStack stack) {
        return Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
    }

    public static int repair(ItemStack stack, int amount) {
        if (!canRepair(stack) || amount <= 0) return 0;
        int repaired = Math.min(amount, stack.getDamageValue());
        stack.setDamageValue(stack.getDamageValue() - repaired);
        return repaired;
    }

    public static int barWidth(ItemStack stack) {
        if (stack.getMaxDamage() <= 0) return 0;
        return Math.round(13.0F * remaining(stack) / stack.getMaxDamage());
    }

    /** Five synchronized variants used by JEI to demonstrate a 250-point repair. */
    public static List<ItemStack> jeiStacks(int damage) {
        List<ItemStack> stacks = new ArrayList<>(9);
        addJeiStack(stacks, LaoWuMod.CAT_SWORD.get().getDefaultInstance(), damage);
        addJeiStack(stacks, LaoWuMod.CAT_PICKAXE.get().getDefaultInstance(), damage);
        addJeiStack(stacks, LaoWuMod.CAT_AXE.get().getDefaultInstance(), damage);
        addJeiStack(stacks, LaoWuMod.CAT_SHOVEL.get().getDefaultInstance(), damage);
        addJeiStack(stacks, LaoWuMod.CAT_HOE.get().getDefaultInstance(), damage);
        addJeiStack(stacks, LaoWuMod.CAT_HELMET.get().getDefaultInstance(), damage);
        addJeiStack(stacks, LaoWuMod.CAT_CHESTPLATE.get().getDefaultInstance(), damage);
        addJeiStack(stacks, LaoWuMod.CAT_LEGGINGS.get().getDefaultInstance(), damage);
        addJeiStack(stacks, LaoWuMod.CAT_BOOTS.get().getDefaultInstance(), damage);
        return stacks;
    }

    /** A focused JEI lookup must keep the input and output on the same tool. */
    public static List<ItemStack> jeiStacks(Item focusedTool, int damage) {
        if (focusedTool == null) return jeiStacks(damage);
        ItemStack stack = focusedTool.getDefaultInstance();
        if (!isCatTool(stack)) return jeiStacks(damage);
        List<ItemStack> stacks = new ArrayList<>(1);
        addJeiStack(stacks, stack, damage);
        return stacks;
    }

    /** Shows the exact same focused item after one 500-point Hiss recharge. */
    public static List<ItemStack> jeiRepairedStacks(Item focusedItem, int inputDamage) {
        List<ItemStack> inputs = jeiStacks(focusedItem, inputDamage);
        List<ItemStack> outputs = new ArrayList<>(inputs.size());
        for (ItemStack input : inputs) {
            ItemStack output = input.copy();
            output.setDamageValue(Math.max(0, output.getDamageValue() - REPAIR_AMOUNT));
            outputs.add(output);
        }
        return outputs;
    }

    private static void addJeiStack(List<ItemStack> stacks, ItemStack stack, int damage) {
        stack.setDamageValue(Math.min(stack.getMaxDamage(), Math.max(0, damage)));
        stacks.add(stack);
    }

    private CatToolBehavior() {
    }
}
