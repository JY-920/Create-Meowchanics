package cn.laowu.mod.mixin;

import cn.laowu.mod.accessory.CatCommonAccessories;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Read-only post-health observation. No redirects, render hooks or changes to vanilla damage flow. */
@Mixin(value = LivingEntity.class, remap = false)
public abstract class LivingEntityCatAccessoryDamageMixin {
    @Inject(method = {"actuallyHurt", "m_6475_"}, at = @At("RETURN"), remap = false, require = 1)
    private void laowu$acceptedAccessoryDamage(DamageSource source, float amount, CallbackInfo callback) {
        CatCommonAccessories.finishDamage((LivingEntity)(Object)this, source);
    }
}
