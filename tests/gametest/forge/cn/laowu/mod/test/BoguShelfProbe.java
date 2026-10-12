package cn.laowu.mod.test;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("laowu") @PrefixGameTestTemplate(false)
public final class BoguShelfProbe {

 private static net.minecraft.world.entity.player.Player player(GameTestHelper h) {
  return net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"bogu-probe"));
 }
 private static net.minecraft.core.BlockPos place(GameTestHelper h,net.minecraft.core.Direction facing,int x) {
  var root=h.absolutePos(new net.minecraft.core.BlockPos(x,2,5));var p=player(h);
  p.setYRot(facing.getOpposite().toYRot());p.setPos(root.getX()+.5,root.getY(),root.getZ()-2);
  h.getLevel().setBlock(root.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
  p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(cn.laowu.mod.LaoWuMod.BOGU_SHELF_ITEM.get()));
  var result=p.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(p,net.minecraft.world.InteractionHand.MAIN_HAND,
   new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(root.below()),net.minecraft.core.Direction.UP,root.below(),false)));
  h.assertTrue(result.consumesAction(),"Actual BlockItem placement succeeds for "+facing);
  return root;
 }
 @GameTest(template="artillery_probe",timeoutTicks=40)
 public static void fourDirectionsOccupyExactlyNineCells(GameTestHelper h) {
  int x=5;
  for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL) {
   var root=place(h,direction,x);x+=10;
   for(int i=0;i<9;i++) {
    var at=cn.laowu.mod.block.BoguShelfBlock.partPos(root,direction,i);var s=h.getLevel().getBlockState(at);
    h.assertTrue(s.is(cn.laowu.mod.LaoWuMod.BOGU_SHELF.get())&&s.getValue(cn.laowu.mod.block.BoguShelfBlock.PART)==i&&s.getValue(cn.laowu.mod.block.BoguShelfBlock.FACING)==direction,"All nine parts face consistently");
    var shape=s.getShape(h.getLevel(),at);
    h.assertTrue(!shape.isEmpty()&&shape.bounds().minX>=-1e-8&&shape.bounds().maxX<=1+1e-8&&shape.bounds().minY>=0&&shape.bounds().maxY<=1+1e-8,"Collision stays in local cell");
    h.assertTrue(s.getPistonPushReaction()==net.minecraft.world.level.material.PushReaction.BLOCK,"Structure cannot be pushed by piston");
   }
   h.getLevel().removeBlock(root,false);
   h.assertTrue(!cn.laowu.mod.block.BoguShelfBlock.complete(h.getLevel(),root,direction),"Whole structure removed");
  }h.succeed();
 }
 @GameTest(template="artillery_probe",timeoutTicks=40)
 public static void obstaclesRejectWholePlacement(GameTestHelper h) {
  var p=player(h);p.setYRot(180);var root=h.absolutePos(new net.minecraft.core.BlockPos(5,2,5));
  h.getLevel().setBlock(root.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
  var obstruction=root.offset(-1,2,0);h.getLevel().setBlock(obstruction,net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
  p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(cn.laowu.mod.LaoWuMod.BOGU_SHELF_ITEM.get()));
  var result=p.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(p,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(root.below()),net.minecraft.core.Direction.UP,root.below(),false)));
  h.assertTrue(!result.consumesAction()&&p.getMainHandItem().getCount()==1,"Blocked placement consumes nothing");
  h.assertTrue(h.getLevel().getBlockState(root).isAir()&&h.getLevel().getBlockState(obstruction).is(net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK),"No partial shelf or overwritten obstacle");h.succeed();
 }
 @GameTest(template="artillery_probe",timeoutTicks=40)
 public static void breakingAnyPartDropsExactlyOneItem(GameTestHelper h) {
  for(int i=0;i<9;i++) {
   var root=place(h,net.minecraft.core.Direction.NORTH,5);var at=cn.laowu.mod.block.BoguShelfBlock.partPos(root,net.minecraft.core.Direction.NORTH,i);
   h.getLevel().destroyBlock(at,true);
   for(int k=0;k<9;k++)h.assertTrue(h.getLevel().getBlockState(cn.laowu.mod.block.BoguShelfBlock.partPos(root,net.minecraft.core.Direction.NORTH,k)).isAir(),"No orphan part after breaking "+i);
   var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(root).inflate(5),e->e.getItem().is(cn.laowu.mod.LaoWuMod.BOGU_SHELF_ITEM.get()));
   h.assertTrue(drops.stream().mapToInt(e->e.getItem().getCount()).sum()==1,"Exactly one shelf drop for part "+i);
   drops.forEach(net.minecraft.world.entity.Entity::discard);
  }
  var root=place(h,net.minecraft.core.Direction.NORTH,5);h.getLevel().destroyBlock(root,false);
  h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(root).inflate(5)).isEmpty(),"Creative-style break produces no loot");
  h.succeed();
 }
 @GameTest(template="artillery_probe",timeoutTicks=60)
 public static void catSeatsHissesAndRestoresWithoutDuplication(GameTestHelper h) {
  var root=place(h,net.minecraft.core.Direction.NORTH,5);var p=player(h);p.setPos(root.getX()+.5,root.getY(),root.getZ()-2);
  var cat=h.spawn(net.minecraft.world.entity.EntityType.CAT,5,2,3);cat.setOrderedToSit(false);
  var block=(cn.laowu.mod.block.BoguShelfBlock)cn.laowu.mod.LaoWuMod.BOGU_SHELF.get();var s=h.getLevel().getBlockState(root);
  var fixtureSeat=cn.laowu.mod.LaoWuMod.BOGU_SHELF_SEAT.get().create(h.getLevel());fixtureSeat.configure(root,net.minecraft.core.Direction.NORTH,cat);h.getLevel().addFreshEntity(fixtureSeat);cat.startRiding(fixtureSeat,true);fixtureSeat.hold(cat);
  h.assertTrue(cat.getVehicle() instanceof cn.laowu.mod.entity.BoguShelfSeat,"Real cat mounted in shelf; hand="+p.getMainHandItem()+", distance="+cat.distanceToSqr(p)+", giant="+cn.laowu.mod.CatGiantMount.active(cat)+", viewer="+cn.laowu.mod.CatProfileData.isBeingViewed(cat)+", owner="+cat.isOwnedBy(p)+", pos="+cat.position()+" player="+p.position());
  var seat=(cn.laowu.mod.entity.BoguShelfSeat)cat.getVehicle();
  h.assertTrue(!cat.isOrderedToSit()&&cat.isInSittingPose()&&cn.laowu.mod.CatPoseData.getPose(cat)==0,"Display sits without changing actual command");
  seat.hissBriefly();
  cn.laowu.mod.entity.BoguShelfSeat.tickCat(cat);
  h.assertTrue(cn.laowu.mod.CatPoseData.isHissing(cat)&&!cat.isInSittingPose(),"Explicit exhibit hissing is not ambient pet attraction");
  var save=new net.minecraft.nbt.CompoundTag();seat.save(save);
  h.assertTrue(save.getList("Passengers",10).size()==1&&save.getLong("Shelf")==root.asLong()&&!save.getBoolean("WasOrdered"),"Persist real passenger and previous order");
  var rebuilt=net.minecraft.world.entity.EntityType.loadEntityRecursive(save,h.getLevel(),e->e);
  h.assertTrue(rebuilt instanceof cn.laowu.mod.entity.BoguShelfSeat&&((cn.laowu.mod.entity.BoguShelfSeat)rebuilt).cat()!=null,"NBT restores root and passenger");
  var restored=(cn.laowu.mod.entity.BoguShelfSeat)rebuilt;var restoredCat=restored.cat();restored.release();
  h.assertTrue(!restored.hissing(),"Brief display hiss is not resumed after save/load");
  h.assertTrue(!restoredCat.isOrderedToSit(),"Saved original order restored on detach");
  var box=new net.minecraft.world.item.ItemStack(cn.laowu.mod.LaoWuMod.CAT_STORAGE_BOX.get());
  ((cn.laowu.mod.item.CatStorageBoxItem)box.getItem()).interactLivingEntity(box,p,cat,net.minecraft.world.InteractionHand.MAIN_HAND);
  h.assertTrue(!cat.isRemoved()&&cn.laowu.mod.item.CatStorageBoxItem.count(box)==0,"Cannot snapshot forced shelf sitting into storage");
  h.getLevel().destroyBlock(root,true);
  h.assertTrue(!cat.isPassenger()&&!cat.isOrderedToSit()&&!cat.isInSittingPose()&&cn.laowu.mod.CatPoseData.getPose(cat)==0,"Break releases and restores original cat pose");
  h.assertTrue(!cat.isRemoved()&&h.getLevel().noCollision(cat),"Cat remains alive in clear space");h.succeed();
 }
 @GameTest(template="artillery_probe",timeoutTicks=40)
 public static void originalSittingOrderIsRestored(GameTestHelper h) {
  var root=place(h,net.minecraft.core.Direction.NORTH,5);
  var cat=h.spawn(net.minecraft.world.entity.EntityType.CAT,5,2,3);cat.setOrderedToSit(true);
  var seat=cn.laowu.mod.LaoWuMod.BOGU_SHELF_SEAT.get().create(h.getLevel());
  seat.configure(root,net.minecraft.core.Direction.NORTH,cat);h.getLevel().addFreshEntity(seat);cat.startRiding(seat,true);seat.hold(cat);seat.release();
  h.assertTrue(!cat.isPassenger()&&cat.isOrderedToSit(),"Restore pre-existing order on detach");h.succeed();
 }

 @GameTest(template="artillery_probe",timeoutTicks=40)
 public static void blockedFrontAndCeilingReleaseToClearRear(GameTestHelper h) {
  var root=place(h,net.minecraft.core.Direction.NORTH,5);
  var cat=h.spawn(net.minecraft.world.entity.EntityType.CAT,5,2,3);
  var seat=cn.laowu.mod.LaoWuMod.BOGU_SHELF_SEAT.get().create(h.getLevel());
  seat.configure(root,net.minecraft.core.Direction.NORTH,cat);h.getLevel().addFreshEntity(seat);cat.startRiding(seat,true);seat.hold(cat);
  for(int x=-3;x<=3;x++)for(int y=0;y<=3;y++)
   h.getLevel().setBlock(root.offset(x,y,-1),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
  for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++) {
   h.getLevel().setBlock(root.offset(x,3,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
   h.getLevel().setBlock(root.offset(x,-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
  }
  seat.release();
  h.assertTrue(!cat.isPassenger()&&h.getLevel().noCollision(cat),"Release never teleports into low ceiling when the front is blocked");
  h.assertTrue(cat.getY()<root.getY()+3,"Release uses a ground-level exit, not an unchecked roof coordinate");h.succeed();
 }
 @GameTest(template="artillery_probe",timeoutTicks=40)
 public static void wildCatSnapshotDoesNotSaveDisplaySitting(GameTestHelper h) {
  var root=place(h,net.minecraft.core.Direction.NORTH,5);
  var cat=h.spawn(net.minecraft.world.entity.EntityType.CAT,5,2,3);cat.setOrderedToSit(false);
  var seat=cn.laowu.mod.LaoWuMod.BOGU_SHELF_SEAT.get().create(h.getLevel());
  seat.configure(root,net.minecraft.core.Direction.NORTH,cat);h.getLevel().addFreshEntity(seat);cat.startRiding(seat,true);seat.hold(cat);
  h.assertTrue(cat.isInSittingPose()&&!cat.isOrderedToSit(),"Display sitting is independent from stay command before capture");
  var snapshot=cn.laowu.mod.item.CatPancakeItem.capture(cat);
  var tag=snapshot.getTag();
  h.assertTrue(!tag.getCompound(cn.laowu.mod.item.CatPancakeItem.CAT_DATA_TAG).getBoolean("Sitting"),"Pouch snapshot retains original wild-cat order, not display sitting");
  seat.release();cat.discard();
  var restored=cn.laowu.mod.item.CatPancakeItem.deployCat(h.getLevel(),snapshot,net.minecraft.world.phys.Vec3.atBottomCenterOf(root.offset(0,0,2)),0);
  h.assertTrue(restored!=null&&!restored.isOrderedToSit(),"Restored wild cat remains free");h.succeed();
 }
 @GameTest(template="artillery_probe",timeoutTicks=30)
 public static void shelfExistsWithoutCreate(GameTestHelper h) {
  h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(new net.minecraft.resources.ResourceLocation("laowu","round_bogu_shelf")),"Bogu shelf must register with or without Create");
  h.succeed();
 }
}
