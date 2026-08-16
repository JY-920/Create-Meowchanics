package cn.laowu.mod;

import cn.laowu.mod.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.GameRules;

public final class CatChestData {
    public static final String HAS_CHEST_TAG = "LaoWuHasChest";
    public static final String ITEMS_TAG = "LaoWuChestItems";
    public static final String ADDRESS_TAG = "LaoWuPackageAddress";
    public static final int MAX_ADDRESS_LENGTH = 25;

    public static boolean hasChest(Cat cat) {
        // The transport outfit is now the sole logistics identity. The old
        // vault-installed boolean is retained only as migration data and no
        // longer turns an ordinary cat into a container by itself.
        return CatClothesData.getOutfit(cat) == CatOutfitType.TRANSPORT;
    }

    /** Transport cats use the package UI; flight cats expose the same storage as a normal chest. */
    public static boolean hasInventory(Cat cat) {
        CatOutfitType outfit = CatClothesData.getOutfit(cat);
        return outfit == CatOutfitType.TRANSPORT || outfit == CatOutfitType.FLIGHT;
    }

    public static void install(Cat cat) {
        cat.getPersistentData().putBoolean(HAS_CHEST_TAG, true);
        ModNetwork.syncCatChestToTracking(cat);
    }

    public static void uninstall(Cat cat) {
        cat.getPersistentData().remove(HAS_CHEST_TAG);
        ModNetwork.syncCatChestToTracking(cat);
    }

    public static CatChestContainer openContainer(Cat cat) {
        return new CatChestContainer(cat);
    }

    public static String getAddress(Cat cat) {
        return cat.getPersistentData().getString(ADDRESS_TAG);
    }

    public static void setAddress(Cat cat, String address) {
        String cleaned = address == null ? "" : address.trim();
        if (cleaned.length() > MAX_ADDRESS_LENGTH) {
            cleaned = cleaned.substring(0, MAX_ADDRESS_LENGTH);
        }
        cat.getPersistentData().putString(ADDRESS_TAG, cleaned);
    }

    public static void syncToPlayer(ServerPlayer player, Cat cat) {
        ModNetwork.syncCatChestToPlayer(player, cat);
    }

    public static void dropOnDeath(Cat cat) {
        if (!hasInventory(cat) || cat.level().isClientSide
                || !cat.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;

        CatChestContainer inventory = new CatChestContainer(cat);
        Containers.dropContents(cat.level(), cat, inventory);
        cat.getPersistentData().remove(ITEMS_TAG);
        cat.getPersistentData().remove(ADDRESS_TAG);
        cat.getPersistentData().remove(HAS_CHEST_TAG);
    }

    private CatChestData() {}
}
