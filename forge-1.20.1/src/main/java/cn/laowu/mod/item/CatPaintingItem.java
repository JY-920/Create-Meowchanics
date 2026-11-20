package cn.laowu.mod.item;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.entity.CatPaintingEntity;
import cn.laowu.mod.painting.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

public class CatPaintingItem extends Item {
 public CatPaintingItem(Properties properties){super(properties.stacksTo(1));}
 @Override public boolean isFoil(ItemStack stack){return CatPaintingData.namedEntry(stack).isPresent()||super.isFoil(stack);}
 @Override public InteractionResult useOn(UseOnContext context){
  var level=context.getLevel();var player=context.getPlayer();var stack=context.getItemInHand();
  Direction facing=context.getClickedFace();var data=CatPaintingData.resolve(stack);
  if(player==null || facing.getAxis().isVertical())return InteractionResult.FAIL;
  if(data.isEmpty() && CatPaintingData.hasStoredPainting(stack))return InteractionResult.FAIL;
  if(level.isClientSide)return InteractionResult.SUCCESS;
  var candidates=new ArrayList<CatPaintingEntity>();
  if(data.isPresent()){
   var entity=placement(context,data.get());if(entity!=null)candidates.add(entity);
  }else{
   for(var builtin:PaintingBuiltinCatalog.entries()){
    var entity=placement(context,new CatPaintingData(1,builtin.hash(),builtin.spec(),new UUID(0,0)));
    if(entity!=null)candidates.add(entity);
   }
  }
  if(candidates.isEmpty())return InteractionResult.FAIL;
  // Equal chance among all pictures fitting this wall, not only the largest size.
  var entity=candidates.get(level.random.nextInt(candidates.size()));
  if(!level.addFreshEntity(entity))return InteractionResult.FAIL;
  entity.playPlacementSound();
  level.gameEvent(player,net.minecraft.world.level.gameevent.GameEvent.ENTITY_PLACE,entity.position());
  if(!player.getAbilities().instabuild)stack.shrink(1);
  return InteractionResult.CONSUME;
 }
 private static CatPaintingEntity placement(UseOnContext context,CatPaintingData data){
  var level=context.getLevel();var player=context.getPlayer();var stack=context.getItemInHand();var facing=context.getClickedFace();
  var entity=new CatPaintingEntity(LaoWuMod.CAT_PAINTING_ENTITY.get(),level);
  entity.place(context.getClickedPos().relative(facing),facing,data);
  for(BlockPos p:entity.occupiedAndSupport())
   if(!level.hasChunkAt(p)||!player.mayUseItemAt(p,facing,stack)||!level.mayInteract(player,p))return null;
  return entity.survives()?entity:null;
 }
 @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){
  var data=CatPaintingData.resolve(stack);
  if(data.isPresent()){
   PaintingBuiltinCatalog.find(data.get().hash()).ifPresent(e->lines.add(Component.translatable(e.titleKey())));
   var s=data.get().spec();lines.add(Component.literal(s.widthBlocks()+" × "+s.heightBlocks()+" · "+s.mode().name()));
  }else{
   lines.add(Component.translatable("painting.laowu.blank"));
  }
 }
}

