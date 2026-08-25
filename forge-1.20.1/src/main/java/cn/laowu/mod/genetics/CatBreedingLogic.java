package cn.laowu.mod.genetics;

import net.minecraft.util.Mth;

/** Shared mutation-rate calculation for machines, screens and debug tools. */
public final class CatBreedingLogic {
    /** Luck 100 raises the current machine/food chance by one third. */
    private static final float LUCK_DIVISOR = 300.0F;

    public static float effectiveMutationChance(float boxChance, CatBreedingMode mode,
                                                CatAttributeProfile first,
                                                CatAttributeProfile second) {
        return effectiveMutationChance(boxChance, mode, first, CatTraitProfile.EMPTY,
                second, CatTraitProfile.EMPTY);
    }

    public static float effectiveMutationChance(float boxChance, CatBreedingMode mode,
                                                CatAttributeProfile first,
                                                CatTraitProfile firstTraits,
                                                CatAttributeProfile second,
                                                CatTraitProfile secondTraits) {
        float firstLuck = first == null ? 0.0F
                : CatAttributeEffects.effectiveValue(first, firstTraits,
                CatStat.LUCK, false);
        float secondLuck = second == null ? 0.0F
                : CatAttributeEffects.effectiveValue(second, secondTraits,
                CatStat.LUCK, false);
        float averageLuck = Mth.clamp((firstLuck + secondLuck) * 0.5F,
                CatAttributeProfile.MIN_VALUE, CatAttributeProfile.MAX_VALUE);
        float foodAdjusted = boxChance + mode.mutationBonus();
        return Mth.clamp(foodAdjusted * (1.0F + averageLuck / LUCK_DIVISOR),
                0.0F, 1.0F);
    }

    public static int basisPoints(float chance) {
        return Mth.clamp(Math.round(chance * 10_000.0F), 0, 10_000);
    }

    private CatBreedingLogic() {}
}
