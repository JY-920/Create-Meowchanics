package cn.laowu.mod.compat.create;

import net.neoforged.fml.loading.FMLLoader;

/** Loader-only presence check, safe before optional integration classes are resolved. */
public final class CreateIntegration {
    public static boolean isLoaded() {
        var mods = FMLLoader.getLoadingModList();
        return mods != null && mods.getMods().stream().anyMatch(mod -> "create".equals(mod.getModId()));
    }

    private CreateIntegration() {}
}

