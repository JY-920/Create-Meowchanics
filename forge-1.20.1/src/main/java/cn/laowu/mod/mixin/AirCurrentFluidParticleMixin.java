package cn.laowu.mod.mixin;

import cn.laowu.mod.create.HissingCollectorBlockEntity;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Hides the fan's straight airflow particle while its collector is dispersing fluid. */
@Mixin(value = AirCurrent.class, remap = false)
abstract class AirCurrentFluidParticleMixin {
    @Shadow @Final public IAirCurrentSource source;

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
                    remap = false),
            require = 0,
            remap = false
    )
    private void laowu$hideNamedFanParticle(Level level, ParticleOptions particle,
                                            double x, double y, double z,
                                            double velocityX, double velocityY,
                                            double velocityZ) {
        forwardUnlessCollectorDisperses(level, particle, x, y, z,
                velocityX, velocityY, velocityZ);
    }

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;m_7106_(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
                    remap = false),
            require = 0,
            remap = false
    )
    private void laowu$hideProductionFanParticle(Level level, ParticleOptions particle,
                                                 double x, double y, double z,
                                                 double velocityX, double velocityY,
                                                 double velocityZ) {
        forwardUnlessCollectorDisperses(level, particle, x, y, z,
                velocityX, velocityY, velocityZ);
    }

    private void forwardUnlessCollectorDisperses(Level level, ParticleOptions particle,
                                                  double x, double y, double z,
                                                  double velocityX, double velocityY,
                                                  double velocityZ) {
        BlockPos collectorPos = source.getAirCurrentPos()
                .relative(source.getAirflowOriginSide());
        BlockEntity blockEntity = level.getBlockEntity(collectorPos);
        if (blockEntity instanceof HissingCollectorBlockEntity collector
                && collector.isDispersingFluid())
            return;
        level.addParticle(particle, x, y, z, velocityX, velocityY, velocityZ);
    }
}
