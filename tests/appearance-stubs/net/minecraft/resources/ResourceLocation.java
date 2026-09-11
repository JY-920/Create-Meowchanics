package net.minecraft.resources;
public record ResourceLocation(String namespace, String path) {
    public static ResourceLocation fromNamespaceAndPath(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
}
