package cn.laowu.mod.test;
import cn.laowu.mod.LaoWuMod;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("laowu") @PrefixGameTestTemplate(false)
public final class MaodieFighterProbe {
 private static class Arena {
  final GameTestHelper h; final LivingEntity cat; final ServerPlayer p; final List<Entity> all=new ArrayList<>();
  Arena(GameTestHelper h) {
   this.h=h;
   var prior=h.getLevel().getDifficulty();h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
   for(int x=0;x<32;x++)for(int z=0;z<32;z++){h.setBlock(new BlockPos(x,0,z),Blocks.STONE);for(int y=1;y<8;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);}
   var id=ResourceLocation.fromNamespaceAndPath("laowu","maodie_fighter");
   h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(id),"Independent fighter entity registered, not giant_cat_boss");
   cat=(LivingEntity)BuiltInRegistries.ENTITY_TYPE.get(id).create(h.getLevel());
   cat.setPos(at(16,16));h.getLevel().addFreshEntity(cat);all.add(cat);
   var stub=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"fighter-connection"));
   p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"fighter-victim"),net.minecraft.server.level.ClientInformation.createDefault());
   p.connection=stub.connection;
   p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);p.setHealth(1000);
   p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getAbilities().invulnerable=false;p.getFoodData().setFoodLevel(17);
   try{var protection=ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");protection.setAccessible(true);protection.setInt(p,0);}catch(Exception e){throw new AssertionError(e);}
   p.setNoGravity(true);p.setPos(at(16,18));h.getLevel().addNewPlayer(p);all.add(p);
   ((net.minecraft.world.entity.Mob)cat).setTarget(p);
   try{var f=GameTestHelper.class.getDeclaredField("testInfo");f.setAccessible(true);
    ((GameTestInfo)f.get(h)).addListener(new GameTestListener(){
     public void testStructureLoaded(GameTestInfo i){}public void testPassed(GameTestInfo i,net.minecraft.gametest.framework.GameTestRunner r){clean();}public void testFailed(GameTestInfo i,net.minecraft.gametest.framework.GameTestRunner r){clean();}public void testAddedForRerun(GameTestInfo i,GameTestInfo j,net.minecraft.gametest.framework.GameTestRunner r){}
     void clean(){for(var e:all)e.discard();h.getLevel().getServer().setDifficulty(prior,true);}
    });
   }catch(Exception e){throw new AssertionError(e);}
  }
  Vec3 at(int x,int z){return Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(x,1,z)));}
  void attack(int phase){try{cat.getClass().getMethod("beginAttack",byte.class).invoke(cat,(byte)phase);}catch(Exception e){throw new AssertionError(e);}
   h.runAfterDelay(cn.laowu.mod.entity.MaodieFighterTiming.duration((byte)phase)+1,()->((net.minecraft.world.entity.Mob)cat).setNoAi(true));
  }
 }
 @GameTest(template="maodie_probe",batch="fighter_dead_combo",timeoutTicks=32)
 public static void deathDuringComboStopsRemainingHitsAndStillRemovesCorpse(GameTestHelper h){
  var a=new Arena(h);a.attack(2);
  h.runAfterDelay(5,()->{
   h.assertTrue(a.p.getHealth()==998,"Living fighter lands first punch before lethal damage");
   a.cat.hurt(h.getLevel().damageSources().genericKill(),Float.MAX_VALUE);
   h.assertTrue(!a.cat.isAlive()&&!a.cat.isRemoved(),"Lethal damage enters real twenty-tick death animation");
  });
  h.runAfterDelay(19,()->{
   h.assertTrue(!a.cat.isRemoved(),"Corpse remains during vanilla death animation");
   h.assertTrue(a.p.getHealth()==998,"Dying fighter must not land the remaining five combo punches");
  });
  h.runAfterDelay(28,()->{h.assertTrue(a.cat.isRemoved(),"super.tick still completes vanilla death removal");h.succeed();});
 }
 @GameTest(template="maodie_probe",batch="fighter_dead_pull",timeoutTicks=28)
 public static void deathDuringPullNeverAddsTargetVelocity(GameTestHelper h){
  var a=new Arena(h);a.p.setPos(a.at(16,22));a.attack(5);
  a.cat.hurt(h.getLevel().damageSources().genericKill(),Float.MAX_VALUE);
  h.assertTrue(!a.cat.isAlive()&&!a.cat.isRemoved(),"Pull fighter has entered real death animation");
  h.runAfterDelay(14,()->{
   h.assertTrue(!a.cat.isRemoved(),"Pull checks the death-animation interval, not a discarded entity");
   h.assertTrue(a.p.getDeltaMovement().horizontalDistance()<.001,"Dying fighter cannot keep pulling its living target");
   h.assertTrue(a.p.getHealth()==1000,"Dying pull never damages target");
  });
  h.runAfterDelay(23,()->{h.assertTrue(a.cat.isRemoved(),"Dead pulling fighter completes normal removal");h.succeed();});
 }
 @GameTest(template="maodie_probe",batch="fighter_dead_orbit",timeoutTicks=28)
 public static void deathDuringBackstepNeverMovesCorpseAroundTarget(GameTestHelper h){
  var a=new Arena(h);a.p.setYRot(0);a.cat.setPos(a.at(16,20));a.attack(6);Vec3 original=a.cat.position();
  a.cat.hurt(h.getLevel().damageSources().genericKill(),Float.MAX_VALUE);
  h.assertTrue(!a.cat.isAlive()&&!a.cat.isRemoved(),"Backstep fighter has entered real death animation");
  h.runAfterDelay(14,()->{
   h.assertTrue(!a.cat.isRemoved(),"Backstep checks corpse before vanilla removal");
   h.assertTrue(a.cat.position().subtract(original).horizontalDistance()<.001,"Dying fighter cannot orbit around target");
  });
  h.runAfterDelay(23,()->{h.assertTrue(a.cat.isRemoved(),"Dead backstep fighter completes normal removal");h.succeed();});
 }
 @GameTest(template="maodie_probe",batch="fighter_size",timeoutTicks=20)
 public static void butterSizedIndependentFighter(GameTestHelper h){
  var a=new Arena(h);h.assertTrue(Math.abs(a.cat.getBbWidth()-.9)<.001&&Math.abs(a.cat.getBbHeight()-1.7)<.001,"Butter scale with taller upright collision");h.assertTrue(a.cat.getMaxHealth()==300,"Challenge health");h.succeed();
 }
 @GameTest(template="maodie_probe",batch="fighter_combo",timeoutTicks=40)
 public static void sixPunchesOnlyHitAtV11Keyframes(GameTestHelper h){
  var a=new Arena(h);a.attack(2);
  h.runAfterDelay(3,()->h.assertTrue(a.p.getHealth()==1000,"Half-speed windup has no early damage"));
  h.runAfterDelay(5,()->h.assertTrue(a.p.getHealth()==998,"First impact"));
  h.runAfterDelay(9,()->h.assertTrue(a.p.getHealth()==996,"Second impact bypasses only this combo's i-frames"));
  h.runAfterDelay(15,()->h.assertTrue(a.p.getHealth()==994,"Third impact"));
  h.runAfterDelay(21,()->h.assertTrue(a.p.getHealth()==992,"Fourth impact"));
  h.runAfterDelay(25,()->h.assertTrue(a.p.getHealth()==990,"Fifth impact"));
  h.runAfterDelay(31,()->{h.assertTrue(a.p.getHealth()==988,"Sixth impact");h.succeed();});
 }
 @GameTest(template="maodie_probe",batch="fighter_uppercut",timeoutTicks=38)
 public static void uppercutHasEightDamageAndLift(GameTestHelper h){
  var a=new Arena(h);a.attack(3);
  h.runAfterDelay(5,()->h.assertTrue(a.p.getHealth()==1000,"Uppercut telegraph"));
  h.runAfterDelay(7,()->{h.assertTrue(a.p.getHealth()==992,"Uppercut once");h.assertTrue(a.p.getDeltaMovement().y>.4,"Uppercut sends upward motion to real player");h.succeed();});
 }
 @GameTest(template="maodie_probe",batch="fighter_sweep",timeoutTicks=42)
 public static void circularAttackHitsRearButNotPets(GameTestHelper h){
  var a=new Arena(h);a.attack(4);a.p.setPos(a.at(16,13));
  var pet=EntityType.CAT.create(h.getLevel());pet.setNoAi(true);pet.setPos(a.at(17,16));
  // Join queues random traits until the first tick; settle this unrelated fixture before taking its HP baseline.
  cn.laowu.mod.genetics.CatTraitData.set(pet,cn.laowu.mod.genetics.CatTraitProfile.EMPTY);
  h.getLevel().addFreshEntity(pet);cn.laowu.mod.genetics.CatAttributeData.ensure(pet);
  cn.laowu.mod.genetics.CatAttributeEffects.refresh(pet);a.all.add(pet);float hp=pet.getHealth();
  h.runAfterDelay(9,()->{h.assertTrue(a.p.getHealth()==994,"Sweep rear player hit");h.assertTrue(pet.getHealth()==hp,
   "Pets untouched: before="+hp+" current="+pet.getHealth()+" max="+pet.getMaxHealth()+" hurtTime="+pet.hurtTime);h.succeed();});
 }
 @GameTest(template="maodie_probe",batch="fighter_wall",timeoutTicks=46)
 public static void punchesCannotHitThroughSolidWall(GameTestHelper h){
  var a=new Arena(h);for(int x=14;x<=18;x++)for(int y=1;y<=4;y++)h.setBlock(new BlockPos(x,y,17),Blocks.STONE);a.attack(2);
  h.runAfterDelay(38,()->{h.assertTrue(a.p.getHealth()==1000,"Wall blocks both fists");h.succeed();});
 }
 @GameTest(template="maodie_probe",batch="fighter_pull",timeoutTicks=42)
 public static void pullBringsTargetNearWithoutDamage(GameTestHelper h){
  var a=new Arena(h);a.p.setPos(a.at(16,22));a.attack(5);
  h.runAfterDelay(3,()->h.assertTrue(a.p.getDeltaMovement().horizontalDistance()<.001,"Pull first telegraphs its reach"));
  h.runAfterDelay(9,()->{Vec3 v=a.p.getDeltaMovement();h.assertTrue(v.dot(a.cat.position().subtract(a.p.position()))>.1&&v.horizontalDistance()<=.351,"Pull requests bounded motion toward fighter; client integrates real player movement");h.assertTrue(a.p.getHealth()==1000,"Pull doesn't punch");h.succeed();});
 }
 @GameTest(template="maodie_probe",batch="fighter_back",timeoutTicks=40)
 public static void backstepMovesBehindTargetWithoutTeleporting(GameTestHelper h){
  var a=new Arena(h);a.p.setYRot(0);a.cat.setPos(a.at(16,20));a.attack(6);Vec3 original=a.cat.position();
  final Vec3[] previous={original};
  for(int tick=1;tick<=12;tick++)h.runAfterDelay(tick,()->{h.assertTrue(a.cat.position().distanceTo(previous[0])<=1.11,"Backstep moves in bounded collision-checked steps");previous[0]=a.cat.position();});
  h.runAfterDelay(13,()->{Vec3 d=a.cat.position().subtract(a.p.position());h.assertTrue(d.z<-.7,"Arrives behind target facing +Z");h.assertTrue(a.cat.position().distanceTo(original)>1,"Actually moved");h.succeed();});
 }
 @GameTest(template="maodie_probe",batch="fighter_reload",timeoutTicks=58)
 public static void reloadingAfterLeftPunchCannotReplayHits(GameTestHelper h){
  var a=new Arena(h);a.attack(2);
  h.runAfterDelay(5,()->{
   h.assertTrue(a.p.getHealth()==998,"First punch applied before reload");
   var tag=new net.minecraft.nbt.CompoundTag();a.cat.saveWithoutId(tag);a.cat.discard();
   var restored=LaoWuMod.MAODIE_FIGHTER.get().create(h.getLevel());restored.load(tag);restored.setNoAi(true);h.getLevel().addFreshEntity(restored);a.all.add(restored);
   h.runAfterDelay(38,()->{h.assertTrue(a.p.getHealth()==998,"Reload does not replay combo punches");h.succeed();});
  });
 }
 @GameTest(template="maodie_probe",batch="fighter_obstructed_pull",timeoutTicks=35)
 public static void solidWallBlocksPullVelocity(GameTestHelper h){
  var a=new Arena(h);a.p.setPos(a.at(16,22));for(int x=12;x<=20;x++)for(int y=1;y<=4;y++)h.setBlock(new BlockPos(x,y,19),Blocks.STONE);a.attack(5);
  h.runAfterDelay(22,()->{h.assertTrue(a.p.getDeltaMovement().horizontalDistance()<.001,"Pull cannot act across wall");h.succeed();});
 }
 @GameTest(template="maodie_probe",batch="fighter_obstructed_back",timeoutTicks=35)
 public static void solidWallStopsBehindArc(GameTestHelper h){
  var a=new Arena(h);a.cat.setPos(a.at(16,20));a.p.setYRot(0);
  for(int x=13;x<=19;x++)for(int y=1;y<=4;y++)h.setBlock(new BlockPos(x,y,19),Blocks.STONE);
  a.attack(6);h.runAfterDelay(24,()->{h.assertTrue(a.cat.getZ()>=a.at(16,20).z-.6,"Blocked arc does not pass through target wall");h.assertTrue(h.getLevel().noCollision(a.cat,a.cat.getBoundingBox().deflate(.01)),"No overlap with wall");h.succeed();});
 }
 @GameTest(template="maodie_probe",batch="fighter_jab",timeoutTicks=14)
 public static void fastJabHitsOnceAndTracksTarget(GameTestHelper h){
  var a=new Arena(h);a.p.setPos(a.at(18,16));a.attack(7);
  h.runAfterDelay(3,()->h.assertTrue(a.p.getHealth()==1000,"Jab windup"));
  h.runAfterDelay(5,()->{h.assertTrue(a.p.getHealth()==996,"Jab impacts at tick four");h.assertTrue(Math.abs(((cn.laowu.mod.entity.MaodieFighter)a.cat).attackYaw()+90)<2,"Synced yaw faces opponent");h.succeed();});
 }
 @GameTest(template="maodie_probe",batch="fighter_slam",timeoutTicks=32)
 public static void jumpSlamHitsOnlyAtLandingPeak(GameTestHelper h){
  var a=new Arena(h);a.attack(9);
  h.runAfterDelay(15,()->h.assertTrue(a.p.getHealth()==1000,"Slam airborne windup does not damage"));
  h.runAfterDelay(17,()->{h.assertTrue(a.p.getHealth()==990,"Slam impacts once at tick sixteen");h.succeed();});
 }
 @GameTest(template="maodie_probe",batch="fighter_block",timeoutTicks=15)
 public static void guardReducesFrontAttackButNotRear(GameTestHelper h){
  var a=new Arena(h);a.attack(8);float before=a.cat.getHealth();
  a.cat.hurt(a.p.damageSources().playerAttack(a.p),8);
  h.assertTrue(a.cat.getHealth()==before-2,"Front guard reduces damage to a quarter");
  a.cat.invulnerableTime=0;a.p.setPos(a.at(16,14));a.cat.hurt(a.p.damageSources().playerAttack(a.p),8);
  h.assertTrue(a.cat.getHealth()==before-10,"Rear attack bypasses guard");h.succeed();
 }
 @GameTest(template="maodie_probe",batch="fighter_block_no_pull",timeoutTicks=18)
 public static void defensiveBlockNeverPullsItsTarget(GameTestHelper h){
  var a=new Arena(h);a.p.setPos(a.at(16,22));a.p.setDeltaMovement(Vec3.ZERO);a.attack(8);
  for(int tick=1;tick<=13;tick++)h.runAfterDelay(tick,()->{
   h.assertTrue(a.p.getDeltaMovement().lengthSqr()<.000001,"BLOCK must leave target velocity unchanged throughout its entire defensive action");
   h.assertTrue(a.p.getHealth()==1000,"BLOCK never deals offensive damage");
  });
  h.runAfterDelay(14,h::succeed);
 }
 @GameTest(template="maodie_probe",batch="fighter_pause",timeoutTicks=24)
 public static void completedMoveLeavesReadablePauseBeforeNextAttack(GameTestHelper h){
  var a=new Arena(h);var fighter=(cn.laowu.mod.entity.MaodieFighter)a.cat;fighter.beginAttack((byte)7);
  for(int tick=11;tick<=19;tick++)h.runAfterDelay(tick,()->{
   h.assertTrue(fighter.getPhase()==cn.laowu.mod.entity.MaodieFighter.IDLE,"Jab recovery must leave at least ten ticks before another move");
   h.assertTrue(a.p.getHealth()==996,"Readable pause must not hide extra attacks");
  });
  h.runAfterDelay(20,()->{fighter.setNoAi(true);h.succeed();});
 }
 @GameTest(template="maodie_probe",batch="fighter_creative",timeoutTicks=40)
 public static void comboCannotDamageCreativeOrSpectatorTargets(GameTestHelper h){
  var a=new Arena(h);a.attack(2);a.p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);a.p.getAbilities().invulnerable=false;
  h.runAfterDelay(15,()->{h.assertTrue(a.p.getHealth()==1000,"Creative players never receive fighter hit logic");a.p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);});
  h.runAfterDelay(33,()->{h.assertTrue(a.p.getHealth()==1000,"Spectators never receive fighter hit logic");h.succeed();});
 }
 @GameTest(template="maodie_probe",batch="fighter_noai",timeoutTicks=44)
 public static void noAiSuppressesDamageAndMovement(GameTestHelper h){
  var a=new Arena(h);a.attack(2);((net.minecraft.world.entity.Mob)a.cat).setNoAi(true);Vec3 at=a.cat.position();
  h.runAfterDelay(38,()->{h.assertTrue(a.p.getHealth()==1000&&a.cat.position().distanceTo(at)<.1,"NoAI does not execute attack");h.succeed();});
 }
 @SuppressWarnings("unchecked")
 @GameTest(template="maodie_probe",batch="fighter_combo_knockback",timeoutTicks=20)
 public static void earlyComboDoesNotStackVanillaKnockback(GameTestHelper h){
  var a=new Arena(h);a.p.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);a.attack(2);
  var fighter=(cn.laowu.mod.entity.MaodieFighter)a.cat;fighter.setNoAi(true);
  try{
   var clockField=fighter.getClass().getDeclaredField("START");clockField.setAccessible(true);
   var clock=(net.minecraft.network.syncher.EntityDataAccessor<Long>)clockField.get(null);
   var strike=fighter.getClass().getDeclaredMethod("strike",float.class,double.class,boolean.class,boolean.class);strike.setAccessible(true);
   a.p.setDeltaMovement(new Vec3(.08,.12,.015));
   int[] impacts={4,8,14,20,24,30};
   for(int i=0;i<impacts.length;i++){
    fighter.getEntityData().set(clock,h.getLevel().getGameTime()-impacts[i]);a.p.setOnGround(true);
    Vec3 before=a.p.getDeltaMovement();
    strike.invoke(fighter,2F,2.65,false,false);
    Vec3 after=a.p.getDeltaMovement(),change=after.subtract(before);
    h.assertTrue(a.p.getHealth()==1000-2*(i+1),"Zero-resistance player receives combo impact "+(i+1));
    if(i<5){
     h.assertTrue(Math.abs(change.y)<.000001,"Early combo must not add vanilla upward launch: hit="+(i+1)+" before="+before+" after="+after);
     h.assertTrue(change.horizontalDistance()<=.025001,"Early combo adds at most a .025 horizontal impulse: hit="+(i+1)+" delta="+change);
     h.assertTrue(Math.abs(after.x-before.x)<.000001&&after.z>=before.z,"Early combo preserves existing motion and only nudges away from fighter");
     h.assertTrue(a.p.hurtMarked,"Controlled early impulse is synchronized to connected players");
    }else{
     h.assertTrue(after.horizontalDistance()>.25&&after.y>.2,"Final combo punch retains a real finishing knockback");
    }
   }
   h.succeed();
  }catch(ReflectiveOperationException e){throw new AssertionError(e);}
 }
}
