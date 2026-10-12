package cn.laowu.mod.create;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatAttributeProfile;
import cn.laowu.mod.genetics.CatBreedingLogic;
import cn.laowu.mod.genetics.CatBreedingMode;
import cn.laowu.mod.genetics.CatGenome;
import cn.laowu.mod.genetics.CatGenomeData;
import cn.laowu.mod.genetics.CatMaterialRegistry;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatTrait;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.function.BiPredicate;

/** Create-native automation adapter around the shared breeding state. */
public final class BreedingBoxBlockEntity extends SmartBlockEntity implements MenuProvider, BreedingBoxAccess {
    public static final int FATHER_SLOT=0, MOTHER_SLOT=1, CHILD_SLOT=2, FOOD_SLOT=3, SLOT_COUNT=4;
    private final BreedingBoxData data;
    private ScrollOptionBehaviour<BreedingReplacementMode> replacementMode;
    private SidedFilteringBehaviour parentFilters;
    public BreedingBoxBlockEntity(BlockPos pos, BlockState state) {
        super(LaoWuMod.BREEDING_BOX_BE.get(),pos,state);
        data=new BreedingBoxData(pos,state,this);
        data.setAutomation(new BreedingBoxData.ParentAutomation() {
            public int mode() { return replacementMode == null ? 0 : replacementMode.get().ordinal(); }
            public ItemStack filter(Direction side) { return parentFilters == null ? ItemStack.EMPTY : parentFilters.getFilter(side); }
        });
    }
    @Override public BreedingBoxData breedingData() { return data; }
    @Override public void tickCare() { tick(); }
    @Override public void destroyCare() { destroy(); }
    @Override public void setLevel(Level level) { super.setLevel(level); data.setLevel(level); }
    @Override public void onLoad() { super.onLoad(); data.onLoad(); }
    public BreedingBoxTier tier() { return data.tier(); }
    public ItemStackHandler inventory() { return data.inventory(); }
    public ContainerData menuData() { return data.menuData(); }
    public int effectiveDurationTicks() { return data.effectiveDurationTicks(); }
    public float effectiveMutationChance() { return data.effectiveMutationChance(); }
    public int effectiveMutationBasisPoints() { return data.effectiveMutationBasisPoints(); }
    public ItemStack insertParent(ItemStack stack) { return data.insertParent(stack); }
    public ItemStack insertFood(ItemStack stack) { return data.insertFood(stack); }
    public ItemStack extractManual() { return data.extractManual(); }
    public void setCustomName(Component name) { data.setCustomName(name); }
    public void dropContents(Level level, BlockPos pos) { data.dropContents(level,pos); }
    @Override public Component getDisplayName() { return data.getDisplayName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return data.createMenu(id,inventory,player); }
    @Override public AABB getRenderBoundingBox() { return data.getRenderBoundingBox(); }
    public static boolean isAdultPancake(ItemStack stack) { return BreedingBoxData.isAdultPancake(stack); }
    public static boolean isBreedableParent(ItemStack stack) { return BreedingBoxData.isBreedableParent(stack); }
    private void setChangedAndSync() { data.touch(); }
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
            // Create 6.0's sided wrapper discards the child predicate's return
            // value. Preserve it here so non-cat filters are rejected instead
            // of being consumed by the short-interaction handler.
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
        BreedingBoxData.tickBox(level,pos,state,box);
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
        if (!isAdvancedState(state) || !state.hasProperty(BreedingBoxBlock.FACING)) {
            return false;
        }
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
        if (data != null) data.automationChanged();
    }

    private static final class OutsetSideValueBoxTransform
            extends CenteredSideValueBoxTransform {
        private OutsetSideValueBoxTransform(
                BiPredicate<BlockState, Direction> allowedDirections) {
            super(allowedDirections);
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8.0D, 8.0D, 16.2D);
        }
    }
    @Override protected void write(CompoundTag tag, boolean clientPacket) { super.write(tag,clientPacket); data.writeData(tag); }
    @Override protected void read(CompoundTag tag, boolean clientPacket) { super.read(tag,clientPacket); data.readData(tag); }
    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) { if(cap==ForgeCapabilities.ITEM_HANDLER)return data.getCapability(cap,side); return super.getCapability(cap,side); }
    @Override public void invalidateCaps() { super.invalidateCaps(); data.invalidateCaps(); }
    @Override public void reviveCaps() { super.reviveCaps(); data.reviveCaps(); }
}
