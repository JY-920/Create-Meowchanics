package cn.laowu.mod.create;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.WishAdoptionBoxMenu;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.item.CatPancakeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Server-authoritative immediate adoption. Preview is metadata, never an extractable item. */
public final class WishAdoptionBoxBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_COUNT = 9, OUTPUT_START = 9, OUTPUT_COUNT = 9, SLOT_COUNT = 18;
    private WishAdoptionOffer offer;
    private boolean locked, internalChange, pending = true;
    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override protected int getStackLimit(int slot, @NotNull ItemStack stack) {
            return slot < INPUT_COUNT ? 1 : super.getStackLimit(slot, stack);
        }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot < INPUT_COUNT && stack.getItem() instanceof CatPancakeItem;
        }
        @Override protected void onContentsChanged(int slot) {
            if (internalChange) return;
            pending = true;
            setChanged();
        }
    };
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> inventory);

    public WishAdoptionBoxBlockEntity(BlockPos pos, BlockState state) {
        super(LaoWuMod.WISH_ADOPTION_BOX_BE.get(), pos, state);
    }
    public ItemStackHandler inventory() { return inventory; }
    public WishAdoptionOffer offer() { return offer; }
    public boolean locked() { return locked; }
    public ItemStack rewardPreview() { return offer == null ? ItemStack.EMPTY : offer.rewardStack(); }

    public void ensureOffer() {
        if (level == null || level.isClientSide) return;
        WishAdoptionOffer revised = offer == null ? WishAdoptionOffer.roll(level.random, null) : offer.normalized(level.random);
        if (revised != offer) {
            offer = revised;
            pending = true;
            sync();
        }
    }
    public void setLocked(boolean value) {
        if (level == null || level.isClientSide || locked == value) return;
        locked = value;
        sync();
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, WishAdoptionBoxBlockEntity box) {
        box.tickServer();
    }
    private void tickServer() {
        if (level == null || level.isClientSide) return;
        ensureOffer();
        if (offer == null || !pending && level.getGameTime() % 20 != 0) return;
        pending = false;
        boolean changed = false;
        for (int input = 0; input < INPUT_COUNT; input++) {
            ItemStack pancake = inventory.getStackInSlot(input);
            if (!(pancake.getItem() instanceof CatPancakeItem)) continue;
            boolean uninitialized = CatAttributeData.read(pancake).isEmpty();
            var profile = CatAttributeData.ensure(pancake, level.random);
            if (uninitialized) setChanged();
            if (!offer.matches(profile)) continue;
            List<ItemStack> outputs = mergedOutputs(offer.rewardStack());
            // No reward fits: leave the exact cat, offer and lock intact.
            if (outputs == null) continue;
            internalChange = true;
            try {
                inventory.setStackInSlot(input, pancake.getCount() == 1
                        ? ItemStack.EMPTY : pancake.copyWithCount(pancake.getCount() - 1));
                for (int i = 0; i < OUTPUT_COUNT; i++)
                    inventory.setStackInSlot(OUTPUT_START + i, outputs.get(i));
                if (!locked) offer = WishAdoptionOffer.roll(level.random, offer);
            } finally {
                internalChange = false;
            }
            changed = true;
            pending = true; // Recheck earlier slots against the newly rolled card next tick.
            if (offer == null) break;
        }
        if (changed) {
            sync();
            level.playSound(null, worldPosition, SoundEvents.VILLAGER_YES,
                    SoundSource.BLOCKS, .65F, 1.15F);
        }
    }
    @Nullable private List<ItemStack> mergedOutputs(ItemStack reward) {
        if (reward.isEmpty()) return null;
        List<ItemStack> result = new ArrayList<>(OUTPUT_COUNT);
        for (int i = 0; i < OUTPUT_COUNT; i++) result.add(inventory.getStackInSlot(OUTPUT_START + i).copy());
        ItemStack remaining = reward.copy();
        for (ItemStack existing : result) {
            if (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, remaining)) continue;
            int moved = Math.min(remaining.getCount(), Math.max(0, existing.getMaxStackSize() - existing.getCount()));
            existing.grow(moved); remaining.shrink(moved);
            if (remaining.isEmpty()) return result;
        }
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i).isEmpty()) { result.set(i, remaining); return result; }
        }
        return null;
    }

    @Override public Component getDisplayName() { return Component.translatable("container.laowu.wish_adoption_box"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        ensureOffer();
        return new WishAdoptionBoxMenu(id, inventory, this);
    }
    /** Breaking keeps the exact card and lock; contents retain their existing separate-drop behaviour. */
    public void writeCardToItem(ItemStack stack) {
        ensureOffer();
        cn.laowu.mod.item.WishAdoptionBoxBlockItem.writeCard(stack,offer,locked);
    }
    public ItemStack portableStack() {
        ItemStack stack=new ItemStack(LaoWuMod.WISH_ADOPTION_BOX_ITEM.get());
        writeCardToItem(stack);
        return stack;
    }
    public void dropContents(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        internalChange = true;
        try {
            for (int i = 0; i < SLOT_COUNT; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) Block.popResource(level, pos, stack.copy());
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        } finally { internalChange = false; }
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        writeOffer(tag);
    }
    private void writeOffer(CompoundTag tag) {
        tag.putBoolean("Locked", locked);
        if (offer != null) tag.put("Offer", offer.save());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        internalChange = true;
        try {
            if (tag.contains("Inventory")) inventory.deserializeNBT(tag.getCompound("Inventory"));
            offer = WishAdoptionOffer.load(tag.getCompound("Offer"));
            locked = tag.getBoolean("Locked");
            pending = true;
        } finally { internalChange = false; }
    }
    @Override public CompoundTag getUpdateTag() {
        // World rendering needs only the card, not 18 potentially large cat inventories.
        CompoundTag tag = new CompoundTag();
        writeOffer(tag);
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER && !remove) return itemCapability.cast();
        return super.getCapability(capability, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); itemCapability.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); itemCapability = LazyOptional.of(() -> inventory); }
}
