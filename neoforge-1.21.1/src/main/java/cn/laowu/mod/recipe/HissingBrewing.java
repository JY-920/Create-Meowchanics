package cn.laowu.mod.recipe;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.compat.create.CreateIntegration;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

public final class HissingBrewing {
    private HissingBrewing() {}

    public static void register(RegisterBrewingRecipesEvent event) {
        var builder = event.getBuilder();
        builder.addMix(LaoWuMod.HISSING_POTION, Items.REDSTONE, LaoWuMod.LONG_HISSING_POTION);
        builder.addMix(LaoWuMod.STRONG_HISSING_POTION, Items.REDSTONE, LaoWuMod.LONG_STRONG_HISSING_POTION);
        builder.addMix(LaoWuMod.POWERFUL_HISSING_POTION, Items.REDSTONE, LaoWuMod.LONG_POWERFUL_HISSING_POTION);

        if (CreateIntegration.isLoaded()) return;
        builder.addMix(Potions.AWKWARD, LaoWuMod.CAT_POWDER.get(), LaoWuMod.HISSING_POTION);
        builder.addMix(LaoWuMod.HISSING_POTION, Items.BLAZE_POWDER, LaoWuMod.STRONG_HISSING_POTION);
        builder.addMix(LaoWuMod.STRONG_HISSING_POTION, Items.GLOWSTONE_DUST, LaoWuMod.POWERFUL_HISSING_POTION);
        builder.addMix(LaoWuMod.LONG_HISSING_POTION, Items.BLAZE_POWDER, LaoWuMod.LONG_STRONG_HISSING_POTION);
        builder.addMix(LaoWuMod.LONG_STRONG_HISSING_POTION, Items.GLOWSTONE_DUST, LaoWuMod.LONG_POWERFUL_HISSING_POTION);
    }
}
