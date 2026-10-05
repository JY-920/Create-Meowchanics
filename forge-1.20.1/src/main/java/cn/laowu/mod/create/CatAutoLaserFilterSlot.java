package cn.laowu.mod.create;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.minecraft.core.*;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** One authored back-face slot shared by rendering, picking and Create's hover box. */
public final class CatAutoLaserFilterSlot extends ValueBoxTransform {
    public static final CatAutoLaserFilterSlot INSTANCE = new CatAutoLaserFilterSlot();
    // Back face UV [0,16]..[16,28]: inner square U6..10,V23..27 -> centre Y=3/16.
    private static final Vec3 CENTER = new Vec3(.5, 3d / 16, -1d / 512);
    private static final ValueBoxTransform.Sided NORTH = new ValueBoxTransform.Sided() {
        @Override protected Vec3 getSouthLocation() { return new Vec3(.5, 3d / 16, 1 + 1d / 512); }
    }.fromSide(Direction.NORTH);

    private CatAutoLaserFilterSlot() {}
    @Override public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        return CatMachineOrientation.position(state, CENTER);
    }
    @Override public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack pose) {
        CatMachineOrientation.rotatePose(pose, CatMachineOrientation.bottom(state));
        NORTH.rotate(level, pos, state, pose);
    }
    @Override public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 hit) {
        var bottom = CatMachineOrientation.bottom(state);
        var delta = hit.subtract(getLocalOffset(level,pos,state));
        double x = delta.dot(CatMachineOrientation.toWorld(bottom,new Vec3(1,0,0)));
        double y = delta.dot(CatMachineOrientation.toWorld(bottom,new Vec3(0,1,0)));
        double z = delta.dot(CatMachineOrientation.toWorld(bottom,new Vec3(0,0,1)));
        return Math.abs(x) <= 2d/16 && Math.abs(y) <= 2d/16 && Math.abs(z) < 1d/16;
    }
    public static boolean hits(BlockState state, BlockHitResult hit) {
        return CatMachineOrientation.toLocal(CatMachineOrientation.bottom(state),hit.getDirection()) == Direction.NORTH
                && INSTANCE.testHit(null,hit.getBlockPos(),state,hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos())));
    }
}
