package cn.laowu.mod.client;

import cn.laowu.mod.ServerConfig;
import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.CatSuitSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/** Client-only configuration mirror and optional JEI callback (no hard JEI dependency). */
public final class ClientWorldSettings {
    public static Runnable recipeRefresh = () -> {};
    private static CompoundTag values;
    public static CompoundTag values() { return values == null ? null : values.copy(); }
    /** Draft-only reset: server permissions and global locks still apply when saving. */
    public static java.util.Map<CatOutfitType, Double> editableCareerDefaults(CompoundTag draft) {
        var defaults = new java.util.EnumMap<CatOutfitType, Double>(CatOutfitType.class);
        if (draft == null || !draft.getBoolean("can_edit")) return defaults;
        CompoundTag locks = draft.getCompound(ServerConfig.LOCKS_TAG);
        for (CatOutfitType outfit : ServerConfig.CAREERS) {
            if (!locks.getBoolean(ServerConfig.careerDamageKey(outfit)))
                defaults.put(outfit, outfit.defaultDamageCoefficient());
        }
        return defaults;
    }
    public static java.util.Map<CatSuitSetting, Double> editableSuitDefaults(CompoundTag draft, CatOutfitType outfit) {
        var defaults = new java.util.EnumMap<CatSuitSetting, Double>(CatSuitSetting.class);
        if (draft == null || !draft.getBoolean("can_edit")) return defaults;
        CompoundTag locks = draft.getCompound(ServerConfig.LOCKS_TAG);
        for (CatSuitSetting setting : CatSuitSetting.values())
            if (setting.appliesTo(outfit) && !locks.getBoolean(setting.lockKey(outfit)))
                defaults.put(setting, setting.defaultValue(outfit));
        return defaults;
    }
    public static void reset() {
        values = null;
        ServerConfig.clearMirror();
    }
    public static void receive(CompoundTag tag) {
        if (!ServerConfig.valid(tag)) return;
        values = tag.copy();
        // An integrated server shares the spec with its client. Never write an
        // older queued reply back over that server's newer authoritative values.
        if (Minecraft.getInstance().getSingleplayerServer() == null)
            ServerConfig.receiveMirror(tag);
        recipeRefresh.run();
        if (Minecraft.getInstance().screen instanceof WorldSettingsScreen screen) screen.receive(tag);
    }
    private ClientWorldSettings() {}
}
