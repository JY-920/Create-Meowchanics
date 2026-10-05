package cn.laowu.mod.client;

import cn.laowu.mod.create.CatMachineBlocks;
import cn.laowu.mod.compat.jei.LaoWuJeiPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import mezz.jei.api.recipe.RecipeType;

/** Test-only checks against JEI's actual visible runtime registry after joining an isolated world. */
final class CatMachineRecipeClientProbe {
    @SuppressWarnings({"rawtypes","unchecked"})
    static void verify(Minecraft mc) {
        var manager=com.simibubi.create.compat.jei.CreateJEI.runtime.getRecipeManager();
        for(String recipe:new String[]{"cat_press","cat_mixer","cat_depot","haji_basin"})
            visible(manager,"minecraft:crafting","laowu:"+recipe);
        for(String recipe:new String[]{"cat_casing_from_cardboard"}) {
            visible(manager,"create:item_application","laowu:"+recipe);
            visible(manager,"create:deploying","laowu:"+recipe+"_using_deployer");
        }
        for(String category:new String[]{"mixing","packing","automatic_packing"})
            check(manager.createRecipeCatalystLookup(LaoWuJeiPlugin.catMachineCategory(category))
                .getItemStack().anyMatch(s->s.is(CatMachineBlocks.HAJI_BASIN_ITEM.get())),"Cat basin missing JEI catalyst "+category);
        check(net.minecraft.client.resources.language.I18n.get("block.laowu.haji_basin").equals("Cat Basin"),"Basin display name not updated");
        System.out.println("PASS: four crafting, cardboard manual and deployer recipes visible in real JEI, cat basin name/catalysts");
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void visible(mezz.jei.api.recipe.IRecipeManager manager,String category,String id) {
        var type=manager.getRecipeType(ResourceLocation.parse(category)).orElseThrow();
        long count=manager.createRecipeLookup((RecipeType)type).get().filter(r->recipeId(r).equals(id)).count();
        check(count==1,"JEI recipe missing or duplicated "+category+"/"+id+" count="+count);
    }
    private static String recipeId(Object recipe) {return recipe instanceof net.minecraft.world.item.crafting.RecipeHolder<?> r?r.id().toString():"";}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
