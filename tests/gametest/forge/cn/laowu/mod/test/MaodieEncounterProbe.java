package cn.laowu.mod.test;
import cn.laowu.mod.*;
import cn.laowu.mod.block.BoguShelfBlock;
import cn.laowu.mod.entity.*;
import cn.laowu.mod.genetics.*;
import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("laowu") @PrefixGameTestTemplate(false)
public final class MaodieEncounterProbe {

 private static void difficulty(GameTestHelper h,net.minecraft.world.Difficulty mode) {
  var previous=h.getLevel().getDifficulty();
  try{var field=GameTestHelper.class.getDeclaredField("testInfo");field.setAccessible(true);
   ((GameTestInfo)field.get(h)).addListener(new GameTestListener(){
    private void cleanup(){h.getLevel().getServer().setDifficulty(previous,true);}
    public void testStructureLoaded(GameTestInfo i){}
    public void testPassed(GameTestInfo i){cleanup();}
    public void testFailed(GameTestInfo i){cleanup();}
   });
  }catch(Exception e){throw new AssertionError(e);}
  h.getLevel().getServer().setDifficulty(mode,true);
 }
 private record Fixture(BlockPos root,Cat cat,BoguShelfSeat seat,Player player) {}
 private static Fixture fixture(GameTestHelper h) {
  var root=h.absolutePos(new BlockPos(16,2,16));var l=h.getLevel();
  for(int x=-14;x<=14;x++)for(int z=-14;z<=14;z++) {
   l.setBlock(root.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);
   for(int y=0;y<6;y++)l.removeBlock(root.offset(x,y,z),false);
  }
  var block=(BoguShelfBlock)LaoWuMod.BOGU_SHELF.get();var state=block.defaultBlockState();
  l.setBlock(root,state,3);block.setPlacedBy(l,root,state,null,ItemStack.EMPTY);
  var cat=EntityType.CAT.create(l);cat.setPos(root.getX()+.5,root.getY(),root.getZ()-1);cat.setNoAi(true);cat.setOrderedToSit(false);l.addFreshEntity(cat);
  CatGenomeData.ensure(cat);CatAttributeData.ensure(cat);CatTraitData.ensure(cat);
  cat.getPersistentData().putString(CatClothesData.OUTFIT_TAG,CatOutfitType.TERMINATOR.id());
  cat.getPersistentData().putString("ScriptOpaqueState","keep:445");
  CatProfileData.openContainer(cat).setItem(4,new ItemStack(Items.NAME_TAG));
  var seat=LaoWuMod.BOGU_SHELF_SEAT.get().create(l);seat.configure(root,Direction.NORTH,cat);l.addFreshEntity(seat);cat.startRiding(seat,true);seat.hold(cat);
  var p=net.minecraftforge.common.util.FakePlayerFactory.get(l,new GameProfile(UUID.randomUUID(),"maodie-test"));p.setPos(root.getX()+.5,root.getY(),root.getZ()-2);p.setShiftKeyDown(true);
  return new Fixture(root,cat,seat,p);
 }
 private static void click(Fixture f,InteractionHand hand) {CommonEvents.onCatInteract(new net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract(f.player,hand,f.cat));}
 private static Object method(Object obj,String name) {try{return obj.getClass().getMethod(name).invoke(obj);}catch(ReflectiveOperationException e){throw new GameTestAssertException("Missing encounter behavior "+name+": "+e);}}
 private static boolean aura(Fixture f) {return (Boolean)method(f.seat,"isSummoningMaodie");}
 private static List<MaodieFighter> bosses(GameTestHelper h,Fixture f){return h.getLevel().getEntitiesOfClass(MaodieFighter.class,new AABB(f.root).inflate(14));}
 private static void third(GameTestHelper h,Fixture f) {click(f,InteractionHand.MAIN_HAND);h.runAfterDelay(10,()->click(f,InteractionHand.MAIN_HAND));h.runAfterDelay(20,()->click(f,InteractionHand.MAIN_HAND));}
 private static MaodieFighter boss(GameTestHelper h,Fixture f) {var bs=bosses(h,f);h.assertTrue(bs.size()==1,"exactly one fighter after final hiss, got "+bs.size());return bs.get(0);}

 @GameTest(template="maodie_probe",timeoutTicks=40)
 public static void thirdHissDirectlySummonsAtTenTicks(GameTestHelper h) {
  var f=fixture(h);third(h,f);
  h.runAfterDelay(21,()->{
   h.assertTrue(aura(f)&&f.cat.isAlive()&&CatPoseData.isHissing(f.cat)&&bosses(h,f).isEmpty(),"third click keeps original through final hiss");
   click(f,InteractionHand.MAIN_HAND);
  });
  h.runAfterDelay(29,()->h.assertTrue(bosses(h,f).isEmpty()&&f.cat.getVehicle()==f.seat,"no early replacement before ten ticks"));
  h.runAfterDelay(31,()->{
   var b=boss(h,f);
   h.assertTrue(f.cat.isRemoved()&&f.seat.isRemoved()&&!b.isSummoning(),"direct fighter replaces source without extra summon delay");
   h.assertTrue(!b.hasCustomName()&&!b.isCustomNameVisible(),"fighter has no custom boss name");
   h.assertTrue(b.getZ()<f.root.getZ()&&b.getZ()>f.root.getZ()-2
    &&Math.abs(b.getX()-f.root.getX()+.25)<.1,"spawn immediately in front of the displayed cat, not four blocks away");
   h.succeed();
  });
 }
 @GameTest(template="maodie_probe",timeoutTicks=25)
 public static void fastClicksAndRepeatedPendingClicksDoNotPostpone(GameTestHelper h) {
  var f=fixture(h);f.player.setShiftKeyDown(false);
  click(f,InteractionHand.MAIN_HAND);click(f,InteractionHand.MAIN_HAND);
  h.assertTrue(f.seat.maodieState().count()==1,"same tick duplicates count only once");
  h.runAfterDelay(2,()->click(f,InteractionHand.MAIN_HAND));
  h.runAfterDelay(4,()->click(f,InteractionHand.MAIN_HAND));
  h.runAfterDelay(10,()->click(f,InteractionHand.MAIN_HAND));
  h.runAfterDelay(13,()->h.assertTrue(f.cat.isAlive()&&bosses(h,f).isEmpty(),"third fast-click hiss lasts ten ticks"));
  h.runAfterDelay(15,()->{boss(h,f);h.assertTrue(f.cat.isRemoved(),"extra pending clicks do not restart hiss");h.succeed();});
 }
 @GameTest(template="maodie_probe",timeoutTicks=50)
 public static void offhandAndItemsNeverCount(GameTestHelper h) {
  var f=fixture(h);f.player.setShiftKeyDown(false);click(f,InteractionHand.MAIN_HAND);
  h.assertTrue(CatPoseData.isHissing(f.cat),"ordinary empty click hisses");
  h.runAfterDelay(11,()->h.assertTrue(!CatPoseData.isHissing(f.cat),"half-second hiss ends"));
  h.runAfterDelay(12,()->click(f,InteractionHand.OFF_HAND));
  h.runAfterDelay(23,()->{f.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));click(f,InteractionHand.MAIN_HAND);f.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);});
  h.runAfterDelay(35,()->{h.assertTrue(!aura(f)&&bosses(h,f).isEmpty()&&f.seat.maodieState().count()==1,"only empty main-hand stroke counts");h.succeed();});
 }
 @GameTest(template="maodie_probe",timeoutTicks=20)
 public static void safeSpawnUsesFrontForEveryFacing(GameTestHelper h) {
  var f=fixture(h);
  for(var direction:new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}){
   var at=cn.laowu.mod.encounter.MaodieEncounter.findBossSpawn(h.getLevel(),f.root,direction);
   h.assertTrue(at.isPresent(),"open front has safe spawn");
   var relative=at.orElseThrow().subtract(Vec3.atBottomCenterOf(f.root));
   double forward=relative.x*direction.getStepX()+relative.z*direction.getStepZ();
   double side=relative.x*direction.getClockWise().getStepX()+relative.z*direction.getClockWise().getStepZ();
   h.assertTrue(forward>=.95&&forward<=2&&Math.abs(side+.75)<.01,"close to cat's lateral seat position in front of "+direction);
  }
  h.succeed();
 }
 private static void blockFront(GameTestHelper h,Fixture f,boolean blocked){
  for(int x=-4;x<=4;x++)for(int z=-3;z<=-1;z++)for(int y=0;y<=8;y++)
   if(blocked)h.getLevel().setBlock(f.root.offset(x,y,z),Blocks.STONE.defaultBlockState(),3);
   else h.getLevel().removeBlock(f.root.offset(x,y,z),false);
 }
 @GameTest(template="maodie_probe",timeoutTicks=60)
 public static void blockedFrontNeverFallsBackBehindOrToSide(GameTestHelper h) {
  var f=fixture(h);blockFront(h,f,true);third(h,f);
  h.runAfterDelay(22,()->{
   h.assertTrue(!aura(f)&&f.cat.isAlive()&&bosses(h,f).isEmpty()&&f.seat.maodieState().count()==2,"blocked front keeps cat and third chance despite clear sides/rear");
   blockFront(h,f,false);
  });
  h.runAfterDelay(32,()->click(f,InteractionHand.MAIN_HAND));
  h.runAfterDelay(43,()->{boss(h,f);h.succeed();});
 }
 @GameTest(template="maodie_probe",timeoutTicks=40)
 public static void blockedAtCommitNeverDestroysSource(GameTestHelper h) {
  var f=fixture(h);third(h,f);h.runAfterDelay(25,()->blockFront(h,f,true));
  h.runAfterDelay(32,()->{h.assertTrue(f.cat.isAlive()&&!f.cat.isRemoved()&&bosses(h,f).isEmpty()&&!aura(f),"space rechecked after final hiss; source survives obstruction");h.succeed();});
 }
 @GameTest(template="maodie_probe",timeoutTicks=20)
 public static void unloadedFrontNeverLoadsChunks(GameTestHelper h) {
  var remote=new BlockPos(25000000,100,25000000);
  h.assertTrue(!h.getLevel().hasChunkAt(remote),"remote source starts unloaded");
  h.assertTrue(cn.laowu.mod.encounter.MaodieEncounter.findBossSpawn(h.getLevel(),remote,Direction.NORTH).isEmpty(),"no unchecked remote spawn");
  h.assertTrue(!h.getLevel().hasChunkAt(remote),"safe spawn search must not load remote chunks");h.succeed();
 }
 @GameTest(template="maodie_probe",timeoutTicks=45)
 public static void tamingDuringFinalHissPreservesCatAndSit(GameTestHelper h) {
  var f=fixture(h);third(h,f);
  h.runAfterDelay(22,()->{
   f.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COD,64));f.player.setShiftKeyDown(false);
   for(int i=0;i<30&&!f.cat.isTame();i++)f.cat.mobInteract(f.player,InteractionHand.MAIN_HAND);
   h.assertTrue(f.cat.isTame()&&f.cat.isOrderedToSit(),"actual vanilla taming succeeded");
  });
  h.runAfterDelay(34,()->{h.assertTrue(f.cat.isAlive()&&f.cat.isTame()&&f.cat.isOrderedToSit()&&!f.cat.isPassenger()&&bosses(h,f).isEmpty(),"taming cancels summon without reversing sitting");h.succeed();});
 }
 @GameTest(template="maodie_probe",timeoutTicks=40)
 public static void shelfBreakDuringFinalHissPreservesCat(GameTestHelper h) {
  var f=fixture(h);third(h,f);h.runAfterDelay(22,()->h.getLevel().removeBlock(f.root,false));
  h.runAfterDelay(34,()->{h.assertTrue(f.cat.isAlive()&&!f.cat.isRemoved()&&bosses(h,f).isEmpty(),"breaking shelf before replacement preserves cat");h.succeed();});
 }
 @GameTest(template="maodie_probe",timeoutTicks=40)
 public static void leavingSeatDuringFinalHissPreservesCat(GameTestHelper h) {
  var f=fixture(h);third(h,f);h.runAfterDelay(22,()->f.cat.stopRiding());
  h.runAfterDelay(34,()->{h.assertTrue(f.cat.isAlive()&&!f.cat.isRemoved()&&bosses(h,f).isEmpty(),"dismount cancels pending transformation");h.succeed();});
 }
 @GameTest(template="maodie_probe",timeoutTicks=45)
 public static void sourceSnapshotKeepsCommitTimeChangesAndReloadOwnership(GameTestHelper h) {
  var f=fixture(h);third(h,f);
  h.runAfterDelay(25,()->{
   f.cat.setCustomName(Component.literal("changed during final hiss"));
   f.cat.getPersistentData().putString("ScriptOpaqueState","late:446");
   var opaque=new CompoundTag();opaque.putString("opaque","nested:456");f.cat.getPersistentData().put("ThirdPartyUnknown",opaque);
  });
  h.runAfterDelay(32,()->{
   var b=boss(h,f);var snapshot=b.sourceCatSnapshot();
   h.assertTrue(snapshot.getUUID("UUID").equals(f.cat.getUUID()),"snapshot retains original ownership UUID");
   var pd=snapshot.getCompound("ForgeData");
   h.assertTrue(pd.getString("ScriptOpaqueState").equals("late:446")&&pd.getCompound("ThirdPartyUnknown").getString("opaque").equals("nested:456"),"commit preserves unknown and late data");
   var saved=new CompoundTag();b.saveWithoutId(saved);b.discard();
   // Previously saved pending-restoration fighters migrate to permanent fighters.
   saved.putBoolean("MaodieReturnPending",true);saved.putBoolean("NoAI",true);
   var copy=LaoWuMod.MAODIE_FIGHTER.get().create(h.getLevel());copy.load(saved);h.getLevel().addFreshEntity(copy);
   h.assertTrue(copy.sourceCatSnapshot().getUUID("UUID").equals(f.cat.getUUID())&&!copy.restorationPending()&&!copy.isNoAi(),"reload preserves backup without pending restore or frozen AI");
   cn.laowu.mod.encounter.MaodieEncounter.requestEnd(copy);
   h.assertTrue(!cn.laowu.mod.encounter.MaodieEncounter.tryRestore(copy)&&!copy.isRemoved()&&h.getLevel().getEntity(f.cat.getUUID())==null,"legacy restore entry points cannot turn fighter back into cat");
   h.succeed();
  });
 }
 @GameTest(template="maodie_probe",batch="maodie_peaceful_summon",timeoutTicks=45)
 public static void peacefulSummonKeepsOriginalCat(GameTestHelper h) {
  difficulty(h,net.minecraft.world.Difficulty.PEACEFUL);var f=fixture(h);third(h,f);
  h.runAfterDelay(35,()->{h.assertTrue(bosses(h,f).isEmpty()&&f.cat.isAlive()&&!f.cat.isRemoved()&&f.cat.getVehicle()==f.seat&&!aura(f),"Peaceful must reject Boss promotion without consuming the source cat");h.succeed();});
 }
 @GameTest(template="maodie_probe",batch="maodie_peaceful_switch",timeoutTicks=45)
 public static void switchingPeacefulDoesNotRestoreOriginal(GameTestHelper h) {
  difficulty(h,net.minecraft.world.Difficulty.NORMAL);var f=fixture(h);third(h,f);
  h.runAfterDelay(32,()->h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL,true));
  h.runAfterDelay(36,()->{h.assertTrue(bosses(h,f).isEmpty()&&h.getLevel().getEntity(f.cat.getUUID())==null,"Peaceful removes the permanent fighter without restoring an ordinary cat");h.succeed();});
 }
 @GameTest(template="maodie_probe",timeoutTicks=475)
 public static void noParticipantsNeverRestoresCat(GameTestHelper h) {
  var f=fixture(h);third(h,f);
  h.runAfterDelay(460,()->{h.assertTrue(boss(h,f).isAlive()&&h.getLevel().getEntity(f.cat.getUUID())==null,"more than twenty seconds without participants keeps fighter");h.succeed();});
 }
 @GameTest(template="maodie_probe",batch="maodie_peaceful_last_hiss",timeoutTicks=45)
 public static void switchingPeacefulDuringLastHissKeepsOriginalCat(GameTestHelper h) {
  difficulty(h,net.minecraft.world.Difficulty.NORMAL);var f=fixture(h);third(h,f);
  h.runAfterDelay(25,()->h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL,true));
  h.runAfterDelay(35,()->{h.assertTrue(bosses(h,f).isEmpty()&&f.cat.isAlive()&&!f.cat.isRemoved()
   &&f.cat.getVehicle()==f.seat&&!aura(f),"Difficulty switch during the final hiss must cancel promotion safely");h.succeed();});
 }
 @GameTest(template="maodie_probe",batch="maodie_peaceful_spawn_egg")
 public static void peacefulAlsoRemovesFighterWithoutShelfSource(GameTestHelper h) {
  difficulty(h,net.minecraft.world.Difficulty.NORMAL);
  var fighter=LaoWuMod.MAODIE_FIGHTER.get().create(h.getLevel());
  fighter.setNoAi(true);fighter.setPos(h.absoluteVec(new Vec3(16,2,16)));h.getLevel().addFreshEntity(fighter);
  h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL,true);
  fighter.checkDespawn();h.assertTrue(fighter.isRemoved(),"Spawn-egg/command fighters must also obey Peaceful removal");h.succeed();
 }
 @GameTest(template="maodie_probe",timeoutTicks=65)
 public static void defeatedFighterDiesWithoutRestoringCat(GameTestHelper h) {
  var f=fixture(h);third(h,f);
  h.runAfterDelay(32,()->boss(h,f).hurt(h.getLevel().damageSources().genericKill(),Float.MAX_VALUE));
  h.runAfterDelay(58,()->{h.assertTrue(bosses(h,f).isEmpty()&&h.getLevel().getEntity(f.cat.getUUID())==null,"fighter death does not recreate source");h.succeed();});
 }
 @GameTest(template="maodie_probe",timeoutTicks=65)
 public static void killCommandDoesNotRestoreCat(GameTestHelper h) {
  var f=fixture(h);third(h,f);
  h.runAfterDelay(32,()->{var b=boss(h,f);h.getLevel().getServer().getCommands().performPrefixedCommand(h.getLevel().getServer().createCommandSourceStack(),"kill "+b.getUUID());});
  h.runAfterDelay(58,()->{h.assertTrue(bosses(h,f).isEmpty()&&h.getLevel().getEntity(f.cat.getUUID())==null,"kill command ends fighter, never restores source");h.succeed();});
 }
 @GameTest(template="maodie_probe",timeoutTicks=30)
 public static void legacyGiantBossStillRestoresSavedSourceWithoutDuplication(GameTestHelper h) {
  var f=fixture(h);var snapshot=new CompoundTag();f.cat.saveAsPassenger(snapshot);f.seat.sanitizeSnapshot(snapshot);
  var b=LaoWuMod.GIANT_CAT_BOSS.get().create(h.getLevel());b.configureMaodie(snapshot,f.root);b.setPos(f.root.getX()+5.5,f.root.getY(),f.root.getZ()+.5);h.getLevel().addFreshEntity(b);
  f.cat.stopRiding();f.cat.discard();f.seat.discard();b.kill();
  h.assertTrue(h.getLevel().getEntity(f.cat.getUUID()) instanceof Cat,"old GiantCatBoss saves retain restoration contract");
  var duplicate=LaoWuMod.GIANT_CAT_BOSS.get().create(h.getLevel());duplicate.configureMaodie(snapshot,f.root);duplicate.setPos(f.root.getX()+6.5,f.root.getY(),f.root.getZ()+.5);h.getLevel().addFreshEntity(duplicate);duplicate.kill();
  h.assertTrue(h.getLevel().getEntitiesOfClass(Cat.class,new AABB(f.root).inflate(14),c->c.getUUID().equals(f.cat.getUUID())).size()==1,"legacy UUID restoration is idempotent");h.succeed();
 }
 @GameTest(template="maodie_probe",timeoutTicks=20)
 public static void oldOrdinaryBossKeepsLegacyHealthAndDeath(GameTestHelper h) {
  var f=fixture(h);var b=LaoWuMod.GIANT_CAT_BOSS.get().create(h.getLevel());b.setPos(f.root.getX()+5,f.root.getY(),f.root.getZ());h.getLevel().addFreshEntity(b);
  h.assertTrue(b.getMaxHealth()==800&&!b.isMaodie(),"ordinary legacy boss unchanged");b.kill();h.assertTrue(b.getHealth()<=0,"legacy boss dies");h.succeed();
 }
}
