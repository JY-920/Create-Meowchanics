package cn.laowu.mod.genetics;

import cn.laowu.mod.ServerConfig;
import com.google.gson.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import java.util.*;

/** Real production registry/profile/parser tests, without a game world or KubeJS runtime. */
public final class CatTraitScriptRegression {
    private static int checks;
    private static final String A = "testpack:agile", B = "testpack:brave";
    private static JsonObject definition(String extra) {
        return JsonParser.parseString("{\"schema_version\":1,\"title\":\"Agile\",\"rarity\":\"good\","
                + "\"max_level\":3,\"description\":[\"I\",\"II\",\"III\"]" + extra + "}").getAsJsonObject();
    }
    private static void reload(Map<String, JsonObject> definitions) {
        var resources = new LinkedHashMap<ResourceLocation, JsonElement>();
        definitions.forEach((id, json) -> resources.put(ResourceLocation.tryParse(id), json));
        new CatTraitRegistry().apply(resources, null, null);
    }
    private static CatTraitType type(String id) { return CatTraitRegistry.resolve(ResourceLocation.tryParse(id), false); }
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    private static void rejected(String id, JsonObject json) {
        boolean rejected = false;
        try { ScriptedCatTraitDefinition.parse(id, json); } catch (RuntimeException expected) { rejected = true; }
        check(rejected, "Invalid trait definition must be rejected: " + json);
    }
    public static void run() {
        var disabled = List.copyOf(ServerConfig.DISABLED_TRAITS.get());
        CatTraitRegistry.resetServer(); CatTraitRegistry.resetClient();
        try {
            ServerConfig.DISABLED_TRAITS.set(List.of());
            var natives = CatTraitRegistry.values(false);
            check(natives.size() == CatTrait.values().length && natives.stream().allMatch(t -> t instanceof CatTrait),
                    "No custom definitions means original catalog");
            var original = new ArrayList<CompoundTag>();
            for (int i = 0; i < 200; i++) original.add(CatTraitProfile.founder(RandomSource.create(i)).save());
            var good = definition(",\"stat_bonuses\":{\"speed\":[5,10,15]}");
            var parsed = ScriptedCatTraitDefinition.parse(A, good);
            check(parsed.bonus(CatStat.SPEED, 3) == 15 && parsed.description(2).equals("II"), "Level-based metadata");
            check(!parsed.natural() && !parsed.mutation() && parsed.inheritable() && parsed.enabled(), "Safe defaults");
            check(ScriptedCatTraitDefinition.parse(A, parsed.toJson()).equals(parsed), "Canonical round trip");
            rejected("invalid id", good); rejected(CatTrait.THORNS.id().toString(), good);
            for (String field : List.of("schema_version", "title", "rarity")) {
                var bad = good.deepCopy(); bad.remove(field); rejected(A, bad);
            }
            for (String extra : List.of(",\"max_level\":0", ",\"max_level\":8", ",\"weight\":0",
                    ",\"weight\":10001", ",\"enabled\":1", ",\"naturall\":true", ",\"rarity\":\"epic\"",
                    ",\"stat_bonuses\":{\"speed\":[1,2]}", ",\"stat_bonuses\":{\"attack\":1.2}",
                    ",\"stat_bonuses\":{\"strength\":10}", ",\"stat_bonuses\":{\"luck\":1000000}",
                    ",\"conflicts\":[\"testpack:agile\"]", ",\"slots\":[\"wrong\"]"))
                rejected(A, definition(extra));
            var oversize = definition(""); oversize.addProperty("title", "A".repeat(97)); rejected(A, oversize);
            oversize = definition(""); var descriptions = new JsonArray();
            for (int i = 0; i < 3; i++) descriptions.add("猫".repeat(500));
            oversize.add("description", descriptions); rejected(A, oversize);

            reload(Map.of(A, good, B, definition("")));
            var agile = type(A);
            check(CatTraitRegistry.values(false).size() == natives.size() + 2, "Custom catalog appended");
            check(CatTraitRegistry.values(true).size() == natives.size(), "Client/server mirrors are separate");
            for (int i = 0; i < 200; i++) check(original.get(i).equals(CatTraitProfile.founder(RandomSource.create(i)).save()),
                    "Manual-only custom traits do not perturb native founder sequence");
            CatTraitRegistry.receive(CatTraitRegistry.networkData());
            check(CatTraitRegistry.find(A, true).appearanceAttributeBonus(CatStat.SPEED, 2) == 10, "Definition sync");
            var clientRevision = CatTraitRegistry.revision(true);
            var malformed = CatTraitRegistry.networkData();
            malformed.getList("Definitions", Tag.TAG_COMPOUND).getCompound(0).putString("Json", "{}");
            CatTraitRegistry.receive(malformed);
            check(CatTraitRegistry.revision(true) == clientRevision && CatTraitRegistry.find(A, true) != null,
                    "Invalid sync is atomic and retains old definitions");
            malformed = new CompoundTag(); var wrongList = new ListTag(); wrongList.add(StringTag.valueOf("bad"));
            malformed.put("Definitions", wrongList); CatTraitRegistry.receive(malformed);
            check(CatTraitRegistry.revision(true) == clientRevision, "Wrong NBT list type cannot clear mirror");
            var parent = CatTraitProfile.EMPTY.withLevel(agile, 3).withLevel(type(B), 2);
            for (int i = 0; i < 40; i++) {
                var child = CatTraitProfile.breed(parent, parent, 0F, RandomSource.create(i));
                check(child.level(agile) == 1 && child.level(type(B)) == 1, "Custom shared inheritance resets to I");
            }
            var saved = parent.save();
            check(CatTraitProfile.load(saved).orElseThrow().save().equals(saved), "Custom profile NBT round trip");
            reload(Map.of());
            check(!agile.available() && parent.level(agile) == 0 && parent.rawLevel(agile) == 3,
                    "Removed definition suspends effects without deleting data");
            check(CatTraitProfile.load(saved).orElseThrow().save().equals(saved), "Missing definition survives NBT");
            check(CatTraitProfile.EMPTY.withLevel(agile, 1).traits().isEmpty(), "Cannot acquire missing definition");
            check(parent.withLevel(agile, 0).rawLevel(agile) == 0, "Missing definition can still be explicitly removed");
            var lowered = definition(",\"max_level\":1,\"description\":\"I\"");
            reload(Map.of(A, lowered));
            check(parent.level(agile) == 1 && parent.rawLevel(agile) == 3, "Temporary lowered max keeps saved level");
            reload(Map.of(A, good, B, definition("")));
            check(parent.level(agile) == 3 && agile.appearanceAttributeBonus(CatStat.SPEED, 3) == 15,
                    "Existing references recover after reload");
            reload(Map.of(A, new JsonObject(), B, definition("")));
            check(parent.level(agile) == 3, "Malformed edit retains previous working definition");
            var malformedResources = (net.minecraft.server.packs.resources.ResourceManager) java.lang.reflect.Proxy.newProxyInstance(
                    CatTraitScriptRegression.class.getClassLoader(),
                    new Class<?>[]{net.minecraft.server.packs.resources.ResourceManager.class},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("getResource")) return arguments[0].toString().equals("testpack:cat_traits/agile.json")
                                ? Optional.of(new Object()) : Optional.empty();
                        throw new UnsupportedOperationException(method.getName());
                    });
            new CatTraitRegistry().apply(Map.of(), malformedResources, null);
            check(agile.available() && !type(B).available(), "Syntax-invalid existing resource is retained; truly removed resource is not");
            reload(Map.of(A, definition(",\"enabled\":false")));
            check(!agile.enabled() && !agile.inheritable() && agile.appearanceAttributeBonus(CatStat.SPEED, 3) == 0,
                    "Disabled definition cannot grant effects or inheritance");
            check(CatTraitProfile.load(saved).orElseThrow().rawLevel(agile) == 3, "Disabled data preserved");

            reload(Map.of(A, definition(",\"conflicts\":[\"testpack:brave\"]"), B, definition("")));
            check(CatTraitProfile.EMPTY.withLevel(agile, 1).withLevel(type(B), 1).traits().size() == 1, "Forward conflict");
            check(CatTraitProfile.EMPTY.withLevel(type(B), 1).withLevel(agile, 1).traits().size() == 1, "Reverse conflict");
            check(CatTraitProfile.load(saved).orElseThrow().traits().size() == 2, "Conflict reload cannot delete saved traits");
            reload(Map.of(A, definition(",\"slots\":[\"health_bonus\"]")));
            check(CatTraitProfile.EMPTY.withLevel(agile, 1).withLevel(CatTrait.TOUGH, 1).traits().size() == 1,
                    "Custom slot conflicts with native slot");
            reload(Map.of(A, good, B, definition("")));
            var full = CatTraitProfile.EMPTY.withLevel(agile, 1).withLevel(type(B), 1)
                    .withLevel(CatTrait.THORNS, 1).withLevel(CatTrait.NIGHT_OWL, 1);
            check(full.traits().size() == 4 && full.withLevel(CatTrait.DOUGHY, 1).traits().size() == 4, "Shared four-slot limit");
            ServerConfig.DISABLED_TRAITS.set(List.of(A));
            check(CatTraitProfile.EMPTY.withLevel(agile, 1).traits().isEmpty(), "Blacklist forbids new acquisition");
            check(parent.withLevel(agile, 2).level(agile) == 2, "Blacklist preserves management of already owned traits");
            check(!CatTraitProfile.breed(parent, parent, 0F, RandomSource.create(8)).has(agile),
                    "Blacklist also blocks inheritance");
            var nativeIds = Arrays.stream(CatTrait.values()).map(t -> t.id().toString()).toList();
            ServerConfig.DISABLED_TRAITS.set(nativeIds);
            reload(Map.of(A, definition(",\"natural\":true,\"mutation\":true,\"weight\":10"),
                    B, definition(",\"natural\":true,\"mutation\":true,\"weight\":1")));
            int firstA = 0, firstB = 0;
            var random = RandomSource.create(87121);
            for (int i = 0; i < 2000; i++) {
                var founder = CatTraitProfile.founder(random);
                if (!founder.traits().isEmpty()) {
                    if (founder.traits().get(0).trait().id().toString().equals(A)) firstA++; else firstB++;
                }
            }
            check(firstA > firstB * 6 && firstB > 50, "Within-tier custom generation weights take effect");
            for (int i = 0; i < 20; i++) {
                var child = CatTraitProfile.breed(CatTraitProfile.EMPTY, CatTraitProfile.EMPTY, 1F, random);
                check(child.traits().size() == 1 && (child.has(agile) || child.has(type(B))), "Custom mutation pool");
            }
            ServerConfig.DISABLED_TRAITS.set(List.of());
            var duplicate = saved.copy(); duplicate.getList("Traits", Tag.TAG_COMPOUND).add(
                    duplicate.getList("Traits", Tag.TAG_COMPOUND).getCompound(0).copy());
            check(CatTraitProfile.load(duplicate).orElseThrow().traits().size() == 2, "Duplicate saved IDs ignored");
            CatTraitRegistry.resetClient();
            check(CatTraitRegistry.find(A, true) == null && CatTraitRegistry.find(A, false) != null, "Logout clears only client");
            System.out.println("PASS: " + checks + " custom trait schema, reload, sync, slot, save, inheritance and generation checks");
        } finally {
            ServerConfig.DISABLED_TRAITS.set(disabled);
            CatTraitRegistry.resetServer(); CatTraitRegistry.resetClient(); CatTraitHooks.reset();
        }
    }
}
