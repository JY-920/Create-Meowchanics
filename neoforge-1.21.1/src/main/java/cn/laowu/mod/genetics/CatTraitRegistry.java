package cn.laowu.mod.genetics;

import com.google.gson.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.LoggerFactory;
import java.util.*;

/** Atomic server data-pack registry and a separate authoritative client mirror. */
public final class CatTraitRegistry extends SimpleJsonResourceReloadListener {
    public static final int MAX_CUSTOM_TRAITS = 128;
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger("laowu/trait_registry");
    private static volatile Map<ResourceLocation, ScriptedCatTraitDefinition> server = Map.of(), client = Map.of();
    private static volatile long serverRevision, clientRevision;
    public CatTraitRegistry() { super(new Gson(), "cat_traits"); }
    @Override protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, ScriptedCatTraitDefinition> next = new LinkedHashMap<>();
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            ResourceLocation id = entry.getKey();
            try {
                if (next.size() >= MAX_CUSTOM_TRAITS) throw new IllegalArgumentException("Too many custom traits");
                var definition = ScriptedCatTraitDefinition.parse(id.toString(), entry.getValue().getAsJsonObject());
                next.put(id, definition);
            } catch (RuntimeException error) {
                // A malformed edit retains its previous working definition; a removed file deliberately does not.
                if (next.size() < MAX_CUSTOM_TRAITS && server.containsKey(id)) next.put(id, server.get(id));
                LOG.error("Ignoring cat trait {}: {}", id, error.getMessage());
            }
        });
        // The base JSON reader omits syntactically invalid files before apply().
        // Distinguish that from a genuinely removed resource, without retaining removed scripts forever.
        if (manager != null) for (var previous : server.entrySet()) {
            var id = previous.getKey();
            var file = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "cat_traits/" + id.getPath() + ".json");
            if (!resources.containsKey(id) && next.size() < MAX_CUSTOM_TRAITS && manager.getResource(file).isPresent()) {
                next.put(id, previous.getValue());
                LOG.warn("Retaining previous cat trait {} because its resource could not be decoded", id);
            }
        }
        server = Map.copyOf(next); serverRevision++;
        LOG.info("Loaded {} custom cat traits; built-in trait IDs are unchanged", next.size());
    }
    public static ScriptedCatTraitDefinition definition(ResourceLocation id, boolean clientSide) {
        return (clientSide ? client : server).get(id);
    }
    public static CatTraitType resolve(ResourceLocation id, boolean clientSide) {
        if (id == null) return null;
        CatTrait builtin = CatTrait.byId(id).orElse(null);
        return builtin != null ? builtin : new ScriptedCatTrait(id, clientSide);
    }
    public static CatTraitType find(String id, boolean clientSide) {
        var parsed = ResourceLocation.tryParse(id);
        var type = resolve(parsed, clientSide);
        return type != null && type.available() ? type : null;
    }
    public static List<CatTraitType> values(boolean clientSide) {
        var result = new ArrayList<CatTraitType>(List.of(CatTrait.values()));
        (clientSide ? client : server).keySet().stream().sorted()
                .map(id -> resolve(id, clientSide)).forEach(result::add);
        return List.copyOf(result);
    }
    public static long revision(boolean clientSide) { return clientSide ? clientRevision : serverRevision; }
    public static CompoundTag networkData() {
        var tag = new CompoundTag(); var entries = new ListTag();
        server.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            var value = new CompoundTag(); value.putString("Id", entry.getKey().toString());
            value.putString("Json", entry.getValue().toJson().toString()); entries.add(value);
        });
        tag.put("Definitions", entries);
        return tag;
    }
    public static void receive(CompoundTag tag) {
        if (!tag.contains("Definitions", Tag.TAG_LIST)) return;
        if (!(tag.get("Definitions") instanceof ListTag entries)
                || !entries.isEmpty() && entries.getElementType() != Tag.TAG_COMPOUND) return;
        if (entries.size() > MAX_CUSTOM_TRAITS) return;
        Map<ResourceLocation, ScriptedCatTraitDefinition> next = new LinkedHashMap<>();
        try {
            for (int i = 0; i < entries.size(); i++) {
                var entry = entries.getCompound(i);
                String json = entry.getString("Json");
                if (json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > ScriptedCatTraitDefinition.MAX_JSON_LENGTH)
                    throw new IllegalArgumentException("Oversize trait sync");
                var definition = ScriptedCatTraitDefinition.parse(entry.getString("Id"), JsonParser.parseString(json).getAsJsonObject());
                if (next.put(definition.id(), definition) != null) throw new IllegalArgumentException("Duplicate trait ID");
            }
            client = Map.copyOf(next); clientRevision++;
        } catch (RuntimeException error) { LOG.error("Invalid trait registry sync: {}", error.getMessage()); }
    }
    public static void resetServer() { server = Map.of(); serverRevision++; }
    public static void resetClient() { client = Map.of(); clientRevision++; }
}
