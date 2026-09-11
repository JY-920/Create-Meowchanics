package cn.laowu.mod.recipe;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Strict top row channels, centre armour; solitary armour resets in either crafting grid. */
public final class KimiArmorDyeRecipe extends CustomRecipe {
    public KimiArmorDyeRecipe(CraftingBookCategory category) {
        super(category);
    }
    @Override public boolean matches(net.minecraft.world.item.crafting.CraftingInput input, Level level) { return !result(input).isEmpty(); }
    @Override public ItemStack assemble(net.minecraft.world.item.crafting.CraftingInput input, net.minecraft.core.HolderLookup.Provider access) { return result(input); }
    private ItemStack result(net.minecraft.world.item.crafting.CraftingInput input) {
        int width = input.width(), height = input.height();
        int left = 0, top = 0, originalWidth = width, originalHeight = height;
        if (input instanceof CraftingGridOrigin origin) {
            left = origin.laowu$left(); top = origin.laowu$top();
            originalWidth = origin.laowu$width(); originalHeight = origin.laowu$height();
        }
        if (originalWidth < 1 || originalHeight < 1) return ItemStack.EMPTY;
        int[] cells = new int[originalWidth * originalHeight];
        java.util.Arrays.fill(cells, -2);
        ItemStack armor = ItemStack.EMPTY;
        for (int i = 0; i < width * height; i++) {
            ItemStack stack = input.getItem(i);
            int slot = (i / width + top) * originalWidth + i % width + left;
            if (slot < 0 || slot >= cells.length) return ItemStack.EMPTY;
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof KimiArmorItem) { cells[slot] = -1; armor = stack; }
            else if (stack.getItem() instanceof DyeItem dye) cells[slot] = dye.getDyeColor().getId();
            else cells[slot] = -3;
        }
        if (armor.isEmpty()) return ItemStack.EMPTY;
        boolean helmet = ((KimiArmorItem) armor.getItem()).getType() == ArmorItem.Type.HELMET;
        int code = KimiDyeCrafting.result(cells, originalWidth, originalHeight, KimiArmorDye.read(armor), helmet);
        if (code < 0) return ItemStack.EMPTY;
        ItemStack result = armor.copyWithCount(1);
        KimiArmorDye.write(result, code);
        return result;
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 1; }
    @Override public RecipeSerializer<?> getSerializer() { return LaoWuMod.KIMI_ARMOR_DYE_SERIALIZER.get(); }
}
