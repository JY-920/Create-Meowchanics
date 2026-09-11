import cn.laowu.mod.item.CatFilterLogic;
import java.util.Arrays;

public final class CatFilterLogicRegression {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static boolean reference(int n, int k, boolean any, boolean invert) {
        boolean result = any ? false : true;
        for (int i = 0; i < n; i++) result = any ? result || i < k : result && i < k;
        return invert ? !result : result;
    }
    public static void main(String[] args) {
        int checks = 0;
        for (int flags = 0; flags < 32; flags++) {
            CatFilterLogic logic = new CatFilterLogic(63, 63, flags);
            for (int n = 0; n <= 12; n++) for (int k = 0; k <= n; k++) {
                for (int t = 0; t <= 4; t++) for (int p = 0; p <= t; p++) {
                    boolean expected;
                    boolean a = reference(n, k, (flags & 1) != 0, (flags & 2) != 0);
                    boolean b = reference(t, p, (flags & 4) != 0, (flags & 8) != 0);
                    if (n == 0 && t == 0) expected = true;
                    else if (n == 0) expected = b;
                    else if (t == 0) expected = a;
                    else expected = (flags & 16) != 0 ? a || b : a && b;
                    check(logic.matches(n, k, t, p) == expected, "Boolean truth table");
                    checks++;
                }
            }
        }
        // Discard non-6x90 cats: inclusive 90 survives, a single 89 is selected.
        CatFilterLogic discard = new CatFilterLogic(0, 63, 2);
        check(!discard.matches(6, 6, 0, 0), "All six 90+ must be kept");
        check(discard.matches(6, 5, 0, 0), "One low stat must pass to discard");
        check(discard.matches(6, 0, 0, 0), "All low stats must pass to discard");
        CatFilterLogic anyTrait = new CatFilterLogic(0, 0, 4);
        check(anyTrait.matches(0, 0, 2, 1), "One selected good trait is sufficient");
        check(!anyTrait.matches(0, 0, 2, 0), "No selected good trait must fail");
        CatFilterLogic excludeTraits = new CatFilterLogic(0, 0, 12);
        check(excludeTraits.matches(0, 0, 2, 0), "No bad traits must pass");
        check(!excludeTraits.matches(0, 0, 2, 1), "Any bad trait must fail");
        CatFilterLogic eitherGroup = new CatFilterLogic(0, 63, 20);
        check(eitherGroup.matches(6, 6, 2, 0), "Good attributes OR good traits");
        check(eitherGroup.matches(6, 5, 2, 1), "Good trait can satisfy OR");
        check(!eitherGroup.matches(6, 5, 2, 0), "Neither group must fail");
        check(!eitherGroup.matches(6, 5, 0, 0), "Empty trait group must not satisfy OR");
        check(new CatFilterLogic(0, 0, 31).matches(0, 0, 0, 0), "Empty inverted groups ignored");

        int[] min = new int[6], max = new int[6];
        Arrays.fill(max, 999);
        check(CatFilterLogic.inferMask(min, max, 999) == 0, "Legacy defaults stay unrestricted");
        min[2] = 90;
        check(CatFilterLogic.inferMask(min, max, 999) == 4, "Legacy non-default range enabled");
        Arrays.fill(max, 100);
        min[2] = 0;
        max[5] = 89;
        check(CatFilterLogic.inferMask(min, max, 100) == 32, "Legacy upper bound enabled");
        CatFilterLogic masked = new CatFilterLogic(4, 32, 255);
        check(masked.enabled(0, 2) && !masked.enabled(0, 5) && masked.enabled(1, 5), "Page-specific masks");
        check(masked.flags() == 31, "Untrusted flags sanitized");
        System.out.println("PASS: " + checks + " truth-table cases + disposal, traits, empty groups and migration masks");
    }
}
