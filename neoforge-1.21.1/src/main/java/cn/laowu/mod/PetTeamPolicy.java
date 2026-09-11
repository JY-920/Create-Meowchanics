package cn.laowu.mod;

import java.util.UUID;

/** Pure relationship policy, shared by combat, retaliation and support. */
public final class PetTeamPolicy {
    public static boolean friendly(int colourA, int colourB, UUID ownerA, UUID ownerB,
                                   String teamA, String teamB) {
        return colourA == colourB || ownerA != null && ownerB != null
                && !ownerA.equals(ownerB) && teamA != null && teamA.equals(teamB);
    }
    private PetTeamPolicy() {}
}
