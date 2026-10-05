package cn.laowu.mod.compat.create;

import net.neoforged.fml.loading.FMLLoader;

/** Loader-only presence check, safe before optional integration classes are resolved. */
public final class CreateIntegration {
    public static boolean isLoaded() {
        var mods = FMLLoader.getLoadingModList();
        return mods != null && mods.getMods().stream().anyMatch(mod -> "create".equals(mod.getModId()));
    }

    private CreateIntegration() {}

    public static <T extends net.minecraft.world.level.block.Block> net.neoforged.neoforge.registries.DeferredBlock<T>
    registerBlock(net.neoforged.neoforge.registries.DeferredRegister.Blocks registry,
                  String name, java.util.function.Supplier<? extends T> factory) {
        if (isLoaded()) return registry.register(name, factory);
        return net.neoforged.neoforge.registries.DeferredBlock.createBlock(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("laowu", name));
    }

    public static <T extends net.minecraft.world.item.Item> net.neoforged.neoforge.registries.DeferredItem<T>
    registerItem(net.neoforged.neoforge.registries.DeferredRegister.Items registry,
                 String name, java.util.function.Supplier<? extends T> factory) {
        if (isLoaded()) return registry.register(name, factory);
        return net.neoforged.neoforge.registries.DeferredItem.createItem(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("laowu", name));
    }

    public static <R, T extends R> net.neoforged.neoforge.registries.DeferredHolder<R, T>
    register(net.neoforged.neoforge.registries.DeferredRegister<R> registry, String name,
             java.util.function.Supplier<? extends T> factory) {
        if (isLoaded()) return registry.register(name, factory);
        return net.neoforged.neoforge.registries.DeferredHolder.create(
                registry.getRegistryKey(),
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("laowu", name));
    }
}
