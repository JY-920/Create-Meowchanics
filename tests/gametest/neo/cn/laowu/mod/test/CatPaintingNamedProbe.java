package cn.laowu.mod.test;
import cn.laowu.mod.*;
import cn.laowu.mod.entity.CatPaintingEntity;
import cn.laowu.mod.painting.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.*;
import java.util.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("laowu") @PrefixGameTestTemplate(false)
public class CatPaintingNamedProbe {
 private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h){
  var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"NamedPhoto"));
  p.getAbilities().instabuild=false;return p;
 }
 private static void name(ItemStack stack,String value){stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal(value));}
 private static void clearName(ItemStack stack){stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_NAME);}
 private static InteractionResult place(net.minecraft.server.level.ServerPlayer p,BlockPos wall,ItemStack stack){
  p.setItemInHand(InteractionHand.MAIN_HAND,stack);
  return stack.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(wall),Direction.SOUTH,wall,false)));
 }
 @GameTest(template="artillery_probe",timeoutTicks=40)
 public static void anvilRenameImmediatelyGlintsAndSelectsPhoto(GameTestHelper h){
  var p=player(h);
  for(String title:List.of("奶油","《奶油》","小米","《小米》")){
   var menu=new AnvilMenu(17,p.getInventory(),ContainerLevelAccess.NULL);
   menu.getSlot(0).set(new ItemStack(LaoWuMod.CAT_PAINTING.get()));
   menu.setItemName(title);menu.createResult();
   var result=menu.getSlot(2).getItem();
   h.assertTrue(!result.isEmpty()&&result.hasFoil(),"Actual anvil output immediately glints: "+title);
   var expected=PaintingBuiltinCatalog.named(title).orElseThrow();
   h.assertTrue(CatPaintingData.resolve(result).orElseThrow().hash().equals(expected.hash()),"Actual anvil output selects correct photo");
   clearName(result);
   h.assertTrue(!result.hasFoil()&&CatPaintingData.resolve(result).isEmpty(),"Removing special name restores a normal blank");
  }
  var ordinary=new ItemStack(LaoWuMod.CAT_PAINTING.get());name(ordinary,"奶油猫");
  h.assertTrue(!ordinary.hasFoil()&&CatPaintingData.resolve(ordinary).isEmpty(),"Partial name never activates");
  h.succeed();
 }
 @GameTest(template="artillery_probe",timeoutTicks=80)
 public static void namedPlacementPersistencePickAndDrop(GameTestHelper h){
  var p=player(h);int index=0;
  for(String title:List.of("《奶油》","小米")){
   var expected=PaintingBuiltinCatalog.named(title).orElseThrow();var anchor=h.absolutePos(new BlockPos(8+index++*10,4,5));
   var shape=new CatPaintingEntity(LaoWuMod.CAT_PAINTING_ENTITY.get(),h.getLevel());
   var data=new CatPaintingData(1,expected.hash(),expected.spec(),new UUID(0,0));
   shape.place(anchor.south(),Direction.SOUTH,data);CatPaintingProbe.support(h,shape);
   var stack=new ItemStack(LaoWuMod.CAT_PAINTING.get());name(stack,title);
   h.assertTrue(place(p,anchor,stack).consumesAction()&&stack.isEmpty(),"Named photo places and consumes exactly one");
   var all=h.getLevel().getEntitiesOfClass(CatPaintingEntity.class,new AABB(anchor).inflate(4),e->!e.isRemoved());
   h.assertTrue(all.size()==1,"Exactly one named painting");var entity=all.get(0);
   h.assertTrue(entity.getPaintingData().orElseThrow().equals(data),"Named photo is never randomized");
   var picked=entity.getPickResult();h.assertTrue(picked.hasFoil()&&CatPaintingData.resolve(picked).orElseThrow().equals(data),"Pick preserves the name, photo and glint");
   var tag=new net.minecraft.nbt.CompoundTag();entity.addAdditionalSaveData(tag);
   var restored=new CatPaintingEntity(LaoWuMod.CAT_PAINTING_ENTITY.get(),h.getLevel());restored.readAdditionalSaveData(tag);
   h.assertTrue(restored.getPaintingData().orElseThrow().equals(data)&&restored.getPickResult().hasFoil(),"Save/load retains special artwork and glint");
   entity.dropItem(p);entity.dropItem(p);entity.discard();
   var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(anchor).inflate(4));
   h.assertTrue(drops.size()==1&&drops.get(0).getItem().hasFoil(),"One glowing named drop");
   var again=drops.get(0).getItem().copy();drops.get(0).discard();
   h.assertTrue(place(p,anchor,again).consumesAction(),"Named drop can be placed again");
  }
  h.succeed();
 }
 @GameTest(template="artillery_probe",timeoutTicks=40)
 public static void namedOverridesCustomButDoesNotDestroyStoredImage(GameTestHelper h){
  var original=CatPaintingProbe.data(1,2);var stack=CatPaintingData.createStack(original);
  name(stack,"奶油");
  h.assertTrue(stack.hasFoil()&&CatPaintingData.resolve(stack).orElseThrow().hash().equals(PaintingBuiltinCatalog.named("奶油").orElseThrow().hash()),"Special name overrides printed image");
  h.assertTrue(CatPaintingData.read(stack).orElseThrow().equals(original),"Renaming does not mutate stored custom image");
  name(stack,"小米");h.assertTrue(CatPaintingData.resolve(stack).orElseThrow().spec().heightBlocks()==4,"Renaming switches named photos");
  clearName(stack);h.assertTrue(!stack.hasFoil()&&CatPaintingData.resolve(stack).orElseThrow().equals(original),"Clearing name restores existing print");
  var p=player(h);var wall=h.absolutePos(new BlockPos(8,4,5));h.getLevel().setBlock(wall,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
  var tooBig=new ItemStack(LaoWuMod.CAT_PAINTING.get());name(tooBig,"小米");
  h.assertTrue(!place(p,wall,tooBig).consumesAction()&&tooBig.getCount()==1,"Too-small wall never rerolls or consumes a named painting");
  h.succeed();
 }
}
