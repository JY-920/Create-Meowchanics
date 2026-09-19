package cn.laowu.mod;

import cn.laowu.mod.genetics.CatStat;
import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;
import java.util.List;

/** Stable IDs, validation and original defaults shared by TOML, GUI and combat. */
public enum CatSuitSetting {
    DAMAGE("damage_coefficient", "伤害系数 K", 0, false, null),
    INTERVAL_BASE("attack_interval_base", "基础攻击间隔（tick）", 1, true, null),
    INTERVAL_PER_SPEED("attack_interval_per_speed", "速度缩短系数", 0, false, null),
    MIN_INTERVAL("minimum_attack_interval", "最短攻击间隔（tick）", 1, true, null),
    HEALTH("health_bonus", "生命值加成", 0, false, null),
    ARMOR("armor_bonus", "护甲值加成", 0, false, null),
    TOUGHNESS("toughness_bonus", "韧性加成", 0, false, null),
    ATTACK_STAT("attack_stat_bonus", "战斗力属性加成", 0, true, CatStat.ATTACK),
    HEALTH_STAT("health_stat_bonus", "生命属性加成", 0, true, CatStat.HEALTH),
    SPEED_STAT("speed_stat_bonus", "速度属性加成", 0, true, CatStat.SPEED),
    STAMINA_STAT("stamina_stat_bonus", "耐力属性加成", 0, true, CatStat.STAMINA),
    INTELLIGENCE_STAT("intelligence_stat_bonus", "智力属性加成", 0, true, CatStat.INTELLIGENCE),
    LUCK_STAT("luck_stat_bonus", "幸运属性加成", 0, true, CatStat.LUCK);

    public static final String TAG = "career_settings";
    public static final List<CatSuitSetting> COMBAT = Arrays.stream(values()).filter(s -> s.stat == null).toList();
    public static final List<CatSuitSetting> BONUSES = Arrays.stream(values()).filter(s -> s.stat != null).toList();
    public static final List<CatSuitSetting> EXTRA = Arrays.stream(values()).filter(s -> s != DAMAGE).toList();
    private final String id, chineseName;
    private final double minimum;
    private final boolean integer;
    private final CatStat stat;

    CatSuitSetting(String id, String chineseName, double minimum, boolean integer, CatStat stat) {
        this.id = id;
        this.chineseName = chineseName;
        this.minimum = minimum;
        this.integer = integer;
        this.stat = stat;
    }

    public String id() { return id; }
    public boolean integer() { return integer; }
    public CatStat stat() { return stat; }
    public boolean isAttackSetting() { return ordinal() <= MIN_INTERVAL.ordinal(); }
    public boolean appliesTo(CatOutfitType outfit) {
        return outfit != CatOutfitType.NONE && !outfit.isPreviewOnly()
                && !(outfit.isSupport() && isAttackSetting());
    }
    public boolean valid(double value) {
        return Double.isFinite(value) && value >= minimum && value <= ServerConfig.MAX_MULTIPLIER
                && (!integer || value == Math.rint(value));
    }
    public boolean validConfig(Object value, boolean global) {
        return value instanceof Number number && ((global && number.doubleValue() == -1)
                || valid(number.doubleValue()));
    }
    public String lockKey(CatOutfitType outfit) {
        return this == DAMAGE ? ServerConfig.careerDamageKey(outfit) : TAG + "." + outfit.id() + "." + id;
    }
    public String comment(CatOutfitType outfit) {
        return outfit.configName() + "；" + chineseName + "；默认 " + defaultValue(outfit)
                + "；范围 " + (int) minimum + "~999999" + (integer ? "（整数）" : "")
                + (!appliesTo(outfit) ? "；辅助职业不使用攻击参数" : "");
    }
    public double defaultValue(CatOutfitType outfit) {
        return switch (this) {
            case DAMAGE -> outfit.defaultDamageCoefficient();
            case INTERVAL_BASE -> switch (outfit) {
                case TERMINATOR, NONE -> 24;
                case FISHING -> 36;
                case FLIGHT -> 40;
                case FIRE -> 15;
                case HONEY -> 41;
                case TRANSPORT -> 34;
                case DYNAMITE -> 56;
                case ENGINEERING -> 50;
                case MEDICAL, MUSIC, AGENT, COCKROACH -> 24;
                case DIVING -> 28;
            };
            case INTERVAL_PER_SPEED -> switch (outfit) {
                case TERMINATOR, DYNAMITE, NONE, AGENT, DIVING -> 0.12D;
                case FIRE, TRANSPORT -> 0.08D;
                case ENGINEERING -> 0.25D;
                default -> 0.10D;
            };
            case MIN_INTERVAL -> switch (outfit) {
                case TERMINATOR -> 10;
                case FISHING, FLIGHT, TRANSPORT -> 20;
                case FIRE -> 5;
                case HONEY -> 24;
                case DYNAMITE -> 38;
                case ENGINEERING -> 20;
                case NONE, MEDICAL, MUSIC -> 8;
                case AGENT -> 10;
                case DIVING -> 14;
                case COCKROACH -> 12;
            };
            case HEALTH -> switch (outfit) {
                case TERMINATOR, FISHING -> 20;
                case FLIGHT -> 30;
                case FIRE -> 40;
                case HONEY, DYNAMITE -> 24;
                case TRANSPORT -> 30;
                case ENGINEERING -> 30;
                case MEDICAL, MUSIC, AGENT -> 6;
                case DIVING -> 24;
                case COCKROACH -> 12;
                case NONE -> 0;
            };
            case ARMOR -> switch (outfit) {
                case TERMINATOR -> 6;
                case TRANSPORT -> 10;
                case FISHING, HONEY, DYNAMITE -> 5;
                case FLIGHT -> 7;
                case FIRE -> 10;
                case ENGINEERING -> 12;
                case MEDICAL, MUSIC, AGENT -> 2;
                case DIVING -> 6;
                case COCKROACH -> 4;
                case NONE -> 0;
            };
            case TOUGHNESS -> switch (outfit) {
                case TERMINATOR, TRANSPORT -> 3;
                case FISHING, HONEY, DYNAMITE -> 2;
                case FLIGHT -> 3;
                case FIRE -> 4;
                case ENGINEERING -> 5;
                case DIVING -> 2;
                case COCKROACH -> 1;
                case NONE, MEDICAL, MUSIC, AGENT -> 0;
            };
            case ATTACK_STAT -> outfit == CatOutfitType.TERMINATOR || outfit == CatOutfitType.FLIGHT
                    || outfit == CatOutfitType.DYNAMITE || outfit == CatOutfitType.AGENT ? 10 : 0;
            case SPEED_STAT -> outfit == CatOutfitType.HONEY || outfit == CatOutfitType.TRANSPORT
                    || outfit == CatOutfitType.MEDICAL || outfit == CatOutfitType.MUSIC ? 10 : 0;
            case STAMINA_STAT -> outfit == CatOutfitType.FIRE || outfit == CatOutfitType.DIVING ? 10 : 0;
            case LUCK_STAT -> outfit == CatOutfitType.ENGINEERING || outfit == CatOutfitType.FISHING ? 10 : 0;
            case INTELLIGENCE_STAT -> 0;
            case HEALTH_STAT -> 0;
        };
    }
    public double read(CompoundTag draft, CatOutfitType outfit) {
        CompoundTag values = this == DAMAGE ? draft.getCompound(ServerConfig.CAREER_DAMAGE_TAG)
                : draft.getCompound(TAG).getCompound(outfit.id());
        String key = this == DAMAGE ? outfit.id() : id;
        return values.contains(key, 6) ? values.getDouble(key) : defaultValue(outfit);
    }
    public void write(CompoundTag draft, CatOutfitType outfit, double value) {
        if (this == DAMAGE) {
            CompoundTag values = draft.getCompound(ServerConfig.CAREER_DAMAGE_TAG);
            values.putDouble(outfit.id(), value);
            draft.put(ServerConfig.CAREER_DAMAGE_TAG, values);
        } else {
            CompoundTag all = draft.getCompound(TAG);
            CompoundTag values = all.getCompound(outfit.id());
            values.putDouble(id, value);
            all.put(outfit.id(), values);
            draft.put(TAG, all);
        }
    }
    public static CatSuitSetting forStat(CatStat stat) {
        return switch (stat) {
            case ATTACK -> ATTACK_STAT;
            case HEALTH -> HEALTH_STAT;
            case SPEED -> SPEED_STAT;
            case STAMINA -> STAMINA_STAT;
            case INTELLIGENCE -> INTELLIGENCE_STAT;
            case LUCK -> LUCK_STAT;
        };
    }
}
