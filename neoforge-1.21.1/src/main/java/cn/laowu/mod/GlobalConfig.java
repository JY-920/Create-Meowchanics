package cn.laowu.mod;

import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.genetics.CatTraitConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.EnumMap;
import java.util.Map;

/** Instance-wide overrides. Only the host/server file is authoritative in a world. */
public final class GlobalConfig {
    public static final String FILE_NAME = "laowu-global.toml";
    public static final String[] SWITCHES = {"show_hell_recipes", "wild_cats_flee", "cats_hiss",
            ServerConfig.DEATH_PENALTY_ENABLED_KEY};
    public static final ModConfigSpec.ConfigValue<Number> DEATH_ATTRIBUTE_LOSS;
    public static final ModConfigSpec.ConfigValue<Number> DEATH_OUTCOME;
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<java.util.List<?>> DISABLED_TRAITS;
    public static final Map<CatStat, ModConfigSpec.ConfigValue<Number>> MULTIPLIERS = new EnumMap<>(CatStat.class);
    public static final Map<CatOutfitType, ModConfigSpec.ConfigValue<Number>> CAREER_DAMAGE_COEFFICIENTS = new EnumMap<>(CatOutfitType.class);
    public static final Map<CatOutfitType, Map<CatSuitSetting, ModConfigSpec.ConfigValue<Number>>> CAREER_SETTINGS = new EnumMap<>(CatOutfitType.class);
    private static final Map<String, ModConfigSpec.ConfigValue<Number>> SWITCH_VALUES = new java.util.LinkedHashMap<>();

    static {
        var b = new ModConfigSpec.Builder();
        DISABLED_TRAITS = b.comment(CatTraitConfig.comments(true))
                .<java.util.List<?>>define(CatTraitConfig.KEY, java.util.List.of(-1),
                        value -> CatTraitConfig.validList(value, true));
        b.comment("全局覆盖：-1=使用各存档独立设置；0=强制关闭；1=强制开启。",
                "覆盖时游戏内对应选项锁定，但不会改写存档原值；改回-1即可恢复。",
                "联机时只以服务器的此文件为准，客户端本地文件不能覆盖服务器规则。",
                "Global overrides: -1 = per-world setting; 0 = force off; 1 = force on.",
                "Only the server/host config is authoritative. Existing world values are preserved.",
                "是否在JEI展示地狱配方；仅影响展示。");
        SWITCH_VALUES.put("show_hell_recipes", b.<Number>define("show_hell_recipes", -1, GlobalConfig::validSwitch));
        SWITCH_VALUES.put("wild_cats_flee", b.comment("野生猫是否使用原版避人/受伤逃跑AI。-1=存档，0=关，1=开。")
                .<Number>define("wild_cats_flee", -1, GlobalConfig::validSwitch));
        SWITCH_VALUES.put("cats_hiss", b.comment("猫咪是否哈气；航空箱产气不受此开关影响。-1=存档，0=关，1=开。")
                .<Number>define("cats_hiss", -1, GlobalConfig::validSwitch));
        SWITCH_VALUES.put(ServerConfig.DEATH_PENALTY_ENABLED_KEY,
                b.comment("死亡变猫饼时是否随机扣除一项基础属性。-1=存档，0=关，1=开；存档默认开启。")
                        .<Number>define(ServerConfig.DEATH_PENALTY_ENABLED_KEY, -1, GlobalConfig::validSwitch));
        DEATH_OUTCOME = b.comment("驯养猫死亡处理：-1=存档，0=不生成猫饼但正常掉落装备，1=猫饼物品，2=猫饼生物。")
                .<Number>define(ServerConfig.DEATH_OUTCOME_KEY, -1, value -> value instanceof Number number
                        && Double.isFinite(number.doubleValue()) && number.doubleValue() == Math.rint(number.doubleValue())
                        && number.doubleValue() >= -1 && number.doubleValue() <= 2);
        DEATH_ATTRIBUTE_LOSS = b.comment("死亡时随机一项基础属性的扣除点数；-1=存档（默认20），0=不扣除。",
                "非负整数强制覆盖并锁定；最低扣到0，不改变属性上限。死亡扣属性开关关闭时不生效。")
                .<Number>define(ServerConfig.DEATH_ATTRIBUTE_LOSS_KEY, -1, GlobalConfig::validDeathAttributeLoss);
        b.comment("属性倍率：每项-1=使用存档设置（存档默认1）；0~999999=全局强制倍率。",
                "仅缩放实际能力公式的属性输入，不改写猫咪基础属性、培养上限或套装固定加成。",
                "Each multiplier: -1 = per-world (default 1); 0..999999 = forced global value.",
                "Values between -1 and 0, NaN and infinity are invalid.")
                .push("attribute_multipliers");
        for (CatStat stat : CatStat.values()) {
            // Number accepts both TOML integers (-1, 2) and decimals (-1.0, 1.5).
            MULTIPLIERS.put(stat, b.<Number>define(stat.serializedName(), -1.0D,
                    GlobalConfig::validMultiplier));
        }
        b.pop();
        b.comment("职业套装公式系数K：-1=使用存档；0~999999=强制覆盖并锁定。",
                "直接替换公式内的原系数，不是整体伤害倍率；存档默认使用各套装原值。物流猫仍不主动攻击。",
                "Career formula coefficient K: -1 = per-world; 0..999999 = forced coefficient, not an extra damage multiplier.")
                .push(ServerConfig.CAREER_DAMAGE_TAG);
        for (CatOutfitType outfit : ServerConfig.CAREERS)
            CAREER_DAMAGE_COEFFICIENTS.put(outfit, b.comment(outfit.damageCoefficientConfigComment(),
                    "-1 = 沿用存档；填写 0~999999 则强制使用该 K，并锁定游戏内对应选项。")
                    .<Number>define(outfit.id(), -1.0D, GlobalConfig::validMultiplier));
        b.pop();
        b.comment("逐套装全局覆盖：每项 -1 使用存档；其他有效值强制覆盖并锁定，解除覆盖恢复存档原值。",
                "攻击间隔=max(最短间隔, round(基础间隔-速度缩短系数×速度属性×速度倍率))，20tick=1秒。",
                "六维属性是套装临时加成，不修改基因；物流猫攻击参数无效。")
                .push(CatSuitSetting.TAG);
        for (CatOutfitType outfit : ServerConfig.CAREERS) {
            b.push(outfit.id());
            var settings = new EnumMap<CatSuitSetting, ModConfigSpec.ConfigValue<Number>>(CatSuitSetting.class);
            for (CatSuitSetting setting : CatSuitSetting.EXTRA)
                settings.put(setting, b.comment(setting.comment(outfit), "-1 = 沿用存档；其他有效值全局固定并锁定。")
                        .<Number>define(setting.id(), -1.0D, value -> setting.validConfig(value, true)));
            CAREER_SETTINGS.put(outfit, settings);
            b.pop();
        }
        b.pop();
        SPEC = b.build();
    }

    static boolean validMultiplier(Object value) {
        if (!(value instanceof Number number)) return false;
        double v = number.doubleValue();
        return v == -1.0D || ServerConfig.validMultiplier(v);
    }

    static boolean validSwitch(Object value) {
        if (!(value instanceof Number number)) return false;
        double v = number.doubleValue();
        return v == -1.0D || v == 0.0D || v == 1.0D;
    }

    static boolean validDeathAttributeLoss(Object value) {
        if (!(value instanceof Number number)) return false;
        return number.doubleValue() == -1 || ServerConfig.validDeathAttributeLoss(number.doubleValue());
    }
    private static int deathAttributeLossOverride() {
        return SPEC.isLoaded() ? DEATH_ATTRIBUTE_LOSS.get().intValue() : -1;
    }
    public static int deathAttributeLoss(int worldValue) {
        int override = deathAttributeLossOverride();
        return override < 0 ? worldValue : override;
    }
    private static int deathOutcomeOverride() {
        return SPEC.isLoaded() ? DEATH_OUTCOME.get().intValue() : -1;
    }
    public static int deathOutcome(int worldValue) {
        int override = deathOutcomeOverride();
        return override < 0 ? worldValue : override;
    }

    private static double multiplierOverride(CatStat stat) {
        return SPEC.isLoaded() ? MULTIPLIERS.get(stat).get().doubleValue() : -1.0D;
    }

    private static int switchOverride(String key) {
        var value = SWITCH_VALUES.get(key);
        if (value == null) throw new IllegalArgumentException("Unknown world setting: " + key);
        return SPEC.isLoaded() ? value.get().intValue() : -1;
    }

    public static double multiplier(CatStat stat, double worldValue) {
        double override = multiplierOverride(stat);
        return override < 0 ? worldValue : override;
    }

    public static boolean switchValue(String key, boolean worldValue) {
        int override = switchOverride(key);
        return override < 0 ? worldValue : override == 1;
    }
    private static double careerDamageOverride(CatOutfitType outfit) {
        return SPEC.isLoaded() ? CAREER_DAMAGE_COEFFICIENTS.get(outfit).get().doubleValue() : -1.0D;
    }
    public static double careerDamageCoefficient(CatOutfitType outfit, double worldValue) {
        double override = careerDamageOverride(outfit);
        return override < 0 ? worldValue : override;
    }

    public static boolean isLocked(String key) {
        if (ServerConfig.DEATH_OUTCOME_KEY.equals(key)) return deathOutcomeOverride() >= 0;
        if (ServerConfig.DEATH_ATTRIBUTE_LOSS_KEY.equals(key)) return deathAttributeLossOverride() >= 0;
        if (CatTraitConfig.KEY.equals(key))
            return SPEC.isLoaded() && !CatTraitConfig.inherits(DISABLED_TRAITS.get());
        for (CatStat stat : CatStat.values()) {
            if (stat.serializedName().equals(key)) return multiplierOverride(stat) >= 0;
        }
        for (CatOutfitType outfit : ServerConfig.CAREERS) {
            if (ServerConfig.careerDamageKey(outfit).equals(key)) return careerDamageOverride(outfit) >= 0;
            for (CatSuitSetting setting : CatSuitSetting.EXTRA)
                if (setting.lockKey(outfit).equals(key)) return careerSettingOverride(outfit, setting) >= 0;
        }
        return switchOverride(key) >= 0;
    }

    private static double careerSettingOverride(CatOutfitType outfit, CatSuitSetting setting) {
        return SPEC.isLoaded() ? CAREER_SETTINGS.get(outfit).get(setting).get().doubleValue() : -1.0D;
    }
    public static double careerSetting(CatOutfitType outfit, CatSuitSetting setting, double worldValue) {
        double override = careerSettingOverride(outfit, setting);
        return override < 0 ? worldValue : override;
    }

    public static java.util.Set<String> disabledTraitIds(java.util.Set<String> worldValue) {
        return isLocked(CatTraitConfig.KEY) ? CatTraitConfig.ids(DISABLED_TRAITS.get()) : worldValue;
    }
    private GlobalConfig() {}
}
