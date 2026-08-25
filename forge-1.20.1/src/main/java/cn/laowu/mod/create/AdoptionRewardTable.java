package cn.laowu.mod.create;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Quality-tiered resources drawn from the vanilla villager trade ecosystem. */
final class AdoptionRewardTable {
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
                    entry(Items.COMPASS, 1, 1), entry(Items.BELL, 1, 1)),
            List.of(
                    entry(Items.EMERALD, 5, 10), entry(Items.DIAMOND, 1, 2),
                    entry(Items.NAME_TAG, 1, 1), entry(Items.SADDLE, 1, 1),
                    entry(Items.EXPERIENCE_BOTTLE, 4, 10), entry(Items.CLOCK, 1, 1),
                    entry(Items.DIAMOND_PICKAXE, 1, 1), entry(Items.DIAMOND_AXE, 1, 1),
                    entry(Items.DIAMOND_SHOVEL, 1, 1), entry(Items.DIAMOND_SWORD, 1, 1)),
            List.of(
                    entry(Items.EMERALD, 8, 16), entry(Items.DIAMOND, 2, 4),
                    entry(Items.NAME_TAG, 1, 2), entry(Items.BELL, 1, 1),
                    entry(Items.EXPERIENCE_BOTTLE, 8, 16),
                    entry(Items.GOLDEN_CARROT, 8, 16),
                    entry(Items.DIAMOND_PICKAXE, 1, 1), entry(Items.DIAMOND_AXE, 1, 1),
                    entry(Items.DIAMOND_SWORD, 1, 1), entry(Items.DIAMOND_CHESTPLATE, 1, 1),
                    entry(Items.DIAMOND_LEGGINGS, 1, 1))
    );

    static List<ItemStack> roll(int quality, RandomSource random) {
        int score = Mth.clamp(quality, 0, 100);
        int tier = Math.min(TIERS.size() - 1, score / 20);
        int rolls = 1 + score / 40;
        java.util.ArrayList<ItemStack> rewards = new java.util.ArrayList<>(rolls);
        List<Entry> pool = TIERS.get(tier);
        for (int roll = 0; roll < rolls; roll++) {
            Entry selected = pool.get(random.nextInt(pool.size()));
            int count = selected.min + random.nextInt(selected.max - selected.min + 1);
            if (selected.item.getMaxStackSize() > 1) {
                count += random.nextInt(1 + score / 25);
            }
            rewards.add(new ItemStack(selected.item,
                    Math.min(count, selected.item.getMaxStackSize())));
        }
        return List.copyOf(rewards);
    }

    private static Entry entry(Item item, int min, int max) {
        return new Entry(item, min, max);
    }

    private record Entry(Item item, int min, int max) {}

    private AdoptionRewardTable() {}
}
