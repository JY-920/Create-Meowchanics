package cn.laowu.mod.create;
import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
/** Original basin behaviour, including spouts and heat; only its shell and type differ. */
public final class HajiBasinBlock extends BasinBlock implements CatWorkSurfaceWrench {
    public HajiBasinBlock(Properties properties) {
        super(properties); registerDefaultState(defaultBlockState().setValue(CatMachineOrientation.BOTTOM, net.minecraft.core.Direction.DOWN));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block,net.minecraft.world.level.block.state.BlockState> b) {
        super.createBlockStateDefinition(b); b.add(CatMachineOrientation.BOTTOM);
    }
    @Override public net.minecraft.world.level.block.state.BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c) {
        return defaultBlockState().setValue(CatMachineOrientation.BOTTOM,CatMachineOrientation.placementBottom(c,false));
    }
    @Override public boolean canSurvive(net.minecraft.world.level.block.state.BlockState s, net.minecraft.world.level.LevelReader w, net.minecraft.core.BlockPos p) {
        return !(w.getBlockEntity(p.relative(CatMachineOrientation.top(s))) instanceof com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity);
    }
    @Override public net.minecraft.world.InteractionResult onWrenched(net.minecraft.world.level.block.state.BlockState s, net.minecraft.world.item.context.UseOnContext c) {
        if(c.getClickedFace()!=CatMachineOrientation.top(s))return rotateWorkSurface(s,c);
        if(!c.getLevel().isClientSide) withBlockEntityDo(c.getLevel(),c.getClickedPos(),be->be.onWrenched(CatMachineOrientation.toLocal(CatMachineOrientation.bottom(s),c.getClickedFace())));
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
    @Override public net.minecraft.world.level.block.state.BlockState getRotatedBlockState(net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.Direction face) {
        return CatWorkSurfaceWrench.super.getRotatedBlockState(state,face);
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.block.state.BlockState s, net.minecraft.world.level.BlockGetter w, net.minecraft.core.BlockPos p, net.minecraft.world.phys.shapes.CollisionContext c) {
        return CatMachineOrientation.rotateShape(com.simibubi.create.AllShapes.BASIN_BLOCK_SHAPE,CatMachineOrientation.bottom(s));
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getInteractionShape(net.minecraft.world.level.block.state.BlockState s, net.minecraft.world.level.BlockGetter w, net.minecraft.core.BlockPos p) {
        return CatMachineOrientation.rotateShape(com.simibubi.create.AllShapes.BASIN_RAYTRACE_SHAPE,CatMachineOrientation.bottom(s));
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(net.minecraft.world.level.block.state.BlockState s, net.minecraft.world.level.BlockGetter w, net.minecraft.core.BlockPos p, net.minecraft.world.phys.shapes.CollisionContext c) {
        // Create shortens the item rim for upright intake. Rotating that shortened rim
        // sideways would leave a hole in the closed world-top wall beside the mouth.
        var shape=CatMachineOrientation.bottom(s)==net.minecraft.core.Direction.DOWN
            && c instanceof net.minecraft.world.phys.shapes.EntityCollisionContext ec && ec.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity
            ? com.simibubi.create.AllShapes.BASIN_COLLISION_SHAPE : com.simibubi.create.AllShapes.BASIN_BLOCK_SHAPE;
        return CatMachineOrientation.rotateShape(shape,CatMachineOrientation.bottom(s));
    }
    @Override public void updateEntityAfterFallOn(net.minecraft.world.level.BlockGetter world, net.minecraft.world.entity.Entity entity) {
        // Preserve Block's landing response, but not BasinBlock's unconditional
        // world-up insertion. The block entity collects only its rotated interior.
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(1,0,1));
    }
    @Override public BlockEntityType<? extends BasinBlockEntity> getBlockEntityType() {
        return CatMachineBlocks.BASIN_BE.get();
    }
}
