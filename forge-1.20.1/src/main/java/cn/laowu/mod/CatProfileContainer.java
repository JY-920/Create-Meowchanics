package cn.laowu.mod;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
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
        for (int index = 0; index < saved.size(); index++) {
            CompoundTag entry = saved.getCompound(index);
            int slot = entry.getByte("Slot") & 255;
            if (slot >= 0 && slot < getContainerSize()) {
                super.setItem(slot, net.minecraft.world.item.ItemStack.of(entry));
            }
        }
        loading = false;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (!loading) {
            ListTag saved = new ListTag();
            for (int slot = 0; slot < getContainerSize(); slot++) {
                var stack = getItem(slot);
                if (stack.isEmpty()) continue;
                CompoundTag entry = stack.save(new CompoundTag());
                entry.putByte("Slot", (byte) slot);
                saved.add(entry);
            }
            cat.getPersistentData().put(CatProfileData.ITEMS_TAG, saved);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return cat.isAlive() && player.distanceToSqr(cat) <= 64.0D;
    }
}
