package cn.laowu.mod.create;

import cn.laowu.mod.LaoWuMod;
import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Create basin processing internals with a full-cube P4 shell. Normal placement
 * and wrench rotation use the four horizontal directions exclusively.
 */
public final class InfiltrationTankBlock extends BasinBlock {
    /** Visual front of the custom four-sided model; unrelated to basin output. */
    public static final DirectionProperty MODEL_FACING = DirectionProperty.create(
            "model_facing", Direction.Plane.HORIZONTAL);

    public InfiltrationTankBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.DOWN)
                .setValue(MODEL_FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // HorizontalDirectionalBlock convention: the logical front faces back
        // toward the player who placed it.
        return defaultBlockState()
                .setValue(FACING, Direction.DOWN)
                .setValue(MODEL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Direction current = state.getValue(MODEL_FACING);
        Direction next = current.getClockWise();
        if (!context.getLevel().isClientSide) {
            context.getLevel().setBlockAndUpdate(context.getClickedPos(),
                    state.setValue(FACING, Direction.DOWN).setValue(MODEL_FACING, next));
        }
        com.simibubi.create.content.equipment.wrench.IWrenchable.playRotateSound(
                context.getLevel(), context.getClickedPos());
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MODEL_FACING);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Class<BasinBlockEntity> getBlockEntityClass() {
        return (Class) InfiltrationTankBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BasinBlockEntity> getBlockEntityType() {
        return LaoWuMod.INFILTRATION_TANK_BE.get();
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        // Dropped items stay outside; only automation inserts ingredients.
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
    }
}
