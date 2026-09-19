package cn.laowu.mod.entity;

import cn.laowu.mod.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Persistable root holds the real cat and player; only the root moves, under server authority. */
public final class CatFlightCarrier extends Entity {
    // With the single band's Y=23..24.125 rail, this places Create's real
    // raised-wrench jaws on the rail without changing or stretching the arms.
    public static final double CAT_HEIGHT = 2.5;
    private static final EntityDataAccessor<Boolean> GLIDING = SynchedEntityData.defineId(CatFlightCarrier.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> SECONDS = SynchedEntityData.defineId(CatFlightCarrier.class, EntityDataSerializers.LONG);
    private float forward, side, inputYaw, inputPitch;
    private boolean up, down;
    private long inputTime = -100, lastPacketTime = -1;
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private float lerpYaw;
    public CatFlightCarrier(EntityType<? extends CatFlightCarrier> type, Level level) { super(type, level); setNoGravity(true); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(GLIDING, false); builder.define(SECONDS, 0L); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { tag.putBoolean("Gliding", gliding()); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { entityData.set(GLIDING, tag.getBoolean("Gliding")); setNoGravity(true); }
    public boolean gliding() { return entityData.get(GLIDING); }
    public long seconds() { return entityData.get(SECONDS); }
    public Cat cat() { return getPassengers().stream().filter(Cat.class::isInstance).map(Cat.class::cast).findFirst().orElse(null); }
    public Player rider() { return getPassengers().stream().filter(Player.class::isInstance).map(Player.class::cast).findFirst().orElse(null); }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Cat cat && cat() == null && cat.isTame()
                && CatClothesData.getOutfit(cat) == CatOutfitType.FLIGHT
                || passenger instanceof Player player && rider() == null && cat() != null && cat().isOwnedBy(player);
    }
    @Override protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        if (hasPassenger(passenger)) move.accept(passenger, getX(), getY() + (passenger instanceof Cat ? CAT_HEIGHT : 0), getZ());
    }
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) { return position(); }
    // Do not expose a controlling passenger: client vehicle-position packets must never drive this root.
    @Override public LivingEntity getControllingPassenger() { return null; }
    @Override public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        lerpX = x; lerpY = y; lerpZ = z; lerpYaw = yaw; lerpSteps = Math.max(1, steps);
    }
    public void input(ServerPlayer sender, float forward, float side, float yaw, float pitch, boolean up, boolean down) {
        long now = level().getGameTime();
        if (sender.getVehicle() != this || rider() != sender || cat() == null || !cat().isOwnedBy(sender)
                || now == lastPacketTime || !CatPilotFlightRules.validInput(forward, side, yaw, pitch)) return;
        this.forward = forward; this.side = side; inputYaw = Mth.wrapDegrees(yaw); inputPitch = pitch; this.up = up; this.down = down;
        inputTime = lastPacketTime = now;
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (lerpSteps > 0) {
                setPos(getX() + (lerpX - getX()) / lerpSteps, getY() + (lerpY - getY()) / lerpSteps, getZ() + (lerpZ - getZ()) / lerpSteps);
                setYRot(getYRot() + Mth.wrapDegrees(lerpYaw - getYRot()) / lerpSteps);
                lerpSteps--;
            }
            return;
        }
        Cat cat = cat();
        Player player = rider();
        if (cat == null || !cat.isAlive() || player == null || !player.isAlive() || !cat.isOwnedBy(player)
                || CatClothesData.getOutfit(cat) != CatOutfitType.FLIGHT || CatPoseData.isPancake(cat)
                || CatProfileData.isBeingViewed(cat) || !CatPilotFlight.wrench(player) || cat.isOrderedToSit()) {
            release(true); return;
        }
        cat.setTarget(null); cat.getNavigation().stop(); cat.setDeltaMovement(Vec3.ZERO);
        cat.fallDistance = player.fallDistance = fallDistance = 0;
        long capacity = CatPilotFlight.duration(cat);
        long used = Math.max(0, Math.min(capacity, cat.getPersistentData().getLong(CatPilotFlight.USED)));
        if (used >= capacity || getY() > level().getMaxBuildHeight() + 32) entityData.set(GLIDING, true);
        if (!gliding()) used = Math.min(capacity, used + 1);
        cat.getPersistentData().putLong(CatPilotFlight.USED, used);
        entityData.set(SECONDS, (capacity - used) / 20 + ((capacity - used) % 20 == 0 ? 0 : 1));
        boolean fresh = level().getGameTime() - inputTime <= 10;
        float yaw = fresh ? inputYaw : player.getYRot();
        Vec3 motion = CatPilotFlightRules.step(getDeltaMovement(), fresh ? forward : 0, fresh ? side : 0,
                yaw, fresh ? inputPitch : 0, fresh && up, fresh && down, gliding(), CatPilotFlight.speed(cat));
        if (!gliding() && !(fresh && down) && tickCount <= 10) motion = new Vec3(motion.x, Math.max(0.18, motion.y), motion.z);
        var destination = getBoundingBox().move(motion);
        if (!level().hasChunksAt(net.minecraft.core.BlockPos.containing(destination.minX, destination.minY, destination.minZ),
                net.minecraft.core.BlockPos.containing(destination.maxX, destination.maxY, destination.maxZ)))
            motion = new Vec3(0, Math.min(-0.035, motion.y), 0);
        setYRot(yaw);
        cat.setYRot(yaw); cat.setYBodyRot(yaw); cat.setYHeadRot(yaw);
        move(MoverType.SELF, motion);
        setDeltaMovement(new Vec3(horizontalCollision ? 0 : motion.x, verticalCollision ? 0 : motion.y, horizontalCollision ? 0 : motion.z));
        if (tickCount > 20 && onGround() || isInWaterOrBubble() || isInLava()) release(false);
    }
    public void release(boolean emergency) {
        if (level().isClientSide) return;
        for (Entity passenger : java.util.List.copyOf(getPassengers())) {
            passenger.stopRiding();
            passenger.setDeltaMovement(getDeltaMovement().multiply(0.3, 0, 0.3));
            passenger.fallDistance = 0;
            // No potion safety net: detaching resumes normal gravity.
            // Leave pre-existing effects from other items/mods untouched.
        }
        discard();
    }

}
