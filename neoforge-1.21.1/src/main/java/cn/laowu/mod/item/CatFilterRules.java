package cn.laowu.mod.item;

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

/** Immutable, NBT-backed ranges used by the cat filter and its Create wrapper. */
public final class CatFilterRules {
    public static final int CURRENT_PAGE = 0;
    public static final int POTENTIAL_PAGE = 1;
    public static final int STAT_COUNT = CatStat.values().length;
    public static final int MIN_VALUE = CatAttributeProfile.MIN_VALUE;
    public static final int MAX_VALUE = CatAttributeProfile.MAX_VALUE;

    private static final String ROOT_TAG = "LaoWuCatFilter";
    private static final String CURRENT_MIN_TAG = "CurrentMin";
    private static final String CURRENT_MAX_TAG = "CurrentMax";
    private static final String POTENTIAL_MIN_TAG = "PotentialMin";
    private static final String POTENTIAL_MAX_TAG = "PotentialMax";
    private static final String REQUIRED_TRAITS_TAG = "RequiredTraits";
    /** Read-only migration key from the first single-trait implementation. */
    private static final String REQUIRED_TRAIT_TAG = "RequiredTrait";

    private final int[] currentMin;
    private final int[] currentMax;
    private final int[] potentialMin;
    private final int[] potentialMax;
    private final List<CatTrait> requiredTraits;

    private CatFilterRules(int[] currentMin, int[] currentMax,
                           int[] potentialMin, int[] potentialMax,
                           List<CatTrait> requiredTraits) {
        this.currentMin = currentMin;
        this.currentMax = currentMax;
        this.potentialMin = potentialMin;
        this.potentialMax = potentialMax;
        this.requiredTraits = sanitizeTraits(requiredTraits);
    }

    public static CatFilterRules read(ItemStack stack) {
        CompoundTag root = ItemCustomData.copy(stack);
        CompoundTag filter = root.contains(ROOT_TAG, Tag.TAG_COMPOUND)
                ? root.getCompound(ROOT_TAG) : new CompoundTag();
        int[] currentMin = readArray(filter, CURRENT_MIN_TAG, MIN_VALUE);
        int[] currentMax = readArray(filter, CURRENT_MAX_TAG, MAX_VALUE);
        int[] potentialMin = readArray(filter, POTENTIAL_MIN_TAG, MIN_VALUE);
        int[] potentialMax = readArray(filter, POTENTIAL_MAX_TAG, MAX_VALUE);
        normalize(currentMin, currentMax);
        normalize(potentialMin, potentialMax);
        List<CatTrait> requiredTraits = readTraits(filter);
        return new CatFilterRules(currentMin, currentMax, potentialMin, potentialMax,
                requiredTraits);
    }

    public static CatFilterRules fromValues(int[] currentMin, int[] currentMax,
                                            int[] potentialMin, int[] potentialMax,
                                            List<CatTrait> requiredTraits) {
        int[] safeCurrentMin = copyOrDefault(currentMin, MIN_VALUE);
        int[] safeCurrentMax = copyOrDefault(currentMax, MAX_VALUE);
        int[] safePotentialMin = copyOrDefault(potentialMin, MIN_VALUE);
        int[] safePotentialMax = copyOrDefault(potentialMax, MAX_VALUE);
        normalize(safeCurrentMin, safeCurrentMax);
        normalize(safePotentialMin, safePotentialMax);
        return new CatFilterRules(safeCurrentMin, safeCurrentMax,
                safePotentialMin, safePotentialMax, requiredTraits);
    }

    public void write(ItemStack stack) {
        CompoundTag filter = new CompoundTag();
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
        ItemCustomData.update(stack, tag -> tag.put(ROOT_TAG, filter));
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

    public boolean matches(CatAttributeProfile profile) {
        for (CatStat stat : CatStat.values()) {
            int index = stat.ordinal();
            int current = profile.current(stat);
            int potential = profile.potential(stat);
            if (current < currentMin[index] || current > currentMax[index]
                    || potential < potentialMin[index] || potential > potentialMax[index]) {
                return false;
            }
        }
        return true;
    }

    public boolean matches(CatAttributeProfile profile, CatTraitProfile traits) {
        return matches(profile) && requiredTraits.stream()
                .allMatch(trait -> traits != null && traits.has(trait));
    }

    public boolean isDefault() {
        for (CatStat stat : CatStat.values()) {
            int index = stat.ordinal();
            if (currentMin[index] != MIN_VALUE || currentMax[index] != MAX_VALUE
                    || potentialMin[index] != MIN_VALUE || potentialMax[index] != MAX_VALUE) {
                return false;
            }
        }
        return requiredTraits.isEmpty();
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

    private static void normalize(int[] minimum, int[] maximum) {
        for (int index = 0; index < STAT_COUNT; index++) {
            minimum[index] = Mth.clamp(minimum[index], MIN_VALUE, MAX_VALUE);
            maximum[index] = Mth.clamp(maximum[index], minimum[index], MAX_VALUE);
        }
    }
}
