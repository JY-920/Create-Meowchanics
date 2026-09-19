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
        {2,40,.10,20,30,7,3,10,0,0,0,0,0},
        {.75,15,.08,5,40,10,4,0,0,0,10,0,0},
        {1.2,41,.10,24,24,5,2,0,0,10,0,0,0},
        {0,34,.08,20,30,10,3,0,0,10,0,0,0},
        {1.5,56,.12,38,24,5,2,10,0,0,0,0,0},
        {3,50,.25,20,30,12,5,0,0,0,0,0,10},
        {0,24,.10,8,6,2,0,0,0,10,0,0,0},
        {0,24,.10,8,6,2,0,0,0,10,0,0,0},
        {1.75,24,.12,10,6,2,0,10,0,0,0,0,0},
        {1,28,.12,14,24,6,2,0,0,0,10,0,0},
        {.85,24,.10,12,12,4,1,0,0,0,0,0,0}
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
        supportAndArtillery();
        balanceMigration();
        System.out.println("PASS: " + checks + " suit-setting checks; original defaults, TOML/packet validation, "
                + "all 8192 reset lock masks, global/remote authority, interval formulas, live stat conversion and protected balance migration");
    }

    private static void supportAndArtillery() throws Exception {
        GlobalWorldConfigRegression.load(GlobalConfig.SPEC, CommentedConfig.inMemory());
        GlobalWorldConfigRegression.load(ServerConfig.SPEC, CommentedConfig.inMemory());
        for (int stamina : new int[]{0, 1, 50, 100, 999, 1000000})
            check(CatCrankPower.stressCapacity(stamina) == 128.0 * stamina, "No extra stress-capacity ceiling");
        for (int attack : new int[]{0, 50, 100, 999})
            check(Math.abs(CatAttributeEffects.attackDamage(attack) * CatOutfitType.NONE.defaultDamageCoefficient()
                    - (1 + .04 * attack)) < 0.00001, "Lower ordinary-cat damage formula");
        for (CatOutfitType outfit : new CatOutfitType[]{CatOutfitType.MEDICAL, CatOutfitType.TRANSPORT, CatOutfitType.MUSIC}) {
            check(outfit.role() == CatCombatRole.SUPPORT && !outfit.isPreviewOnly(), "Active support category");
            for (int speed=0;speed<=999;speed++)
                check(Math.abs(CatAttributeEffects.movementMultiplier(speed, outfit)
                        / CatAttributeEffects.movementMultiplier(speed) - 1.2) < 0.00001, "Same-stat support movement advantage");
        }
        check(CatSupportRules.LOGISTICS_CAST_TICKS == 20 && CatSupportRules.LOGISTICS_TARGET_TICKS == 100,
                "Independent global and recipient logistics cooldowns");
        check(CatSupportRules.triageScore(0, 1, 256, .7) < CatSupportRules.triageScore(0, 64, 256, .1),
                "Low-intelligence nearest triage");
        check(CatSupportRules.triageScore(100, 1, 256, .7) > CatSupportRules.triageScore(100, 64, 256, .1),
                "High-intelligence health-percentage triage");
        var engineer = CatSuitSettings.current(CatOutfitType.ENGINEERING);
        check(engineer.intervalTicks(0) == 50 && engineer.intervalTicks(50) == 38
                && engineer.intervalTicks(100) == 25 && engineer.intervalTicks(200) == 20,
                "Faster artillery with a visible Speed contribution");
        for (int shot=0;shot<30;shot++)
            check(CatArtilleryMunition.shot(shot).ordinal() == shot%3, "Stable three-munition cycle");
        var settings = ServerConfig.CAREER_SETTINGS.get(CatOutfitType.ENGINEERING);
        settings.get(CatSuitSetting.INTERVAL_BASE).set(80);
        settings.get(CatSuitSetting.INTERVAL_PER_SPEED).set(.1);
        settings.get(CatSuitSetting.MIN_INTERVAL).set(60);
        check(ServerConfig.migrateLegacyBalanceDefaults(), "First-load balance migration");
        check(settings.get(CatSuitSetting.INTERVAL_BASE).get().intValue() == 50
                && settings.get(CatSuitSetting.MIN_INTERVAL).get().intValue() == 20, "Old default reload upgraded");
        settings.get(CatSuitSetting.INTERVAL_BASE).set(80);
        check(!ServerConfig.migrateLegacyBalanceDefaults()
                && settings.get(CatSuitSetting.INTERVAL_BASE).get().intValue() == 80, "Do not repeat migration");
        ServerConfig.INTERNAL_BALANCE_REVISION.set(0);
        settings.get(CatSuitSetting.INTERVAL_BASE).set(81);
        check(ServerConfig.migrateLegacyBalanceDefaults()
                && settings.get(CatSuitSetting.INTERVAL_BASE).get().intValue() == 81, "Preserve custom reload values");
    }

    private static void balanceMigration() throws Exception {
        var groups = java.util.Map.of(
                CatOutfitType.FLIGHT, java.util.Map.of(CatSuitSetting.INTERVAL_BASE, 36.0,
                        CatSuitSetting.HEALTH, 12.0, CatSuitSetting.ARMOR, 3.0, CatSuitSetting.TOUGHNESS, 1.0),
                CatOutfitType.FIRE, java.util.Map.of(CatSuitSetting.DAMAGE, .60, CatSuitSetting.INTERVAL_BASE, 14.0),
                CatOutfitType.HONEY, java.util.Map.of(CatSuitSetting.DAMAGE, .85, CatSuitSetting.INTERVAL_BASE, 42.0),
                CatOutfitType.DYNAMITE, java.util.Map.of(CatSuitSetting.DAMAGE, 1.35),
                CatOutfitType.TRANSPORT, java.util.Map.of(CatSuitSetting.ARMOR, 6.0),
                CatOutfitType.ENGINEERING, java.util.Map.of(CatSuitSetting.ATTACK_STAT, 10.0,
                        CatSuitSetting.INTELLIGENCE_STAT, 20.0, CatSuitSetting.LUCK_STAT, 40.0),
                CatOutfitType.MEDICAL, java.util.Map.of(CatSuitSetting.HEALTH, 0.0,
                        CatSuitSetting.ARMOR, 0.0, CatSuitSetting.TOUGHNESS, 0.0));
        for (int revision : new int[]{0, 1, 2}) for (var group : groups.entrySet()) {
            var fields = new java.util.ArrayList<>(group.getValue().keySet());
            for (int mask = 0; mask < (1 << fields.size()); mask++) {
                GlobalWorldConfigRegression.load(GlobalConfig.SPEC, CommentedConfig.inMemory());
                GlobalWorldConfigRegression.load(ServerConfig.SPEC, CommentedConfig.inMemory());
                ServerConfig.INTERNAL_BALANCE_REVISION.set(revision);
                for (var all : groups.entrySet()) for (var value : all.getValue().entrySet())
                    setWorld(all.getKey(), value.getKey(), value.getValue());
                for (int field = 0; field < fields.size(); field++)
                    if ((mask & (1 << field)) != 0)
                        setWorld(group.getKey(), fields.get(field), group.getValue().get(fields.get(field)) + 1);
                // A global override is authoritative but must not overwrite the world value.
                setGlobal(group.getKey(), fields.get(0), 77);
                double fishingK = (mask & 1) == 0 ? .55 : 2.6;
                double fishingInterval = (mask & 1) == 0 ? 36 : 79;
                setWorld(CatOutfitType.FISHING, CatSuitSetting.DAMAGE, fishingK);
                setWorld(CatOutfitType.FISHING, CatSuitSetting.INTERVAL_BASE, fishingInterval);
                ServerConfig.SHOW_HELL_RECIPES.set(true);
                check(ServerConfig.migrateLegacyBalanceDefaults(), "Migrate earlier balance revision");
                check(ServerConfig.INTERNAL_BALANCE_REVISION.get() == 4, "Store revision 4");
                for (var other : groups.entrySet()) for (var value : other.getValue().entrySet()) {
                    int index = fields.indexOf(value.getKey());
                    double expected = other.getKey() == group.getKey() && mask != 0
                            ? value.getValue() + ((mask & (1 << index)) != 0 ? 1 : 0)
                            : revision == 2 && other.getKey() != CatOutfitType.ENGINEERING
                                    && other.getKey() != CatOutfitType.MEDICAL
                                    ? value.getValue() : value.getKey().defaultValue(other.getKey());
                    check(stored(other.getKey(), value.getKey()) == expected,
                            "Upgrade untouched groups atomically; preserve customized group " + other.getKey());
                }
                check(ServerConfig.careerSetting(group.getKey(), fields.get(0)) == 77, "Global override still wins");
                check(stored(CatOutfitType.FISHING, CatSuitSetting.DAMAGE) == fishingK
                                && stored(CatOutfitType.FISHING, CatSuitSetting.INTERVAL_BASE) == fishingInterval,
                        "Fishing defaults AND custom fishing data remain unchanged");
                check(ServerConfig.SHOW_HELL_RECIPES.get(), "Preserve unrelated world option");
                setWorld(CatOutfitType.FLIGHT, CatSuitSetting.HEALTH, 12);
                check(!ServerConfig.migrateLegacyBalanceDefaults()
                                && stored(CatOutfitType.FLIGHT, CatSuitSetting.HEALTH) == 12,
                        "Never reapply balance after later customization");
            }
        }
        GlobalWorldConfigRegression.load(GlobalConfig.SPEC, CommentedConfig.inMemory());
        GlobalWorldConfigRegression.load(ServerConfig.SPEC, CommentedConfig.inMemory());

        // Revision 3 is already balanced: upgrading music must not reapply any old suit migration.
        var musicFields=java.util.List.of(CatSuitSetting.HEALTH,CatSuitSetting.ARMOR,
                CatSuitSetting.TOUGHNESS,CatSuitSetting.SPEED_STAT);
        for(int mask=0;mask<16;mask++) {
            GlobalWorldConfigRegression.load(GlobalConfig.SPEC, CommentedConfig.inMemory());
            GlobalWorldConfigRegression.load(ServerConfig.SPEC, CommentedConfig.inMemory());
            ServerConfig.INTERNAL_BALANCE_REVISION.set(3);
            for(int i=0;i<musicFields.size();i++)
                setWorld(CatOutfitType.MUSIC,musicFields.get(i),(mask&(1<<i))==0?0:1);
            setWorld(CatOutfitType.ENGINEERING,CatSuitSetting.ATTACK_STAT,10);
            setWorld(CatOutfitType.ENGINEERING,CatSuitSetting.INTELLIGENCE_STAT,20);
            setWorld(CatOutfitType.ENGINEERING,CatSuitSetting.LUCK_STAT,40);
            setWorld(CatOutfitType.MEDICAL,CatSuitSetting.HEALTH,0);
            setWorld(CatOutfitType.MEDICAL,CatSuitSetting.ARMOR,0);
            check(ServerConfig.migrateLegacyBalanceDefaults(),"Music migration from revision 3");
            check(ServerConfig.INTERNAL_BALANCE_REVISION.get()==4,"Music revision stored");
            for(int i=0;i<musicFields.size();i++) {
                var field=musicFields.get(i);
                check(stored(CatOutfitType.MUSIC,field)==(mask==0?field.defaultValue(CatOutfitType.MUSIC):(mask&(1<<i))==0?0:1),
                        "Only completely untouched music group upgrades");
            }
            check(stored(CatOutfitType.ENGINEERING,CatSuitSetting.LUCK_STAT)==40
                    &&stored(CatOutfitType.ENGINEERING,CatSuitSetting.INTELLIGENCE_STAT)==20
                    &&stored(CatOutfitType.MEDICAL,CatSuitSetting.HEALTH)==0,
                    "Do not reapply previous migrations to later custom values");
            check(!ServerConfig.migrateLegacyBalanceDefaults(),"Music migration runs only once");
        }

        GlobalWorldConfigRegression.load(GlobalConfig.SPEC, CommentedConfig.inMemory());
        GlobalWorldConfigRegression.load(ServerConfig.SPEC, CommentedConfig.inMemory());
        var flight = CatSuitSettings.current(CatOutfitType.FLIGHT);
        check(CatAttributeEffects.maximumHealth(flight.attribute(50, CatStat.HEALTH))
                                + flight.value(CatSuitSetting.HEALTH) == 60
                        && CatAttributeEffects.armor(flight.attribute(50, CatStat.STAMINA))
                                + flight.value(CatSuitSetting.ARMOR) == 17
                        && CatAttributeEffects.armorToughness(flight.attribute(50, CatStat.STAMINA))
                                + flight.value(CatSuitSetting.TOUGHNESS) == 5.5,
                "Melee pilot gets the approved stronger survival bonuses");
        check(flight.intervalTicks(flight.attribute(50, CatStat.SPEED)) == 35
                        && Math.abs(CatAttributeEffects.attackDamage(flight.attribute(50, CatStat.ATTACK))
                                * flight.value(CatSuitSetting.DAMAGE) - 13.6) < .00001,
                "Pilot reload adjusted, burst damage unchanged");
    }

    private static double stored(CatOutfitType outfit, CatSuitSetting setting) {
        return setting == CatSuitSetting.DAMAGE ? ServerConfig.CAREER_DAMAGE_COEFFICIENTS.get(outfit).get()
                : ServerConfig.CAREER_SETTINGS.get(outfit).get(setting).get().doubleValue();
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
            check(reset.size() == (selected.isSupport() ? 9 : 13), "No unsupported support attack controls");
            clean.putBoolean("can_edit", false);
            check(ClientWorldSettings.editableSuitDefaults(clean, selected).isEmpty(), "Read-only reset blocked");
        }
    }

    private static void formulas() {
        for (int i = 0; i < ServerConfig.CAREERS.size(); i++) {
            var settings = CatSuitSettings.current(ServerConfig.CAREERS.get(i));
            for (int speed = 0; speed <= 999; speed++) {
                int old = (int) Math.max(DEFAULTS[i][3], Math.round(DEFAULTS[i][1] - DEFAULTS[i][2] * speed));
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
        int[] contextIndex = {5,4,7,3,6,8,9,15,16,17};
        for (int i = 0; i < ServerConfig.CAREERS.size(); i++) {
            CatOutfitType outfit = ServerConfig.CAREERS.get(i);
            Object[] flags = new Object[19];
            java.util.Arrays.fill(flags, false);
            if(i<contextIndex.length)flags[contextIndex[i]] = true;
            flags[18] = outfit;
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
        Object[] flags = new Object[19];
        java.util.Arrays.fill(flags, false);
        flags[18] = CatOutfitType.NONE;
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
