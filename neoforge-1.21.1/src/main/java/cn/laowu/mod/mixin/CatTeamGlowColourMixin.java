package cn.laowu.mod.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(net.minecraft.world.entity.Entity.class)
public abstract class CatTeamGlowColourMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void laowu$preview(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof net.minecraft.world.entity.animal.Cat cat
                && cn.laowu.mod.client.CatTeamPreview.visible(cat))
            cir.setReturnValue(cn.laowu.mod.CatTeamRules.rgb(cat));
    }
}
