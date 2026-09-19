package cn.laowu.mod.mixin;

import cn.laowu.mod.CatAgentSmoke;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Also reject cached TargetGoal targets and vanilla Brain/TargetingConditions reacquisition. */
@Mixin(value = LivingEntity.class, remap = false)
public abstract class LivingEntityAgentSmokeTargetMixin {
    @Inject(method = {"canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", "m_6779_(Lnet/minecraft/world/entity/LivingEntity;)Z"},
            at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void laowu$concealedAgent(LivingEntity candidate, CallbackInfoReturnable<Boolean> callback) {
        if ((Object)this instanceof Mob observer && CatAgentSmoke.hiddenFrom(observer, candidate))
            callback.setReturnValue(false);
    }
}
