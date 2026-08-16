package cn.laowu.mod.create;

import cn.laowu.mod.CatPoseData;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.mixin.NozzleBlockEntityAccessor;
import cn.laowu.mod.particle.NozzleFluidPuffData;
import cn.laowu.mod.recipe.PotionArrowInfiltration;
import com.simibubi.create.AllParticleTypes;
import com.simibubi.create.content.fluids.particle.FluidParticleData;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.fan.NozzleBlock;
import com.simibubi.create.content.kinetics.fan.NozzleBlockEntity;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessing;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Create nozzle airflow plus a bidirectional 8000 mB fluid tank. */
public final class HissingCollectorBlockEntity extends NozzleBlockEntity
        implements IHaveGoggleInformation {
    public static final int CAPACITY = 8000;
    public static final int MAX_CATS = 6;
    public static final int MAX_RATE_PER_SECOND = 2000;
    private static final double PER_CAT_PER_TICK =
            (double) MAX_RATE_PER_SECOND / MAX_CATS / 20.0D;
    /** An 8000 mB tank lasts 40 seconds while the fan is blowing. */
    private static final int FLUID_CONSUMPTION_PER_TICK = 10;
    /** Vanilla lingering clouds let the same victim be affected once per second. */
    private static final int POTION_REAPPLICATION_TICKS = 20;

    private SmartFluidTankBehaviour outputTank;
    private double productionRemainder;

    public HissingCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(LaoWuMod.HISSING_COLLECTOR_BE.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        // Keep the historical OUTPUT behaviour id so existing worlds retain
        // their saved tank contents; the flags below make the port bidirectional.
        outputTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT,
                this, 1, CAPACITY, true)
                .allowInsertion()
                .allowExtraction()
                .whenFluidUpdates(this::notifyUpdate);
        // The collector is also a disperser while the fan blows. Keeping this
        // unfiltered lets pipes insert water, lava, Create potion fluid, or a
        // fluid from another add-on without a special-case capability wrapper.
        outputTank.getPrimaryHandler().setValidator(stack -> true);
        behaviours.add(outputTank);
    }

    @Override
    public void tick() {
        // Reuse Create's native nozzle range, visibility cache and entity list.
        super.tick();
        if (level == null || outputTank == null) return;

        NozzleBlockEntityAccessor nozzle = (NozzleBlockEntityAccessor) (Object) this;
        if (nozzle.laowu$getRange() <= 0.0F) return;

        AirflowMode airflow = getAirflowMode();
        if (airflow == AirflowMode.NONE) return;

        if (level.isClientSide) {
            if (airflow == AirflowMode.BLOWING)
                spawnClientProcessingParticles(nozzle);
            return;
        }

        if (airflow == AirflowMode.SUCKING) {
            collectHiss(nozzle.laowu$getPushingEntities());
            return;
        }

        disperseStoredFluid(nozzle.laowu$getPushingEntities());
    }

    /**
     * Reads the adjacent Create fan directly. Unlike the nozzle's lazily
     * refreshed private flag, this cannot briefly confuse suction with blowing
     * after the fan reverses.
     */
    private AirflowMode getAirflowMode() {
        Direction outward = getBlockState().getValue(NozzleBlock.FACING);
        BlockEntity sourceBlockEntity = level.getBlockEntity(
                worldPosition.relative(outward.getOpposite()));
        if (!(sourceBlockEntity instanceof IAirCurrentSource source)
                || source.getSpeed() == 0.0F)
            return AirflowMode.NONE;

        Direction airflow = source.getAirFlowDirection();
        if (airflow == outward) return AirflowMode.BLOWING;
        if (airflow == outward.getOpposite()) return AirflowMode.SUCKING;
        return AirflowMode.NONE;
    }

    private void collectHiss(List<Entity> entities) {
        FluidStack stored = outputTank.getPrimaryHandler().getFluid();
        if (!stored.isEmpty() && !stored.getFluid().isSame(LaoWuMod.HISSING_GAS.get()))
            return;

        int cats = 0;
        for (Entity entity : entities) {
            if (entity instanceof Cat cat && cat.isAlive() && CatPoseData.isHissing(cat)) {
                if (++cats >= MAX_CATS) break;
            }
        }
        if (cats == 0) return;

        productionRemainder += cats * PER_CAT_PER_TICK;
        int generated = (int) Math.floor(productionRemainder);
        if (generated <= 0) return;
        productionRemainder -= generated;

        outputTank.getPrimaryHandler().fill(
                new FluidStack(LaoWuMod.HISSING_GAS.get(), generated),
                IFluidHandler.FluidAction.EXECUTE);
    }

    private void disperseStoredFluid(List<Entity> entities) {
        FluidStack stored = outputTank.getPrimaryHandler().getFluid().copy();
        if (stored.isEmpty()) return;

        FluidStack drained = outputTank.getPrimaryHandler().drain(
                FLUID_CONSUMPTION_PER_TICK, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) return;

        if (stored.getFluid().is(FluidTags.WATER)) {
            applyFanProcessing(entities, AllFanProcessingTypes.SPLASHING);
        } else if (stored.getFluid().is(FluidTags.LAVA)) {
            applyFanProcessing(entities, AllFanProcessingTypes.BLASTING);
        } else if (PotionArrowInfiltration.isUsablePotionFluid(stored)
                && level.getGameTime() % POTION_REAPPLICATION_TICKS == 0L) {
            applyLingeringPotion(entities, stored);
        }
    }

    /** Mirrors Create's native bulk fan processing for entities in the nozzle cache. */
    private void applyFanProcessing(List<Entity> entities, FanProcessingType type) {
        for (Entity entity : entities) {
            if (!entity.isAlive()) continue;
            if (entity instanceof ItemEntity item) {
                if (FanProcessing.canProcess(item, type))
                    FanProcessing.applyProcessing(item, type);
            } else {
                type.affectEntity(entity, level);
            }
        }
    }

    /** Applies the same duration and instant-effect strength used by lingering clouds. */
    private void applyLingeringPotion(List<Entity> entities, FluidStack potionFluid) {
        CompoundTag tag = potionFluid.getTag();
        if (tag == null) return;
        List<MobEffectInstance> effects = PotionUtils.getAllEffects(tag);
        if (effects.isEmpty()) return;

        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive()
                    || !living.isAffectedByPotions())
                continue;

            for (MobEffectInstance effect : effects) {
                if (effect.getEffect().isInstantenous()) {
                    effect.getEffect().applyInstantenousEffect(
                            null, null, living, effect.getAmplifier(), 0.5D);
                    continue;
                }

                int duration = effect.isInfiniteDuration()
                        ? MobEffectInstance.INFINITE_DURATION
                        : Math.max(1, effect.getDuration() / 4);
                living.addEffect(new MobEffectInstance(
                        effect.getEffect(), duration, effect.getAmplifier(),
                        effect.isAmbient(), effect.isVisible(), effect.showIcon()));
            }
        }
    }

    /**
     * Client-only fluid particles. Create's FluidStackParticle resolves the
     * texture and tint from the complete FluidStack, so NBT-coloured potion
     * fluids and fluids supplied by other mods retain their real colour.
     */
    private void spawnClientProcessingParticles(NozzleBlockEntityAccessor nozzle) {
        FluidStack stored = outputTank.getPrimaryHandler().getFluid();
        if (stored.isEmpty()) return;

        // These are Create's familiar item-processing particles. They are
        // separate from the fluid-coloured particles emitted by the net.
        FanProcessingType processingType = null;
        if (stored.getFluid().is(FluidTags.WATER))
            processingType = AllFanProcessingTypes.SPLASHING;
        else if (stored.getFluid().is(FluidTags.LAVA))
            processingType = AllFanProcessingTypes.BLASTING;
        if (processingType != null) {
            for (Entity entity : nozzle.laowu$getPushingEntities()) {
                if (entity instanceof ItemEntity && entity.isAlive())
                    processingType.spawnProcessingParticles(level, entity.position());
            }
        }

        float range = nozzle.laowu$getRange();
        int nativeInterval = Mth.clamp(
                AllConfigs.server().kinetics.fanPushDistance.get() - (int) range,
                1, 10);
        // Twice the native nozzle emission frequency; every emission also
        // carries four short-lived flower/POOF sprites.
        int particleInterval = Math.max(1, (nativeInterval + 1) / 2);
        if (level.random.nextInt(particleInterval) != 0) return;

        FluidStack particleFluid = stored.copy();
        particleFluid.setAmount(1);
        FluidParticleData particle = new FluidParticleData(
                AllParticleTypes.FLUID_PARTICLE.get(), particleFluid);

        // Fluid-texture mote around the net.
        Vec3 center = VecHelper.getCenterOf(worldPosition);
        Vec3 point = VecHelper.offsetRandomly(center, level.random, 1.0F);
        Vec3 motion = center.subtract(point).normalize().scale(
                -Mth.clamp(range * 0.025F, 0.0F, 0.5F));
        level.addParticle(particle, point.x, point.y, point.z,
                motion.x, motion.y, motion.z);

        // Create's nozzle uses the eight-frame generic POOF sequence. Spawn
        // that same flower-like animation around the collector, but carry the
        // full FluidStack so water, lava and NBT-coloured potions tint it.
        NozzleFluidPuffData puff = new NozzleFluidPuffData(particleFluid);
        for (int index = 0; index < 4; index++) {
            Vec3 puffPoint = VecHelper.offsetRandomly(center, level.random, 1.0F);
            Vec3 puffMotion = center.subtract(puffPoint).normalize().scale(
                    -Mth.clamp(range * 0.025F, 0.0F, 0.5F));
            level.addParticle(puff, puffPoint.x, puffPoint.y, puffPoint.z,
                    puffMotion.x, puffMotion.y, puffMotion.z);
        }
    }

    /** Used by client mixins to replace Create's two default white airflow particles. */
    public boolean isDispersingFluid() {
        return level != null && outputTank != null
                && !outputTank.getPrimaryHandler().getFluid().isEmpty()
                && getAirflowMode() == AirflowMode.BLOWING;
    }

    /** Preserve the native poof while tinting it to the dispersed fluid. */
    public ParticleOptions colourNativeNozzleParticle(ParticleOptions fallback) {
        if (!isDispersingFluid()) return fallback;
        return new NozzleFluidPuffData(outputTank.getPrimaryHandler().getFluid());
    }

    private enum AirflowMode {
        NONE,
        SUCKING,
        BLOWING
    }

    /** The rotated model's outward face is its sole pipe-accessible fluid port. */
    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability,
                                                      @Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            if (outputTank != null && (side == null
                    || side == getBlockState().getValue(NozzleBlock.FACING))) {
                return outputTank.getCapability().cast();
            }
            return LazyOptional.empty();
        }
        return super.getCapability(capability, side);
    }

    public SmartFluidTankBehaviour getOutputTank() {
        return outputTank;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (outputTank == null) return false;
        LazyOptional<IFluidHandler> capability = outputTank.getCapability().cast();
        return containedFluidTooltip(tooltip, isPlayerSneaking, capability);
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putDouble("ProductionRemainder", productionRemainder);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        productionRemainder = tag.getDouble("ProductionRemainder");
    }
}
