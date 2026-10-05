package cn.laowu.mod.compat.create;

import net.minecraftforge.fml.loading.FMLLoader;

/** Loader-only presence check, safe before optional integration classes are resolved. */
public final class CreateIntegration {
    public static boolean isLoaded() {
        var mods = FMLLoader.getLoadingModList();
        return mods != null && mods.getMods().stream().anyMatch(mod -> "create".equals(mod.getModId()));
    }

    private CreateIntegration() {}

    public static <T extends net.minecraft.world.level.block.Block> net.minecraftforge.registries.RegistryObject<T>
    registerBlock(net.minecraftforge.registries.DeferredRegister<net.minecraft.world.level.block.Block> registry,
                  String name, java.util.function.Supplier<? extends T> factory) {
        return register(registry, name, factory);
    }

    public static <T extends net.minecraft.world.item.Item> net.minecraftforge.registries.RegistryObject<T>
    registerItem(net.minecraftforge.registries.DeferredRegister<net.minecraft.world.item.Item> registry,
                 String name, java.util.function.Supplier<? extends T> factory) {
        return register(registry, name, factory);
    }

    @SuppressWarnings("unchecked")
    public static <R, T extends R> net.minecraftforge.registries.RegistryObject<T>
    register(net.minecraftforge.registries.DeferredRegister<R> registry, String name,
             java.util.function.Supplier<? extends T> factory) {
        if (isLoaded()) return registry.register(name, factory);
        return (net.minecraftforge.registries.RegistryObject<T>)(Object)
                net.minecraftforge.registries.RegistryObject.create(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("laowu", name),
                        registry.getRegistryKey(), "laowu");
    }
}
