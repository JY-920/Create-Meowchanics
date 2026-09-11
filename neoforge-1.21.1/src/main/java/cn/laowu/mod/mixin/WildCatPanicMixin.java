package cn.laowu.mod.mixin;

import cn.laowu.mod.ServerConfig;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep goals installed, so changing the world setting is reversible. */
@Mixin(PanicGoal.class)
public abstract class WildCatPanicMixin {
    @Shadow @Final protected PathfinderMob mob;
    @Inject(method = {"canUse"}, at = @At("HEAD"), cancellable = true)
    private void laowu$wildCatFlee(CallbackInfoReturnable<Boolean> cir) {
        if (mob instanceof Cat cat && !cat.isTame() && !ServerConfig.wildCatsFlee())
            cir.setReturnValue(false);
    }

    @Inject(method = {"canContinueToUse"}, at = @At("HEAD"), cancellable = true)
    private void laowu$stopWildCatFlee(CallbackInfoReturnable<Boolean> cir) {
        if (mob instanceof Cat cat && !cat.isTame() && !ServerConfig.wildCatsFlee()) {
            mob.getNavigation().stop();
            cir.setReturnValue(false);
        }
    }
}
