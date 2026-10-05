package cn.laowu.mod.create;
import cn.laowu.mod.LaoWuMod;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
public final class CreatureTransmitterBlock extends Block implements IBE<CreatureTransmitterBlockEntity>, IWrenchable {
    public CreatureTransmitterBlock(Properties p){super(p);}
    @Override public Class<CreatureTransmitterBlockEntity> getBlockEntityClass(){return CreatureTransmitterBlockEntity.class;}
    @Override public BlockEntityType<? extends CreatureTransmitterBlockEntity> getBlockEntityType(){return CreatureTransmitterRegistration.ENTITY.get();}
    @Override public boolean isSignalSource(BlockState state){return true;}
    @Override public int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){
        return getBlockEntityOptional(level,pos).map(CreatureTransmitterBlockEntity::getSignal).orElse(0);
    }
    public InteractionResult interact(Level level,BlockPos pos,Player player,InteractionHand hand) {
        return interact(level,pos,player,hand,null);
    }
    public InteractionResult interact(Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        var held=player.getItemInHand(hand);
        if(!player.mayBuild()||player.isSpectator()||com.simibubi.create.AllItems.WRENCH.isIn(held))return InteractionResult.PASS;
        var be=getBlockEntity(level,pos);if(be==null)return InteractionResult.PASS;
        if(player.isShiftKeyDown()){
            if(level.isClientSide)toggleRange(be);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(hit!=null&&CreatureTransmitterFilterSlot.hits(be.getBlockState(),hit)) {
            var filtering=be.getBehaviour(com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour.TYPE);
            if(filtering.canShortInteract(held)) {
                filtering.onShortInteract(player,hand,Direction.UP,hit);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        if(held.is(LaoWuMod.CREATURE_FILTER.get()))return InteractionResult.PASS;
        if(level.isClientSide)open(be);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    private static void open(CreatureTransmitterBlockEntity be){
        net.createmod.catnip.gui.ScreenOpener.open(new cn.laowu.mod.client.CreatureTransmitterScreen(be));
    }
    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    private static void toggleRange(CreatureTransmitterBlockEntity be){
        cn.laowu.mod.client.CreatureTransmitterRange.toggle(be);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){return interact(l,p,player,hand,hit).consumesAction()?ItemInteractionResult.SUCCESS:ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){return interact(l,p,player,InteractionHand.MAIN_HAND,hit);}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&!level.isClientSide){
            level.updateNeighborsAt(pos,this);
        }
        IBE.onRemove(state,level,pos,next);super.onRemove(state,level,pos,next,moving);
    }
}
