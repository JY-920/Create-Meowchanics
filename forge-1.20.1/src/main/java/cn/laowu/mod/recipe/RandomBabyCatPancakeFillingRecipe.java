package cn.laowu.mod.recipe;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.CatPancakeItem;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatAttributeProfile;
import cn.laowu.mod.genetics.CatTraitData;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
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
            ProcessingRecipeBuilder.ProcessingRecipeParams params) {
        super(params);
    }

    @Override
    public List<ItemStack> rollResults() {
        List<ResourceLocation> variants = BuiltInRegistries.CAT_VARIANT.keySet()
                .stream().toList();
        ResourceLocation chosen = variants.isEmpty()
                ? CatPancakeItem.DEFAULT_VARIANT
                : variants.get(ThreadLocalRandom.current().nextInt(variants.size()));
        ItemStack pancake = CatPancakeItem.babyVariantStack(chosen);
        RandomSource random = RandomSource.create(ThreadLocalRandom.current().nextLong());
        CatAttributeData.set(pancake, CatAttributeProfile.founder(random));
        CatTraitData.setInjected(pancake, random);
        return List.of(pancake);
    }

    /** Keep the subclass intact when recipes are synchronized to clients. */
    @Override
    public RecipeSerializer<?> getSerializer() {
        return LaoWuMod.RANDOM_BABY_CAT_PANCAKE_FILLING_SERIALIZER.get();
    }
}
