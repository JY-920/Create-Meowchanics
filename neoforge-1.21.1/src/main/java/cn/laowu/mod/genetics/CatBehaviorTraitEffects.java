package cn.laowu.mod.genetics;

import cn.laowu.mod.CatClothesData;
import cn.laowu.mod.CatLogisticsBehavior;
import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.CatPancakeBehavior;
import cn.laowu.mod.CatPoseData;
import cn.laowu.mod.CatProfileData;
import cn.laowu.mod.HissingCatBehavior;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.CatAttributeCanItem;
import cn.laowu.mod.item.CatTraitFishItem;
import cn.laowu.mod.item.BreedingOnlyCatCanItem;
import cn.laowu.mod.network.ModNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Server-owned behaviour traits. Expensive searches are staggered and their
 * targets are cached in persistent entity data, so adding cats does not create
 * a synchronized every-tick world scan.
 */
public final class CatBehaviorTraitEffects {
    private static final TagKey<net.minecraft.world.level.block.Block> CHEWABLE_REDSTONE =
            BlockTags.create(ResourceLocation.fromNamespaceAndPath(
                    LaoWuMod.MOD_ID, "cat_chewable_redstone"));

    private static final String TARGET_TAG = "LaoWuTraitBehaviourTarget";
    private static final String TARGET_KIND_TAG = "LaoWuTraitBehaviourTargetKind";
    private static final String ATTACK_COOLDOWN_TAG = "LaoWuTraitBehaviourAttackCooldown";
    private static final String FUR_TIMER_TAG = "LaoWuTraitFurTimer";
    private static final String BELT_TARGET_TAG = "LaoWuTraitBeltTarget";
    private static final String LEGACY_HEAT_TARGET_TAG = "LaoWuTraitHeatTarget";
    private static final String CHEST_TARGET_TAG = "LaoWuTraitChestTarget";
    private static final String MINECART_TARGET_TAG = "LaoWuTraitMinecartTarget";
    private static final String SPLASH_FLEE_UNTIL_TAG = "LaoWuTraitSplashFleeUntil";
    private static final String SPLASH_FLEE_X_TAG = "LaoWuTraitSplashFleeX";
    private static final String SPLASH_FLEE_Z_TAG = "LaoWuTraitSplashFleeZ";
    private static final String STRICT_HISSING_TAG = "LaoWuTraitStrictHissing";
    private static final String STRICT_TARGET_TAG = "LaoWuTraitStrictTarget";
    private static final String STRICT_ATTACKING_TAG = "LaoWuTraitStrictAttacking";
    private static final String STRICT_ATTACK_COOLDOWN_TAG =
            "LaoWuTraitStrictAttackCooldown";
    private static final String SKY_ACTIVE_TAG = "LaoWuTraitSkyActive";
    private static final String SKY_TARGET_X_TAG = "LaoWuTraitSkyTargetX";
    private static final String SKY_TARGET_Y_TAG = "LaoWuTraitSkyTargetY";
    private static final String SKY_TARGET_Z_TAG = "LaoWuTraitSkyTargetZ";
    private static final String SKY_RETARGET_AT_TAG = "LaoWuTraitSkyRetargetAt";
    private static final String SKY_ANCHOR_X_TAG = "LaoWuTraitSkyAnchorX";
    private static final String SKY_ANCHOR_Y_TAG = "LaoWuTraitSkyAnchorY";
    private static final String SKY_ANCHOR_Z_TAG = "LaoWuTraitSkyAnchorZ";
    private static final String ATTACH_TARGET_TAG = "LaoWuTraitAttachTarget";
    private static final String LEGACY_ATTACH_PLAYER_TAG = "LaoWuTraitAttachPlayer";
    private static final String ATTACH_UNTIL_TAG = "LaoWuTraitAttachUntil";
    private static final String HIGH_STEP_ACTIVE_TAG = "LaoWuTraitHighStepActive";
    private static final String HIGH_STEP_PREVIOUS_TAG = "LaoWuTraitPreviousStepHeight";

    private static final int FUR_INTERVAL_TICKS = 20 * 60 * 3;
    private static final double ACTIVE_SEARCH_RANGE = 12.0D;

    /** @return true when a trait owns movement/hissing for this tick. */
    public static boolean tick(Cat cat) {
        if (cat.level().isClientSide || CatPoseData.isPancake(cat)) return false;
        CatTraitProfile traits = CatTraitData.ensure(cat);
        maintainPassiveEffects(cat, traits);

        if (tickFluidPanic(cat, traits)) return true;
        if (tickSelectedElder(cat, traits)) return true;
        if (tickSkyCat(cat, traits)) return true;
        if (tickAutoAttach(cat, traits)) return true;
        if (tickMinecartChaser(cat, traits)) return true;
        if (tickBeltSeeker(cat, traits)) return true;
        if (tickBootThief(cat, traits)) return true;
        return tickCombatBehaviour(cat, traits);
    }

    private static void maintainPassiveEffects(Cat cat, CatTraitProfile traits) {
        maintainStepHeight(cat, traits.has(CatTrait.HIGH_STEP));

        if ((traits.has(CatTrait.ANOREXIA) || traits.has(CatTrait.CUDDLE_ONLY))
                && cat.isInLove()) {
            cat.resetLove();
        }
        if (cat.getPersistentData().contains(LEGACY_HEAT_TARGET_TAG, Tag.TAG_LONG)) {
            cat.getPersistentData().remove(LEGACY_HEAT_TARGET_TAG);
            cat.setLying(false);
            // cat.setRelaxStateOne(false); // package-private in 1.21.1
        }

        if (traits.has(CatTrait.ROLLING_LOG)) {
            cat.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,
                    40, 0, true, false, false));
        }

        if (traits.has(CatTrait.DROWNING) && cat.isInWater()) {
            cat.setSwimming(false);
            Vec3 movement = cat.getDeltaMovement();
            cat.setDeltaMovement(movement.x * 0.82D,
                    Math.min(movement.y, -0.055D), movement.z * 0.82D);
        }

        if (traits.has(CatTrait.DING_DONG_CAT) && staggered(cat, 4)) {
            leaveSnow(cat);
        }
        if (traits.has(CatTrait.TOM_TREE_FELLER) && staggered(cat, 10)) {
            breakBlockBelow(cat, BlockTags.LOGS);
        }
        if (traits.has(CatTrait.CABLE_BITER) && staggered(cat, 60)) {
            chewNearbyRedstone(cat);
        }
        if (traits.has(CatTrait.HIGH_EXPLOSIVE_FUEL) && staggered(cat, 20)
                && CatClothesData.getOutfit(cat) == CatOutfitType.FIRE) {
            igniteNearbyEntities(cat);
        }

        boolean sheddingOnBelt = traits.has(CatTrait.SHEDDING) && isOnRunningBelt(cat);
        boolean timedShedding = traits.has(CatTrait.EDWARD) || sheddingOnBelt;
        CompoundTag data = cat.getPersistentData();
        if (!timedShedding) {
            data.remove(FUR_TIMER_TAG);
        } else {
            int ticks = data.getInt(FUR_TIMER_TAG) + 1;
            if (ticks >= FUR_INTERVAL_TICKS) {
                ticks = 0;
                cat.spawnAtLocation(new ItemStack(LaoWuMod.CAT_FUR.get()));
                cat.playSound(SoundEvents.WOOL_HIT, 0.75F, 1.3F);
            }
            data.putInt(FUR_TIMER_TAG, ticks);
        }
    }

    private static void maintainStepHeight(Cat cat, boolean active) {
        CompoundTag data = cat.getPersistentData();
        if (active) {
            if (!data.getBoolean(HIGH_STEP_ACTIVE_TAG)) {
                data.putBoolean(HIGH_STEP_ACTIVE_TAG, true);
                data.putFloat(HIGH_STEP_PREVIOUS_TAG, cat.maxUpStep());
            }
            // cat.setMaxUpStep(3.0F); // no public setter in 1.21.1
        } else if (data.getBoolean(HIGH_STEP_ACTIVE_TAG)) {
            // cat.setMaxUpStep(data.contains(HIGH_STEP_PREVIOUS_TAG, Tag.TAG_FLOAT)
            //         ? data.getFloat(HIGH_STEP_PREVIOUS_TAG) : 0.6F); // no public setter in 1.21.1
            data.remove(HIGH_STEP_ACTIVE_TAG);
            data.remove(HIGH_STEP_PREVIOUS_TAG);
        }
    }

    private static void leaveSnow(Cat cat) {
        if (!mobGriefing(cat)) return;
        BlockPos pos = cat.blockPosition();
        Level level = cat.level();
        if (level.getBlockState(pos).isAir()
                && level.getBlockState(pos.below()).isFaceSturdy(
                level, pos.below(), Direction.UP)) {
            level.setBlock(pos, Blocks.SNOW.defaultBlockState(), 3);
        }
    }

    private static void breakBlockBelow(Cat cat,
                                        TagKey<net.minecraft.world.level.block.Block> tag) {
        if (!mobGriefing(cat)) return;
        BlockPos pos = cat.getOnPos();
        if (cat.level().getBlockState(pos).is(tag)) {
            cat.level().destroyBlock(pos, true, cat);
            cat.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }

    private static void chewNearbyRedstone(Cat cat) {
        if (!mobGriefing(cat)) return;
        BlockPos origin = cat.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(
                origin.offset(-3, -2, -3), origin.offset(3, 2, 3))) {
            if (!cat.level().getBlockState(candidate).is(CHEWABLE_REDSTONE)) continue;
            double distance = candidate.distSqr(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate.immutable();
            }
        }
        if (best != null && cat.level().destroyBlock(best, true, cat)) {
            cat.playSound(SoundEvents.GENERIC_EAT, 0.8F, 1.4F);
        }
    }

    private static void igniteNearbyEntities(Cat cat) {
        for (LivingEntity target : cat.level().getEntitiesOfClass(LivingEntity.class,
                cat.getBoundingBox().inflate(1.5D),
                target -> target != cat && target.isAlive())) {
            target.igniteForTicks(60);
        }
        if (cat.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.FLAME,
                    cat.getX(), cat.getY(0.5D), cat.getZ(),
                    14, 1.15D, 0.65D, 1.15D, 0.025D);
        }
    }

    private static boolean tickFluidPanic(Cat cat, CatTraitProfile traits) {
        CompoundTag data = cat.getPersistentData();
        if (!traits.has(CatTrait.CODE_CONFLICT)) {
            data.remove(SPLASH_FLEE_UNTIL_TAG);
            return false;
        }
        long now = cat.level().getGameTime();
        boolean touchingFluid = !cat.level().getFluidState(cat.blockPosition()).isEmpty();
        if (CatPoseData.isHissing(cat) && touchingFluid) {
            startFluidPanic(cat, cat.position());
        }
        if (data.getLong(SPLASH_FLEE_UNTIL_TAG) <= now) return false;

        clearHissing(cat);
        cat.getNavigation().moveTo(data.getDouble(SPLASH_FLEE_X_TAG),
                cat.getY(), data.getDouble(SPLASH_FLEE_Z_TAG), 1.6D);
        return true;
    }

    public static void notifyFluidSplash(Cat cat, Vec3 splashPosition) {
        if (cat.level().isClientSide || !CatPoseData.isHissing(cat)
                || !CatTraitData.ensure(cat).has(CatTrait.CODE_CONFLICT)) return;
        startFluidPanic(cat, splashPosition);
    }

    private static void startFluidPanic(Cat cat, Vec3 source) {
        HissingCatBehavior.interruptPair(cat, 50);
        Vec3 away = cat.position().subtract(source);
        if (away.horizontalDistanceSqr() < 1.0E-4D) {
            float angle = cat.getRandom().nextFloat() * Mth.TWO_PI;
            away = new Vec3(Mth.cos(angle), 0.0D, Mth.sin(angle));
        } else {
            away = new Vec3(away.x, 0.0D, away.z).normalize();
        }
        CompoundTag data = cat.getPersistentData();
        data.putLong(SPLASH_FLEE_UNTIL_TAG, cat.level().getGameTime() + 50L);
        data.putDouble(SPLASH_FLEE_X_TAG, cat.getX() + away.x * 6.0D);
        data.putDouble(SPLASH_FLEE_Z_TAG, cat.getZ() + away.z * 6.0D);
    }

    private static boolean tickSelectedElder(Cat cat, CatTraitProfile traits) {
        CompoundTag data = cat.getPersistentData();
        if (!traits.has(CatTrait.SELECTED_ELDER)) {
            if (data.getBoolean(STRICT_HISSING_TAG)
                    || data.getBoolean(STRICT_ATTACKING_TAG)
                    || data.hasUUID(STRICT_TARGET_TAG)) clearStrictHissing(cat);
            return false;
        }
        if (HissingCatBehavior.isPairInterrupted(cat)) {
            if (data.getBoolean(STRICT_HISSING_TAG)
                    || data.getBoolean(STRICT_ATTACKING_TAG)) clearStrictHissing(cat);
            return false;
        }

        LivingEntity target = data.hasUUID(STRICT_TARGET_TAG)
                ? resolveEntity(cat, data.getUUID(STRICT_TARGET_TAG)) : null;
        if (data.getBoolean(STRICT_ATTACKING_TAG)) {
            if (target == cat || target == null || !target.isAlive()
                    || cat.distanceToSqr(target) > 16.0D * 16.0D) {
                clearStrictAttack(cat, target);
                target = null;
            } else {
                stopStrictHissingPose(cat);
                cat.setTarget(target);
                cat.getLookControl().setLookAt(target, 90.0F, 90.0F);
                face(cat, target.position());
                double reach = Math.max(1.2D,
                        cat.getBbWidth() + target.getBbWidth() * 0.55D);
                if (cat.distanceToSqr(target) > reach * reach) {
                    cat.getNavigation().moveTo(target, 1.3D);
                } else {
                    cat.getNavigation().stop();
                    int cooldown = data.getInt(STRICT_ATTACK_COOLDOWN_TAG);
                    if (cooldown <= 0) {
                        cat.doHurtTarget(target);
                        data.putInt(STRICT_ATTACK_COOLDOWN_TAG,
                                CatAttributeEffects.attackIntervalTicks(cat));
                    } else {
                        data.putInt(STRICT_ATTACK_COOLDOWN_TAG, cooldown - 1);
                    }
                }
                return true;
            }
        }

        if (target == cat || target != null && (!target.isAlive()
                || cat.distanceToSqr(target) > 25.0D)) target = null;
        if (target == null || staggered(cat, 10)) {
            target = nearestLiving(cat, 5.0D,
                    living -> living != cat && living.isAlive());
            if (target != null) data.putUUID(STRICT_TARGET_TAG, target.getUUID());
        }
        if (target == null) {
            clearStrictHissing(cat);
            return false;
        }
        data.putBoolean(STRICT_HISSING_TAG, true);
        if (!CatPoseData.isHissing(cat)) {
            CatPoseData.setPose(cat, CatPoseData.HISSING);
            ModNetwork.syncToTracking(cat, CatPoseData.HISSING);
        }
        cat.getNavigation().stop();
        cat.getLookControl().setLookAt(target, 90.0F, 90.0F);
        face(cat, target.position());
        ModNetwork.setAudioSession(cat, true);
        return true;
    }

    /** A selected elder retaliates against the creature it was already hissing at. */
    public static void onSelectedElderHurt(Cat cat, Entity damageSource) {
        if (cat.level().isClientSide || CatPoseData.isPancake(cat)
                || !CatTraitData.ensure(cat).has(CatTrait.SELECTED_ELDER)) return;
        CompoundTag data = cat.getPersistentData();
        LivingEntity target = data.hasUUID(STRICT_TARGET_TAG)
                ? resolveEntity(cat, data.getUUID(STRICT_TARGET_TAG)) : null;
        if (target == cat || target != null && !target.isAlive()) target = null;
        if (target == null && damageSource instanceof LivingEntity living
                && living != cat && living.isAlive()) target = living;
        if (target == null) return;

        data.putUUID(STRICT_TARGET_TAG, target.getUUID());
        data.putBoolean(STRICT_ATTACKING_TAG, true);
        data.remove(STRICT_ATTACK_COOLDOWN_TAG);
        stopStrictHissingPose(cat);
        cat.setOrderedToSit(false);
        cat.setTarget(target);
    }

    private static void stopStrictHissingPose(Cat cat) {
        cat.getPersistentData().remove(STRICT_HISSING_TAG);
        clearHissing(cat);
    }

    private static void clearStrictAttack(Cat cat, LivingEntity target) {
        CompoundTag data = cat.getPersistentData();
        data.remove(STRICT_ATTACKING_TAG);
        data.remove(STRICT_ATTACK_COOLDOWN_TAG);
        data.remove(STRICT_TARGET_TAG);
        if (target == null || cat.getTarget() == target) cat.setTarget(null);
    }

    private static void clearStrictHissing(Cat cat) {
        CompoundTag data = cat.getPersistentData();
        LivingEntity target = data.hasUUID(STRICT_TARGET_TAG)
                ? resolveEntity(cat, data.getUUID(STRICT_TARGET_TAG)) : null;
        data.remove(STRICT_HISSING_TAG);
        data.remove(STRICT_TARGET_TAG);
        data.remove(STRICT_ATTACKING_TAG);
        data.remove(STRICT_ATTACK_COOLDOWN_TAG);
        if (target == null || cat.getTarget() == target) cat.setTarget(null);
        clearHissing(cat);
    }

    private static boolean tickSkyCat(Cat cat, CatTraitProfile traits) {
        CompoundTag data = cat.getPersistentData();
        if (!traits.has(CatTrait.SKY_CAT)) {
            if (data.getBoolean(SKY_ACTIVE_TAG) || cat.isNoGravity()) {
                clearSkyState(cat);
            }
            return false;
        }
        // Sitting is an explicit player command and must win over autonomous
        // flight. Resetting the anchor here makes the next take-off start from
        // the cat's new resting position instead of an obsolete altitude.
        if (cat.isOrderedToSit()) {
            if (data.getBoolean(SKY_ACTIVE_TAG) || cat.isNoGravity()) clearSkyState(cat);
            return false;
        }

        long now = cat.level().getGameTime();
        if (!data.getBoolean(SKY_ACTIVE_TAG)) {
            setSkyAnchor(data, cat.position());
        }
        Vec3 centre = skyCentre(cat, data);
        Vec3 existingTarget = skyTarget(data);
        boolean escapedFlightBand = horizontalDistanceSqr(cat.position(), centre) > 14.0D * 14.0D
                || cat.getY() > centre.y + 7.0D
                || cat.getY() < centre.y - 3.0D;
        if (!data.getBoolean(SKY_ACTIVE_TAG)
                || data.getLong(SKY_RETARGET_AT_TAG) <= now
                || cat.position().distanceToSqr(existingTarget) < 2.0D
                || horizontalDistanceSqr(existingTarget, centre) > 12.0D * 12.0D
                || existingTarget.y > centre.y + 6.0D
                || escapedFlightBand) {
            chooseSkyTarget(cat, now, centre, escapedFlightBand);
        }
        data.putBoolean(SKY_ACTIVE_TAG, true);
        cat.setNoGravity(true);
        cat.fallDistance = 0.0F;
        cat.getNavigation().stop();

        Vec3 target = skyTarget(data);
        Vec3 delta = target.subtract(cat.position());
        Vec3 desired = delta.lengthSqr() < 0.05D
                ? Vec3.ZERO : delta.normalize().scale(0.24D);
        Vec3 movement = cat.getDeltaMovement().scale(0.78D).add(desired.scale(0.22D));
        if (cat.getY() > centre.y + 6.0D) {
            movement = new Vec3(movement.x, Math.min(-0.08D, movement.y), movement.z);
        }
        movement = new Vec3(Mth.clamp(movement.x, -0.28D, 0.28D),
                Mth.clamp(movement.y, -0.20D, 0.20D),
                Mth.clamp(movement.z, -0.28D, 0.28D));
        cat.setDeltaMovement(movement);
        face(cat, cat.position().add(movement));
        return true;
    }

    private static void chooseSkyTarget(Cat cat, long now, Vec3 centre,
                                        boolean returning) {
        double angle = cat.getRandom().nextDouble() * Math.PI * 2.0D;
        double radius = returning
                ? cat.getRandom().nextDouble() * 2.5D
                : 3.0D + cat.getRandom().nextDouble() * 5.0D;
        double y = Mth.clamp(centre.y + 1.5D + cat.getRandom().nextDouble() * 3.5D,
                cat.level().getMinBuildHeight() + 2.0D,
                cat.level().getMaxBuildHeight() - 3.0D);
        CompoundTag data = cat.getPersistentData();
        data.putDouble(SKY_TARGET_X_TAG, centre.x + Math.cos(angle) * radius);
        data.putDouble(SKY_TARGET_Y_TAG, y);
        data.putDouble(SKY_TARGET_Z_TAG, centre.z + Math.sin(angle) * radius);
        data.putLong(SKY_RETARGET_AT_TAG, now + 60L + cat.getRandom().nextInt(61));
    }

    private static Vec3 skyCentre(Cat cat, CompoundTag data) {
        LivingEntity owner = cat.getOwner();
        if (owner != null && owner.isAlive() && owner.level() == cat.level()) {
            return owner.position();
        }
        if (!data.contains(SKY_ANCHOR_X_TAG, Tag.TAG_DOUBLE)
                || !data.contains(SKY_ANCHOR_Y_TAG, Tag.TAG_DOUBLE)
                || !data.contains(SKY_ANCHOR_Z_TAG, Tag.TAG_DOUBLE)) {
            setSkyAnchor(data, cat.position());
        }
        return new Vec3(data.getDouble(SKY_ANCHOR_X_TAG),
                data.getDouble(SKY_ANCHOR_Y_TAG),
                data.getDouble(SKY_ANCHOR_Z_TAG));
    }

    private static void setSkyAnchor(CompoundTag data, Vec3 position) {
        data.putDouble(SKY_ANCHOR_X_TAG, position.x);
        data.putDouble(SKY_ANCHOR_Y_TAG, position.y);
        data.putDouble(SKY_ANCHOR_Z_TAG, position.z);
    }

    private static double horizontalDistanceSqr(Vec3 first, Vec3 second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return x * x + z * z;
    }

    private static void clearSkyState(Cat cat) {
        CompoundTag data = cat.getPersistentData();
        data.remove(SKY_ACTIVE_TAG);
        data.remove(SKY_TARGET_X_TAG);
        data.remove(SKY_TARGET_Y_TAG);
        data.remove(SKY_TARGET_Z_TAG);
        data.remove(SKY_RETARGET_AT_TAG);
        data.remove(SKY_ANCHOR_X_TAG);
        data.remove(SKY_ANCHOR_Y_TAG);
        data.remove(SKY_ANCHOR_Z_TAG);
        cat.setNoGravity(false);
    }

    private static Vec3 skyTarget(CompoundTag data) {
        return new Vec3(data.getDouble(SKY_TARGET_X_TAG),
                data.getDouble(SKY_TARGET_Y_TAG), data.getDouble(SKY_TARGET_Z_TAG));
    }

    /** Event-driven: nearby carriers rush the living target a player just attacked. */
    public static void notifyPlayerAttack(Player player, LivingEntity target) {
        if (player.level().isClientSide || target == null || !target.isAlive()) return;
        long until = player.level().getGameTime() + 60L;
        for (Cat cat : player.level().getEntitiesOfClass(Cat.class,
                player.getBoundingBox().inflate(ACTIVE_SEARCH_RANGE),
                cat -> cat.isAlive() && !CatPoseData.isPancake(cat)
                        && !CatLogisticsBehavior.isActive(cat)
                        && CatTraitData.ensure(cat).has(CatTrait.AUTO_ATTACH))) {
            cat.getPersistentData().putUUID(ATTACH_TARGET_TAG, target.getUUID());
            cat.getPersistentData().remove(LEGACY_ATTACH_PLAYER_TAG);
            cat.getPersistentData().putLong(ATTACH_UNTIL_TAG, until);
        }
    }

    private static boolean tickAutoAttach(Cat cat, CatTraitProfile traits) {
        CompoundTag data = cat.getPersistentData();
        if (!traits.has(CatTrait.AUTO_ATTACH) || !data.hasUUID(ATTACH_TARGET_TAG)
                || data.getLong(ATTACH_UNTIL_TAG) <= cat.level().getGameTime()
                || !(cat.level() instanceof ServerLevel level)) {
            data.remove(ATTACH_TARGET_TAG);
            data.remove(LEGACY_ATTACH_PLAYER_TAG);
            data.remove(ATTACH_UNTIL_TAG);
            return false;
        }
        Entity entity = level.getEntity(data.getUUID(ATTACH_TARGET_TAG));
        if (!(entity instanceof LivingEntity target) || !target.isAlive()
                || cat.distanceToSqr(target) > 32.0D * 32.0D) {
            data.remove(ATTACH_TARGET_TAG);
            data.remove(ATTACH_UNTIL_TAG);
            return false;
        }
        Vec3 delta = target.position().subtract(cat.position());
        if (delta.horizontalDistanceSqr() <= 1.2D) {
            cat.getNavigation().stop();
            cat.setDeltaMovement(0.0D, cat.getDeltaMovement().y, 0.0D);
        } else {
            Vec3 pull = new Vec3(delta.x, 0.0D, delta.z).normalize().scale(0.48D);
            cat.getNavigation().stop();
            cat.setDeltaMovement(pull.x, cat.getDeltaMovement().y, pull.z);
        }
        face(cat, target.position());
        return true;
    }

    private static boolean tickMinecartChaser(Cat cat, CatTraitProfile traits) {
        CompoundTag data = cat.getPersistentData();
        if (!traits.has(CatTrait.LOW_LEVEL_CODE)
                || !(cat.level() instanceof ServerLevel level)) {
            data.remove(MINECART_TARGET_TAG);
            return false;
        }
        AbstractMinecart minecart = data.hasUUID(MINECART_TARGET_TAG)
                ? level.getEntity(data.getUUID(MINECART_TARGET_TAG)) instanceof AbstractMinecart cart
                ? cart : null : null;
        if (!isMovingMinecart(cat, minecart) || staggered(cat, 10)) {
            minecart = level.getEntitiesOfClass(AbstractMinecart.class,
                            cat.getBoundingBox().inflate(10.0D),
                            cart -> isMovingMinecart(cat, cart))
                    .stream().min(Comparator.comparingDouble(cat::distanceToSqr)).orElse(null);
            if (minecart == null) {
                data.remove(MINECART_TARGET_TAG);
                return false;
            }
            data.putUUID(MINECART_TARGET_TAG, minecart.getUUID());
        }
        if (cat.getBoundingBox().inflate(0.12D).intersects(minecart.getBoundingBox())) {
            CatPancakeBehavior.flatten(cat);
            return true;
        }
        cat.setOrderedToSit(false);
        cat.getNavigation().moveTo(minecart, 1.75D);
        face(cat, minecart.position());
        return true;
    }

    private static boolean isMovingMinecart(Cat cat, AbstractMinecart cart) {
        return cart != null && cart.isAlive() && cat.distanceToSqr(cart) <= 100.0D
                && cart.getDeltaMovement().horizontalDistanceSqr() > 0.0016D;
    }

    private static boolean tickBeltSeeker(Cat cat, CatTraitProfile traits) {
        CompoundTag data = cat.getPersistentData();
        if (!traits.has(CatTrait.SHEDDING)) {
            data.remove(BELT_TARGET_TAG);
            return false;
        }
        if (isOnRunningBelt(cat)) return true;
        BlockPos target = data.contains(BELT_TARGET_TAG, Tag.TAG_LONG)
                ? BlockPos.of(data.getLong(BELT_TARGET_TAG)) : null;
        if (!isRunningBelt(cat.level(), target) || staggered(cat, 40)) {
            target = findNearestBlock(cat, 7, 3,
                    pos -> isRunningBelt(cat.level(), pos));
            if (target == null) {
                data.remove(BELT_TARGET_TAG);
                return false;
            }
            data.putLong(BELT_TARGET_TAG, target.asLong());
        }
        cat.setOrderedToSit(false);
        cat.getNavigation().moveTo(target.getX() + 0.5D,
                target.getY() + 0.8D, target.getZ() + 0.5D, 1.25D);
        return true;
    }

    private static boolean isOnRunningBelt(Cat cat) {
        return isRunningBelt(cat.level(), cat.getOnPos())
                || isRunningBelt(cat.level(), cat.blockPosition().below())
                || isRunningBelt(cat.level(), cat.blockPosition());
    }

    private static boolean isRunningBelt(Level level, BlockPos pos) {
        return pos != null && level.isLoaded(pos)
                && level.getBlockState(pos).getBlock() instanceof BeltBlock
                && level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic
                && kinetic.getSpeed() != 0.0F;
    }

    private static boolean tickBootThief(Cat cat, CatTraitProfile traits) {
        CompoundTag data = cat.getPersistentData();
        if (!traits.has(CatTrait.TRIPOD_CAT)) {
            data.remove(CHEST_TARGET_TAG);
            return false;
        }
        BlockPos target = data.contains(CHEST_TARGET_TAG, Tag.TAG_LONG)
                ? BlockPos.of(data.getLong(CHEST_TARGET_TAG)) : null;
        if (!containerHasBoot(cat.level(), target) || staggered(cat, 80)) {
            target = findNearestBlock(cat, 4, 3,
                    pos -> containerHasBoot(cat.level(), pos));
            if (target == null) {
                data.remove(CHEST_TARGET_TAG);
                return false;
            }
            data.putLong(CHEST_TARGET_TAG, target.asLong());
        }
        if (cat.distanceToSqr(Vec3.atCenterOf(target)) > 4.0D) {
            cat.getNavigation().moveTo(target.getX() + 0.5D,
                    target.getY(), target.getZ() + 0.5D, 1.15D);
            return true;
        }
        cat.getNavigation().stop();
        stealOneBoot(cat, target);
        data.remove(CHEST_TARGET_TAG);
        return true;
    }

    private static boolean containerHasBoot(Level level, BlockPos pos) {
        IItemHandler handler = itemHandler(level, pos);
        if (handler == null) return false;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (isBoot(handler.getStackInSlot(slot))) return true;
        }
        return false;
    }

    private static void stealOneBoot(Cat cat, BlockPos pos) {
        IItemHandler handler = itemHandler(cat.level(), pos);
        if (handler == null) return;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (!isBoot(handler.getStackInSlot(slot))) continue;
            ItemStack simulated = handler.extractItem(slot, 1, true);
            if (simulated.isEmpty() || !canStore(cat, simulated)) return;
            ItemStack extracted = handler.extractItem(slot, 1, false);
            if (extracted.isEmpty()) return;
            ItemStack remainder = storeInCatInventory(cat, extracted);
            if (!remainder.isEmpty()) cat.spawnAtLocation(remainder);
            cat.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 0.8F, 1.3F);
            return;
        }
    }

    private static IItemHandler itemHandler(Level level, BlockPos pos) {
        if (pos == null || !level.isLoaded(pos)) return null;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return null;
        return Capabilities.ItemHandler.BLOCK.getCapability(
                level, pos, level.getBlockState(pos), blockEntity, null);
    }

    private static boolean isBoot(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem().getEquipmentSlot(stack) == EquipmentSlot.FEET;
    }

    private static boolean tickCombatBehaviour(Cat cat, CatTraitProfile traits) {
        CatTrait kind = combatTrait(traits);
        if (kind == null) {
            clearBehaviourTarget(cat);
            return false;
        }
        CompoundTag data = cat.getPersistentData();
        LivingEntity target = resolveTarget(cat, data);
        if (target == null || !validCombatTarget(cat, kind, target)
                || staggered(cat, 10)) {
            target = findCombatTarget(cat, kind);
            if (target == null) {
                clearBehaviourTarget(cat);
                return false;
            }
            data.putUUID(TARGET_TAG, target.getUUID());
            data.putString(TARGET_KIND_TAG, kind.serializedName());
        }

        cat.setTarget(target);
        face(cat, target.position());
        double reach = Math.max(1.2D,
                cat.getBbWidth() + target.getBbWidth() * 0.55D);
        if (cat.distanceToSqr(target) > reach * reach) {
            cat.getNavigation().moveTo(target, kind == CatTrait.HUNTER_KIMI ? 1.35D : 1.25D);
            return true;
        }

        cat.getNavigation().stop();
        int cooldown = data.getInt(ATTACK_COOLDOWN_TAG);
        if (cooldown > 0) {
            data.putInt(ATTACK_COOLDOWN_TAG, cooldown - 1);
            return true;
        }
        if (kind == CatTrait.MISCHIEVOUS) {
            target.hurt(cat.damageSources().mobAttack(cat), 1.0F);
            pushTowardLedge(cat, target);
            data.putInt(ATTACK_COOLDOWN_TAG, 40);
        } else {
            cat.doHurtTarget(target);
            data.putInt(ATTACK_COOLDOWN_TAG,
                    CatAttributeEffects.attackIntervalTicks(cat));
        }
        return true;
    }

    private static CatTrait combatTrait(CatTraitProfile traits) {
        if (traits.has(CatTrait.FILICIDE)) return CatTrait.FILICIDE;
        if (traits.has(CatTrait.FOOD_GUARD)) return CatTrait.FOOD_GUARD;
        if (traits.has(CatTrait.EDWARD)) return CatTrait.EDWARD;
        if (traits.has(CatTrait.HUNTER_KIMI)) return CatTrait.HUNTER_KIMI;
        if (traits.has(CatTrait.MISCHIEVOUS)) return CatTrait.MISCHIEVOUS;
        if (traits.has(CatTrait.STITCH)) return CatTrait.STITCH;
        return null;
    }

    private static LivingEntity findCombatTarget(Cat cat, CatTrait kind) {
        return switch (kind) {
            case FILICIDE -> nearestLiving(cat, ACTIVE_SEARCH_RANGE,
                    living -> living instanceof Cat kitten && kitten != cat
                            && kitten.isBaby() && kitten.isAlive());
            case EDWARD -> nearestLiving(cat, 16.0D,
                    living -> living instanceof Player player
                            && player.isSleeping() && player.isAlive()
                            && !player.isSpectator());
            case FOOD_GUARD -> findFoodGuardTarget(cat);
            case HUNTER_KIMI -> nearestLiving(cat, ACTIVE_SEARCH_RANGE,
                    living -> isHuntable(living) && living.isAlive());
            case MISCHIEVOUS -> nearestLiving(cat, 3.0D,
                    living -> living != cat && living.isAlive() && isNearLedge(living));
            case STITCH -> nearestLiving(cat, ACTIVE_SEARCH_RANGE,
                    living -> living != cat && living.isAlive());
            default -> null;
        };
    }

    private static boolean validCombatTarget(Cat cat, CatTrait kind, LivingEntity target) {
        if (!target.isAlive() || target == cat) return false;
        double range = kind == CatTrait.EDWARD ? 16.0D
                : kind == CatTrait.MISCHIEVOUS ? 3.0D : ACTIVE_SEARCH_RANGE;
        if (cat.distanceToSqr(target) > range * range) return false;
        return switch (kind) {
            case FILICIDE -> target instanceof Cat kitten && kitten.isBaby();
            case EDWARD -> target instanceof Player player && player.isSleeping();
            case FOOD_GUARD -> isNearGuardedFood(cat, target);
            case HUNTER_KIMI -> isHuntable(target);
            case MISCHIEVOUS -> isNearLedge(target);
            default -> true;
        };
    }

    private static LivingEntity findFoodGuardTarget(Cat cat) {
        List<ItemEntity> food = cat.level().getEntitiesOfClass(ItemEntity.class,
                cat.getBoundingBox().inflate(4.0D),
                item -> item.isAlive() && isFood(cat, item.getItem())
                        && isInFront(cat, item.position()));
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ItemEntity item : food) {
            for (LivingEntity candidate : cat.level().getEntitiesOfClass(LivingEntity.class,
                    item.getBoundingBox().inflate(2.25D),
                    candidate -> candidate != cat && candidate.isAlive())) {
                double distance = cat.distanceToSqr(candidate);
                if (distance < bestDistance) {
                    best = candidate;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    private static boolean isNearGuardedFood(Cat cat, LivingEntity target) {
        return cat.level().getEntitiesOfClass(ItemEntity.class,
                target.getBoundingBox().inflate(2.25D),
                item -> item.isAlive() && isFood(cat, item.getItem())
                        && isInFront(cat, item.position())).size() > 0;
    }

    private static boolean isInFront(Cat cat, Vec3 position) {
        Vec3 toItem = position.subtract(cat.position());
        if (toItem.horizontalDistanceSqr() < 0.01D) return true;
        return cat.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize()
                .dot(toItem.multiply(1.0D, 0.0D, 1.0D).normalize()) > 0.15D;
    }

    private static boolean isHuntable(LivingEntity living) {
        return living instanceof Chicken || living instanceof Rabbit
                || living instanceof Cod || living instanceof Salmon
                || living instanceof TropicalFish;
    }

    private static void pushTowardLedge(Cat cat, LivingEntity target) {
        Vec3 away = target.position().subtract(cat.position());
        if (away.horizontalDistanceSqr() < 1.0E-4D) return;
        Vec3 push = new Vec3(away.x, 0.0D, away.z).normalize();
        BlockPos landing = BlockPos.containing(target.position()
                .add(push.scale(0.9D))).below();
        double strength = target.level().getBlockState(landing).isAir() ? 0.7D : 0.28D;
        target.push(push.x * strength, 0.12D, push.z * strength);
    }

    private static boolean isNearLedge(LivingEntity target) {
        if (!target.onGround()) return false;
        double probeDistance = target.getBbWidth() * 0.5D + 0.35D;
        double supportY = target.getBoundingBox().minY - 0.05D;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos support = BlockPos.containing(
                    target.getX() + direction.getStepX() * probeDistance,
                    supportY,
                    target.getZ() + direction.getStepZ() * probeDistance);
            if (!target.level().getBlockState(support)
                    .isFaceSturdy(target.level(), support, Direction.UP)) return true;
        }
        return false;
    }

    private static LivingEntity resolveTarget(Cat cat, CompoundTag data) {
        if (!data.hasUUID(TARGET_TAG) || !(cat.level() instanceof ServerLevel level)) return null;
        Entity entity = level.getEntity(data.getUUID(TARGET_TAG));
        return entity instanceof LivingEntity living ? living : null;
    }

    private static LivingEntity resolveEntity(Cat cat, UUID uuid) {
        if (!(cat.level() instanceof ServerLevel level)) return null;
        Entity entity = level.getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void clearBehaviourTarget(Cat cat) {
        CompoundTag data = cat.getPersistentData();
        if (data.hasUUID(TARGET_TAG)) cat.setTarget(null);
        data.remove(TARGET_TAG);
        data.remove(TARGET_KIND_TAG);
        data.remove(ATTACK_COOLDOWN_TAG);
    }

    private static LivingEntity nearestLiving(Cat cat, double range,
                                               Predicate<LivingEntity> predicate) {
        return cat.level().getEntitiesOfClass(LivingEntity.class,
                        cat.getBoundingBox().inflate(range), predicate)
                .stream().min(Comparator.comparingDouble(cat::distanceToSqr)).orElse(null);
    }

    private static BlockPos findNearestBlock(Cat cat, int horizontal, int vertical,
                                             Predicate<BlockPos> predicate) {
        BlockPos origin = cat.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(
                origin.offset(-horizontal, -vertical, -horizontal),
                origin.offset(horizontal, vertical, horizontal))) {
            if (!predicate.test(candidate)) continue;
            double distance = candidate.distSqr(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate.immutable();
            }
        }
        return best;
    }

    public static boolean refusesFood(Cat cat, ItemStack stack) {
        return CatTraitData.ensure(cat).has(CatTrait.ANOREXIA) && isFood(cat, stack);
    }

    public static boolean cannotBreed(Cat cat) {
        return CatTraitData.ensure(cat).has(CatTrait.CUDDLE_ONLY);
    }

    private static boolean isFood(Cat cat, ItemStack stack) {
        return !stack.isEmpty() && (cat.isFood(stack)
                || stack.getItem().getFoodProperties(stack, null) != null
                || stack.is(LaoWuMod.CAT_FOOD.get())
                || stack.is(LaoWuMod.PHEROMONE_CAT_FOOD.get())
                || stack.is(LaoWuMod.CAT_STRIP.get())
                || stack.getItem() instanceof CatAttributeCanItem
                || stack.getItem() instanceof BreedingOnlyCatCanItem
                || stack.getItem() instanceof CatTraitFishItem);
    }

    public static float childAttackMultiplier(Cat attacker, LivingEntity target) {
        return target instanceof Cat kitten && kitten.isBaby()
                && CatTraitData.ensure(attacker).has(CatTrait.FILICIDE) ? 2.0F : 1.0F;
    }

    public static void applyMinorIllnessOnDeath(Cat cat) {
        if (!CatTraitData.ensure(cat).has(CatTrait.MINOR_ILLNESS)) return;
        CatAttributeProfile profile = CatAttributeData.ensure(cat);
        for (CatStat stat : CatStat.values()) profile = profile.withValues(stat, 0, 0);
        CatAttributeData.set(cat, profile);
    }

    public static void collectHuntedDrops(Cat hunter,
                                          java.util.Collection<ItemEntity> drops) {
        if (!CatTraitData.ensure(hunter).has(CatTrait.HUNTER_KIMI)) return;
        for (ItemEntity drop : drops) {
            ItemStack stack = drop.getItem();
            if (stack.getItem().getFoodProperties(stack, null) == null) continue;
            ItemStack remainder = storeInCatInventory(hunter, stack.copy());
            if (remainder.isEmpty()) drop.discard();
            else drop.setItem(remainder);
        }
    }

    private static boolean canStore(Cat cat, ItemStack stack) {
        ItemStack copy = stack.copy();
        return storeInCatInventory(cat, copy, true).isEmpty();
    }

    private static ItemStack storeInCatInventory(Cat cat, ItemStack stack) {
        return storeInCatInventory(cat, stack, false);
    }

    private static ItemStack storeInCatInventory(Cat cat, ItemStack input, boolean simulate) {
        Container inventory = CatProfileData.openContainer(cat);
        ItemStack remaining = input.copy();
        for (int slot = CatProfileData.ACCESSORY_SLOTS;
             slot < CatProfileData.SLOT_COUNT && !remaining.isEmpty(); slot++) {
            ItemStack stored = inventory.getItem(slot);
            if (!stored.isEmpty() && ItemStack.isSameItemSameComponents(stored, remaining)) {
                int moved = Math.min(remaining.getCount(),
                        Math.min(stored.getMaxStackSize(), inventory.getMaxStackSize())
                                - stored.getCount());
                if (moved > 0) {
                    if (!simulate) stored.grow(moved);
                    remaining.shrink(moved);
                }
            } else if (stored.isEmpty()) {
                int moved = Math.min(remaining.getCount(),
                        Math.min(remaining.getMaxStackSize(), inventory.getMaxStackSize()));
                if (!simulate) inventory.setItem(slot, remaining.copyWithCount(moved));
                remaining.shrink(moved);
            }
        }
        if (!simulate && remaining.getCount() != input.getCount()) inventory.setChanged();
        return remaining;
    }

    private static void clearHissing(Cat cat) {
        HissingCatBehavior.pauseForLogistics(cat);
        if (CatPoseData.isHissing(cat)) {
            CatPoseData.setPose(cat, CatPoseData.NORMAL);
            ModNetwork.syncToTracking(cat, CatPoseData.NORMAL);
        }
        ModNetwork.setAudioSession(cat, false);
    }

    private static void face(Cat cat, Vec3 target) {
        double dx = target.x - cat.getX();
        double dz = target.z - cat.getZ();
        if (dx * dx + dz * dz < 1.0E-5D) return;
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        cat.setYRot(yaw);
        cat.yBodyRot = yaw;
        cat.setYHeadRot(yaw);
    }

    private static boolean staggered(Cat cat, int interval) {
        return Math.floorMod(cat.tickCount + cat.getId(), interval) == 0;
    }

    private static boolean mobGriefing(Cat cat) {
        return cat.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    private CatBehaviorTraitEffects() {}
}
