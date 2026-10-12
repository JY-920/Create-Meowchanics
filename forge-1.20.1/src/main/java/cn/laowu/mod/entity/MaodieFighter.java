package cn.laowu.mod.entity;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.accessory.CatAccessoryItems;
import cn.laowu.mod.encounter.MaodieEncounter;
import cn.laowu.mod.genetics.CatGenome;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Small permanent articulated boxer; the server clock matches the approved v11 clips. */
public final class MaodieFighter extends Monster implements MaodieChallenge {
 public static final float MODEL_SCALE=1.5F;
 public static final byte SUMMON=0,IDLE=1,COMBO=2,UPPERCUT=3,SWEEP=4,PULL=5,BACKSTEP=6,JAB=7,BLOCK=8,JUMP_SLAM=9;
 private static final EntityDataAccessor<Byte> PHASE=SynchedEntityData.defineId(MaodieFighter.class,EntityDataSerializers.BYTE);
 private static final EntityDataAccessor<Long> START=SynchedEntityData.defineId(MaodieFighter.class,EntityDataSerializers.LONG);
 private static final EntityDataAccessor<Float> FACING=SynchedEntityData.defineId(MaodieFighter.class,EntityDataSerializers.FLOAT);
 private static final EntityDataAccessor<CompoundTag> GENOME=SynchedEntityData.defineId(MaodieFighter.class,EntityDataSerializers.COMPOUND_TAG);
 private static final byte[] MOVES={JAB,COMBO,BACKSTEP,UPPERCUT,SWEEP,JAB,JUMP_SLAM,BLOCK};
 private final ServerBossEvent bossEvent=new ServerBossEvent(getDisplayName(),BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.NOTCHED_10);
 private final Set<UUID> comboVictims=new HashSet<>();
 private CompoundTag source=new CompoundTag(); private BlockPos origin=BlockPos.ZERO;
 private int absence,cooldown=MaodieFighterTiming.pauseTicks(0),nextMove,blockCooldown;
 private Vec3 orbitCenter=Vec3.ZERO; private double orbitStart,orbitDelta,orbitRadius; private boolean orbitBlocked;
 public MaodieFighter(EntityType<? extends MaodieFighter> type,Level level){
  super(type,level);setPersistenceRequired();xpReward=0;
 }
 public static AttributeSupplier.Builder createAttributes(){
  return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,300).add(Attributes.ATTACK_DAMAGE,4)
   .add(Attributes.MOVEMENT_SPEED,.38).add(Attributes.FOLLOW_RANGE,32).add(Attributes.KNOCKBACK_RESISTANCE,.8);
 }
 @Override protected void defineSynchedData(){
  super.defineSynchedData();entityData.define(PHASE,IDLE);entityData.define(START,0L);
  entityData.define(FACING,0F);entityData.define(GENOME,new CompoundTag());
 }
 @Override protected void registerGoals(){
  targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this,Player.class,true));
 }
 public byte getPhase(){return entityData.get(PHASE);}
 public float getPhaseTicks(float partial){return Math.max(0,level().getGameTime()-entityData.get(START)+partial);}
 public boolean isSummoning(){return getPhase()==SUMMON;}
 public float attackYaw(){return entityData.get(FACING);}
 public Optional<CatGenome> getInheritedGenome(){var tag=entityData.get(GENOME);return tag.isEmpty()?Optional.empty():CatGenome.load(tag);}
 public void setInheritedGenome(CatGenome genome){entityData.set(GENOME,genome.save());}
 public void configureMaodie(CompoundTag tag,BlockPos root){source=tag.copy();origin=root.immutable();setCustomName(null);setCustomNameVisible(false);}
 public boolean isMaodie(){return source.hasUUID("UUID");}
 public CompoundTag sourceCatSnapshot(){return source.copy();}
 public BlockPos maodieOrigin(){return origin;}
 public boolean restorationPending(){return false;}
 public int updateMaodieAbsence(boolean present){return absence=present?0:absence+1;}
 /** Legacy challenge API: permanent fighters never turn back into ordinary cats. */
 public void markRestorationPending(){}
 private void phase(byte phase){
  entityData.set(PHASE,phase);entityData.set(START,level().getGameTime());getNavigation().stop();comboVictims.clear();
 }
 /** Shelf summoning already completed before this entity was created. */
 public void beginSummoning(){phase(IDLE);cooldown=MaodieFighterTiming.pauseTicks(0);setDeltaMovement(Vec3.ZERO);}
 /** Called by server AI; clients only render the synchronized action. */
 public void beginAttack(byte attack){
  if(level().isClientSide||isNoAi()||attack<COMBO||attack>JUMP_SLAM)return;
  var t=getTarget();
  if(validPlayer(t)){
   trackTarget(360);
   if(attack==BACKSTEP){
    Vec3 d=t.position().subtract(position());
    orbitCenter=t.position();orbitRadius=Mth.clamp(d.horizontalDistance(),1.6,2.1);
    orbitStart=Math.atan2(getZ()-t.getZ(),getX()-t.getX());
    Vec3 behind=t.getLookAngle().multiply(1,0,1).normalize().scale(-1);
    double end=Math.atan2(behind.z,behind.x);
    orbitDelta=Mth.wrapDegrees((float)Math.toDegrees(end-orbitStart))*Mth.DEG_TO_RAD;
    orbitBlocked=false;
   }
  }else entityData.set(FACING,getYRot());
  phase(attack);
  if(attack==BLOCK)blockCooldown=24;
  if(attack==BACKSTEP||attack==PULL)playSound(SoundEvents.PLAYER_ATTACK_SWEEP,.22F,1.5F);
 }
 private boolean validPlayer(LivingEntity t){return t instanceof Player p&&p.isAlive()&&!p.isSpectator()&&!p.getAbilities().instabuild;}
 private void trackTarget(float maxTurn){
  var t=getTarget();if(!validPlayer(t))return;
  Vec3 d=t.position().subtract(position());
  float desired=(float)Math.toDegrees(Math.atan2(-d.x,d.z));
  entityData.set(FACING,attackYaw()+Mth.clamp(Mth.wrapDegrees(desired-attackYaw()),-maxTurn,maxTurn));
  getLookControl().setLookAt(t,45,45);
 }
 @Override public void tick(){
  super.tick();if(isRemoved()||!isAlive())return;
  if(level().isClientSide){
   setYRot(attackYaw());yBodyRot=attackYaw();yHeadRot=attackYaw();return;
  }
  // Boss UI remains live even when commands or encounters disable combat AI.
  bossEvent.setProgress(Mth.clamp(getHealth()/getMaxHealth(),0F,1F));
  bossEvent.setName(getDisplayName());
  if(MaodieEncounter.tickBoss(this))return;
  if(blockCooldown>0)blockCooldown--;
  if(isNoAi()){getNavigation().stop();setDeltaMovement(new Vec3(0,getDeltaMovement().y,0));return;}
  int age=(int)getPhaseTicks(0);
  if(isSummoning()){phase(IDLE);cooldown=MaodieFighterTiming.pauseTicks(0);return;}
  if(!validPlayer(getTarget()))setTarget(null);
  trackTarget(getPhase()==BACKSTEP?80:40);
  setYRot(attackYaw());yBodyRot=attackYaw();yHeadRot=attackYaw();
  if(getPhase()!=IDLE){
   getNavigation().stop();setDeltaMovement(new Vec3(0,getDeltaMovement().y,0));
   switch(getPhase()){
    case COMBO -> {if(MaodieFighterTiming.isImpact(COMBO,age))strike(2,2.65,false,false);}
    case JAB -> {if(MaodieFighterTiming.isImpact(JAB,age))strike(4,2.65,false,false);}
    case UPPERCUT -> {if(MaodieFighterTiming.isImpact(UPPERCUT,age))strike(8,2.65,false,true);}
    case SWEEP -> {if(MaodieFighterTiming.isImpact(SWEEP,age))strike(6,3.5,true,false);}
    case PULL -> {if(MaodieFighterTiming.isPullPulse(age))pull();}
    case BACKSTEP -> {if(age>=1&&age<=MaodieFighterTiming.duration(BACKSTEP))orbit(age);}
    case JUMP_SLAM -> {
     if(age==MaodieFighterTiming.actionTick(1)&&onGround())setDeltaMovement(0,.24,0);
     if(MaodieFighterTiming.isImpact(JUMP_SLAM,age))strike(10,3.25,true,false);
    }
    case BLOCK -> {}
    default -> {finish();return;}
   }
   if(age>=MaodieFighterTiming.duration(getPhase()))finish();
   return;
  }
  if(cooldown>0)cooldown--;
  LivingEntity target=getTarget();if(!validPlayer(target))return;
  double distance=distanceToSqr(target);
  if(cooldown<=0&&distance<=64&&hasLineOfSight(target)){
   if(distance>16){beginAttack(PULL);return;}
   if(distance<=7.0){beginAttack(MOVES[Math.floorMod(nextMove++,MOVES.length)]);return;}
  }
  if(distance>3.2)getNavigation().moveTo(target,1.3);
  else getNavigation().stop();
 }
 private void finish(){phase(IDLE);cooldown=MaodieFighterTiming.pauseTicks(random.nextInt(7));}
 private void strike(float damage,double radius,boolean allAround,boolean uppercut){
  boolean landed=false;
  for(var p:((ServerLevel)level()).players()){
   Vec3 d=p.position().subtract(position());double horizontal=d.horizontalDistance();
   if(!validPlayer(p)||horizontal>radius||Math.abs(d.y)>2||!hasLineOfSight(p))continue;
   Vec3 forward=new Vec3(-Mth.sin(attackYaw()*Mth.DEG_TO_RAD),0,Mth.cos(attackYaw()*Mth.DEG_TO_RAD));
   if(!allAround&&horizontal>.01&&d.multiply(1,0,1).normalize().dot(forward)<.55)continue;
   // Repeat hits bypass this combo's own immunity only, then restore the window.
   int immunity=p.invulnerableTime;
   boolean repeat=getPhase()==COMBO&&comboVictims.contains(p.getUUID())&&p.getLastHurtByMob()==this;
   if(repeat)p.invulnerableTime=0;
   Vec3 beforeHit=p.getDeltaMovement();
   boolean hurt=p.hurt(damageSources().mobAttack(this),damage);
   if(repeat)p.invulnerableTime=Math.max(immunity,p.invulnerableTime);
   if(!hurt)continue;
   landed=true;if(getPhase()==COMBO)comboVictims.add(p.getUUID());
   if(getPhase()==COMBO&&MaodieFighterTiming.isEarlyCombo(getPhaseTicks(0))){
    // hurt() already applies vanilla .4 knockback, including a ground launch.
    // Early punches replace only that hit's impulse, preserving pre-hit movement.
    double strength=.025*(1-Mth.clamp(p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE),0,1));
    p.setDeltaMovement(beforeHit.add(d.multiply(1,0,1).normalize().scale(strength)));
    p.hurtMarked=true;
   }else if(uppercut){p.setDeltaMovement(p.getDeltaMovement().add(0,.5,0));p.hurtMarked=true;}
   else p.knockback(.3,-d.x,-d.z);
  }
  if(!landed)playSound(SoundEvents.PLAYER_ATTACK_NODAMAGE,.3F,1.25F+random.nextFloat()*.2F);
  else if(getPhase()==JUMP_SLAM)playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK,.7F,.65F);
  else if(uppercut)playSound(SoundEvents.PLAYER_ATTACK_STRONG,.6F,.85F);
  else if(getPhase()==SWEEP)playSound(SoundEvents.PLAYER_ATTACK_SWEEP,.55F,.85F);
  else playSound(SoundEvents.PLAYER_ATTACK_WEAK,.45F,1.1F+random.nextFloat()*.25F);
 }
 private void pull(){
  var t=getTarget();if(!validPlayer(t)||distanceToSqr(t)>64||!hasLineOfSight(t))return;
  Vec3 d=position().subtract(t.position()).multiply(1,0,1);
  if(d.horizontalDistance()<1.8)return;
  Vec3 v=t.getDeltaMovement().add(d.normalize().scale(.14));
  double speed=v.horizontalDistance();if(speed>.35)v=new Vec3(v.x*.35/speed,v.y,v.z*.35/speed);
  t.setDeltaMovement(v);t.hurtMarked=true;
 }
 private void orbit(int age){
  if(orbitBlocked||!validPlayer(getTarget()))return;
  double progress=MaodieFighterTiming.orbitProgress(age),angle=orbitStart+orbitDelta*progress;
  Vec3 desired=new Vec3(orbitCenter.x+Math.cos(angle)*orbitRadius,getY(),orbitCenter.z+Math.sin(angle)*orbitRadius);
  Vec3 step=desired.subtract(position());if(step.length()>1.1)step=step.normalize().scale(1.1);
  AABB swept=getBoundingBox().expandTowards(step).deflate(.001);
  if(!level().hasChunksAt(BlockPos.containing(swept.minX,swept.minY-1,swept.minZ),BlockPos.containing(swept.maxX,swept.maxY,swept.maxZ))
     ||!level().noCollision(this,swept)
     ||!level().getBlockCollisions(this,swept.move(0,-.15,0)).iterator().hasNext()){
   orbitBlocked=true;return;
  }
  Vec3 before=position();move(MoverType.SELF,step);
  if(position().distanceTo(before.add(step))>.05)orbitBlocked=true;
  trackTarget(80);
 }
 private boolean facingAttacker(DamageSource damage){
  Entity attacker=damage.getDirectEntity();if(attacker==null)attacker=damage.getEntity();
  if(attacker==null)return false;
  Vec3 d=attacker.position().subtract(position()).multiply(1,0,1);
  Vec3 forward=new Vec3(-Mth.sin(attackYaw()*Mth.DEG_TO_RAD),0,Mth.cos(attackYaw()*Mth.DEG_TO_RAD));
  return d.lengthSqr()>.001&&d.normalize().dot(forward)>.35&&hasLineOfSight(attacker);
 }
 @Override public boolean hurt(DamageSource damage,float amount){
  if(!level().isClientSide&&!isNoAi()&&amount>0){
   if(getPhase()==IDLE&&blockCooldown==0&&facingAttacker(damage)&&damage.getEntity() instanceof LivingEntity attacker&&validPlayer(attacker)){
    setTarget(attacker);beginAttack(BLOCK);
   }
   if(getPhase()==BLOCK&&facingAttacker(damage)){
    amount*=.25F;playSound(SoundEvents.SHIELD_BLOCK,.45F,1.25F);
   }
  }
  return super.hurt(damage,amount);
 }
 @Override public void startSeenByPlayer(ServerPlayer player){
  super.startSeenByPlayer(player);
  if(isAlive()&&!isRemoved()){
   bossEvent.setName(getDisplayName());
   bossEvent.setProgress(Mth.clamp(getHealth()/getMaxHealth(),0F,1F));
   bossEvent.addPlayer(player);
  }
 }
 @Override public void stopSeenByPlayer(ServerPlayer player){
  super.stopSeenByPlayer(player);bossEvent.removePlayer(player);
 }
 @Override public void die(DamageSource source){
  super.die(source);bossEvent.setProgress(0F);bossEvent.removeAllPlayers();
 }
 @Override public void remove(RemovalReason reason){
  bossEvent.removeAllPlayers();super.remove(reason);
 }
 /** Same training pool as Butter Cat; the independent trophy never replaces a training reward. */
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit){
  super.dropCustomDeathLoot(source,looting,recentlyHit);
  int drops=Mth.nextInt(random,1,3);
  for(int roll=0;roll<drops;roll++)spawnAtLocation(new ItemStack(randomSuperReward()));
  if(random.nextFloat()<.25F)spawnAtLocation(new ItemStack(CatAccessoryItems.boxingReward()));
 }
 private Item randomSuperReward(){
  return switch(random.nextInt(7)){
   case 0 -> LaoWuMod.SUPER_ATTACK_CAT_CAN.get();
   case 1 -> LaoWuMod.SUPER_HEALTH_CAT_CAN.get();
   case 2 -> LaoWuMod.SUPER_SPEED_CAT_CAN.get();
   case 3 -> LaoWuMod.SUPER_STAMINA_CAT_CAN.get();
   case 4 -> LaoWuMod.SUPER_INTELLIGENCE_CAT_CAN.get();
   case 5 -> LaoWuMod.SUPER_LUCK_CAT_CAN.get();
   default -> LaoWuMod.SUPER_DRIED_FISH.get();
  };
 }
 @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.CAT_HURT;}
 @Override protected SoundEvent getDeathSound(){return SoundEvents.CAT_DEATH;}
 @Override public void addAdditionalSaveData(CompoundTag tag){
  super.addAdditionalSaveData(tag);tag.put("MaodieSourceCat",source.copy());tag.putLong("MaodieShelf",origin.asLong());
  tag.putBoolean("MaodieReturnPending",false);tag.putInt("MaodieAbsence",absence);tag.putInt("FighterNextMove",nextMove);tag.put("InheritedGenome",entityData.get(GENOME).copy());
 }
 @Override public void readAdditionalSaveData(CompoundTag tag){
  super.readAdditionalSaveData(tag);source=tag.getCompound("MaodieSourceCat").copy();origin=BlockPos.of(tag.getLong("MaodieShelf"));
  absence=tag.getInt("MaodieAbsence");nextMove=Math.max(0,tag.getInt("FighterNextMove"));
  entityData.set(GENOME,tag.getCompound("InheritedGenome").copy());phase(IDLE);cooldown=MaodieFighterTiming.pauseTicks(0);
  setCustomName(null);setCustomNameVisible(false);
  if(tag.getBoolean("MaodieReturnPending"))setNoAi(false);
 }
}
