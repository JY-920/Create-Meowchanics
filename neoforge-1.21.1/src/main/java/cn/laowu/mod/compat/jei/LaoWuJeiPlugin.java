package cn.laowu.mod.compat.jei;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.compat.create.CreateIntegration;
import net.minecraft.resources.ResourceLocation;
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
@JeiPlugin
public final class LaoWuJeiPlugin implements IModPlugin {
    @Override public ResourceLocation getPluginUid() { return LaoWuMod.id("jei_plugin"); }
    @Override public void registerItemSubtypes(ISubtypeRegistration argument) {
        if (CreateIntegration.isLoaded()) new CreateJeiIntegration().registerItemSubtypes(argument);
        else argument.registerSubtypeInterpreter(LaoWuMod.CAT_PANCAKE.get(), (stack,context) -> cn.laowu.mod.item.CatPancakeItem.getOutfit(stack).id());
    }
    @Override public void registerExtraIngredients(IExtraIngredientRegistration argument) {
        if (CreateIntegration.isLoaded()) new CreateJeiIntegration().registerExtraIngredients(argument);
        else argument.addExtraItemStacks(cn.laowu.mod.item.CatPancakeItem.jeiDisplayStacks());
    }
    @Override public void registerCategories(IRecipeCategoryRegistration argument) {
        if (CreateIntegration.isLoaded()) new CreateJeiIntegration().registerCategories(argument);
    }
    @Override public void registerRecipes(IRecipeRegistration argument) {
        if (CreateIntegration.isLoaded()) new CreateJeiIntegration().registerRecipes(argument);
        else argument.addRecipes(mezz.jei.api.constants.RecipeTypes.CRAFTING, KimiArmorDyeJeiRecipes.examples());
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration argument) {
        if (CreateIntegration.isLoaded()) new CreateJeiIntegration().registerRecipeCatalysts(argument);
    }
    @Override public void onRuntimeAvailable(IJeiRuntime argument) {
        if (CreateIntegration.isLoaded()) new CreateJeiIntegration().onRuntimeAvailable(argument);
        else hideStandaloneIndustrialIngredients(argument);
    }
    private static void hideStandaloneIndustrialIngredients(IJeiRuntime runtime) {
        // Keep the registries intact for old saves; remove only JEI display entries.
        var ingredients = runtime.getIngredientManager();
        var items = ingredients.getAllItemStacks().stream()
                .filter(stack -> stack.is(LaoWuMod.CAT_SHELL.get()) || stack.is(LaoWuMod.CAT_GRENADE.get())
                        || stack.is(LaoWuMod.HISSING_GAS_BUCKET.get()) || stack.is(LaoWuMod.LIQUID_CAT_BUCKET.get()))
                .toList();
        if (!items.isEmpty()) ingredients.removeIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, items);
        var fluidType = mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK;
        var fluids = ingredients.getAllIngredients(fluidType).stream()
                .filter(stack -> stack.getFluid() == LaoWuMod.HISSING_GAS.get() || stack.getFluid() == LaoWuMod.FLOWING_HISSING_GAS.get()
                        || stack.getFluid() == LaoWuMod.LIQUID_CAT.get() || stack.getFluid() == LaoWuMod.FLOWING_LIQUID_CAT.get())
                .toList();
        if (!fluids.isEmpty()) ingredients.removeIngredientsAtRuntime(fluidType, fluids);
    }
    @Override public void onRuntimeUnavailable() {
        if (CreateIntegration.isLoaded()) new CreateJeiIntegration().onRuntimeUnavailable();
    }
    public static RecipeType<?> catMachineCategory(String path) {
        if (!CreateIntegration.isLoaded()) throw new IllegalStateException("Create machine categories are unavailable");
        return CreateJeiIntegration.catMachineCategory(path);
    }
}
