package cn.laowu.mod.entity;

import cn.laowu.mod.LaoWuMod;
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
public final class MechanicalLaserProjectile extends ThrowableItemProjectile {
    public static final double MAX_TRAVEL_DISTANCE = 16.5D;
    private static final String DAMAGE_TAG = "LaoWuMechanicalLaserDamage";
    private static final String DISTANCE_TAG = "LaoWuMechanicalLaserDistance";
    private static final DustParticleOptions LASER_DUST = new DustParticleOptions(
            new Vector3f(1.0F, 0.16F, 0.56F), 0.8F);

    private float attackDamage = 2.0F;
    private double travelledDistance;

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
        super.tick();
        travelledDistance += before.distanceTo(position());

        if (level().isClientSide) {
            level().addParticle(LASER_DUST, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        } else if (travelledDistance >= MAX_TRAVEL_DISTANCE || tickCount > 20) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity)
                && !(entity instanceof Cat)
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

        target.hurt(level.damageSources().mobProjectile(this, cat), attackDamage);
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(DAMAGE_TAG)) {
            attackDamage = Math.max(0.0F, tag.getFloat(DAMAGE_TAG));
        }
        if (tag.contains(DISTANCE_TAG)) {
            travelledDistance = Math.max(0.0D, tag.getDouble(DISTANCE_TAG));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
