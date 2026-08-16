package cn.laowu.mod.compat.jei;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.CatPancakeItem;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

@JeiPlugin
public final class LaoWuJeiPlugin implements IModPlugin {
    @Override public ResourceLocation getPluginUid() { return LaoWuMod.id("jei_plugin"); }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(LaoWuMod.CAT_PANCAKE.get(),
                new ISubtypeInterpreter<ItemStack>() {
                    @Override
                    public Object getSubtypeData(ItemStack stack, UidContext context) {
                        return CatPancakeItem.getOutfit(stack).id();
                    }

                    @Override
                    public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                        return CatPancakeItem.getOutfit(stack).id();
                    }
                });
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
        if (level != null) {
            var recipes = level.getRecipeManager()
                    .getAllRecipesFor(LaoWuMod.INFILTRATING_TYPE.get())
                    .stream()
                    .map(holder -> new RecipeHolder<com.simibubi.create.content.processing.basin.BasinRecipe>(
                            holder.id(), holder.value()))
                    .toList();
            registration.addRecipes(InfiltratingJeiCategory.TYPE, recipes);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(LaoWuMod.INFILTRATION_TANK_ITEM.get(), InfiltratingJeiCategory.TYPE);
    }

    /** Keep legacy datapack recipes functional while omitting them from the public recipe guide. */
    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        hideRecipeById(runtime, "mixing", "liquid_cat_mixing");
        hideRecipeById(runtime, "crushing", "cat_powder_crushing");
        hideRecipeById(runtime, "milling", "cat_powder_milling");
        hideRecipeById(runtime, "crushing", "cat_powder_milling");
        hideRecipeById(runtime, "sawing", "cat_strip_cutting");
        hideNonOrangeCatPancakeRecipes(runtime);
        ensureCatGrenadeAssemblyVisible(runtime);
    }

    /**
     * Create normally imports every sequenced assembly recipe into JEI. Some
     * JEI/Create combinations omit this one from the runtime lookup even
     * though the deployers can execute it, so repair only that missing entry
     * (or unhide it) without creating a duplicate page.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void ensureCatGrenadeAssemblyVisible(IJeiRuntime runtime) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        ResourceLocation categoryId = ResourceLocation.fromNamespaceAndPath(
                "create", "sequenced_assembly");
        ResourceLocation recipeId = LaoWuMod.id("cat_grenade_sequenced_assembly");
        var jeiRecipes = runtime.getRecipeManager();
        jeiRecipes.getRecipeType(categoryId).ifPresent(type -> {
            List registered = jeiRecipes.createRecipeLookup((RecipeType) type)
                    .includeHidden().get()
                    .filter(candidate -> candidate instanceof RecipeHolder<?> holder
                            && holder.id().equals(recipeId))
                    .toList();
            if (!registered.isEmpty()) {
                jeiRecipes.unhideRecipes((RecipeType) type, registered);
                return;
            }

            List missing = level.getRecipeManager()
                    .getAllRecipesFor(AllRecipeTypes.SEQUENCED_ASSEMBLY.getType())
                    .stream()
                    .filter(holder -> holder.id().equals(recipeId))
                    .toList();
            if (!missing.isEmpty())
                jeiRecipes.addRecipes((RecipeType) type, (List) missing);
        });
    }

    /**
     * Runtime recipes exist once per skin so processing can preserve the cat's
     * appearance. JEI only needs the orange representative; hide every page
     * whose input or output contains another skin.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void hideNonOrangeCatPancakeRecipes(IJeiRuntime runtime) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        var recipeManager = runtime.getRecipeManager();
        for (var category : recipeManager.createRecipeCategoryLookup()
                .includeHidden().get().toList()) {
            RecipeType type = category.getRecipeType();
            List hidden = recipeManager.createRecipeLookup(type)
                    .includeHidden().get()
                    .filter(candidate -> containsNonOrangePancake(
                            candidate, level.registryAccess()))
                    .toList();
            if (!hidden.isEmpty()) recipeManager.hideRecipes(type, hidden);
        }
    }

    private static boolean containsNonOrangePancake(
            Object candidate, net.minecraft.core.HolderLookup.Provider registries) {
        Recipe<?> recipe;
        if (candidate instanceof RecipeHolder<?> holder) {
            recipe = holder.value();
        } else if (candidate instanceof Recipe<?> directRecipe) {
            recipe = directRecipe;
        } else {
            return false;
        }

        for (var ingredient : recipe.getIngredients()) {
            for (ItemStack stack : ingredient.getItems()) {
                if (isNonOrangePancake(stack)) return true;
            }
        }
        if (recipe instanceof ProcessingRecipe<?, ?> processing) {
            for (var output : processing.getRollableResults()) {
                if (isNonOrangePancake(output.getStack())) return true;
            }
        } else if (isNonOrangePancake(recipe.getResultItem(registries))) {
            return true;
        }
        return false;
    }

    private static boolean isNonOrangePancake(ItemStack stack) {
        return stack.is(LaoWuMod.CAT_PANCAKE.get())
                && !CatPancakeItem.DEFAULT_VARIANT.equals(CatPancakeItem.variantId(stack));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void hideRecipeById(IJeiRuntime runtime, String category, String recipePath) {
        var recipeManager = runtime.getRecipeManager();
        recipeManager.getRecipeType(ResourceLocation.fromNamespaceAndPath("create", category))
                .ifPresent(type -> {
                    List hidden = recipeManager.createRecipeLookup((RecipeType) type)
                            .includeHidden().get()
                            .filter(candidate -> candidate instanceof RecipeHolder<?> holder
                                    && holder.id().equals(LaoWuMod.id(recipePath)))
                            .toList();
                    if (!hidden.isEmpty()) recipeManager.hideRecipes((RecipeType) type, hidden);
                });
    }
}
