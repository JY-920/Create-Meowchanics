package cn.laowu.mod;

import cn.laowu.mod.client.ClientWorldSettings;
import cn.laowu.mod.genetics.*;
import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

/** Uses actual config specs, draft data and effective-stat conversion, not a game world. */
public final class SuitSettingsRegression {
    private static int checks;
    private static final double[][] DEFAULTS = {
        {.75,24,.12,10,20,6,3,10,0,0,0,0,0},
        {.55,36,.10,20,20,5,2,0,0,0,0,0,10},
        {2,36,.10,20,12,3,1,10,0,0,0,0,0},
        {.6,14,.08,5,40,10,4,0,0,0,10,0,0},
        {.85,42,.10,24,24,5,2,0,0,10,0,0,0},
        {0,34,.08,20,30,6,3,0,0,10,0,0,0},
        {1.35,56,.12,38,24,5,2,10,0,0,0,0,0}
    };

    public static void run() throws Exception {
        ServerConfig.resetWorldState();
        CommentedConfig world = CommentedConfig.inMemory(), global = CommentedConfig.inMemory();
        GlobalWorldConfigRegression.load(GlobalConfig.SPEC, global);
        GlobalWorldConfigRegression.load(ServerConfig.SPEC, world);
        defaultsAndRanges(world, global);
        inheritanceAndAuthority();
        resetDrafts();
        formulas();
        effectiveStats();
        legacyPreservation();
        System.out.println("PASS: " + checks + " suit-setting checks; original defaults, TOML/packet validation, "
                + "all 8192 reset lock masks, global/remote authority, interval formulas, live stat conversion and legacy K");
    }

    private static void defaultsAndRanges(CommentedConfig world, CommentedConfig global) {
        check(CatSuitSetting.values().length == 13 && CatSuitSetting.COMBAT.size() == 7
                && CatSuitSetting.BONUSES.size() == 6, "Complete two-section catalog");
        for (int i = 0; i < ServerConfig.CAREERS.size(); i++) {
            CatOutfitType outfit = ServerConfig.CAREERS.get(i);
            for (CatSuitSetting setting : CatSuitSetting.values()) {
                double expected = DEFAULTS[i][setting.ordinal()];
                check(setting.defaultValue(outfit) == expected, "Original default " + outfit + "/" + setting);
                check(ServerConfig.careerSetting(outfit, setting) == expected, "Effective default");
                check(!GlobalConfig.isLocked(setting.lockKey(outfit)), "Initially inherited");
                String path = setting == CatSuitSetting.DAMAGE
                        ? ServerConfig.CAREER_DAMAGE_TAG + "." + outfit.id()
                        : CatSuitSetting.TAG + "." + outfit.id() + "." + setting.id();
                check(world.getComment(path).contains(outfit.configName()), "World file names suit");
                check(global.getComment(path).contains("-1"), "Global file documents inheritance");
            }
        }
        for (CatSuitSetting setting : CatSuitSetting.values()) {
            for (Object invalid : new Object[]{"20", true, Double.NaN, Double.POSITIVE_INFINITY, -2, -.5, 1000000}) {
                check(!setting.validConfig(invalid, false), "Invalid world setting " + setting);
                check(!setting.validConfig(invalid, true), "Invalid global setting " + setting);
            }
            check(!setting.validConfig(-1, false) && setting.validConfig(-1, true), "Exact global sentinel only");
            check(setting.valid(999999), "Retain large finite freedom");
            check(setting.valid(1.5) != setting.integer(), "Integer settings reject fractions");
            boolean interval = setting == CatSuitSetting.INTERVAL_BASE || setting == CatSuitSetting.MIN_INTERVAL;
            check(setting.valid(0) != interval, "Positive tick interval floor");
        }
    }

    private static void inheritanceAndAuthority() {
        for (CatOutfitType outfit : ServerConfig.CAREERS) {
            for (CatSuitSetting setting : CatSuitSetting.values()) {
                setWorld(outfit, setting, 17);
                setGlobal(outfit, setting, 23);
                check(ServerConfig.careerSetting(outfit, setting) == 23, "Forced global value");
                check(GlobalConfig.isLocked(setting.lockKey(outfit)), "Forced global lock");
                CompoundTag forged = ServerConfig.snapshot();
                forged.getCompound(ServerConfig.LOCKS_TAG).putBoolean(setting.lockKey(outfit), false);
                setting.write(forged, outfit, 99);
                ServerConfig.applyUnlockedValues(forged);
                setGlobal(outfit, setting, -1);
                check(ServerConfig.careerSetting(outfit, setting) == 17, "Forged lock cannot alter stored world value");

                setGlobal(outfit, setting, 29);
                ServerConfig.setRemoteWorld(true);
                check(ServerConfig.careerSetting(outfit, setting) == 17, "Local global ignored before remote sync");
                CompoundTag remote = ServerConfig.snapshot();
                setting.write(remote, outfit, 31);
                remote.getCompound(ServerConfig.LOCKS_TAG).putBoolean(setting.lockKey(outfit), true);
                ServerConfig.receiveMirror(remote);
                setting.write(remote, outfit, 41);
                check(ServerConfig.careerSetting(outfit, setting) == 31, "Copied authoritative remote values");
                check(ServerConfig.snapshot().getCompound(ServerConfig.LOCKS_TAG).getBoolean(setting.lockKey(outfit)),
                        "Remote field lock");
                ServerConfig.clearMirror();
                check(ServerConfig.careerSetting(outfit, setting) == 29, "Remote never changes local global");
                setGlobal(outfit, setting, -1);
                check(ServerConfig.careerSetting(outfit, setting) == 17, "Remote never changes stored world");

                if (setting != CatSuitSetting.DAMAGE) {
                    CompoundTag invalid = ServerConfig.snapshot();
                    invalid.getCompound(CatSuitSetting.TAG).getCompound(outfit.id()).remove(setting.id());
                    check(!ServerConfig.valid(invalid), "Missing parameter rejected");
                    setting.write(invalid, outfit, -1);
                    check(!ServerConfig.valid(invalid), "World packet cannot use global sentinel");
                    setting.write(invalid, outfit, setting.integer() ? 1.5 : Double.NaN);
                    check(!ServerConfig.valid(invalid), "Invalid packet cannot be saved");
                    invalid.getCompound(CatSuitSetting.TAG).getCompound(outfit.id()).putInt(setting.id(), 3);
                    check(!ServerConfig.valid(invalid), "Packet NBT type is validated");
                }
                setWorld(outfit, setting, setting.defaultValue(outfit));
            }
        }
        CompoundTag oldPacket = ServerConfig.snapshot();
        oldPacket.remove(CatSuitSetting.TAG);
        check(!ServerConfig.valid(oldPacket), "Old incomplete packet cannot erase suit settings");
    }

    private static void resetDrafts() {
        CatOutfitType outfit = CatOutfitType.TERMINATOR;
        CompoundTag authoritative = ServerConfig.snapshot();
        CompoundTag draft = authoritative.copy();
        draft.putBoolean("can_edit", true);
        draft.putString("future_setting", "retain");
        for (CatSuitSetting setting : CatSuitSetting.values()) setting.write(draft, outfit, 17);
        for (int mask = 0; mask < 8192; mask++) {
            for (CatSuitSetting setting : CatSuitSetting.values())
                draft.getCompound(ServerConfig.LOCKS_TAG).putBoolean(setting.lockKey(outfit),
                        (mask & (1 << setting.ordinal())) != 0);
            CompoundTag before = draft.copy();
            var reset = ClientWorldSettings.editableSuitDefaults(draft, outfit);
            check(reset.size() == 13 - Integer.bitCount(mask), "Reset mask size");
            for (CatSuitSetting setting : CatSuitSetting.values()) {
                if ((mask & (1 << setting.ordinal())) == 0)
                    check(reset.get(setting) == DEFAULTS[0][setting.ordinal()], "Reset true default");
                else check(!reset.containsKey(setting), "Keep forced value");
            }
            check(before.equals(draft), "Reset calculation does not mutate draft or other suits");
        }
        check(authoritative.equals(ServerConfig.snapshot()), "Reset does not save automatically");
        for (CatOutfitType selected : ServerConfig.CAREERS) {
            CompoundTag clean = authoritative.copy();
            clean.putBoolean("can_edit", true);
            var reset = ClientWorldSettings.editableSuitDefaults(clean, selected);
            check(reset.size() == (selected == CatOutfitType.TRANSPORT ? 9 : 13), "No unsupported logistics attack controls");
            clean.putBoolean("can_edit", false);
            check(ClientWorldSettings.editableSuitDefaults(clean, selected).isEmpty(), "Read-only reset blocked");
        }
    }

    private static void formulas() {
        for (int i = 0; i < ServerConfig.CAREERS.size(); i++) {
            var settings = CatSuitSettings.current(ServerConfig.CAREERS.get(i));
            for (int speed = 0; speed <= 999; speed++) {
                int old = (int) Math.max(DEFAULTS[i][3], Math.min(60L, Math.round(DEFAULTS[i][1] - DEFAULTS[i][2] * speed)));
                check(settings.intervalTicks(speed) == old, "Default interval parity");
            }
            for (CatStat stat : CatStat.values())
                check(settings.attribute(50, stat) == 50 + (int) DEFAULTS[i][CatSuitSetting.forStat(stat).ordinal()],
                        "Preview includes suit stats once");
        }
        var base = CatSuitSettings.current(CatOutfitType.TERMINATOR);
        var edited = base.with(CatSuitSetting.INTERVAL_BASE, 200)
                .with(CatSuitSetting.MIN_INTERVAL, 30).with(CatSuitSetting.INTERVAL_PER_SPEED, 1);
        check(edited.intervalTicks(0) == 200 && edited.intervalTicks(100) == 100
                && edited.intervalTicks(200) == 30, "Custom base/rate/floor, no hidden old 60-tick ceiling");
        check(edited.with(CatSuitSetting.INTERVAL_PER_SPEED, 0).intervalTicks(999) == 200, "Zero rate gives constant interval");
        check(edited.with(CatSuitSetting.MIN_INTERVAL, 999999).intervalTicks(0) == 999999, "Minimum takes precedence");
        check(edited.with(CatSuitSetting.MIN_INTERVAL, 1).with(CatSuitSetting.INTERVAL_PER_SPEED, 999999)
                .intervalTicks(999 * 999999D) == 1, "No overflow or zero tick interval");
        check(base.value(CatSuitSetting.INTERVAL_BASE) == 24, "Preview edits are immutable");
        try {
            edited.values().put(CatSuitSetting.DAMAGE, 9D);
            throw new AssertionError("Immutable settings expected");
        } catch (UnsupportedOperationException expected) { checks++; }
        try {
            edited.with(CatSuitSetting.MIN_INTERVAL, 0);
            throw new AssertionError("Invalid settings expected");
        } catch (IllegalArgumentException expected) { checks++; }
        for (CatStat stat : CatStat.values())
            check(edited.with(CatSuitSetting.forStat(stat), 999999).attribute(50, stat) == 999,
                    "Effective stat safety cap remains");
    }

    private static void effectiveStats() throws Exception {
        Class<?> contextType = Class.forName("cn.laowu.mod.genetics.CatAttributeEffects$TraitContext");
        var constructor = contextType.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        var effective = CatAttributeEffects.class.getDeclaredMethod("effectiveValue",
                CatAttributeProfile.class, CatTraitProfile.class, CatStat.class, contextType);
        effective.setAccessible(true);
        CatAttributeProfile genes = CatAttributeProfile.founder(RandomSource.create(42));
        for (CatStat stat : CatStat.values()) genes = genes.withValues(stat, 50, 100);
        CompoundTag saved = genes.save();
        int[] contextIndex = {5,4,7,3,6,8,9};
        for (int i = 0; i < ServerConfig.CAREERS.size(); i++) {
            CatOutfitType outfit = ServerConfig.CAREERS.get(i);
            Object[] flags = new Object[15];
            java.util.Arrays.fill(flags, false);
            flags[contextIndex[i]] = true;
            Object context = constructor.newInstance(flags);
            for (CatStat stat : CatStat.values()) {
                CatSuitSetting setting = CatSuitSetting.forStat(stat);
                setWorld(outfit, setting, 11 + stat.ordinal());
                check((int) effective.invoke(null, genes, CatTraitProfile.EMPTY, stat, context) == 61 + stat.ordinal(),
                        "Real effective stat picks configured suit bonus " + outfit + "/" + stat);
                setWorld(outfit, setting, 999999);
                check((int) effective.invoke(null, genes, CatTraitProfile.EMPTY, stat, context) == 999,
                        "Real effective stat clamps large bonuses");
                setWorld(outfit, setting, setting.defaultValue(outfit));
            }
            check(saved.equals(genes.save()), "Suit does not modify raw current/cap genes");
        }
        Object[] flags = new Object[15];
        java.util.Arrays.fill(flags, false);
        Object noSuit = constructor.newInstance(flags);
        for (CatStat stat : CatStat.values())
            check((int) effective.invoke(null, genes, CatTraitProfile.EMPTY, stat, noSuit) == 50, "No outfit has no suit bonus");
    }

    private static void legacyPreservation() throws Exception {
        ServerConfig.resetWorldState();
        CommentedConfig world = CommentedConfig.inMemory(), global = CommentedConfig.inMemory();
        world.set("career_damage_coefficients.terminator", 1.91D);
        world.set("attribute_multipliers.speed", 3D);
        global.set("career_damage_coefficients.fire", 2.81D);
        GlobalWorldConfigRegression.load(GlobalConfig.SPEC, global);
        GlobalWorldConfigRegression.load(ServerConfig.SPEC, world);
        check(ServerConfig.careerDamageCoefficient(CatOutfitType.TERMINATOR) == 1.91, "Retain existing world K");
        check(ServerConfig.careerDamageCoefficient(CatOutfitType.FIRE) == 2.81, "Retain existing forced K");
        check(ServerConfig.multiplier(CatStat.SPEED) == 3, "Retain unrelated attribute multiplier");
        check(ServerConfig.careerSetting(CatOutfitType.TERMINATOR, CatSuitSetting.ARMOR) == 6, "Fill missing bonus defaults");
        check(!GlobalConfig.isLocked(CatSuitSetting.ARMOR.lockKey(CatOutfitType.TERMINATOR)), "New global fields inherit");
    }

    private static void setWorld(CatOutfitType outfit, CatSuitSetting setting, double value) {
        if (setting == CatSuitSetting.DAMAGE) ServerConfig.CAREER_DAMAGE_COEFFICIENTS.get(outfit).set(value);
        else ServerConfig.CAREER_SETTINGS.get(outfit).get(setting).set(value);
    }
    private static void setGlobal(CatOutfitType outfit, CatSuitSetting setting, double value) {
        if (setting == CatSuitSetting.DAMAGE) GlobalConfig.CAREER_DAMAGE_COEFFICIENTS.get(outfit).set(value);
        else GlobalConfig.CAREER_SETTINGS.get(outfit).get(setting).set(value);
    }
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
}
