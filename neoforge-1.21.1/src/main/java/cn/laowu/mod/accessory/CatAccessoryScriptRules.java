package cn.laowu.mod.accessory;

/** Pure validation shared by the live script API and regression tests. */
public final class CatAccessoryScriptRules {
    public static final double MAX_DAMAGE = 1_000_000_000D;
    public static final int MAX_COOLDOWN = 20 * 60 * 60 * 24 * 7;
    public static double damage(double value) {
        if (!Double.isFinite(value) || value < 0 || value > MAX_DAMAGE)
            throw new IllegalArgumentException("Damage must be finite and within 0..1000000000");
        return value;
    }
    public static int ticks(int value) {
        if (value < 0 || value > MAX_COOLDOWN) throw new IllegalArgumentException("Invalid cooldown ticks");
        return value;
    }
    public static String key(String key) {
        if (key == null || key.length() > 128 || !key.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
            throw new IllegalArgumentException("State/cooldown keys must be namespaced IDs");
        return key;
    }
    public static boolean canActivate(long now, long due, int charge, int cost) {
        if (cost < 0 || cost > 1_000_000) throw new IllegalArgumentException("Invalid charge cost");
        return now >= due && charge >= cost;
    }
    private CatAccessoryScriptRules() {}
}
