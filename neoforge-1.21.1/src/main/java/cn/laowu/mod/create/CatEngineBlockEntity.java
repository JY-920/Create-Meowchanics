package cn.laowu.mod.create;

import cn.laowu.mod.LaoWuMod;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class CatEngineBlockEntity extends GeneratingKineticBlockEntity
        implements IHaveGoggleInformation {
    public static final int CAPACITY = 1000;
    public static final int FUEL_PER_TICK = 1;
    /** The shaft's real unloaded rotation speed. */
    public static final float GENERATED_RPM = 96.0F;
    /** 64 SU of capacity per RPM gives a maximum of 6144 SU. */
    public static final float STRESS_CAPACITY_PER_RPM = 64.0F;

    private SmartFluidTankBehaviour fuelTank;
    private ScrollOptionBehaviour<WindmillBearingBlockEntity.RotationDirection> rotationDirection;
    private boolean active;

    public CatEngineBlockEntity(BlockPos pos, BlockState state) {
        super(LaoWuMod.CAT_ENGINE_BE.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        rotationDirection = new ScrollOptionBehaviour<>(
                WindmillBearingBlockEntity.RotationDirection.class,
                CreateLang.translateDirect("contraptions.windmill.rotation_direction"),
                this, new ElevatedTopValueBoxTransform());
        rotationDirection.withCallback(this::onRotationDirectionChanged);
        behaviours.add(rotationDirection);

        fuelTank = SmartFluidTankBehaviour.single(this, CAPACITY)
                .whenFluidUpdates(this::setChanged);
        fuelTank.getPrimaryHandler().setValidator(
                stack -> stack.getFluid().isSame(LaoWuMod.HISSING_GAS.get()));
        behaviours.add(fuelTank);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || fuelTank == null) return;

        boolean nextActive = !fuelTank.getPrimaryHandler()
                .drain(FUEL_PER_TICK, IFluidHandler.FluidAction.EXECUTE)
                .isEmpty();
        if (nextActive == active) return;

        active = nextActive;
        setChanged();
        updateGeneratedRotation();
        sendData();
    }

    private void onRotationDirectionChanged(int ignored) {
        if (level == null || level.isClientSide) return;
        setChanged();
        updateGeneratedRotation();
    }

    @Override
    public float getGeneratedSpeed() {
        if (!active) return 0.0F;
        Direction facing = getBlockState().getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
        WindmillBearingBlockEntity.RotationDirection selected = rotationDirection == null
                ? WindmillBearingBlockEntity.RotationDirection.CLOCKWISE
                : rotationDirection.get();
        float signedSpeed = selected == WindmillBearingBlockEntity.RotationDirection.CLOCKWISE
                ? GENERATED_RPM : -GENERATED_RPM;
        return convertToDirection(signedSpeed, facing);
    }

    @Override
    public float calculateAddedStressCapacity() {
        lastCapacityProvided = STRESS_CAPACITY_PER_RPM;
        return lastCapacityProvided;
    }

    public @Nullable IFluidHandler getFluidHandler(@Nullable Direction side) {
        return fuelTank != null && (side == null || side == Direction.DOWN)
                ? fuelTank.getCapability() : null;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        if (fuelTank != null) {
            containedFluidTooltip(tooltip, isPlayerSneaking, fuelTank.getCapability());
        }
        return true;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1.0D);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putBoolean("Active", active);
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        active = tag.getBoolean("Active");
        super.read(tag, registries, clientPacket);
    }

    /** Places Create's direction board on the engine model's 13-pixel top. */
    private static final class ElevatedTopValueBoxTransform extends CenteredSideValueBoxTransform {
        private ElevatedTopValueBoxTransform() {
            super((state, side) -> side == Direction.UP);
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8.0D, 8.0D, 13.0D);
        }
    }
}
