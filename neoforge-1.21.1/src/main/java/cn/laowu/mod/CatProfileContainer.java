package cn.laowu.mod;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;

/** One live container view for the four accessory and nine inventory slots. */
public final class CatProfileContainer extends SimpleContainer {
    private final Cat cat;
    private boolean loading;

    CatProfileContainer(Cat cat) {
        super(CatProfileData.SLOT_COUNT);
        this.cat = cat;
        loading = true;
        ListTag saved = cat.getPersistentData().getList(
                CatProfileData.ITEMS_TAG, Tag.TAG_COMPOUND);
        fromTag(saved, cat.registryAccess());
        loading = false;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (!loading) {
            cat.getPersistentData().put(CatProfileData.ITEMS_TAG,
                    createTag(cat.registryAccess()));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return cat.isAlive() && player.distanceToSqr(cat) <= 64.0D;
    }
}
