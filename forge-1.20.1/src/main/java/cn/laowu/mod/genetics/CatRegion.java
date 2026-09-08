package cn.laowu.mod.genetics;

import java.util.HashMap;
import java.util.Map;

/**
 * Stable semantic regions in the 64x32 vanilla cat texture layout.
 *
 * <p>The RGB values belong to {@code textures/entity/cat/region_map.png}.
 * Keep the serialized names stable so worlds remain readable when the visual
 * implementation changes.</p>
 */
public enum CatRegion {
    HEAD_PRIMARY("head_primary", 0x00FFFA),
    HEAD_SECONDARY("head_secondary", 0x62FF00),
    LEFT_EYE("left_eye", 0xFF0000),
    RIGHT_EYE("right_eye", 0x1E009A),
    EARS("ears", 0x335454),
    MUZZLE("muzzle", 0xFFC100),
    BODY_FRONT("body_front", 0xFFF975),
    BODY_REAR("body_rear", 0x007E9A),
    FRONT_LEGS("front_legs", 0xEC00FF),
    HIND_LEGS("hind_legs", 0x6F6F6F),
    TAIL("tail", 0x007308);

    private static final Map<Integer, CatRegion> BY_RGB = new HashMap<>();

    static {
        for (CatRegion region : values()) BY_RGB.put(region.mapRgb, region);
    }

    private final String serializedName;
    private final int mapRgb;

    CatRegion(String serializedName, int mapRgb) {
        this.serializedName = serializedName;
        this.mapRgb = mapRgb;
    }

    public String serializedName() {
        return serializedName;
    }

    public static CatRegion fromMapRgb(int rgb) {
        return BY_RGB.get(rgb & 0xFFFFFF);
    }
}
