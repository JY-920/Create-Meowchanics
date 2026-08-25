package cn.laowu.mod.genetics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A versioned six-dimensional profile containing current values and immutable
 * potential ceilings. Gameplay consumes these raw genes through
 * {@link CatAttributeEffects}, keeping temporary effects out of saved heredity.
 */
public final class CatAttributeProfile {
    public static final int DATA_VERSION = 3;
    public static final int MIN_VALUE = 0;
    public static final int MAX_VALUE = 100;
    private static final int FOUNDER_MAX_CURRENT = 100;

    private static final String VERSION_TAG = "Version";
    private static final String CURRENT_TAG = "Current";
    private static final String POTENTIAL_TAG = "Potential";

    private final Map<CatStat, Integer> current;
    private final Map<CatStat, Integer> potential;

    private CatAttributeProfile(EnumMap<CatStat, Integer> current,
                                EnumMap<CatStat, Integer> potential) {
        this.current = Collections.unmodifiableMap(current);
        this.potential = Collections.unmodifiableMap(potential);
    }

    /**
     * Non-bred cats roll every locus independently across all six visible
     * colour bands. The value 100 is the rare perfect tier. Limits are rolled
     * uniformly between the current value and the absolute maximum.
     */
    public static CatAttributeProfile founder(RandomSource random) {
        EnumMap<CatStat, Integer> current = new EnumMap<>(CatStat.class);
        EnumMap<CatStat, Integer> potential = new EnumMap<>(CatStat.class);
        for (CatStat stat : CatStat.values()) {
            rollRandomLocus(stat, current, potential, random);
        }
        return new CatAttributeProfile(current, potential);
    }

    /**
     * Compatibility entry point used by natural cat breeding. It keeps the
     * original five-locus behaviour but delegates to the food-aware engine.
     */
    public static CatAttributeProfile fuse(CatAttributeProfile first,
                                           CatAttributeProfile second,
                                           RandomSource random) {
        return breed(first, second, CatBreedingMode.SUPER, 0.0F, random);
    }

    /**
     * Food-driven Pokemon-style inheritance. Current value and Attribute Limit
     * always travel as one locus. Uninherited loci receive a full 0..100 roll;
     * a successful mutation adds a second roll and keeps the better candidate.
     */
    public static CatAttributeProfile breed(CatAttributeProfile first,
                                            CatAttributeProfile second,
                                            CatBreedingMode mode,
                                            float mutationChance,
                                            RandomSource random) {
        EnumMap<CatStat, Integer> current = new EnumMap<>(CatStat.class);
        EnumMap<CatStat, Integer> potential = new EnumMap<>(CatStat.class);
        EnumSet<CatStat> inherited = EnumSet.noneOf(CatStat.class);
        CatStat targeted = mode.targetedStat();
        if (targeted != null) inherited.add(targeted);

        List<CatStat> candidates = new ArrayList<>(List.of(CatStat.values()));
        if (targeted != null) candidates.remove(targeted);
        shuffle(candidates, random);
        int remaining = Math.max(0, mode.inheritedLoci() - inherited.size());
        for (int index = 0; index < remaining && index < candidates.size(); index++) {
            inherited.add(candidates.get(index));
        }

        for (CatStat stat : CatStat.values()) {
            if (inherited.contains(stat)) {
                if (stat == targeted) {
                    inheritTargetedLocus(stat, first, second, current, potential);
                } else {
                    inheritLocus(stat, random.nextBoolean() ? first : second,
                            current, potential);
                }
                continue;
            }

            Locus rolled = rollRandomLocus(random);
            if (random.nextFloat() < mutationChance) {
                rolled = better(rolled, rollRandomLocus(random));
            }
            current.put(stat, rolled.current());
            potential.put(stat, rolled.potential());
        }
        return new CatAttributeProfile(current, potential);
    }

    private static void rollRandomLocus(CatStat stat,
                                        EnumMap<CatStat, Integer> current,
                                        EnumMap<CatStat, Integer> potential,
                                        RandomSource random) {
        Locus locus = rollRandomLocus(random);
        current.put(stat, locus.current());
        potential.put(stat, locus.potential());
    }

    private static Locus rollRandomLocus(RandomSource random) {
        int ceiling = random.nextInt(MAX_VALUE + 1);
        int value = random.nextInt(Math.min(FOUNDER_MAX_CURRENT, ceiling) + 1);
        return new Locus(value, ceiling);
    }

    private static Locus better(Locus first, Locus second) {
        if (second.potential() != first.potential()) {
            return second.potential() > first.potential() ? second : first;
        }
        return second.current() > first.current() ? second : first;
    }

    private static void inheritTargetedLocus(CatStat stat,
                                             CatAttributeProfile first,
                                             CatAttributeProfile second,
                                             EnumMap<CatStat, Integer> current,
                                             EnumMap<CatStat, Integer> potential) {
        CatAttributeProfile donor = first.potential(stat) > second.potential(stat) ? first
                : second.potential(stat) > first.potential(stat) ? second
                : first.current(stat) >= second.current(stat) ? first : second;
        int ceiling = donor.potential(stat);
        int value = donor.current(stat);
        int firstCeiling = first.potential(stat);
        int secondCeiling = second.potential(stat);
        if (firstCeiling >= 90 && secondCeiling >= 90
                && Math.abs(firstCeiling - secondCeiling) <= 5) {
            ceiling = Math.min(MAX_VALUE, Math.max(firstCeiling, secondCeiling) + 1);
            // A targeted breakthrough must be visible in the ordinary NOW
            // panel as well as the crouched MAX panel. Advance the inherited
            // current value by the same single step without exceeding its new
            // limit; otherwise two 91/91 parents appeared to produce 91 again.
            value = Math.min(ceiling, value + 1);
        }
        current.put(stat, Math.min(value, ceiling));
        potential.put(stat, ceiling);
    }

    private static void shuffle(List<CatStat> values, RandomSource random) {
        for (int index = values.size() - 1; index > 0; index--) {
            Collections.swap(values, index, random.nextInt(index + 1));
        }
    }

    private static void inheritLocus(CatStat stat, CatAttributeProfile parent,
                                     EnumMap<CatStat, Integer> current,
                                     EnumMap<CatStat, Integer> potential) {
        current.put(stat, parent.current(stat));
        potential.put(stat, parent.potential(stat));
    }

    public int current(CatStat stat) {
        return current.get(stat);
    }

    public int potential(CatStat stat) {
        return potential.get(stat);
    }

    /** Immutable editor operation used by the development attribute wand. */
    public CatAttributeProfile withValues(CatStat stat, int value, int ceiling) {
        int clampedCeiling = Mth.clamp(ceiling, MIN_VALUE, MAX_VALUE);
        int clampedValue = Mth.clamp(value, MIN_VALUE, clampedCeiling);
        EnumMap<CatStat, Integer> editedCurrent = new EnumMap<>(current);
        EnumMap<CatStat, Integer> editedPotential = new EnumMap<>(potential);
        editedCurrent.put(stat, clampedValue);
        editedPotential.put(stat, clampedCeiling);
        return new CatAttributeProfile(editedCurrent, editedPotential);
    }

    public CompoundTag save() {
        CompoundTag root = new CompoundTag();
        root.putInt(VERSION_TAG, DATA_VERSION);
        CompoundTag currentValues = new CompoundTag();
        CompoundTag potentialValues = new CompoundTag();
        for (CatStat stat : CatStat.values()) {
            currentValues.putInt(stat.serializedName(), current(stat));
            potentialValues.putInt(stat.serializedName(), potential(stat));
        }
        root.put(CURRENT_TAG, currentValues);
        root.put(POTENTIAL_TAG, potentialValues);
        return root;
    }

    public static Optional<CatAttributeProfile> load(CompoundTag root) {
        if (root == null
                || !root.contains(CURRENT_TAG, Tag.TAG_COMPOUND)
                || !root.contains(POTENTIAL_TAG, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        int version = root.contains(VERSION_TAG, Tag.TAG_INT) ? root.getInt(VERSION_TAG) : 1;
        if (version < 1 || version > DATA_VERSION) return Optional.empty();

        CompoundTag currentValues = root.getCompound(CURRENT_TAG);
        CompoundTag potentialValues = root.getCompound(POTENTIAL_TAG);
        EnumMap<CatStat, Integer> current = new EnumMap<>(CatStat.class);
        EnumMap<CatStat, Integer> potential = new EnumMap<>(CatStat.class);
        for (CatStat stat : CatStat.values()) {
            String key = stat.serializedName();
            if (!currentValues.contains(key, Tag.TAG_INT)
                    || !potentialValues.contains(key, Tag.TAG_INT)) {
                return Optional.empty();
            }
            int rawValue = currentValues.getInt(key);
            int rawCeiling = potentialValues.getInt(key);
            int value;
            int ceiling;
            if (version == 1) {
                // V1 founders were restricted to 24..60, which concentrated
                // nearly every cat in two adjacent colour bands. Expand that
                // interval deterministically over the new 0..100 range while
                // preserving the original amount of growth room.
                value = Math.round((Mth.clamp(rawValue, 24, 60) - 24)
                        * (FOUNDER_MAX_CURRENT / 36.0F));
                int growthRoom = Mth.clamp(rawCeiling - rawValue, 0, 40);
                ceiling = Math.min(MAX_VALUE, value + growthRoom);
            } else {
                // V2 briefly allowed values up to 150. V3 restores the design
                // contract that 100 is the absolute maximum and rewrites the
                // clamped profile the next time the server touches the cat.
                value = Mth.clamp(rawValue, MIN_VALUE, MAX_VALUE);
                ceiling = Mth.clamp(rawCeiling, value, MAX_VALUE);
            }
            current.put(stat, value);
            potential.put(stat, ceiling);
        }
        return Optional.of(new CatAttributeProfile(current, potential));
    }

    public static boolean isCurrentVersion(CompoundTag root) {
        return root != null
                && root.contains(VERSION_TAG, Tag.TAG_INT)
                && root.getInt(VERSION_TAG) == DATA_VERSION;
    }

    private record Locus(int current, int potential) {}
}
