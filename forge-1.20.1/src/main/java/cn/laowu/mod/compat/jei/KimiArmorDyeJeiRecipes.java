package cn.laowu.mod.compat.jei;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.KimiArmorDye;
import cn.laowu.mod.item.KimiDyePalette;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.core.NonNullList;
import java.util.*;

/** Twenty fixed examples, not 16^3 animated colour combinations. Actual recipe preserves item data. */
final class KimiArmorDyeJeiRecipes {
    static List<CraftingRecipe> examples() {
        List<CraftingRecipe> result = new ArrayList<>();
        for (var armor : List.of(LaoWuMod.CAT_HELMET.get(),LaoWuMod.CAT_CHESTPLATE.get(),
                LaoWuMod.CAT_LEGGINGS.get(),LaoWuMod.CAT_BOOTS.get())) {
            var armorId=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(armor).getPath();
            for(int example=0;example<5;example++) {
                boolean helmet = armor == LaoWuMod.CAT_HELMET.get();
                if (!helmet && example == 2) continue;
                NonNullList<Ingredient> ingredients=NonNullList.withSize(9,Ingredient.EMPTY);
                ItemStack out=new ItemStack(armor);
                ingredients.set(4,Ingredient.of(armor));
                int code=0;
                DyeItem[] dyes={(DyeItem)Items.BLUE_DYE,(DyeItem)Items.RED_DYE,(DyeItem)Items.LIME_DYE};
                for(int region=0;region<(helmet ? 3 : 2);region++) if(example==region || example>=3) {
                    code=KimiDyePalette.replace(code,region,dyes[region].getDyeColor().getId());
                    if(example<4) ingredients.set(region,Ingredient.of(dyes[region]));
                }
                if(example==4) {
                    ItemStack input=out.copy(); KimiArmorDye.write(input,code);
                    ingredients.set(4,Ingredient.of(input));
                } else KimiArmorDye.write(out,code);
                var id=LaoWuMod.id("jei/kimi_dye/"+armorId+"_"+example);
                result.add(new ShapedRecipe(id,"kimi_dye",CraftingBookCategory.EQUIPMENT,3,3,ingredients,out));
            }
        }
        for (DyeColor colour : DyeColor.values()) {
            ItemStack out=new ItemStack(LaoWuMod.CAT_LASER_POINTER.get());
            cn.laowu.mod.item.CatLaserPointerItem.dye(out,colour);
            NonNullList<Ingredient> inputs=NonNullList.create();
            inputs.add(Ingredient.of(LaoWuMod.CAT_LASER_POINTER.get()));
            inputs.add(Ingredient.of(DyeItem.byColor(colour)));
            var id=LaoWuMod.id("jei/laser_dye/"+colour.getName());
            result.add(new ShapelessRecipe(id,"laser_dye",CraftingBookCategory.EQUIPMENT,out,inputs));
        }
        return result;
    }
    private KimiArmorDyeJeiRecipes() {}
}
