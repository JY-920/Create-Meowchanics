package cn.laowu.mod.create;

import com.simibubi.create.content.logistics.depot.DepotBlock;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Create's depot interaction and processing contract with an independently registered shell. */
public final class CatDepotBlock extends DepotBlock implements CatWorkSurfaceWrench {
    private static final VoxelShape BODY = Shapes.or(box(0, 0, 0, 16, 11, 16), box(1, 11, 1, 15, 13, 15));

    public CatDepotBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CatMachineOrientation.BOTTOM, net.minecraft.core.Direction.DOWN));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) {
        super.createBlockStateDefinition(b); b.add(CatMachineOrientation.BOTTOM);
    }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c) {
        return super.getStateForPlacement(c).setValue(CatMachineOrientation.BOTTOM, CatMachineOrientation.placementBottom(c, false));
    }
    @Override public net.minecraft.world.InteractionResult onWrenched(BlockState state, net.minecraft.world.item.context.UseOnContext c) {
        return rotateWorkSurface(state,c);
    }
    @Override public BlockState getRotatedBlockState(BlockState state, net.minecraft.core.Direction face) {
        return CatWorkSurfaceWrench.super.getRotatedBlockState(state,face);
    }

    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,
            net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit) {
        if(hit.getDirection()!=CatMachineOrientation.top(state))return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        // Ordinary clicks insert any held material, including machine items.
        // Crouching bypasses block interaction in the standard player use flow.
        return super.useItemOn(stack,state,level,pos,player,hand,hit.withDirection(net.minecraft.core.Direction.UP));
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return CatMachineOrientation.rotateShape(BODY, CatMachineOrientation.bottom(state));
    }

    @Override public BlockEntityType<? extends DepotBlockEntity> getBlockEntityType() {
        return CatDepotRegistration.DEPOT_BE.get();
    }
}
