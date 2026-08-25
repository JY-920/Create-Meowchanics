package cn.laowu.mod;

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

/** Server-backed cat profile menu matching the supplied 290-pixel main layout. */
public final class CatProfileMenu extends AbstractContainerMenu {
    public static final int MAIN_WIDTH = 290;
    public static final int MAIN_HEIGHT = 209;
    /** Centres Create's 176-pixel player inventory beneath the 218-pixel main panel. */
    public static final int PLAYER_PANEL_X = 21;
    public static final int PLAYER_PANEL_Y = 217;
    public static final int SCREEN_HEIGHT = 325;

    private static final int ACCESSORY_X = 222;
    private static final int ACCESSORY_Y = 45;
    private static final int CAT_INVENTORY_X = 222;
    private static final int CAT_INVENTORY_Y = 140;

    private final Container catInventory;
    private final int catId;
    private final String initialName;
    private final Cat viewedCat;
    private boolean viewLockReleased;

    /** Forge client factory; entity id and current name follow the open packet. */
    public CatProfileMenu(int containerId, Inventory playerInventory,
                          FriendlyByteBuf buffer) {
        this(containerId, playerInventory, buffer.readVarInt(),
                buffer.readUtf(CatProfileData.MAX_NAME_LENGTH), null);
    }

    public CatProfileMenu(int containerId, Inventory playerInventory, Cat cat) {
        this(containerId, playerInventory, cat.getId(),
                CatProfileData.editableName(cat), cat);
    }

    private CatProfileMenu(int containerId, Inventory playerInventory, int catId,
                           String initialName, Cat serverCat) {
        super(LaoWuMod.CAT_PROFILE_MENU.get(), containerId);
        this.catId = catId;
        this.initialName = initialName;
        // The client gets a prediction-only container populated by vanilla's
        // initial menu sync. It must never reuse the server's cached container
        // in an integrated game, where both logical cats share an entity id.
        this.catInventory = serverCat == null
                ? new SimpleContainer(CatProfileData.SLOT_COUNT)
                : CatProfileData.openContainer(serverCat);
        this.viewedCat = serverCat;
        initializeSlots(playerInventory);
        if (viewedCat != null) CatProfileData.beginViewing(viewedCat);
    }

    private void initializeSlots(Inventory playerInventory) {
        catInventory.startOpen(playerInventory.player);

        for (int row = 0; row < CatProfileData.ACCESSORY_SLOTS; row++) {
            addSlot(new Slot(catInventory, row,
                    ACCESSORY_X, ACCESSORY_Y + row * 20));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int slot = CatProfileData.ACCESSORY_SLOTS + row * 3 + column;
                addSlot(new Slot(catInventory, slot,
                        CAT_INVENTORY_X + column * 20,
                        CAT_INVENTORY_Y + row * 20));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, 9 + row * 9 + column,
                        PLAYER_PANEL_X + 8 + column * 18,
                        PLAYER_PANEL_Y + 18 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column,
                    PLAYER_PANEL_X + 8 + column * 18,
                    PLAYER_PANEL_Y + 76));
        }
    }

    public int getCatId() {
        return catId;
    }

    public String getInitialName() {
        return initialName;
    }

    public Cat getCat(Player player) {
        Entity entity = player.level().getEntity(catId);
        return entity instanceof Cat cat ? cat : null;
    }

    @Override
    public boolean stillValid(Player player) {
        Cat cat = getCat(player);
        return cat != null && catInventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int catSlots = CatProfileData.SLOT_COUNT;
        if (index < catSlots) {
            if (!moveItemStackTo(stack, catSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack,
                CatProfileData.ACCESSORY_SLOTS, catSlots, false)
                && !moveItemStackTo(stack, 0,
                CatProfileData.ACCESSORY_SLOTS, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        catInventory.setChanged();
        catInventory.stopOpen(player);
        if (!viewLockReleased && viewedCat != null) {
            viewLockReleased = true;
            CatProfileData.endViewing(viewedCat);
        }
    }
}
