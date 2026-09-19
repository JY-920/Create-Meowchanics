package cn.laowu.mod.entity;
import cn.laowu.mod.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
/** Real cat at the base and player on its back; all motion, breathing and stamina are server-owned. */
public final class CatDivingCarrier extends Entity {
    // Entity Y is the player's feet, not the seated pelvis (+0.751 blocks in the player model).
    // A 1-pixel pad above the cat's back at Y0.626 keeps the player seated, not floating overhead.
    public static final double RIDER_HEIGHT = -.0625;
    private static final EntityDataAccessor<Boolean> SURFACING = SynchedEntityData.defineId(CatDivingCarrier.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SWIMMING = SynchedEntityData.defineId(CatDivingCarrier.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> SECONDS = SynchedEntityData.defineId(CatDivingCarrier.class, EntityDataSerializers.LONG);
    private float forward, side, inputYaw, inputPitch, lerpYaw;
    private boolean up, down;
    private long inputTime = -100, lastPacketTime = -1;
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    public CatDivingCarrier(EntityType<? extends CatDivingCarrier> type, Level level) { super(type, level); setNoGravity(true); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(SURFACING, false); builder.define(SWIMMING, false); builder.define(SECONDS, 0L); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { tag.putBoolean("Surfacing", surfacing()); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { entityData.set(SURFACING, tag.getBoolean("Surfacing")); setNoGravity(true); }
    public boolean surfacing() { return entityData.get(SURFACING); }
    public boolean swimming() { return entityData.get(SWIMMING); }
    public long seconds() { return entityData.get(SECONDS); }
    public Cat cat() { return getPassengers().stream().filter(Cat.class::isInstance).map(Cat.class::cast).findFirst().orElse(null); }
    public Player rider() { return getPassengers().stream().filter(Player.class::isInstance).map(Player.class::cast).findFirst().orElse(null); }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Cat c && cat() == null && c.isTame() && CatClothesData.getOutfit(c) == CatOutfitType.DIVING
                || passenger instanceof Player p && rider() == null && cat() != null && cat().isOwnedBy(p);
    }
    @Override protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        if (hasPassenger(passenger)) move.accept(passenger, getX(), getY() + (passenger instanceof Player ? RIDER_HEIGHT : 0), getZ());
    }
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) { return position(); }
    @Override public LivingEntity getControllingPassenger() { return null; }
    @Override public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        lerpX=x; lerpY=y; lerpZ=z; lerpYaw=yaw; lerpSteps=Math.max(1,steps);
    }
    public void input(ServerPlayer sender, float forward, float side, float yaw, float pitch, boolean up, boolean down) {
        long now=level().getGameTime();
        if (sender.getVehicle()!=this || rider()!=sender || cat()==null || !cat().isOwnedBy(sender)
                || now==lastPacketTime || !CatPilotFlightRules.validInput(forward,side,yaw,pitch)) return;
        this.forward=forward; this.side=side; inputYaw=Mth.wrapDegrees(yaw); inputPitch=pitch; this.up=up; this.down=down;
        inputTime=lastPacketTime=now;
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (lerpSteps>0) {
                setPos(getX()+(lerpX-getX())/lerpSteps,getY()+(lerpY-getY())/lerpSteps,getZ()+(lerpZ-getZ())/lerpSteps);
                setYRot(getYRot()+Mth.wrapDegrees(lerpYaw-getYRot())/lerpSteps);lerpSteps--;
            }
            return;
        }
        Cat cat=cat(); Player player=rider();
        if (cat==null || player==null || !cat.isAlive() || !player.isAlive() || !cat.isOwnedBy(player)
                || CatClothesData.getOutfit(cat)!=CatOutfitType.DIVING || CatPoseData.isPancake(cat)
                || CatProfileData.isBeingViewed(cat) || !CatPilotFlight.wrench(player) || cat.isOrderedToSit() || isInLava()) {
            release();return;
        }
        cat.setTarget(null);cat.getNavigation().stop();cat.setDeltaMovement(Vec3.ZERO);
        boolean wet=level().getFluidState(net.minecraft.core.BlockPos.containing(getX(),getY()+.3,getZ())).is(FluidTags.WATER);
        entityData.set(SWIMMING,wet);
        long capacity=CatDivingMount.duration(cat);
        long used=Math.max(0,Math.min(capacity,cat.getPersistentData().getLong(CatDivingMount.USED)));
        if (wet && used>=capacity) entityData.set(SURFACING,true);
        if (wet && !surfacing()) {
            used=Math.min(capacity,used+1);
            cat.setAirSupply(cat.getMaxAirSupply());player.setAirSupply(player.getMaxAirSupply());
        } else if (!wet && onGround()) {
            used=Math.max(0,used-2);
            if(used==0)entityData.set(SURFACING,false);
        }
        cat.getPersistentData().putLong(CatDivingMount.USED,used);
        entityData.set(SECONDS,(capacity-used)/20+((capacity-used)%20==0?0:1));
        boolean fresh=level().getGameTime()-inputTime<=10;
        float yaw=fresh?inputYaw:player.getYRot();
        Vec3 motion;
        if(wet) {
            cat.fallDistance=player.fallDistance=fallDistance=0;
            motion=CatDivingRules.swim(getDeltaMovement(),fresh?forward:0,fresh?side:0,yaw,fresh?inputPitch:0,
                    fresh&&up,fresh&&down,surfacing(),CatDivingMount.speed(cat,true));
        } else {
            Vec3 previous=new Vec3(getDeltaMovement().x,0,getDeltaMovement().z);
            Vec3 horizontal=CatPilotFlightRules.step(previous,fresh?forward:0,fresh?side:0,yaw,0,false,false,false,CatDivingMount.speed(cat,false));
            double dy=onGround()?(fresh&&up?.42:0):(getDeltaMovement().y-.08)*.98;
            motion=new Vec3(horizontal.x,dy,horizontal.z);
        }
        var destination=getBoundingBox().move(motion);
        if(!level().hasChunksAt(net.minecraft.core.BlockPos.containing(destination.minX,destination.minY,destination.minZ),
                net.minecraft.core.BlockPos.containing(destination.maxX,destination.maxY,destination.maxZ))) motion=Vec3.ZERO;
        setYRot(yaw);cat.setYRot(yaw);cat.setYBodyRot(yaw);cat.setYHeadRot(yaw);
        move(MoverType.SELF,motion);
        setDeltaMovement(new Vec3(horizontalCollision?0:motion.x,verticalCollision?0:motion.y,horizontalCollision?0:motion.z));
    }
    public void release() {
        if(level().isClientSide)return;
        for(Entity passenger:java.util.List.copyOf(getPassengers())) {
            passenger.stopRiding();passenger.setDeltaMovement(getDeltaMovement().scale(.3));
        }
        discard();
    }
}
