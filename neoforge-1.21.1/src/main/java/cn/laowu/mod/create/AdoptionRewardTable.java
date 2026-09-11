package cn.laowu.mod.create;

import net.minecraft.core.RegistryAccess;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.npc.VillagerProfession;

import java.util.List;
import java.util.Optional;

/** Quality-tiered resources drawn from the vanilla villager trade ecosystem. */
final class AdoptionRewardTable {
    // Curated buy/sell resources: never generate treasure-map trades or search world structures.
    private static final java.util.Map<VillagerProfession, List<Item>> PROFESSION_POOLS = java.util.Map.ofEntries(
            java.util.Map.entry(VillagerProfession.FARMER, List.of(Items.WHEAT, Items.BREAD, Items.PUMPKIN_PIE, Items.CAKE, Items.GOLDEN_CARROT)),
            java.util.Map.entry(VillagerProfession.FISHERMAN, List.of(Items.COD, Items.COOKED_COD, Items.COOKED_SALMON, Items.CAMPFIRE, Items.FISHING_ROD)),
            java.util.Map.entry(VillagerProfession.SHEPHERD, List.of(Items.WHITE_WOOL, Items.WHITE_CARPET, Items.SHEARS, Items.WHITE_BED, Items.WHITE_BANNER)),
            java.util.Map.entry(VillagerProfession.FLETCHER, List.of(Items.STICK, Items.ARROW, Items.FLINT, Items.BOW, Items.CROSSBOW)),
            java.util.Map.entry(VillagerProfession.LIBRARIAN, List.of(Items.PAPER, Items.BOOK, Items.BOOKSHELF, Items.ENCHANTED_BOOK, Items.NAME_TAG)),
            java.util.Map.entry(VillagerProfession.CARTOGRAPHER, List.of(Items.PAPER, Items.GLASS_PANE, Items.MAP, Items.COMPASS, Items.GLOBE_BANNER_PATTERN)),
            java.util.Map.entry(VillagerProfession.CLERIC, List.of(Items.ROTTEN_FLESH, Items.REDSTONE, Items.LAPIS_LAZULI, Items.ENDER_PEARL, Items.EXPERIENCE_BOTTLE)),
            java.util.Map.entry(VillagerProfession.ARMORER, List.of(Items.COAL, Items.IRON_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_CHESTPLATE)),
            java.util.Map.entry(VillagerProfession.WEAPONSMITH, List.of(Items.COAL, Items.IRON_AXE, Items.IRON_SWORD, Items.DIAMOND_AXE, Items.DIAMOND_SWORD)),
            java.util.Map.entry(VillagerProfession.TOOLSMITH, List.of(Items.COAL, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_PICKAXE)),
            java.util.Map.entry(VillagerProfession.BUTCHER, List.of(Items.CHICKEN, Items.PORKCHOP, Items.COOKED_CHICKEN, Items.RABBIT_STEW, Items.COOKED_PORKCHOP)),
            java.util.Map.entry(VillagerProfession.LEATHERWORKER, List.of(Items.LEATHER, Items.LEATHER_LEGGINGS, Items.LEATHER_CHESTPLATE, Items.LEATHER_HORSE_ARMOR, Items.SADDLE)),
            java.util.Map.entry(VillagerProfession.MASON, List.of(Items.CLAY_BALL, Items.BRICK, Items.POLISHED_ANDESITE, Items.TERRACOTTA, Items.QUARTZ_BLOCK))
    );

    static ItemStack rollProfession(VillagerProfession profession, int quality, RandomSource random, RegistryAccess registries) {
        int score = Mth.clamp(quality, 0, 100);
        // Unknown mod professions receive trade currency, not unrelated profession goods.
        List<Item> pool = PROFESSION_POOLS.getOrDefault(profession, List.of(Items.EMERALD));
        int tier = Math.min(pool.size() - 1, score / 20);
        int selectedTier = tier > 0 && random.nextInt(4) == 0 ? tier - 1 : tier;
        Item item = pool.get(selectedTier);
        int count = 1 + score / 20 + random.nextInt(3);
        return createReward(entry(item, 1, 1), count, score, random, registries);
    }

    private static final List<List<Entry>> TIERS = List.of(
            List.of(
                    entry(Items.WHEAT, 4, 10), entry(Items.CARROT, 4, 10),
                    entry(Items.POTATO, 4, 10), entry(Items.BEETROOT, 4, 10),
                    entry(Items.COD, 2, 6), entry(Items.SALMON, 2, 5),
                    entry(Items.STICK, 8, 16), entry(Items.FLINT, 2, 6),
                    entry(Items.CLAY_BALL, 4, 12), entry(Items.PAPER, 4, 10),
                    entry(Items.COAL, 2, 6), entry(Items.STRING, 4, 8)),
            List.of(
                    entry(Items.BREAD, 3, 7), entry(Items.APPLE, 2, 5),
                    entry(Items.COOKED_COD, 2, 5), entry(Items.COOKED_SALMON, 2, 5),
                    entry(Items.ARROW, 8, 16), entry(Items.BRICK, 4, 10),
                    entry(Items.WHITE_WOOL, 3, 7), entry(Items.GLASS, 4, 8),
                    entry(Items.BOOK, 2, 5), entry(Items.LANTERN, 1, 3),
                    entry(Items.EMERALD, 1, 2)),
            List.of(
                    entry(Items.EMERALD, 2, 5), entry(Items.IRON_INGOT, 3, 7),
                    entry(Items.REDSTONE, 6, 12), entry(Items.LAPIS_LAZULI, 6, 12),
                    entry(Items.GLOWSTONE_DUST, 4, 10), entry(Items.ENDER_PEARL, 1, 3),
                    entry(Items.GOLDEN_CARROT, 2, 5), entry(Items.BOOKSHELF, 1, 3),
                    entry(Items.COMPASS, 1, 1), entry(Items.BELL, 1, 1),
                    entry(Items.ENCHANTED_BOOK, 1, 1)),
            List.of(
                    entry(Items.EMERALD, 5, 10), entry(Items.DIAMOND, 1, 2),
                    entry(Items.NAME_TAG, 1, 1), entry(Items.SADDLE, 1, 1),
                    entry(Items.EXPERIENCE_BOTTLE, 4, 10), entry(Items.CLOCK, 1, 1),
                    entry(Items.DIAMOND_PICKAXE, 1, 1), entry(Items.DIAMOND_AXE, 1, 1),
                    entry(Items.DIAMOND_SHOVEL, 1, 1), entry(Items.DIAMOND_SWORD, 1, 1),
                    entry(Items.ENCHANTED_BOOK, 1, 1)),
            List.of(
                    entry(Items.EMERALD, 8, 16), entry(Items.DIAMOND, 2, 4),
                    entry(Items.NAME_TAG, 1, 2), entry(Items.BELL, 1, 1),
                    entry(Items.EXPERIENCE_BOTTLE, 8, 16),
                    entry(Items.GOLDEN_CARROT, 8, 16),
                    entry(Items.DIAMOND_PICKAXE, 1, 1), entry(Items.DIAMOND_AXE, 1, 1),
                    entry(Items.DIAMOND_SWORD, 1, 1), entry(Items.DIAMOND_CHESTPLATE, 1, 1),
                    entry(Items.DIAMOND_LEGGINGS, 1, 1),
                    entry(Items.ENCHANTED_BOOK, 1, 1))
    );

    static List<ItemStack> roll(int quality, RandomSource random,
                                RegistryAccess registries) {
        int score = Mth.clamp(quality, 0, 100);
        int tier = Math.min(TIERS.size() - 1, score / 20);
        int rolls = 1 + score / 40;
        java.util.ArrayList<ItemStack> rewards = new java.util.ArrayList<>(rolls);
        List<Entry> pool = TIERS.get(tier);
        for (int roll = 0; roll < rolls; roll++) {
            Entry selected = pool.get(random.nextInt(pool.size()));
            int count = selected.min + random.nextInt(selected.max - selected.min + 1);
            if (new ItemStack(selected.item).getMaxStackSize() > 1) {
                count += random.nextInt(1 + score / 25);
            }
            rewards.add(createReward(selected, count, score, random, registries));
        }
        return List.copyOf(rewards);
    }

    private static ItemStack createReward(Entry selected, int count, int score,
                                          RandomSource random,
                                          RegistryAccess registries) {
        if (selected.item == Items.ENCHANTED_BOOK) {
            int power = Mth.clamp(8 + score / 3, 10, 30);
            return EnchantmentHelper.enchantItem(random, new ItemStack(Items.BOOK),
                    power, registries, Optional.empty());
        }
        ItemStack reward = new ItemStack(selected.item);
        reward.setCount(Math.min(count, reward.getMaxStackSize()));
        return reward;
    }

    private static Entry entry(Item item, int min, int max) {
        return new Entry(item, min, max);
    }

    private record Entry(Item item, int min, int max) {}

    private AdoptionRewardTable() {}
}
