package cn.laowu.mod.genetics;

/** The inheritance contract selected by the food in a breeding box. */
public enum CatBreedingMode {
    NORMAL(5, null, 0.0F),
    SUPER(5, null, 0.0F),
    MUTATION(5, null, 0.40F),
    ATTACK(5, CatStat.ATTACK, 0.0F),
    HEALTH(5, CatStat.HEALTH, 0.0F),
    SPEED(5, CatStat.SPEED, 0.0F),
    STAMINA(5, CatStat.STAMINA, 0.0F),
    INTELLIGENCE(5, CatStat.INTELLIGENCE, 0.0F),
    LUCK(5, CatStat.LUCK, 0.0F);

    private final int inheritedLoci;
    private final CatStat targetedStat;
    private final float mutationBonus;

    CatBreedingMode(int inheritedLoci, CatStat targetedStat, float mutationBonus) {
        this.inheritedLoci = inheritedLoci;
        this.targetedStat = targetedStat;
        this.mutationBonus = mutationBonus;
    }

    public int inheritedLoci() {
        return inheritedLoci;
    }

    public CatStat targetedStat() {
        return targetedStat;
    }

    public float mutationBonus() {
        return mutationBonus;
    }

    /** Only newly mutated traits use these weights; parental inheritance is unchanged. */
    public int mutationTraitWeight(CatTraitRarity rarity) {
        if (this != MUTATION) return rarity.generationWeight();
        return switch (rarity) {
            case DEFECT -> 10;
            case COMMON -> 25;
            case GOOD -> 45;
            case EXCELLENT -> 20;
        };
    }
}
