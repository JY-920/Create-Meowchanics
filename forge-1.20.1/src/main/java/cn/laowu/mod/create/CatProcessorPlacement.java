package cn.laowu.mod.create;
import com.simibubi.create.content.logistics.depot.DepotBlock;
import com.simibubi.create.content.processing.basin.BasinBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public final class CatProcessorPlacement {
    /** Placement is free except for the block directly in front of any basin/depot work face. */
    public static boolean canPlaceAt(LevelReader level,BlockPos pos) {
        for(Direction side:Direction.values()) {
            BlockPos adjacent=pos.relative(side);
            if(!level.hasChunkAt(adjacent))continue;
            BlockState neighbour=level.getBlockState(adjacent);
            if((neighbour.getBlock() instanceof BasinBlock||neighbour.getBlock() instanceof DepotBlock)
                &&CatMachineOrientation.top(neighbour)==side.getOpposite())return false;
        }
        return true;
    }
    public static boolean alignedTarget(LevelReader level,BlockPos pos,BlockState state,boolean mixer) {
        Direction bottom=CatMachineOrientation.bottom(state);
        BlockPos target=pos.relative(bottom,2);
        if(!level.hasChunkAt(target))return false;
        BlockState support=level.getBlockState(target);
        return (support.getBlock() instanceof BasinBlock||(!mixer&&support.getBlock() instanceof DepotBlock))
            &&CatMachineOrientation.bottom(support)==bottom;
    }
    public static boolean validTarget(LevelReader level,BlockPos pos,BlockState state,boolean mixer) {
        Direction bottom=CatMachineOrientation.bottom(state);
        BlockPos gap=pos.relative(bottom);
        return alignedTarget(level,pos,state,mixer)&&level.hasChunkAt(gap)
            &&level.getBlockState(gap).getCollisionShape(level,gap).isEmpty();
    }
    private CatProcessorPlacement() {}
}
