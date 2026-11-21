package cn.laowu.mod.test;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.compat.create.CreateIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

import java.util.List;
import java.util.function.BiFunction;

final class HissingBrewingChecks {
    private static final List<Item> BOTTLES = List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION);
    private static final List<String> NORMAL = List.of("laowu:hissing", "laowu:strong_hissing", "laowu:powerful_hissing");
    private static final List<String> EXTENDED = List.of("laowu:long_hissing", "laowu:long_strong_hissing", "laowu:long_powerful_hissing");
    private final GameTestHelper test;
    private final BiFunction<Item, Potion, ItemStack> bottle;
    private final BlockPos position;
    private final BrewingStandBlockEntity stand;

    HissingBrewingChecks(GameTestHelper test, BiFunction<Item, Potion, ItemStack> bottle) {
        this.test = test;
        this.bottle = bottle;
        BlockPos relative = new BlockPos(1, 1, 1);
        test.setBlock(relative, Blocks.BREWING_STAND);
        position = test.absolutePos(relative);
        stand = (BrewingStandBlockEntity) test.getLevel().getBlockEntity(position);
    }

    void upgrades() {
        boolean create = CreateIntegration.isLoaded();
        for (String id : List.of("hissing_potion_infiltrating", "strong_hissing_potion_mixing", "powerful_hissing_potion_mixing")) {
            test.assertTrue(test.getLevel().getRecipeManager().byKey(LaoWuMod.id(id)).isPresent() == create, "Industrial recipe gate: " + id);
        }
        checkAcrossBottles("minecraft:awkward", LaoWuMod.CAT_POWDER.get(), NORMAL.get(0), !create);
        checkAcrossBottles(NORMAL.get(0), Items.BLAZE_POWDER, NORMAL.get(1), !create);
        checkAcrossBottles(NORMAL.get(1), Items.GLOWSTONE_DUST, NORMAL.get(2), !create);
        checkAcrossBottles(EXTENDED.get(0), Items.BLAZE_POWDER, EXTENDED.get(1), !create);
        checkAcrossBottles(EXTENDED.get(1), Items.GLOWSTONE_DUST, EXTENDED.get(2), !create);
        test.succeed();
    }

    void durationsAndBottles() {
        for (int tier = 0; tier < NORMAL.size(); tier++) {
            String normal = NORMAL.get(tier);
            String longer = EXTENDED.get(tier);
            checkAcrossBottles(normal, Items.REDSTONE, longer, true);
            checkAcrossBottles(longer, Items.REDSTONE, longer, false);
            checkEffect(normal, tier, 3600);
            checkEffect(longer, tier, 9600);
            for (String id : List.of(normal, longer)) {
                checkBatch(id, List.of(Items.POTION), Items.GUNPOWDER, id, List.of(Items.SPLASH_POTION), true);
                checkBatch(id, List.of(Items.SPLASH_POTION), Items.DRAGON_BREATH, id, List.of(Items.LINGERING_POTION), true);
            }
        }
        test.succeed();
    }

    void vanillaAndInvalidInputs() {
        checkAcrossBottles("minecraft:awkward", Items.BLAZE_POWDER, "minecraft:strength", true);
        checkAcrossBottles("minecraft:strength", Items.GLOWSTONE_DUST, "minecraft:strong_strength", true);
        checkAcrossBottles("minecraft:swiftness", Items.REDSTONE, "minecraft:long_swiftness", true);
        checkAcrossBottles("minecraft:water", LaoWuMod.CAT_POWDER.get(), "minecraft:water", false);
        checkAcrossBottles(NORMAL.get(0), Items.GLOWSTONE_DUST, NORMAL.get(0), false);
        checkAcrossBottles(NORMAL.get(2), Items.GLOWSTONE_DUST, NORMAL.get(2), false);
        test.succeed();
    }

    private Potion potion(String id) {
        var key = ResourceLocation.tryParse(id);
        test.assertTrue(BuiltInRegistries.POTION.containsKey(key), "Missing potion " + id);
        return BuiltInRegistries.POTION.get(key);
    }

    private void checkEffect(String id, int amplifier, int duration) {
        var effects = potion(id).getEffects();
        test.assertTrue(effects.size() == 1, "Single effect for " + id);
        var effect = effects.get(0);
        test.assertTrue(effect.getDescriptionId().equals("effect.laowu.hissing_attack")
                && effect.getAmplifier() == amplifier && effect.getDuration() == duration, "Wrong effect for " + id);
    }

    private void checkAcrossBottles(String from, Item reagent, String to, boolean shouldBrew) {
        checkBatch(from, BOTTLES, reagent, to, BOTTLES, shouldBrew);
    }

    private void checkBatch(String from, List<Item> inputs, Item reagent, String to, List<Item> outputs, boolean shouldBrew) {
        stand.clearContent();
        for (int slot = 0; slot < inputs.size(); slot++) {
            stand.setItem(slot, bottle.apply(inputs.get(slot), potion(from)));
        }
        stand.setItem(3, new ItemStack(reagent, 2));
        stand.setItem(4, new ItemStack(Items.BLAZE_POWDER));
        String mix = from + " + " + reagent + " -> " + to;
        if (shouldBrew) test.assertTrue(stand.canPlaceItem(3, new ItemStack(reagent)), "Ingredient slot rejected " + mix);
        for (int tick = 0; tick <= 400; tick++) {
            BrewingStandBlockEntity.serverTick(test.getLevel(), position, stand.getBlockState(), stand);
        }
        test.assertTrue(stand.getItem(3).getCount() == (shouldBrew ? 1 : 2), "Reagent consumption: " + mix);
        for (int slot = 0; slot < outputs.size(); slot++) {
            var expected = bottle.apply(outputs.get(slot), potion(shouldBrew ? to : from));
            test.assertTrue(ItemStack.matches(stand.getItem(slot), expected), "Wrong output in slot " + slot + " for " + mix);
        }
    }
}
