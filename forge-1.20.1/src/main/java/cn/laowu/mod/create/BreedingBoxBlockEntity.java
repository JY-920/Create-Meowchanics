package cn.laowu.mod.create;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatAttributeProfile;
import cn.laowu.mod.genetics.CatBreedingLogic;
import cn.laowu.mod.genetics.CatBreedingMode;
import cn.laowu.mod.genetics.CatGenome;
import cn.laowu.mod.genetics.CatGenomeData;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitProfile;
import cn.laowu.mod.item.BreedingCatFoodItem;
import cn.laowu.mod.item.CatPancakeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
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

import java.util.EnumMap;
import java.util.Map;

/** Four-slot storage, sided automation and server-authoritative breeding timer. */
public final class BreedingBoxBlockEntity extends BlockEntity implements MenuProvider {
    public static final int FATHER_SLOT = 0;
    public static final int MOTHER_SLOT = 1;
    public static final int CHILD_SLOT = 2;
    public static final int FOOD_SLOT = 3;
    public static final int SLOT_COUNT = 4;

    private static final String INVENTORY_TAG = "Inventory";
    private static final String PROGRESS_TAG = "BreedingProgress";
    private static final String CUSTOM_NAME_TAG = "CustomName";

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected int getStackLimit(int slot, @NotNull ItemStack stack) {
            return slot == FOOD_SLOT ? super.getStackLimit(slot, stack) : 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case FATHER_SLOT, MOTHER_SLOT -> isBreedableParent(stack);
                case FOOD_SLOT -> BreedingCatFoodItem.isBreedingFood(stack);
                default -> false;
            };
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == FATHER_SLOT || slot == MOTHER_SLOT || slot == FOOD_SLOT) progress = 0;
            setChangedAndSync();
        }
    };

    private final Map<Direction, LazyOptional<IItemHandler>> sidedCapabilities =
            new EnumMap<>(Direction.class);
    private LazyOptional<IItemHandler> unsidedCapability = LazyOptional.empty();
    private int progress;
    private Component customName;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> effectiveDurationTicks();
                case 2 -> tier().ordinal();
                case 3 -> effectiveMutationBasisPoints();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public BreedingBoxBlockEntity(BlockPos pos, BlockState state) {
        super(LaoWuMod.BREEDING_BOX_BE.get(), pos, state);
        rebuildCapabilities();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  BreedingBoxBlockEntity box) {
        box.tickServer();
    }

    private void tickServer() {
        if (level == null || level.isClientSide) return;
        ItemStack father = inventory.getStackInSlot(FATHER_SLOT);
        ItemStack mother = inventory.getStackInSlot(MOTHER_SLOT);
        ItemStack food = inventory.getStackInSlot(FOOD_SLOT);
        boolean initializedParent = false;
        if (isAdultPancake(father)) initializedParent |= ensureParentData(father);
        if (isAdultPancake(mother)) initializedParent |= ensureParentData(mother);
        if (initializedParent) setChangedAndSync();
        if (!isBreedableParent(father) || !isBreedableParent(mother)
                || !BreedingCatFoodItem.isBreedingFood(food)
                || !inventory.getStackInSlot(CHILD_SLOT).isEmpty()) {
            if (progress != 0) {
                progress = 0;
                setChangedAndSync();
            }
            return;
        }

        progress++;
        int duration = effectiveDurationTicks();
        if (progress < duration) {
            if ((progress & 15) == 0) setChangedAndSync();
            return;
        }

        ItemStack child = createChild(father, mother, food);
        if (child.isEmpty()) {
            progress = duration - 1;
            return;
        }
        inventory.extractItem(FOOD_SLOT, 1, false);
        inventory.setStackInSlot(CHILD_SLOT, child);
        progress = 0;
        setChangedAndSync();
        level.playSound(null, worldPosition, SoundEvents.CAT_PURR,
                SoundSource.BLOCKS, 0.9F, 1.15F);
    }

    private boolean ensureParentData(ItemStack stack) {
        boolean changed = false;
        if (CatAttributeData.read(stack).isEmpty()) {
            CatAttributeData.set(stack, CatAttributeProfile.founder(level.random));
            changed = true;
        }
        if (CatGenomeData.read(stack).isEmpty()) {
            CatGenomeData.set(stack, CatGenome.uniform(CatPancakeItem.variantId(stack)));
            changed = true;
        }
        if (CatTraitData.read(stack).isEmpty()) {
            CatTraitData.set(stack, CatTraitProfile.founder(level.random));
            changed = true;
        }
        return changed;
    }

    private ItemStack createChild(ItemStack father, ItemStack mother, ItemStack food) {
        CatAttributeProfile firstAttributes = CatAttributeData.read(father).orElse(null);
        CatAttributeProfile secondAttributes = CatAttributeData.read(mother).orElse(null);
        CatGenome firstGenome = CatGenomeData.read(father).orElse(null);
        CatGenome secondGenome = CatGenomeData.read(mother).orElse(null);
        CatTraitProfile firstTraits = CatTraitData.read(father).orElse(CatTraitProfile.EMPTY);
        CatTraitProfile secondTraits = CatTraitData.read(mother).orElse(CatTraitProfile.EMPTY);
        CatBreedingMode mode = BreedingCatFoodItem.mode(food).orElse(null);
        if (firstAttributes == null || secondAttributes == null || mode == null
                || firstGenome == null || secondGenome == null) return ItemStack.EMPTY;

        float mutationChance = CatBreedingLogic.effectiveMutationChance(
                tier().mutationChance(), mode, firstAttributes, firstTraits,
                secondAttributes, secondTraits);

        ItemStack child = CatPancakeItem.babyVariantStack(level.random.nextBoolean()
                ? CatPancakeItem.variantId(father) : CatPancakeItem.variantId(mother));
        CatAttributeData.set(child, CatAttributeProfile.breed(
                firstAttributes, secondAttributes, mode, mutationChance, level.random));
        CatGenomeData.set(child, CatGenome.fuse(firstGenome, secondGenome,
                BuiltInRegistries.CAT_VARIANT.keySet(), mutationChance, level.random));
        CatTraitData.set(child, CatTraitProfile.breed(
                firstTraits, secondTraits, mutationChance, level.random));
        return child;
    }

    public float effectiveMutationChance() {
        ItemStack food = inventory.getStackInSlot(FOOD_SLOT);
        CatBreedingMode mode = BreedingCatFoodItem.mode(food).orElse(CatBreedingMode.NORMAL);
        CatAttributeProfile first = CatAttributeData.read(
                inventory.getStackInSlot(FATHER_SLOT)).orElse(null);
        CatAttributeProfile second = CatAttributeData.read(
                inventory.getStackInSlot(MOTHER_SLOT)).orElse(null);
        CatTraitProfile firstTraits = CatTraitData.read(
                inventory.getStackInSlot(FATHER_SLOT)).orElse(CatTraitProfile.EMPTY);
        CatTraitProfile secondTraits = CatTraitData.read(
                inventory.getStackInSlot(MOTHER_SLOT)).orElse(CatTraitProfile.EMPTY);
        return CatBreedingLogic.effectiveMutationChance(
                tier().mutationChance(), mode, first, firstTraits, second, secondTraits);
    }

    public int effectiveMutationBasisPoints() {
        return CatBreedingLogic.basisPoints(effectiveMutationChance());
    }

    /** Both parents contribute, while the five-second test tier is never lengthened. */
    public int effectiveDurationTicks() {
        int base = tier().durationTicks();
        int reduction = prosperousReductionTicks(
                inventory.getStackInSlot(FATHER_SLOT))
                + prosperousReductionTicks(inventory.getStackInSlot(MOTHER_SLOT));
        int minimum = Math.min(base, 20 * 20);
        return Math.max(minimum, base - reduction);
    }

    private static int prosperousReductionTicks(ItemStack parent) {
        int level = CatTraitData.read(parent).orElse(CatTraitProfile.EMPTY)
                .level(CatTrait.PROSPEROUS_LITTER);
        return level <= 0 ? 0
                : CatTrait.PROSPEROUS_LITTER
                .prosperousBreedingReductionSeconds(level) * 20;
    }

    public BreedingBoxTier tier() {
        return getBlockState().getBlock() instanceof BreedingBoxBlock box
                ? box.tier() : BreedingBoxTier.BASIC;
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public ContainerData menuData() {
        return menuData;
    }

    /** Inserts an adult pancake into father first, then mother. */
    public ItemStack insertParent(ItemStack stack) {
        if (!isAdultPancake(stack)) return stack;
        for (int slot : new int[]{FATHER_SLOT, MOTHER_SLOT}) {
            if (inventory.getStackInSlot(slot).isEmpty()) {
                return inventory.insertItem(slot, stack, false);
            }
        }
        return stack;
    }

    public ItemStack insertFood(ItemStack stack) {
        return inventory.insertItem(FOOD_SLOT, stack, false);
    }

    /** Manual sneak-removal order: child, all food, mother, then father. */
    public ItemStack extractManual() {
        ItemStack child = inventory.extractItem(CHILD_SLOT, 1, false);
        if (!child.isEmpty()) return child;
        int foodCount = inventory.getStackInSlot(FOOD_SLOT).getCount();
        ItemStack food = inventory.extractItem(FOOD_SLOT, foodCount, false);
        if (!food.isEmpty()) return food;
        ItemStack mother = inventory.extractItem(MOTHER_SLOT, 1, false);
        return mother.isEmpty() ? inventory.extractItem(FATHER_SLOT, 1, false) : mother;
    }

    public void setCustomName(Component customName) {
        this.customName = customName;
        setChangedAndSync();
    }

    @Override
    public Component getDisplayName() {
        return customName != null ? customName
                : Component.translatable("container.laowu." + tier().serializedName());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new cn.laowu.mod.BreedingBoxMenu(id, playerInventory, this);
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) Block.popResource(level, pos, stack.copy());
        }
        inventory.setSize(SLOT_COUNT);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(INVENTORY_TAG, inventory.serializeNBT());
        tag.putInt(PROGRESS_TAG, progress);
        if (customName != null) tag.putString(CUSTOM_NAME_TAG,
                Component.Serializer.toJson(customName));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(INVENTORY_TAG)) inventory.deserializeNBT(tag.getCompound(INVENTORY_TAG));
        progress = Math.max(0, tag.getInt(PROGRESS_TAG));
        if (tag.contains(CUSTOM_NAME_TAG)) {
            customName = Component.Serializer.fromJson(tag.getString(CUSTOM_NAME_TAG));
        }
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
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability,
                                                      @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER && !remove) {
            if (side == null) return unsidedCapability.cast();
            return sidedCapabilities.getOrDefault(side, LazyOptional.empty()).cast();
        }
        return super.getCapability(capability, side);
    }

    private void rebuildCapabilities() {
        sidedCapabilities.values().forEach(LazyOptional::invalidate);
        sidedCapabilities.clear();
        Direction front = getBlockState().hasProperty(BreedingBoxBlock.FACING)
                ? getBlockState().getValue(BreedingBoxBlock.FACING) : Direction.NORTH;
        Direction visualLeft = front.getClockWise();
        Direction visualRight = front.getCounterClockWise();
        for (Direction side : Direction.values()) {
            SlotAccessHandler handler;
            if (side == front) {
                handler = new SlotAccessHandler(CHILD_SLOT, false, true);
            } else if (side == visualLeft) {
                handler = new SlotAccessHandler(FATHER_SLOT, true, true);
            } else if (side == visualRight) {
                handler = new SlotAccessHandler(MOTHER_SLOT, true, true);
            } else {
                handler = new SlotAccessHandler(FOOD_SLOT, true, true);
            }
            sidedCapabilities.put(side, LazyOptional.of(() -> handler));
        }
        unsidedCapability = LazyOptional.of(() -> new SlotAccessHandler(FOOD_SLOT, true, true));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        rebuildCapabilities();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sidedCapabilities.values().forEach(LazyOptional::invalidate);
        unsidedCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        rebuildCapabilities();
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).expandTowards(0.0D, 0.25D, 0.0D);
    }

    public static boolean isAdultPancake(ItemStack stack) {
        return stack.getItem() instanceof CatPancakeItem && !CatPancakeItem.isBaby(stack);
    }

    public static boolean isBreedableParent(ItemStack stack) {
        return isAdultPancake(stack) && !CatTraitData.read(stack)
                .orElse(CatTraitProfile.EMPTY).has(CatTrait.CUDDLE_ONLY);
    }

    private final class SlotAccessHandler implements IItemHandler {
        private final int backingSlot;
        private final boolean insertion;
        private final boolean extraction;

        private SlotAccessHandler(int backingSlot, boolean insertion, boolean extraction) {
            this.backingSlot = backingSlot;
            this.insertion = insertion;
            this.extraction = extraction;
        }

        @Override public int getSlots() { return 1; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) {
            return slot == 0 ? inventory.getStackInSlot(backingSlot) : ItemStack.EMPTY;
        }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack,
                                                       boolean simulate) {
            return slot == 0 && insertion
                    ? inventory.insertItem(backingSlot, stack, simulate) : stack;
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 0 && extraction
                    ? inventory.extractItem(backingSlot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) {
            return slot == 0 ? inventory.getSlotLimit(backingSlot) : 0;
        }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == 0 && insertion && inventory.isItemValid(backingSlot, stack);
        }
    }
}
