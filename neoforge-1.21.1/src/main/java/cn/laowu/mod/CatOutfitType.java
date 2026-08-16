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
    TRANSPORT("transport");

    private final String id;

    CatOutfitType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
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
