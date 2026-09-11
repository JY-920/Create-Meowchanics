package net.minecraft.network.chat;
public record Component(String key, Object[] arguments) {
    public static Component translatable(String key, Object... arguments) {
        return new Component(key, arguments);
    }
    public static Component empty() { return new Component("", new Object[0]); }
}
