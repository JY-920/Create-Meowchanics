package cn.laowu.mod;

import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitConfig;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;

/** World-owned settings. Clients receive a mirror, never authority over the world. */
public final class ServerConfig {
    public static final double MAX_MULTIPLIER = 999_999.0D;
    public static final String CAREER_DAMAGE_TAG = "career_damage_coefficients";
    public static final java.util.List<CatOutfitType> CAREERS = java.util.Arrays.stream(CatOutfitType.values())
.filter(outfit -> outfit != CatOutfitType.NONE && !outfit.isPreviewOnly()).toList();
    public static final String LOCKS_TAG = "global_locks";
    public static final String DEATH_PENALTY_ENABLED_KEY = "cat_death_attribute_penalty_enabled";
    public static final String DEATH_ATTRIBUTE_LOSS_KEY = "cat_death_attribute_loss";
    public static final String DEATH_OUTCOME_KEY = "cat_death_outcome";
    public static final int DEATH_NONE = 0, DEATH_ITEM = 1, DEATH_ENTITY = 2;
    public static final ForgeConfigSpec.IntValue DEATH_OUTCOME;
    public static final int DEFAULT_DEATH_ATTRIBUTE_LOSS = 20;
    public static final ForgeConfigSpec.BooleanValue DEATH_ATTRIBUTE_PENALTY_ENABLED;
    public static final ForgeConfigSpec.IntValue DEATH_ATTRIBUTE_LOSS;
    public static final ForgeConfigSpec.IntValue INTERNAL_BALANCE_REVISION;
    public static final ForgeConfigSpec SPEC;
    public static final Map<CatStat, ForgeConfigSpec.DoubleValue> MULTIPLIERS = new EnumMap<>(CatStat.class);
    public static final Map<CatOutfitType, ForgeConfigSpec.DoubleValue> CAREER_DAMAGE_COEFFICIENTS = new EnumMap<>(CatOutfitType.class);
    public static final Map<CatOutfitType, Map<CatSuitSetting, ForgeConfigSpec.ConfigValue<Number>>> CAREER_SETTINGS = new EnumMap<>(CatOutfitType.class);
    public static final ForgeConfigSpec.BooleanValue SHOW_HELL_RECIPES;
    public static final ForgeConfigSpec.BooleanValue WILD_CATS_FLEE;
    public static final ForgeConfigSpec.BooleanValue CATS_HISS;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<?>> DISABLED_TRAITS;
    private static final Map<ServerPlayer, CompoundTag> SENT = new WeakHashMap<>();
    private static volatile CompoundTag remoteValues;
    private static volatile boolean remoteWorld;
    static {
        var b = new ForgeConfigSpec.Builder();
        b.comment("世界/服务器配置。游戏内按 V -> 世界设置；仅 OP 等级2及以上或单人世界主人可修改。",
                "倍率只缩放实际能力公式中的属性输入，不修改基础属性、培养上限或遗传数据。",
                "1=默认，0=仅保留公式固定项；范围0~999999。套装固定生命/护甲/韧性加成不缩放。")
                .push("attribute_multipliers");
        for (CatStat stat : CatStat.values()) {
            MULTIPLIERS.put(stat, b.defineInRange(stat.serializedName(), 1.0D, 0.0D, MAX_MULTIPLIER));
        }
        b.pop();
        b.comment("职业套装公式系数K：伤害=(2+0.08×战斗力属性×战斗力倍率)×K，范围0~999999。",
                "直接替换原套装系数，不再额外相乘。默认：机械0.75、钓鱼0.55、飞行2、喷火0.75、采蜜1.2、物流0、雷管1.5、工程3、医疗0、音乐0。",
                "辅助猫不会攻击。原版生命、护甲等生物属性上限仍然适用。")
                .push(CAREER_DAMAGE_TAG);
        for (CatOutfitType outfit : CAREERS)
            CAREER_DAMAGE_COEFFICIENTS.put(outfit, b.comment(outfit.damageCoefficientConfigComment())
                    .defineInRange(outfit.id(), outfit.defaultDamageCoefficient(), 0.0D, MAX_MULTIPLIER));
        b.pop();
        b.comment("逐套装配置；原伤害系数仍保留在 career_damage_coefficients，避免丢失旧值。",
                "攻击间隔=max(最短间隔, round(基础间隔-速度缩短系数×速度属性×速度倍率))，20tick=1秒。",
                "生命值/护甲/韧性为直接生物加成；六维属性加成为临时有效属性，不改基因或遗传。",
                "保存后已穿戴套装也会更新；原版属性上限、受伤无敌帧及俯冲动作耗时仍然适用。")
                .push(CatSuitSetting.TAG);
        for (CatOutfitType outfit : CAREERS) {
            b.push(outfit.id());
            var settings = new EnumMap<CatSuitSetting, ForgeConfigSpec.ConfigValue<Number>>(CatSuitSetting.class);
            for (CatSuitSetting setting : CatSuitSetting.EXTRA)
                settings.put(setting, b.comment(setting.comment(outfit))
                        .<Number>define(setting.id(), setting.defaultValue(outfit), value -> setting.validConfig(value, false)));
            CAREER_SETTINGS.put(outfit, settings);
            b.pop();
        }
        b.pop();
        SHOW_HELL_RECIPES = b.comment("是否在 JEI 展示原先隐藏的地狱配方；只影响展示，不禁用配方。")
                .define("show_hell_recipes", false);
        WILD_CATS_FLEE = b.comment("野生猫是否使用原版避人/受伤逃跑 AI。默认关闭；不影响词条特殊行为。")
                .define("wild_cats_flee", false);
        CATS_HISS = b.comment("猫咪是否哈气。关闭会停止哈气姿势、配对音效及哈气产出，不停止物流运输。")
                .define("cats_hiss", true);
        DEATH_OUTCOME = b.comment("已驯养猫死亡处理：0=不生成猫饼（物品栏和饰品正常掉落），1=掉落猫饼物品，2=转变为猫饼生物。",
                "默认1。后两项保留主人与随身物品，并使用死亡属性设置；遵守doMobLoot，蟑螂分裂优先。")
                .defineInRange(DEATH_OUTCOME_KEY, DEATH_ITEM, DEATH_NONE, DEATH_ENTITY);
        DEATH_ATTRIBUTE_PENALTY_ENABLED = b.comment("猫咪死亡变成猫饼时，是否随机扣除一项基础属性。默认开启；不改变属性上限。",
                "适用于所有死亡后生成猫饼的宠物猫和职业猫；不改变词条独立效果或蟑螂分裂继承。")
                .define(DEATH_PENALTY_ENABLED_KEY, true);
        DEATH_ATTRIBUTE_LOSS = b.comment("每次死亡随机一项基础属性的固定扣除点数，最低扣到0；不是百分比，也不是六项全扣。",
                "默认20；0等同于不扣除。关闭死亡扣属性开关时此数值不生效。")
                .defineInRange(DEATH_ATTRIBUTE_LOSS_KEY, DEFAULT_DEATH_ATTRIBUTE_LOSS, 0, Integer.MAX_VALUE);
        DISABLED_TRAITS = b.comment(CatTraitConfig.comments(false))
                .<java.util.List<?>>define(CatTraitConfig.KEY, java.util.List.of(),
                        value -> CatTraitConfig.validList(value, false));
        INTERNAL_BALANCE_REVISION = b.comment("内部平衡迁移标记；只升级仍完整等于旧默认的相关参数组，不覆盖自定义值；钓鱼猫不迁移。")
                .defineInRange("internal_balance_revision", 0, 0, Integer.MAX_VALUE);
        SPEC = b.build();
    }

    /** One-time migration; custom world values and all global overrides remain authoritative. */
    public static boolean migrateLegacyBalanceDefaults() {
        if (!SPEC.isLoaded() || INTERNAL_BALANCE_REVISION.get() >= 4) return false;
        if (INTERNAL_BALANCE_REVISION.get() < 1)
            migrateUntouched(CatOutfitType.ENGINEERING, Map.of(
                    CatSuitSetting.INTERVAL_BASE, 80.0, CatSuitSetting.INTERVAL_PER_SPEED, 0.10,
                    CatSuitSetting.MIN_INTERVAL, 60.0));
        if (INTERNAL_BALANCE_REVISION.get() < 2) {
            migrateUntouched(CatOutfitType.FLIGHT, Map.of(
                    CatSuitSetting.INTERVAL_BASE, 36.0, CatSuitSetting.HEALTH, 12.0,
                    CatSuitSetting.ARMOR, 3.0, CatSuitSetting.TOUGHNESS, 1.0));
            migrateUntouched(CatOutfitType.FIRE, Map.of(
                    CatSuitSetting.DAMAGE, 0.60, CatSuitSetting.INTERVAL_BASE, 14.0));
            migrateUntouched(CatOutfitType.HONEY, Map.of(
                    CatSuitSetting.DAMAGE, 0.85, CatSuitSetting.INTERVAL_BASE, 42.0));
            migrateUntouched(CatOutfitType.DYNAMITE, Map.of(CatSuitSetting.DAMAGE, 1.35));
            migrateUntouched(CatOutfitType.TRANSPORT, Map.of(CatSuitSetting.ARMOR, 6.0));
        }
        if (INTERNAL_BALANCE_REVISION.get() < 3) {
            migrateUntouched(CatOutfitType.ENGINEERING, Map.of(
                    CatSuitSetting.ATTACK_STAT, 10.0, CatSuitSetting.INTELLIGENCE_STAT, 20.0,
                    CatSuitSetting.LUCK_STAT, 40.0));
            migrateUntouched(CatOutfitType.MEDICAL, Map.of(
                    CatSuitSetting.HEALTH, 0.0, CatSuitSetting.ARMOR, 0.0,
                    CatSuitSetting.TOUGHNESS, 0.0));
        }
        migrateUntouched(CatOutfitType.MUSIC, Map.of(CatSuitSetting.HEALTH, 0.0,
                CatSuitSetting.ARMOR, 0.0, CatSuitSetting.TOUGHNESS, 0.0, CatSuitSetting.SPEED_STAT, 0.0));
        INTERNAL_BALANCE_REVISION.set(4);
        return true;
    }

    private static void migrateUntouched(CatOutfitType outfit, Map<CatSuitSetting, Double> previous) {
        // Read world-owned values directly: global overrides must neither be rewritten
        // nor used to decide whether the stored world group was customized.
        for (var entry : previous.entrySet()) {
            var setting = entry.getKey();
            double stored = setting == CatSuitSetting.DAMAGE
                    ? CAREER_DAMAGE_COEFFICIENTS.get(outfit).get()
                    : CAREER_SETTINGS.get(outfit).get(setting).get().doubleValue();
            if (Double.compare(stored, entry.getValue()) != 0) return;
        }
        for (var setting : previous.keySet()) {
            if (setting == CatSuitSetting.DAMAGE)
                CAREER_DAMAGE_COEFFICIENTS.get(outfit).set(setting.defaultValue(outfit));
            else CAREER_SETTINGS.get(outfit).get(setting).set(setting.defaultValue(outfit));
        }
    }

    public static double scale(CatStat stat, int value) {
        return Math.max(0, value) * multiplier(stat);
    }
    public static java.util.Set<String> disabledTraitIds() {
        CompoundTag mirror = remoteValues;
        if (mirror != null) return CatTraitConfig.read(mirror);
        java.util.Set<String> worldValue = SPEC.isLoaded()
                ? CatTraitConfig.ids(DISABLED_TRAITS.get()) : java.util.Set.of();
        return remoteWorld ? worldValue : GlobalConfig.disabledTraitIds(worldValue);
    }
    public static boolean isTraitDisabled(CatTrait trait) {
        return isTraitDisabled((cn.laowu.mod.genetics.CatTraitType) trait);
    }
    public static boolean isTraitDisabled(cn.laowu.mod.genetics.CatTraitType trait) {
        return disabledTraitIds().contains(trait.id().toString());
    }
    public static double multiplier(CatStat stat) {
        CompoundTag mirror = remoteValues;
        if (mirror != null) return mirror.getDouble(stat.serializedName());
        double worldValue = SPEC.isLoaded() ? MULTIPLIERS.get(stat).get() : 1.0D;
        return remoteWorld ? worldValue : GlobalConfig.multiplier(stat, worldValue);
    }
    public static String careerDamageKey(CatOutfitType outfit) {
        return "career_coefficient." + outfit.id();
    }
    public static double careerDamageCoefficient(CatOutfitType outfit) {
        if (outfit == CatOutfitType.NONE || outfit.isPreviewOnly()) return outfit.defaultDamageCoefficient();
        CompoundTag mirror = remoteValues;
        if (mirror != null) return mirror.getCompound(CAREER_DAMAGE_TAG).getDouble(outfit.id());
        double worldValue = SPEC.isLoaded() ? CAREER_DAMAGE_COEFFICIENTS.get(outfit).get() : outfit.defaultDamageCoefficient();
        return remoteWorld ? worldValue : GlobalConfig.careerDamageCoefficient(outfit, worldValue);
    }
    public static double careerSetting(CatOutfitType outfit, CatSuitSetting setting) {
        if (setting == CatSuitSetting.DAMAGE) return careerDamageCoefficient(outfit);
        if (outfit == CatOutfitType.NONE || outfit.isPreviewOnly()) return setting.defaultValue(outfit);
        CompoundTag mirror = remoteValues;
        if (mirror != null) return setting.read(mirror, outfit);
        double worldValue = SPEC.isLoaded() ? CAREER_SETTINGS.get(outfit).get(setting).get().doubleValue()
                : setting.defaultValue(outfit);
        return remoteWorld ? worldValue : GlobalConfig.careerSetting(outfit, setting, worldValue);
    }
    public static int careerStatBonus(CatOutfitType outfit, CatStat stat) {
        return (int) careerSetting(outfit, CatSuitSetting.forStat(stat));
    }
    /** Finite float output for Minecraft damage APIs, including extreme modded input. */
    public static float scaleDamage(double ordinaryDamage, double multiplier) {
        if (!(ordinaryDamage > 0) || !(multiplier > 0)) return 0.0F;
        return (float) Math.min(Float.MAX_VALUE, ordinaryDamage * multiplier);
    }
    public static boolean validMultiplier(double value) {
        return Double.isFinite(value) && value >= 0 && value <= MAX_MULTIPLIER;
    }
    public static boolean showHellRecipes() {
        return effectiveSwitch("show_hell_recipes", SPEC.isLoaded() && SHOW_HELL_RECIPES.get());
    }
    public static boolean wildCatsFlee() {
        return effectiveSwitch("wild_cats_flee", SPEC.isLoaded() && WILD_CATS_FLEE.get());
    }
    public static boolean catsHiss() {
        return effectiveSwitch("cats_hiss", !SPEC.isLoaded() || CATS_HISS.get());
    }
    public static boolean deathAttributePenaltyEnabled() {
        return effectiveSwitch(DEATH_PENALTY_ENABLED_KEY, !SPEC.isLoaded() || DEATH_ATTRIBUTE_PENALTY_ENABLED.get());
    }
    public static int deathOutcome() {
        CompoundTag mirror = remoteValues;
        if (mirror != null) return mirror.getInt(DEATH_OUTCOME_KEY);
        int worldValue = SPEC.isLoaded() ? DEATH_OUTCOME.get() : DEATH_ITEM;
        return remoteWorld ? worldValue : GlobalConfig.deathOutcome(worldValue);
    }
    public static int deathAttributeLoss() {
        CompoundTag mirror = remoteValues;
        if (mirror != null) return mirror.getInt(DEATH_ATTRIBUTE_LOSS_KEY);
        int worldValue = SPEC.isLoaded() ? DEATH_ATTRIBUTE_LOSS.get() : DEFAULT_DEATH_ATTRIBUTE_LOSS;
        return remoteWorld ? worldValue : GlobalConfig.deathAttributeLoss(worldValue);
    }
    public static boolean validDeathAttributeLoss(double value) {
        return Double.isFinite(value) && value >= 0 && value <= Integer.MAX_VALUE && value == Math.rint(value);
    }
    private static boolean effectiveSwitch(String key, boolean worldValue) {
        CompoundTag mirror = remoteValues;
        if (mirror != null) return mirror.getBoolean(key);
        return remoteWorld ? worldValue : GlobalConfig.switchValue(key, worldValue);
    }
    public static boolean canEdit(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        return player.hasPermissions(2) || (server != null && server.isSingleplayer()
                && server.isSingleplayerOwner(player.getGameProfile()));
    }
    public static CompoundTag snapshot() {
        CompoundTag mirror = remoteValues;
        if (mirror != null) return mirror.copy();
        CompoundTag tag = new CompoundTag();
        CompoundTag locks = new CompoundTag();
        for (CatStat stat : CatStat.values()) {
            tag.putDouble(stat.serializedName(), multiplier(stat));
            locks.putBoolean(stat.serializedName(), !remoteWorld && GlobalConfig.isLocked(stat.serializedName()));
        }
        tag.putBoolean("show_hell_recipes", showHellRecipes());
        tag.putBoolean("wild_cats_flee", wildCatsFlee());
        tag.putBoolean("cats_hiss", catsHiss());
        tag.putBoolean(DEATH_PENALTY_ENABLED_KEY, deathAttributePenaltyEnabled());
        tag.putInt(DEATH_ATTRIBUTE_LOSS_KEY, deathAttributeLoss());
        tag.putInt(DEATH_OUTCOME_KEY, deathOutcome());
        locks.putBoolean(DEATH_OUTCOME_KEY, !remoteWorld && GlobalConfig.isLocked(DEATH_OUTCOME_KEY));
        locks.putBoolean(DEATH_ATTRIBUTE_LOSS_KEY, !remoteWorld && GlobalConfig.isLocked(DEATH_ATTRIBUTE_LOSS_KEY));
        CompoundTag careers = new CompoundTag();
        for (CatOutfitType outfit : CAREERS) {
            careers.putDouble(outfit.id(), careerDamageCoefficient(outfit));
            locks.putBoolean(careerDamageKey(outfit), !remoteWorld && GlobalConfig.isLocked(careerDamageKey(outfit)));
        }
        tag.put(CAREER_DAMAGE_TAG, careers);
        for (CatOutfitType outfit : CAREERS) {
            for (CatSuitSetting setting : CatSuitSetting.EXTRA) {
                setting.write(tag, outfit, careerSetting(outfit, setting));
                locks.putBoolean(setting.lockKey(outfit), !remoteWorld && GlobalConfig.isLocked(setting.lockKey(outfit)));
            }
        }
        CatTraitConfig.write(tag, disabledTraitIds());
        locks.putBoolean(CatTraitConfig.KEY, !remoteWorld && GlobalConfig.isLocked(CatTraitConfig.KEY));
        for (String key : GlobalConfig.SWITCHES)
            locks.putBoolean(key, !remoteWorld && GlobalConfig.isLocked(key));
        tag.put(LOCKS_TAG, locks);
        tag.putInt("revision", tag.hashCode());
        return tag;
    }
    public static boolean valid(CompoundTag tag) {
        if (!tag.contains(DEATH_OUTCOME_KEY, 3) || tag.getInt(DEATH_OUTCOME_KEY) < DEATH_NONE
                || tag.getInt(DEATH_OUTCOME_KEY) > DEATH_ENTITY) return false;
        if (!CatTraitConfig.validTag(tag)) return false;
        if (!tag.contains("show_hell_recipes", 1) || !tag.contains("wild_cats_flee", 1)) return false;
        if (!tag.contains("cats_hiss", 1)) return false;
        if (!tag.contains(DEATH_PENALTY_ENABLED_KEY, 1)
                || !tag.contains(DEATH_ATTRIBUTE_LOSS_KEY, 3)
                || !validDeathAttributeLoss(tag.getInt(DEATH_ATTRIBUTE_LOSS_KEY))) return false;
        for (CatStat stat : CatStat.values()) {
            if (!tag.contains(stat.serializedName(), 6)) return false;
            double value = tag.getDouble(stat.serializedName());
            if (!validMultiplier(value)) return false;
        }
        if (!tag.contains(CAREER_DAMAGE_TAG, 10)) return false;
        CompoundTag careers = tag.getCompound(CAREER_DAMAGE_TAG);
        for (CatOutfitType outfit : CAREERS) {
            if (!careers.contains(outfit.id(), 6) || !validMultiplier(careers.getDouble(outfit.id()))) return false;
        }
        if (!tag.contains(CatSuitSetting.TAG, 10)) return false;
        for (CatOutfitType outfit : CAREERS) {
            CompoundTag settings = tag.getCompound(CatSuitSetting.TAG).getCompound(outfit.id());
            for (CatSuitSetting setting : CatSuitSetting.EXTRA)
                if (!settings.contains(setting.id(), 6) || !setting.valid(settings.getDouble(setting.id()))) return false;
        }
        return true;
    }
    /** The server checks its own global file, never lock flags submitted by a client. */
    static void applyUnlockedValues(CompoundTag tag) {
        if (!GlobalConfig.isLocked(CatTraitConfig.KEY))
            DISABLED_TRAITS.set(java.util.List.copyOf(CatTraitConfig.read(tag)));
        for (CatStat stat : CatStat.values()) {
            if (!GlobalConfig.isLocked(stat.serializedName()))
                MULTIPLIERS.get(stat).set(tag.getDouble(stat.serializedName()));
        }
        if (!GlobalConfig.isLocked("show_hell_recipes"))
            SHOW_HELL_RECIPES.set(tag.getBoolean("show_hell_recipes"));
        if (!GlobalConfig.isLocked("wild_cats_flee"))
            WILD_CATS_FLEE.set(tag.getBoolean("wild_cats_flee"));
        if (!GlobalConfig.isLocked("cats_hiss"))
            CATS_HISS.set(tag.getBoolean("cats_hiss"));
        if (!GlobalConfig.isLocked(DEATH_PENALTY_ENABLED_KEY))
            DEATH_ATTRIBUTE_PENALTY_ENABLED.set(tag.getBoolean(DEATH_PENALTY_ENABLED_KEY));
        if (!GlobalConfig.isLocked(DEATH_ATTRIBUTE_LOSS_KEY))
            DEATH_ATTRIBUTE_LOSS.set(tag.getInt(DEATH_ATTRIBUTE_LOSS_KEY));
        if (!GlobalConfig.isLocked(DEATH_OUTCOME_KEY))
            DEATH_OUTCOME.set(tag.getInt(DEATH_OUTCOME_KEY));
        for (CatOutfitType outfit : CAREERS) {
            if (!GlobalConfig.isLocked(careerDamageKey(outfit)))
                CAREER_DAMAGE_COEFFICIENTS.get(outfit).set(tag.getCompound(CAREER_DAMAGE_TAG).getDouble(outfit.id()));
            for (CatSuitSetting setting : CatSuitSetting.EXTRA)
                if (!GlobalConfig.isLocked(setting.lockKey(outfit)))
                    CAREER_SETTINGS.get(outfit).get(setting).set(setting.read(tag, outfit));
        }
    }
    /** Keep remote effective values separate from both local files and the world spec. */
    public static void receiveMirror(CompoundTag tag) {
        if (valid(tag)) {
            remoteWorld = true;
            remoteValues = tag.copy();
        }
    }
    public static void setRemoteWorld(boolean remote) {
        remoteWorld = remote;
        if (!remote) remoteValues = null;
    }
    public static void clearMirror() {
        remoteValues = null;
        remoteWorld = false;
    }
    public static void resetWorldState() {
        clearMirror();
        SENT.clear();
    }
    /** All permission, range and stale-edit checks run on the logical server. */
    public static void request(ServerPlayer player, boolean apply, CompoundTag tag) {
        if (apply) {
            if (!canEdit(player)) {
                player.sendSystemMessage(Component.translatable("screen.laowu.world.denied"));
            } else if (!SPEC.isLoaded() || !valid(tag) || tag.getInt("revision") != snapshot().getInt("revision")) {
                player.sendSystemMessage(Component.translatable("screen.laowu.world.stale"));
            } else {
                applyUnlockedValues(tag);
                SPEC.save();
                for (ServerPlayer other : player.getServer().getPlayerList().getPlayers()) send(other);
            }
        }
        send(player);
    }
    private static void send(ServerPlayer player) {
        CompoundTag tag = snapshot();
        tag.putBoolean("can_edit", canEdit(player));
        SENT.put(player, tag.copy());
        ModNetwork.sendWorldSettings(player, tag);
    }
    /** Covers joining players, OP changes and on-disk config reloads without per-cat packets. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        CompoundTag values = snapshot();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            CompoundTag tag = values.copy();
            tag.putBoolean("can_edit", canEdit(player));
            if (!tag.equals(SENT.get(player))) send(player);
        }
    }
    private ServerConfig() {}
}
