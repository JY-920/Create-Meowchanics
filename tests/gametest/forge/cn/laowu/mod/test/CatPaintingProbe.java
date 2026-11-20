package cn.laowu.mod.test;
import cn.laowu.mod.*;
import cn.laowu.mod.entity.CatPaintingEntity;
import cn.laowu.mod.painting.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

import net.minecraftforge.gametest.*;
@GameTestHolder("laowu") @PrefixGameTestTemplate(false)
public class CatPaintingProbe {
 @GameTest(template="artillery_probe",timeoutTicks=40)
 public static void paintingRegistration(GameTestHelper h){
  h.assertTrue(BuiltInRegistries.ITEM.containsKey(LaoWuMod.id("cat_painting")),"Core painting item registered");
  h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(LaoWuMod.id("cat_painting")),"Core painting entity registered");
  h.assertTrue(BuiltInRegistries.BLOCK.containsKey(LaoWuMod.id("cat_painting_table"))==cn.laowu.mod.compat.create.CreateIntegration.isLoaded(),"Table follows optional Create");
  h.succeed();
 }
 static CatPaintingData data(int w,int height){return new CatPaintingData(1,"a".repeat(64),new PaintingSpec(w,height,PaintingSpec.Mode.PIXEL,PaintingSpec.Crop.KEEP_WIDTH),new UUID(10,20));}
 @GameTest(template="artillery_probe",timeoutTicks=40)
 public static void paintingItemData(GameTestHelper h){
  var d=data(2,3);var stack=CatPaintingData.createStack(d);
  h.assertTrue(CatPaintingData.read(stack).orElseThrow().equals(d),"Item roundtrip");
  h.assertTrue(CatPaintingData.read(new ItemStack(LaoWuMod.CAT_PAINTING.get())).isEmpty(),"Blank painting stays blank");
  var tag=d.tag();tag.putInt("Width",9);h.assertTrue(CatPaintingData.fromTag(tag).isEmpty(),"Invalid dimensions rejected");
  tag=d.tag();tag.putString("Hash","../../outside");h.assertTrue(CatPaintingData.fromTag(tag).isEmpty(),"Invalid hash rejected");
  h.succeed();
 }
 static void support(GameTestHelper h,CatPaintingEntity entity){
  var b=entity.supportBox();
  for(var p:BlockPos.betweenClosed(BlockPos.containing(b.minX,b.minY,b.minZ),BlockPos.containing(b.maxX,b.maxY,b.maxZ)))h.getLevel().setBlock(p,Blocks.STONE.defaultBlockState(),3);
 }
 @GameTest(template="artillery_probe",timeoutTicks=40)
 public static void paintingWallGeometry(GameTestHelper h){
  int i=0;
  for(Direction face:Direction.Plane.HORIZONTAL)for(int[] size:new int[][]{{1,1},{2,2},{8,1},{1,8}}){
   var anchor=h.absolutePos(new BlockPos(12,4,6));i++;
   var entity=new CatPaintingEntity(LaoWuMod.CAT_PAINTING_ENTITY.get(),h.getLevel());entity.place(anchor,face,data(size[0],size[1]));
   support(h,entity);
   StringBuilder diagnostics=new StringBuilder();
   for(var p:entity.occupiedAndSupport())if(!h.getLevel().hasChunkAt(p))diagnostics.append(" unloaded=").append(p);
   diagnostics.append(" collisionFree=").append(h.getLevel().noCollision(entity)).append(" bounds=").append(entity.getBoundingBox()).append(" support=").append(entity.supportBox());
   h.assertTrue(entity.survives(),"Full wall supports "+Arrays.toString(size)+" facing "+face+diagnostics);
   var save=new net.minecraft.nbt.CompoundTag();entity.addAdditionalSaveData(save);
   var restored=new CatPaintingEntity(LaoWuMod.CAT_PAINTING_ENTITY.get(),h.getLevel());restored.readAdditionalSaveData(save);
   h.assertTrue(restored.getBoundingBox().equals(entity.getBoundingBox()),"Reload keeps even-size centering");
   var support=entity.supportBox();BlockPos missing=BlockPos.containing(support.minX,support.minY,support.minZ);h.getLevel().setBlock(missing,Blocks.AIR.defaultBlockState(),3);
   h.assertTrue(!entity.survives(),"A missing wall block rejects full painting");
   for(var p:BlockPos.betweenClosed(BlockPos.containing(support.minX,support.minY,support.minZ),BlockPos.containing(support.maxX,support.maxY,support.maxZ)))h.getLevel().setBlock(p,Blocks.AIR.defaultBlockState(),3);
  }
  h.succeed();
 }
 @GameTest(template="artillery_probe",timeoutTicks=40)
 public static void paintingPlacementConsumption(GameTestHelper h){
  var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PaintingProbe"));
  player.getAbilities().instabuild=false;
  var stack=CatPaintingData.createStack(data(1,1));player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
  var wall=h.absolutePos(new BlockPos(3,3,3));h.getLevel().setBlock(wall,Blocks.STONE.defaultBlockState(),3);
  var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(wall),Direction.SOUTH,wall,false);
  var result=stack.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
  h.assertTrue(result.consumesAction()&&stack.isEmpty(),"Successful survival placement consumes one");
  var second=CatPaintingData.createStack(data(1,1));player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,second);
  second.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
  h.assertTrue(second.getCount()==1,"Conflicting placement never consumes");
  player.getAbilities().instabuild=true;
  var other=wall.east(3);h.getLevel().setBlock(other,Blocks.STONE.defaultBlockState(),3);
  var creativeHit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(other),Direction.SOUTH,other,false);
  second.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,creativeHit));
  h.assertTrue(second.getCount()==1,"Creative placement preserves item");
  h.succeed();
 }
}
