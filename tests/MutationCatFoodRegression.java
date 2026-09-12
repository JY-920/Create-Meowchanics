package cn.laowu.mod;

import cn.laowu.mod.genetics.*;
import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Exercises the actual food, breeding, rarity, config and NBT code on both loaders. */
public final class MutationCatFoodRegression {
    private static int checks;

    public static void run() throws Exception {
        ServerConfig.resetWorldState();
        GlobalWorldConfigRegression.load(GlobalConfig.SPEC, CommentedConfig.inMemory());
        GlobalWorldConfigRegression.load(ServerConfig.SPEC, CommentedConfig.inMemory());
        check(CatBreedingMode.MUTATION.inheritedLoci() == 5, "Five inherited loci");
        check(CatBreedingMode.MUTATION.targetedStat() == null, "No forced target");
        check(Math.abs(CatBreedingMode.MUTATION.mutationBonus() - .4F) < 1e-6,
                "Food adds forty percent");
        numericInheritance();
        effectiveChance();
        rarityWeights();
        traitSafety();
        System.out.println("PASS: " + checks + " mutation-food checks; five paired loci, "
                + "40%/Luck calculation, exact rarity weights, inheritance, conflicts, "
                + "four-trait cap, disabled pools and unchanged parental NBT");
    }

    private static CatAttributeProfile attributes(int current, int limit) {
        CatAttributeProfile profile = CatAttributeProfile.founder(RandomSource.create(1L));
        for (CatStat stat : CatStat.values()) profile = profile.withValues(stat, current, limit);
        return profile;
    }

    private static void numericInheritance() {
        CatAttributeProfile father = attributes(10, 20), mother = attributes(40, 60);
        CompoundTag beforeFather = father.save(), beforeMother = mother.save();
        for (float chance : new float[]{0F, .6F, 1F}) for (int seed = 0; seed < 2000; seed++) {
            AtomicInteger rolls = new AtomicInteger();
            RandomSource delegate = RandomSource.create(seed);
            RandomSource random = (RandomSource) Proxy.newProxyInstance(
                    RandomSource.class.getClassLoader(), new Class<?>[]{RandomSource.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("nextInt") && args != null && args.length == 1) {
                            int bound = (int) args[0];
                            // Fresh pairs can never accidentally equal a parental pair.
                            if (bound == 101) return rolls.getAndIncrement() == 0 ? 80 : 95;
                            if (bound == 81) return 30;
                            if (bound == 96) return 50;
                        }
                        try { return method.invoke(delegate, args); }
                        catch (InvocationTargetException failure) { throw failure.getCause(); }
                    });
            CatAttributeProfile child = CatAttributeProfile.breed(
                    father, mother, CatBreedingMode.MUTATION, chance, random);
            int inherited = 0;
            for (CatStat stat : CatStat.values()) {
                int value = child.current(stat), limit = child.potential(stat);
                if ((value == 10 && limit == 20) || (value == 40 && limit == 60)) inherited++;
                else {
                    check((value == 30 && limit == 80) || (value == 50 && limit == 95),
                            "Fresh roll keeps current and potential together");
                    check(chance != 1F || limit == 95, "Successful mutation keeps higher limit");
                }
                check(value >= 0 && value <= limit && limit <= 100, "Valid gene bounds");
            }
            check(inherited == 5, "Exactly five paired parental loci, not two or six");
            check(rolls.get() == (chance == 0F ? 1 : chance == 1F ? 2 : rolls.get()),
                    "Mutation adds exactly one roll to only the fresh locus");
            check(rolls.get() >= 1 && rolls.get() <= 2, "Only one fresh locus is rolled");
            check(CatAttributeProfile.load(child.save()).orElseThrow().save().equals(child.save()),
                    "Offspring NBT round trip");
        }
        check(father.save().equals(beforeFather) && mother.save().equals(beforeMother),
                "Breeding preserves parental genes");
        for (CatStat target : CatStat.values()) {
            CatBreedingMode food = Arrays.stream(CatBreedingMode.values())
                    .filter(mode -> mode.targetedStat() == target).findFirst().orElseThrow();
            CatAttributeProfile child = CatAttributeProfile.breed(
                    attributes(91, 91), attributes(94, 94), food, 0F, RandomSource.create(5L));
            check(child.current(target) == 95 && child.potential(target) == 95,
                    "Targeted 90+ growth remains unchanged");
        }
    }

    private static void effectiveChance() {
        for (float base : new float[]{.1F, .15F, .2F}) for (int luck : new int[]{0, 50, 100}) {
            CatAttributeProfile parent = attributes(luck, 100);
            float actual = CatBreedingLogic.effectiveMutationChance(
                    base, CatBreedingMode.MUTATION, parent, parent);
            float expected = (base + .4F) * (1F + luck / 300F);
            check(Math.abs(actual - expected) < 1e-6, "Box, food and parental Luck combined");
            check(CatBreedingLogic.basisPoints(actual) == Math.round(expected * 10000F),
                    "Pixel GUI receives correct effective value");
            check(Math.abs(CatBreedingLogic.effectiveMutationChance(
                    base, CatBreedingMode.NORMAL, parent, parent) - base * (1F + luck / 300F)) < 1e-6,
                    "Other foods do not receive mutation bonus");
        }
        check(Math.abs(CatBreedingLogic.effectiveMutationChance(
                .2F, CatBreedingMode.MUTATION, null, null) - .6F) < 1e-6,
                "Empty parent slots still display food and box contributions");
        check(CatBreedingLogic.effectiveMutationChance(
                .9F, CatBreedingMode.MUTATION, attributes(100, 100), attributes(100, 100)) == 1F,
                "Effective chance remains capped at 100%");
    }

    private static RandomSource rarityRoll(int roll) {
        RandomSource delegate = RandomSource.create(12345L);
        AtomicInteger intCalls = new AtomicInteger();
        return (RandomSource) Proxy.newProxyInstance(
                RandomSource.class.getClassLoader(), new Class<?>[]{RandomSource.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("nextInt") && args != null && args.length == 1
                            && intCalls.getAndIncrement() == 0) {
                        check(roll >= 0 && roll < (int) args[0], "Weighted roll uses available total");
                        return roll;
                    }
                    try { return method.invoke(delegate, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
    }

    private static void rarityWeights() {
        for (CatBreedingMode mode : CatBreedingMode.values()) {
            int[] expected = mode == CatBreedingMode.MUTATION
                    ? new int[]{10, 25, 45, 20} : new int[]{30, 35, 30, 5};
            int[] counts = new int[4];
            for (int roll = 0; roll < 100; roll++) {
                CatTraitProfile child = CatTraitProfile.breed(
                        CatTraitProfile.EMPTY, CatTraitProfile.EMPTY, mode, 1F, rarityRoll(roll));
                check(child.traits().size() == 1, "Exactly one new trait after success");
                CatTraitInstance trait = child.traits().get(0);
                counts[trait.trait().rarity().ordinal()]++;
                check(trait.level() == 1, "New traits always start at level I");
            }
            check(Arrays.equals(counts, expected), "Exact rarity distribution for " + mode);
        }
        for (int roll = 0; roll < 100; roll++) {
            check(CatTraitProfile.breed(CatTraitProfile.EMPTY, CatTraitProfile.EMPTY,
                    1F, rarityRoll(roll)).save().equals(
                    CatTraitProfile.breed(CatTraitProfile.EMPTY, CatTraitProfile.EMPTY,
                            CatBreedingMode.NORMAL, 1F, rarityRoll(roll)).save()),
                    "Natural/debug compatibility overload retains original weights");
        }
        for (CatTraitRarity rarity : CatTraitRarity.values()) {
            ServerConfig.DISABLED_TRAITS.set(Arrays.stream(CatTrait.values())
                    .filter(trait -> trait.rarity() != rarity).map(trait -> trait.id().toString()).toList());
            int weight = CatBreedingMode.MUTATION.mutationTraitWeight(rarity);
            for (int roll = 0; roll < weight; roll++) {
                CatTraitProfile child = CatTraitProfile.breed(
                        CatTraitProfile.EMPTY, CatTraitProfile.EMPTY,
                        CatBreedingMode.MUTATION, 1F, rarityRoll(roll));
                check(child.traits().size() == 1 && child.traits().get(0).trait().rarity() == rarity,
                        "Unavailable rarities are omitted and remaining weights renormalised");
            }
        }
        ServerConfig.DISABLED_TRAITS.set(List.of());
    }

    private static void traitSafety() {
        CatTraitProfile full = CatTraitProfile.EMPTY.withLevel(CatTrait.THORNS, 7)
                .withLevel(CatTrait.NIGHT_OWL, 7).withLevel(CatTrait.HEAT_RESISTANCE, 1)
                .withLevel(CatTrait.LOLI, 1);
        CatTraitProfile father = CatTraitProfile.EMPTY.withLevel(CatTrait.NIGHT_OWL, 7)
                .withLevel(CatTrait.LOLI, 1).withLevel(CatTrait.STITCH, 1).withLevel(CatTrait.DOUGHY, 1);
        CatTraitProfile mother = CatTraitProfile.EMPTY.withLevel(CatTrait.NIGHT_OWL, 6)
                .withLevel(CatTrait.LOLI, 1).withLevel(CatTrait.MISCHIEVOUS, 1)
                .withLevel(CatTrait.THORNS, 7);
        CompoundTag fatherNBT = father.save(), motherNBT = mother.save();
        int inheritedThorns = 0;
        CatTraitProfile donor = CatTraitProfile.EMPTY.withLevel(CatTrait.THORNS, 7);
        for (int seed = 0; seed < 5000; seed++) {
            CatTraitProfile fixed = CatTraitProfile.breed(full, full,
                    CatBreedingMode.MUTATION, 1F, RandomSource.create(seed));
            check(fixed.traits().size() == 4, "Four shared traits are never replaced by mutation");
            for (CatTraitInstance trait : full.traits())
                check(fixed.level(trait.trait()) == 1, "Shared traits guaranteed and reset to level I");
            CatTraitProfile child = CatTraitProfile.breed(father, mother,
                    CatBreedingMode.MUTATION, 1F, RandomSource.create(seed));
            check(child.has(CatTrait.NIGHT_OWL) && child.has(CatTrait.LOLI), "Shared priorities preserved");
            check(child.traits().size() <= 4, "Four-trait cap");
            Set<CatTraitSlot> slots = EnumSet.noneOf(CatTraitSlot.class);
            Set<CatTrait> unique = EnumSet.noneOf(CatTrait.class);
            int newTraits = 0;
            for (CatTraitInstance trait : child.traits()) {
                check(trait.level() == 1 && unique.add(trait.trait()), "No duplicate or inherited levels");
                for (CatTraitSlot slot : trait.trait().occupiedSlots())
                    check(slots.add(slot), "No appearance or behaviour conflicts");
                if (!father.has(trait.trait()) && !mother.has(trait.trait())) newTraits++;
            }
            check(newTraits == 1, "Success reserves one compatible trait absent from both parents");
            check(!child.has(CatTrait.DOUGHY), "Doughy still never inherited");
            CatTraitProfile single = CatTraitProfile.breed(donor, CatTraitProfile.EMPTY,
                    CatBreedingMode.MUTATION, 0F, RandomSource.create(seed));
            if (single.has(CatTrait.THORNS)) inheritedThorns++;
            check(single.traits().size() <= 1, "No mutation when chance is zero");
            check(single.save().equals(CatTraitProfile.breed(donor, CatTraitProfile.EMPTY,
                    CatBreedingMode.NORMAL, 0F, RandomSource.create(seed)).save()),
                    "Food does not change single-parent inheritance");
        }
        check(inheritedThorns > 2250 && inheritedThorns < 2750, "Single-parent chance stays near 50%");
        check(father.save().equals(fatherNBT) && mother.save().equals(motherNBT),
                "Trait breeding leaves parent NBT untouched");
        List<String> allIds = Arrays.stream(CatTrait.values()).map(trait -> trait.id().toString()).toList();
        ServerConfig.DISABLED_TRAITS.set(allIds);
        check(CatTraitProfile.breed(father, mother, CatBreedingMode.MUTATION, 1F,
                RandomSource.create(1L)).traits().isEmpty(), "All-disabled world remains safe");
        GlobalConfig.DISABLED_TRAITS.set(allIds);
        ServerConfig.DISABLED_TRAITS.set(List.of());
        check(CatTraitProfile.breed(null, null, CatBreedingMode.MUTATION, 1F,
                RandomSource.create(1L)).traits().isEmpty(), "Global blacklist cannot be bypassed");
        GlobalConfig.DISABLED_TRAITS.set(List.of(-1));
        check(CatTraitProfile.load(fatherNBT).orElseThrow().save().equals(fatherNBT),
                "Existing saved traits remain intact");
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
