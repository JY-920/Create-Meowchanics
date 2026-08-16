package cn.laowu.mod.create;

import cn.laowu.mod.LaoWuMod;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A standalone horizontal kinetic source built only on Create's public
 * kinetic block API. Its shaft follows the horizontal placement axis and its
 * fluid capability is supplied by {@link CatEngineBlockEntity} from below.
 */
public final class CatEngineBlock extends HorizontalKineticBlock
        implements IBE<CatEngineBlockEntity> {
    private static final VoxelShape NORTH_SOUTH_COLLISION = Shapes.or(
            box(3.0D, 3.0D, 0.0D, 13.0D, 13.0D, 16.0D),
            box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D));
    private static final VoxelShape EAST_WEST_COLLISION = Shapes.or(
            box(0.0D, 3.0D, 3.0D, 16.0D, 13.0D, 13.0D),
            box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D));
    public CatEngineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return getPhysicalShape(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return getPhysicalShape(state);
    }

    private static VoxelShape getPhysicalShape(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis() == Direction.Axis.Z
                ? NORTH_SOUTH_COLLISION
                : EAST_WEST_COLLISION;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction side) {
        return side.getAxis() == getRotationAxis(state);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public Class<CatEngineBlockEntity> getBlockEntityClass() {
        return CatEngineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CatEngineBlockEntity> getBlockEntityType() {
        return LaoWuMod.CAT_ENGINE_BE.get();
    }
}
