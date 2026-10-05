package cn.laowu.mod.create;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Canonical frame: local DOWN is the physical bottom; vectors rotate, positions pivot at center. */
public final class CatMachineOrientation {
    public static final DirectionProperty BOTTOM = DirectionProperty.create("bottom");
    public static Direction bottom(BlockState state) { return state.hasProperty(BOTTOM) ? state.getValue(BOTTOM) : Direction.DOWN; }
    public static Direction top(BlockState state) { return bottom(state).getOpposite(); }
    /** Sneak placement points the working face toward the player. */
    public static Direction placementBottom(net.minecraft.world.item.context.BlockPlaceContext context, boolean workingBottom) {
        if (context.isSecondaryUseActive()) {
            Direction look = context.getNearestLookingDirection();
            return workingBottom ? look.getOpposite() : look;
        }
        return context.getClickedFace().getOpposite();
    }
    public static Vec3 toWorld(Direction bottom, Vec3 v) {
        return switch (bottom) {
            case DOWN -> v;
            case UP -> new Vec3(v.x,-v.y,-v.z);
            case NORTH -> new Vec3(v.x,-v.z,v.y);
            case SOUTH -> new Vec3(v.x,v.z,-v.y);
            case EAST -> new Vec3(-v.y,v.z,-v.x);
            case WEST -> new Vec3(v.y,v.z,v.x);
        };
    }
    public static Direction toWorld(Direction bottom, Direction local) {
        Vec3 v=toWorld(bottom,Vec3.atLowerCornerOf(local.getNormal()));
        return Direction.getNearest(v.x,v.y,v.z);
    }
    public static Direction toLocal(Direction bottom, Direction world) {
        for(Direction local:Direction.values()) if(toWorld(bottom,local)==world)return local;
        throw new IllegalArgumentException("Unknown direction");
    }
    public static Vec3 vector(BlockState state,Vec3 local) {return toWorld(bottom(state),local);}
    public static Vec3 position(BlockState state,Vec3 local) {
        return vector(state,local.subtract(.5,.5,.5)).add(.5,.5,.5);
    }
    public static BlockPos rotatePosition(BlockPos origin,BlockPos localPosition,BlockState state) {
        Vec3 v=vector(state,Vec3.atLowerCornerOf(localPosition.subtract(origin)));
        return origin.offset((int)Math.round(v.x),(int)Math.round(v.y),(int)Math.round(v.z));
    }
    public static VoxelShape rotateShape(VoxelShape shape,Direction bottom) {
        if(bottom==Direction.DOWN)return shape;
        VoxelShape[] result={Shapes.empty()};
        shape.forAllBoxes((x1,y1,z1,x2,y2,z2)->{
            Vec3 a=toWorld(bottom,new Vec3(x1-.5,y1-.5,z1-.5)).add(.5,.5,.5);
            Vec3 b=toWorld(bottom,new Vec3(x2-.5,y2-.5,z2-.5)).add(.5,.5,.5);
            result[0]=Shapes.or(result[0],Shapes.box(Math.min(a.x,b.x),Math.min(a.y,b.y),Math.min(a.z,b.z),Math.max(a.x,b.x),Math.max(a.y,b.y),Math.max(a.z,b.z)));
        });
        return result[0].optimize();
    }
    public static void rotatePose(com.mojang.blaze3d.vertex.PoseStack pose,Direction bottom) {
        switch(bottom){
            case DOWN -> {}
            case UP -> pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180));
            case NORTH -> pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
            case SOUTH -> pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));
            case EAST -> {pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));}
            case WEST -> {pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90));pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));}
        }
    }
    private CatMachineOrientation() {}
}
