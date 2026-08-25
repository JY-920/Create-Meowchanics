package cn.laowu.mod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.GameRules;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/** Persistent general-purpose storage owned by every living cat. */
public final class CatProfileData {
    public static final String ITEMS_TAG = "LaoWuProfileItems";
    public static final int ACCESSORY_SLOTS = 4;
    public static final int INVENTORY_SLOTS = 9;
    public static final int SLOT_COUNT = ACCESSORY_SLOTS + INVENTORY_SLOTS;
    public static final int MAX_NAME_LENGTH = 50;
    private static final String VIEW_LOCK_TAG = "LaoWuProfileViewLock";
    private static final String PREVIOUS_NO_AI_TAG = "LaoWuProfilePreviousNoAi";

    /*
     * All simultaneous server menus for one UUID share a weakly-held live
     * container instead of loading stale NBT snapshots. Client prediction is
     * deliberately kept outside this cache.
     */
    private static final Map<UUID, WeakReference<CatProfileContainer>> OPEN_CONTAINERS =
            new HashMap<>();
    private static final Map<UUID, Integer> ACTIVE_VIEWERS = new HashMap<>();

    public static CatProfileContainer openContainer(Cat cat) {
        // Entity.equals() compares only the runtime integer id. Never put a
        // client cat into the server cache: in an integrated game its id is the
        // same as the server cat and client click prediction would mutate the
        // authoritative inventory before the click packet arrived.
        if (cat.level().isClientSide) return new CatProfileContainer(cat);
        OPEN_CONTAINERS.entrySet().removeIf(entry -> entry.getValue().get() == null);
        UUID catUuid = cat.getUUID();
        WeakReference<CatProfileContainer> reference = OPEN_CONTAINERS.get(catUuid);
        CatProfileContainer existing = reference == null ? null : reference.get();
        if (existing != null) return existing;
        CatProfileContainer created = new CatProfileContainer(cat);
        OPEN_CONTAINERS.put(catUuid, new WeakReference<>(created));
        return created;
    }

    public static boolean canOpen(ServerPlayer player, Cat cat) {
        return cat.isAlive() && player.distanceToSqr(cat) <= 64.0D;
    }

    public static void setName(Cat cat, String requestedName) {
        String cleaned = cleanName(requestedName);
        cat.setCustomName(cleaned.isEmpty()
                ? null : net.minecraft.network.chat.Component.literal(cleaned));
    }

    public static String editableName(Cat cat) {
        return cleanName(cat.hasCustomName() ? cat.getCustomName().getString() : "");
    }

    private static String cleanName(String requestedName) {
        String cleaned = requestedName == null ? "" : requestedName.trim();
        return cleaned.length() <= MAX_NAME_LENGTH
                ? cleaned : cleaned.substring(0, MAX_NAME_LENGTH);
    }

    /**
     * Pauses autonomous movement while at least one player is inspecting the
     * cat. The saved marker lets a cat recover its original NoAI state after a
     * server interruption instead of remaining frozen forever.
     */
    public static void beginViewing(Cat cat) {
        if (cat.level().isClientSide) return;
        UUID catUuid = cat.getUUID();
        int viewers = ACTIVE_VIEWERS.getOrDefault(catUuid, 0);
        if (viewers == 0) {
            if (cat.getPersistentData().getBoolean(VIEW_LOCK_TAG)) {
                restoreViewLock(cat);
            }
            cat.getPersistentData().putBoolean(PREVIOUS_NO_AI_TAG, cat.isNoAi());
            cat.getPersistentData().putBoolean(VIEW_LOCK_TAG, true);
            cat.getNavigation().stop();
            cat.setDeltaMovement(0.0D, cat.getDeltaMovement().y, 0.0D);
            cat.setNoAi(true);
        }
        ACTIVE_VIEWERS.put(catUuid, viewers + 1);
    }

    public static void endViewing(Cat cat) {
        if (cat.level().isClientSide) return;
        UUID catUuid = cat.getUUID();
        Integer viewers = ACTIVE_VIEWERS.get(catUuid);
        if (viewers == null) return;
        if (viewers > 1) {
            ACTIVE_VIEWERS.put(catUuid, viewers - 1);
            return;
        }
        ACTIVE_VIEWERS.remove(catUuid);
        restoreViewLock(cat);
    }

    public static boolean isBeingViewed(Cat cat) {
        return ACTIVE_VIEWERS.containsKey(cat.getUUID());
    }

    /** Repairs a persisted lock left by a crash or interrupted menu session. */
    public static void recoverInterruptedViewLock(Cat cat) {
        if (!cat.level().isClientSide && !ACTIVE_VIEWERS.containsKey(cat.getUUID())
                && cat.getPersistentData().getBoolean(VIEW_LOCK_TAG)) {
            restoreViewLock(cat);
        }
    }

    private static void restoreViewLock(Cat cat) {
        boolean previousNoAi = cat.getPersistentData().getBoolean(PREVIOUS_NO_AI_TAG);
        cat.getPersistentData().remove(VIEW_LOCK_TAG);
        cat.getPersistentData().remove(PREVIOUS_NO_AI_TAG);
        cat.setNoAi(previousNoAi);
        cat.getNavigation().stop();
    }

    public static void dropOnDeath(Cat cat) {
        OPEN_CONTAINERS.remove(cat.getUUID());
        ACTIVE_VIEWERS.remove(cat.getUUID());
        cat.getPersistentData().remove(VIEW_LOCK_TAG);
        cat.getPersistentData().remove(PREVIOUS_NO_AI_TAG);
        if (cat.level().isClientSide
                || !cat.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;
        CatProfileContainer inventory = new CatProfileContainer(cat);
        Containers.dropContents(cat.level(), cat, inventory);
        cat.getPersistentData().remove(ITEMS_TAG);
    }

    private CatProfileData() {}
}
