package cn.laowu.mod;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;

/** A live inventory view backed by the owning cat's persistent Forge data. */
public final class CatChestContainer extends SimpleContainer {
    public static final int SLOT_COUNT = 27;

    private final Cat cat;
    private boolean loading;

    public CatChestContainer(Cat cat) {
        super(SLOT_COUNT);
        this.cat = cat;
        loading = true;
        ListTag saved = cat.getPersistentData().getList(CatChestData.ITEMS_TAG, Tag.TAG_COMPOUND);
        fromTag(saved);
        loading = false;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (!loading) {
            cat.getPersistentData().put(CatChestData.ITEMS_TAG, createTag());
            if (!cat.level().isClientSide && CatChestData.hasChest(cat)) {
                CatLogisticsBehavior.onInventoryChanged(cat);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return cat.isAlive() && CatChestData.hasInventory(cat)
                && player.distanceToSqr(cat) <= 64.0D;
    }
}
