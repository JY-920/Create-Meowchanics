package cn.laowu.mod.item;

import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.genetics.CatAttributeEffects;
import cn.laowu.mod.genetics.CatAttributeProfile;
import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;

/** Immutable, NBT-backed ranges used by the cat filter and its Create wrapper. */
public final class CatFilterRules {
    public static final int CURRENT_PAGE = 0;
    public static final int POTENTIAL_PAGE = 1;
    public static final int STAT_COUNT = CatStat.values().length;
    public static final int MIN_VALUE = CatAttributeProfile.MIN_VALUE;
    /** Effective attributes may contain trait, outfit and future accessory bonuses. */
    public static final int MAX_CURRENT_VALUE = 999;
    /** Heritable current values and Attribute Limits remain on the 0..100 scale. */
    public static final int MAX_POTENTIAL_VALUE = CatAttributeProfile.MAX_VALUE;

    private static final String ROOT_TAG = "LaoWuCatFilter";
    private static final String VERSION_TAG = "Version";
    private static final int DATA_VERSION = 1;
    private static final String CURRENT_MIN_TAG = "CurrentMin";
    private static final String CURRENT_MAX_TAG = "CurrentMax";
    private static final String POTENTIAL_MIN_TAG = "PotentialMin";
    private static final String POTENTIAL_MAX_TAG = "PotentialMax";
    private static final String REQUIRED_TRAITS_TAG = "RequiredTraits";
    /** Read-only migration key from the first single-trait implementation. */
    private static final String REQUIRED_TRAIT_TAG = "RequiredTrait";
    private static final String GROWTH_TAG = "Growth";
    private static final String OWNERSHIP_TAG = "Ownership";
    private static final String CAREER_TAG = "Career";
    private static final String CAT_NAME_TAG = "CatName";
    public static final int MAX_NAME_LENGTH = 50;

    private final int[] currentMin;
    private final int[] currentMax;
    private final int[] potentialMin;
    private final int[] potentialMax;
    private final List<CatTrait> requiredTraits;
    private final GrowthFilter growth;
    private final OwnershipFilter ownership;
    private final CareerFilter career;
    private final String catName;

    private CatFilterRules(int[] currentMin, int[] currentMax,
                           int[] potentialMin, int[] potentialMax,
                           List<CatTrait> requiredTraits,
                           GrowthFilter growth, OwnershipFilter ownership,
                           CareerFilter career, String catName) {
        this.currentMin = currentMin;
        this.currentMax = currentMax;
        this.potentialMin = potentialMin;
        this.potentialMax = potentialMax;
        this.requiredTraits = sanitizeTraits(requiredTraits);
        this.growth = growth == null ? GrowthFilter.ANY : growth;
        this.ownership = ownership == null ? OwnershipFilter.ANY : ownership;
        this.career = career == null ? CareerFilter.ANY : career;
        this.catName = cleanName(catName);
    }

    public static CatFilterRules read(ItemStack stack) {
        CompoundTag root = stack.getTag();
        CompoundTag filter = root != null && root.contains(ROOT_TAG, Tag.TAG_COMPOUND)
                ? root.getCompound(ROOT_TAG) : new CompoundTag();
        int[] currentMin = readArray(filter, CURRENT_MIN_TAG, MIN_VALUE);
        int[] currentMax = readArray(filter, CURRENT_MAX_TAG, MAX_CURRENT_VALUE);
        int[] potentialMin = readArray(filter, POTENTIAL_MIN_TAG, MIN_VALUE);
        int[] potentialMax = readArray(filter, POTENTIAL_MAX_TAG, MAX_POTENTIAL_VALUE);
        if (!filter.contains(VERSION_TAG, Tag.TAG_INT)) {
            migrateLegacyCurrentDefaults(currentMin, currentMax);
        }
        normalize(currentMin, currentMax, MAX_CURRENT_VALUE);
        normalize(potentialMin, potentialMax, MAX_POTENTIAL_VALUE);
        List<CatTrait> requiredTraits = readTraits(filter);
        return new CatFilterRules(currentMin, currentMax, potentialMin, potentialMax,
                requiredTraits,
                GrowthFilter.byId(filter.getString(GROWTH_TAG)),
                OwnershipFilter.byId(filter.getString(OWNERSHIP_TAG)),
                CareerFilter.byId(filter.getString(CAREER_TAG)),
                filter.getString(CAT_NAME_TAG));
    }

    public static CatFilterRules fromValues(int[] currentMin, int[] currentMax,
                                            int[] potentialMin, int[] potentialMax,
                                            List<CatTrait> requiredTraits) {
        int[] safeCurrentMin = copyOrDefault(currentMin, MIN_VALUE);
        int[] safeCurrentMax = copyOrDefault(currentMax, MAX_CURRENT_VALUE);
        int[] safePotentialMin = copyOrDefault(potentialMin, MIN_VALUE);
        int[] safePotentialMax = copyOrDefault(potentialMax, MAX_POTENTIAL_VALUE);
        normalize(safeCurrentMin, safeCurrentMax, MAX_CURRENT_VALUE);
        normalize(safePotentialMin, safePotentialMax, MAX_POTENTIAL_VALUE);
        return new CatFilterRules(safeCurrentMin, safeCurrentMax,
                safePotentialMin, safePotentialMax, requiredTraits,
                GrowthFilter.ANY, OwnershipFilter.ANY, CareerFilter.ANY, "");
    }

    public static CatFilterRules fromValues(int[] currentMin, int[] currentMax,
                                            int[] potentialMin, int[] potentialMax,
                                            List<CatTrait> requiredTraits,
                                            GrowthFilter growth,
                                            OwnershipFilter ownership,
                                            CareerFilter career,
                                            String catName) {
        int[] safeCurrentMin = copyOrDefault(currentMin, MIN_VALUE);
        int[] safeCurrentMax = copyOrDefault(currentMax, MAX_CURRENT_VALUE);
        int[] safePotentialMin = copyOrDefault(potentialMin, MIN_VALUE);
        int[] safePotentialMax = copyOrDefault(potentialMax, MAX_POTENTIAL_VALUE);
        normalize(safeCurrentMin, safeCurrentMax, MAX_CURRENT_VALUE);
        normalize(safePotentialMin, safePotentialMax, MAX_POTENTIAL_VALUE);
        return new CatFilterRules(safeCurrentMin, safeCurrentMax,
                safePotentialMin, safePotentialMax, requiredTraits,
                growth, ownership, career, catName);
    }

    public void write(ItemStack stack) {
        CompoundTag filter = new CompoundTag();
        filter.putInt(VERSION_TAG, DATA_VERSION);
        filter.putIntArray(CURRENT_MIN_TAG, currentMin);
        filter.putIntArray(CURRENT_MAX_TAG, currentMax);
        filter.putIntArray(POTENTIAL_MIN_TAG, potentialMin);
        filter.putIntArray(POTENTIAL_MAX_TAG, potentialMax);
        if (!requiredTraits.isEmpty()) {
            ListTag traits = new ListTag();
            requiredTraits.forEach(trait -> traits.add(
                    StringTag.valueOf(trait.id().toString())));
            filter.put(REQUIRED_TRAITS_TAG, traits);
        }
        if (growth != GrowthFilter.ANY) filter.putString(GROWTH_TAG, growth.id());
        if (ownership != OwnershipFilter.ANY) {
            filter.putString(OWNERSHIP_TAG, ownership.id());
        }
        if (career != CareerFilter.ANY) filter.putString(CAREER_TAG, career.id());
        if (!catName.isEmpty()) filter.putString(CAT_NAME_TAG, catName);
        stack.getOrCreateTag().put(ROOT_TAG, filter);
    }

    public int min(int page, CatStat stat) {
        return (page == POTENTIAL_PAGE ? potentialMin : currentMin)[stat.ordinal()];
    }

    public int max(int page, CatStat stat) {
        return (page == POTENTIAL_PAGE ? potentialMax : currentMax)[stat.ordinal()];
    }

    public List<CatTrait> requiredTraits() {
        return requiredTraits;
    }

    public GrowthFilter growth() {
        return growth;
    }

    public OwnershipFilter ownership() {
        return ownership;
    }

    public CareerFilter career() {
        return career;
    }

    public String catName() {
        return catName;
    }

    public boolean matches(CatAttributeProfile profile, CatTraitProfile traits,
                           boolean night, boolean day) {
        CatTraitProfile resolvedTraits = traits == null
                ? CatTraitProfile.EMPTY : traits;
        for (CatStat stat : CatStat.values()) {
            int index = stat.ordinal();
            int current = CatAttributeEffects.effectiveValue(
                    profile, resolvedTraits, stat, night, day);
            int potential = profile.potential(stat);
            if (current < currentMin[index] || current > currentMax[index]
                    || potential < potentialMin[index] || potential > potentialMax[index]) {
                return false;
            }
        }
        return requiredTraits.stream().allMatch(resolvedTraits::has);
    }

    public boolean matches(CatAttributeProfile profile) {
        return matches(profile, CatTraitProfile.EMPTY, false, false);
    }

    public boolean matches(CatAttributeProfile profile, CatTraitProfile traits) {
        return matches(profile, traits, false, false);
    }

    public boolean matches(ItemStack stack, CatAttributeProfile profile,
                           CatTraitProfile traits) {
        return matches(profile, traits) && matchesIdentity(stack);
    }

    public boolean matches(ItemStack stack, CatAttributeProfile profile,
                           CatTraitProfile traits, boolean night, boolean day) {
        return matches(profile, traits, night, day)
                && matchesIdentity(stack);
    }

    /** Categorical identity conditions are exact; numeric ranges remain ranges. */
    public boolean matchesIdentity(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!growth.matches(stack) || !ownership.matches(stack)
                || !career.matches(stack)) return false;
        return catName.isEmpty()
                || catName.equalsIgnoreCase(CatPancakeItem.customCatName(stack));
    }

    public boolean isDefault() {
        for (CatStat stat : CatStat.values()) {
            int index = stat.ordinal();
            if (currentMin[index] != MIN_VALUE
                    || currentMax[index] != MAX_CURRENT_VALUE
                    || potentialMin[index] != MIN_VALUE
                    || potentialMax[index] != MAX_POTENTIAL_VALUE) {
                return false;
            }
        }
        return requiredTraits.isEmpty()
                && growth == GrowthFilter.ANY
                && ownership == OwnershipFilter.ANY
                && career == CareerFilter.ANY
                && catName.isEmpty();
    }

    /**
     * Produces a stable ordering for advanced breeding-box parent replacement.
     * Breeding selects genes, so only Attribute Limits are ranked; configured
     * Current Attribute ranges remain useful to ordinary item filtering but do
     * not make a trained parent genetically better here. Matching a configured
     * Limit range is more important than merely being close to its target, and
     * matching a requested trait is more important than either. Within a range
     * its configured maximum is the ideal value, so 60..60 prefers 60 and
     * 50..80 prefers 80. Every unconfigured Limit targets 100, which also makes
     * an absent or untouched filter rank parents by their six Limits alone.
     */
    public OptionalLong replacementScore(CatAttributeProfile profile,
                                         CatTraitProfile traits) {
        if (profile == null) return OptionalLong.empty();

        long score = 0L;
        for (CatStat stat : CatStat.values()) {
            int minimum = min(POTENTIAL_PAGE, stat);
            int maximum = max(POTENTIAL_PAGE, stat);
            int value = profile.potential(stat);
            if (minimum == MIN_VALUE && maximum == MAX_POTENTIAL_VALUE) {
                score += value;
                continue;
            }

            if (value >= minimum && value <= maximum) score += 10_000L;
            score += MAX_POTENTIAL_VALUE - Math.abs(value - maximum);
        }

        CatTraitProfile safeTraits = traits == null ? CatTraitProfile.EMPTY : traits;
        for (CatTrait trait : requiredTraits) {
            if (safeTraits.has(trait)) score += 1_000_000L;
        }
        return OptionalLong.of(score);
    }

    private static List<CatTrait> readTraits(CompoundTag filter) {
        List<CatTrait> result = new ArrayList<>(CatTraitProfile.MAX_TRAITS);
        if (filter.contains(REQUIRED_TRAITS_TAG, Tag.TAG_LIST)) {
            ListTag traits = filter.getList(REQUIRED_TRAITS_TAG, Tag.TAG_STRING);
            for (int index = 0;
                 index < traits.size() && result.size() < CatTraitProfile.MAX_TRAITS;
                 index++) {
                CatTrait.byId(ResourceLocation.tryParse(traits.getString(index)))
                        .filter(trait -> !result.contains(trait))
                        .ifPresent(result::add);
            }
        } else if (filter.contains(REQUIRED_TRAIT_TAG, Tag.TAG_STRING)) {
            CatTrait.byId(ResourceLocation.tryParse(filter.getString(REQUIRED_TRAIT_TAG)))
                    .ifPresent(result::add);
        }
        return result;
    }

    private static List<CatTrait> sanitizeTraits(List<CatTrait> traits) {
        if (traits == null || traits.isEmpty()) return List.of();
        EnumSet<CatTrait> unique = EnumSet.noneOf(CatTrait.class);
        List<CatTrait> result = new ArrayList<>(CatTraitProfile.MAX_TRAITS);
        for (CatTrait trait : traits) {
            if (trait != null && unique.add(trait)) result.add(trait);
            if (result.size() >= CatTraitProfile.MAX_TRAITS) break;
        }
        return List.copyOf(result);
    }

    public static String cleanName(String requested) {
        String cleaned = requested == null ? "" : requested.trim();
        return cleaned.length() <= MAX_NAME_LENGTH
                ? cleaned : cleaned.substring(0, MAX_NAME_LENGTH);
    }

    public enum GrowthFilter {
        ANY("any"), ADULT("adult"), KITTEN("kitten");

        private final String id;

        GrowthFilter(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public boolean matches(ItemStack stack) {
            return this == ANY || CatPancakeItem.isBaby(stack) == (this == KITTEN);
        }

        public static GrowthFilter byId(String id) {
            return CatFilterRules.byId(values(), id, ANY);
        }
    }

    public enum OwnershipFilter {
        ANY("any"), OWNED("owned"), UNOWNED("unowned");

        private final String id;

        OwnershipFilter(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public boolean matches(ItemStack stack) {
            return this == ANY || CatPancakeItem.hasOwner(stack) == (this == OWNED);
        }

        public static OwnershipFilter byId(String id) {
            return CatFilterRules.byId(values(), id, ANY);
        }
    }

    public enum CareerFilter {
        ANY("any", null),
        NO_CAREER("none", CatOutfitType.NONE),
        ANY_CAREER("any_career", null),
        TERMINATOR("terminator", CatOutfitType.TERMINATOR),
        FISHING("fishing", CatOutfitType.FISHING),
        FLIGHT("flight", CatOutfitType.FLIGHT),
        FIRE("fire", CatOutfitType.FIRE),
        HONEY("honey", CatOutfitType.HONEY),
        TRANSPORT("transport", CatOutfitType.TRANSPORT),
        DYNAMITE("dynamite", CatOutfitType.DYNAMITE);

        private final String id;
        private final CatOutfitType outfit;

        CareerFilter(String id, CatOutfitType outfit) {
            this.id = id;
            this.outfit = outfit;
        }

        public String id() {
            return id;
        }

        public boolean matches(ItemStack stack) {
            CatOutfitType actual = CatPancakeItem.getOutfit(stack);
            if (this == ANY) return true;
            if (this == ANY_CAREER) return actual != CatOutfitType.NONE;
            return actual == outfit;
        }

        public static CareerFilter byId(String id) {
            return CatFilterRules.byId(values(), id, ANY);
        }
    }

    private static <E extends Enum<E>> E byId(E[] values, String id, E fallback) {
        if (id == null || id.isBlank()) return fallback;
        String normalized = id.toLowerCase(Locale.ROOT);
        for (E value : values) {
            String serialized;
            if (value instanceof GrowthFilter growth) serialized = growth.id();
            else if (value instanceof OwnershipFilter ownership) serialized = ownership.id();
            else if (value instanceof CareerFilter career) serialized = career.id();
            else continue;
            if (serialized.equals(normalized)) return value;
        }
        return fallback;
    }

    private static int[] readArray(CompoundTag tag, String key, int fallback) {
        return tag.contains(key, Tag.TAG_INT_ARRAY)
                ? copyOrDefault(tag.getIntArray(key), fallback)
                : filled(fallback);
    }

    private static int[] copyOrDefault(int[] values, int fallback) {
        if (values == null || values.length != STAT_COUNT) return filled(fallback);
        return values.clone();
    }

    private static int[] filled(int value) {
        int[] result = new int[STAT_COUNT];
        java.util.Arrays.fill(result, value);
        return result;
    }

    public static int maxValue(int page) {
        return page == POTENTIAL_PAGE ? MAX_POTENTIAL_VALUE : MAX_CURRENT_VALUE;
    }

    private static void migrateLegacyCurrentDefaults(int[] minimum, int[] maximum) {
        for (int index = 0; index < STAT_COUNT; index++) {
            if (minimum[index] == MIN_VALUE
                    && maximum[index] == MAX_POTENTIAL_VALUE) {
                maximum[index] = MAX_CURRENT_VALUE;
            }
        }
    }

    private static void normalize(int[] minimum, int[] maximum, int allowedMaximum) {
        for (int index = 0; index < STAT_COUNT; index++) {
            minimum[index] = Mth.clamp(minimum[index], MIN_VALUE, allowedMaximum);
            maximum[index] = Mth.clamp(maximum[index], minimum[index], allowedMaximum);
        }
    }
}
