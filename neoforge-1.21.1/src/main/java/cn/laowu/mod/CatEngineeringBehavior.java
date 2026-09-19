package cn.laowu.mod;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.kinetics.crank.HandCrankBlock;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Cat;

import java.util.Map;
import java.util.WeakHashMap;

/** A seated engineer drives the real Create crank, never a cosmetic duplicate. */
public final class CatEngineeringBehavior {
    public static final int REACH_TICKS = 10;
    private static final Map<Cat, Work> WORK = new WeakHashMap<>();

    // Keep only scalar identities: retaining a BE here would retain its whole world.
    private record Work(long position, int identity, long started) {}

    /** Shared, read-only geometry lookup; never scans entities or loads another chunk. */
    public static HandCrankBlockEntity findCrank(Cat cat) {
        if (CatClothesData.getOutfit(cat) != CatOutfitType.ENGINEERING
                || !cat.isAlive() || !cat.isTame() || cat.isInWater()
                || CatPoseData.isPancake(cat) || CatPoseData.isHissing(cat)
                || !(cat.getVehicle() instanceof SeatEntity seat)) return null;
        BlockPos seatPos = seat.blockPosition();
        BlockPos crankPos = seatPos.above();
        if (!cat.level().hasChunkAt(seatPos) || !cat.level().hasChunkAt(crankPos)
                || !(cat.level().getBlockState(seatPos).getBlock() instanceof SeatBlock)) return null;
        var state = cat.level().getBlockState(crankPos);
        if (!AllBlocks.HAND_CRANK.has(state)
                || !state.getValue(HandCrankBlock.FACING).getAxis().isHorizontal()) return null;
        return cat.level().getBlockEntity(crankPos) instanceof HandCrankBlockEntity crank
                && !crank.isRemoved() ? crank : null;
    }

    public static float facingYaw(HandCrankBlockEntity crank) {
        // Side-on: the cat's right flank faces the crank. Both paws grip its rod.
        return crank.getBlockState().getValue(HandCrankBlock.FACING).getClockWise().toYRot();
    }

    public static void tick(Cat cat) {
        if (cat.level().isClientSide) return;
        HandCrankBlockEntity crank = findCrank(cat);
        if (crank == null || CatProfileData.isBeingViewed(cat)) {
            WORK.remove(cat);
            return;
        }
        cat.getNavigation().stop();
        cat.setTarget(null);
        float yaw = facingYaw(crank);
        cat.setYRot(Mth.approachDegrees(cat.getYRot(), yaw, 18.0F));
        cat.setYBodyRot(cat.getYRot());
        cat.setYHeadRot(cat.getYRot());
        cat.setXRot(0.0F);
        long now = cat.level().getGameTime();
        Work work = WORK.get(cat);
        if (work == null || work.position() != crank.getBlockPos().asLong()
                || work.identity() != System.identityHashCode(crank)) {
            work = new Work(crank.getBlockPos().asLong(), System.identityHashCode(crank), now);
            WORK.put(cat, work);
        }
        if (now - work.started() < REACH_TICKS) return;
        // Keep the real Create speed/phase. Capacity is total SU, not SU per RPM.
        ((CatCrankPower) crank).laowu$drive(cat);
        crank.turn(crank.backwards);
        if (cat.tickCount % 5 == 0) crank.sendData();
    }

    private CatEngineeringBehavior() {}
}
