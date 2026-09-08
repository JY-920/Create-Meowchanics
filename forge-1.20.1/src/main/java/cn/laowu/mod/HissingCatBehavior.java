package cn.laowu.mod;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.phys.AABB;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraftforge.registries.ForgeRegistries;
import cn.laowu.mod.genetics.CatAttributeEffects;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitData;

import java.util.Comparator;

/** Temporary debug behaviour used while tuning the hissing pose. */
public final class HissingCatBehavior {
    private static final double SEARCH_DISTANCE = 5.0D;
    private static final double STOP_DISTANCE = 1.5D;
    /** Each cat can be pulled this many blocks per tick (about 14 blocks/second). */
    private static final double MAX_PULL_SPEED = 0.70D;
    private static final String ACTIVE_TAG = "LaoWuHissingAttractionActive";
    private static final String LOCKED_TAG = "LaoWuHissingPositionLocked";
    private static final String LOCK_X_TAG = "LaoWuHissingLockX";
    private static final String LOCK_Z_TAG = "LaoWuHissingLockZ";
    public static final String FIGHT_TARGET_TAG = "LaoWuHissingFightTarget";
    private static final String ATTACK_COOLDOWN_TAG = "LaoWuHissingAttackCooldown";
    private static final String PAIR_INTERRUPTED_UNTIL_TAG =
            "LaoWuHissingPairInterruptedUntil";

    public static void tick(Cat cat) {
        if (isPairInterrupted(cat)) {
            stopInterruptedHissing(cat);
            return;
        }
        cat.getPersistentData().remove(PAIR_INTERRUPTED_UNTIL_TAG);
        if (isHissingForbidden(cat)) {
            if (isFighting(cat)) endFight(cat);
            stopPreviousAttraction(cat);
            clearPose(cat);
            ModNetwork.setAudioSession(cat, false);
            return;
        }
        // Pet cats never participate in the ambient attraction/hissing system.
        // Logistics hissing is handled earlier by CatLogisticsBehavior and is
        // deliberately kept separate from this automatic encounter behavior.
        if (cat.isTame()) {
            if (isFighting(cat)) endFight(cat);
            stopPreviousAttraction(cat);
            clearPose(cat);
            ModNetwork.setAudioSession(cat, false);
            return;
        }

        if (cat.getPersistentData().hasUUID(FIGHT_TARGET_TAG)) {
            tickFight(cat);
            return;
        }

        Cat partner = findNearestPartner(cat);
        if (partner == null) {
            stopPreviousAttraction(cat);
            clearPose(cat);
            ModNetwork.setAudioSession(cat, false);
            return;
        }

        if (!CatPoseData.isHissing(cat)) {
            CatPoseData.setPose(cat, 1);
            ModNetwork.syncToTracking(cat, 1);
        }

        cat.getPersistentData().putBoolean(ACTIVE_TAG, true);
        updateHissingAudio(cat);
        facePartner(cat, partner);

        // Cats powering a Create generator must remain on their two seat blocks.
        if (isOnRedSeat(cat) && isOnRedSeat(partner)) {
            lockBodyInPlace(cat);
            return;
        }

        double dx = partner.getX() - cat.getX();
        double dz = partner.getZ() - cat.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance > STOP_DISTANCE) {
            unlockPosition(cat);
            cat.getNavigation().stop();

            // Fast direct pull is intentional: this mod's behaviour is meant to look absurd.
            double remainingGap = horizontalDistance - STOP_DISTANCE;
            double speed = Math.min(MAX_PULL_SPEED, Math.max(0.12D, remainingGap * 0.55D));
            double verticalSpeed = cat.getDeltaMovement().y;
            cat.setDeltaMovement(dx / horizontalDistance * speed, verticalSpeed, dz / horizontalDistance * speed);
        } else {
            lockBodyInPlace(cat);
        }
    }

    private static Cat findNearestPartner(Cat cat) {
        AABB area = cat.getBoundingBox().inflate(SEARCH_DISTANCE);
        return cat.level().getEntitiesOfClass(Cat.class, area, other ->
                        other != cat
                                && other.isAlive()
                                && !other.isTame()
                                && !isPairInterrupted(other)
                                && !isHissingForbidden(other)
                                && !CatPoseData.isPancake(other)
                                && cat.distanceToSqr(other) <= SEARCH_DISTANCE * SEARCH_DISTANCE)
                .stream()
                .min(Comparator.comparingDouble(cat::distanceToSqr))
                .orElse(null);
    }

    private static void facePartner(Cat cat, Cat partner) {
        LookControl look = cat.getLookControl();
        look.setLookAt(partner, 90.0F, 90.0F);

        double dx = partner.getX() - cat.getX();
        double dz = partner.getZ() - cat.getZ();
        float targetYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float yaw = Mth.rotLerp(0.35F, cat.getYRot(), targetYaw);
        cat.setYRot(yaw);
        cat.yBodyRot = yaw;
        cat.setYHeadRot(yaw);
    }

    private static void stopPreviousAttraction(Cat cat) {
        if (cat.getPersistentData().getBoolean(ACTIVE_TAG)) {
            cat.getNavigation().stop();
            cat.getPersistentData().remove(ACTIVE_TAG);
            unlockPosition(cat);
        }
    }

    public static Cat getCurrentPartner(Cat cat) {
        return findNearestPartner(cat);
    }

    public static void startFight(Cat first, Cat second) {
        CatPoseData.setPose(first, 0);
        CatPoseData.setPose(second, 0);
        ModNetwork.syncToTracking(first, 0);
        ModNetwork.syncToTracking(second, 0);
        unlockPosition(first);
        unlockPosition(second);
        first.getNavigation().stop();
        second.getNavigation().stop();
        first.getPersistentData().putUUID(FIGHT_TARGET_TAG, second.getUUID());
        second.getPersistentData().putUUID(FIGHT_TARGET_TAG, first.getUUID());
        first.setTarget(second);
        second.setTarget(first);
    }

    public static boolean isFighting(Cat cat) {
        return cat.getPersistentData().hasUUID(FIGHT_TARGET_TAG);
    }

    public static void pauseForLogistics(Cat cat) {
        if (isFighting(cat)) endFight(cat);
        stopPreviousAttraction(cat);
        unlockPosition(cat);
        ModNetwork.setAudioSession(cat, false);
    }

    /** Stops both participants when fluid interrupts one side of a hissing pair. */
    public static void interruptPair(Cat cat, int pauseTicks) {
        if (cat.level().isClientSide) return;
        Cat partner = findNearestPartner(cat);
        long until = cat.level().getGameTime() + Math.max(1, pauseTicks);
        cat.getPersistentData().putLong(PAIR_INTERRUPTED_UNTIL_TAG, until);
        if (partner != null && CatPoseData.isHissing(partner)) {
            partner.getPersistentData().putLong(PAIR_INTERRUPTED_UNTIL_TAG, until);
            stopInterruptedHissing(partner);
        }
        stopInterruptedHissing(cat);
    }

    public static boolean isPairInterrupted(Cat cat) {
        return cat.getPersistentData().getLong(PAIR_INTERRUPTED_UNTIL_TAG)
                > cat.level().getGameTime();
    }

    private static void stopInterruptedHissing(Cat cat) {
        if (isFighting(cat)) endFight(cat);
        stopPreviousAttraction(cat);
        unlockPosition(cat);
        clearPose(cat);
        ModNetwork.setAudioSession(cat, false);
    }

    private static void tickFight(Cat cat) {
        if (!(cat.level() instanceof ServerLevel level)) return;
        Entity entity = level.getEntity(cat.getPersistentData().getUUID(FIGHT_TARGET_TAG));
        if (!(entity instanceof Cat opponent) || !opponent.isAlive()
                || cat.distanceToSqr(opponent) > SEARCH_DISTANCE * SEARCH_DISTANCE) {
            endFight(cat);
            return;
        }

        ModNetwork.setAudioSession(cat, isAudioHost(cat, opponent));

        facePartner(cat, opponent);
        double dx = opponent.getX() - cat.getX();
        double dz = opponent.getZ() - cat.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance > 1.15D) {
            cat.getNavigation().moveTo(opponent, 1.25D);
        } else {
            cat.getNavigation().stop();
            cat.setDeltaMovement(0.0D, cat.getDeltaMovement().y, 0.0D);
            int cooldown = cat.getPersistentData().getInt(ATTACK_COOLDOWN_TAG);
            if (cooldown <= 0) {
                cat.doHurtTarget(opponent);
                cat.getPersistentData().putInt(ATTACK_COOLDOWN_TAG,
                        CatAttributeEffects.attackIntervalTicks(cat));
            } else {
                cat.getPersistentData().putInt(ATTACK_COOLDOWN_TAG, cooldown - 1);
            }
        }
    }

    public static void endFight(Cat cat) {
        ModNetwork.setAudioSession(cat, false);
        cat.getPersistentData().remove(FIGHT_TARGET_TAG);
        cat.getPersistentData().remove(ATTACK_COOLDOWN_TAG);
        cat.setTarget(null);
        cat.getNavigation().stop();
    }

    private static void updateHissingAudio(Cat cat) {
        AABB area = cat.getBoundingBox().inflate(SEARCH_DISTANCE);
        var group = cat.level().getEntitiesOfClass(Cat.class, area, other ->
                other.isAlive() && !other.isTame() && CatPoseData.isHissing(other)
                        && !isHissingForbidden(other)
                        && cat.distanceToSqr(other) <= SEARCH_DISTANCE * SEARCH_DISTANCE);
        if (!group.contains(cat)) group.add(cat);
        Cat host = group.stream()
                .filter(HissingCatBehavior::hasAirRaidSiren)
                .min(Comparator.comparingInt(Cat::getId))
                .orElseGet(() -> group.stream()
                        .max(Comparator.comparingInt(Cat::getId)).orElse(cat));
        ModNetwork.setAudioSession(cat, group.size() >= 2 && cat == host);
    }

    private static boolean isAudioHost(Cat cat, Cat opponent) {
        boolean catSiren = hasAirRaidSiren(cat);
        boolean opponentSiren = hasAirRaidSiren(opponent);
        return catSiren != opponentSiren ? catSiren : cat.getId() > opponent.getId();
    }

    private static boolean hasAirRaidSiren(Cat cat) {
        return CatTraitData.ensure(cat).has(CatTrait.AIR_RAID_SIREN);
    }

    private static void clearPose(Cat cat) {
        if (CatPoseData.isHissing(cat)) {
            CatPoseData.setPose(cat, 0);
            ModNetwork.syncToTracking(cat, 0);
        }
    }

    public static boolean isHissingForbidden(Cat cat) {
        return CatTraitData.ensure(cat).has(CatTrait.GOOD_CAT);
    }

    private static void lockBodyInPlace(Cat cat) {
        cat.getNavigation().stop();
        if (!cat.getPersistentData().getBoolean(LOCKED_TAG)) {
            cat.getPersistentData().putBoolean(LOCKED_TAG, true);
            cat.getPersistentData().putDouble(LOCK_X_TAG, cat.getX());
            cat.getPersistentData().putDouble(LOCK_Z_TAG, cat.getZ());
        }

        double lockX = cat.getPersistentData().getDouble(LOCK_X_TAG);
        double lockZ = cat.getPersistentData().getDouble(LOCK_Z_TAG);
        cat.setPos(lockX, cat.getY(), lockZ);
        cat.setDeltaMovement(0.0D, cat.getDeltaMovement().y, 0.0D);
    }

    private static void unlockPosition(Cat cat) {
        cat.getPersistentData().remove(LOCKED_TAG);
        cat.getPersistentData().remove(LOCK_X_TAG);
        cat.getPersistentData().remove(LOCK_Z_TAG);
    }

    private static boolean isOnRedSeat(Cat cat) {
        BlockPos[] candidates = {cat.getOnPos(), cat.blockPosition(), cat.blockPosition().below()};
        for (BlockPos pos : candidates) {
            var key = ForgeRegistries.BLOCKS.getKey(cat.level().getBlockState(pos).getBlock());
            if (key != null && key.getNamespace().equals("create") && key.getPath().equals("red_seat")) return true;
        }
        return false;
    }

    private HissingCatBehavior() {}
}
