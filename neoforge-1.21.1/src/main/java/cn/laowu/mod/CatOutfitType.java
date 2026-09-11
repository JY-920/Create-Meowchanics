package cn.laowu.mod;

import java.util.Locale;

/** Persistent identity of the optional equipment rendered on a cat. */
public enum CatOutfitType {
    NONE("none"),
    TERMINATOR("terminator"),
    FISHING("fishing"),
    FLIGHT("flight"),
    FIRE("fire"),
    HONEY("honey"),
    TRANSPORT("transport"),
    DYNAMITE("dynamite");

    private final String id;

    CatOutfitType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** The original K in (2 + 0.08 * Combat Power) * K, not an extra final-damage multiplier. */
    public double defaultDamageCoefficient() {
        return switch (this) {
            case TERMINATOR -> 0.75D;
            case FISHING -> 0.55D;
            case FLIGHT -> 2.00D;
            case FIRE -> 0.60D;
            case HONEY -> 0.85D;
            case TRANSPORT -> 0.00D;
            case DYNAMITE -> 1.35D;
            case NONE -> 1.00D;
        };
    }

    /** Stable, readable TOML help without depending on client translations. */
    public String damageCoefficientConfigComment() {
        return configName() + "；套装默认系数 K = " + defaultDamageCoefficient()
                + (this == TRANSPORT ? "（辅助职业，不主动攻击）" : "");
    }

    public String configName() {
        return switch (this) {
            case TERMINATOR -> "机械套装";
            case FISHING -> "钓鱼套装";
            case FLIGHT -> "飞行套装";
            case FIRE -> "喷火套装";
            case HONEY -> "采蜜套装";
            case TRANSPORT -> "物流套装";
            case DYNAMITE -> "雷管套装";
            case NONE -> "无套装";
        };
    }

    public static CatOutfitType byId(String id) {
        if (id == null || id.isBlank()) return NONE;
        String normalized = id.toLowerCase(Locale.ROOT);
        for (CatOutfitType type : values()) {
            if (type.id.equals(normalized)) return type;
        }
        return NONE;
    }

    public static CatOutfitType byOrdinal(int ordinal) {
        CatOutfitType[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NONE;
    }
}
