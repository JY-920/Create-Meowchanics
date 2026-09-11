package cn.laowu.mod.mixin;

import cn.laowu.mod.ServerConfig;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep goals installed, so changing the world setting is reversible. */
@Mixin(PanicGoal.class)
public abstract class WildCatPanicMixin {
    // Capture the constructor argument: Mixin cannot alias a protected target field.
    @Unique private PathfinderMob laowu$mob;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/PathfinderMob;D)V", at = @At("RETURN"))
    private void laowu$captureMob(PathfinderMob mob, double speed, CallbackInfo ci) {
        laowu$mob = mob;
    }

    @Inject(method = {"canUse", "m_8036_"}, at = @At("HEAD"), cancellable = true)
    private void laowu$wildCatFlee(CallbackInfoReturnable<Boolean> cir) {
        if (laowu$mob instanceof Cat cat && !cat.isTame() && !ServerConfig.wildCatsFlee())
            cir.setReturnValue(false);
    }

    @Inject(method = {"canContinueToUse", "m_8045_"}, at = @At("HEAD"), cancellable = true)
    private void laowu$stopWildCatFlee(CallbackInfoReturnable<Boolean> cir) {
        if (laowu$mob instanceof Cat cat && !cat.isTame() && !ServerConfig.wildCatsFlee()) {
            cat.getNavigation().stop();
            cir.setReturnValue(false);
        }
    }
}
