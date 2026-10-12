package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.compat.create.CreateIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.*;
import mezz.jei.api.*;
import mezz.jei.api.runtime.IJeiRuntime;

/** Test-only observer of JEI's real runtime and recipes synchronized from the server. */
@JeiPlugin
public final class StandaloneRecipeJeiProbe implements IModPlugin {
    private static IJeiRuntime runtime;
    @Override public net.minecraft.resources.ResourceLocation getPluginUid() {
        return LaoWuMod.id("standalone_recipe_test");
    }
    @Override public void onRuntimeAvailable(IJeiRuntime value) { runtime = value; }
    @Override public void onRuntimeUnavailable() { runtime = null; }

    static void verify(Minecraft mc) {
        if (runtime == null) throw new AssertionError("JEI runtime not ready");
        verifyIndustrialIngredientVisibility();
        if (CreateIntegration.isLoaded()) return;
        var manager = runtime.getRecipeManager();
        var crafts = manager.createRecipeLookup(mezz.jei.api.constants.RecipeTypes.CRAFTING).get().toList();
        long ordinary = crafts.stream().filter(r -> r.id().toString().startsWith("laowu:standalone/")).count();
        if (ordinary != 55) throw new AssertionError("JEI should show all 55 ordinary standalone crafts, got " + ordinary);
        var examples = crafts.stream().filter(r -> r.id().equals(LaoWuMod.id("jei/pheromone_cat_food_crafting"))).toList();
        if (examples.size() != 1) throw new AssertionError("JEI named-food recipe missing or duplicated");
        var display = examples.get(0).value().getResultItem(mc.level.registryAccess());
        if (cn.laowu.mod.item.PheromoneCatFoodItem.ownerName(display).isEmpty()) throw new AssertionError("Named-food display must show bound owner");
        long fish = manager.createRecipeLookup(mezz.jei.api.constants.RecipeTypes.CAMPFIRE_COOKING).get()
                .filter(r -> r.id().toString().startsWith("laowu:standalone/dried_fish_")).count();
        if (fish != 2) throw new AssertionError("Both dried-fish campfire recipes must be visible");
        var synced = mc.level.getRecipeManager().byKey(LaoWuMod.id("standalone/pheromone_cat_food")).orElseThrow().value();
        if (!(synced instanceof cn.laowu.mod.recipe.PheromoneCatFoodCraftingRecipe))
            throw new AssertionError("Special recipe serializer did not synchronize to client");
        var text = new net.minecraft.world.item.ItemStack(LaoWuMod.PHEROMONE_CAT_FOOD.get())
                .getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(mc.level), mc.player, net.minecraft.world.item.TooltipFlag.NORMAL).stream()
                .map(net.minecraft.network.chat.Component::getString).collect(java.util.stream.Collectors.joining(""));
        if (!text.replaceAll("\\s+", "").contains(net.minecraft.network.chat.Component.translatable("item.laowu.pheromone_cat_food.tooltip.standalone").getString().replaceAll("\\s+", "")))
            throw new AssertionError("Standalone recipe instructions are missing");
        System.out.println("PASS: REAL JEI 55 CRAFTS + NAMED FOOD + TWO FISH RECIPES + CLIENT SERIALIZER");
    }

    private static void verifyIndustrialIngredientVisibility() {
        boolean create=CreateIntegration.isLoaded();
        var filter=runtime.getIngredientFilter();
        filter.setFilterText("");
        var items=filter.getFilteredItemStacks();
        for(var name:java.util.List.of("cat_shell","cat_grenade","hissing_gas_bucket","liquid_cat_bucket")) {
            var id=LaoWuMod.id(name);
            boolean visible=items.stream().anyMatch(s->net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).equals(id));
            if(visible!=create) throw new AssertionError("JEI industrial item visibility: "+id+" Create="+create);
        }
        var fluids=filter.getFilteredIngredients(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK);
        for(var name:java.util.List.of("hissing_gas","flowing_hissing_gas","liquid_cat","flowing_liquid_cat")) {
            var id=LaoWuMod.id(name);
            boolean visible=fluids.stream().anyMatch(s->net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(s.getFluid()).equals(id));
            if(!create&&visible || create&&!name.startsWith("flowing_")&&!visible)
                throw new AssertionError("JEI industrial fluid visibility: "+id+" Create="+create);
        }
        if(items.stream().noneMatch(s->s.is(LaoWuMod.CAT_INGOT.get())))
            throw new AssertionError("Unrelated cat ingots must remain visible");
        System.out.println("PASS: REAL JEI INDUSTRIAL VISIBILITY Create="+create);
    }
}
