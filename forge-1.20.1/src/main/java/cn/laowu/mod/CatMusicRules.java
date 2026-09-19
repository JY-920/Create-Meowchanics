package cn.laowu.mod;

/** Music adds attack frequency, not raw genes or production/healing speed. */
public final class CatMusicRules {
    public static final int BUFF_TICKS = 40;
    public static final int DPS_WINDOW = 100;
    public static double haste(double speed) { return .05 + .002 * finite(speed); }
    public static double radius(double intelligence) { return Math.min(24, 5 + .03 * finite(intelligence)); }
    public static int interval(int original, double bonus) {
        // One server tick is the resolution of existing career weapon controllers.
        // The percentage itself has no artificial ceiling.
        return Math.max(1, (int)Math.ceil(Math.max(1, original) / (1 + finite(bonus))));
    }
    private static double finite(double value) { return Double.isFinite(value) ? Math.max(0, value) : 0; }
    private CatMusicRules() {}
}
