package cn.laowu.mod.mixin;

import cn.laowu.mod.LaoWuMod;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/** Keeps Lao Wu ammunition exclusive to the Cat Cannon without removing its Create projectile data. */
@Mixin(value = PotatoCannonItem.class, remap = false)
abstract class PotatoCannonAmmoMixin {
    // This overrides a vanilla ProjectileWeaponItem method, so Forge renames it
    // in production even though the target class belongs to Create.
    @Inject(method = {"getAllSupportedProjectiles", "m_6437_"}, at = @At("RETURN"),
            cancellable = true, remap = false, require = 1)
    private void laowu$excludeCatPancakesFromTheRegularCannon(
            CallbackInfoReturnable<Predicate<ItemStack>> callback) {
        Predicate<ItemStack> createProjectiles = callback.getReturnValue();
        callback.setReturnValue(stack -> !stack.is(LaoWuMod.CAT_PANCAKE.get())
                && !stack.is(LaoWuMod.CAT_GRENADE.get())
                && createProjectiles.test(stack));
    }
}
