package cn.laowu.mod.recipe;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.PheromoneCatFoodItem;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.List;

/** Copies the renamed Name Tag's player name into the mixed food's NBT. */
public final class PheromoneCatFoodMixingRecipe extends MixingRecipe {
    public PheromoneCatFoodMixingRecipe(
            ProcessingRecipeParams params) {
        super(params);
    }

    @Override
    public List<ItemStack> rollResults(RandomSource random) {
        return NamedPlayerNameTagIngredient.consumeMatchedOwner()
                .map(owner -> List.of(PheromoneCatFoodItem.createForOwner(owner)))
                // Recipe viewers do not run against a real basin. Preserve the
                // declared unbound output for their static recipe display.
                .orElseGet(() -> super.rollResults(random));
    }

    /** Keep this subclass when recipes are synchronized to clients. */
    @Override
    public RecipeSerializer<?> getSerializer() {
        return LaoWuMod.PHEROMONE_CAT_FOOD_MIXING_SERIALIZER.get();
    }
}
