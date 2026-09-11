package cn.laowu.mod.item;

/** Pure matching algebra shared by the item filter, editor and breeding ranking. */
public record CatFilterLogic(int currentMask, int limitMask, int flags) {
    public static final int STAT_MASK = 63;
    public static final int ATTRIBUTE_ANY = 0;
    public static final int ATTRIBUTE_INVERT = 1;
    public static final int TRAIT_ANY = 2;
    public static final int TRAIT_INVERT = 3;
    public static final int GROUP_ANY = 4;

    public CatFilterLogic {
        currentMask &= STAT_MASK;
        limitMask &= STAT_MASK;
        flags &= 31;
    }

    public boolean option(int option) { return (flags & (1 << option)) != 0; }
    public boolean enabled(int page, int stat) {
        return ((page == 1 ? limitMask : currentMask) & (1 << stat)) != 0;
    }

    public boolean matches(int attributeCount, int attributesPassed, int traitCount, int traitsPassed) {
        boolean attributes = group(attributeCount, attributesPassed,
                option(ATTRIBUTE_ANY), option(ATTRIBUTE_INVERT));
        boolean traits = group(traitCount, traitsPassed, option(TRAIT_ANY), option(TRAIT_INVERT));
        // Disabled/empty groups are absent, not true operands in an OR expression.
        if (attributeCount == 0) return traitCount == 0 || traits;
        if (traitCount == 0) return attributes;
        return option(GROUP_ANY) ? attributes || traits : attributes && traits;
    }

    private static boolean group(int count, int passed, boolean any, boolean inverted) {
        if (count == 0) return true;
        boolean matched = any ? passed > 0 : passed == count;
        return inverted ? !matched : matched;
    }

    /** Infer old filters' active conditions without changing their AND semantics. */
    public static int inferMask(int[] minimum, int[] maximum, int fullMaximum) {
        int mask = 0;
        for (int i = 0; i < 6; i++)
            if (minimum[i] != 0 || maximum[i] != fullMaximum) mask |= 1 << i;
        return mask;
    }
}
