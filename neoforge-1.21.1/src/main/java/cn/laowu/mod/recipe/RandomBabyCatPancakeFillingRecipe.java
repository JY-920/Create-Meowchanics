package cn.laowu.mod.recipe;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.CatPancakeItem;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** A normal Create filling recipe whose produced kitten skin is rolled at processing time. */
public final class RandomBabyCatPancakeFillingRecipe extends FillingRecipe {
    public RandomBabyCatPancakeFillingRecipe(
            ProcessingRecipeParams params) {
        super(params);
    }

    @Override
    public List<ItemStack> rollResults(RandomSource random) {
        List<ResourceLocation> variants = BuiltInRegistries.CAT_VARIANT.keySet()
                .stream().toList();
        ResourceLocation chosen = variants.isEmpty()
                ? CatPancakeItem.DEFAULT_VARIANT
                : variants.get(random.nextInt(variants.size()));
        return List.of(CatPancakeItem.babyVariantStack(chosen));
    }

    /** Keep the subclass intact when recipes are synchronized to clients. */
    @Override
    public RecipeSerializer<?> getSerializer() {
        return LaoWuMod.RANDOM_BABY_CAT_PANCAKE_FILLING_SERIALIZER.get();
    }
}
