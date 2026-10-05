package cn.laowu.mod.create;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import java.util.List;
public final class CatDeploymentBlock extends BaseEntityBlock implements com.simibubi.create.content.equipment.wrench.IWrenchable {
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public static final com.mojang.serialization.MapCodec<CatDeploymentBlock> CODEC=simpleCodec(CatDeploymentBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    public final boolean ejecting;
    public CatDeploymentBlock(Properties properties){this(properties,false);}
    public CatDeploymentBlock(Properties properties,boolean ejecting){
        super(properties);this.ejecting=ejecting;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override public BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override public BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return box(0,0,0,16,ejecting?14:13,16);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new CatDeploymentBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return createTickerHelper(t,LaoWuMod.CAT_DEPLOYMENT_BE.get(),CatDeploymentBlockEntity::tick);}
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(l.getBlockEntity(p) instanceof CatDeploymentBlockEntity be&&be.interact(player,hand).consumesAction())return net.minecraft.world.ItemInteractionResult.SUCCESS;
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        return l.getBlockEntity(p) instanceof CatDeploymentBlockEntity be?be.interact(player,InteractionHand.MAIN_HAND):InteractionResult.PASS;
    }
    @Override public List<ItemStack> getDrops(BlockState s,net.minecraft.world.level.storage.loot.LootParams.Builder params){
        var be=params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        return List.of(be instanceof CatDeploymentBlockEntity platform?platform.portableStack():new ItemStack(this));
    }
    @Override public BlockState playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){
        if(!l.isClientSide&&player.getAbilities().instabuild&&l.getBlockEntity(p) instanceof CatDeploymentBlockEntity be
                &&(!be.tank.isEmpty()||!be.catStack().isEmpty()))popResource(l,p,be.portableStack());
        return super.playerWillDestroy(l,p,s,player);
    }
    @Override public InteractionResult onWrenched(BlockState s,UseOnContext context){
        if(!context.getLevel().isClientSide)context.getLevel().setBlock(context.getClickedPos(),rotate(s,Rotation.CLOCKWISE_90),3);
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult onSneakWrenched(BlockState s,UseOnContext context){
        var l=context.getLevel();var p=context.getClickedPos();var player=context.getPlayer();
        if(!l.isClientSide&&player!=null&&l.getBlockEntity(p) instanceof CatDeploymentBlockEntity be){
            ItemStack item=be.portableStack();l.removeBlock(p,false);
            if(!player.getInventory().add(item))player.drop(item,false);
        }
        return InteractionResult.SUCCESS;
    }
}
