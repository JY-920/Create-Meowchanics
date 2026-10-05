package cn.laowu.mod.create;
import cn.laowu.mod.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
public final class CatEditorBlock extends Block implements com.simibubi.create.content.equipment.wrench.IWrenchable {
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public CatEditorBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){
        return s.getValue(FACING).getAxis()==Direction.Axis.X ? box(0,0,2,16,12,14) : box(2,0,0,14,12,16);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override public BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override public BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
    public InteractionResult open(Level level,BlockPos pos,Player player,InteractionHand hand){
        if(player.isShiftKeyDown()||com.simibubi.create.AllItems.WRENCH.isIn(player.getItemInHand(hand)))return InteractionResult.PASS;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        var cat=CatEditorMenu.select(player,pos);
        if(cat==null){player.displayClientMessage(Component.translatable("gui.laowu.cat_editor.no_cat"),true);return InteractionResult.CONSUME;}
        if(player instanceof ServerPlayer server){
            var provider=new SimpleMenuProvider((id,inv,p)->new CatEditorMenu(id,inv,cat,pos),Component.translatable("block.laowu.cat_editor"));
            server.openMenu(provider,buf->CatEditorMenu.writeOpeningData(buf,cat,pos));
        }
        return InteractionResult.CONSUME;
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        return open(l,p,player,hand).consumesAction()?ItemInteractionResult.SUCCESS:ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){return open(l,p,player,InteractionHand.MAIN_HAND);}
}
