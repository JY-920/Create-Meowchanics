package cn.laowu.mod.genetics;

/** Stable six-dimensional cat attributes in panel display order. */
public enum CatStat {
    ATTACK("attack"),
    HEALTH("health"),
    SPEED("speed"),
    STAMINA("stamina"),
    INTELLIGENCE("intelligence"),
    LUCK("luck");

    private final String serializedName;

    CatStat(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
