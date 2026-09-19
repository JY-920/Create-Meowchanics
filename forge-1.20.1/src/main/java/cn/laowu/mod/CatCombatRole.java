package cn.laowu.mod;

/** Combat behaviour categories; outfit/save identities remain unchanged. */
public enum CatCombatRole {
    MELEE, RANGED, SUPPORT;
    public String translationKey() { return "career.laowu.role." + name().toLowerCase(java.util.Locale.ROOT); }
}
