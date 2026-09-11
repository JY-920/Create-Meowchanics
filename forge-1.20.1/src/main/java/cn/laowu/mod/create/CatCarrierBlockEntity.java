package cn.laowu.mod.create;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** One persistent cat and 8000 mB. The displayed cat never enters the world/entity tick list. */
public final class CatCarrierBlockEntity extends BlockEntity
        implements com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation {
    private ItemStack cat = ItemStack.EMPTY;
    private boolean pendingSync;
    public final FluidTank tank = new FluidTank(8000,
            fluid -> fluid.getFluid().isSame(LaoWuMod.HISSING_GAS.get())) {
        @Override protected void onContentsChanged() { changed(); }
    };
    private net.minecraftforge.common.util.LazyOptional<IFluidHandler> fluidCap =
            net.minecraftforge.common.util.LazyOptional.of(() -> tank);
    public CatCarrierBlockEntity(BlockPos pos, BlockState state) {
        super(LaoWuMod.CAT_CARRIER_BE.get(), pos, state);
    }
    public ItemStack catStack() { return cat; }
    @Override public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip, boolean sneaking) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("block.laowu.cat_carrier"));
        tooltip.add(net.minecraft.network.chat.Component.literal("    ")
                .append(net.minecraft.network.chat.Component.translatable("fluid.laowu.hissing_gas"))
                .append(": " + tank.getFluidAmount() + " / 8000 mB"));
        return true;
    }
    public IFluidHandler getFluidHandler(Direction side) { return tank; }
    public InteractionResult interact(Player player, net.minecraft.world.InteractionHand hand) {
        if (level == null) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && !cat.isEmpty()) {
                ItemStack taken = cat;
                cat = ItemStack.EMPTY;
                if (!player.getInventory().add(taken)) player.drop(taken, false);
                changed();
                sync();
            }
            return InteractionResult.SUCCESS;
        }
        if (!held.is(LaoWuMod.CAT_PANCAKE.get())) return InteractionResult.PASS;
        if (!level.isClientSide && cat.isEmpty()) {
            cat = held.copyWithCount(1);
            held.shrink(1); // A container transfer consumes the inserted stack even in Creative.
            changed();
            sync();
        }
        return InteractionResult.SUCCESS;
    }
    public static void tick(Level level, BlockPos pos, BlockState state, CatCarrierBlockEntity box) {
        if (!level.isClientSide && box.pendingSync && level.getGameTime() % 20 == 0) box.sync();
        if (!level.isClientSide && !box.cat.isEmpty() && level.getGameTime() % 5 == 0) {
            // Half of the two-cat tank output: 100 mB / 5 ticks = 400 mB/s.
            box.tank.fill(new FluidStack(LaoWuMod.HISSING_GAS.get(), 100), IFluidHandler.FluidAction.EXECUTE);
        }
    }
    private void changed() {
        setChanged();
        pendingSync = true;
    }
    private void sync() {
        pendingSync = false;
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    public ItemStack portableStack() {
        ItemStack result = new ItemStack(LaoWuMod.CAT_CARRIER_ITEM.get());
        saveToItem(result);
        return result;
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!cat.isEmpty()) tag.put("Cat", cat.save(new CompoundTag()));
        tag.put("Tank", tank.writeToNBT(new CompoundTag()));
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        cat = ItemStack.of(tag.getCompound("Cat"));
        if (!cat.isEmpty() && !cat.is(LaoWuMod.CAT_PANCAKE.get())) cat = ItemStack.EMPTY;
        if (!cat.isEmpty()) cat.setCount(1);
        tank.readFromNBT(tag.getCompound("Tank"));
        if (!tank.isEmpty() && !tank.getFluid().getFluid().isSame(LaoWuMod.HISSING_GAS.get()))
            tank.setFluid(FluidStack.EMPTY);
        if (tank.getFluidAmount() > 8000) tank.getFluid().setAmount(8000);
    }
    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); fluidCap.invalidate(); }
    @Override public void reviveCaps() {
        super.reviveCaps();
        fluidCap = net.minecraftforge.common.util.LazyOptional.of(() -> tank);
    }
}
