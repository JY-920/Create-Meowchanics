package cn.laowu.mod.entity;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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

/** A fishing cat's visible rod shot, always knocking its target away. */
public final class FishingRodProjectile extends ThrowableItemProjectile {
    private static final String DAMAGE_TAG = "LaoWuFishingRodDamage";
    private static final int MAX_LIFETIME = 60;
    private static final EntityDataAccessor<Integer> CAT_OWNER_ID =
            SynchedEntityData.defineId(FishingRodProjectile.class,
                    EntityDataSerializers.INT);

    private float attackDamage = 2.0F;

    public FishingRodProjectile(EntityType<? extends FishingRodProjectile> type, Level level) {
        super(type, level);
    }

    public FishingRodProjectile(Level level, Cat owner, float attackDamage) {
        super(LaoWuMod.FISHING_ROD_PROJECTILE.get(), owner, level);
        this.attackDamage = Math.max(0.0F, attackDamage);
        entityData.set(CAT_OWNER_ID, owner.getId());
        setItem(Items.FISHING_ROD.getDefaultInstance());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CAT_OWNER_ID, -1);
    }

    /** Resolves the firing cat on both the server and remote clients. */
    public Cat getCatOwnerForRender() {
        Entity owner = getOwner();
        if (owner instanceof Cat cat) return cat;
        int ownerId = entityData.get(CAT_OWNER_ID);
        Entity syncedOwner = ownerId < 0 ? null : level().getEntity(ownerId);
        return syncedOwner instanceof Cat cat ? cat : null;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FISHING_ROD;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.025D;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > MAX_LIFETIME) discard();
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

        if (target.hurt(level.damageSources().thrown(this, cat), attackDamage)) {
            Vec3 direction = target.position().subtract(cat.position());
            Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
            if (horizontal.lengthSqr() > 1.0E-5D) {
                Vec3 impulse = horizontal.normalize().scale(0.45D);
                target.setDeltaMovement(target.getDeltaMovement().scale(0.35D)
                        .add(impulse.x, 0.16D, impulse.z));
                target.hasImpulse = true;
            }
        }

        level.sendParticles(ParticleTypes.POOF,
                target.getX(), target.getY(0.55D), target.getZ(),
                8, target.getBbWidth() * 0.3D,
                target.getBbHeight() * 0.2D, target.getBbWidth() * 0.3D, 0.08D);
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level() instanceof ServerLevel level) {
            Vec3 hit = result.getLocation();
            level.sendParticles(ParticleTypes.POOF, hit.x, hit.y, hit.z,
                    5, 0.12D, 0.12D, 0.12D, 0.03D);
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat(DAMAGE_TAG, attackDamage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(DAMAGE_TAG)) attackDamage = Math.max(0.0F, tag.getFloat(DAMAGE_TAG));
    }

}
