package cn.laowu.mod.create;

/** Immutable machine properties shared by blocks, menus and client rendering. */
public enum BreedingBoxTier {
    BASIC(1, 20 * 60 * 2, 0.10F, "basic_breeding_box"),
    INTERMEDIATE(2, 20 * 90, 0.15F, "intermediate_breeding_box"),
    ADVANCED(3, 20 * 60, 0.20F, "advanced_breeding_box");

    private final int level;
    private final int durationTicks;
    private final float mutationChance;
    private final String serializedName;

    BreedingBoxTier(int level, int durationTicks, float mutationChance,
                    String serializedName) {
        this.level = level;
        this.durationTicks = durationTicks;
        this.mutationChance = mutationChance;
        this.serializedName = serializedName;
    }

    public int level() {
        return level;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public float mutationChance() {
        return mutationChance;
    }

    public String serializedName() {
        return serializedName;
    }

    public static BreedingBoxTier byOrdinal(int ordinal) {
        BreedingBoxTier[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : BASIC;
    }
}
