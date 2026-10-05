package cn.laowu.mod.create;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.minecraft.core.*;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Filter hit tests and its rendered plate use the same local side as spout controls. */
public final class CatBasinFilter extends ValueBoxTransform.Sided {
    /** The basin content renderer has already rotated its parent pose. */
    private boolean parentAlreadyRotated;
    public void renderInLocalFrame(Runnable render){
        boolean previous=parentAlreadyRotated;parentAlreadyRotated=true;
        try{render.run();}finally{parentAlreadyRotated=previous;}
    }
    private final ValueBoxTransform.Sided local=new ValueBoxTransform.Sided(){
        @Override protected Vec3 getSouthLocation(){return new Vec3(.5,.75,16.05/16);}
    };
    @Override protected Vec3 getSouthLocation(){return new Vec3(.5,.75,16.05/16);}
    private void select(BlockState state){local.fromSide(CatMachineOrientation.toLocal(CatMachineOrientation.bottom(state),direction));}
    @Override public Vec3 getLocalOffset(LevelAccessor level,BlockPos pos,BlockState state){
        select(state);
        Vec3 offset=local.getLocalOffset(level,pos,state);
        return parentAlreadyRotated?offset:CatMachineOrientation.position(state,offset);
    }
    @Override public void rotate(LevelAccessor level,BlockPos pos,BlockState state,PoseStack pose){
        select(state);
        if(!parentAlreadyRotated)CatMachineOrientation.rotatePose(pose,CatMachineOrientation.bottom(state));
        local.rotate(level,pos,state,pose);
    }
    @Override protected boolean isSideActive(BlockState state,Direction face){
        return face.getAxis()!=CatMachineOrientation.bottom(state).getAxis();
    }
}
