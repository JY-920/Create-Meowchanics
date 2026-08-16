package cn.laowu.mod.compat.jei;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.CatPancakeItem;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public final class LaoWuJeiPlugin implements IModPlugin {
    @Override public ResourceLocation getPluginUid() { return LaoWuMod.id("jei_plugin"); }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        // Cat skins are presentation data, not separate JEI ingredients. This
        // deliberately collapses any number of present/future texture NBT
        // variants while keeping outfit states distinct and searchable.
        registration.registerSubtypeInterpreter(LaoWuMod.CAT_PANCAKE.get(), (stack, context) ->
                CatPancakeItem.getOutfit(stack).id());
        registration.useNbtForSubtypes(LaoWuMod.CAT_POUCH.get());
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        registration.addExtraItemStacks(CatPancakeItem.jeiDisplayStacks());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new InfiltratingJeiCategory());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null)
            registration.addRecipes(InfiltratingJeiCategory.TYPE,
                    new ArrayList<>(level.getRecipeManager()
                            .getAllRecipesFor(LaoWuMod.INFILTRATING_TYPE.get())));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(LaoWuMod.INFILTRATION_TANK_ITEM.get(), InfiltratingJeiCategory.TYPE);
    }

    /**
     * These legacy recipes remain valid datapack recipes, but are deliberately
     * omitted from JEI so the public recipe guide only presents the new,
     * non-harmful Hakimi Honey production chain.
     */
    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        hideCreateRecipe(runtime, "mixing", "liquid_cat_mixing");
        hideCreateRecipe(runtime, "crushing", "cat_powder_crushing");
        hideCreateRecipe(runtime, "milling", "cat_powder_milling");
        // Create also mirrors milling recipes in the Crushing Wheels category.
        hideCreateRecipe(runtime, "crushing", "cat_powder_milling");
        hideCreateRecipe(runtime, "sawing", "cat_strip_cutting");
        hideNonOrangeCatPancakeRecipes(runtime);
    }

    /**
     * Runtime recipes may exist once per skin so processing can preserve NBT.
     * Walk JEI's actual category contents (including Create's generated
     * *_using_deployer wrappers) and hide every page carrying a non-orange
     * pancake. This scales to hundreds of future texture-NBT combinations
     * without maintaining a list of recipe ids.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void hideNonOrangeCatPancakeRecipes(IJeiRuntime runtime) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        var recipeManager = runtime.getRecipeManager();
        var categories = recipeManager.createRecipeCategoryLookup()
                .includeHidden().get().toList();
        for (var category : categories) {
            RecipeType type = category.getRecipeType();
            List hidden = recipeManager.createRecipeLookup(type)
                    .includeHidden().get()
                    .filter(candidate -> candidate instanceof Recipe<?> recipe
                            && containsNonOrangePancake(recipe, level.registryAccess()))
                    .toList();
            if (!hidden.isEmpty()) recipeManager.hideRecipes(type, hidden);
        }
    }

    private static boolean containsNonOrangePancake(
            Recipe<?> recipe, net.minecraft.core.RegistryAccess registryAccess) {
        for (var ingredient : recipe.getIngredients()) {
            for (ItemStack stack : ingredient.getItems()) {
                if (isNonOrangePancake(stack)) return true;
            }
        }

        if (recipe instanceof ProcessingRecipe<?> processing) {
            for (var output : processing.getRollableResults()) {
                if (isNonOrangePancake(output.getStack())) return true;
            }
        } else if (isNonOrangePancake(recipe.getResultItem(registryAccess))) {
            return true;
        }
        return false;
    }

    private static boolean isNonOrangePancake(ItemStack stack) {
        return stack.is(LaoWuMod.CAT_PANCAKE.get())
                && !CatPancakeItem.DEFAULT_VARIANT.equals(CatPancakeItem.variantId(stack));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void hideCreateRecipe(IJeiRuntime runtime, String category, String recipePath) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        var recipe = level.getRecipeManager().byKey(LaoWuMod.id(recipePath)).orElse(null);
        if (recipe == null) return;

        var recipeManager = runtime.getRecipeManager();
        recipeManager.getRecipeType(ResourceLocation.fromNamespaceAndPath("create", category))
                .ifPresent(type -> recipeManager.hideRecipes((RecipeType) type, List.of(recipe)));
    }
}
