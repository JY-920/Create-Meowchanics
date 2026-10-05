package cn.laowu.mod.create;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class CatProcessorBlock extends KineticBlock implements IBE<KineticBlockEntity> {
    public static final EnumProperty<Direction.Axis> SHAFT_AXIS=EnumProperty.create("shaft_axis",Direction.Axis.class);
    public static final BooleanProperty SHAFT_OPEN=BooleanProperty.create("shaft_open");
    private final boolean mixer;
    protected CatProcessorBlock(Properties properties,boolean mixer) {
        super(properties);this.mixer=mixer;
        registerDefaultState(defaultBlockState().setValue(CatMachineOrientation.BOTTOM,Direction.DOWN).setValue(SHAFT_AXIS,Direction.Axis.X).setValue(SHAFT_OPEN,true));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) {
        super.createBlockStateDefinition(b);b.add(CatMachineOrientation.BOTTOM,SHAFT_AXIS,SHAFT_OPEN);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) {
        if(!CatProcessorPlacement.canPlaceAt(c.getLevel(),c.getClickedPos()))return null;
        if(!c.isSecondaryUseActive()) for(Direction bottom:c.getNearestLookingDirections()) {
            BlockState state=defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom).setValue(SHAFT_AXIS,bottom.getAxis()==Direction.Axis.X?Direction.Axis.Z:Direction.Axis.X);
            if(CatProcessorPlacement.alignedTarget(c.getLevel(),c.getClickedPos(),state,mixer))return state;
        }
        Direction bottom=CatMachineOrientation.placementBottom(c,true);
        return defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom)
            .setValue(SHAFT_AXIS,bottom.getAxis()==Direction.Axis.X?Direction.Axis.Z:Direction.Axis.X);
    }
    @Override public boolean canSurvive(BlockState state,LevelReader world,BlockPos pos) {
        return CatProcessorPlacement.canPlaceAt(world,pos);
    }
    @Override public Direction.Axis getRotationAxis(BlockState state) {return state.getValue(SHAFT_AXIS);}
    @Override public boolean hasShaftTowards(LevelReader world,BlockPos pos,BlockState state,Direction face) {
        // Retain shaft_open as a saved-state compatibility key; ports are now permanent.
        return face.getAxis()==getRotationAxis(state)&&face.getAxis()!=CatMachineOrientation.bottom(state).getAxis();
    }
    @Override protected boolean areStatesKineticallyEquivalent(BlockState a,BlockState b) {return a.equals(b);}
    @Override public SpeedLevel getMinimumRequiredSpeedLevel() {return mixer?SpeedLevel.MEDIUM:SpeedLevel.NONE;}
    @Override public Class<KineticBlockEntity> getBlockEntityClass() {return KineticBlockEntity.class;}
    @Override public BlockState getRotatedBlockState(BlockState state,Direction face) {
        // IWrenchable handles survival checks, network detach/reconnect and wrench sounds.
        Direction bottom=CatMachineOrientation.bottom(state).getClockWise(face.getAxis());
        Direction shaft=Direction.get(Direction.AxisDirection.POSITIVE,state.getValue(SHAFT_AXIS)).getClockWise(face.getAxis());
        return state.setValue(CatMachineOrientation.BOTTOM,bottom).setValue(SHAFT_AXIS,shaft.getAxis()).setValue(SHAFT_OPEN,true);
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context) {
        return CatMachineOrientation.rotateShape(box(0,2,0,16,16,16),CatMachineOrientation.bottom(state));
    }
}
