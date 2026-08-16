package cn.laowu.mod;

import cn.laowu.mod.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Cat;

/** Persistent and client-synchronised marker for the cat's optional outfit. */
public final class CatClothesData {
    public static final String EQUIPPED_TAG = "LaoWuHasClothes";
    public static final String OUTFIT_TAG = "LaoWuCatOutfit";

    public static boolean isEquipped(Cat cat) {
        return getOutfit(cat) != CatOutfitType.NONE;
    }

    /** Old worlds only contain the boolean marker, which always meant Terminator. */
    public static CatOutfitType getOutfit(Cat cat) {
        String saved = cat.getPersistentData().getString(OUTFIT_TAG);
        CatOutfitType type = CatOutfitType.byId(saved);
        if (type != CatOutfitType.NONE) return type;
        return cat.getPersistentData().getBoolean(EQUIPPED_TAG)
                ? CatOutfitType.TERMINATOR : CatOutfitType.NONE;
    }

    public static void equip(Cat cat) {
        equip(cat, CatOutfitType.TERMINATOR);
    }

    public static void equip(Cat cat, CatOutfitType outfit) {
        if (outfit == CatOutfitType.NONE) {
            unequip(cat);
            return;
        }
        cat.getPersistentData().putBoolean(EQUIPPED_TAG, true);
        cat.getPersistentData().putString(OUTFIT_TAG, outfit.id());
        if (outfit == CatOutfitType.TRANSPORT) CatChestData.install(cat);
        CareerCatBehavior.onOutfitChanged(cat, true);
        ModNetwork.syncCatClothesToTracking(cat);
    }

    public static void unequip(Cat cat) {
        boolean transport = getOutfit(cat) == CatOutfitType.TRANSPORT;
        if (transport) CatLogisticsBehavior.cancelForPancake(cat);
        cat.getPersistentData().remove(EQUIPPED_TAG);
        cat.getPersistentData().remove(OUTFIT_TAG);
        if (transport) CatChestData.uninstall(cat);
        CareerCatBehavior.onOutfitChanged(cat, false);
        ModNetwork.syncCatClothesToTracking(cat);
    }

    public static void syncToPlayer(ServerPlayer player, Cat cat) {
        ModNetwork.syncCatClothesToPlayer(player, cat);
    }

    private CatClothesData() {
    }
}
