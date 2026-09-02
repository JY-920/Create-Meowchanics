package cn.laowu.mod.create;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatAttributeProfile;
import cn.laowu.mod.genetics.CatBreedingLogic;
import cn.laowu.mod.genetics.CatBreedingMode;
import cn.laowu.mod.genetics.CatGenome;
import cn.laowu.mod.genetics.CatGenomeData;
import cn.laowu.mod.genetics.CatMaterialRegistry;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatTraitProfile;
import cn.laowu.mod.item.BreedingCatFoodItem;
import cn.laowu.mod.item.CatFilterRules;
import cn.laowu.mod.item.CatPancakeItem;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.SidedFilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.function.BiPredicate;

/** Four-slot storage, sided automation and server-authoritative breeding timer. */
public final class BreedingBoxBlockEntity extends SmartBlockEntity implements MenuProvider {
    public static final int FATHER_SLOT = 0;
    public static final int MOTHER_SLOT = 1;
    public static final int CHILD_SLOT = 2;
    public static final int FOOD_SLOT = 3;
    public static final int SLOT_COUNT = 4;

    private static final String INVENTORY_TAG = "Inventory";
    private static final String PROGRESS_TAG = "BreedingProgress";
    private static final String CUSTOM_NAME_TAG = "CustomName";
    private static final String REDSTONE_POWERED_TAG = "ReplacementRedstonePowered";

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

    private final Map<Direction, IItemHandler> sidedHandlers = new EnumMap<>(Direction.class);
    private IItemHandler unsidedHandler;
    private ScrollOptionBehaviour<BreedingReplacementMode> replacementMode;
    private SidedFilteringBehaviour parentFilters;
    private int progress;
    private boolean redstonePowered;
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
        rebuildHandlers();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        replacementMode = new ScrollOptionBehaviour<>(
                BreedingReplacementMode.class,
                Component.translatable("gui.laowu.breeding_box.replacement.mode"),
                this,
                new OutsetSideValueBoxTransform((state, side) ->
                        isAdvancedState(state) && side == Direction.UP));
        replacementMode.onlyActiveWhen(() -> tier() == BreedingBoxTier.ADVANCED);
        replacementMode.withCallback(this::onReplacementModeChanged);
        behaviours.add(replacementMode);

        CenteredSideValueBoxTransform filterTransform =
                new OutsetSideValueBoxTransform(BreedingBoxBlockEntity::isParentFilterSide);
        parentFilters = new SidedFilteringBehaviour(this, filterTransform,
                this::configureParentFilter, this::isActiveParentFilterSide) {
            @Override
            public boolean setFilter(Direction side, ItemStack stack) {
                FilteringBehaviour filter = get(side);
                return filter != null && filter.setFilter(stack);
            }
        };
        behaviours.add(parentFilters);
    }

    public static void tickBox(Level level, BlockPos pos, BlockState state,
                               BreedingBoxBlockEntity box) {
        box.tick();
        if (!level.isClientSide) box.tickServer();
    }

    private FilteringBehaviour configureParentFilter(Direction side,
                                                      FilteringBehaviour filter) {
        filter.withPredicate(stack -> stack.is(LaoWuMod.CAT_FILTER.get()));
        filter.withCallback(ignored -> setChangedAndSync());
        filter.setLabel(Component.translatable(side == fatherSide()
                ? "gui.laowu.breeding_box.replacement.father_filter"
                : "gui.laowu.breeding_box.replacement.mother_filter"));
        return filter;
    }

    private static boolean isAdvancedState(BlockState state) {
        return state.getBlock() instanceof BreedingBoxBlock box
                && box.tier() == BreedingBoxTier.ADVANCED;
    }

    private static boolean isParentFilterSide(BlockState state, Direction side) {
        if (!isAdvancedState(state) || !state.hasProperty(BreedingBoxBlock.FACING)) return false;
        Direction front = state.getValue(BreedingBoxBlock.FACING);
        return side == front.getClockWise() || side == front.getCounterClockWise();
    }

    private boolean isActiveParentFilterSide(Direction side) {
        return isParentFilterSide(getBlockState(), side);
    }

    private Direction fatherSide() {
        return getBlockState().hasProperty(BreedingBoxBlock.FACING)
                ? getBlockState().getValue(BreedingBoxBlock.FACING).getClockWise()
                : Direction.EAST;
    }

    private Direction motherSide() {
        return getBlockState().hasProperty(BreedingBoxBlock.FACING)
                ? getBlockState().getValue(BreedingBoxBlock.FACING).getCounterClockWise()
                : Direction.WEST;
    }

    private BreedingReplacementMode replacementMode() {
        return tier() == BreedingBoxTier.ADVANCED && replacementMode != null
                ? replacementMode.get() : BreedingReplacementMode.LOCKED;
    }

    private void onReplacementModeChanged(int ignored) {
        if (level != null) redstonePowered = level.hasNeighborSignal(worldPosition);
        setChangedAndSync();
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

        tickReplacementRedstone();
        father = inventory.getStackInSlot(FATHER_SLOT);
        mother = inventory.getStackInSlot(MOTHER_SLOT);
        food = inventory.getStackInSlot(FOOD_SLOT);
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
        if (replacementMode() == BreedingReplacementMode.AUTOMATIC) replaceParentsFromPools();
        level.playSound(null, worldPosition, SoundEvents.CAT_PURR,
                SoundSource.BLOCKS, 0.9F, 1.15F);
    }

    private void tickReplacementRedstone() {
        if (tier() != BreedingBoxTier.ADVANCED || level == null) return;
        boolean powered = level.hasNeighborSignal(worldPosition);
        boolean risingEdge = powered && !redstonePowered;
        if (powered != redstonePowered) {
            redstonePowered = powered;
            setChanged();
        }
        if (risingEdge && replacementMode() == BreedingReplacementMode.REDSTONE) {
            replaceParentsFromPools();
        }
    }

    private void replaceParentsFromPools() {
        if (level == null || level.isClientSide
                || tier() != BreedingBoxTier.ADVANCED || parentFilters == null) return;
        boolean fatherChanged = tryReplaceParent(FATHER_SLOT, fatherSide());
        boolean motherChanged = tryReplaceParent(MOTHER_SLOT, motherSide());
        if (fatherChanged || motherChanged) {
            setChangedAndSync();
            level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP,
                    SoundSource.BLOCKS, 0.45F, 1.15F);
        }
    }

    private boolean tryReplaceParent(int parentSlot, Direction poolSide) {
        ItemStack filterStack = parentFilters.getFilter(poolSide);
        ItemStack current = inventory.getStackInSlot(parentSlot);
        if (!isBreedableParent(current)) return false;
        if (ensureParentData(current)) setChangedAndSync();

        CatFilterRules rules = CatFilterRules.read(filterStack);
        OptionalLong currentScore = replacementScore(rules, current);
        if (currentScore.isEmpty()) return false;

        BlockPos poolPos = worldPosition.relative(poolSide);
        if (!level.hasChunkAt(poolPos)) return false;
        IItemHandler handler = level.getCapability(
                Capabilities.ItemHandler.BLOCK, poolPos, poolSide.getOpposite());
        if (handler == null) return false;

        ParentCandidate best = null;
        long bestScore = rules.matchesIdentity(current)
                ? currentScore.getAsLong() : Long.MIN_VALUE;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stored = handler.getStackInSlot(slot);
            if (!isBreedableParent(stored)) continue;
            ItemStack prepared = stored.copyWithCount(1);
            ensureParentData(prepared);
            if (!rules.matchesIdentity(prepared)) continue;
            OptionalLong score = replacementScore(rules, prepared);
            if (score.isEmpty() || score.getAsLong() <= bestScore) continue;
            bestScore = score.getAsLong();
            best = new ParentCandidate(slot, stored.copy(), prepared);
        }
        if (best == null || !handler.isItemValid(best.slot(), current)) return false;

        ItemStack simulated = handler.extractItem(best.slot(), 1, true);
        if (simulated.isEmpty()
                || !ItemStack.isSameItemSameComponents(simulated, best.original())) return false;
        ItemStack extracted = handler.extractItem(best.slot(), 1, false);
        if (extracted.isEmpty()) return false;
        if (!ItemStack.isSameItemSameComponents(extracted, best.original())) {
            ItemStack rollback = ItemHandlerHelper.insertItemStacked(handler, extracted, false);
            if (!rollback.isEmpty()) Block.popResource(level, poolPos, rollback);
            return false;
        }

        ItemStack oldParent = current.copyWithCount(1);
        ItemStack oldRemainder = ItemHandlerHelper.insertItemStacked(handler, oldParent, false);
        if (!oldRemainder.isEmpty()) {
            ItemStack rollback = ItemHandlerHelper.insertItemStacked(handler, extracted, false);
            if (!rollback.isEmpty()) Block.popResource(level, poolPos, rollback);
            return false;
        }

        inventory.setStackInSlot(parentSlot, best.prepared());
        return true;
    }

    private static OptionalLong replacementScore(CatFilterRules rules, ItemStack parent) {
        CatAttributeProfile attributes = CatAttributeData.read(parent).orElse(null);
        return attributes == null ? OptionalLong.empty()
                : rules.replacementScore(attributes,
                CatTraitData.read(parent).orElse(CatTraitProfile.EMPTY));
    }

    private record ParentCandidate(int slot, ItemStack original, ItemStack prepared) {}

    private static final class OutsetSideValueBoxTransform
            extends CenteredSideValueBoxTransform {
        private OutsetSideValueBoxTransform(BiPredicate<BlockState, Direction> allowedDirections) {
            super(allowedDirections);
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8.0D, 8.0D, 16.2D);
        }
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
                CatMaterialRegistry.mutationMaterials(), mutationChance, level.random));
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

    public int effectiveDurationTicks() {
        int base = tier().durationTicks();
        int reduction = prosperousReductionTicks(inventory.getStackInSlot(FATHER_SLOT))
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

    public ItemStackHandler inventory() { return inventory; }
    public ContainerData menuData() { return menuData; }

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
    protected void write(CompoundTag tag, HolderLookup.Provider registries,
                         boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put(INVENTORY_TAG, inventory.serializeNBT(registries));
        tag.putInt(PROGRESS_TAG, progress);
        tag.putBoolean(REDSTONE_POWERED_TAG, redstonePowered);
        if (customName != null) tag.putString(CUSTOM_NAME_TAG,
                Component.Serializer.toJson(customName, registries));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries,
                        boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains(INVENTORY_TAG)) {
            inventory.deserializeNBT(registries, tag.getCompound(INVENTORY_TAG));
        }
        progress = Math.max(0, tag.getInt(PROGRESS_TAG));
        redstonePowered = tag.getBoolean(REDSTONE_POWERED_TAG);
        customName = tag.contains(CUSTOM_NAME_TAG)
                ? Component.Serializer.fromJson(tag.getString(CUSTOM_NAME_TAG), registries) : null;
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) notifyUpdate();
    }

    @Nullable
    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (tier() == BreedingBoxTier.BASIC) return null;
        if (side == null) return unsidedHandler;
        return sidedHandlers.getOrDefault(side, unsidedHandler);
    }

    private void rebuildHandlers() {
        sidedHandlers.clear();
        if (tier() == BreedingBoxTier.BASIC) {
            unsidedHandler = null;
            return;
        }
        Direction front = getBlockState().hasProperty(BreedingBoxBlock.FACING)
                ? getBlockState().getValue(BreedingBoxBlock.FACING) : Direction.NORTH;
        Direction visualLeft = front.getClockWise();
        Direction visualRight = front.getCounterClockWise();
        for (Direction side : Direction.values()) {
            SlotAccessHandler handler;
            if (side == front) handler = new SlotAccessHandler(CHILD_SLOT, false, true);
            else if (side == visualLeft) handler = new SlotAccessHandler(FATHER_SLOT, true, true);
            else if (side == visualRight) handler = new SlotAccessHandler(MOTHER_SLOT, true, true);
            else handler = new SlotAccessHandler(FOOD_SLOT, true, true);
            sidedHandlers.put(side, handler);
        }
        unsidedHandler = new SlotAccessHandler(FOOD_SLOT, true, true);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        rebuildHandlers();
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
