package cn.laowu.mod.accessory;

import com.google.gson.JsonObject;

/** Optional v1 charge contract; units are effect activations, not item durability. */
public record CatAccessoryCharge(int capacity, int initial) {
    public static final CatAccessoryCharge NONE = new CatAccessoryCharge(0, 0);
    public CatAccessoryCharge {
        if (capacity < 0 || capacity > 1_000_000 || initial < 0 || initial > capacity)
            throw new IllegalArgumentException("charge requires 0 <= initial <= capacity <= 1000000");
    }
    public static CatAccessoryCharge parse(JsonObject json) {
        if (!json.has("charge")) return NONE;
        JsonObject c = json.getAsJsonObject("charge");
        return new CatAccessoryCharge(integer(c, "capacity", 0), integer(c, "initial", 0));
    }
    private static int integer(JsonObject json, String key, int fallback) {
        if (!json.has(key)) return fallback;
        if (!json.get(key).isJsonPrimitive() || !json.getAsJsonPrimitive(key).isNumber())
            throw new IllegalArgumentException("charge." + key + " must be an integer");
        double value = json.get(key).getAsDouble();
        if (!Double.isFinite(value) || value != Math.rint(value) || value < 0 || value > 1_000_000)
            throw new IllegalArgumentException("Invalid charge." + key);
        return (int)value;
    }
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("capacity", capacity); json.addProperty("initial", initial);
        return json;
    }
    public int clamp(int value) { return Math.max(0, Math.min(capacity, value)); }
}
