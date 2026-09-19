package cn.laowu.mod.genetics;

import cn.laowu.mod.ServerConfig;
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

    /** Spout-created cats carry Doughy unless the generation policy disables it. */
    public static CatTraitProfile injected(RandomSource random) {
        return roll(random, true);
    }

    private static CatTraitProfile roll(RandomSource random, boolean forceDoughy) {
        int countRoll = random.nextInt(100);
        int desired = countRoll < 35 ? 0 : countRoll < 75 ? 1
                : countRoll < 93 ? 2 : countRoll < 99 ? 3 : 4;

        List<CatTraitInstance> selected = new ArrayList<>(MAX_TRAITS);
        var disabled = ServerConfig.disabledTraitIds();
        List<CatTraitType> available = new ArrayList<>(CatTraitRegistry.values(false));
        available.removeIf(trait -> !trait.natural() || disabled.contains(trait.id().toString()));
        if (forceDoughy && available.contains(CatTrait.DOUGHY)) {
            selected.add(new CatTraitInstance(CatTrait.DOUGHY, 1));
            available.remove(CatTrait.DOUGHY);
            desired = Math.max(1, desired);
        }

        while (selected.size() < desired && !available.isEmpty()) {
            List<CatTraitType> compatible = available.stream()
                    .filter(candidate -> compatibleWith(selected, candidate)).toList();
            if (compatible.isEmpty()) break;

            CatTraitType chosen = chooseWeightedTrait(compatible, random);
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
        return breed(first, second, CatBreedingMode.NORMAL, mutationChance, random);
    }

    /** Food affects new mutation rarity, never the probability or level of inherited traits. */
    public static CatTraitProfile breed(CatTraitProfile first,
                                        CatTraitProfile second,
                                        CatBreedingMode mode,
                                        float mutationChance,
                                        RandomSource random) {
        CatTraitProfile father = first == null ? EMPTY : first;
        CatTraitProfile mother = second == null ? EMPTY : second;
        var disabled = ServerConfig.disabledTraitIds();
        java.util.Set<ResourceLocation> fatherTraits = new java.util.HashSet<>();
        java.util.Set<ResourceLocation> motherTraits = new java.util.HashSet<>();
        father.traits.forEach(instance -> fatherTraits.add(instance.trait().id()));
        mother.traits.forEach(instance -> motherTraits.add(instance.trait().id()));

        List<CatTraitInstance> selected = new ArrayList<>(MAX_TRAITS);
        for (CatTraitType trait : CatTraitRegistry.values(false)) {
            if (trait.inheritable() && !disabled.contains(trait.id().toString()) && fatherTraits.contains(trait.id())
                    && motherTraits.contains(trait.id())
                    && compatibleWith(selected, trait)) {
                selected.add(new CatTraitInstance(trait, 1));
            }
        }

        boolean mutates = random.nextFloat()
                < Math.max(0.0F, Math.min(1.0F, mutationChance));
        int parentalLimit = mutates && selected.size() < MAX_TRAITS
                ? MAX_TRAITS - 1 : MAX_TRAITS;
        List<CatTraitType> oneParentTraits = new ArrayList<>();
        for (CatTraitType trait : CatTraitRegistry.values(false)) {
            if (!trait.inheritable() || disabled.contains(trait.id().toString())) continue;
            if (fatherTraits.contains(trait.id()) ^ motherTraits.contains(trait.id())) {
                oneParentTraits.add(trait);
            }
        }
        List<CatTraitType> passedInheritanceRoll = new ArrayList<>();
        for (CatTraitType trait : oneParentTraits) {
            if (random.nextBoolean()) passedInheritanceRoll.add(trait);
        }
        shuffle(passedInheritanceRoll, random);
        for (CatTraitType trait : passedInheritanceRoll) {
            if (selected.size() >= parentalLimit) break;
            if (compatibleWith(selected, trait)) {
                selected.add(new CatTraitInstance(trait, 1));
            }
        }

        if (mutates && selected.size() < MAX_TRAITS) {
            List<CatTraitType> mutationPool = CatTraitRegistry.values(false).stream()
                    .filter(trait -> trait.mutation() && !disabled.contains(trait.id().toString()))
                    .filter(trait -> !fatherTraits.contains(trait.id())
                            && !motherTraits.contains(trait.id()))
                    .filter(trait -> compatibleWith(selected, trait))
                    .toList();
            if (!mutationPool.isEmpty()) {
                selected.add(new CatTraitInstance(
                        chooseWeightedTrait(mutationPool, mode, random), 1));
            }
        }
        return CatTraitHooks.breed(father, mother, selected.isEmpty() ? EMPTY : new CatTraitProfile(selected));
    }

    /** Select rarity first so adding traits does not silently reweight a tier. */
    private static CatTraitType chooseWeightedTrait(List<CatTraitType> compatible,
                                                RandomSource random) {
        return chooseWeightedTrait(compatible, CatBreedingMode.NORMAL, random);
    }

    private static CatTraitType chooseWeightedTrait(List<CatTraitType> compatible,
                                                CatBreedingMode mode,
                                                RandomSource random) {
        CatBreedingMode resolvedMode = mode == null ? CatBreedingMode.NORMAL : mode;
        List<CatTraitRarity> compatibleRarities = Arrays.stream(CatTraitRarity.values())
                .filter(rarity -> compatible.stream()
                        .anyMatch(candidate -> candidate.rarity() == rarity))
                .toList();
        int totalWeight = compatibleRarities.stream()
                .mapToInt(resolvedMode::mutationTraitWeight).sum();
        int roll = random.nextInt(Math.max(1, totalWeight));
        CatTraitRarity chosenRarity = compatibleRarities
                .get(compatibleRarities.size() - 1);
        for (CatTraitRarity rarity : compatibleRarities) {
            roll -= resolvedMode.mutationTraitWeight(rarity);
            if (roll < 0) {
                chosenRarity = rarity;
                break;
            }
        }
        CatTraitRarity selectedRarity = chosenRarity;
        List<CatTraitType> candidates = compatible.stream()
                .filter(candidate -> candidate.rarity() == selectedRarity)
                .toList();
        int traitWeight = candidates.stream().mapToInt(CatTraitType::generationWeight).sum();
        int choice = random.nextInt(traitWeight);
        for (CatTraitType candidate : candidates) {
            choice -= candidate.generationWeight();
            if (choice < 0) return candidate;
        }
        return candidates.get(candidates.size() - 1);
    }

    private static void shuffle(List<CatTraitType> values, RandomSource random) {
        for (int index = values.size() - 1; index > 0; index--) {
            Collections.swap(values, index, random.nextInt(index + 1));
        }
    }

    private static boolean compatibleWith(List<CatTraitInstance> selected, CatTraitType candidate) {
        EnumSet<CatTraitSlot> occupied = EnumSet.noneOf(CatTraitSlot.class);
        for (CatTraitInstance instance : selected) {
            if (instance.trait().id().equals(candidate.id())
                    || instance.trait().conflicts().contains(candidate.id())
                    || candidate.conflicts().contains(instance.trait().id())) return false;
            occupied.addAll(instance.trait().occupiedSlots());
        }
        return Collections.disjoint(occupied, candidate.occupiedSlots());
    }

    public List<CatTraitInstance> traits() {
        return traits;
    }

    public boolean has(CatTrait trait) { return has((CatTraitType) trait); }
    public boolean has(CatTraitType trait) { return level(trait) > 0; }
    public int level(CatTrait trait) { return level((CatTraitType) trait); }
    public int level(CatTraitType trait) {
        int raw = rawLevel(trait);
        return raw <= 0 || trait == null || !trait.available() ? 0 : trait.clampLevel(raw);
    }
    /** Stored ownership/level, including missing custom definitions. */
    public int rawLevel(CatTraitType trait) {
        return trait == null ? 0 : traits.stream().filter(instance -> instance.trait().id().equals(trait.id()))
                .mapToInt(CatTraitInstance::level).findFirst().orElse(0);
    }

    /**
     * Development/editor mutation that still enforces uniqueness, capacity and
     * future appearance/behaviour conflicts. A non-positive level removes the
     * trait; adding an incompatible fifth trait leaves the profile unchanged.
     */
    public CatTraitProfile withLevel(CatTrait trait, int level) { return withLevel((CatTraitType) trait, level); }
    public CatTraitProfile withLevel(CatTraitType trait, int level) {
        if (trait == null) return this;
        List<CatTraitInstance> edited = new ArrayList<>(traits);
        int existingIndex = -1;
        for (int index = 0; index < edited.size(); index++) {
            if (edited.get(index).trait().id().equals(trait.id())) {
                existingIndex = index;
                break;
            }
        }

        if (level <= 0) {
            if (existingIndex < 0) return this;
            edited.remove(existingIndex);
            return edited.isEmpty() ? EMPTY : new CatTraitProfile(edited);
        }

        if (!trait.available()) return this;
        CatTraitInstance replacement = new CatTraitInstance(trait, trait.clampLevel(level));
        if (existingIndex >= 0) {
            edited.set(existingIndex, replacement);
            return new CatTraitProfile(edited);
        }
        // Existing levels can still be managed; only newly acquired traits are blocked.
        if (!trait.enabled() || ServerConfig.isTraitDisabled(trait)
                || edited.size() >= MAX_TRAITS || !compatibleWith(edited, trait)) return this;
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

    public static Optional<CatTraitProfile> load(CompoundTag root) { return load(root, false); }
    public static Optional<CatTraitProfile> load(CompoundTag root, boolean clientSide) {
        if (root == null || !root.contains(VERSION_TAG, Tag.TAG_INT)
                || root.getInt(VERSION_TAG) != DATA_VERSION
                || !root.contains(ENTRIES_TAG, Tag.TAG_LIST)) return Optional.empty();

        ListTag entries = root.getList(ENTRIES_TAG, Tag.TAG_COMPOUND);
        List<CatTraitInstance> loaded = new ArrayList<>(MAX_TRAITS);
        for (int index = 0; index < entries.size() && loaded.size() < MAX_TRAITS; index++) {
            CompoundTag entry = entries.getCompound(index);
            ResourceLocation id = ResourceLocation.tryParse(entry.getString(ID_TAG));
            CatTraitType trait = CatTraitRegistry.resolve(id, clientSide);
            if (trait == null || entry.getString(ID_TAG).length() > 128
                    || loaded.stream().anyMatch(old -> old.trait().id().equals(id))) continue;
            // Preserve saved custom entries across definition removal/conflict edits.
            // Continue validating legacy native/native conflicts exactly as before.
            if (trait instanceof CatTrait && !compatibleWith(loaded.stream()
                    .filter(old -> old.trait() instanceof CatTrait).toList(), trait)) continue;
            loaded.add(new CatTraitInstance(trait, entry.getInt(LEVEL_TAG)));
        }
        return Optional.of(loaded.isEmpty() ? EMPTY : new CatTraitProfile(loaded));
    }

    public static boolean isCurrentVersion(CompoundTag root) {
        return root != null && root.contains(VERSION_TAG, Tag.TAG_INT)
                && root.getInt(VERSION_TAG) == DATA_VERSION;
    }
}
