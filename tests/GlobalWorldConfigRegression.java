package cn.laowu.mod;

import cn.laowu.mod.genetics.CatStat;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import net.minecraft.nbt.CompoundTag;
import java.nio.file.Files;
import java.nio.file.Path;

/** Runs the real config specs, world resolver and write guards without a game world. */
public final class GlobalWorldConfigRegression {
    private static int checks;

    public static void main(String[] args) throws Exception {
        if (Runtime.version().feature() >= 21) {
            // NeoForge's named 1.21 Attribute codec references built-in registries.
            // Bootstrap vanilla registries without opening a window or a world.
            java.io.PrintStream out = System.out, err = System.err;
            try {
                Class.forName("net.minecraft.SharedConstants").getMethod("tryDetectVersion").invoke(null);
                Class.forName("net.minecraft.server.Bootstrap").getMethod("bootStrap").invoke(null);
            } finally {
                System.setOut(out);
                System.setErr(err);
            }
        }
        Object worldSpec = ServerConfig.SPEC;
        Object globalSpec = GlobalConfig.SPEC;
        check(ServerConfig.multiplier(CatStat.HEALTH) == 1, "Unloaded default multiplier");
        check(ServerConfig.MAX_MULTIPLIER == 999999, "Multiplier cap");
        check(ServerConfig.CAREERS.size() == 7, "All seven careers, not NONE");
        for (CatOutfitType outfit : ServerConfig.CAREERS)
            check(ServerConfig.careerDamageCoefficient(outfit) == outfit.defaultDamageCoefficient(), "Unloaded career default " + outfit);
        check(ServerConfig.catsHiss() && !ServerConfig.wildCatsFlee(), "Unloaded switch defaults");
        CommentedConfig legacyWorld = CommentedConfig.inMemory();
        legacyWorld.set("attribute_multipliers.health", 2D);
        legacyWorld.set("show_hell_recipes", true);
        for (CatOutfitType outfit : ServerConfig.CAREERS)
            legacyWorld.set("career_damage_multipliers." + outfit.id(), 1D);
        load(worldSpec, legacyWorld);
        for (CatOutfitType outfit : ServerConfig.CAREERS)
            check(ServerConfig.careerDamageCoefficient(outfit) == outfit.defaultDamageCoefficient(),
                    "Old outer factor 1 must not replace default K " + outfit);
        check(ServerConfig.multiplier(CatStat.HEALTH) == 2, "Unrelated legacy world multiplier preserved");
        check(ServerConfig.showHellRecipes(), "Unrelated legacy world switch preserved");
        CommentedConfig global = CommentedConfig.inMemory();
        load(globalSpec, global);
        CommentedConfig worldA = CommentedConfig.inMemory();
        load(worldSpec, worldA);
        String[] suitNames = {"机械套装", "钓鱼套装", "飞行套装", "喷火套装", "采蜜套装", "物流套装", "雷管套装"};
        for (int i = 0; i < suitNames.length; i++) {
            CatOutfitType outfit = ServerConfig.CAREERS.get(i);
            String path = ServerConfig.CAREER_DAMAGE_TAG + "." + outfit.id();
            for (CommentedConfig config : new CommentedConfig[]{global, worldA}) {
                String comment = config.getComment(path);
                check(comment != null && comment.contains(suitNames[i]), "Generated TOML names the suit " + outfit);
                check(comment.contains("K = " + outfit.defaultDamageCoefficient()), "Generated TOML gives original K " + outfit);
            }
            check(global.getComment(path).contains("-1 = 沿用存档"), "Global sentinel is explained per suit");
            check(GlobalConfig.CAREER_DAMAGE_COEFFICIENTS.get(outfit).get().doubleValue() == -1,
                    "Adding comments must not force world overrides");
        }
        ServerConfig.MULTIPLIERS.get(CatStat.ATTACK).set(1.5D);
        ServerConfig.MULTIPLIERS.get(CatStat.HEALTH).set(0.75D);
        check(ServerConfig.multiplier(CatStat.ATTACK) == 1.5D, "-1 must use world A");
        check(!locked("attack"), "-1 must leave the UI unlocked");

        for (CatStat stat : CatStat.values()) {
            double stored = ServerConfig.MULTIPLIERS.get(stat).get();
            for (Number value : new Number[]{0, 1, 2L, 1.25D, 50, 999999}) {
                setGlobal(global, "attribute_multipliers." + stat.serializedName(), value);
                check(ServerConfig.multiplier(stat) == value.doubleValue(), "Global multiplier " + stat);
                check(locked(stat.serializedName()), "Forced multiplier must lock " + stat);
                check(ServerConfig.scale(stat, 100) == 100 * value.doubleValue(), "Actual formula input " + stat);
                check(ServerConfig.MULTIPLIERS.get(stat).get() == stored, "Override overwrote world value " + stat);
            }
            setGlobal(global, "attribute_multipliers." + stat.serializedName(), -1);
            check(ServerConfig.multiplier(stat) == stored && !locked(stat.serializedName()),
                    "Removing override must restore world value " + stat);
        }
        for (CatOutfitType outfit : ServerConfig.CAREERS) {
            String key = ServerConfig.careerDamageKey(outfit);
            String path = ServerConfig.CAREER_DAMAGE_TAG + "." + outfit.id();
            check(ServerConfig.CAREER_DAMAGE_COEFFICIENTS.get(outfit).get() == outfit.defaultDamageCoefficient(), "World career default");
            ServerConfig.CAREER_DAMAGE_COEFFICIENTS.get(outfit).set(1.25D);
            check(ServerConfig.careerDamageCoefficient(outfit) == 1.25D, "World career independent value");
            check(!locked(key), "World career editable");
            for (Number value : new Number[]{0, 1, 2L, 0.5D, 50, 999999}) {
                setGlobal(global, path, value);
                check(ServerConfig.careerDamageCoefficient(outfit) == value.doubleValue(), "Career override " + outfit);
                check(locked(key), "Career override lock " + outfit);
                check(Math.abs(nativeAttack(8, ServerConfig.careerDamageCoefficient(outfit))
                        - Math.min(2048, 8 * value.doubleValue())) < 1.0E-6D, "Native formula coefficient " + outfit);
                check(ServerConfig.CAREER_DAMAGE_COEFFICIENTS.get(outfit).get() == 1.25D, "Career raw value preserved");
                check(ServerConfig.multiplier(CatStat.ATTACK) == 1.5D, "Career does not override combat stat");
            }
            setGlobal(global, path, -1);
            check(ServerConfig.careerDamageCoefficient(outfit) == 1.25D && !locked(key), "Career restore " + outfit);
            int revision = ServerConfig.snapshot().getInt("revision");
            setGlobal(global, path, 1.25D);
            check(ServerConfig.snapshot().getInt("revision") != revision, "Career lock-only change invalidates drafts");
            setGlobal(global, path, -1);
        }
        check(ServerConfig.careerDamageCoefficient(CatOutfitType.NONE) == 1, "Unsuited cats keep their native damage");

        double[] originals = {0.75D, 0.55D, 2.0D, 0.6D, 0.85D, 0D, 1.35D};
        for (int i = 0; i < originals.length; i++) {
            check(ServerConfig.CAREERS.get(i).defaultDamageCoefficient() == originals[i],
                    "Original suit balance " + ServerConfig.CAREERS.get(i));
        }
        check(Math.abs(nativeAttack(10, 0.75D) - 7.5D) < 1.0E-6D, "Original mechanical formula");
        check(Math.abs(nativeAttack(10, 1.2D) - 12D) < 1.0E-6D, "K=1.2 replaces 0.75; not 9 damage");
        check(Math.abs(nativeAttack(10, 0.6D) - 6D) < 1.0E-6D, "Original fire formula");
        check(nativeAttack(10, 999999D) == 2048D, "Coefficient stays inside vanilla attribute limit");
        check(nativeAttack(10, 0D) == 0D, "K=0 has no base damage");
        check(ServerConfig.scaleDamage(8, 2) == 16, "Damage multiplier applies once");
        check(ServerConfig.scaleDamage(0, 999999) == 0, "Zero baseline remains zero");
        check(ServerConfig.scaleDamage(Double.POSITIVE_INFINITY, 0) == 0, "Disabled damage avoids NaN");
        check(ServerConfig.scaleDamage(Double.MAX_VALUE, 999999) == Float.MAX_VALUE, "Damage overflow stays finite");
        check(ServerConfig.scaleDamage(Double.NaN, 2) == 0, "Invalid damage is safe");
        check(ServerConfig.scaleDamage(8, Double.NaN) == 0, "Invalid damage factor is safe");
        for (String key : GlobalConfig.SWITCHES) {
            boolean stored = ServerConfig.snapshot().getBoolean(key);
            for (int value : new int[]{0, 1}) {
                setGlobal(global, key, value);
                check(ServerConfig.snapshot().getBoolean(key) == (value == 1), "Forced switch " + key);
                check(locked(key), "Switch lock " + key);
            }
            setGlobal(global, key, -1);
            check(ServerConfig.snapshot().getBoolean(key) == stored && !locked(key),
                    "Restored switch " + key);
        }

        int unlockedRevision = ServerConfig.snapshot().getInt("revision");
        setGlobal(global, "attribute_multipliers.attack", 1.5D);
        check(ServerConfig.snapshot().getInt("revision") != unlockedRevision,
                "A lock change must invalidate drafts even if the effective value is unchanged");
        setGlobal(global, "attribute_multipliers.attack", 2);
        setGlobal(global, "cats_hiss", 0);
        setGlobal(global, "career_damage_coefficients.fire", 2);
        CompoundTag forged = ServerConfig.snapshot();
        forged.put(ServerConfig.LOCKS_TAG, new CompoundTag()); // Client tries to remove the locks.
        forged.putDouble("attack", 49);
        forged.putDouble("health", 3);
        forged.putBoolean("cats_hiss", true);
        forged.getCompound(ServerConfig.CAREER_DAMAGE_TAG).putDouble("fire", 999999);
        forged.getCompound(ServerConfig.CAREER_DAMAGE_TAG).putDouble("honey", 3);
        check(ServerConfig.valid(forged), "Forged request should pass range checks for this guard test");
        ServerConfig.applyUnlockedValues(forged);
        check(ServerConfig.MULTIPLIERS.get(CatStat.ATTACK).get() == 1.5D,
                "A client cannot change a globally locked raw world multiplier");
        check(ServerConfig.CATS_HISS.get(), "Locked world switch was overwritten");
        check(ServerConfig.multiplier(CatStat.ATTACK) == 2 && !ServerConfig.catsHiss(),
                "Forged request bypassed effective global values");
        check(ServerConfig.multiplier(CatStat.HEALTH) == 3, "Unlocked fields must still save");
        check(ServerConfig.CAREER_DAMAGE_COEFFICIENTS.get(CatOutfitType.FIRE).get() == 1.25D,
                "Client cannot overwrite locked career world data");
        check(ServerConfig.careerDamageCoefficient(CatOutfitType.FIRE) == 2, "Forged career lock ignored");
        check(ServerConfig.careerDamageCoefficient(CatOutfitType.HONEY) == 3, "Unlocked career saves");

        CommentedConfig worldB = CommentedConfig.inMemory();
        load(worldSpec, worldB);
        ServerConfig.MULTIPLIERS.get(CatStat.ATTACK).set(4.0D);
        ServerConfig.CATS_HISS.set(false);
        ServerConfig.CAREER_DAMAGE_COEFFICIENTS.get(CatOutfitType.FIRE).set(4D);
        check(ServerConfig.careerDamageCoefficient(CatOutfitType.FIRE) == 2, "Career global applies in world B");
        setGlobal(global, "career_damage_coefficients.fire", -1);
        check(ServerConfig.careerDamageCoefficient(CatOutfitType.FIRE) == 4, "Career world B restore");
        check(ServerConfig.multiplier(CatStat.ATTACK) == 2 && !ServerConfig.catsHiss(),
                "Global overrides must apply to a second world too");
        setGlobal(global, "attribute_multipliers.attack", -1.0D);
        setGlobal(global, "cats_hiss", -1);
        check(ServerConfig.multiplier(CatStat.ATTACK) == 4 && !ServerConfig.catsHiss(),
                "World B must keep its independent originals");
        load(worldSpec, worldA);
        check(ServerConfig.careerDamageCoefficient(CatOutfitType.FIRE) == 1.25D, "Career world A restore");
        check(ServerConfig.multiplier(CatStat.ATTACK) == 1.5D && ServerConfig.catsHiss(),
                "Returning to world A must restore its independent originals");

        setGlobal(global, "attribute_multipliers.attack", 50);
        setGlobal(global, "career_damage_coefficients.fire", 999999);
        setGlobal(global, "cats_hiss", 0);
        ServerConfig.setRemoteWorld(true);
        check(ServerConfig.careerDamageCoefficient(CatOutfitType.FIRE) == 1.25D, "Do not use local global career before server sync");
        check(ServerConfig.multiplier(CatStat.ATTACK) == 1.5D && ServerConfig.catsHiss(),
                "Local global config must not apply while waiting for a remote server");
        CompoundTag remote = ServerConfig.snapshot();
        remote.putDouble("attack", 7);
        remote.putBoolean("cats_hiss", true);
        remote.getCompound(ServerConfig.CAREER_DAMAGE_TAG).putDouble("fire", 7);
        remote.getCompound(ServerConfig.LOCKS_TAG).putBoolean(ServerConfig.careerDamageKey(CatOutfitType.FIRE), true);
        remote.getCompound(ServerConfig.LOCKS_TAG).putBoolean("attack", true);
        ServerConfig.receiveMirror(remote);
        remote.putDouble("attack", 9);
        remote.getCompound(ServerConfig.CAREER_DAMAGE_TAG).putDouble("fire", 9);
        check(ServerConfig.careerDamageCoefficient(CatOutfitType.FIRE) == 7, "Server career mirror is authoritative and copied");
        check(locked(ServerConfig.careerDamageKey(CatOutfitType.FIRE)), "Career server lock sync");
        check(ServerConfig.CAREER_DAMAGE_COEFFICIENTS.get(CatOutfitType.FIRE).get() == 1.25D, "Career mirror never writes local world");
        check(ServerConfig.multiplier(CatStat.ATTACK) == 7 && ServerConfig.catsHiss(),
                "Server mirror must override local config and copy the packet");
        check(locked("attack"), "Server lock flags must reach the client");
        check(ServerConfig.MULTIPLIERS.get(CatStat.ATTACK).get() == 1.5D && ServerConfig.CATS_HISS.get(),
                "Receiving a mirror must not rewrite world data");
        ServerConfig.snapshot().putDouble("attack", 11);
        ServerConfig.snapshot().getCompound(ServerConfig.CAREER_DAMAGE_TAG).putDouble("fire", 11);
        check(ServerConfig.careerDamageCoefficient(CatOutfitType.FIRE) == 7, "Nested career snapshot defensive copy");
        check(ServerConfig.multiplier(CatStat.ATTACK) == 7, "Snapshot must be a defensive copy");
        ServerConfig.resetWorldState();
        check(ServerConfig.careerDamageCoefficient(CatOutfitType.FIRE) == 999999, "Clear remote career between worlds");
        check(ServerConfig.multiplier(CatStat.ATTACK) == 50 && !ServerConfig.catsHiss(),
                "Remote values leaked into a new local world");

        for (Object value : new Object[]{-2, -0.5D, 1000000D, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, true, "2"}) {
            check(!GlobalConfig.validMultiplier(value), "Invalid multiplier accepted: " + value);
            CommentedConfig invalid = CommentedConfig.inMemory();
            invalid.set("attribute_multipliers.attack", value);
            invalid.set("career_damage_coefficients.fire", value);
            load(globalSpec, invalid); // The actual loader spec must correct invalid files to -1.
            check(!GlobalConfig.isLocked("attack"), "Spec failed to correct invalid multiplier: " + value);
            check(!GlobalConfig.isLocked(ServerConfig.careerDamageKey(CatOutfitType.FIRE)), "Spec failed to correct invalid career: " + value);
        }
        for (double value : new double[]{0, 0.125D, 50.1D, 999999}) {
            CompoundTag values = ServerConfig.snapshot();
            for (CatStat stat : CatStat.values()) values.putDouble(stat.serializedName(), value);
            for (CatOutfitType outfit : ServerConfig.CAREERS)
                values.getCompound(ServerConfig.CAREER_DAMAGE_TAG).putDouble(outfit.id(), value);
            check(ServerConfig.valid(values), "World input boundary accepted: " + value);
            ServerConfig.applyUnlockedValues(values);
            for (CatStat stat : CatStat.values())
                check(ServerConfig.MULTIPLIERS.get(stat).get() == value, "World stat input saves");
            for (CatOutfitType outfit : ServerConfig.CAREERS)
                check(ServerConfig.CAREER_DAMAGE_COEFFICIENTS.get(outfit).get() == value, "World career input saves");
        }
        for (double value : new double[]{-1, -0.1D, 1000000, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            for (CatStat stat : CatStat.values()) {
                CompoundTag invalid = ServerConfig.snapshot();
                invalid.putDouble(stat.serializedName(), value);
                check(!ServerConfig.valid(invalid), "Invalid world stat " + stat + ": " + value);
            }
            for (CatOutfitType outfit : ServerConfig.CAREERS) {
                CompoundTag invalid = ServerConfig.snapshot();
                invalid.getCompound(ServerConfig.CAREER_DAMAGE_TAG).putDouble(outfit.id(), value);
                check(!ServerConfig.valid(invalid), "Invalid world career " + outfit + ": " + value);
            }
        }
        CompoundTag incomplete = ServerConfig.snapshot();
        incomplete.remove(ServerConfig.CAREER_DAMAGE_TAG);
        check(!ServerConfig.valid(incomplete), "Legacy packet without careers rejected");
        for (CatOutfitType outfit : ServerConfig.CAREERS) {
            incomplete = ServerConfig.snapshot();
            incomplete.getCompound(ServerConfig.CAREER_DAMAGE_TAG).remove(outfit.id());
            check(!ServerConfig.valid(incomplete), "Missing career rejected");
            incomplete.getCompound(ServerConfig.CAREER_DAMAGE_TAG).putInt(outfit.id(), 1);
            check(!ServerConfig.valid(incomplete), "Incorrect NBT career type rejected");
        }
        for (Object value : new Object[]{-2, -0.5D, 0.5D, 2, Double.NaN, true, "true"}) {
            check(!GlobalConfig.validSwitch(value), "Invalid switch accepted: " + value);
            CommentedConfig invalid = CommentedConfig.inMemory();
            invalid.set("cats_hiss", value);
            load(globalSpec, invalid);
            check(!GlobalConfig.isLocked("cats_hiss"), "Spec failed to correct invalid switch: " + value);
        }
        if (args.length > 0) {
            CommentedConfig sample = new TomlParser().parse(Files.readString(Path.of(args[0])));
            load(globalSpec, sample);
            for (CatStat stat : CatStat.values())
                check(!GlobalConfig.isLocked(stat.serializedName()), "Sample multiplier default");
            for (String key : GlobalConfig.SWITCHES)
                check(!GlobalConfig.isLocked(key), "Sample switch default");
            for (CatOutfitType outfit : ServerConfig.CAREERS)
                check(!GlobalConfig.isLocked(ServerConfig.careerDamageKey(outfit)), "Sample career default");
        }
        TraitBlacklistRegression.run();
        CatInteractionFixesRegression.run();
        CareerDefaultsAndKnockbackRegression.run();
        SuitSettingsRegression.run();
        System.out.println("PASS: " + checks + " checks; both worlds, all overrides, unlock/restore, "
                + "formula coefficients, 999999 cap, forged-lock write guard, remote authority, reloads and TOML validation");
    }


    /** Exercise the real vanilla AttributeInstance operation used by CareerCatBehavior. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static double nativeAttack(double base, double coefficient) throws Exception {
        Class<?> attribute = Class.forName("net.minecraft.world.entity.ai.attributes.Attribute");
        Class<?> ranged = Class.forName("net.minecraft.world.entity.ai.attributes.RangedAttribute");
        Object value = ranged.getConstructor(String.class, double.class, double.class, double.class)
                .newInstance("test.career_damage", base, 0D, 2048D);
        Class<?> instance = Class.forName("net.minecraft.world.entity.ai.attributes.AttributeInstance");
        Class<?> operation = Class.forName("net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation");
        Class<?> modifier = Class.forName("net.minecraft.world.entity.ai.attributes.AttributeModifier");
        Object target, modification;
        try {
            target = instance.getConstructor(attribute, java.util.function.Consumer.class)
                    .newInstance(value, (java.util.function.Consumer<Object>) ignored -> {});
            modification = modifier.getConstructor(java.util.UUID.class, String.class, double.class, operation)
                    .newInstance(java.util.UUID.fromString("a01ea9c1-ee05-44cf-acf8-d786966cdf50"),
                            "test career coefficient", coefficient - 1D,
                            Enum.valueOf((Class) operation, "MULTIPLY_TOTAL"));
        } catch (NoSuchMethodException neoForge) {
            Class<?> holder = Class.forName("net.minecraft.core.Holder");
            Object wrapped = holder.getMethod("direct", Object.class).invoke(null, value);
            target = instance.getConstructor(holder, java.util.function.Consumer.class)
                    .newInstance(wrapped, (java.util.function.Consumer<Object>) ignored -> {});
            Class<?> resource = Class.forName("net.minecraft.resources.ResourceLocation");
            Object id = resource.getMethod("fromNamespaceAndPath", String.class, String.class)
                    .invoke(null, "laowu", "test_career_coefficient");
            modification = modifier.getConstructor(resource, double.class, operation)
                    .newInstance(id, coefficient - 1D,
                            Enum.valueOf((Class) operation, "ADD_MULTIPLIED_TOTAL"));
        }
        instance.getMethod("addTransientModifier", modifier).invoke(target, modification);
        return (double) instance.getMethod("getValue").invoke(target);
    }

    private static boolean locked(String key) {
        return ServerConfig.snapshot().getCompound(ServerConfig.LOCKS_TAG).getBoolean(key);
    }
    private static void setGlobal(CommentedConfig config, String key, Number value) throws Exception {
        config.set(key, value);
        GlobalConfig.SPEC.getClass().getMethod("afterReload").invoke(GlobalConfig.SPEC);
    }
    static void load(Object spec, CommentedConfig config) throws Exception {
        // Correct first to avoid noisy expected warnings while testing invalid values.
        spec.getClass().getMethod("correct", CommentedConfig.class).invoke(spec, config);
        try {
            spec.getClass().getMethod("setConfig", CommentedConfig.class).invoke(spec, config);
        } catch (NoSuchMethodException neoForge) {
            Class<?> loaded = Class.forName("net.neoforged.fml.config.IConfigSpec$ILoadedConfig");
            // This API is sealed. Construct its real in-memory handle with no disk path;
            // all values were corrected above, so acceptConfig never needs to save it.
            var constructor = Class.forName("net.neoforged.fml.config.LoadedConfig")
                    .getDeclaredConstructor(CommentedConfig.class, Path.class,
                            Class.forName("net.neoforged.fml.config.ModConfig"));
            constructor.setAccessible(true);
            Object handle = constructor.newInstance(config, null, null);
            spec.getClass().getMethod("acceptConfig", loaded).invoke(spec, handle);
        }
    }
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
}
