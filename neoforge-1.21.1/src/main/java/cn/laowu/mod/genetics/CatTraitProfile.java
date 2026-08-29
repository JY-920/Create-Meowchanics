package cn.laowu.mod.genetics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/** Versioned, conflict-checked collection of at most four levelled traits. */
public final class CatTraitProfile {
    public static final int DATA_VERSION = 1;
    public static final int MAX_TRAITS = 4;
    private static final String VERSION_TAG = "Version";
    private static final String ENTRIES_TAG = "Traits";
    private static final String ID_TAG = "Id";
    private static final String LEVEL_TAG = "Level";

    public static final CatTraitProfile EMPTY = new CatTraitProfile(List.of());

    private final List<CatTraitInstance> traits;

    private CatTraitProfile(List<CatTraitInstance> traits) {
        this.traits = List.copyOf(traits);
    }

    /** Ordinary cats use a deliberately broad 0..4 trait-count distribution. */
    public static CatTraitProfile founder(RandomSource random) {
        return roll(random, false);
    }

    /** Spout-created cats always carry Doughy, even when the count roll is zero. */
    public static CatTraitProfile injected(RandomSource random) {
        return roll(random, true);
    }

    private static CatTraitProfile roll(RandomSource random, boolean forceDoughy) {
        int countRoll = random.nextInt(100);
        int desired = countRoll < 35 ? 0 : countRoll < 75 ? 1
                : countRoll < 93 ? 2 : countRoll < 99 ? 3 : 4;

        List<CatTraitInstance> selected = new ArrayList<>(MAX_TRAITS);
        List<CatTrait> available = new ArrayList<>(List.of(CatTrait.values()));
        if (forceDoughy) {
            selected.add(new CatTraitInstance(CatTrait.DOUGHY, 1));
            available.remove(CatTrait.DOUGHY);
            desired = Math.max(1, desired);
        }

        while (selected.size() < desired && !available.isEmpty()) {
            List<CatTrait> compatible = available.stream()
                    .filter(candidate -> compatibleWith(selected, candidate)).toList();
            if (compatible.isEmpty()) break;

            CatTrait chosen = chooseWeightedTrait(compatible, random);
            selected.add(new CatTraitInstance(chosen, 1));
            available.remove(chosen);
        }
        return new CatTraitProfile(selected);
    }

    /**
     * Player-readable inheritance contract:
     * shared parental traits are guaranteed, one-parent traits independently
     * have a 50% chance, and a successful mutation reserves one free slot for
     * a compatible trait absent from both parents. Every inherited or mutated
     * trait starts at level I; Doughy is never inherited.
     */
    public static CatTraitProfile breed(CatTraitProfile first,
                                        CatTraitProfile second,
                                        float mutationChance,
                                        RandomSource random) {
        CatTraitProfile father = first == null ? EMPTY : first;
        CatTraitProfile mother = second == null ? EMPTY : second;
        EnumSet<CatTrait> fatherTraits = EnumSet.noneOf(CatTrait.class);
        EnumSet<CatTrait> motherTraits = EnumSet.noneOf(CatTrait.class);
        father.traits.forEach(instance -> fatherTraits.add(instance.trait()));
        mother.traits.forEach(instance -> motherTraits.add(instance.trait()));

        List<CatTraitInstance> selected = new ArrayList<>(MAX_TRAITS);
        for (CatTrait trait : CatTrait.values()) {
            if (trait != CatTrait.DOUGHY && fatherTraits.contains(trait)
                    && motherTraits.contains(trait)
                    && compatibleWith(selected, trait)) {
                selected.add(new CatTraitInstance(trait, 1));
            }
        }

        boolean mutates = random.nextFloat()
                < Math.max(0.0F, Math.min(1.0F, mutationChance));
        int parentalLimit = mutates && selected.size() < MAX_TRAITS
                ? MAX_TRAITS - 1 : MAX_TRAITS;
        List<CatTrait> oneParentTraits = new ArrayList<>();
        for (CatTrait trait : CatTrait.values()) {
            if (trait == CatTrait.DOUGHY) continue;
            if (fatherTraits.contains(trait) ^ motherTraits.contains(trait)) {
                oneParentTraits.add(trait);
            }
        }
        List<CatTrait> passedInheritanceRoll = new ArrayList<>();
        for (CatTrait trait : oneParentTraits) {
            if (random.nextBoolean()) passedInheritanceRoll.add(trait);
        }
        shuffle(passedInheritanceRoll, random);
        for (CatTrait trait : passedInheritanceRoll) {
            if (selected.size() >= parentalLimit) break;
            if (compatibleWith(selected, trait)) {
                selected.add(new CatTraitInstance(trait, 1));
            }
        }

        if (mutates && selected.size() < MAX_TRAITS) {
            List<CatTrait> mutationPool = Arrays.stream(CatTrait.values())
                    .filter(trait -> !fatherTraits.contains(trait)
                            && !motherTraits.contains(trait))
                    .filter(trait -> compatibleWith(selected, trait))
                    .toList();
            if (!mutationPool.isEmpty()) {
                selected.add(new CatTraitInstance(
                        chooseWeightedTrait(mutationPool, random), 1));
            }
        }
        return selected.isEmpty() ? EMPTY : new CatTraitProfile(selected);
    }

    /** Select rarity first so adding traits does not silently reweight a tier. */
    private static CatTrait chooseWeightedTrait(List<CatTrait> compatible,
                                                RandomSource random) {
        List<CatTraitRarity> compatibleRarities = Arrays.stream(CatTraitRarity.values())
                .filter(rarity -> compatible.stream()
                        .anyMatch(candidate -> candidate.rarity() == rarity))
                .toList();
        int totalWeight = compatibleRarities.stream()
                .mapToInt(CatTraitRarity::generationWeight).sum();
        int roll = random.nextInt(Math.max(1, totalWeight));
        CatTraitRarity chosenRarity = compatibleRarities
                .get(compatibleRarities.size() - 1);
        for (CatTraitRarity rarity : compatibleRarities) {
            roll -= rarity.generationWeight();
            if (roll < 0) {
                chosenRarity = rarity;
                break;
            }
        }
        CatTraitRarity selectedRarity = chosenRarity;
        List<CatTrait> candidates = compatible.stream()
                .filter(candidate -> candidate.rarity() == selectedRarity)
                .toList();
        return candidates.get(random.nextInt(candidates.size()));
    }

    private static void shuffle(List<CatTrait> values, RandomSource random) {
        for (int index = values.size() - 1; index > 0; index--) {
            Collections.swap(values, index, random.nextInt(index + 1));
        }
    }

    private static boolean compatibleWith(List<CatTraitInstance> selected, CatTrait candidate) {
        EnumSet<CatTraitSlot> occupied = EnumSet.noneOf(CatTraitSlot.class);
        for (CatTraitInstance instance : selected) {
            if (instance.trait() == candidate) return false;
            occupied.addAll(instance.trait().occupiedSlots());
        }
        return Collections.disjoint(occupied, candidate.occupiedSlots());
    }

    public List<CatTraitInstance> traits() {
        return traits;
    }

    public boolean has(CatTrait trait) {
        return traits.stream().anyMatch(instance -> instance.trait() == trait);
    }

    public int level(CatTrait trait) {
        return traits.stream().filter(instance -> instance.trait() == trait)
                .mapToInt(CatTraitInstance::level).findFirst().orElse(0);
    }

    /**
     * Development/editor mutation that still enforces uniqueness, capacity and
     * future appearance/behaviour conflicts. A non-positive level removes the
     * trait; adding an incompatible fifth trait leaves the profile unchanged.
     */
    public CatTraitProfile withLevel(CatTrait trait, int level) {
        if (trait == null) return this;
        List<CatTraitInstance> edited = new ArrayList<>(traits);
        int existingIndex = -1;
        for (int index = 0; index < edited.size(); index++) {
            if (edited.get(index).trait() == trait) {
                existingIndex = index;
                break;
            }
        }

        if (level <= 0) {
            if (existingIndex < 0) return this;
            edited.remove(existingIndex);
            return edited.isEmpty() ? EMPTY : new CatTraitProfile(edited);
        }

        CatTraitInstance replacement = new CatTraitInstance(trait, level);
        if (existingIndex >= 0) {
            edited.set(existingIndex, replacement);
            return new CatTraitProfile(edited);
        }
        if (edited.size() >= MAX_TRAITS || !compatibleWith(edited, trait)) return this;
        edited.add(replacement);
        return new CatTraitProfile(edited);
    }

    public CompoundTag save() {
        CompoundTag root = new CompoundTag();
        root.putInt(VERSION_TAG, DATA_VERSION);
        ListTag entries = new ListTag();
        for (CatTraitInstance instance : traits) {
            CompoundTag entry = new CompoundTag();
            entry.putString(ID_TAG, instance.trait().id().toString());
            entry.putInt(LEVEL_TAG, instance.level());
            entries.add(entry);
        }
        root.put(ENTRIES_TAG, entries);
        return root;
    }

    public static Optional<CatTraitProfile> load(CompoundTag root) {
        if (root == null || !root.contains(VERSION_TAG, Tag.TAG_INT)
                || root.getInt(VERSION_TAG) != DATA_VERSION
                || !root.contains(ENTRIES_TAG, Tag.TAG_LIST)) return Optional.empty();

        ListTag entries = root.getList(ENTRIES_TAG, Tag.TAG_COMPOUND);
        List<CatTraitInstance> loaded = new ArrayList<>(MAX_TRAITS);
        for (int index = 0; index < entries.size() && loaded.size() < MAX_TRAITS; index++) {
            CompoundTag entry = entries.getCompound(index);
            ResourceLocation id = ResourceLocation.tryParse(entry.getString(ID_TAG));
            CatTrait trait = CatTrait.byId(id).orElse(null);
            if (trait == null || !compatibleWith(loaded, trait)) continue;
            loaded.add(new CatTraitInstance(trait, entry.getInt(LEVEL_TAG)));
        }
        return Optional.of(loaded.isEmpty() ? EMPTY : new CatTraitProfile(loaded));
    }

    public static boolean isCurrentVersion(CompoundTag root) {
        return root != null && root.contains(VERSION_TAG, Tag.TAG_INT)
                && root.getInt(VERSION_TAG) == DATA_VERSION;
    }
}
