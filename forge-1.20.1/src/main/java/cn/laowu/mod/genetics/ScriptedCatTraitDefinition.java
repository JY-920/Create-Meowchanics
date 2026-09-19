package cn.laowu.mod.genetics;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Strict version-1 definition. This class deliberately has no KubeJS dependency. */
public record ScriptedCatTraitDefinition(ResourceLocation id, String title, List<String> descriptions,
        CatTraitRarity rarity, int maxLevel, boolean enabled, boolean natural, boolean mutation,
        boolean inheritable, int weight, Set<CatTraitSlot> slots, Set<ResourceLocation> conflicts,
        Map<CatStat, List<Integer>> bonuses) {
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_JSON_LENGTH = 4096;
    private static final Set<String> KEYS = Set.of("schema_version", "title", "description", "rarity",
            "max_level", "enabled", "natural", "mutation", "inheritable", "weight", "slots", "conflicts", "stat_bonuses");
    public ScriptedCatTraitDefinition {
        descriptions = List.copyOf(descriptions);
        slots = Set.copyOf(slots); conflicts = Set.copyOf(conflicts); bonuses = Map.copyOf(bonuses);
    }
    public static ScriptedCatTraitDefinition parse(String name, JsonObject json) {
        ResourceLocation id = ResourceLocation.tryParse(name);
        if (id == null || name.length() > 128) throw new IllegalArgumentException("Invalid trait ID");
        if (CatTrait.byId(id).isPresent()) throw new IllegalArgumentException("Built-in trait IDs cannot be replaced");
        if (json.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_JSON_LENGTH)
            throw new IllegalArgumentException("Definition exceeds 4096 UTF-8 bytes");
        for (String key : json.keySet()) if (!KEYS.contains(key)) throw new IllegalArgumentException("Unknown trait key: " + key);
        if (integer(json.get("schema_version"), 1, 1) != SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported schema");
        int max = json.has("max_level") ? integer(json.get("max_level"), 1, 7) : 1;
        String title = string(json.get("title"), 96);
        if (title.isBlank()) throw new IllegalArgumentException("Empty trait title");
        CatTraitRarity rarity = CatTraitRarity.valueOf(string(json.get("rarity"), 32).toUpperCase(Locale.ROOT));
        List<String> descriptions = new ArrayList<>();
        JsonElement description = json.get("description");
        if (description == null) descriptions.add("");
        else if (description.isJsonArray()) {
            if (description.getAsJsonArray().size() != max) throw new IllegalArgumentException("Description array must match max_level");
            description.getAsJsonArray().forEach(value -> descriptions.add(string(value, 512)));
        } else descriptions.add(string(description, 512));
        Set<CatTraitSlot> slots = new LinkedHashSet<>();
        for (String slot : strings(json, "slots", CatTraitSlot.values().length))
            slots.add(CatTraitSlot.valueOf(slot.toUpperCase(Locale.ROOT)));
        Set<ResourceLocation> conflicts = new LinkedHashSet<>();
        for (String other : strings(json, "conflicts", 32)) {
            ResourceLocation parsed = ResourceLocation.tryParse(other);
            if (parsed == null || parsed.equals(id)) throw new IllegalArgumentException("Invalid/self conflict: " + other);
            conflicts.add(parsed);
        }
        Map<CatStat, List<Integer>> bonuses = new EnumMap<>(CatStat.class);
        if (json.has("stat_bonuses")) {
            JsonObject stats = json.getAsJsonObject("stat_bonuses");
            for (var entry : stats.entrySet()) {
                CatStat stat = Arrays.stream(CatStat.values()).filter(s -> s.serializedName().equals(entry.getKey()))
                        .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown stat: " + entry.getKey()));
                List<Integer> levels = new ArrayList<>();
                if (entry.getValue().isJsonArray()) {
                    var array = entry.getValue().getAsJsonArray();
                    if (array.size() != max) throw new IllegalArgumentException("Stat array must match max_level");
                    array.forEach(value -> levels.add(integer(value, -999999, 999999)));
                } else levels.add(integer(entry.getValue(), -999999, 999999));
                bonuses.put(stat, List.copyOf(levels));
            }
        }
        var result = new ScriptedCatTraitDefinition(id, title, descriptions, rarity, max,
                bool(json, "enabled", true), bool(json, "natural", false), bool(json, "mutation", false),
                bool(json, "inheritable", true), json.has("weight") ? integer(json.get("weight"), 1, 10000) : 1,
                slots, conflicts, bonuses);
        if (result.toJson().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_JSON_LENGTH)
            throw new IllegalArgumentException("Canonical definition exceeds 4096 UTF-8 bytes");
        return result;
    }
    private static String string(JsonElement value, int max) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                || value.getAsString().length() > max) throw new IllegalArgumentException("Expected bounded string");
        return value.getAsString();
    }
    private static int integer(JsonElement value, int min, int max) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Expected integer");
        double number = value.getAsDouble();
        if (!Double.isFinite(number) || number != Math.rint(number) || number < min || number > max)
            throw new IllegalArgumentException("Integer outside " + min + ".." + max);
        return (int) number;
    }
    private static boolean bool(JsonObject json, String key, boolean fallback) {
        if (!json.has(key)) return fallback;
        var value = json.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Expected boolean: " + key);
        return value.getAsBoolean();
    }
    private static List<String> strings(JsonObject json, String key, int max) {
        if (!json.has(key)) return List.of();
        var array = json.getAsJsonArray(key);
        if (array.size() > max) throw new IllegalArgumentException("Too many " + key);
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(string(value, 128)));
        return values;
    }
    public int bonus(CatStat stat, int level) {
        var values = bonuses.get(stat);
        return values == null ? 0 : values.get(Math.min(values.size() - 1, Math.max(0, level - 1)));
    }
    public String description(int level) {
        return descriptions.get(Math.min(descriptions.size() - 1, Math.max(0, level - 1)));
    }
    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("schema_version", SCHEMA_VERSION); json.addProperty("title", title);
        var text = new JsonArray();
        if (descriptions.size() == 1) json.addProperty("description", descriptions.get(0));
        else { descriptions.forEach(text::add); json.add("description", text); }
        json.addProperty("rarity", rarity.serializedName()); json.addProperty("max_level", maxLevel);
        json.addProperty("enabled", enabled); json.addProperty("natural", natural); json.addProperty("mutation", mutation);
        json.addProperty("inheritable", inheritable); json.addProperty("weight", weight);
        var slotArray = new JsonArray(); slots.stream().map(Enum::name).sorted().forEach(slotArray::add); json.add("slots", slotArray);
        var conflictArray = new JsonArray(); conflicts.stream().map(Object::toString).sorted().forEach(conflictArray::add); json.add("conflicts", conflictArray);
        var stats = new JsonObject();
        bonuses.forEach((stat, values) -> {
            if (values.size() == 1) stats.addProperty(stat.serializedName(), values.get(0));
            else { var array = new JsonArray(); values.forEach(array::add); stats.add(stat.serializedName(), array); }
        });
        json.add("stat_bonuses", stats);
        return json;
    }
}
