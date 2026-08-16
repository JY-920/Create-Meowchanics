package cn.laowu.mod.item;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;

import java.util.List;

/** Diamond-equivalent mining stats, including diamond's 1561-point capacity. */
public final class CatToolTier {
    public static final Tier INSTANCE = TierSortingRegistry.registerTier(
            new ForgeTier(3, net.minecraft.world.item.Tiers.DIAMOND.getUses(),
                    8.0F, 3.0F, 10,
                    BlockTags.NEEDS_DIAMOND_TOOL,
                    () -> Ingredient.of(LaoWuMod.CAT_INGOT.get())),
            LaoWuMod.id("cat"),
            List.of(net.minecraft.world.item.Tiers.DIAMOND),
            List.of(net.minecraft.world.item.Tiers.NETHERITE));

    private CatToolTier() {
    }
}
