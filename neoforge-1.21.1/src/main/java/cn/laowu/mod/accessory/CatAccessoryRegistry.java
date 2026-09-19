package cn.laowu.mod.accessory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** Reloadable server definitions plus a separate client mirror; never rewrites external packs. */
public final class CatAccessoryRegistry extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("CreateMeowchanics/Accessories");
    public static final int MAX_DEFINITIONS = 256;
    private record Snapshot(Map<String, CatAccessoryDefinition> ids,
                            Map<String, CatAccessoryDefinition> items, long revision) {}
    private static volatile Snapshot server = snapshot(CatAccessoryItems.DEFAULTS, 0);
    private static volatile Snapshot client = snapshot(CatAccessoryItems.DEFAULTS, 0);

    public CatAccessoryRegistry() { super(new Gson(), "cat_accessories"); }

    @Override protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager manager,
                                   ProfilerFiller profiler) {
        Map<String, CatAccessoryDefinition> result = new LinkedHashMap<>(CatAccessoryItems.DEFAULTS);
        // Sort for deterministic duplicate handling, independent of filesystem iteration.
        Map<String, JsonElement> sorted = new TreeMap<>();
        json.forEach((id, value) -> sorted.put(id.toString(), value));
        sorted.forEach((id, value) -> {
            try {
                CatAccessoryDefinition def = CatAccessoryDefinition.parse(id, value.getAsJsonObject());
                if (!BuiltInRegistries.ITEM.containsKey(ResourceLocation.tryParse(def.item()))
                        || def.item().equals("minecraft:air"))
                    throw new IllegalArgumentException("Unregistered item " + def.item());
                if (result.size() >= MAX_DEFINITIONS && !result.containsKey(id))
                    throw new IllegalArgumentException("Too many accessory definitions (max 256)");
                if (result.values().stream().anyMatch(old -> !old.id().equals(id) && old.item().equals(def.item())))
                    throw new IllegalArgumentException("Duplicate item; override the original definition ID instead");
                result.put(id, def);
            } catch (RuntimeException error) {
                // A malformed edit must not destroy the last working custom definition.
                if (server.ids.containsKey(id)) result.put(id, server.ids.get(id));
                LOGGER.error("Ignoring cat accessory {}: {}", id, error.getMessage());
            }
        });
        server = snapshot(result, server.revision + 1);
        LOGGER.info("Loaded {} cat accessory definitions (schema 1)", result.size());
    }

    public static CatAccessoryDefinition find(ItemStack stack, boolean clientSide) {
        return stack.isEmpty() ? null : (clientSide ? client : server).items.get(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }
    /** Non-boss registered accessories, including valid data-pack additions. */
    public static java.util.List<String> rewardItemIds() {
        return server.items.keySet().stream().filter(id -> !id.equals("minecraft:air")
                && BuiltInRegistries.ITEM.containsKey(ResourceLocation.tryParse(id))
                && !CatAccessoryRarity.bossOnly(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id)))))
                .sorted().toList();
    }
    public static long revision() { return server.revision; }
    public static long revision(boolean clientSide) { return clientSide ? client.revision : server.revision; }
    public static CompoundTag networkData() {
        CompoundTag tag = new CompoundTag();
        ListTag entries = new ListTag();
        server.ids.values().forEach(def -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", def.id());
            entry.putString("Json", def.toJson().toString());
            entries.add(entry);
        });
        tag.put("Definitions", entries);
        return tag;
    }
    public static void receive(CompoundTag tag) {
        ListTag entries = tag.getList("Definitions", Tag.TAG_COMPOUND);
        if (entries.size() > MAX_DEFINITIONS) return;
        Map<String, CatAccessoryDefinition> result = new LinkedHashMap<>();
        try {
            for (int i = 0; i < entries.size(); i++) {
                CompoundTag entry = entries.getCompound(i);
                String json = entry.getString("Json");
                if (json.length() > 8192) throw new IllegalArgumentException("Oversize definition");
                CatAccessoryDefinition def = CatAccessoryDefinition.parse(entry.getString("Id"),
                        JsonParser.parseString(json).getAsJsonObject());
                result.put(def.id(), def);
            }
            client = snapshot(result, client.revision + 1);
        } catch (RuntimeException error) {
            LOGGER.error("Invalid accessory registry sync: {}", error.getMessage());
        }
    }
    public static void resetServer() { server = snapshot(CatAccessoryItems.DEFAULTS, server.revision + 1); }
    public static void resetClient() { client = snapshot(CatAccessoryItems.DEFAULTS, client.revision + 1); }
    private static Snapshot snapshot(Map<String, CatAccessoryDefinition> definitions, long revision) {
        Map<String, CatAccessoryDefinition> items = new LinkedHashMap<>();
        definitions.values().forEach(def -> items.put(def.item(), def));
        return new Snapshot(Collections.unmodifiableMap(new LinkedHashMap<>(definitions)),
                Collections.unmodifiableMap(items), revision);
    }
}
