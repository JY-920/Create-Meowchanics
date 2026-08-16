package cn.laowu.mod.create;

import cn.laowu.mod.LaoWuMod;
import com.simibubi.create.content.kinetics.fan.NozzleBlock;
import com.simibubi.create.content.kinetics.fan.NozzleBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import javax.annotation.Nullable;

/** A fluid-producing nozzle that keeps Create's native six-way fan attachment rules. */
public final class HissingCollectorBlock extends NozzleBlock {
    /** Independent roll/front direction for the supplied asymmetric cat-face model. */
    public static final DirectionProperty MODEL_FACING = DirectionProperty.create(
            "model_facing", Direction.Plane.HORIZONTAL);

    public HissingCollectorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(MODEL_FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState nozzleState = super.getStateForPlacement(context);
        return nozzleState == null ? null : nozzleState.setValue(
                MODEL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return super.rotate(state, rotation).setValue(
                MODEL_FACING, rotation.rotate(state.getValue(MODEL_FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return super.mirror(state, mirror).setValue(
                MODEL_FACING, mirror.mirror(state.getValue(MODEL_FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MODEL_FACING);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Class<NozzleBlockEntity> getBlockEntityClass() {
        return (Class) HissingCollectorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends NozzleBlockEntity> getBlockEntityType() {
        return LaoWuMod.HISSING_COLLECTOR_BE.get();
    }
}
