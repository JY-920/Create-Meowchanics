package cn.laowu.mod.create;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;

/** The native spout contract in the basin's local frame; never invents or drops outputs. */
public final class CatBasinOutput {
    public static boolean canOutputTo(BlockGetter level,BlockPos pos,Direction local) {
        var state=level.getBlockState(pos);
        if(!(state.getBlock() instanceof HajiBasinBlock))return BasinBlock.canOutputTo(level,pos,local);
        if(local.getAxis().isVertical())return false;
        Direction direction=CatMachineOrientation.toWorld(CatMachineOrientation.bottom(state),local);
        BlockPos neighbour=pos.relative(direction), output=neighbour.relative(CatMachineOrientation.bottom(state));
        var neighbourState=level.getBlockState(neighbour);
        if(FunnelBlock.isFunnel(neighbourState)){
            if(FunnelBlock.getFunnelFacing(neighbourState)==direction)return false;
        }else if(!neighbourState.getCollisionShape(level,neighbour).isEmpty())return false;
        else if(level.getBlockEntity(output) instanceof BeltBlockEntity belt)
            return belt.getSpeed()==0||belt.getMovementFacing()!=direction.getOpposite();
        DirectBeltInputBehaviour input=BlockEntityBehaviour.get(level,output,DirectBeltInputBehaviour.TYPE);
        return input!=null&&input.canInsertFromSide(direction);
    }
    private CatBasinOutput(){}
}

