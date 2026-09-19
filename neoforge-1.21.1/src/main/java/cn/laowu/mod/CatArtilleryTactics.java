package cn.laowu.mod;

/** Hysteresis keeps smart engineers from unpacking/repacking every tick at a range boundary. */
public final class CatArtilleryTactics {
    public static final double RANGE = 32, IDEAL = 16, INNER = 8, OUTER = 24;
    public static boolean smart(int intelligence) { return intelligence >= 80; }
    public static boolean preferred(double distance) { return distance >= 14 && distance <= 18; }
    public static boolean relocate(int intelligence, double distance, int deployedTicks) {
        return smart(intelligence) && deployedTicks >= 32 && (distance < INNER || distance > OUTER);
    }
    private CatArtilleryTactics() {}
}
