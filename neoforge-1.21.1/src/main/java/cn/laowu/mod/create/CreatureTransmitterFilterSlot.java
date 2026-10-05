package cn.laowu.mod.create;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.minecraft.core.*;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Authored top slot: the item, native hover plate and hit region share one transform. */
public final class CreatureTransmitterFilterSlot extends ValueBoxTransform.Sided {
    public static final CreatureTransmitterFilterSlot INSTANCE=new CreatureTransmitterFilterSlot();
    public CreatureTransmitterFilterSlot(){fromSide(Direction.UP);}
    @Override protected Vec3 getSouthLocation(){return new Vec3(.5,.5,15.5/16);}
    @Override protected boolean isSideActive(BlockState state,Direction side){return side==Direction.UP;}
    @Override public boolean testHit(LevelAccessor level,BlockPos pos,BlockState state,Vec3 hit){
        return Math.abs(hit.x-.5)<=2d/16&&Math.abs(hit.z-.5)<=2d/16&&Math.abs(hit.y-1)<1d/16;
    }
    public static boolean hits(BlockState state,BlockHitResult hit){
        return hit.getDirection()==Direction.UP&&INSTANCE.testHit(null,hit.getBlockPos(),state,
            hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos())));
    }
}
