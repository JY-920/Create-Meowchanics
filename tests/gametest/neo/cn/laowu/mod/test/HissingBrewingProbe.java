package cn.laowu.mod.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class HissingBrewingProbe {
    private static HissingBrewingChecks stand(GameTestHelper test) {
        return new HissingBrewingChecks(test, (item, potion) ->
                PotionContents.createItemStack(item, BuiltInRegistries.POTION.wrapAsHolder(potion)));
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
