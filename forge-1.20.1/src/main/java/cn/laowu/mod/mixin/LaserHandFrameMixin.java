package cn.laowu.mod.mixin;
import cn.laowu.mod.client.LaserPointerAim;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.player.LocalPlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemInHandRenderer.class)
public abstract class LaserHandFrameMixin {
    @Inject(method={"renderHandsWithItems","m_109314_"},at=@At("HEAD"))
    private void laowu$frame(float partial, PoseStack pose, MultiBufferSource.BufferSource buffers,
                            LocalPlayer player,int light,CallbackInfo ci) {
        LaserPointerAim.beginHand(pose,partial);
    }
}
