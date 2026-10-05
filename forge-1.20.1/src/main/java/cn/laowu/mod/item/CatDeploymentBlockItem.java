package cn.laowu.mod.item;
import cn.laowu.mod.create.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/** Create-style shift-use targeting, stored server-side rather than trusting a placement packet. */
public final class CatDeploymentBlockItem extends BlockItem {
    public CatDeploymentBlockItem(Block block,Properties properties){super(block,properties.stacksTo(1));}
    public static CompoundTag selection(ItemStack stack){return stack.hasTag()?stack.getTag().getCompound("CatDeploymentTarget"):new CompoundTag();}
    private static void select(ItemStack stack,CompoundTag selection){stack.getOrCreateTag().put("CatDeploymentTarget",selection);}
    @Override public InteractionResult useOn(UseOnContext context){
        return usePlatformOn(context);
    }
    @Override public boolean onBlockStartBreak(ItemStack stack,BlockPos pos,Player player){
        if(player.isShiftKeyDown()&&getBlock() instanceof CatDeploymentBlock b&&b.ejecting&&selection(stack).contains("Pos")){
            if(!player.level().isClientSide)select(stack,new CompoundTag());
            return true;
        }
        return false;
    }
    private InteractionResult usePlatformOn(UseOnContext context){
        var player=context.getPlayer();var level=context.getLevel();
        if(getBlock() instanceof CatDeploymentBlock b&&b.ejecting&&player!=null&&player.isShiftKeyDown()){
            if(!level.isClientSide){
                CompoundTag tag=new CompoundTag();var p=context.getClickedPos();
                tag.putIntArray("Pos",new int[]{p.getX(),p.getY(),p.getZ()});tag.putString("Dimension",level.dimension().location().toString());
                select(context.getItemInHand(),tag);
                player.displayClientMessage(Component.translatable("tooltip.laowu.cat_deployment.target",p.getX(),p.getY(),p.getZ()),true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useOn(context);
    }
    @Override public InteractionResult place(BlockPlaceContext context){
        var selection=selection(context.getItemInHand());int[] coordinates=selection.getIntArray("Pos");
        if(getBlock() instanceof CatDeploymentBlock b&&b.ejecting&&coordinates.length==3){
            var target=new BlockPos(coordinates[0],coordinates[1],coordinates[2]);
            if(!selection.getString("Dimension").equals(context.getLevel().dimension().location().toString())
                    ||!CatDeploymentBlockEntity.validTarget(context.getClickedPos(),target)){
                if(context.getPlayer()!=null)context.getPlayer().displayClientMessage(Component.translatable("tooltip.laowu.cat_deployment.invalid_target"),true);
                return InteractionResult.FAIL;
            }
        }
        return super.place(context);
    }
    @Override protected boolean updateCustomBlockEntityTag(BlockPos pos,Level level,Player player,ItemStack stack,BlockState state){
        boolean applied=super.updateCustomBlockEntityTag(pos,level,player,stack,state);
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof CatDeploymentBlockEntity be&&be.ejecting()){
            CompoundTag selection=selection(stack);int[] coordinates=selection.getIntArray("Pos");
            if(coordinates.length==3&&selection.getString("Dimension").equals(level.dimension().location().toString()))
                applied|=be.setTarget(new BlockPos(coordinates[0],coordinates[1],coordinates[2]));
        }
        return applied;
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){
        super.appendHoverText(stack,level,lines,flag);
        CompoundTag data=BlockItem.getBlockEntityData(stack);
        if(data==null)data=new CompoundTag();
        CompoundTag fluid=data.getCompound("Tank");
        int amount="laowu:hissing_gas".equals(fluid.getString("FluidName"))?Math.max(0,Math.min(4000,fluid.getInt("Amount"))):0;
        lines.add(Component.translatable("tooltip.laowu.cat_carrier.gas",amount,4000).withStyle(net.minecraft.ChatFormatting.GRAY));
        if(getBlock() instanceof CatDeploymentBlock b&&b.ejecting){
            int[] coordinates=selection(stack).getIntArray("Pos");
            if(coordinates.length==3)lines.add(Component.translatable("tooltip.laowu.cat_deployment.target",coordinates[0],coordinates[1],coordinates[2]).withStyle(net.minecraft.ChatFormatting.GREEN));
        }
    }
}
