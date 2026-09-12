package cn.laowu.mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import cn.laowu.mod.item.CatLaserPointerItem;

/** Per-owner and per-world; survives death, dimensions, logout and replacing the pointer. */
public final class CatCombatPreferences extends SavedData {
    public static final String FILE_ID = "laowu_cat_combat";
    private final Set<UUID> aggressiveOwners = new HashSet<>();

    public static CatCombatPreferences get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CatCombatPreferences::new, (tag, lookup) -> load(tag), null), FILE_ID);
    }

    public boolean aggressive(UUID owner) { return owner != null && aggressiveOwners.contains(owner); }

    public void setAggressive(UUID owner, boolean enabled) {
        if (owner == null) return;
        boolean changed = enabled ? aggressiveOwners.add(owner) : aggressiveOwners.remove(owner);
        if (changed) setDirty();
    }

    public static CatCombatPreferences load(CompoundTag tag) {
        CatCombatPreferences data = new CatCombatPreferences();
        ListTag entries = tag.getList("AggressiveOwners", Tag.TAG_INT_ARRAY);
        for (Tag entry : entries) {
            try { data.aggressiveOwners.add(NbtUtils.loadUUID(entry)); }
            catch (IllegalArgumentException ignored) { /* Preserve other valid owners in old/corrupt data. */ }
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        ListTag entries = new ListTag();
        aggressiveOwners.stream().sorted().forEach(owner -> entries.add(NbtUtils.createUUID(owner)));
        tag.put("AggressiveOwners", entries);
        return tag;
    }

    /** -1 queries; 0/1 change only the authenticated sender's own cats. */
    public static void request(ServerPlayer player, int action) {
        if (action < -1 || action > 1 || !player.isAlive()
                || !(player.getMainHandItem().getItem() instanceof CatLaserPointerItem
                || player.getOffhandItem().getItem() instanceof CatLaserPointerItem)) return;
        CatCombatPreferences data = get(player.server);
        if (action >= 0) data.setAggressive(player.getUUID(), action == 1);
        cn.laowu.mod.network.ModNetwork.sendLaserSettings(player, data.aggressive(player.getUUID()));
    }
}
