package cn.laowu.mod.test;
import cn.laowu.mod.*;
import cn.laowu.mod.block.BoguShelfBlock;
import cn.laowu.mod.entity.BoguShelfSeat;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.UUID;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("laowu") @PrefixGameTestTemplate(false)
public final class BoguShelfHissProbe {
 private static float relativeHeadYaw(Fixture a){return net.minecraft.util.Mth.wrapDegrees(a.cat.getYHeadRot()-a.seat.getYRot());}
 private static long hissStart(BoguShelfSeat seat){
  try{var f=BoguShelfSeat.class.getDeclaredField("hissUntil");f.setAccessible(true);return f.getLong(seat)-10;}
  catch(ReflectiveOperationException e){throw new AssertionError(e);}
 }
 private static final class Fixture {
  final Cat cat;final BoguShelfSeat seat;final ServerPlayer player;
  Fixture(GameTestHelper h){
   var l=h.getLevel();var root=h.absolutePos(new BlockPos(16,2,16));
   for(int x=10;x<23;x++)for(int z=10;z<23;z++){
    h.setBlock(new BlockPos(x,1,z),Blocks.STONE);
    for(int y=2;y<9;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
   }
   player=new ServerPlayer(l.getServer(),l,new GameProfile(UUID.randomUUID(),"shelf-gaze"),net.minecraft.server.level.ClientInformation.createDefault());
   player.connection=net.neoforged.neoforge.common.util.FakePlayerFactory.get(l,new GameProfile(UUID.randomUUID(),"shelf-connection")).connection;
   player.setGameMode(GameType.CREATIVE);player.setNoGravity(true);l.addNewPlayer(player);
   var block=(BoguShelfBlock)LaoWuMod.BOGU_SHELF.get();
   var state=block.defaultBlockState().setValue(BoguShelfBlock.FACING,Direction.SOUTH);
   l.setBlock(root,state,3);block.setPlacedBy(l,root,state,player,ItemStack.EMPTY);
   cat=EntityType.CAT.create(l);cat.setPos(root.getX(),root.getY(),root.getZ()+2);l.addFreshEntity(cat);
   seat=LaoWuMod.BOGU_SHELF_SEAT.get().create(l);seat.configure(root,Direction.SOUTH,cat);l.addFreshEntity(seat);cat.startRiding(seat,true);
   cat.setPos(seat.position());player.setPos(seat.position().add(3,2,3));seat.hold(cat);
   try{
    var f=GameTestHelper.class.getDeclaredField("testInfo");f.setAccessible(true);
    ((GameTestInfo)f.get(h)).addListener(new GameTestListener(){
     public void testStructureLoaded(GameTestInfo i){}public void testPassed(GameTestInfo i,GameTestRunner r){clean();}public void testFailed(GameTestInfo i,GameTestRunner r){clean();}
     public void testAddedForRerun(GameTestInfo previous,GameTestInfo next,GameTestRunner r){clean();}
     void clean(){cat.discard();seat.discard();player.discard();}
    });
   }catch(ReflectiveOperationException e){throw new AssertionError(e);}
  }
 }
 @GameTest(template="maodie_probe",batch="shelf_gaze",timeoutTicks=28)
 public static void nearbyPlayerTurnsOnlyShelfCatsHead(GameTestHelper h){
  var a=new Fixture(h);float body=a.seat.getYRot();
  h.runAfterDelay(8,()->{
   h.assertTrue(relativeHeadYaw(a)<-20&&relativeHeadYaw(a)>=-65,"Shelf cat looks toward right-hand player");
   h.assertTrue(a.cat.getXRot()<0&&a.cat.getXRot()>=-35,"Shelf cat looks upward without twisting");
   h.assertTrue(a.seat.getYRot()==body&&a.cat.yBodyRot==body,"Head gaze must not rotate seat or anchored body");
   a.player.setPos(a.seat.position().add(-3,0,3));a.seat.hissBriefly();
  });
  h.runAfterDelay(16,()->{
   h.assertTrue(relativeHeadYaw(a)>20,"Head follows player across the shelf during hiss");
   h.assertTrue(a.seat.getYRot()==body&&a.cat.position().distanceTo(a.seat.position())<.01,"Seat facing and passenger position remain anchored");
   h.succeed();
  });
 }
 @GameTest(template="maodie_probe",batch="shelf_one_hiss",timeoutTicks=38)
 public static void oneStrokeIsOneImmediateTenTickHissWithoutLoopSession(GameTestHelper h){
  var a=new Fixture(h);a.seat.hissBriefly();long start=hissStart(a.seat);a.seat.hissBriefly();
  h.assertTrue(start==hissStart(a.seat)&&a.seat.hissing()&&CatPoseData.isHissing(a.cat),"Same-tick duplicate does not restart one visible hiss");
  h.assertTrue(!a.cat.getPersistentData().getBoolean("LaoWuAudioSession"),"Shelf one-shot must never open delayed intro/loop audio session");
  h.runAfterDelay(8,()->h.assertTrue(a.seat.hissing()&&CatPoseData.isHissing(a.cat),"Single hiss stays continuous before deadline"));
  h.runAfterDelay(11,()->h.assertTrue(!a.seat.hissing()&&!CatPoseData.isHissing(a.cat)&&a.cat.isInSittingPose(),"The only hiss ends after ten ticks"));
  h.runAfterDelay(31,()->{
   h.assertTrue(!a.seat.hissing()&&!CatPoseData.isHissing(a.cat)&&!a.cat.getPersistentData().getBoolean("LaoWuAudioSession"),"No second action or delayed audio session resumes");
   h.succeed();
  });
 }
 @GameTest(template="maodie_probe",batch="shelf_hiss_reload",timeoutTicks=20)
 public static void savedShelfDoesNotResumeHissOrAudio(GameTestHelper h){
  var a=new Fixture(h);a.seat.hissBriefly();
  var tag=new net.minecraft.nbt.CompoundTag();a.seat.save(tag);
  var restored=(BoguShelfSeat)EntityType.loadEntityRecursive(tag,h.getLevel(),e->e);
  h.assertTrue(restored!=null&&restored.cat()!=null,"Shelf passenger loads");
  var cat=restored.cat();restored.hold(cat);
  h.assertTrue(!restored.hissing()&&hissStart(restored)<0&&!CatPoseData.isHissing(cat),"One-shot animation is not resumed from NBT");
  h.assertTrue(!cat.getPersistentData().getBoolean("LaoWuAudioSession"),"Loaded shelf cannot restart delayed loop");
  cat.discard();restored.discard();h.succeed();
 }
}
