package cn.laowu.mod;

import cn.laowu.mod.create.WishAdoptionBoxBlockEntity;
import cn.laowu.mod.create.WishAdoptionOffer;
import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.item.CatPancakeItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import java.util.ArrayList;

/** Vanilla menu packets synchronize offers and validate lock actions; no client-supplied rewards. */
public final class WishAdoptionBoxMenu extends AbstractContainerMenu {
    public static final int MACHINE_SLOTS = 18, PLAYER_PANEL_Y = 172, SCREEN_WIDTH = 176, SCREEN_HEIGHT = 280;
    private static final int DATA_COUNT = 23;
    private final WishAdoptionBoxBlockEntity box;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final ItemStackHandler machineInventory;

    public WishAdoptionBoxMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, findBox(inventory, buffer));
    }
    private static WishAdoptionBoxBlockEntity findBox(Inventory inventory, FriendlyByteBuf buffer) {
        return inventory.player.level().getBlockEntity(buffer.readBlockPos())
                instanceof WishAdoptionBoxBlockEntity box ? box : null;
    }
    public WishAdoptionBoxMenu(int id, Inventory inventory, WishAdoptionBoxBlockEntity blockEntity) {
        super(LaoWuMod.WISH_ADOPTION_BOX_MENU.get(), id);
        // Client click prediction must never share a block entity's authoritative inventory.
        box = !inventory.player.level().isClientSide ? blockEntity : null;
        machineInventory = box == null ? new ItemStackHandler(MACHINE_SLOTS) : box.inventory();
        access = blockEntity == null ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        data = box == null ? new SimpleContainerData(DATA_COUNT) : new ContainerData() {
            @Override public int getCount() { return DATA_COUNT; }
            @Override public void set(int index, int value) {}
            @Override public int get(int index) {
                var offer = box.offer();
                if (index == 1) return box.locked() ? 1 : 0;
                if (offer == null) return 0;
                if (index == 0) return offer.maximum() ? 2 : 1;
                if (index == 2) return offer.conditions().size();
                int item = Item.getId(box.rewardPreview().getItem());
                if (index == 3) return item & 65535;
                if (index == 4) return item >>> 16;
                int row = (index - 5) / 3;
                if (row < 0 || row >= offer.conditions().size()) return 0;
                var c = offer.conditions().get(row);
                return switch ((index - 5) % 3) {
                    case 0 -> c.stat().ordinal() + 1;
                    case 1 -> c.min();
                    default -> c.max();
                };
            }
        };
        addDataSlots(data);
        for (int group = 0; group < 2; group++) {
            final boolean input = group == 0;
            for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++)
                addSlot(new SlotItemHandler(machineInventory, group * 9 + row * 3 + col,
                        (input ? 27 : 92) + col * 20, (input ? 91 : 97) + row * 20) {
                    @Override public boolean mayPlace(ItemStack stack) {
                        return input && stack.getItem() instanceof CatPancakeItem;
                    }
                    @Override public int getMaxStackSize(ItemStack stack) {
                        return input ? 1 : super.getMaxStackSize(stack);
                    }
                });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, PLAYER_PANEL_Y + 18 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, 8 + col * 18, PLAYER_PANEL_Y + 76));
    }

    public boolean locked() { return data.get(1) != 0; }
    public WishAdoptionOffer offer() {
        if (box != null) return box.offer();
        int count = data.get(2);
        if (data.get(0) == 0 || count < 1 || count > 6) return null;
        try {
            var rules = new ArrayList<WishAdoptionOffer.Condition>();
            for (int i = 0; i < count; i++)
                rules.add(new WishAdoptionOffer.Condition(CatStat.values()[data.get(5 + i * 3) - 1],
                        data.get(6 + i * 3), data.get(7 + i * 3)));
            int itemId = (data.get(3) & 65535) | (data.get(4) & 65535) << 16;
            return new WishAdoptionOffer(data.get(0) == 2, rules,
                    BuiltInRegistries.ITEM.getKey(Item.byId(itemId)).toString());
        } catch (RuntimeException incompleteSync) { return null; }
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (box == null || player.isSpectator() || player.containerMenu != this || !stillValid(player) || (button != 0 && button != 1))
            return false;
        box.setLocked(button == 1); // Set, never toggle: two viewers cannot accidentally invert each other.
        broadcastChanges();
        return true;
    }
    @Override public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> level.getBlockEntity(pos) instanceof WishAdoptionBoxBlockEntity
                && (box == null || level.getBlockEntity(pos) == box && !box.isRemoved())
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64, false);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!(stack.getItem() instanceof CatPancakeItem) || !moveItemStackTo(stack, 0, 9, false))
            return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
