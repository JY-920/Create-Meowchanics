package cn.laowu.mod;

import cn.laowu.mod.create.BreedingBoxBlockEntity;
import cn.laowu.mod.create.BreedingBoxTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Read-only observation menu. Parent slots stay off-screen; result and food
 * slots are visible for status/tooltip rendering but reject every click.
 */
public final class BreedingBoxMenu extends AbstractContainerMenu {
    public static final int MACHINE_SLOTS = BreedingBoxBlockEntity.SLOT_COUNT;
    private static final int HIDDEN_SLOT = -10000;
    private static final int DISPLAY_SLOT_X = 183;
    private static final int CHILD_DISPLAY_Y = 165;
    private static final int FOOD_DISPLAY_Y = 185;

    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final ItemStackHandler machineInventory;

    public BreedingBoxMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, findBox(playerInventory, buffer.readBlockPos()));
    }

    public BreedingBoxMenu(int id, Inventory playerInventory, BreedingBoxBlockEntity box) {
        super(LaoWuMod.BREEDING_BOX_MENU.get(), id);
        if (box == null) {
            this.machineInventory = new ItemStackHandler(MACHINE_SLOTS);
            this.data = new SimpleContainerData(4);
            this.access = ContainerLevelAccess.NULL;
        } else {
            this.machineInventory = box.inventory();
            this.data = box.menuData();
            this.access = ContainerLevelAccess.create(box.getLevel(), box.getBlockPos());
        }

        addDataSlots(data);
        addMachineSlots();
    }

    private static BreedingBoxBlockEntity findBox(Inventory inventory, BlockPos pos) {
        return inventory.player.level().getBlockEntity(pos) instanceof BreedingBoxBlockEntity box
                ? box : null;
    }

    private void addMachineSlots() {
        for (int slot = 0; slot < MACHINE_SLOTS; slot++) {
            int x = slot == BreedingBoxBlockEntity.CHILD_SLOT
                    || slot == BreedingBoxBlockEntity.FOOD_SLOT
                    ? DISPLAY_SLOT_X : HIDDEN_SLOT;
            int y = slot == BreedingBoxBlockEntity.CHILD_SLOT ? CHILD_DISPLAY_Y
                    : slot == BreedingBoxBlockEntity.FOOD_SLOT ? FOOD_DISPLAY_Y : HIDDEN_SLOT;
            addSlot(new SlotItemHandler(machineInventory, slot, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }
    }

    public ItemStack father() {
        return machineInventory.getStackInSlot(BreedingBoxBlockEntity.FATHER_SLOT);
    }

    public ItemStack mother() {
        return machineInventory.getStackInSlot(BreedingBoxBlockEntity.MOTHER_SLOT);
    }

    public ItemStack child() {
        return machineInventory.getStackInSlot(BreedingBoxBlockEntity.CHILD_SLOT);
    }

    public ItemStack food() {
        return machineInventory.getStackInSlot(BreedingBoxBlockEntity.FOOD_SLOT);
    }

    public int progress() {
        return data.get(0);
    }

    public int duration() {
        return Math.max(1, data.get(1));
    }

    public BreedingBoxTier tier() {
        return BreedingBoxTier.byOrdinal(data.get(2));
    }

    public float effectiveMutationPercent() {
        return data.get(3) / 100.0F;
    }

    public int progressWidth(int width) {
        return Math.min(width, progress() * width / duration());
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) ->
                level.getBlockEntity(pos) instanceof BreedingBoxBlockEntity
                        && player.distanceToSqr(pos.getX() + 0.5D,
                        pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D, false);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
