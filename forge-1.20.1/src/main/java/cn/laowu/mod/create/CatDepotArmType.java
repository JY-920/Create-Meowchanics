package cn.laowu.mod.create;

import com.simibubi.create.content.kinetics.mechanicalArm.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** A native depot target, restricted to our block and aligned to its working face. */
public final class CatDepotArmType extends ArmInteractionPointType {
    @Override public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof CatDepotBlock;
    }
    @Override public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
        return new AllArmInteractionPointTypes.DepotPoint(this,level,pos,state) {
            private Direction angleBottom;
            @Override public ArmAngleTarget getTargetAngles(BlockPos armPos, boolean ceiling) {
                updateCachedState();
                Direction bottom = CatMachineOrientation.bottom(cachedState);
                if (bottom != angleBottom) {
                    cachedAngles = null;
                    angleBottom = bottom;
                }
                return super.getTargetAngles(armPos,ceiling);
            }
            @Override protected Vec3 getInteractionPositionVector() {
                return Vec3.atLowerCornerOf(pos).add(CatMachineOrientation.position(cachedState,new Vec3(.5,.875,.5)));
            }
            @Override protected Direction getInteractionDirection() {
                return CatMachineOrientation.bottom(cachedState);
            }
        };
    }
}
