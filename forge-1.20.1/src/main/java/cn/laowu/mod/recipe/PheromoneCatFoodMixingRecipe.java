package cn.laowu.mod.recipe;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.PheromoneCatFoodItem;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.List;

/** Copies the renamed Name Tag's player name into the mixed food's NBT. */
public final class PheromoneCatFoodMixingRecipe extends MixingRecipe {
    public PheromoneCatFoodMixingRecipe(
            ProcessingRecipeBuilder.ProcessingRecipeParams params) {
        super(params);
    }

    @Override
    public List<ItemStack> rollResults() {
        return NamedPlayerNameTagIngredient.consumeMatchedOwner()
                .map(owner -> List.of(PheromoneCatFoodItem.createForOwner(owner)))
                // Recipe viewers do not run against a real basin. Preserve the
                // declared unbound output for their static recipe display.
                .orElseGet(super::rollResults);
    }

    /** Treat the renamed Name Tag as a reusable basin catalyst. */
    @Override
    public NonNullList<ItemStack> getRemainingItems(Container container) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(1, ItemStack.EMPTY);
        NamedPlayerNameTagIngredient.consumeMatchedNameTag()
                .ifPresent(nameTag -> remaining.set(0, nameTag));
        return remaining;
    }

    /** Keep this subclass when recipes are synchronized to clients. */
    @Override
    public RecipeSerializer<?> getSerializer() {
        return LaoWuMod.PHEROMONE_CAT_FOOD_MIXING_SERIALIZER.get();
    }
}
