package cn.laowu.mod.create;
import com.simibubi.create.AllShapes;
import net.minecraft.core.*;import net.minecraft.network.chat.Component;
import net.minecraft.world.*;import net.minecraft.world.entity.player.Player;import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;import net.minecraft.world.level.block.state.properties.*;import net.minecraft.world.phys.BlockHitResult;import net.minecraft.world.phys.shapes.*;

public class CatPaintingTableBlock extends HorizontalDirectionalBlock implements EntityBlock {
 
 public CatPaintingTableBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
 @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return AllShapes.SCHEMATICS_TABLE.get(state.getValue(FACING));}
 @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return AllShapes.TABLE_POLE_SHAPE;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CatPaintingTableBlockEntity(pos,state);}
 @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
  if(!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer server && level.getBlockEntity(pos) instanceof CatPaintingTableBlockEntity){
   var provider=new SimpleMenuProvider((id,inv,p)->new CatPaintingTableMenu(id,inv,pos),Component.translatable("block.laowu.cat_painting_table"));
   net.minecraftforge.network.NetworkHooks.openScreen(server,provider,pos);
  }
  return InteractionResult.sidedSuccess(level.isClientSide);
 }
 @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving){
  if(!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof CatPaintingTableBlockEntity be)Containers.dropContents(level,pos,be.painting);
  super.onRemove(state,level,pos,replacement,moving);
 }
}
