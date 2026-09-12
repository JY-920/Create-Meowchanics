package cn.laowu.mod.client;

/** Source atlas is the user's unmodified 64x64 pixel art. */
public final class CatHealthBarLayout {
    public static final int WIDTH = 64, HEIGHT = 12, FILL_WIDTH = 34;
    public static final int LOW_COLOUR = 0xFFF2A6A6;
    public static final int HALF_COLOUR = 0xFFF2DEA0;
    public static final int FULL_COLOUR = 0xFFA8E6B0;
    public static int width(float health, float maximum) { return percent(health, maximum) == 100 ? 64 : 60; }
    public static double fraction(float health, float maximum) {
        if (!Float.isFinite(health) || !Float.isFinite(maximum) || maximum <= 0) return 0;
        return Math.max(0, Math.min(1, (double) health / maximum));
    }
    public static int percent(float health, float maximum) {
        double fraction = fraction(health, maximum);
        return fraction <= 0 ? 0 : Math.max(1, (int) Math.round(fraction * 100));
    }
    public static int fillPixels(float health, float maximum) {
        double fraction = fraction(health, maximum);
        return fraction <= 0 ? 0 : Math.max(1, (int) Math.round(fraction * FILL_WIDTH));
    }

    /** Interpolate actual health, not the rounded percentage, without threshold colour jumps. */
    public static int fillColour(float health, float maximum) {
        double fraction = fraction(health, maximum);
        return fraction <= .5 ? interpolate(LOW_COLOUR, HALF_COLOUR, fraction * 2)
                : interpolate(HALF_COLOUR, FULL_COLOUR, (fraction - .5) * 2);
    }

    private static int interpolate(int from, int to, double amount) {
        int colour = 0xFF000000;
        for (int shift : new int[]{16, 8, 0}) {
            int a = from >> shift & 255, b = to >> shift & 255;
            colour |= (int) Math.round(a + (b - a) * amount) << shift;
        }
        return colour;
    }

    /** The supplied atlas's light pixel is F8F8EC, so compensate its tint rather than recolour the art. */
    private static int fillTint(float health, float maximum) {
        int colour = fillColour(health, maximum);
        int red = Math.min(255, Math.round((colour >> 16 & 255) * 255F / 248));
        int green = Math.min(255, Math.round((colour >> 8 & 255) * 255F / 248));
        int blue = Math.min(255, Math.round((colour & 255) * 255F / 236));
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }
    public static Glyph glyph(char value) {
        if (value == '%') return new Glyph(2, 27, 7);
        if (value >= '0' && value <= '7') return new Glyph(16 + (value - '0') * 6, 27, 5);
        if (value == '8' || value == '9') return new Glyph(2 + (value - '8') * 6, 36, 5);
        return new Glyph(10, 27, 5);
    }

    /** One drawing plan for in-game rendering and pixel-accurate offline checks. */
    public static void draw(float health, float maximum, Blitter out) {
        int width = width(health, maximum), value = percent(health, maximum);
        int numberArea = width - 43;
        out.blit(0, 0, width, HEIGHT, width == 64 ? 0 : 4, width == 64 ? 0 : 13, width, HEIGHT, 0, -1);
        // Both supplied strips include example digits; clear only their text areas.
        out.blit(3, 2, numberArea, 1, 26, 2, 1, 1, .04F, -1);
        out.blit(3, 3, numberArea, 1, 2, 3, 1, 1, .04F, -1);
        out.blit(3, 4, numberArea, 5, 26, 5, 1, 1, .04F, -1);
        int fill = fillPixels(health, maximum);
        if (fill > 0) out.blit(width - 38, 4, fill, 3, 17, 28, 1, 1, .06F, fillTint(health, maximum));
        String text = (value < 10 ? "0" : "") + value + "%";
        int textWidth = (text.length() - 1) * 4 + 7;
        int x = 3 + (numberArea - textWidth) / 2;
        for (int i = 0; i < text.length(); i++) {
            Glyph glyph = glyph(text.charAt(i));
            out.blit(x, 2, glyph.width(), 7, glyph.u(), glyph.v(), glyph.width(), 7, .08F, -1);
            x += 4;
        }
    }
    @FunctionalInterface public interface Blitter {
        void blit(int x, int y, int width, int height, int u, int v, int sourceWidth, int sourceHeight, float z, int tint);
    }
    public record Glyph(int u, int v, int width) {}
    private CatHealthBarLayout() {}
}
