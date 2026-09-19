package cn.laowu.mod.accessory;

import cn.laowu.mod.LaoWuMod;
import com.google.gson.JsonParser;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Registered items are independent of data-pack definitions and retain stable save IDs. */
public final class CatAccessoryItems {
    public static final Map<String, CatAccessoryDefinition> DEFAULTS = defaults();
    private static final Map<String, Supplier<? extends Item>> ITEMS = new LinkedHashMap<>();
    public static void register(java.util.function.BiFunction<String, Supplier<Item>, Supplier<? extends Item>> register) {
        if (!ITEMS.isEmpty()) return;
        DEFAULTS.values().forEach(def -> ITEMS.put(def.item(), register.apply(
                def.item().substring("laowu:".length()), () -> new Item(properties(def.item())))));
    }
    private static Item.Properties properties(String id) {
        var properties=new Item.Properties().stacksTo(1).rarity(CatAccessoryRarity.forItem(id));
        if(id.equals("laowu:cat_cork_vest")||id.equals("laowu:cat_roly_poly"))properties.durability(50);
        return properties;
    }
    public static void display(net.minecraft.world.item.CreativeModeTab.Output output) {
        ITEMS.values().forEach(item -> output.accept(item.get()));
    }
    public static ItemStack tabIcon() { return ITEMS.get("laowu:cat_taunt_bell").get().getDefaultInstance(); }
    /** Registry-backed boss reward; no crafting recipe. */
    public static Item butterReward() { return ITEMS.get("laowu:cat_butter_cube").get(); }
    private static Map<String, CatAccessoryDefinition> defaults() {
        Map<String, CatAccessoryDefinition> result = new LinkedHashMap<>();
        add(result, "laowu:cat_taunt_bell", "{\"schema_version\":1,\"item\":\"laowu:cat_taunt_bell\",\"exclusive_group\":\"laowu:threat\",\"effects\":{\"aggro_bias\":3}}");
        add(result, "laowu:cat_silent_bell", "{\"schema_version\":1,\"item\":\"laowu:cat_silent_bell\",\"exclusive_group\":\"laowu:threat\",\"effects\":{\"aggro_bias\":-3}}");
        add(result, "laowu:cat_stability_anchor", "{\"schema_version\":1,\"item\":\"laowu:cat_stability_anchor\",\"effects\":{\"knockback_resistance\":1}}");
        add(result, "laowu:cat_fire_charm", "{\"schema_version\":1,\"item\":\"laowu:cat_fire_charm\",\"effects\":{\"fire_immune\":1}}");
        add(result, "laowu:cat_impact_core", "{\"schema_version\":1,\"item\":\"laowu:cat_impact_core\",\"effects\":{\"projectile_knockback\":1}}");
        add(result, "laowu:cat_followup_gear", "{\"schema_version\":1,\"item\":\"laowu:cat_followup_gear\",\"effects\":{\"attack\":-20,\"extra_strike_chance\":25},\"required_outfit\":\"terminator\"}");
        add(result, "laowu:cat_loot_magnet", "{\"schema_version\":1,\"item\":\"laowu:cat_loot_magnet\",\"effects\":{\"loot_magnet_radius\":3}}");
        add(result, "laowu:cat_ace_feather", "{\"schema_version\":1,\"item\":\"laowu:cat_ace_feather\",\"required_outfit\":\"flight\",\"effects\":{\"attack\":-20,\"pilot_dodge_per_speed\":0.15}}");
        add(result, "laowu:cat_blast_fuse", "{\"schema_version\":1,\"item\":\"laowu:cat_blast_fuse\",\"required_outfit\":\"dynamite\",\"effects\":{\"health\":-20,\"self_destruct_multiplier\":10}}");
        add(result, "laowu:cat_health_badge", "{\"schema_version\":1,\"item\":\"laowu:cat_health_badge\",\"effects\":{\"health\":10}}");
        add(result, "laowu:cat_attack_badge", "{\"schema_version\":1,\"item\":\"laowu:cat_attack_badge\",\"effects\":{\"attack\":10}}");
        add(result, "laowu:cat_speed_badge", "{\"schema_version\":1,\"item\":\"laowu:cat_speed_badge\",\"effects\":{\"speed\":10}}");
        add(result, "laowu:cat_stamina_badge", "{\"schema_version\":1,\"item\":\"laowu:cat_stamina_badge\",\"effects\":{\"stamina\":10}}");
        add(result, "laowu:cat_intelligence_badge", "{\"schema_version\":1,\"item\":\"laowu:cat_intelligence_badge\",\"effects\":{\"intelligence\":10}}");
        add(result, "laowu:cat_luck_badge", "{\"schema_version\":1,\"item\":\"laowu:cat_luck_badge\",\"effects\":{\"luck\":10}}");
        add(result, "laowu:cat_butter_cube", "{\"schema_version\":1,\"item\":\"laowu:cat_butter_cube\",\"effects\":{\"speed\":10,\"moving_damage_reduction\":25}}");
        add(result, "laowu:cat_reel_hook", "{\"schema_version\":1,\"item\":\"laowu:cat_reel_hook\",\"required_outfit\":\"fishing\",\"effects\":{\"speed\":10,\"fishing_pull\":1}}");
        add(result, "laowu:cat_blue_flame_nozzle", "{\"schema_version\":1,\"item\":\"laowu:cat_blue_flame_nozzle\",\"required_outfit\":\"fire\",\"effects\":{\"stamina\":-20,\"super_flame_multiplier\":1.5}}");
        add(result, "laowu:cat_honey_stamp", "{\"schema_version\":1,\"item\":\"laowu:cat_honey_stamp\",\"required_outfit\":\"honey\",\"effects\":{\"attack\":-10,\"honey_patch\":1}}");
        add(result, "laowu:cat_concentrated_pouch", "{\"schema_version\":1,\"item\":\"laowu:cat_concentrated_pouch\",\"required_outfit\":\"transport\",\"effects\":{\"speed\":-30,\"enhanced_potions\":1}}");
        add(result, "laowu:cat_mixed_magazine", "{\"schema_version\":1,\"item\":\"laowu:cat_mixed_magazine\",\"required_outfit\":\"engineering\",\"effects\":{\"speed\":-10,\"engineering_special_ammo\":1}}");
        add(result, "laowu:cat_guard_bandage", "{\"schema_version\":1,\"item\":\"laowu:cat_guard_bandage\",\"required_outfit\":\"medical\",\"effects\":{\"intelligence\":-10,\"medical_guard\":20}}");
        add(result, "laowu:cat_rhythm_tambourine", "{\"schema_version\":1,\"item\":\"laowu:cat_rhythm_tambourine\",\"required_outfit\":\"music\",\"effects\":{\"health\":-10,\"music_speed_bonus\":20}}");
        add(result, "laowu:cat_medic_smoke_canister", "{\"schema_version\":1,\"item\":\"laowu:cat_medic_smoke_canister\",\"required_outfit\":\"agent\",\"effects\":{\"attack\":-10,\"healing_smoke\":1}}");
        add(result, "laowu:cat_purifying_filter", "{\"schema_version\":1,\"item\":\"laowu:cat_purifying_filter\",\"required_outfit\":\"diving\",\"effects\":{\"attack\":-10,\"diving_cleanse\":1}}");
        add(result, "laowu:cat_rebirth_ootheca", "{\"schema_version\":1,\"item\":\"laowu:cat_rebirth_ootheca\",\"required_outfit\":\"cockroach\",\"effects\":{\"stamina\":-20,\"cockroach_split\":1}}");
        add(result, "laowu:cat_chew_bone", "{\"schema_version\":1,\"item\":\"laowu:cat_chew_bone\",\"effects\":{\"damage_combo\":4}}");
        add(result, "laowu:cat_mouse_plush", "{\"schema_version\":1,\"item\":\"laowu:cat_mouse_plush\",\"effects\":{\"opening_damage\":35}}");
        add(result, "laowu:cat_catnip_pouch", "{\"schema_version\":1,\"item\":\"laowu:cat_catnip_pouch\",\"effects\":{\"critical_haste\":15}}");
        add(result, "laowu:cat_spiked_collar", "{\"schema_version\":1,\"item\":\"laowu:cat_spiked_collar\",\"effects\":{\"melee_reflect\":50}}");
        add(result, "laowu:cat_cork_vest", "{\"schema_version\":1,\"item\":\"laowu:cat_cork_vest\",\"exclusive_group\":\"laowu:lifeguard\",\"effects\":{\"emergency_shield\":15}}");
        add(result, "laowu:cat_roly_poly", "{\"schema_version\":1,\"item\":\"laowu:cat_roly_poly\",\"exclusive_group\":\"laowu:lifeguard\",\"effects\":{\"heavy_hit_cap\":30}}");
        add(result, "laowu:cat_old_food_bowl", "{\"schema_version\":1,\"item\":\"laowu:cat_old_food_bowl\",\"effects\":{\"rest_heal\":1}}");
        add(result, "laowu:cat_warm_scarf", "{\"schema_version\":1,\"item\":\"laowu:cat_warm_scarf\",\"effects\":{\"healing_received\":20}}");
        add(result, "laowu:cat_tracking_tag", "{\"schema_version\":1,\"item\":\"laowu:cat_tracking_tag\",\"effects\":{\"owner_attack_bonus\":30}}");
        add(result, "laowu:cat_sorting_pouch", "{\"schema_version\":1,\"item\":\"laowu:cat_sorting_pouch\",\"effects\":{\"sample_pickup\":1}}");
        return Collections.unmodifiableMap(result);
    }
    private static void add(Map<String, CatAccessoryDefinition> result, String id, String json) {
        result.put(id, CatAccessoryDefinition.parse(id, JsonParser.parseString(json).getAsJsonObject()));
    }
    private CatAccessoryItems() {}
}
