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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;

public final class CatEngineBlockEntity extends GeneratingKineticBlockEntity
        implements IHaveGoggleInformation {
    public static final int FLUID_CAPACITY = 1000;
    public static final int HISS_PER_TICK = 1;
    /** The shaft's real unloaded rotation speed. */
    public static final float GENERATED_RPM = 96.0F;
    /** Create tooltip notation: 64 SU of capacity for every RPM. */
    public static final float STRESS_CAPACITY_PER_RPM = 64.0F;

    private SmartFluidTankBehaviour hissTank;
    private ScrollOptionBehaviour<WindmillBearingBlockEntity.RotationDirection> rotationDirection;
    private boolean active;

    public CatEngineBlockEntity(BlockPos pos, BlockState state) {
        super(LaoWuMod.CAT_ENGINE_BE.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        // This is Create's own windmill direction selector. Scroll interaction,
        // the hold-to-open board, icons, networking and value persistence are
        // all provided by Create's ScrollOptionBehaviour/Value Settings UI.
        rotationDirection = new ScrollOptionBehaviour<>(
                WindmillBearingBlockEntity.RotationDirection.class,
                CreateLang.translateDirect("contraptions.windmill.rotation_direction"),
                this, new ElevatedTopValueBoxTransform());
        rotationDirection.withCallback(this::onRotationDirectionChanged);
        behaviours.add(rotationDirection);

        hissTank = SmartFluidTankBehaviour.single(this, FLUID_CAPACITY)
                .whenFluidUpdates(this::setChanged);
        hissTank.getPrimaryHandler().setValidator(
                stack -> stack.getFluid().isSame(LaoWuMod.HISSING_GAS.get()));
        behaviours.add(hissTank);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || hissTank == null) return;

        boolean nextActive = !hissTank.getPrimaryHandler()
                .drain(HISS_PER_TICK, IFluidHandler.FluidAction.EXECUTE)
                .isEmpty();
        setActive(nextActive);
    }

    private void setActive(boolean nextActive) {
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

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER && hissTank != null
                && (side == null || side == Direction.DOWN)) {
            return hissTank.getCapability().cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        if (hissTank != null) {
            containedFluidTooltip(tooltip, isPlayerSneaking, hissTank.getCapability().cast());
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
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putBoolean("Active", active);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        active = tag.getBoolean("Active");
    }

    /** Places Create's native direction board on the 13-pixel collision top. */
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
