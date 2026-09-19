package cn.laowu.mod.accessory;

import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Version 1 data contract. Namespaced IDs, never enum ordinals, are the public API. */
public record CatAccessoryDefinition(String id, String item, boolean enabled,
        String requiredOutfit, String exclusiveGroup, String description, Map<String, Double> effects,
        CatAccessoryCharge charge, String script) {
    public static final int SCHEMA_VERSION = 1;
    public static final Set<String> STATS = Set.of("health", "attack", "speed", "stamina", "intelligence", "luck");
    public static final Set<String> OUTFITS = Set.of("any", "none", "terminator", "fishing", "flight",
            "fire", "honey", "transport", "dynamite", "engineering", "medical", "music", "agent", "diving", "cockroach");
    public static final Set<String> MECHANISMS = Set.of("aggro_bias", "knockback_resistance",
            "fire_immune", "projectile_knockback", "extra_strike_chance", "loot_magnet_radius",
            "pilot_dodge_per_speed", "self_destruct_multiplier", "moving_damage_reduction",
            "fishing_pull", "super_flame_multiplier", "honey_patch", "enhanced_potions",
            "engineering_special_ammo", "medical_guard", "music_movement_bonus", "music_speed_bonus",
            "healing_smoke", "diving_cleanse", "cockroach_split",
            "damage_combo", "opening_damage", "critical_haste", "melee_reflect", "emergency_shield", "heavy_hit_cap", "rest_heal", "healing_received", "owner_attack_bonus", "sample_pickup");
    private static final Set<String> SWITCHES = Set.of("knockback_resistance", "fire_immune",
            "projectile_knockback", "fishing_pull", "honey_patch", "enhanced_potions",
            "engineering_special_ammo", "healing_smoke", "diving_cleanse", "cockroach_split", "sample_pickup");

    /** Source-compatible constructor for older integrations. */
    public CatAccessoryDefinition(String id,String item,boolean enabled,String requiredOutfit,String exclusiveGroup,
            String description,Map<String,Double> effects,CatAccessoryCharge charge) {
        this(id,item,enabled,requiredOutfit,exclusiveGroup,description,effects,charge,"");
    }
    public CatAccessoryDefinition {
        effects = Collections.unmodifiableMap(new LinkedHashMap<>(effects));
    }

    public static CatAccessoryDefinition parse(String id, JsonObject json) {
        requireId(id);
        if (!json.has("schema_version") || json.get("schema_version").getAsDouble() != SCHEMA_VERSION)
            throw new IllegalArgumentException("Unsupported/missing schema_version (expected 1)");
        String item = json.get("item").getAsString();
        requireId(item);
        String outfit = string(json, "required_outfit", "any");
        if (!OUTFITS.contains(outfit)) throw new IllegalArgumentException("Unknown outfit " + outfit);
        String group = string(json, "exclusive_group", "");
        if (!group.isEmpty()) requireId(group);
        String description = string(json, "description", "");
        if (description.length() > 512) throw new IllegalArgumentException("Description exceeds 512 characters");
        String script=string(json,"script","");
        if(!script.isEmpty())requireId(script);
        Map<String, Double> effects = new LinkedHashMap<>();
        if (!json.has("effects") || !json.get("effects").isJsonObject())
            throw new IllegalArgumentException("effects must be an object (can be empty for scripted effects)");
        for (var entry : json.getAsJsonObject("effects").entrySet()) {
            String key = entry.getKey();
            if (!STATS.contains(key) && !MECHANISMS.contains(key))
                throw new IllegalArgumentException("Unknown effect " + key);
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber())
                throw new IllegalArgumentException(key + " must be numeric");
            double value = entry.getValue().getAsDouble();
            double min = STATS.contains(key) ? -300 : key.equals("aggro_bias") ? -16 : 0;
            double max = STATS.contains(key) ? 300 : SWITCHES.contains(key) ? 1 : switch (key) {
                case "aggro_bias", "loot_magnet_radius" -> 16;
                case "knockback_resistance", "fire_immune", "projectile_knockback" -> 1;
                case "pilot_dodge_per_speed" -> 10;
                case "music_speed_bonus" -> 300;
                case "moving_damage_reduction", "medical_guard" -> 80;
                default -> 100;
            };
            if (!Double.isFinite(value) || value < min || value > max)
                throw new IllegalArgumentException(key + " outside [" + min + ", " + max + "]");
            if ((STATS.contains(key) || SWITCHES.contains(key) || key.equals("music_speed_bonus")) && value != Math.rint(value))
                throw new IllegalArgumentException(key + " must be an integer");
            effects.put(key, value);
        }
        return new CatAccessoryDefinition(id, item,
                !json.has("enabled") || json.get("enabled").getAsBoolean(),
                outfit, group, description, effects, CatAccessoryCharge.parse(json), script);
    }

    public boolean activeFor(String outfit) {
        return enabled && (requiredOutfit.equals("any") || requiredOutfit.equals(outfit));
    }

    public double value(String key) { return effects.getOrDefault(key, 0.0D); }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("schema_version", SCHEMA_VERSION);
        json.addProperty("item", item);
        json.addProperty("enabled", enabled);
        json.addProperty("required_outfit", requiredOutfit);
        if(!script.isEmpty())json.addProperty("script",script);
        if (!exclusiveGroup.isEmpty()) json.addProperty("exclusive_group", exclusiveGroup);
        if (!description.isEmpty()) json.addProperty("description", description);
        JsonObject values = new JsonObject();
        effects.forEach(values::addProperty);
        json.add("effects", values);
        if (charge.capacity() > 0) json.add("charge", charge.toJson());
        return json;
    }

    public static double dodgeChance(double speed, double percentPerPoint) {
        return Math.max(0.0D, Math.min(0.8D, speed * percentPerPoint / 100.0D));
    }

    public static double threatWeight(double bias) {
        return bias >= 0.0D ? 1.0D + bias : 1.0D / (1.0D - bias);
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }

    public static void requireId(String id) {
        if (id == null || id.length() > 256 || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
            throw new IllegalArgumentException("Invalid namespaced ID: " + id);
    }
}
