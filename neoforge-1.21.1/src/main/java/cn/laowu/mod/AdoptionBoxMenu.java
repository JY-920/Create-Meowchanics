package cn.laowu.mod;

import cn.laowu.mod.create.AdoptionBoxBlockEntity;
import cn.laowu.mod.item.CatPancakeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Interactive nine-cat input and nine-stack extraction-only adoption menu. */
public final class AdoptionBoxMenu extends AbstractContainerMenu {
    public static final int MACHINE_SLOTS = AdoptionBoxBlockEntity.SLOT_COUNT;
    public static final int PLAYER_PANEL_Y = 94;
    public static final int SCREEN_WIDTH = 176;
    public static final int SCREEN_HEIGHT = PLAYER_PANEL_Y + 108;

    private final ItemStackHandler machineInventory;
    private final ContainerLevelAccess access;

    public AdoptionBoxMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, findBox(playerInventory, buffer.readBlockPos()));
    }

    public AdoptionBoxMenu(int id, Inventory playerInventory, AdoptionBoxBlockEntity box) {
        super(LaoWuMod.ADOPTION_BOX_MENU.get(), id);
        if (box == null) {
            machineInventory = new ItemStackHandler(MACHINE_SLOTS);
            access = ContainerLevelAccess.NULL;
        } else {
            machineInventory = box.inventory();
            access = ContainerLevelAccess.create(box.getLevel(), box.getBlockPos());
        }
        addMachineSlots();
        addPlayerSlots(playerInventory);
    }

    private static AdoptionBoxBlockEntity findBox(Inventory inventory, BlockPos pos) {
        return inventory.player.level().getBlockEntity(pos)
                instanceof AdoptionBoxBlockEntity box ? box : null;
    }

    private void addMachineSlots() {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int slot = row * 3 + column;
                addSlot(new SlotItemHandler(machineInventory, slot,
                        28 + column * 20, 5 + row * 20) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.getItem() instanceof CatPancakeItem;
                    }
                });
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int slot = AdoptionBoxBlockEntity.OUTPUT_START + row * 3 + column;
                addSlot(new SlotItemHandler(machineInventory, slot,
                        93 + column * 20, 11 + row * 20) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }
    }

    private void addPlayerSlots(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new net.minecraft.world.inventory.Slot(playerInventory,
                        9 + row * 9 + column, 8 + column * 18,
                        PLAYER_PANEL_Y + 18 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new net.minecraft.world.inventory.Slot(playerInventory, column,
                    8 + column * 18, PLAYER_PANEL_Y + 76));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) ->
                level.getBlockEntity(pos) instanceof AdoptionBoxBlockEntity
                        && player.distanceToSqr(pos.getX() + 0.5D,
                        pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D, false);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!(stack.getItem() instanceof CatPancakeItem)
                || !moveItemStackTo(stack, AdoptionBoxBlockEntity.INPUT_START,
                AdoptionBoxBlockEntity.INPUT_START + AdoptionBoxBlockEntity.INPUT_COUNT,
                false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
