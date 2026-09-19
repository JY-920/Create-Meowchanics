package cn.laowu.mod;

import cn.laowu.mod.network.ModNetwork;
import cn.laowu.mod.network.MusicRecordPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
import java.util.WeakHashMap;

/** Loop only the existing nine general inventory slots. Never consume records or touch chest storage. */
public final class CatMusicRecords {
    private static final String SLOT_TAG = "LaoWuMusicRecordSlot";
    private static final Map<Cat, Session> SESSIONS = new WeakHashMap<>();
    // Track switches can happen twice in one server tick while inventory is edited.
    private static long sequenceCounter;
    private static final class Session {
        int slot;
        long sequence, until, nextSync;
        CatMusicSongs.Song song;
        ItemStack stack;
    }
    public static boolean available(Cat cat) {
        return cat.isAlive() && !cat.isRemoved() && cat.isTame()
                && CatClothesData.getOutfit(cat) == CatOutfitType.MUSIC && !CatPoseData.isPancake(cat)
                && !cat.isInWaterOrBubble() && !cat.isInLava()
                && (!cat.isNoAi() || CatProfileData.isBeingViewed(cat));
    }
    public static int nextSlot(Cat cat, int after) {
        var inventory = CatProfileData.openContainer(cat);
        for (int offset = 1; offset <= CatProfileData.INVENTORY_SLOTS; offset++) {
            int slot = Math.floorMod(after + offset, CatProfileData.INVENTORY_SLOTS);
            if (CatMusicSongs.read(cat, inventory.getItem(CatProfileData.ACCESSORY_SLOTS + slot)) != null) return slot;
        }
        return -1;
    }
    public static void inventoryChanged(Cat cat) { if (!cat.level().isClientSide) tick(cat); }
    public static void tick(Cat cat) {
        if (cat.level().isClientSide) return;
        if (!available(cat)) { stop(cat); return; }
        long now = cat.level().getGameTime();
        Session state = SESSIONS.get(cat);
        var inventory = CatProfileData.openContainer(cat);
        boolean changed = state != null && !ItemStack.matches(state.stack,
                inventory.getItem(CatProfileData.ACCESSORY_SLOTS + state.slot));
        if (state == null || now >= state.until || changed) {
            int after = state == null ? cat.getPersistentData().getInt(SLOT_TAG) - 1 : state.slot;
            int slot = nextSlot(cat, after);
            if (slot < 0) { stop(cat); return; }
            // A removed/replaced record advances in order. Empty slots and accessory slots never play.
            state = new Session(); state.slot = slot; state.sequence = ++sequenceCounter;
            state.stack = inventory.getItem(CatProfileData.ACCESSORY_SLOTS + slot).copy();
            state.song = CatMusicSongs.read(cat, state.stack);
            state.until = now + state.song.ticks(); state.nextSync = now + 20;
            SESSIONS.put(cat, state); cat.getPersistentData().putInt(SLOT_TAG, slot);
            ModNetwork.musicRecord(cat, null, packet(cat, state));
        } else if (now >= state.nextSync) {
            state.nextSync = now + 20;
            ModNetwork.musicRecord(cat, null, packet(cat, state));
        }
    }
    public static int playingSlot(Cat cat) { Session state = SESSIONS.get(cat); return state == null ? -1 : state.slot; }
    public static void stop(Cat cat) {
        if (cat.level().isClientSide || SESSIONS.remove(cat) == null) return;
        ModNetwork.musicRecord(cat, null, new MusicRecordPacket(cat.getId(), cat.getUUID(), "", 0, 0));
    }
    private static MusicRecordPacket packet(Cat cat, Session state) {
        return new MusicRecordPacket(cat.getId(), cat.getUUID(), state.song.sound().toString(),
                state.sequence, (int)Math.max(0, Math.min(Integer.MAX_VALUE, state.until - cat.level().getGameTime())),
                state.song.networkUrl(), state.song.title());
    }
    public static void syncTo(Cat cat, ServerPlayer player) {
        Session state = SESSIONS.get(cat);
        if (state != null) ModNetwork.musicRecord(cat, player, packet(cat, state));
    }
    private CatMusicRecords() {}
}
