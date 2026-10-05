package cn.laowu.mod.create;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

/** Rotate a container through Create's survival, neighbour-update and sound workflow. */
public interface CatWorkSurfaceWrench extends IWrenchable {
    default InteractionResult rotateWorkSurface(BlockState state, UseOnContext context) {
        return IWrenchable.super.onWrenched(state, context);
    }

    @Override default BlockState getRotatedBlockState(BlockState state, Direction face) {
        Direction bottom=CatMachineOrientation.bottom(state);
        if(face==bottom.getOpposite())return state;
        Direction.Axis axis=face.getAxis();
        // The closed back face has no visible roll axis, so tip the opening by a
        // quarter-turn. Side clicks rotate clockwise about the clicked face axis.
        if(axis==bottom.getAxis())axis=axis==Direction.Axis.X?Direction.Axis.Z:Direction.Axis.X;
        return state.setValue(CatMachineOrientation.BOTTOM,bottom.getClockWise(axis));
    }
}
