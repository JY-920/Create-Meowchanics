package cn.laowu.mod.genetics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * A versioned six-dimensional profile containing current values and immutable
 * potential ceilings. Values are descriptive genes for now; gameplay effects
 * can consume them later without changing the saved format.
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
     * Pokemon-style breeding: five complete loci are inherited from randomly
     * selected parents while the sixth is a fresh roll. Current value and
     * Attribute Limit always travel together as one indivisible locus.
     */
    public static CatAttributeProfile fuse(CatAttributeProfile first,
                                           CatAttributeProfile second,
                                           RandomSource random) {
        CatAttributeProfile randomProfile = founder(random);
        EnumMap<CatStat, Integer> current = new EnumMap<>(CatStat.class);
        EnumMap<CatStat, Integer> potential = new EnumMap<>(CatStat.class);
        CatStat[] stats = CatStat.values();
        int randomIndex = random.nextInt(stats.length);
        boolean[] fromFirst = new boolean[stats.length];
        int inheritedFromFirst = 0;
        for (int index = 0; index < stats.length; index++) {
            if (index == randomIndex) continue;
            fromFirst[index] = random.nextBoolean();
            if (fromFirst[index]) inheritedFromFirst++;
        }

        // "From both parents" is a firm contract: avoid the rare all-five
        // result from only one parent without biasing which actual loci win.
        if (inheritedFromFirst == 0 || inheritedFromFirst == stats.length - 1) {
            int forcedIndex = random.nextInt(stats.length - 1);
            if (forcedIndex >= randomIndex) forcedIndex++;
            fromFirst[forcedIndex] = inheritedFromFirst == 0;
        }

        for (int index = 0; index < stats.length; index++) {
            CatStat stat = stats[index];
            if (index == randomIndex) {
                inheritLocus(stat, randomProfile, current, potential);
            } else {
                inheritLocus(stat, fromFirst[index] ? first : second, current, potential);
            }
        }
        return new CatAttributeProfile(current, potential);
    }

    private static void rollRandomLocus(CatStat stat,
                                        EnumMap<CatStat, Integer> current,
                                        EnumMap<CatStat, Integer> potential,
                                        RandomSource random) {
        int value = random.nextInt(FOUNDER_MAX_CURRENT + 1);
        int ceiling = value + random.nextInt(MAX_VALUE - value + 1);
        current.put(stat, value);
        potential.put(stat, ceiling);
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
}
