package cn.laowu.mod;

import com.simibubi.create.content.logistics.box.PackageItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A frogport-style, package-only view of the first 18 cat chest slots. */
public final class CatPackageMenu extends AbstractContainerMenu {
    public static final int PACKAGE_SLOTS = 18;

    private final Container catInventory;
    private final int catId;
    private final String address;

    /** Forge client factory; the cat id is supplied by NetworkHooks.openScreen. */
    public CatPackageMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        super(LaoWuMod.CAT_PACKAGE_MENU.get(), containerId);
        this.catId = buffer.readVarInt();
        this.address = buffer.readUtf(CatChestData.MAX_ADDRESS_LENGTH);
        Entity entity = playerInventory.player.level().getEntity(catId);
        this.catInventory = entity instanceof Cat cat
                ? CatChestData.openContainer(cat)
                : new SimpleContainer(CatChestContainer.SLOT_COUNT);
        initializeSlots(playerInventory);
    }

    public CatPackageMenu(int containerId, Inventory playerInventory, Cat cat) {
        super(LaoWuMod.CAT_PACKAGE_MENU.get(), containerId);
        this.catInventory = openServerInventory(playerInventory, cat);
        this.catId = cat.getId();
        this.address = CatChestData.getAddress(cat);
        initializeSlots(playerInventory);
    }

    private void initializeSlots(Inventory playerInventory) {
        catInventory.startOpen(playerInventory.player);

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 9; column++) {
                int slotIndex = row * 9 + column;
                addSlot(new Slot(catInventory, slotIndex, 35 + column * 18, 33 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return PackageItem.isPackage(stack);
                    }
                });
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, 9 + row * 9 + column,
                        34 + column * 18, 132 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 34 + column * 18, 190));
        }
    }

    public int getCatId() { return catId; }

    public String getAddress() { return address; }

    private static Container openServerInventory(Inventory playerInventory, Cat cat) {
        CatChestContainer container = CatChestData.openContainer(cat);
        boolean migrated = false;
        for (int slot = PACKAGE_SLOTS; slot < container.getContainerSize(); slot++) {
            ItemStack oldStack = container.removeItemNoUpdate(slot);
            if (oldStack.isEmpty()) continue;
            migrated = true;
            playerInventory.add(oldStack);
            if (!oldStack.isEmpty()) playerInventory.player.drop(oldStack, false);
        }
        if (migrated) container.setChanged();
        return container;
    }

    @Override
    public boolean stillValid(Player player) {
        return catInventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PACKAGE_SLOTS) {
            if (!moveItemStackTo(stack, PACKAGE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!PackageItem.isPackage(stack)
                    || !moveItemStackTo(stack, 0, PACKAGE_SLOTS, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        catInventory.stopOpen(player);
    }
}
