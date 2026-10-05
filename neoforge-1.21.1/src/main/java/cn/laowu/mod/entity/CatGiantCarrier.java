package cn.laowu.mod.entity;

import cn.laowu.mod.CatGiantMount;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Server-driven collision root; the original Cat is a passenger, never replaced. */
public final class CatGiantCarrier extends Entity {
    private static final EntityDataAccessor<Boolean> GROUNDED=SynchedEntityData.defineId(CatGiantCarrier.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> HORIZONTAL_SPEED=SynchedEntityData.defineId(CatGiantCarrier.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> JUMP_WINDUP=SynchedEntityData.defineId(CatGiantCarrier.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE=SynchedEntityData.defineId(CatGiantCarrier.class,EntityDataSerializers.FLOAT);
    private float physicalScale=1F;
    private float forward, side, inputYaw;
    private boolean jump, sprint, pendingJump;
    private long inputTime = -100, lastPacketTime = -1;
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private float lerpYaw;

    public CatGiantCarrier(EntityType<? extends CatGiantCarrier> type, Level level) { super(type,level); setNoGravity(true); }
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) { builder.define(GROUNDED,false);builder.define(HORIZONTAL_SPEED,0F);builder.define(JUMP_WINDUP,0);builder.define(SIZE,1F); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) { setNoGravity(true); }
    public Cat cat() { return getPassengers().stream().filter(Cat.class::isInstance).map(Cat.class::cast).findFirst().orElse(null); }
    public Player rider() { return getPassengers().stream().filter(Player.class::isInstance).map(Player.class::cast).findFirst().orElse(null); }
    public boolean grounded() { return entityData.get(GROUNDED); }
    public float horizontalSpeed() { return entityData.get(HORIZONTAL_SPEED); }
    public int jumpWindup() { return entityData.get(JUMP_WINDUP); }
    public static final float RIDE_HEIGHT=4.05F;
    public void updateSize() {
        if(!level().isClientSide && cat()!=null) {
            float next=CatGiantMount.sizeFactor(cat());
            // Dismount using the old safe seat before a growth step could raise
            // the owner into a roof. The cat keeps its new attribute/size.
            if(next>physicalScale && rider()!=null && !CatGiantMount.hasSpace(cat())) {
                release();return;
            }
            entityData.set(SIZE,next);
        }
        float next=entityData.get(SIZE);
        if(Float.compare(physicalScale,next)!=0) { physicalScale=next;refreshDimensions(); }
    }
    @Override public EntityDimensions getDimensions(Pose pose) {
        float scale=physicalScale>0?physicalScale:1;
        return EntityDimensions.scalable(CatGiantMount.WIDTH*scale,
                (float)Math.max(CatGiantMount.HEIGHT*scale,Math.max(0,CatGiantMount.SEAT*scale-.75)+1.925));
    }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public float maxUpStep() { return 1.0F; }
    @Override protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Cat c && cat()==null && CatGiantMount.active(c)
                || passenger instanceof Player p && rider()==null && cat()!=null && CatGiantMount.active(cat()) && cat().isOwnedBy(p);
    }
    @Override protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        if (hasPassenger(passenger)) move.accept(passenger,getX(),getY()+(passenger instanceof Player ? Math.max(0,CatGiantMount.SEAT*physicalScale-.75) : 0),getZ());
    }
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        if (passenger instanceof Cat) return position();
        double side=getBbWidth()/2+0.8;
        for (Vec3 offset:List.of(new Vec3(side,0,0),new Vec3(-side,0,0),new Vec3(0,0,side),new Vec3(0,0,-side),
                new Vec3(side,0,side),new Vec3(side,0,-side),new Vec3(-side,0,side),new Vec3(-side,0,-side),
                new Vec3(0,CatGiantMount.HEIGHT*physicalScale+.1,0))) {
            Vec3 at=position().add(offset);
            AABB box=passenger.getBoundingBox().move(at.subtract(passenger.position()));
            if (onGround() && offset.y==0 && !level().getBlockCollisions(passenger,box.move(0,-.25,0)).iterator().hasNext())
                continue;
            if (level().hasChunksAt(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))
                    && level().getWorldBorder().isWithinBounds(box)
                    && !level().getBlockCollisions(passenger,box).iterator().hasNext()) return at;
        }
        return passenger.position();
    }
    // Suppresses vanilla client vehicle movement packets; only validated input is accepted.
    @Override public LivingEntity getControllingPassenger() { return null; }
    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps) {
        lerpX=x;lerpY=y;lerpZ=z;lerpYaw=yaw;lerpSteps=Math.max(1,steps);
    }
    public void input(ServerPlayer sender,float forward,float side,float yaw,boolean jump,boolean sprint) {
        long now=level().getGameTime();
        Cat cat=cat();
        if (sender.getVehicle()!=this || rider()!=sender || cat==null || !cat.isAlive() || !sender.isAlive()
                || sender.isSpectator() || cat.distanceToSqr(sender)>Math.pow(Math.max(6,CatGiantMount.SEAT*physicalScale+2),2) || !cat.isOwnedBy(sender)
                || !CatGiantMount.active(cat) || !Float.isFinite(forward)
                || !Float.isFinite(side) || !Float.isFinite(yaw) || Math.abs(forward)>1 || Math.abs(side)>1) return;
        // Preserve an authenticated press/release even if several packets arrive in one server tick.
        // Only grounded edges can request a jump; movement remains rate-limited below.
        if(jump&&!this.jump&&onGround()&&!isInWaterOrBubble())pendingJump=true;
        this.jump=jump;
        if(now==lastPacketTime)return;
        this.forward=forward;this.side=side;inputYaw=Mth.wrapDegrees(yaw);this.sprint=sprint;
        inputTime=lastPacketTime=now;
    }
    @Override public void tick() {
        super.tick();
        // NeoForge EntityTickEvent does not cover every client passenger tick.
        // Reconcile dimensions after spawn/age/attribute packets arrive in any order.
        if(cat()!=null)CatGiantMount.tick(cat());
        updateSize();
        if (level().isClientSide) {
            if (lerpSteps>0) {
                setPos(getX()+(lerpX-getX())/lerpSteps,getY()+(lerpY-getY())/lerpSteps,getZ()+(lerpZ-getZ())/lerpSteps);
                setYRot(getYRot()+Mth.wrapDegrees(lerpYaw-getYRot())/lerpSteps);lerpSteps--;
            }
            setOnGround(grounded());
            return;
        }
        Cat cat=cat();Player player=rider();
        if (cat==null || !cat.isAlive() || !CatGiantMount.active(cat) || player==null || !player.isAlive()
                || !cat.isOwnedBy(player) || player.isShiftKeyDown() || cat.isInLava() || isInLava()) { release();return; }
        cat.setTarget(null);cat.getNavigation().stop();cat.setDeltaMovement(Vec3.ZERO);
        boolean fresh=level().getGameTime()-inputTime<=10;
        float yaw=fresh?inputYaw:player.getYRot();
        float f=fresh?forward:0,s=fresh?side:0;
        Vec3 desired=Vec3.directionFromRotation(0,yaw).scale(f).add(Vec3.directionFromRotation(0,yaw-90).scale(s));
        if (desired.lengthSqr()>1) desired=desired.normalize();
        boolean water=isInWaterOrBubble();
        double speed=water?.11:(fresh&&sprint?.32:.22);
        Vec3 old=getDeltaMovement();
        double vx=Mth.lerp(.5,old.x,desired.x*speed),vz=Mth.lerp(.5,old.z,desired.z*speed);
        boolean jumpNow=fresh&&jump;
        int windup=jumpWindup();
        boolean launch=false;
        if(windup>0) {
            // A valid grounded press commits the short anticipation; release must not cancel it.
            if(!onGround() || water) windup=0;
            else if(windup==1) { windup=0;launch=true; }
            else windup--;
        } else if(onGround() && !water && fresh && pendingJump) windup=3;
        // Never buffer airborne presses or a second press during anticipation for a later landing.
        pendingJump=false;
        entityData.set(JUMP_WINDUP,windup);
        double vy=water ? (jumpNow?.18:Math.max(-.08,old.y*.6+.03))
                : onGround() ? (launch?.65:-.08) : Math.max(-.9,(old.y-.08)*.98);
        Vec3 motion=new Vec3(vx,vy,vz);
        AABB destination=getBoundingBox().expandTowards(motion);
        if (!level().hasChunksAt(BlockPos.containing(destination.minX,destination.minY,destination.minZ),
                BlockPos.containing(destination.maxX,destination.maxY,destination.maxZ))) motion=Vec3.ZERO;
        setYRot(yaw);cat.setYRot(yaw);cat.setYBodyRot(yaw);cat.setYHeadRot(yaw);
        Vec3 before=position();
        move(MoverType.SELF,motion);
        Vec3 actual=position().subtract(before);
        setDeltaMovement(new Vec3(actual.x,verticalCollision?0:motion.y,actual.z));
        entityData.set(GROUNDED,onGround());
        entityData.set(HORIZONTAL_SPEED,(float)Math.min(1,Math.hypot(actual.x,actual.z)));
        cat.fallDistance=player.fallDistance=fallDistance;
    }
    public void release() {
        if (level().isClientSide) return;
        for (Entity passenger:List.copyOf(getPassengers())) {
            Vec3 exit=passenger instanceof Player p ? getDismountLocationForPassenger(p) : null;
            passenger.stopRiding();
            if (exit!=null) passenger.setPos(exit);
            passenger.setDeltaMovement(Vec3.ZERO);
            passenger.fallDistance=0;
        }
        discard();
    }
}
