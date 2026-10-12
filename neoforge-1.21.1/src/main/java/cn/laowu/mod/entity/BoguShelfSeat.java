package cn.laowu.mod.entity;
import cn.laowu.mod.*;
import cn.laowu.mod.block.BoguShelfBlock;
import cn.laowu.mod.network.ModNetwork;
import cn.laowu.mod.encounter.MaodieEncounter;
import cn.laowu.mod.encounter.MaodieSummonState;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
/** Invisible, persisted seat with a real passenger; no copied cats or frozen AI NBT. */
public final class BoguShelfSeat extends Entity {
 private static final EntityDataAccessor<BlockPos> ANCHOR=SynchedEntityData.defineId(BoguShelfSeat.class,EntityDataSerializers.BLOCK_POS);
 private static final EntityDataAccessor<Boolean> HISSING=SynchedEntityData.defineId(BoguShelfSeat.class,EntityDataSerializers.BOOLEAN);
 private static final EntityDataAccessor<Long> HISS_START=SynchedEntityData.defineId(BoguShelfSeat.class,EntityDataSerializers.LONG);
 private static final EntityDataAccessor<Float> HEAD_YAW=SynchedEntityData.defineId(BoguShelfSeat.class,EntityDataSerializers.FLOAT);
 private static final EntityDataAccessor<Float> HEAD_PITCH=SynchedEntityData.defineId(BoguShelfSeat.class,EntityDataSerializers.FLOAT);
 private boolean wasOrdered;
 private static final EntityDataAccessor<Long> AURA_START=SynchedEntityData.defineId(BoguShelfSeat.class,EntityDataSerializers.LONG);
 private final MaodieSummonState maodie=new MaodieSummonState();
 private long hissUntil;
 private long lastLookTick=Long.MIN_VALUE;
 public BoguShelfSeat(EntityType<? extends BoguShelfSeat> type,Level level){super(type,level);setNoGravity(true);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(ANCHOR,BlockPos.ZERO);b.define(HISSING,false);b.define(AURA_START,-1L);b.define(HISS_START,-1L);b.define(HEAD_YAW,0F);b.define(HEAD_PITCH,0F);}
 public boolean isSummoningMaodie(){return maodieAuraStartTick()>=0;}
 public long maodieAuraStartTick(){return entityData.get(AURA_START);}
 public MaodieSummonState maodieState(){return maodie;}
 public void syncMaodieAura(){entityData.set(AURA_START,maodie.startTick());}
 public void cancelMaodieAura(){maodie.reset();entityData.set(AURA_START,-1L);}
 public BlockPos anchor(){return entityData.get(ANCHOR);}
 public long hissStartTick(){return entityData.get(HISS_START);}
 public float headYaw(){return entityData.get(HEAD_YAW);}
 public float headPitch(){return entityData.get(HEAD_PITCH);}
 public boolean hissing(){return entityData.get(HISSING)&&BoguShelfLook.hissing(level().getGameTime(),hissStartTick());}
 public Cat cat(){return getFirstPassenger() instanceof Cat c?c:null;}
 /** Capturing must preserve the pre-display order, without mutating the live passenger. */
 public void sanitizeSnapshot(CompoundTag snapshot){if(cat()!=null)snapshot.putBoolean("Sitting",cat().isOrderedToSit());}
 public void configure(BlockPos root,Direction facing,Cat cat) {
  entityData.set(ANCHOR,root.immutable());wasOrdered=cat.isOrderedToSit();
  var offset=facing.getClockWise();setPos(root.getX()+.5-offset.getStepX()*.75,root.getY()+21D/16,root.getZ()+.5-offset.getStepZ()*.75);
  setYRot(facing.toYRot());
 }
 public void hissBriefly(){
  if(level().isClientSide||cat()==null||isSummoningMaodie()||!ServerConfig.catsHiss())return;
  long now=level().getGameTime();if(hissStartTick()==now)return;
  hissUntil=now+10;entityData.set(HISS_START,now);entityData.set(HISSING,true);hold(cat());
 }
 public static boolean tickCat(Cat cat){
  if(cat.getVehicle() instanceof BoguShelfSeat seat){
   if(cat.isTame()){seat.release();return false;}seat.hold(cat);return true;
  }
  // No block entity or retained global position cache. Existing shelves work after an upgrade.
  // Only eligible wild cats scan, twice per second, and unloaded chunks are never fetched.
  if(cat.level().isClientSide||cat.tickCount%10!=0||!eligible(cat))return false;
  var center=cat.blockPosition();var level=cat.level();
  for(var at:BlockPos.betweenClosed(center.offset(-3,-3,-3),center.offset(3,3,3))){
   if(cat.distanceToSqr(Vec3.atCenterOf(at))>9||!level.hasChunkAt(at))continue;
   var state=level.getBlockState(at);
   if(state.getBlock() instanceof BoguShelfBlock block&&state.getValue(BoguShelfBlock.PART)==BoguShelfBlock.ROOT)
    block.attract(level,at.immutable(),state);
   if(cat.getVehicle() instanceof BoguShelfSeat)return true;
  }
  return false;
 }
 public static boolean eligible(Cat cat){
  return cat.isAlive()&&!cat.isTame()&&!cat.isPassenger()&&!cat.isLeashed()
      &&!CatPoseData.isPancake(cat)&&!CatGiantMount.active(cat)&&!CatProfileData.isBeingViewed(cat);
 }
 public void hold(Cat cat) {
  if(level().isClientSide)return;
  if(entityData.get(HISSING)&&(level().getGameTime()>=hissUntil||!ServerConfig.catsHiss()))entityData.set(HISSING,false);
  cat.getNavigation().stop();cat.setTarget(null);cat.setDeltaMovement(Vec3.ZERO);cat.fallDistance=0;
  cat.setLying(false);
  boolean hiss=hissing()&&ServerConfig.catsHiss();
  // Display pose is not the actual pet stay/sit command.
  cat.setInSittingPose(!hiss);
  int pose=hiss?CatPoseData.HISSING:CatPoseData.NORMAL;
  if(CatPoseData.getPose(cat)!=pose){CatPoseData.setPose(cat,pose);ModNetwork.syncToTracking(cat,pose);}
  // Shelf hisses have their own immediate one-shot sound, never the ambient intro/loop session.
  ModNetwork.setAudioSession(cat,false);
  if(lastLookTick!=level().getGameTime()){
   lastLookTick=level().getGameTime();var player=level().getNearestPlayer(cat,6);
   float yaw,pitch;
   if(player!=null&&!player.isSpectator()){
    Vec3 aim=player.getEyePosition().subtract(cat.getEyePosition());
    yaw=BoguShelfLook.yaw(aim.x,aim.z,getYRot(),headYaw());
    pitch=BoguShelfLook.pitch(aim.y,aim.horizontalDistance(),headPitch());
   }else{yaw=BoguShelfLook.approach(headYaw(),0);pitch=BoguShelfLook.approach(headPitch(),0);}
   entityData.set(HEAD_YAW,yaw);entityData.set(HEAD_PITCH,pitch);
  }
  cat.setYRot(getYRot());cat.yBodyRot=getYRot();cat.setYHeadRot(getYRot()+headYaw());cat.setXRot(headPitch());
  Vec3 look=cat.getEyePosition().add(Vec3.directionFromRotation(headPitch(),getYRot()+headYaw()));
  cat.getLookControl().setLookAt(look.x,look.y,look.z,12,12);
 }
 @Override public void tick() {
  super.tick();if(level().isClientSide)return;
  var root=anchor();
  if(!level().hasChunksAt(root.offset(-2,0,-2),root.offset(2,2,2))){cancelMaodieAura();return;}
  var state=level().getBlockState(root);
  if(!(state.getBlock() instanceof BoguShelfBlock)||state.getValue(BoguShelfBlock.PART)!=BoguShelfBlock.ROOT
       ||!BoguShelfBlock.complete(level(),root,state.getValue(BoguShelfBlock.FACING))){release();return;}
  if(cat()==null){if(tickCount>100)discard();return;}
  if(!cat().isAlive()||cat().isTame()||CatGiantMount.active(cat())||CatPoseData.isPancake(cat())){release();return;}
  hold(cat());
  MaodieEncounter.tickSeat(this);
 }
 public void release() {
  cancelMaodieAura();
  if(cat()!=null){var cat=cat();var destination=getDismountLocationForPassenger(cat);cat.stopRiding();cat.setPos(destination);cat.fallDistance=0;}
  discard();
 }
 @Override protected void removePassenger(Entity entity) {
  super.removePassenger(entity);
  if(!level().isClientSide)cancelMaodieAura();
  if(entity instanceof Cat cat&&!level().isClientSide) {
   boolean ordered=cat.isOrderedToSit();
   cat.setOrderedToSit(ordered);cat.setInSittingPose(ordered);CatPoseData.setPose(cat,0);ModNetwork.syncToTracking(cat,0);ModNetwork.setAudioSession(cat,false);
  }
 }
 @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
  var facing=Direction.fromYRot(getYRot());var root=anchor();
  for(int radius=1;radius<=3;radius++)for(int y:new int[]{0,1,-1,2}) {
   for(var direction:new Direction[]{facing,facing.getOpposite(),facing.getClockWise(),facing.getCounterClockWise()})
    for(int side:new int[]{0,1,-1,2,-2}) {
     var at=root.relative(direction,radius).relative(direction.getClockWise(),side).above(y);
     if(level().isOutsideBuildHeight(at)||!level().hasChunksAt(at.below(),at.above()))continue;
     var p=net.minecraft.world.entity.vehicle.DismountHelper.findSafeDismountLocation(passenger.getType(),level(),at,true);
     if(p!=null&&level().getFluidState(at).isEmpty()&&level().getFluidState(at.below()).isEmpty()
        &&level().noCollision(passenger,passenger.getBoundingBox().move(p.subtract(passenger.position()))))return p;
    }
  }
  // A fully sealed display has no safe external exit. Never teleport into an unchecked ceiling.
  return passenger.position();
 }
 @Override protected void positionRider(Entity passenger,Entity.MoveFunction move){
  if(!hasPassenger(passenger))return;
  move.accept(passenger,getX(),getY(),getZ());
  // Passenger placement runs after vanilla cat AI; do not let its body/look controls undo the display.
  if(passenger instanceof Cat cat){cat.setYRot(getYRot());cat.yBodyRot=getYRot();cat.setYHeadRot(getYRot()+headYaw());cat.setXRot(headPitch());}
 }
 @Override protected boolean canAddPassenger(Entity passenger){return passenger instanceof Cat&&getPassengers().isEmpty();}
 @Override public boolean isPickable(){return false;}
 @Override public boolean isPushable(){return false;}
 @Override public boolean canBeCollidedWith(){return false;}
 @Override protected void addAdditionalSaveData(CompoundTag tag){tag.putLong("Shelf",anchor().asLong());tag.putBoolean("Hissing",hissing());tag.putBoolean("WasOrdered",wasOrdered);}
 @Override protected void readAdditionalSaveData(CompoundTag tag){cancelMaodieAura();entityData.set(ANCHOR,BlockPos.of(tag.getLong("Shelf")));entityData.set(HISSING,false);entityData.set(HISS_START,-1L);entityData.set(HEAD_YAW,0F);entityData.set(HEAD_PITCH,0F);hissUntil=0;lastLookTick=Long.MIN_VALUE;wasOrdered=tag.getBoolean("WasOrdered");setNoGravity(true);}

}
