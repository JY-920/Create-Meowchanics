package cn.laowu.mod;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
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
        // Vanilla SimpleContainer loses slot indices. Migrate old packed inventories without dropping items.
        for (int i = 0; i < saved.size(); i++) {
            CompoundTag entry = saved.getCompound(i);
            int slot = entry.contains("Slot", Tag.TAG_ANY_NUMERIC)
                    ? entry.getByte("Slot") & 255 : (i + CatProfileData.ACCESSORY_SLOTS) % getContainerSize();
            if (slot < getContainerSize()) super.setItem(slot, ItemStack.parseOptional(cat.registryAccess(), entry));
        }
        loading = false;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (!loading) {
            ListTag saved = new ListTag();
            for (int slot = 0; slot < getContainerSize(); slot++) {
                ItemStack stack = getItem(slot);
                if (stack.isEmpty()) continue;
                CompoundTag entry = (CompoundTag) stack.save(cat.registryAccess());
                entry.putByte("Slot", (byte) slot);
                saved.add(entry);
            }
            cat.getPersistentData().put(CatProfileData.ITEMS_TAG, saved);
            cn.laowu.mod.accessory.CatAccessories.equipmentChanged(cat);
            CatMusicRecords.inventoryChanged(cat);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return cat.isAlive() && player.distanceToSqr(cat) <= 64.0D;
    }
}
