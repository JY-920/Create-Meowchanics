import cn.laowu.mod.genetics.*;
import java.util.Arrays;

/** Tests the real CatTrait source with minimal text/resource API stubs, not a game runtime. */
public final class CatAppearanceBonusRegression {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        int checks = 0, appearances = 0;
        for (CatTrait trait : CatTrait.values()) {
            boolean appearance = trait.occupiedSlots().contains(CatTraitSlot.APPEARANCE);
            if (appearance) appearances++;
            for (int level = 1; level <= 7; level++) {
                int total = 0;
                for (CatStat stat : CatStat.values()) {
                    int actual = trait.appearanceAttributeBonus(stat, level);
                    int expected = switch (trait) {
                        case LOLI -> stat == CatStat.HEALTH ? 10 : stat == CatStat.LUCK ? 5 : 0;
                        case HIM -> stat == CatStat.ATTACK ? 15 : stat == CatStat.INTELLIGENCE ? 5 : 0;
                        case ISAAC -> stat == CatStat.LUCK ? 10 : 0;
                        case ROUND_HEAD -> stat == CatStat.STAMINA ? 10 : 0;
                        case OIIAI -> stat == CatStat.SPEED ? 10 : 0;
                        case STREET_DANCE -> stat == CatStat.SPEED || stat == CatStat.STAMINA ? 10 : 0;
                        case PIPA_PERFORMANCE -> stat == CatStat.INTELLIGENCE || stat == CatStat.LUCK ? 10 : 0;
                        case PUSS_IN_BOOTS -> stat == CatStat.ATTACK || stat == CatStat.SPEED ? 10 : 0;
                        case BIG_CHONKY_CAT -> stat == CatStat.HEALTH ? 10 + 2 * (level - 1)
                                : stat == CatStat.STAMINA ? 5 : 0;
                        case ROLLING_LOG -> stat == CatStat.SPEED ? 15 : 0;
                        case RAINBOW_CAT -> stat == CatStat.SPEED ? 10 : stat == CatStat.LUCK ? 20 : 0;
                        case NEKOMATA -> stat == CatStat.ATTACK ? 20 : stat == CatStat.INTELLIGENCE ? 10 : 0;
                        default -> 0;
                    };
                    check(actual == expected, trait + " " + stat + " level " + level);
                    total += actual;
                    checks++;
                }
                check(appearance ? total > 0 : total == 0, "Appearance-only scope " + trait);
                if (trait == CatTrait.BIG_CHONKY_CAT) {
                    Object[] expected = {115 + (level - 1) * 10, 10 + (level - 1) * 2, 5};
                    check(Arrays.equals(trait.summary(level).arguments(), expected), "Pig summary");
                    check(Arrays.equals(trait.description(level).arguments(), expected), "Pig description");
                    int next = Math.min(7, level + 1);
                    check(Arrays.equals(trait.nextLevelDescription(level).arguments(),
                            new Object[]{115 + (next - 1) * 10, 10 + (next - 1) * 2, 5}), "Pig next level");
                }
            }
        }
        check(appearances == 12, "All twelve appearances covered");
        check(CatTrait.BIG_CHONKY_CAT.appearanceAttributeBonus(CatStat.HEALTH, -100) == 10, "Low clamp");
        check(CatTrait.BIG_CHONKY_CAT.appearanceAttributeBonus(CatStat.HEALTH, 100) == 22, "High clamp");
        check(!CatTrait.PIPA_PERFORMANCE.upgradable() && CatTrait.PIPA_PERFORMANCE.maxLevel() == 1,
                "Pipa is not upgradeable");
        System.out.println("PASS: " + checks + " stat/level assertions; twelve appearances, pig tooltips and level clamps");
    }
}
