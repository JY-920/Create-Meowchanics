package cn.laowu.mod.genetics;

import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Extra trait-conditioned rolls appended to vanilla's own morning gift. */
public final class CatMorningGiftRewards {
    public static void giveBonusGifts(Cat cat) {
        if (cat.level().isClientSide || !cat.isAlive()) return;
        CatTraitProfile traits = CatTraitData.ensure(cat);

        CatXiaotingRewards.tryDropTemplate(cat);

        if (traits.has(CatTrait.DORAEMON) && cat.getRandom().nextFloat() < 0.35F) {
            cat.spawnAtLocation(rollRareGift(cat));
        }
    }

    private static ItemStack rollRareGift(Cat cat) {
        int roll = cat.getRandom().nextInt(100);
        if (roll < 30) return new ItemStack(Items.DIAMOND);
        if (roll < 55) return new ItemStack(Items.EMERALD, 2 + cat.getRandom().nextInt(3));
        if (roll < 75) return new ItemStack(Items.NAME_TAG);
        if (roll < 87) return new ItemStack(Items.GOLDEN_APPLE);
        if (roll < 95) return new ItemStack(Items.NAUTILUS_SHELL);
        if (roll < 98) return new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
        return new ItemStack(Items.TOTEM_OF_UNDYING);
    }

    private CatMorningGiftRewards() {}
}
