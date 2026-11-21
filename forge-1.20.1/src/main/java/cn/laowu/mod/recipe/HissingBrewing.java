package cn.laowu.mod.recipe;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.compat.create.CreateIntegration;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.crafting.PartialNBTIngredient;

import java.util.List;

public final class HissingBrewing {
    private HissingBrewing() {}

    public static void register() {
        addMix(LaoWuMod.HISSING_POTION.get(), Items.REDSTONE, LaoWuMod.LONG_HISSING_POTION.get());
        addMix(LaoWuMod.STRONG_HISSING_POTION.get(), Items.REDSTONE, LaoWuMod.LONG_STRONG_HISSING_POTION.get());
        addMix(LaoWuMod.POWERFUL_HISSING_POTION.get(), Items.REDSTONE, LaoWuMod.LONG_POWERFUL_HISSING_POTION.get());

        if (CreateIntegration.isLoaded()) return;
        addMix(Potions.AWKWARD, LaoWuMod.CAT_POWDER.get(), LaoWuMod.HISSING_POTION.get());
        addMix(LaoWuMod.HISSING_POTION.get(), Items.BLAZE_POWDER, LaoWuMod.STRONG_HISSING_POTION.get());
        addMix(LaoWuMod.STRONG_HISSING_POTION.get(), Items.GLOWSTONE_DUST, LaoWuMod.POWERFUL_HISSING_POTION.get());
        addMix(LaoWuMod.LONG_HISSING_POTION.get(), Items.BLAZE_POWDER, LaoWuMod.LONG_STRONG_HISSING_POTION.get());
        addMix(LaoWuMod.LONG_STRONG_HISSING_POTION.get(), Items.GLOWSTONE_DUST, LaoWuMod.LONG_POWERFUL_HISSING_POTION.get());
    }

    private static void addMix(Potion from, Item reagent, Potion to) {
        for (Item bottle : List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION)) {
            ItemStack input = PotionUtils.setPotion(new ItemStack(bottle), from);
            ItemStack output = PotionUtils.setPotion(new ItemStack(bottle), to);
            BrewingRecipeRegistry.addRecipe(PartialNBTIngredient.of(bottle, input.getOrCreateTag()),
                    Ingredient.of(reagent), output);
        }
    }
}
