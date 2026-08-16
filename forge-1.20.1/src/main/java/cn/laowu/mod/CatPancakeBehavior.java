package cn.laowu.mod;

import cn.laowu.mod.network.ModNetwork;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cat;

import java.util.UUID;

public final class CatPancakeBehavior {
    private static final String PREVIOUS_NO_AI_TAG = "LaoWuPancakePreviousNoAI";
    private static final String ROLLER_CONTRAPTION_TAG = "LaoWuPancakeRollerContraption";
    private static final String ANCHOR_X_TAG = "LaoWuPancakeAnchorX";
    private static final String ANCHOR_Z_TAG = "LaoWuPancakeAnchorZ";
    private static final String ANCHOR_Y_ROT_TAG = "LaoWuPancakeAnchorYRot";
    private static final String ANCHOR_X_ROT_TAG = "LaoWuPancakeAnchorXRot";

    /** Returns true while the cat pancake state owns the cat's tick. */
    public static boolean tickPancake(Cat cat) {
        if (!CatPoseData.isPancake(cat)) return false;

        cat.getNavigation().stop();
        cat.setTarget(null);
        restorePhysicsAiState(cat);
        disableActiveControls(cat);
        lockHorizontalPosition(cat);
        cat.setSpeed(0.0F);
        cat.xxa = 0.0F;
        cat.yya = 0.0F;
        cat.zza = 0.0F;
        return true;
    }

    public static void flatten(Cat cat) {
        if (cat.level().isClientSide || CatPoseData.isPancake(cat)) return;

        CatLogisticsBehavior.cancelForPancake(cat);
        HissingCatBehavior.pauseForLogistics(cat);
        ModNetwork.setAudioSession(cat, false);
        cat.stopRiding();
        cat.getNavigation().stop();
        cat.setTarget(null);
        cat.setOrderedToSit(false);
        cat.setInSittingPose(false);

        CompoundTag data = cat.getPersistentData();
        data.putBoolean(PREVIOUS_NO_AI_TAG, cat.isNoAi());
        rememberHorizontalPosition(cat, data);
        disableActiveControls(cat);
        CatPoseData.setPose(cat, CatPoseData.PANCAKE);
        ModNetwork.syncToTracking(cat, CatPoseData.PANCAKE);

        if (cat.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.POOF, cat.getX(), cat.getY() + 0.3D, cat.getZ(),
                    14, 0.24D, 0.16D, 0.24D, 0.03D);
            level.playSound(null, cat.blockPosition(), SoundEvents.WOOL_PLACE,
                    SoundSource.NEUTRAL, 1.0F, 0.65F);
        }
    }

    /** Restores persistent fields that must not leak into a reconstituted cat. */
    public static void sanitizeCapturedData(CompoundTag entityData) {
        CompoundTag forgeData = entityData.getCompound("ForgeData");
        if (forgeData.contains(PREVIOUS_NO_AI_TAG)) {
            entityData.putBoolean("NoAI", forgeData.getBoolean(PREVIOUS_NO_AI_TAG));
        }
        forgeData.remove(PREVIOUS_NO_AI_TAG);
        forgeData.remove(ROLLER_CONTRAPTION_TAG);
        forgeData.remove(ANCHOR_X_TAG);
        forgeData.remove(ANCHOR_Z_TAG);
        forgeData.remove(ANCHOR_Y_ROT_TAG);
        forgeData.remove(ANCHOR_X_ROT_TAG);
    }

    public static void ignoreRollerContraption(Cat cat, UUID contraptionId) {
        cat.getPersistentData().putUUID(ROLLER_CONTRAPTION_TAG, contraptionId);
    }

    public static boolean ignoresContraption(Cat cat, UUID contraptionId) {
        CompoundTag data = cat.getPersistentData();
        return CatPoseData.isPancake(cat) && data.hasUUID(ROLLER_CONTRAPTION_TAG)
                && data.getUUID(ROLLER_CONTRAPTION_TAG).equals(contraptionId);
    }

    /** NoAI also prevents the vanilla living movement step that applies gravity. */
    private static void restorePhysicsAiState(Cat cat) {
        CompoundTag data = cat.getPersistentData();
        if (data.contains(PREVIOUS_NO_AI_TAG) && cat.isNoAi() != data.getBoolean(PREVIOUS_NO_AI_TAG)) {
            cat.setNoAi(data.getBoolean(PREVIOUS_NO_AI_TAG));
        }
    }

    private static void rememberHorizontalPosition(Cat cat, CompoundTag data) {
        data.putDouble(ANCHOR_X_TAG, cat.getX());
        data.putDouble(ANCHOR_Z_TAG, cat.getZ());
        data.putFloat(ANCHOR_Y_ROT_TAG, cat.getYRot());
        data.putFloat(ANCHOR_X_ROT_TAG, cat.getXRot());
    }

    /**
     * A living cat pancake may still fall or ride a vertically moving collision
     * surface, but entity collision and contraptions cannot displace it sideways.
     */
    private static void lockHorizontalPosition(Cat cat) {
        CompoundTag data = cat.getPersistentData();
        if (!data.contains(ANCHOR_X_TAG) || !data.contains(ANCHOR_Z_TAG)) {
            rememberHorizontalPosition(cat, data);
        }

        double y = cat.getY();
        double verticalVelocity = cat.getDeltaMovement().y;
        float yaw = data.getFloat(ANCHOR_Y_ROT_TAG);
        float pitch = data.getFloat(ANCHOR_X_ROT_TAG);

        cat.setPos(data.getDouble(ANCHOR_X_TAG), y, data.getDouble(ANCHOR_Z_TAG));
        cat.setDeltaMovement(0.0D, verticalVelocity, 0.0D);
        cat.setYRot(yaw);
        cat.yRotO = yaw;
        cat.setYHeadRot(yaw);
        cat.yHeadRotO = yaw;
        cat.yBodyRot = yaw;
        cat.yBodyRotO = yaw;
        cat.setXRot(pitch);
        cat.xRotO = pitch;
    }

    private static void disableActiveControls(Cat cat) {
        cat.goalSelector.disableControlFlag(Goal.Flag.MOVE);
        cat.goalSelector.disableControlFlag(Goal.Flag.LOOK);
        cat.goalSelector.disableControlFlag(Goal.Flag.JUMP);
        cat.goalSelector.disableControlFlag(Goal.Flag.TARGET);
        cat.targetSelector.disableControlFlag(Goal.Flag.TARGET);
    }

    private CatPancakeBehavior() {
    }
}
