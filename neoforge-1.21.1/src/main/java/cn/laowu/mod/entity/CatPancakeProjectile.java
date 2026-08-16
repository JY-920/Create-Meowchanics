package cn.laowu.mod.entity;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A recoverable cat pancake projectile with a small, non-destructive impact blast. */
public final class CatPancakeProjectile extends ThrowableItemProjectile {
    private static final String DAMAGE_TAG = "LaoWuPancakeDamage";
    private static final String RADIUS_TAG = "LaoWuPancakeRadius";

    private float impactDamage = 2.0F;
    private float impactRadius = 1.0F;

    public CatPancakeProjectile(EntityType<? extends CatPancakeProjectile> type, Level level) {
        super(type, level);
    }

    public CatPancakeProjectile(Level level, LivingEntity owner, ItemStack stack,
                                float impactDamage, float impactRadius) {
        super(LaoWuMod.CAT_PANCAKE_PROJECTILE.get(), owner, level);
        setItem(stack.copyWithCount(1));
        this.impactDamage = impactDamage;
        this.impactRadius = impactRadius;
    }

    @Override
    protected Item getDefaultItem() {
        return LaoWuMod.CAT_PANCAKE.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05D;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel serverLevel) || isRemoved()) return;

        Vec3 impact = result.getLocation();
        Entity owner = getOwner();
        AABB searchArea = new AABB(impact, impact).inflate(impactRadius);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class,
                searchArea, target -> target.isAlive() && target != owner)) {
            // Measure to the nearest point on the hitbox instead of the entity's
            // centre. This stays reliable for tall mobs, slopes and edge hits.
            AABB box = target.getBoundingBox();
            double nearestX = Math.max(box.minX, Math.min(impact.x, box.maxX));
            double nearestY = Math.max(box.minY, Math.min(impact.y, box.maxY));
            double nearestZ = Math.max(box.minZ, Math.min(impact.z, box.maxZ));
            if (impact.distanceToSqr(nearestX, nearestY, nearestZ)
                    > impactRadius * impactRadius) continue;
            target.hurt(serverLevel.damageSources().thrown(this, owner), impactDamage);
        }

        float charge = Math.max(0.0F, Math.min(1.0F, (impactRadius - 1.0F) / 2.0F));
        int explosionParticles = 2 + Math.round(charge * 6.0F);
        int poofParticles = 10 + Math.round(charge * 30.0F);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                impact.x, impact.y + 0.18D, impact.z, explosionParticles,
                impactRadius * 0.35D, impactRadius * 0.18D, impactRadius * 0.35D,
                0.015D + charge * 0.025D);
        serverLevel.sendParticles(ParticleTypes.POOF,
                impact.x, impact.y + 0.18D, impact.z, poofParticles,
                impactRadius * 0.52D, impactRadius * 0.25D, impactRadius * 0.52D,
                0.035D + charge * 0.045D);
        ModNetwork.playLogisticsSound(serverLevel, BlockPos.containing(impact), true);

        ItemEntity dropped = new ItemEntity(serverLevel,
                impact.x, impact.y + 0.12D, impact.z, getItem().copy());
        dropped.setDeltaMovement(0.0D, 0.08D, 0.0D);
        dropped.setPickUpDelay(10);
        serverLevel.addFreshEntity(dropped);
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat(DAMAGE_TAG, impactDamage);
        tag.putFloat(RADIUS_TAG, impactRadius);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(DAMAGE_TAG)) impactDamage = tag.getFloat(DAMAGE_TAG);
        if (tag.contains(RADIUS_TAG)) impactRadius = tag.getFloat(RADIUS_TAG);
    }

}
