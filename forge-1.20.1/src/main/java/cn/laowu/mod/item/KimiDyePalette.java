package cn.laowu.mod.item;

/** Original artist palette masks; pink nose/ear details and transparency are deliberately excluded. */
public final class KimiDyePalette {
    /** UV-local exclusions: eye-white shadows share EAEAEA with dyeable white fur. */
    public static int region(String texture, int x, int y, int width, int height, int rgb) {
        int size = texture.equals("textures/item/cat_helmet.png") ? 16 : 64;
        int u = x * size / width, v = y * size / height;
        boolean eyeWhite = switch (texture) {
            case "textures/models/armor/kimi_helmet.png" -> (u == 1 || u == 8) && (v == 1 || v == 2);
            case "textures/models/armor/kimi_armor.png", "textures/models/armor/kimi_armor_slim.png"
                    -> (u == 11 || u == 18) && (v == 11 || v == 12);
            case "textures/item/cat_helmet.png" -> (u == 5 || u == 10) && (v == 5 || v == 6);
            default -> false;
        };
        return eyeWhite ? -1 : region(rgb);
    }
    public static int region(int rgb) {
        return switch (rgb & 0xffffff) {
            case 0x65B213, 0x4D890D -> 2;
            case 0xEAEAEA, 0xEAD4AE, 0xAFB6B3, 0xC9CECC, 0xD7DBD9, 0xFCFCFC -> 1;
            case 0xF4B446, 0xEAA939, 0xE79D35, 0xEEA43E, 0xE99537, 0xE38C2C,
                 0xDB7920, 0xE48630, 0xE7C45A, 0xDFB264, 0xD5AA62, 0xE58F30,
                 0xDF9A3E, 0xD5B174, 0xDFC787, 0x894100, 0xBE5A00, 0xDE9834 -> 0;
            default -> -1;
        };
    }
    public static int shade(int source, int target, int region) {
        int anchor = region == 0 ? 0xEAA939 : region == 1 ? 0xEAEAEA : 0x65B213;
        double base = luma(anchor), light = luma(source);
        int result = 0;
        for (int shift = 0; shift <= 16; shift += 8) {
            int channel = (target >> shift) & 255;
            double mapped = light <= base ? channel * light / base
                    : channel + (255 - channel) * (light - base) / (255 - base);
            result |= Math.max(0, Math.min(255, (int) Math.round(mapped))) << shift;
        }
        return result;
    }
    private static double luma(int rgb) {
        return ((rgb >> 16) & 255) * .2126 + ((rgb >> 8) & 255) * .7152 + (rgb & 255) * .0722;
    }
    public static int channel(int code, int region) {
        int value = (code >> (region * 5)) & 31;
        return value >= 1 && value <= 16 ? value - 1 : -1;
    }
    public static int replace(int code, int region, int dye) {
        return (code & ~(31 << (region * 5))) | ((dye + 1) << (region * 5));
    }
    private KimiDyePalette() {}
}
