package cn.laowu.mod.test;
import cn.laowu.mod.*;
import cn.laowu.mod.entity.CatPaintingEntity;
import cn.laowu.mod.painting.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("laowu") @PrefixGameTestTemplate(false)
public class CatPaintingBuiltinProbe {
 private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h){
  var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"GalleryProbe"));
  p.getAbilities().instabuild=false;return p;
 }
 private static InteractionResult place(net.minecraft.server.level.ServerPlayer p,BlockPos wall,ItemStack stack){
  p.setItemInHand(InteractionHand.MAIN_HAND,stack);
  return stack.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(wall),Direction.SOUTH,wall,false)));
 }
 private static List<CatPaintingEntity> paintings(GameTestHelper h,BlockPos wall){
  return h.getLevel().getEntitiesOfClass(CatPaintingEntity.class,new AABB(wall).inflate(5),e->!e.isRemoved());
 }
 private static void wall(GameTestHelper h,BlockPos anchor,int w,int height){
  var shape=new CatPaintingEntity(LaoWuMod.CAT_PAINTING_ENTITY.get(),h.getLevel());
  shape.place(anchor.south(),Direction.SOUTH,CatPaintingProbe.data(w,height));CatPaintingProbe.support(h,shape);
 }
 @GameTest(template="artillery_probe",timeoutTicks=80)
 public static void blankRandomizesAcrossFittingPictures(GameTestHelper h){
  var p=player(h);var anchor=h.absolutePos(new BlockPos(8,4,5));wall(h,anchor,4,4);
  var selected=new HashSet<String>();
  for(int i=0;i<96;i++){
   var stack=new ItemStack(LaoWuMod.CAT_PAINTING.get());
   h.assertTrue(place(p,anchor,stack).consumesAction()&&stack.isEmpty(),"Blank painting is placeable and consumed");
   var all=paintings(h,anchor);h.assertTrue(all.size()==1,"Exactly one painting added");
   var e=all.get(0);var d=e.getPaintingData().orElseThrow();selected.add(d.hash());
   var builtin=PaintingBuiltinCatalog.find(d.hash()).orElseThrow();
   h.assertTrue(d.spec().equals(builtin.spec())&&e.survives(),"Only fitting catalog dimensions");
   h.assertTrue(CatPaintingData.read(e.getPickResult()).isEmpty(),"Builtin pick returns a rerollable blank item");
   e.discard();
  }
  h.assertTrue(selected.size()>=6,"A large wall offers varied artwork rather than only the largest");
  h.succeed();
 }
 @GameTest(template="artillery_probe",timeoutTicks=80)
 public static void smallWallAndOccupiedWall(GameTestHelper h){
  var p=player(h);var anchor=h.absolutePos(new BlockPos(8,4,5));wall(h,anchor,1,1);
  var stack=new ItemStack(LaoWuMod.CAT_PAINTING.get());
  h.assertTrue(place(p,anchor,stack).consumesAction(),"Small walls still offer pictures");
  var e=paintings(h,anchor).get(0);
  h.assertTrue(e.widthBlocks()==1&&e.heightBlocks()==1,"One block wall cannot select oversize picture");
  var next=new ItemStack(LaoWuMod.CAT_PAINTING.get());
  h.assertTrue(!place(p,anchor,next).consumesAction()&&next.getCount()==1,"Overlap fails without consuming");
  e.discard();h.getLevel().setBlock(anchor,Blocks.AIR.defaultBlockState(),3);
  h.assertTrue(!place(p,anchor,next).consumesAction()&&next.getCount()==1,"Missing wall fails without consuming");
  h.succeed();
 }
 @GameTest(template="artillery_probe",timeoutTicks=40)
 public static void galleryPersistenceDropAndCustom(GameTestHelper h){
  var p=player(h);var anchor=h.absolutePos(new BlockPos(8,4,5));wall(h,anchor,4,4);
  var builtin=PaintingBuiltinCatalog.entries().get(0);
  var data=new CatPaintingData(1,builtin.hash(),builtin.spec(),new UUID(0,0));
  var entity=new CatPaintingEntity(LaoWuMod.CAT_PAINTING_ENTITY.get(),h.getLevel());
  entity.place(anchor.south(),Direction.SOUTH,data);
  var saved=new net.minecraft.nbt.CompoundTag();entity.addAdditionalSaveData(saved);
  var restored=new CatPaintingEntity(LaoWuMod.CAT_PAINTING_ENTITY.get(),h.getLevel());restored.readAdditionalSaveData(saved);
  h.assertTrue(restored.getPaintingData().orElseThrow().equals(data)&&restored.getBoundingBox().equals(entity.getBoundingBox()),"Save/load keeps chosen picture and position");
  entity.dropItem(p);entity.dropItem(p);
  var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(anchor).inflate(5));
  h.assertTrue(drops.size()==1&&CatPaintingData.read(drops.get(0).getItem()).isEmpty(),"Survival drop yields one blank painting, never duplicate artwork");
  var custom=CatPaintingProbe.data(2,3);var stack=CatPaintingData.createStack(custom);
  h.assertTrue(place(p,anchor,stack).consumesAction(),"Printed custom painting still places");
  var placed=paintings(h,anchor).get(0);
  h.assertTrue(placed.getPaintingData().orElseThrow().equals(custom)&&CatPaintingData.read(placed.getPickResult()).orElseThrow().equals(custom),"Custom image and dimensions are never randomized");
  var bad=data.tag();bad.putInt("Width",1);
  h.assertTrue(CatPaintingData.fromTag(bad).isEmpty(),"Builtin saved dimensions cannot be spoofed");
  h.succeed();
 }
 @GameTest(template="artillery_probe",timeoutTicks=40)
 public static void resizedGalleryKeepsLegacySaveBounds(GameTestHelper h){
  var author=new UUID(0,0);
  // Pinned from the actual painting.7 runtime JAR; never generate legacy hashes from current Entry.
  var shippedHashes=Map.of(
   "white_stare","93f7c8662de32871eb77ca2c23958f9eb80c64aca7f17162b8450931f93f8e91",
   "grass_cat","cb1d99b60c1ab60e73a9ea56c515618360d9c76b09baa4da2bf5edd159e023c8",
   "folded_ears","ad44bc95b4d26db17d65d004f864c83da7e0962317f5b248279ef8b1f6b1e11a",
   "cowboy_cat","437238452dfb1884187a732ad691e1e3043b6a62fe9807f01047162d55c07d33"
  );
  for(var previous:Map.of("white_stare",1,"grass_cat",2,"folded_ears",2,"cowboy_cat",4).entrySet()){
   var entry=PaintingBuiltinCatalog.entries().stream().filter(e->e.id().equals(previous.getKey())).findFirst().orElseThrow();
   var old=new net.minecraft.nbt.CompoundTag();
   old.putInt("Schema",1);old.putString("Hash",shippedHashes.get(previous.getKey()));
   old.putInt("Width",previous.getValue());old.putInt("Height",previous.getValue());
   old.putString("Mode","PIXEL");old.putString("Crop","KEEP_WIDTH");old.putUUID("Author",author);
   var anchor=h.absolutePos(new BlockPos(8,4,5));
   old.putLong("Anchor",anchor.asLong());old.putByte("Facing",(byte)Direction.SOUTH.get2DDataValue());
   var saved=new net.minecraft.nbt.CompoundTag();saved.put("Painting",old);
   var restored=new CatPaintingEntity(LaoWuMod.CAT_PAINTING_ENTITY.get(),h.getLevel());
   restored.readAdditionalSaveData(saved);
   h.assertTrue(!restored.isRemoved()&&restored.getPaintingData().isPresent(),"Old builtin must not disappear on load: "+entry.id());
   h.assertTrue(restored.widthBlocks()==previous.getValue()&&restored.heightBlocks()==previous.getValue(),"Old occupied wall area stays unchanged");
   h.assertTrue(CatPaintingData.read(restored.getPickResult()).isEmpty(),"Legacy picture can still be picked and placed as a normal blank");
   var again=new net.minecraft.nbt.CompoundTag();restored.addAdditionalSaveData(again);
   var reloaded=new CatPaintingEntity(LaoWuMod.CAT_PAINTING_ENTITY.get(),h.getLevel());reloaded.readAdditionalSaveData(again);
   h.assertTrue(reloaded.getPaintingData().equals(restored.getPaintingData())&&reloaded.getBoundingBox().equals(restored.getBoundingBox()),"Legacy data and bounding box round-trip");
   old.putInt("Width",8);
   h.assertTrue(CatPaintingData.fromTag(old).isEmpty(),"Legacy compatibility never permits arbitrary widths");
  }
  h.succeed();
 }
}

