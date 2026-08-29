package cn.laowu.mod.entity;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

/** Heavy ranged shot used by honey-gathering cats. */
public final class HoneyMissileProjectile extends ThrowableItemProjectile {
    public static final double MAX_TRAVEL_DISTANCE = 13.0D;
    private static final String DAMAGE_TAG = "LaoWuHoneyMissileDamage";
    private static final String DISTANCE_TAG = "LaoWuHoneyMissileDistance";
    private static final int SLOWNESS_DURATION_TICKS = 30;

    private float attackDamage = 2.0F;
    private double travelledDistance;

    public HoneyMissileProjectile(EntityType<? extends HoneyMissileProjectile> type,
                                  Level level) {
        super(type, level);
    }

    public HoneyMissileProjectile(Level level, Cat owner, float attackDamage) {
        super(LaoWuMod.HONEY_MISSILE_PROJECTILE.get(), owner, level);
        this.attackDamage = Math.max(0.0F, attackDamage);
    }

    @Override
    protected Item getDefaultItem() {
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
            if ((tickCount & 1) == 0) {
                level().addParticle(ParticleTypes.FALLING_HONEY,
                        getX(), getY(), getZ(), 0.0D, -0.01D, 0.0D);
            }
        } else if (travelledDistance >= MAX_TRAVEL_DISTANCE || tickCount > 30) {
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
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                SLOWNESS_DURATION_TICKS, 1), cat);
        spawnImpact(level, result.getLocation(), 16);
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level() instanceof ServerLevel level) {
            spawnImpact(level, result.getLocation(), 9);
        }
        discard();
    }

    private static void spawnImpact(ServerLevel level, Vec3 hit, int count) {
        level.sendParticles(ParticleTypes.LANDING_HONEY,
                hit.x, hit.y, hit.z, count,
                0.16D, 0.16D, 0.16D, 0.04D);
        level.playSound(null, hit.x, hit.y, hit.z,
                SoundEvents.HONEY_BLOCK_BREAK, SoundSource.NEUTRAL,
                0.65F, 1.15F);
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
