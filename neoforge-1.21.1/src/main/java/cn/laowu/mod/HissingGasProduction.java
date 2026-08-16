package cn.laowu.mod;

import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Cat;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Produces a small amount of hiss gas while a hissing cat sits above a Create tank. */
public final class HissingGasProduction {
    private static final int PRODUCTION_INTERVAL_TICKS = 5;
    private static final int PRODUCTION_AMOUNT_MB = 100;

    public static void tick(Cat cat) {
        if (!CatPoseData.isHissing(cat)
                || cat.tickCount % PRODUCTION_INTERVAL_TICKS != 0) return;

        BlockPos seatPos = findSeat(cat);
        if (seatPos == null) return;
        FluidTankBlockEntity tank = findTankBelow(cat, seatPos);
        if (tank == null) return;

        FluidTankBlockEntity controller = tank.getControllerBE();
        if (controller == null) return;
        controller.getTankInventory().fill(
                new FluidStack(LaoWuMod.HISSING_GAS.get(), PRODUCTION_AMOUNT_MB),
                IFluidHandler.FluidAction.EXECUTE);
    }

    private static FluidTankBlockEntity findTankBelow(Cat cat, BlockPos seatPos) {
        // Normally the first position is the tank. The second also tolerates
        // seat entities whose floored Y coordinate lands one block too high.
        for (int distance = 1; distance <= 2; distance++) {
            if (cat.level().getBlockEntity(seatPos.below(distance)) instanceof FluidTankBlockEntity tank) {
                return tank;
            }
        }
        return null;
    }

    private static BlockPos findSeat(Cat cat) {
        if (cat.getVehicle() instanceof SeatEntity seatEntity) {
            BlockPos pos = seatEntity.blockPosition();
            if (cat.level().getBlockState(pos).getBlock() instanceof SeatBlock) return pos.immutable();
        }

        BlockPos[] candidates = {cat.blockPosition(), cat.blockPosition().below(), cat.getOnPos()};
        for (BlockPos pos : candidates) {
            if (cat.level().getBlockState(pos).getBlock() instanceof SeatBlock) return pos.immutable();
        }
        return null;
    }

    private HissingGasProduction() {}
}
