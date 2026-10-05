package cn.laowu.mod.entity;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.CatProjectileDamage;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

/** A fast, gravity-free laser fired by a mechanical career cat. */
public final class MechanicalLaserProjectile extends ThrowableItemProjectile implements cn.laowu.mod.api.CatAccessoryProjectile {
    public static final double MAX_TRAVEL_DISTANCE = 16.5D;
    private static final String DAMAGE_TAG = "LaoWuMechanicalLaserDamage";
    private static final String DISTANCE_TAG = "LaoWuMechanicalLaserDistance";
    private static final DustParticleOptions LASER_DUST = new DustParticleOptions(
            new Vector3f(1.0F, 0.16F, 0.56F), 0.8F);

    private float attackDamage = 2.0F;
    @Override public float getAccessoryDamage() { return attackDamage; }
    @Override public void setAccessoryDamage(double amount) {
        attackDamage = (float) cn.laowu.mod.accessory.CatAccessoryScriptRules.damage(amount);
    }
    private double travelledDistance;
    private double maxTravelDistance=MAX_TRAVEL_DISTANCE;
    private int maxLifetimeTicks=20;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> WORK_FLIGHT=
        net.minecraft.network.syncher.SynchedEntityData.defineId(MechanicalLaserProjectile.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    @Override protected void defineSynchedData() {
        super.defineSynchedData();entityData.define(WORK_FLIGHT,false);
    }
    public double getMaxTravelDistance() {return maxTravelDistance;}
    public int getMaxLifetimeTicks() {return maxLifetimeTicks;}
    public void setWorkFlightLimits(double distance,int lifetimeTicks) {
        if(level().isClientSide)return;
        if(Double.isFinite(distance)&&distance>=MAX_TRAVEL_DISTANCE&&lifetimeTicks>0) {
            maxTravelDistance=distance;maxLifetimeTicks=lifetimeTicks;
            entityData.set(WORK_FLIGHT,distance>MAX_TRAVEL_DISTANCE);
        }
    }

    public MechanicalLaserProjectile(
            EntityType<? extends MechanicalLaserProjectile> type, Level level) {
        super(type, level);
    }

    public MechanicalLaserProjectile(Level level, Cat owner, float attackDamage) {
        super(LaoWuMod.MECHANICAL_LASER_PROJECTILE.get(), owner, level);
        this.attackDamage = Math.max(0.0F, attackDamage);
    }

    @Override
    protected Item getDefaultItem() {
        // Rendering is handled by the supplied Blockbench laser model.
        return Items.AIR;
    }

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    public void tick() {
        Vec3 before = position();
        Vec3 velocity=getDeltaMovement();
        super.tick();
        // Work lasers retain the post-accessory velocity in air and water. Ordinary
        // shots keep vanilla drag. Sync the distinction so client trails also agree.
        if(entityData.get(WORK_FLIGHT)&&!isRemoved())setDeltaMovement(velocity);
        travelledDistance += before.distanceTo(position());

        if (level().isClientSide) {
            level().addParticle(LASER_DUST, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        } else if (travelledDistance >= maxTravelDistance || tickCount > maxLifetimeTicks) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity)
                && (!(getOwner() instanceof Cat cat) || !(entity instanceof LivingEntity living)
                || cn.laowu.mod.CatTeamRules.canHarm(cat, living))
                && !(entity instanceof Player);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(level() instanceof ServerLevel level)
                || !(result.getEntity() instanceof LivingEntity target)
                || !target.isAlive()) {
            discard();
            return;
        }

        Entity owner = getOwner();
        if (!(owner instanceof Cat cat) || !cat.isAlive()) {
            discard();
            return;
        }

        CatProjectileDamage.hurt(target, level.damageSources().mobProjectile(this, cat), attackDamage);
        Vec3 hit = result.getLocation();
        level.sendParticles(LASER_DUST, hit.x, hit.y, hit.z,
                12, 0.12D, 0.12D, 0.12D, 0.035D);
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level() instanceof ServerLevel level) {
            Vec3 hit = result.getLocation();
            level.sendParticles(LASER_DUST, hit.x, hit.y, hit.z,
                    8, 0.08D, 0.08D, 0.08D, 0.025D);
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat(DAMAGE_TAG, attackDamage);
        tag.putDouble(DISTANCE_TAG, travelledDistance);
        tag.putDouble("LaoWuMechanicalLaserMaxDistance",maxTravelDistance);
        tag.putInt("LaoWuMechanicalLaserMaxLifetime",maxLifetimeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        maxTravelDistance=MAX_TRAVEL_DISTANCE;maxLifetimeTicks=20;
        entityData.set(WORK_FLIGHT,false);
        setWorkFlightLimits(tag.getDouble("LaoWuMechanicalLaserMaxDistance"),tag.getInt("LaoWuMechanicalLaserMaxLifetime"));
        if (tag.contains(DAMAGE_TAG)) {
            attackDamage = Math.max(0.0F, tag.getFloat(DAMAGE_TAG));
        }
        if (tag.contains(DISTANCE_TAG)) {
            double saved=tag.getDouble(DISTANCE_TAG);
            travelledDistance = Double.isFinite(saved)?Math.max(0.0D,saved):0;
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
