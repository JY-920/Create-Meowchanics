package cn.laowu.mod.entity;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/** A homing Create cardboard package that delivers one logistics-cat buff. */
public final class LogisticsSupportProjectile extends ThrowableItemProjectile {
    private static final ResourceLocation PACKAGE_ID =
            ResourceLocation.fromNamespaceAndPath("create", "cardboard_package_10x8");
    private static final String TARGET_TAG = "LaoWuLogisticsTarget";
    private static final String EFFECT_TAG = "LaoWuLogisticsEffect";
    private static final String DURATION_TAG = "LaoWuLogisticsDuration";
    private static final int MAX_LIFETIME = 40;

    private UUID targetUuid;
    private ResourceLocation effectId;
    private int durationTicks;

    public LogisticsSupportProjectile(EntityType<? extends LogisticsSupportProjectile> type,
                                      Level level) {
        super(type, level);
    }

    public LogisticsSupportProjectile(Level level, Cat owner, Cat target,
                                      MobEffect effect, int durationTicks) {
        super(LaoWuMod.LOGISTICS_SUPPORT_PROJECTILE.get(), owner, level);
        this.targetUuid = target.getUUID();
        this.effectId = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        this.durationTicks = Math.max(1, durationTicks);
        setItem(new ItemStack(packageItem()));
    }

    @Override
    protected Item getDefaultItem() {
        return packageItem();
    }

    private static Item packageItem() {
        Item item = ForgeRegistries.ITEMS.getValue(PACKAGE_ID);
        return item == null || item == Items.AIR ? Items.PAPER : item;
    }

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;
        if (!(level() instanceof ServerLevel level)) return;
        if (tickCount > MAX_LIFETIME) {
            discard();
            return;
        }

        Entity targetEntity = targetUuid == null ? null : level.getEntity(targetUuid);
        if (!(targetEntity instanceof Cat target) || !target.isAlive()) {
            discard();
            return;
        }

        Vec3 destination = target.getBoundingBox().getCenter();
        Vec3 offset = destination.subtract(position());
        if (offset.lengthSqr() <= 0.35D * 0.35D) {
            deliver(level, target);
            return;
        }
        Vec3 desired = offset.normalize().scale(0.92D);
        setDeltaMovement(getDeltaMovement().scale(0.28D).add(desired.scale(0.72D)));
        hasImpulse = true;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (!super.canHitEntity(entity)) return false;
        return targetUuid == null || targetUuid.equals(entity.getUUID());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level() instanceof ServerLevel level
                && result.getEntity() instanceof Cat target
                && (targetUuid == null || targetUuid.equals(target.getUUID()))) {
            deliver(level, target);
        }
    }

    private void deliver(ServerLevel level, Cat target) {
        MobEffect effect = effectId == null ? null
                : ForgeRegistries.MOB_EFFECTS.getValue(effectId);
        Entity owner = getOwner();
        if (effect != null && owner instanceof Cat cat && cat.isAlive()
                && target.isAlive()) {
            target.addEffect(new MobEffectInstance(effect, durationTicks, 0,
                    false, true, true), cat);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    target.getX(), target.getY(0.6D), target.getZ(),
                    8, 0.24D, 0.20D, 0.24D, 0.03D);
            level.playSound(null, target.blockPosition(), SoundEvents.ITEM_PICKUP,
                    SoundSource.NEUTRAL, 0.55F,
                    1.15F + cat.getRandom().nextFloat() * 0.2F);
        }
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
        if (targetUuid != null) tag.putUUID(TARGET_TAG, targetUuid);
        if (effectId != null) tag.putString(EFFECT_TAG, effectId.toString());
        tag.putInt(DURATION_TAG, durationTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        targetUuid = tag.hasUUID(TARGET_TAG) ? tag.getUUID(TARGET_TAG) : null;
        effectId = tag.contains(EFFECT_TAG) ? ResourceLocation.tryParse(
                tag.getString(EFFECT_TAG)) : null;
        durationTicks = Math.max(1, tag.getInt(DURATION_TAG));
        if (getItem().isEmpty()) setItem(new ItemStack(packageItem()));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
