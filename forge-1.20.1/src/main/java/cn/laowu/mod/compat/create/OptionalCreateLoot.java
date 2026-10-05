package cn.laowu.mod.compat.create;

import com.google.gson.JsonElement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Forge 1.20.1 has no root mod-loaded condition for loot tables. */
public final class OptionalCreateLoot {
    public static boolean containsMissingItem(JsonElement value) {
        if (value.isJsonArray()) {
            for (var element : value.getAsJsonArray()) if (containsMissingItem(element)) return true;
        } else if (value.isJsonObject()) {
            var object = value.getAsJsonObject();
            if (object.has("type") && object.has("name") && object.get("type").isJsonPrimitive()
                    && object.get("type").getAsString().equals("minecraft:item")) {
                var id = ResourceLocation.tryParse(object.get("name").getAsString());
                if (id != null && (id.getNamespace().equals("laowu") || id.getNamespace().equals("create"))
                        && !BuiltInRegistries.ITEM.containsKey(id)) return true;
            }
            for (var entry : object.entrySet()) if (containsMissingItem(entry.getValue())) return true;
        }
        return false;
    }
    private OptionalCreateLoot() {}
}
