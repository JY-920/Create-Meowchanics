package cn.laowu.mod.test;
import cn.laowu.mod.*;
import cn.laowu.mod.block.BoguShelfBlock;
import cn.laowu.mod.entity.BoguShelfSeat;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("laowu") @PrefixGameTestTemplate(false)
public final class BoguShelfAutoProbe {
 private static BlockPos place(GameTestHelper h) {
  var root=h.absolutePos(new BlockPos(5,2,5));var block=(BoguShelfBlock)LaoWuMod.BOGU_SHELF.get();
  for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)h.getLevel().setBlock(root.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);
  h.getLevel().setBlock(root,block.defaultBlockState(),3);block.setPlacedBy(h.getLevel(),root,block.defaultBlockState(),null,net.minecraft.world.item.ItemStack.EMPTY);
  return root;
 }
 private static Cat spawn(GameTestHelper h,BlockPos root,double x,double z) {
  var cat=EntityType.CAT.create(h.getLevel());cat.setPos(root.getX()+.5+x,root.getY(),root.getZ()+.5+z);h.getLevel().addFreshEntity(cat);return cat;
 }
 private static BoguShelfSeat seat(GameTestHelper h,BlockPos root,Cat cat) {
  var s=LaoWuMod.BOGU_SHELF_SEAT.get().create(h.getLevel());s.configure(root,Direction.NORTH,cat);h.getLevel().addFreshEntity(s);cat.startRiding(s,true);s.hold(cat);return s;
 }
 private static net.minecraft.world.entity.player.Player player(GameTestHelper h) {
  return net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"bogu-auto"));
 }
 @GameTest(template="artillery_probe",timeoutTicks=50)
 public static void nearestWildCatAutomaticallyOccupiesOneSeat(GameTestHelper h) {
  var root=place(h);var far=spawn(h,root,0,-2.5);var near=spawn(h,root,0,-1.5);
  far.setNoAi(true);near.setNoAi(true);
  h.runAfterDelay(25,()->{
   var diagnostic=LaoWuMod.BOGU_SHELF_SEAT.get().create(h.getLevel());diagnostic.configure(root,Direction.NORTH,near);
   var bounds=near.getBoundingBox().move(diagnostic.position().subtract(near.position()));
   String collisions=h.getLevel().getBlockCollisions(near,bounds).iterator().hasNext()?"yes":"no";
   h.assertTrue(near.getVehicle() instanceof BoguShelfSeat&&!far.isPassenger(),"Nearest wild cat within three blocks is automatically seated; tick="+near.tickCount+", near="+near.position()+", far="+far.position()+", eligible="+BoguShelfSeat.eligible(near)+", root="+root+", complete="+BoguShelfBlock.complete(h.getLevel(),root,Direction.NORTH)+", target="+diagnostic.position()+", collisions="+collisions+", bounds="+bounds);
   h.assertTrue(h.getLevel().getEntitiesOfClass(BoguShelfSeat.class,new AABB(root).inflate(4),s->s.anchor().equals(root)).size()==1,"Repeated scans never create duplicate seats");
   h.assertTrue(near.isInSittingPose(),"Auto display starts seated");h.succeed();
  });
 }
 @GameTest(template="artillery_probe",timeoutTicks=50)
 public static void petsAndOutsideRadiusCatsAreNotAttracted(GameTestHelper h) {
  var root=place(h);var pet=spawn(h,root,0,-1);pet.setTame(true,true);pet.setOwnerUUID(java.util.UUID.randomUUID());pet.setNoAi(true);
  var outside=spawn(h,root,3.2,0);outside.setNoAi(true);
  var holder=player(h);holder.setPos(root.getX()+.5,root.getY(),root.getZ()-2);
  var leashed=spawn(h,root,0,-1.5);leashed.setNoAi(true);leashed.setLeashedTo(holder,false);
  h.runAfterDelay(25,()->{h.assertTrue(!pet.isPassenger()&&!outside.isPassenger()&&!leashed.isPassenger(),"Pets, leashed cats and cats outside the spherical three-block range stay free");h.succeed();});
 }
 @GameTest(template="artillery_probe",timeoutTicks=40)
 public static void shelfRightClickDoesNotPlaceOrToggleCats(GameTestHelper h) {
  var root=place(h);var cat=spawn(h,root,0,-1.5);var p=player(h);p.setPos(root.getX()+.5,root.getY(),root.getZ()-1);
  var block=(BoguShelfBlock)LaoWuMod.BOGU_SHELF.get();
  var result=block.interact(h.getLevel().getBlockState(root),h.getLevel(),root,p,InteractionHand.MAIN_HAND);
  h.assertTrue(result==InteractionResult.PASS&&!cat.isPassenger(),"Shelf has no manual interaction");h.succeed();
 }
 @GameTest(template="artillery_probe",timeoutTicks=40)
 public static void catRightClickHissesForTenTicksThenSits(GameTestHelper h) {
  var root=place(h);var cat=spawn(h,root,0,-1);seat(h,root,cat);var p=player(h);
  var event=new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract(p,InteractionHand.MAIN_HAND,cat);
  CommonEvents.onCatInteract(event);
  h.assertTrue(event.isCanceled()&&CatPoseData.isHissing(cat),"Empty-hand click on display cat triggers hissing");
  h.runAfterDelay(9,()->h.assertTrue(CatPoseData.isHissing(cat),"Hiss persists through tick nine"));
  h.runAfterDelay(11,()->{h.assertTrue(!CatPoseData.isHissing(cat)&&cat.isInSittingPose(),"Hiss automatically ends after ten ticks");h.succeed();});
 }
 @GameTest(template="artillery_probe",timeoutTicks=30)
 public static void neighboringMovingPistonBlocksAttraction(GameTestHelper h) {
  var root=place(h);var cat=spawn(h,root,0,-1.5);cat.setNoAi(true);
  var at=root.offset(-1,1,1);var moving=Blocks.MOVING_PISTON.defaultBlockState().setValue(net.minecraft.world.level.block.piston.MovingPistonBlock.FACING,Direction.SOUTH);
  h.getLevel().setBlock(at,moving,3);
  h.getLevel().setBlockEntity(net.minecraft.world.level.block.piston.MovingPistonBlock.newMovingBlockEntity(at,moving,Blocks.STONE.defaultBlockState(),Direction.SOUTH,true,false));
  var diagnostic=LaoWuMod.BOGU_SHELF_SEAT.get().create(h.getLevel());diagnostic.configure(root,Direction.NORTH,cat);
  var bounds=cat.getBoundingBox().move(diagnostic.position().subtract(cat.position()));
  h.assertTrue(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(moving.getCollisionShape(h.getLevel(),at).move(at.getX(),at.getY(),at.getZ()),net.minecraft.world.phys.shapes.Shapes.create(bounds),net.minecraft.world.phys.shapes.BooleanOp.AND),"Adjacent piston fixture genuinely extends into the display destination");
  ((BoguShelfBlock)LaoWuMod.BOGU_SHELF.get()).attract(h.getLevel(),root,h.getLevel().getBlockState(root));
  h.assertTrue(!cat.isPassenger(),"External geometry protruding from neighboring cells blocks attraction");
  h.getLevel().removeBlock(at,false);h.succeed();
 }
 @GameTest(template="artillery_probe",timeoutTicks=30)
 public static void tamingReleasesDisplayCat(GameTestHelper h) {
  var root=place(h);var cat=spawn(h,root,0,-1);seat(h,root,cat);cat.setTame(true,true);cat.setOwnerUUID(java.util.UUID.randomUUID());
  h.runAfterDelay(3,()->{h.assertTrue(!cat.isPassenger()&&!cat.isOrderedToSit(),"Tamed cat is freed and original order restored");h.succeed();});
 }
}
