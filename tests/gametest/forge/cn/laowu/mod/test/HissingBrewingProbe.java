package cn.laowu.mod.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class HissingBrewingProbe {
    private static HissingBrewingChecks stand(GameTestHelper test) {
        return new HissingBrewingChecks(test, (item, potion) ->
                PotionUtils.setPotion(new ItemStack(item), potion));
    }

    @GameTest(template = "artillery_probe")
    public static void hissingUpgradeRecipes(GameTestHelper test) {
        stand(test).upgrades();
    }

    @GameTest(template = "artillery_probe")
    public static void hissingDurationsAndBottles(GameTestHelper test) {
        stand(test).durationsAndBottles();
    }

    @GameTest(template = "artillery_probe")
    public static void hissingVanillaAndInvalidInputs(GameTestHelper test) {
        stand(test).vanillaAndInvalidInputs();
    }
}
