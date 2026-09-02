package cn.laowu.mod.entity;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** A tumbling, non-block-breaking explosive thrown by a dynamite career cat. */
public final class DynamiteProjectile extends ThrowableItemProjectile {
    public static final double MAX_TRAVEL_DISTANCE = 10.0D;
    private static final double BLAST_RADIUS = 2.75D;
    private static final String DAMAGE_TAG = "LaoWuDynamiteDamage";
    private static final String DISTANCE_TAG = "LaoWuDynamiteDistance";

    private float attackDamage = 2.0F;
    private double travelledDistance;

    public DynamiteProjectile(EntityType<? extends DynamiteProjectile> type,
                              Level level) {
        super(type, level);
    }

    public DynamiteProjectile(Level level, Cat owner, float attackDamage) {
        super(LaoWuMod.DYNAMITE_PROJECTILE.get(), owner, level);
        this.attackDamage = Math.max(0.0F, attackDamage);
    }

    @Override
    protected Item getDefaultItem() {
        // The supplied Blockbench model is rendered by DynamiteProjectileRenderer.
        return Items.AIR;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.035D;
    }

    @Override
    public void tick() {
        Vec3 before = position();
        super.tick();
        travelledDistance += before.distanceTo(position());

        if (level().isClientSide) {
            level().addParticle(ParticleTypes.SMOKE,
                    getX(), getY() + 0.08D, getZ(), 0.0D, 0.01D, 0.0D);
            if ((tickCount & 1) == 0) {
                level().addParticle(ParticleTypes.FLAME,
                        getX(), getY() + 0.08D, getZ(), 0.0D, 0.0D, 0.0D);
            }
        } else if (travelledDistance >= MAX_TRAVEL_DISTANCE || tickCount > 45) {
            detonate(position());
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
        detonate(result.getLocation());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        detonate(result.getLocation());
    }

    private void detonate(Vec3 center) {
        if (!(level() instanceof ServerLevel level) || isRemoved()) return;

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y, center.z, 1,
                0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                center.x, center.y, center.z, 14,
                0.48D, 0.35D, 0.48D, 0.035D);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL,
                0.85F, 1.15F + random.nextFloat() * 0.12F);

        Entity owner = getOwner();
        if (owner instanceof Cat cat && cat.isAlive()) {
            AABB area = new AABB(center, center).inflate(BLAST_RADIUS);
            for (LivingEntity target : level.getEntitiesOfClass(
                    LivingEntity.class, area,
                    candidate -> candidate.isAlive()
                            && candidate != cat
                            && candidate != cat.getOwner()
                            && !(candidate instanceof Cat)
                            && !(candidate instanceof Player)
                            && cat.canAttack(candidate))) {
                double distance = target.position().distanceTo(center);
                if (distance > BLAST_RADIUS) continue;
                double strength = 1.0D - distance / BLAST_RADIUS;
                float damage = (float) (attackDamage * (0.55D + 0.45D * strength));
                if (!target.hurt(level.damageSources().mobProjectile(this, cat), damage)) continue;

                Vec3 away = target.position().subtract(center);
                if (away.lengthSqr() > 1.0E-5D) {
                    double knockback = 0.18D + 0.30D * strength;
                    Vec3 impulse = away.normalize().scale(knockback);
                    target.push(impulse.x, Math.max(0.08D, impulse.y + 0.08D), impulse.z);
                }
            }
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

}
