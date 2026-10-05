package cn.laowu.mod.create;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;
public final class CatAutoLaserBlock extends KineticBlock implements IBE<CatAutoLaserBlockEntity> {

    public static boolean hitsFilter(BlockState state,net.minecraft.world.phys.BlockHitResult hit){
        return CatAutoLaserFilterSlot.hits(state,hit);
    }
    private net.minecraft.world.InteractionResult interactFilter(BlockState state,Level level,BlockPos pos,
            net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
        var held=player.getItemInHand(hand);
        if(!player.mayBuild()||!hitsFilter(state,hit)||!(level.getBlockEntity(pos) instanceof CatAutoLaserBlockEntity be))
            return net.minecraft.world.InteractionResult.PASS;
        if(!held.isEmpty()&&!held.is(cn.laowu.mod.LaoWuMod.CREATURE_FILTER.get()))return net.minecraft.world.InteractionResult.PASS;
        if(held.isEmpty()&&!be.hasFilter())return net.minecraft.world.InteractionResult.PASS;
        if(!level.isClientSide){
            var old=be.getFilter();
            if(held.isEmpty()){be.setFilter(net.minecraft.world.item.ItemStack.EMPTY);player.setItemInHand(hand,old);}
            else {
                be.setFilter(held);
                if(!player.getAbilities().instabuild)held.shrink(1);
                if(!old.isEmpty()&&!player.getInventory().add(old))player.drop(old,false);
            }
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&!level.isClientSide&&level.getBlockEntity(pos) instanceof CatAutoLaserBlockEntity be){
            popResource(level,pos,be.removeFilterOnBreak());
        }
        super.onRemove(state,level,pos,next,moving);
    }

    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,Level level,BlockPos pos,
            net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
        return interactFilter(state,level,pos,player,hand,hit).consumesAction()?net.minecraft.world.ItemInteractionResult.SUCCESS
            :net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,
            net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){
        return interactFilter(state,level,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
    }

    public CatAutoLaserBlock(Properties properties) {super(properties);registerDefaultState(defaultBlockState().setValue(CatMachineOrientation.BOTTOM,Direction.DOWN));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) {super.createBlockStateDefinition(b);b.add(CatMachineOrientation.BOTTOM);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) {return defaultBlockState().setValue(CatMachineOrientation.BOTTOM,CatMachineOrientation.placementBottom(c,false));}
    @Override public Direction.Axis getRotationAxis(BlockState state) {return CatMachineOrientation.bottom(state).getAxis();}
    @Override public boolean hasShaftTowards(LevelReader level,BlockPos pos,BlockState state,Direction face) {return face==CatMachineOrientation.bottom(state);}
    @Override protected boolean areStatesKineticallyEquivalent(BlockState a,BlockState b) {return a.equals(b);}
    @Override public BlockState getRotatedBlockState(BlockState state,Direction face) {return state.setValue(CatMachineOrientation.BOTTOM,CatMachineOrientation.bottom(state).getClockWise(face.getAxis()));}
    @Override public Class<CatAutoLaserBlockEntity> getBlockEntityClass() {return CatAutoLaserBlockEntity.class;}
    @Override public BlockEntityType<? extends CatAutoLaserBlockEntity> getBlockEntityType() {return CatMachineBlocks.AUTO_LASER_BE.get();}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {return CatMachineOrientation.rotateShape(box(0,0,0,16,12,16),CatMachineOrientation.bottom(state));}
}
