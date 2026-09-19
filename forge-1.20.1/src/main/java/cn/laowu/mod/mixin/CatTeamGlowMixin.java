package cn.laowu.mod.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(net.minecraft.client.Minecraft.class)
public abstract class CatTeamGlowMixin {
    @Inject(method = {"shouldEntityAppearGlowing", "m_91314_"}, at = @At("HEAD"), cancellable = true)
    private void laowu$preview(net.minecraft.world.entity.Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cn.laowu.mod.client.CatAgentWatchClient.visible(entity)
                || cn.laowu.mod.client.CatTeamPreview.visible(entity)) cir.setReturnValue(true);
    }
}
