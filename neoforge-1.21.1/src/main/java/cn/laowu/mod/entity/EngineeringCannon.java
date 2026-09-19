package cn.laowu.mod.entity;

import cn.laowu.mod.*;
import cn.laowu.mod.genetics.CatAttributeEffects;
import cn.laowu.mod.genetics.CatStat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Saved with its passenger: an unsaved vehicle would silently lose its cat on chunk unload. */
public final class EngineeringCannon extends Entity {
    public static final float MODEL_SCALE = 0.85F;
    public static final double PIVOT_Y = 15.0 / 16.0 * MODEL_SCALE;
    public static final double BARREL_LENGTH = 17.0 / 16.0 * MODEL_SCALE;
    public static final double SEAT_TOP = 0.25;
    public static final double SEAT_BACK = 1.05;
    private int recoilTicks;
    public EngineeringCannon(EntityType<? extends EngineeringCannon> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) { setNoGravity(true); }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Cat cat
                && cat.isTame() && CatClothesData.getOutfit(cat) == CatOutfitType.ENGINEERING;
    }
    @Override protected Vec3 getPassengerAttachmentPoint(Entity passenger, net.minecraft.world.entity.EntityDimensions dimensions, float scale) {
        return seatOffset();
    }
    private Vec3 seatOffset() {
        Vec3 backward = Vec3.directionFromRotation(0, getYRot()).scale(-SEAT_BACK);
        return new Vec3(backward.x, SEAT_TOP, backward.z);
    }
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 offset = seatOffset();
        return position().add(offset.x, 0, offset.z);
    }
    @Override public void tick() {
        super.tick();
        if (recoilTicks > 0) recoilTicks--;
        setDeltaMovement(Vec3.ZERO);
        if (level().isClientSide) return;
        if (!(getFirstPassenger() instanceof Cat cat)) {
            if (tickCount > 10) discard();
            return;
        }
        if (!cat.isAlive() || cat.isNoAi() || !cat.isTame()
                || CatClothesData.getOutfit(cat) != CatOutfitType.ENGINEERING
                || cat.isOrderedToSit() || CatPoseData.isPancake(cat) || CatProfileData.isBeingViewed(cat)
                || !CatEngineeringCombat.validTarget(cat, cat.getTarget())
                || position().distanceToSqr(cat.getTarget().position()) > CatEngineeringCombat.RANGE * CatEngineeringCombat.RANGE
                || isInWaterOrBubble() || isInLava()) {
            release();
            return;
        }
        cat.getNavigation().stop();
        cat.setDeltaMovement(Vec3.ZERO);
        cat.fallDistance = 0;
        aimAt(cat.getTarget());
        cat.setYRot(getYRot());
        cat.setYBodyRot(getYRot());
        cat.setYHeadRot(getYRot());
    }
    public void release() {
        for (Entity passenger : java.util.List.copyOf(getPassengers())) {
            passenger.stopRiding();
            passenger.setDeltaMovement(Vec3.ZERO);
            passenger.fallDistance = 0;
        }
        discard();
    }
    public Vec3 pivot() { return position().add(0, PIVOT_Y, 0); }
    public Vec3 direction() { return Vec3.directionFromRotation(getXRot(), getYRot()); }
    public Vec3 muzzle() { return pivot().add(direction().scale(BARREL_LENGTH)); }
    public void aimAt(LivingEntity target) {
        Vec3 aim = target.getBoundingBox().getCenter().subtract(pivot());
        if (aim.lengthSqr() < 1.0E-8) return;
        setYRot((float)(Mth.atan2(-aim.x, aim.z) * Mth.RAD_TO_DEG));
        setXRot((float)(-Mth.atan2(aim.y, aim.horizontalDistance()) * Mth.RAD_TO_DEG));
    }
    public boolean hasClearShot(LivingEntity target) {
        return level().clip(new ClipContext(pivot(), target.getBoundingBox().getCenter(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }
    public boolean fire(Cat cat, LivingEntity target) {
        if (!(level() instanceof ServerLevel level) || cat.getVehicle() != this
                || !CatEngineeringCombat.validTarget(cat, target) || !hasClearShot(target)) return false;
        Vec3 center = target.getBoundingBox().getCenter();
        // Always originate at the gun centre; the barrel never skips nearby victims.
        Vec3 start = pivot();
        if (level.clip(new ClipContext(pivot(), start, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.MISS) return false;
        var projectile = new EngineeringCogwheelProjectile(level, cat,
                (float)cat.getAttributeValue(Attributes.ATTACK_DAMAGE));
        int sequence = cat.getPersistentData().getInt("LaoWuArtilleryShot");
        var munition=CatArtilleryMunition.select(cat);
        projectile.setMunition(munition);
        projectile.setAccessoryDamage(projectile.getAccessoryDamage()*munition.damageMultiplier());
        projectile.setPos(start);
        Vec3 aim = center.subtract(start);
        if (CatAttributeEffects.effectiveValue(cat, CatStat.INTELLIGENCE) >= 80)
            aim = aim.add(target.getDeltaMovement().scale(Math.min(20, aim.length() / 1.6)));
        projectile.shoot(aim.x, aim.y, aim.z, 1.6F, 0.0F);
        if (!cn.laowu.mod.accessory.CatAccessoryHooks.projectile(cat, target, projectile)) {
            projectile.discard();
            cat.getPersistentData().putInt("LaoWuArtilleryShot", (sequence + 1) % 3);
            // A canceled valid shot still consumes the reload, like other career projectiles.
            return true;
        }
        if (!level.addFreshEntity(projectile)) return false;
        cat.getPersistentData().putInt("LaoWuArtilleryShot", (sequence + 1) % 3);
        level.broadcastEntityEvent(this, (byte)4);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                start.x, start.y, start.z, 10, 0.08, 0.08, 0.08, 0.035);
        level.playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                net.minecraft.sounds.SoundSource.NEUTRAL, 0.6F, 1.6F);
        return true;
    }
    @Override public void handleEntityEvent(byte event) {
        if (event == 4) recoilTicks = 6; else super.handleEntityEvent(event);
    }
    public float recoil(float partialTick) { return Math.max(0, recoilTicks - partialTick) / 6.0F; }

}
