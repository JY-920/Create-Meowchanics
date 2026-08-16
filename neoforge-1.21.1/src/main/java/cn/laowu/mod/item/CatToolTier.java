package cn.laowu.mod.item;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** Diamond-equivalent mining stats, including diamond's 1561-point capacity. */
public final class CatToolTier {
    public static final Tier INSTANCE = new Tier() {
        @Override public int getUses() { return Tiers.DIAMOND.getUses(); }
        @Override public float getSpeed() { return Tiers.DIAMOND.getSpeed(); }
        @Override public float getAttackDamageBonus() { return Tiers.DIAMOND.getAttackDamageBonus(); }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() {
            return Tiers.DIAMOND.getIncorrectBlocksForDrops();
        }
        @Override public int getEnchantmentValue() { return Tiers.DIAMOND.getEnchantmentValue(); }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(LaoWuMod.CAT_INGOT.get()); }
    };

    private CatToolTier() {
    }
}
