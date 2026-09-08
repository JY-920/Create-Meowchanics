package cn.laowu.mod.create;

import cn.laowu.mod.AdoptionBoxMenu;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatAttributeEffects;
import cn.laowu.mod.genetics.CatAttributeProfile;
import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatTraitEffects;
import cn.laowu.mod.genetics.CatTraitProfile;
import cn.laowu.mod.item.CatPancakeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Nine parallel adoption slots plus nine extraction-only reward slots. */
public final class AdoptionBoxBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_START = 0;
    public static final int INPUT_COUNT = 9;
    public static final int OUTPUT_START = INPUT_START + INPUT_COUNT;
    public static final int OUTPUT_COUNT = 9;
    public static final int SLOT_COUNT = INPUT_COUNT + OUTPUT_COUNT;

    /** A zero-quality cat takes two minutes; a perfect cat takes one minute. */
    public static final int BASE_DURATION_TICKS = 20 * 60 * 2;
    public static final int PERFECT_DURATION_TICKS = 20 * 60;

    private static final String INVENTORY_TAG = "Inventory";
    private static final String PROGRESS_TAG = "AdoptionProgress";

    private final int[] progress = new int[INPUT_COUNT];
    private boolean internalChange;
    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected int getStackLimit(int slot, @NotNull ItemStack stack) {
            return isInputSlot(slot) ? 1 : super.getStackLimit(slot, stack);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isInputSlot(slot) && stack.getItem() instanceof CatPancakeItem;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (internalChange) return;
            if (isInputSlot(slot)) progress[slot - INPUT_START] = 0;
            setChangedAndSync();
        }
    };
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> inventory);

    public AdoptionBoxBlockEntity(BlockPos pos, BlockState state) {
        super(LaoWuMod.ADOPTION_BOX_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  AdoptionBoxBlockEntity box) {
        box.tickServer();
    }

    private void tickServer() {
        if (level == null || level.isClientSide) return;
        boolean progressChanged = false;
        boolean adopted = false;

        for (int input = 0; input < INPUT_COUNT; input++) {
            ItemStack pancake = inventory.getStackInSlot(INPUT_START + input);
            if (!(pancake.getItem() instanceof CatPancakeItem)) {
                if (progress[input] != 0) {
                    progress[input] = 0;
                    progressChanged = true;
                }
                continue;
            }

            boolean initialized = CatAttributeData.read(pancake).isEmpty();
            int quality = qualityScore(pancake);
            if (initialized) setChangedAndSync();
            int duration = durationForQuality(quality);
            boolean justFinished = false;
            if (progress[input] < duration) {
                progress[input]++;
                progressChanged = true;
                justFinished = progress[input] >= duration;
            }
            // A full reward panel is rechecked once per second, not every tick.
            if (progress[input] >= duration
                    && (justFinished || level.getGameTime() % 20L == 0L)
                    && completeAdoption(input, quality)) {
                adopted = true;
            }
        }

        if (adopted) {
            setChangedAndSync();
            level.playSound(null, worldPosition, SoundEvents.VILLAGER_CELEBRATE,
                    SoundSource.BLOCKS, 0.8F, 1.1F);
        } else if (progressChanged && level.getGameTime() % 20L == 0L) {
            // Persist at one-second granularity without dirtying the chunk every tick.
            setChanged();
        }
    }

    /**
     * Current attributes contribute two thirds and trainable limits one third.
     * Existing trait bonuses are reflected in the current-value portion, while
     * the final score remains within the same intuitive 0..100 range.
     */
    public int qualityScore(ItemStack pancake) {
        if (level == null) return 0;
        CatAttributeProfile attributes = CatAttributeData.ensure(pancake, level.random);
        CatTraitProfile traits = CatTraitData.read(pancake).orElse(CatTraitProfile.EMPTY);
        boolean night = CatTraitEffects.isNight(level);
        boolean day = CatTraitEffects.isDay(level);
        int weightedTotal = 0;
        for (CatStat stat : CatStat.values()) {
            int current = CatAttributeEffects.effectiveValue(
                    attributes, traits, stat, night, day);
            weightedTotal += Mth.clamp(current, 0, 100) * 2;
            weightedTotal += Mth.clamp(attributes.potential(stat), 0, 100);
        }
        return Mth.clamp(Math.round(weightedTotal / 18.0F), 0, 100);
    }

    public static int durationForQuality(int quality) {
        int clamped = Mth.clamp(quality, 0, 100);
        return BASE_DURATION_TICKS
                - (BASE_DURATION_TICKS - PERFECT_DURATION_TICKS) * clamped / 100;
    }

    private boolean completeAdoption(int inputIndex, int quality) {
        List<ItemStack> rewards = AdoptionRewardTable.roll(quality, level.random);
        List<ItemStack> resultingOutputs = mergedOutputs(rewards);
        if (resultingOutputs == null) return false;

        internalChange = true;
        try {
            inventory.setStackInSlot(INPUT_START + inputIndex, ItemStack.EMPTY);
            for (int output = 0; output < OUTPUT_COUNT; output++) {
                inventory.setStackInSlot(OUTPUT_START + output,
                        resultingOutputs.get(output));
            }
            progress[inputIndex] = 0;
        } finally {
            internalChange = false;
        }
        return true;
    }

    /** Returns the complete post-insertion output inventory, or null if it cannot all fit. */
    @Nullable
    private List<ItemStack> mergedOutputs(List<ItemStack> rewards) {
        List<ItemStack> result = new ArrayList<>(OUTPUT_COUNT);
        for (int slot = 0; slot < OUTPUT_COUNT; slot++) {
            result.add(inventory.getStackInSlot(OUTPUT_START + slot).copy());
        }

        for (ItemStack reward : rewards) {
            ItemStack remainder = reward.copy();
            for (int slot = 0; slot < result.size() && !remainder.isEmpty(); slot++) {
                ItemStack existing = result.get(slot);
                if (existing.isEmpty()
                        || !ItemStack.isSameItemSameTags(existing, remainder)) continue;
                int moved = Math.min(remainder.getCount(),
                        existing.getMaxStackSize() - existing.getCount());
                if (moved <= 0) continue;
                existing.grow(moved);
                remainder.shrink(moved);
            }
            for (int slot = 0; slot < result.size() && !remainder.isEmpty(); slot++) {
                if (!result.get(slot).isEmpty()) continue;
                int moved = Math.min(remainder.getCount(), remainder.getMaxStackSize());
                result.set(slot, remainder.copyWithCount(moved));
                remainder.shrink(moved);
            }
            if (!remainder.isEmpty()) return null;
        }
        return result;
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public static boolean isInputSlot(int slot) {
        return slot >= INPUT_START && slot < INPUT_START + INPUT_COUNT;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.laowu.adoption_box");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new AdoptionBoxMenu(id, playerInventory, this);
    }

    public void dropContents(Level level, BlockPos pos) {
        internalChange = true;
        try {
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!stack.isEmpty()) Block.popResource(level, pos, stack.copy());
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
            Arrays.fill(progress, 0);
        } finally {
            internalChange = false;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(INVENTORY_TAG, inventory.serializeNBT());
        tag.putIntArray(PROGRESS_TAG, progress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(INVENTORY_TAG)) {
            inventory.deserializeNBT(tag.getCompound(INVENTORY_TAG));
        }
        Arrays.fill(progress, 0);
        int[] loaded = tag.getIntArray(PROGRESS_TAG);
        System.arraycopy(loaded, 0, progress, 0, Math.min(loaded.length, progress.length));
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability,
                                                      @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER && !remove) {
            return itemCapability.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemCapability = LazyOptional.of(() -> inventory);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(0.25D);
    }
}
