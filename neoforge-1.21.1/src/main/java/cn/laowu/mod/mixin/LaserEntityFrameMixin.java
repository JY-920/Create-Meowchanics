package cn.laowu.mod.mixin;
import cn.laowu.mod.client.LaserPointerAim;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntityRenderer.class)
public abstract class LaserEntityFrameMixin {
    @Inject(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("HEAD"))
    private void laowu$frame(LivingEntity entity,float yaw,float partial,PoseStack pose,
                            MultiBufferSource buffers,int light,CallbackInfo ci) {
        LaserPointerAim.beginEntity(entity,pose,partial);
    }
    @Inject(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("RETURN"))
    private void laowu$end(LivingEntity entity,float yaw,float partial,PoseStack pose,
                          MultiBufferSource buffers,int light,CallbackInfo ci) {
        LaserPointerAim.endEntity();
    }
}
