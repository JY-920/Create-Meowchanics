package cn.laowu.mod;

import cn.laowu.mod.network.ModNetwork;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;

/**
 * Server-authoritative final charge for a fatally wounded Dynamite Cat.
 * Rendering reads only the compact transition state synchronized by
 * {@link ModNetwork}; no Creeper entity, model, or per-tick packet is used.
 */
public final class DynamiteCatLastStand {
    public static final int FUSE_DURATION_TICKS = 30;

    private static final String ACTIVE_TAG = "LaoWuDynamiteLastStand";
    private static final String FINISHING_TAG = "LaoWuDynamiteLastStandFinishing";
    private static final String FUSE_TICKS_TAG = "LaoWuDynamiteLastStandFuse";
    private static final String CHASE_TICKS_TAG = "LaoWuDynamiteLastStandChase";
    private static final String TARGET_TAG = "LaoWuDynamiteLastStandTarget";
    private static final String CLIENT_FUSE_START_TAG =
            "LaoWuDynamiteLastStandClientFuseStart";

    private static final int TARGET_SEARCH_INTERVAL = 10;
    private static final int MAX_CHASE_TICKS_WITHOUT_CONTACT = 20 * 12;
    private static final double TARGET_SEARCH_RANGE = 32.0D;
    private static final double PRIME_DISTANCE = 2.5D;
    private static final double BLAST_RADIUS = 4.0D;
    private static final double CHASE_SPEED = 1.30D;
    private static final double FUSED_CHASE_SPEED = 1.08D;
    private static final float FINAL_DAMAGE_MULTIPLIER = 10.0F;

    /**
     * Cancels the first real death and converts it into a one-health charge.
     * Void and forced command deaths intentionally remain terminal.
     */
    public static boolean tryBegin(Cat cat, DamageSource source) {
        if (cat.level().isClientSide || isActive(cat) || isFinishing(cat)
                || CatClothesData.getOutfit(cat) != CatOutfitType.DYNAMITE
                || CatPoseData.isPancake(cat) || isForcedDeath(source)) {
            return false;
        }

        CompoundTag data = cat.getPersistentData();
        data.putBoolean(ACTIVE_TAG, true);
        data.remove(FINISHING_TAG);
        data.remove(FUSE_TICKS_TAG);
        data.putInt(CHASE_TICKS_TAG, 0);

        LivingEntity target = preferredTarget(cat, source);
        rememberTarget(cat, target);
        prepareCat(cat, target);
        ModNetwork.syncDynamiteLastStandToTracking(cat);
        return true;
    }

    /** Returns true while ordinary cat behaviour must remain suppressed. */
    public static boolean tick(Cat cat) {
        if (!isActive(cat) || isFinishing(cat)) return false;
        if (!(cat.level() instanceof ServerLevel level)) return true;

        if (CatClothesData.getOutfit(cat) != CatOutfitType.DYNAMITE) {
            finishDeath(cat);
            return true;
        }

        cat.deathTime = 0;
        cat.setHealth(1.0F);
        cat.setRemainingFireTicks(0);
        cat.setOrderedToSit(false);
        cat.setInSittingPose(false);
        cat.setAggressive(true);
        cat.fallDistance = 0.0F;

        CompoundTag data = cat.getPersistentData();
        int chaseTicks = data.getInt(CHASE_TICKS_TAG) + 1;
        data.putInt(CHASE_TICKS_TAG, chaseTicks);

        LivingEntity target = resolveTarget(level, cat);
        if (!isValidTarget(cat, target)
                && (chaseTicks == 1 || chaseTicks % TARGET_SEARCH_INTERVAL == 0)) {
            target = findNearestEnemy(level, cat);
            rememberTarget(cat, target);
        }

        int fuseTicks = fuseTicks(cat);
        if (fuseTicks < 0) {
            if (isValidTarget(cat, target)) {
                steerTowards(cat, target, CHASE_SPEED);
                if (cat.distanceToSqr(target) <= PRIME_DISTANCE * PRIME_DISTANCE) {
                    beginFuse(cat);
                }
            } else {
                cat.setTarget(null);
                cat.getNavigation().stop();
            }

            // Never leave a permanently immortal one-health cat behind when
            // its killer vanished, teleported, or became an invalid target.
            if (chaseTicks >= MAX_CHASE_TICKS_WITHOUT_CONTACT) beginFuse(cat);
            return true;
        }

        if (isValidTarget(cat, target)) {
            steerTowards(cat, target, FUSED_CHASE_SPEED);
        } else {
            cat.getNavigation().stop();
        }

        fuseTicks++;
        data.putInt(FUSE_TICKS_TAG, fuseTicks);
        if (fuseTicks >= FUSE_DURATION_TICKS) detonate(level, cat);
        return true;
    }

    private static void prepareCat(Cat cat, LivingEntity target) {
        cat.deathTime = 0;
        cat.setHealth(1.0F);
        cat.setOrderedToSit(false);
        cat.setInSittingPose(false);
        cat.setAggressive(true);
        cat.getNavigation().stop();
        cat.setTarget(isValidTarget(cat, target) ? target : null);
        if (CatPoseData.getPose(cat) != CatPoseData.NORMAL) {
            CatPoseData.setPose(cat, CatPoseData.NORMAL);
            ModNetwork.syncToTracking(cat, CatPoseData.NORMAL);
        }
    }

    private static void steerTowards(Cat cat, LivingEntity target, double speed) {
        cat.setTarget(target);
        cat.getLookControl().setLookAt(target, 40.0F, 40.0F);
        cat.getNavigation().moveTo(target, speed);
    }

    private static void beginFuse(Cat cat) {
        if (fuseTicks(cat) >= 0) return;
        cat.getPersistentData().putInt(FUSE_TICKS_TAG, 0);
        cat.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.5F);
        cat.gameEvent(GameEvent.PRIME_FUSE);
        ModNetwork.syncDynamiteLastStandToTracking(cat);
    }

    private static void detonate(ServerLevel level, Cat cat) {
        Vec3 center = cat.position().add(0.0D, cat.getBbHeight() * 0.45D, 0.0D);
        float damage = Math.max(1.0F,
                (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE)
                        * FINAL_DAMAGE_MULTIPLIER);

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y, center.z, 1,
                0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                center.x, center.y, center.z, 24,
                0.75D, 0.55D, 0.75D, 0.045D);
        level.sendParticles(ParticleTypes.FLAME,
                center.x, center.y, center.z, 32,
                0.82D, 0.58D, 0.82D, 0.07D);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL,
                1.35F, 0.82F + cat.getRandom().nextFloat() * 0.08F);

        AABB area = new AABB(center, center).inflate(BLAST_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class, area,
                candidate -> isValidTarget(cat, candidate))) {
            if (target.position().distanceToSqr(center) > BLAST_RADIUS * BLAST_RADIUS) {
                continue;
            }
            if (!target.hurt(level.damageSources().explosion(cat, cat), damage)) continue;

            Vec3 away = target.position().subtract(center);
            if (away.lengthSqr() > 1.0E-5D) {
                double distance = Math.sqrt(away.lengthSqr());
                double strength = 1.0D - Math.min(1.0D, distance / BLAST_RADIUS);
                Vec3 impulse = away.normalize().scale(0.35D + 0.55D * strength);
                target.push(impulse.x, 0.18D + 0.18D * strength, impulse.z);
            }
        }

        finishDeath(cat);
    }

    private static void finishDeath(Cat cat) {
        CompoundTag data = cat.getPersistentData();
        data.putBoolean(FINISHING_TAG, true);
        ModNetwork.syncDynamiteLastStandToTracking(cat, false, -1);
        cat.setHealth(1.0F);
        cat.hurt(cat.damageSources().genericKill(), Float.MAX_VALUE);
    }

    private static LivingEntity preferredTarget(Cat cat, DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living && isValidTarget(cat, living)) {
            return living;
        }
        if (isValidTarget(cat, cat.getTarget())) return cat.getTarget();
        if (isValidTarget(cat, cat.getLastHurtByMob())) return cat.getLastHurtByMob();
        LivingEntity owner = cat.getOwner();
        if (owner != null && isValidTarget(cat, owner.getLastHurtMob())) {
            return owner.getLastHurtMob();
        }
        return cat.level() instanceof ServerLevel level
                ? findNearestEnemy(level, cat) : null;
    }

    private static LivingEntity resolveTarget(ServerLevel level, Cat cat) {
        CompoundTag data = cat.getPersistentData();
        if (data.hasUUID(TARGET_TAG)) {
            Entity entity = level.getEntity(data.getUUID(TARGET_TAG));
            if (entity instanceof LivingEntity living && isValidTarget(cat, living)) {
                return living;
            }
        }
        if (isValidTarget(cat, cat.getTarget())) return cat.getTarget();
        return null;
    }

    private static LivingEntity findNearestEnemy(ServerLevel level, Cat cat) {
        LivingEntity owner = cat.getOwner();
        AABB area = cat.getBoundingBox().inflate(TARGET_SEARCH_RANGE);
        return level.getEntitiesOfClass(LivingEntity.class, area,
                        candidate -> isValidTarget(cat, candidate)
                                && (candidate instanceof Enemy
                                || candidate.getLastHurtMob() == cat
                                || (owner != null && candidate.getLastHurtMob() == owner)))
                .stream()
                .min(Comparator.comparingDouble(cat::distanceToSqr))
                .orElse(null);
    }

    private static boolean isValidTarget(Cat cat, LivingEntity target) {
        return target != null && target.isAlive() && target != cat
                && target != cat.getOwner()
                && !(target instanceof Cat)
                && !(target instanceof Player)
                && cat.canAttack(target);
    }

    private static void rememberTarget(Cat cat, LivingEntity target) {
        CompoundTag data = cat.getPersistentData();
        if (isValidTarget(cat, target)) data.putUUID(TARGET_TAG, target.getUUID());
        else data.remove(TARGET_TAG);
    }

    public static boolean isActive(Cat cat) {
        return cat.getPersistentData().getBoolean(ACTIVE_TAG);
    }

    public static boolean isFinishing(Cat cat) {
        return cat.getPersistentData().getBoolean(FINISHING_TAG);
    }

    public static int fuseTicks(Cat cat) {
        CompoundTag data = cat.getPersistentData();
        return data.contains(FUSE_TICKS_TAG, Tag.TAG_ANY_NUMERIC)
                ? Math.max(0, data.getInt(FUSE_TICKS_TAG)) : -1;
    }

    public static boolean protectsFrom(DamageSource source) {
        return !isForcedDeath(source);
    }

    private static boolean isForcedDeath(DamageSource source) {
        return source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)
                || source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL);
    }

    /** Applies one transition packet to the client-side copy of a cat. */
    public static void setClientState(Cat cat, boolean active, int fuseTicks) {
        CompoundTag data = cat.getPersistentData();
        if (!active) {
            clearTransientState(data);
            return;
        }
        data.putBoolean(ACTIVE_TAG, true);
        data.remove(FINISHING_TAG);
        if (fuseTicks >= 0) {
            data.putInt(FUSE_TICKS_TAG, fuseTicks);
            data.putInt(CLIENT_FUSE_START_TAG, cat.tickCount - fuseTicks);
        } else {
            data.remove(FUSE_TICKS_TAG);
            data.remove(CLIENT_FUSE_START_TAG);
        }
    }

    /** Normalized vanilla-Creeper-style fuse progress used by the renderer. */
    public static float swelling(Cat cat, float partialTick) {
        if (!isActive(cat) || fuseTicks(cat) < 0) return 0.0F;
        CompoundTag data = cat.getPersistentData();
        float ticks = cat.level().isClientSide
                && data.contains(CLIENT_FUSE_START_TAG, Tag.TAG_ANY_NUMERIC)
                ? cat.tickCount - data.getInt(CLIENT_FUSE_START_TAG) + partialTick
                : fuseTicks(cat) + partialTick;
        return ticks / (FUSE_DURATION_TICKS - 2.0F);
    }

    /** The same alternating white-overlay curve used by the vanilla Creeper. */
    public static float whiteOverlayProgress(Cat cat, float partialTick) {
        float swelling = swelling(cat, partialTick);
        if ((int) (swelling * 10.0F) % 2 == 0) return 0.0F;
        return Mth.clamp(swelling, 0.5F, 1.0F);
    }

    /** Removes death-charge state from captured pancakes and restored cats. */
    public static void clearTransientState(CompoundTag data) {
        data.remove(ACTIVE_TAG);
        data.remove(FINISHING_TAG);
        data.remove(FUSE_TICKS_TAG);
        data.remove(CHASE_TICKS_TAG);
        data.remove(TARGET_TAG);
        data.remove(CLIENT_FUSE_START_TAG);
    }

    /** Priority-zero lock that keeps ordinary movement goals out of the charge. */
    public static final class ControlGoal extends Goal {
        private final Cat cat;

        public ControlGoal(Cat cat) {
            this.cat = cat;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return isActive(cat) && !isFinishing(cat);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    private DynamiteCatLastStand() {}
}
