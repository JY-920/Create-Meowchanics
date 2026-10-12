package cn.laowu.mod.recipe;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.PheromoneCatFoodItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Copies a renamed name tag without consuming it or caching the crafting preview. */
public final class PheromoneCatFoodCraftingRecipe extends CustomRecipe {
    public PheromoneCatFoodCraftingRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    private ItemStack result(CraftingContainer input) {
        boolean food = false;
        ItemStack nameTag = ItemStack.EMPTY;
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (stack.is(LaoWuMod.CAT_FOOD.get()) && !food) food = true;
            else if (stack.is(Items.NAME_TAG) && nameTag.isEmpty()) nameTag = stack;
            else return ItemStack.EMPTY;
        }
        if (!food || nameTag.isEmpty() || !nameTag.hasCustomHoverName()) return ItemStack.EMPTY;
        String name = nameTag.getHoverName().getString().trim();
        return name.isEmpty() ? ItemStack.EMPTY : PheromoneCatFoodItem.createForOwner(name);
    }

    @Override public boolean matches(CraftingContainer input, Level level) {
        return !result(input).isEmpty();
    }

    @Override public ItemStack assemble(CraftingContainer input, RegistryAccess access) {
        return result(input);
    }

    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.getContainerSize(), ItemStack.EMPTY);
        if (!result(input).isEmpty()) {
            for (int slot = 0; slot < remaining.size(); slot++) {
                if (input.getItem(slot).is(Items.NAME_TAG)) remaining.set(slot, input.getItem(slot).copyWithCount(1));
            }
        }
        return remaining;
    }

    @Override public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override public RecipeSerializer<?> getSerializer() {
        return LaoWuMod.PHEROMONE_CAT_FOOD_CRAFTING_SERIALIZER.get();
    }
}
