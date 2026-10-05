package cn.laowu.mod.client;

/** Pure colour policy for the original trait bottle sprite. */
public final class CatTraitTokenColours {
    private CatTraitTokenColours() {}
    public static int recolour(int argb, int frame) {
        int red = (argb >>> 16) & 255;
        int green = (argb >>> 8) & 255;
        int blue = argb & 255;
        // The authored glass is pale yellow (red=255); the cork is dark red.
        // Only the saturated gold liquid and its detached droplets are tinted.
        if ((argb >>> 24) == 0 || red < 200 || red >= 255
                || green < 100 || green > 190 || blue > 100 || frame == 2) return argb;
        int colour = switch (frame) {
            case 1 -> 0x7C9DFB;
            case 3 -> 0x575757;
            default -> 0xB59370;
        };
        // Retain the original liquid's highlight/shadow contrast.
        float shade = (red + green + blue) / 411.0F;
        int r = Math.min(255, Math.round(((colour >>> 16) & 255) * shade));
        int g = Math.min(255, Math.round(((colour >>> 8) & 255) * shade));
        int b = Math.min(255, Math.round((colour & 255) * shade));
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}
