package cn.laowu.mod.mixin;

import cn.laowu.mod.create.HissingCollectorBlockEntity;
import com.simibubi.create.content.kinetics.fan.NozzleBlockEntity;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Colours the collector net's native nozzle poof without taking ownership of
 * {@code Level#addParticle}. Other Create add-ons (notably Ratatouille) also
 * decorate that invocation, so a Redirect here would make their required
 * injection fail during startup.
 */
@Mixin(value = NozzleBlockEntity.class, remap = false)
abstract class NozzleFluidParticleMixin {
    @ModifyArg(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
                    remap = false),
            index = 0,
            require = 0,
            remap = false
    )
    private ParticleOptions laowu$colourNamedNozzleParticle(ParticleOptions particle) {
        return colourParticle(particle);
    }

    @ModifyArg(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;m_7106_(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
                    remap = false),
            index = 0,
            require = 0,
            remap = false
    )
    private ParticleOptions laowu$colourProductionNozzleParticle(ParticleOptions particle) {
        return colourParticle(particle);
    }

    private ParticleOptions colourParticle(ParticleOptions particle) {
        if ((Object) this instanceof HissingCollectorBlockEntity collector)
            return collector.colourNativeNozzleParticle(particle);
        return particle;
    }
}
