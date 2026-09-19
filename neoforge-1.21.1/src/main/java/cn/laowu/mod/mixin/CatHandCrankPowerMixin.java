package cn.laowu.mod.mixin;

import cn.laowu.mod.*;
import cn.laowu.mod.genetics.CatAttributeEffects;
import cn.laowu.mod.genetics.CatStat;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.crank.HandCrankBlock;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.lang.ref.WeakReference;

/** Per-crank, transient contribution; players and valve handles keep their vanilla capacity. */
@Mixin(value = HandCrankBlockEntity.class, remap = false)
public abstract class CatHandCrankPowerMixin extends GeneratingKineticBlockEntity implements CatCrankPower {
    @Unique private WeakReference<Cat> laowu$driver = new WeakReference<>(null);
    @Unique private long laowu$lastDrive = Long.MIN_VALUE;
    @Unique private float laowu$capacityPerRpm = -1;

    protected CatHandCrankPowerMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override public void laowu$drive(Cat cat) {
        laowu$driver = new WeakReference<>(cat);
        laowu$lastDrive = level.getGameTime();
        double rpm = Math.abs(((HandCrankBlock) getBlockState().getBlock()).getRotationSpeed());
        float next = (float) (CatCrankPower.stressCapacity(
                CatAttributeEffects.effectiveValue(cat, CatStat.STAMINA)) / Math.max(1, rpm));
        if (laowu$capacityPerRpm != next) {
            laowu$capacityPerRpm = next;
            laowu$refreshCapacity();
        }
    }

    @Override public float calculateAddedStressCapacity() {
        if (laowu$capacityPerRpm < 0) return super.calculateAddedStressCapacity();
        return lastCapacityProvided = laowu$capacityPerRpm;
    }

    @Unique private void laowu$refreshCapacity() {
        float next = calculateAddedStressCapacity();
        if (hasNetwork()) getOrCreateNetwork().updateCapacityFor(this, next);
        sendData();
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void laowu$expireDriver(CallbackInfo ci) {
        if (level == null || level.isClientSide) return;
        Cat cat = laowu$driver.get();
        if (laowu$capacityPerRpm >= 0 && (cat == null || !cat.isAlive()
                || level.getGameTime() - laowu$lastDrive > 1
                || CatProfileData.isBeingViewed(cat)
                || CatEngineeringBehavior.findCrank(cat) != (Object) this)) {
            laowu$capacityPerRpm = -1;
            laowu$driver.clear();
            laowu$refreshCapacity();
        }
        // Correct a saved network's old capacity on its first tick after a reload.
        if (laowu$capacityPerRpm < 0) {
            float previous = lastCapacityProvided;
            if (previous != calculateAddedStressCapacity()) laowu$refreshCapacity();
        }
    }

    @Inject(method = "write", at = @At("TAIL"), remap = false)
    private void laowu$writePower(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        // Never persist a driver's bonus: an unloaded/unattended crank cannot retain it.
        if (clientPacket) tag.putFloat("LaoWuCrankCapacity", laowu$capacityPerRpm);
    }

    @Inject(method = "read", at = @At("TAIL"), remap = false)
    private void laowu$readPower(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if (clientPacket) laowu$capacityPerRpm = tag.contains("LaoWuCrankCapacity")
                ? tag.getFloat("LaoWuCrankCapacity") : -1;
    }
}
