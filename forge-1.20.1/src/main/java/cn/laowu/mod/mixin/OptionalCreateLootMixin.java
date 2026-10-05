package cn.laowu.mod.mixin;

import cn.laowu.mod.compat.create.CreateIntegration;
import cn.laowu.mod.compat.create.OptionalCreateLoot;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;

@Mixin(SimpleJsonResourceReloadListener.class)
public abstract class OptionalCreateLootMixin {
    @Inject(method="scanDirectory", at=@At("RETURN"))
    private static void laowu$skipUnregisteredMachineLoot(ResourceManager resources, String directory, Gson gson,
                                                         Map<ResourceLocation, JsonElement> entries, CallbackInfo ci) {
        if (CreateIntegration.isLoaded() || !directory.equals("loot_tables")) return;
        // Only our unavailable machine tables: never alter other mods or valid cat loot.
        entries.entrySet().removeIf(entry -> entry.getKey().getNamespace().equals("laowu")
                && OptionalCreateLoot.containsMissingItem(entry.getValue()));
    }
}
