package cn.laowu.mod.accessory;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

public final class CatAccessoriesRegression {
    private static int checks;
    private static void check(boolean result, String reason) {
        checks++;
        if (!result) throw new AssertionError(reason);
    }
    private static CatAccessoryDefinition custom(String name, String effects) {
        return CatAccessoryDefinition.parse("test:" + name, JsonParser.parseString(
                "{\"schema_version\":1,\"item\":\"test:" + name + "\",\"effects\":" + effects + "}").getAsJsonObject());
    }
    private static void invalid(String json) {
        try {
            CatAccessoryDefinition.parse("test:invalid", JsonParser.parseString(json).getAsJsonObject());
        } catch (RuntimeException expected) { checks++; return; }
        throw new AssertionError("Accepted malformed definition: " + json);
    }
    public static void main(String[] args) throws Exception {
        Map<String, CatAccessoryDefinition> defs = new LinkedHashMap<>();
        try (var files = Files.list(Path.of(args[0]))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                String id = "laowu:" + file.getFileName().toString().replace(".json", "");
                var def = CatAccessoryDefinition.parse(id, JsonParser.parseString(Files.readString(file)).getAsJsonObject());
                check(!defs.containsKey(id), "unique definition");
                check(def.equals(CatAccessoryDefinition.parse(id, def.toJson())), "network round trip " + id);
                check(def.item().equals(id), "stable builtin item ID");
                defs.put(id, def);
            }
        }
        check(defs.size() == 36, "36 accessories, thirteen career exclusive");
        for (String id : List.of("cat_chew_bone", "cat_mouse_plush", "cat_catnip_pouch", "cat_spiked_collar", "cat_cork_vest", "cat_roly_poly", "cat_old_food_bowl", "cat_warm_scarf", "cat_tracking_tag", "cat_sorting_pouch")) {
            var general = defs.get("laowu:" + id);
            check(general != null && general.requiredOutfit().equals("any"), "General accessory usable by any cat: " + id);
            check(general.effects().values().stream().allMatch(v -> v >= 0), "General accessory has no negative attribute costs");
            check(CatAccessoryLoadout.resolve(List.of(general, general), "none").effects().equals(general.effects()), "Duplicate general accessory does not stack");
        }
        var vest = defs.get("laowu:cat_cork_vest");
        var roly = defs.get("laowu:cat_roly_poly");
        check(CatAccessoryLoadout.resolve(List.of(vest, roly), "none").effects().get("heavy_hit_cap") == null, "Cork and roly remain mutually exclusive under direct NBT edits");
        check(defs.get("laowu:cat_tracking_tag").value("owner_attack_bonus") == 30
                && defs.get("laowu:cat_tracking_tag").value("attack") == 0, "Owner bonus conditional, not permanent genes");
        var careers=new HashSet<String>();
        for(var def:defs.values())if(!def.requiredOutfit().equals("any")){
            check(careers.add(def.requiredOutfit()),"One default exclusive per career");
            check(CatAccessoryLoadout.resolve(List.of(def),"none").effects().isEmpty(),"Exclusive inactive without career");
            check(CatAccessoryLoadout.resolve(List.of(def),def.requiredOutfit()).effects().equals(def.effects()),"Exclusive full effects on intended career");
        }
        check(careers.size()==13,"Every career has its own exclusive");
        var butter = defs.get("laowu:cat_butter_cube");
        check(butter.value("speed") == 10 && butter.value("moving_damage_reduction") == 25, "Butter movement profile");
        invalid("{\"schema_version\":1,\"item\":\"test:x\",\"effects\":{\"moving_damage_reduction\":81}}");
        check(custom("max_reduction", "{\"moving_damage_reduction\":80}").value("moving_damage_reduction") == 80, "Reduction limit");
        var attack = defs.get("laowu:cat_attack_badge");
        var health = defs.get("laowu:cat_health_badge");
        var taunt = defs.get("laowu:cat_taunt_bell");
        var silent = defs.get("laowu:cat_silent_bell");
        var feather = defs.get("laowu:cat_ace_feather");
        var blast = defs.get("laowu:cat_blast_fuse");
        var gear = defs.get("laowu:cat_followup_gear");
        check(gear.value("extra_strike_chance") == 25 && gear.requiredOutfit().equals("terminator") && gear.value("attack")==-20, "25% laser-only follow-up with attack cost");
        check(defs.get("laowu:cat_loot_magnet").value("loot_magnet_radius") == 3, "3-block magnet");
        var two = CatAccessoryLoadout.resolve(List.of(attack, attack, health), "none");
        check(two.effects().get("attack") == 10, "duplicate gear cannot stack");
        check(two.effects().get("health") == 10, "independent stat effects coexist");
        check(CatAccessoryLoadout.resolve(List.of(), "none").effects().isEmpty(), "removal clears all bonuses");
        check(CatAccessoryLoadout.resolve(List.of(taunt, silent), "none").effects().get("aggro_bias") == 3,
                "exclusive threat group, first slot wins even for command-edited equipment");
        check(CatAccessoryLoadout.resolve(List.of(feather, blast), "none").effects().isEmpty(), "wrong career inactive");
        check(CatAccessoryLoadout.resolve(List.of(feather), "flight").effects().get("pilot_dodge_per_speed") == .15,
                "pilot-only effect");
        check(CatAccessoryLoadout.resolve(List.of(blast), "dynamite").effects().get("self_destruct_multiplier") == 10,
                "dynamite 5x -> 10x");
        check(CatAccessoryLoadout.resolve(List.of(attack, health, gear, feather, blast), "dynamite")
                .effects().get("self_destruct_multiplier") == null, "only four equipment slots");
        var userCharm = custom("user_charm", "{\"attack\":12,\"projectile_knockback\":1,\"extra_strike_chance\":30}");
        var combined = CatAccessoryLoadout.resolve(List.of(userCharm, attack, gear), "terminator");
        check(combined.effects().get("attack") == 2, "different items add stat bonuses");
        check(combined.effects().get("extra_strike_chance") == 30, "same mechanism uses maximum, not sum");
        check(combined.ids().contains("test:user_charm"), "external namespaced IDs kept");
        JsonObject disabled = userCharm.toJson();
        disabled.addProperty("enabled", false);
        check(CatAccessoryLoadout.resolve(List.of(CatAccessoryDefinition.parse(userCharm.id(), disabled)), "none")
                .effects().isEmpty(), "disabled data does not execute");
        var scripted = custom("scripted_charm", "{}");
        check(CatAccessoryLoadout.resolve(List.of(scripted), "none").ids().contains("test:scripted_charm"),
                "empty effect definition usable by custom KubeJS behavior");
        for (String stat : CatAccessoryDefinition.STATS) {
            var positive = custom("positive", "{\"" + stat + "\":300}");
            var negative = custom("negative", "{\"" + stat + "\":-300}");
            check(CatAccessoryLoadout.resolve(List.of(positive, negative), "none").effects().get(stat) == 0,
                    "signed stat bonuses cancel before clamping");
            invalid("{\"schema_version\":1,\"item\":\"test:x\",\"effects\":{\"" + stat + "\":301}}");
            invalid("{\"schema_version\":1,\"item\":\"test:x\",\"effects\":{\"" + stat + "\":1.5}}");
        }
        for (int speed = 0; speed <= 999; speed++) {
            double probability = CatAccessoryDefinition.dodgeChance(speed, .15);
            check(probability >= 0 && probability <= .8, "bounded dodge " + speed);
            check(Math.abs(probability - Math.min(.8, speed * .0015)) < 1e-12, "percent units " + speed);
        }
        check(Math.abs(CatAccessoryDefinition.dodgeChance(100, .15) - .15) < 1e-12, "100 speed = 15%, not 1500%");
        check(CatAccessoryDefinition.dodgeChance(-20, .15) == 0, "negative speed not negative probability");
        check(CatAccessoryDefinition.threatWeight(3) == 4, "taunt threat weighting");
        check(CatAccessoryDefinition.threatWeight(-3) == .25, "quiet threat weighting");
        check(CatAccessoryDefinition.threatWeight(0) == 1, "unequipped behavior unchanged");
        invalid("{\"schema_version\":2,\"item\":\"test:x\",\"effects\":{}}");
        invalid("{\"schema_version\":1.5,\"item\":\"test:x\",\"effects\":{}}");
        invalid("{\"item\":\"test:x\",\"effects\":{}}");
        invalid("{\"schema_version\":1,\"item\":\"invalid\",\"effects\":{}}");
        invalid("{\"schema_version\":1,\"item\":\"test:x\",\"effects\":{\"unknown\":1}}");
        invalid("{\"schema_version\":1,\"item\":\"test:x\",\"effects\":{\"fire_immune\":0.5}}");
        invalid("{\"schema_version\":1,\"item\":\"test:x\",\"effects\":{\"extra_strike_chance\":101}}");
        invalid("{\"schema_version\":1,\"item\":\"test:x\",\"effects\":{\"loot_magnet_radius\":99}}");
        invalid("{\"schema_version\":1,\"item\":\"test:x\",\"effects\":{\"attack\":\"NaN\"}}");
        invalid("{\"schema_version\":1,\"item\":\"test:x\",\"required_outfit\":\"unknown\",\"effects\":{}}");
        invalid("{\"schema_version\":1,\"item\":\"test:x\",\"effects\":[]}");
        JsonObject charged = scripted.toJson();
        charged.add("charge", JsonParser.parseString("{\"capacity\":100,\"initial\":20}"));
        var chargedDef = CatAccessoryDefinition.parse(scripted.id(), charged);
        check(chargedDef.charge().capacity() == 100 && chargedDef.charge().initial() == 20, "charge contract");
        check(chargedDef.equals(CatAccessoryDefinition.parse(chargedDef.id(), chargedDef.toJson())), "charge network roundtrip");
        check(scripted.charge().equals(CatAccessoryCharge.NONE), "old v1 definitions remain valid");
        for (int amount = -200; amount <= 200; amount++)
            check(chargedDef.charge().clamp(amount) == Math.max(0, Math.min(100, amount)), "charge clamp");
        for (String bad : new String[]{"{\"capacity\":-1}", "{\"capacity\":1.5}",
                "{\"capacity\":10,\"initial\":11}", "{\"capacity\":1000001}", "{\"initial\":1}"}) {
            charged.add("charge", JsonParser.parseString(bad));
            invalid(charged.toString());
        }
        for (int charge = 0; charge <= 100; charge++) for (int cost = 0; cost <= 101; cost++) {
            check(CatAccessoryScriptRules.canActivate(100, 99, charge, cost) == (charge >= cost), "activation charge gate");
            check(!CatAccessoryScriptRules.canActivate(100, 101, charge, cost), "cooldown prevents activation");
        }
        check(CatAccessoryScriptRules.canActivate(100, 100, 0, 0), "ready exactly at deadline and zero-cost effects");
        for (double invalid : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY, 1E20}) {
            try { CatAccessoryScriptRules.damage(invalid); throw new AssertionError("Accepted invalid damage"); }
            catch (IllegalArgumentException expected) { checks++; }
        }
        try { CatAccessoryScriptRules.key("unscoped"); throw new AssertionError("Accepted unscoped key"); }
        catch (IllegalArgumentException expected) { checks++; }
        System.out.println("PASS: " + checks + " cat accessory definition/loadout/probability/state checks");
    }
}
