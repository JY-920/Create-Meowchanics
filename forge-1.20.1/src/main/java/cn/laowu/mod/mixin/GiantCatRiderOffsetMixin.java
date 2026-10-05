package cn.laowu.mod.mixin;

import cn.laowu.mod.client.GiantCatRiderMotion;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Dispatcher offset moves the whole rider, including armor, hands and cape. */
@Mixin(PlayerRenderer.class)
public abstract class GiantCatRiderOffsetMixin {
    @Inject(method = "getRenderOffset(Lnet/minecraft/client/player/AbstractClientPlayer;F)Lnet/minecraft/world/phys/Vec3;",
            at = @At("RETURN"), cancellable = true)
    private void laowu$giantBack(AbstractClientPlayer player, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        double bob = GiantCatRiderMotion.offset(player, partialTick);
        if (bob != 0) cir.setReturnValue(cir.getReturnValue().add(0, bob, 0));
    }
}
