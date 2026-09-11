package cn.laowu.mod;

import cn.laowu.mod.genetics.*;
import cn.laowu.mod.item.CatFilterRules;
import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraft.nbt.*;
import net.minecraft.util.RandomSource;
import java.util.*;

/** Real config specs, generation and NBT read paths, with no game-world writes. */
public final class TraitBlacklistRegression {
    private static int checks;

    public static void run() throws Exception {
        ServerConfig.resetWorldState();
        CommentedConfig global = CommentedConfig.inMemory();
        CommentedConfig world = CommentedConfig.inMemory();
        GlobalWorldConfigRegression.load(GlobalConfig.SPEC, global);
        GlobalWorldConfigRegression.load(ServerConfig.SPEC, world);
        check(ServerConfig.disabledTraitIds().isEmpty(), "Default allows all");
        check(!GlobalConfig.isLocked(CatTraitConfig.KEY), "Default inherits world");
        String night = CatTrait.NIGHT_OWL.id().toString(), dough = CatTrait.DOUGHY.id().toString();
        for (CommentedConfig config : List.of(global, world)) {
            String comment = config.getComment(CatTraitConfig.KEY);
            check(comment != null && comment.contains("夜猫子") && comment.contains("面团团"), "Chinese names in TOML");
            for (CatTrait trait : CatTrait.values()) check(comment.contains(trait.id() + " = "), "Complete stable ID catalog");
        }

        ServerConfig.DISABLED_TRAITS.set(List.of(night, "laowu:future_trait"));
        check(ServerConfig.isTraitDisabled(CatTrait.NIGHT_OWL), "World blacklist active");
        check(ServerConfig.disabledTraitIds().contains("laowu:future_trait"), "Unknown ID preserved");
        int revision = ServerConfig.snapshot().getInt("revision");
        GlobalConfig.DISABLED_TRAITS.set(List.of(night, "laowu:future_trait"));
        check(ServerConfig.snapshot().getInt("revision") != revision, "Lock-only change invalidates drafts");
        GlobalConfig.DISABLED_TRAITS.set(List.of());
        check(ServerConfig.disabledTraitIds().isEmpty() && GlobalConfig.isLocked(CatTraitConfig.KEY), "Global [] forces none");
        CompoundTag forged = ServerConfig.snapshot();
        forged.put(ServerConfig.LOCKS_TAG, new CompoundTag());
        CatTraitConfig.write(forged, Set.of(dough));
        ServerConfig.applyUnlockedValues(forged);
        check(ServerConfig.DISABLED_TRAITS.get().equals(List.of(night, "laowu:future_trait")), "Client cannot overwrite locked raw world list");
        GlobalConfig.DISABLED_TRAITS.set(List.of(-1));
        check(ServerConfig.isTraitDisabled(CatTrait.NIGHT_OWL), "Removing global override restores world");
        CompoundTag draft = ServerConfig.snapshot();
        CatTraitConfig.write(draft, Set.of(dough, "laowu:future_trait"));
        ServerConfig.applyUnlockedValues(draft);
        check(ServerConfig.isTraitDisabled(CatTrait.DOUGHY), "Unlocked world GUI list applies");
        check(ServerConfig.disabledTraitIds().contains("laowu:future_trait"), "GUI preserves unrecognized IDs");

        // Server mirror always wins over a joining client's local global file.
        GlobalConfig.DISABLED_TRAITS.set(List.of(night));
        CompoundTag remote = ServerConfig.snapshot();
        CatTraitConfig.write(remote, Set.of(dough));
        ServerConfig.receiveMirror(remote);
        CatTraitConfig.write(remote, Set.of()); // Verify defensive copy.
        check(ServerConfig.isTraitDisabled(CatTrait.DOUGHY) && !ServerConfig.isTraitDisabled(CatTrait.NIGHT_OWL), "Remote authority and defensive copy");
        ServerConfig.resetWorldState();
        check(ServerConfig.isTraitDisabled(CatTrait.NIGHT_OWL), "Remote list cleared between worlds");
        GlobalConfig.DISABLED_TRAITS.set(List.of(-1));
        GlobalWorldConfigRegression.load(ServerConfig.SPEC, CommentedConfig.inMemory());
        check(ServerConfig.disabledTraitIds().isEmpty(), "Another world retains its independent defaults");
        GlobalWorldConfigRegression.load(ServerConfig.SPEC, world);
        check(ServerConfig.isTraitDisabled(CatTrait.DOUGHY), "Return to original world restores its list");

        Object[] invalid = {List.of(-1, night), List.of(-2), List.of(0), List.of(true),
                List.of("night_owl"), List.of("laowu:Bad"), List.of(""), "laowu:night_owl",
                Collections.nCopies(1025, night)};
        for (Object value : invalid) {
            check(!CatTraitConfig.validList(value, true), "Invalid global list rejected");
            check(!CatTraitConfig.validList(value, false), "Invalid world list rejected");
            CommentedConfig badGlobal = CommentedConfig.inMemory();
            badGlobal.set(CatTraitConfig.KEY, value);
            GlobalWorldConfigRegression.load(GlobalConfig.SPEC, badGlobal);
            check(!GlobalConfig.isLocked(CatTraitConfig.KEY), "Malformed global value safely corrected to inheritance");
        }
        check(!CatTraitConfig.validList(List.of(-1), false), "World cannot use inheritance sentinel");
        check(CatTraitConfig.validList(List.of(), false), "World may allow all");
        check(CatTraitConfig.validList(List.of(night, night, "other:future_trait"), true), "Duplicates and future namespaced IDs accepted");
        CompoundTag badPacket = ServerConfig.snapshot();
        badPacket.remove(CatTraitConfig.KEY);
        check(!ServerConfig.valid(badPacket), "Legacy/missing list packet rejected");
        badPacket.putString(CatTraitConfig.KEY, night);
        check(!ServerConfig.valid(badPacket), "Wrong NBT container type rejected");
        ListTag numbers = new ListTag();
        numbers.add(IntTag.valueOf(-1));
        badPacket.put(CatTraitConfig.KEY, numbers);
        check(!ServerConfig.valid(badPacket), "Wrong element type is not mistaken for empty");
        CatTraitConfig.write(badPacket, Set.of("broken"));
        check(!ServerConfig.valid(badPacket), "Malformed NBT ID rejected");
        CatTraitConfig.write(badPacket, Set.of());
        check(ServerConfig.valid(badPacket), "Empty NBT list accepted");

        GlobalWorldConfigRegression.load(GlobalConfig.SPEC, global);
        GlobalConfig.DISABLED_TRAITS.set(List.of(-1));
        ServerConfig.DISABLED_TRAITS.set(List.of());
        Map<CatTrait, CompoundTag> saved = new EnumMap<>(CatTrait.class);
        for (CatTrait trait : CatTrait.values())
            saved.put(trait, CatTraitProfile.EMPTY.withLevel(trait, 1).save());
        CatTraitProfile parent = CatTraitProfile.EMPTY.withLevel(CatTrait.NIGHT_OWL, 4)
                .withLevel(CatTrait.THORNS, 3).withLevel(CatTrait.TOUGH, 5);
        CompoundTag parentNBT = parent.save();
        List<String> everyId = Arrays.stream(CatTrait.values()).map(t -> t.id().toString()).toList();
        ServerConfig.DISABLED_TRAITS.set(everyId);
        for (var entry : saved.entrySet()) {
            CatTrait trait = entry.getKey();
            CompoundTag before = entry.getValue().copy();
            CatTraitProfile loaded = CatTraitProfile.load(entry.getValue()).orElseThrow();
            check(loaded.has(trait) && loaded.level(trait) == 1, "Disabled old trait still loads " + trait);
            check(loaded.save().equals(before) && entry.getValue().equals(before), "NBT round trip unchanged " + trait);
            check(loaded.withLevel(trait, trait.maxLevel()).level(trait) == trait.maxLevel(), "Existing trait still upgrades");
            check(!loaded.withLevel(trait, 0).has(trait), "Existing trait can still be explicitly removed");
            check(!CatTraitProfile.EMPTY.withLevel(trait, 1).has(trait), "New editor addition blocked");
        }
        RandomSource random = RandomSource.create(20260910L);
        for (int i = 0; i < 1000; i++) {
            check(CatTraitProfile.founder(random).traits().isEmpty(), "All-disabled founder safe");
            check(CatTraitProfile.injected(random).traits().isEmpty(), "All-disabled injection safe, including Doughy");
            check(CatTraitProfile.breed(parent, parent, 1F, random).traits().isEmpty(), "All-disabled breeding safe");
        }
        ServerConfig.DISABLED_TRAITS.set(List.of(night, dough));
        Set<String> disabled = ServerConfig.disabledTraitIds();
        Set<CatTrait> observed = EnumSet.noneOf(CatTrait.class);
        for (int i = 0; i < 3000; i++) {
            for (CatTraitProfile profile : List.of(CatTraitProfile.founder(random),
                    CatTraitProfile.injected(random), CatTraitProfile.breed(parent, parent, 1F, random),
                    CatTraitProfile.breed(parent, CatTraitProfile.EMPTY, 1F, random))) {
                for (CatTraitInstance instance : profile.traits()) {
                    check(!disabled.contains(instance.trait().id().toString()), "No disabled trait in any generation path");
                    check(instance.level() == 1, "New traits still start at level I");
                    observed.add(instance.trait());
                }
                check(profile.traits().size() <= 4, "Trait count cap unchanged");
                Set<CatTraitSlot> occupied = EnumSet.noneOf(CatTraitSlot.class);
                for (CatTraitInstance instance : profile.traits()) for (CatTraitSlot slot : instance.trait().occupiedSlots())
                    check(occupied.add(slot), "Appearance/behavior conflicts still excluded");
            }
        }
        check(observed.size() == CatTrait.values().length - 2, "Every allowed trait can still appear");
        check(parent.save().equals(parentNBT), "Breeding never removes traits from parents");
        CatTraitProfile shared = CatTraitProfile.breed(parent, parent, 0F, random);
        check(shared.has(CatTrait.THORNS) && shared.has(CatTrait.TOUGH) && !shared.has(CatTrait.NIGHT_OWL), "Unbanned shared traits retain guarantee");
        ServerConfig.DISABLED_TRAITS.set(List.of());
        check(CatTraitProfile.injected(random).has(CatTrait.DOUGHY), "Re-enabling restores guaranteed injected Doughy");
        check(CatTraitProfile.breed(parent, parent, 0F, random).has(CatTrait.NIGHT_OWL), "Re-enabling restores inheritance");
        check(CatTraitProfile.EMPTY.withLevel(CatTrait.NIGHT_OWL, 1).has(CatTrait.NIGHT_OWL), "Re-enabling restores editor");

        filterMigration();
        System.out.println("PASS: " + checks + " trait blacklist / filter-range checks; real generation, config locks, remote authority, old NBT, complete ID comments.");
    }

    private static void filterMigration() {
        CompoundTag old = new CompoundTag();
        old.putInt("Version", 2);
        old.putIntArray("CurrentMin", new int[]{0, 90, 301, 999, 0, 0});
        old.putIntArray("CurrentMax", new int[]{999, 999, 500, 999, 90, 300});
        old.putIntArray("PotentialMin", new int[]{90, 0, 0, 0, 0, 0});
        old.putIntArray("PotentialMax", new int[]{100, 100, 100, 100, 100, 100});
        old.putInt("CurrentEnabled", 63);
        old.putInt("LimitEnabled", 1);
        old.putInt("LogicFlags", 31);
        old.putString("Growth", "adult");
        old.putString("Ownership", "owned");
        old.putString("Career", "fire");
        old.putString("CatName", "小猫");
        ListTag traits = new ListTag();
        traits.add(StringTag.valueOf(CatTrait.NIGHT_OWL.id().toString()));
        old.put("RequiredTraits", traits);
        CompoundTag before = old.copy();
        CatFilterRules rules = CatFilterRules.readData(old);
        int[] expectedMin = {0, 90, 300, 300, 0, 0};
        int[] expectedMax = {300, 300, 300, 300, 90, 300};
        for (CatStat stat : CatStat.values()) {
            check(rules.min(0, stat) == expectedMin[stat.ordinal()] && rules.max(0, stat) == expectedMax[stat.ordinal()], "Old numeric range clamped safely");
            check(rules.enabled(0, stat), "Explicit enabled conditions preserved");
            check(rules.max(1, stat) == 100, "Training limits remain 100");
        }
        check(old.equals(before), "Reading a legacy filter must not mutate item NBT");
        check(rules.logic().flags() == 31 && rules.logic().limitMask() == 1, "All/any/inverse flags preserved");
        check(rules.growth() == CatFilterRules.GrowthFilter.ADULT && rules.ownership() == CatFilterRules.OwnershipFilter.OWNED
                && rules.career() == CatFilterRules.CareerFilter.FIRE && rules.catName().equals("小猫"), "Identity conditions preserved");
        check(rules.requiredTraits().equals(List.of(CatTrait.NIGHT_OWL)), "Filter can still select traits even if generation is disabled");
        for (int version : new int[]{0, 1}) {
            CompoundTag legacy = new CompoundTag();
            if (version > 0) legacy.putInt("Version", version);
            legacy.putIntArray("CurrentMax", new int[]{version == 0 ? 100 : 999, version == 0 ? 100 : 999,
                    version == 0 ? 100 : 999, version == 0 ? 100 : 999, version == 0 ? 100 : 999,
                    version == 0 ? 100 : 999});
            CatFilterRules migrated = CatFilterRules.readData(legacy);
            check(migrated.logic().currentMask() == 0, "Old unrestricted default stays unchecked");
            for (CatStat stat : CatStat.values()) check(migrated.max(0, stat) == 300, "Legacy display default is 300");
        }
        check(CatFilterRules.maxValue(0) == 300 && CatFilterRules.maxValue(1) == 100, "Filter slider bounds");
    }
    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
