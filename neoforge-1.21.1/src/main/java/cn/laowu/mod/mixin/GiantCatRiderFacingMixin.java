package cn.laowu.mod.mixin;

import cn.laowu.mod.client.GiantCatRiderMotion;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Replace the render-local body yaw before the renderer derives relative head yaw. */
@Mixin(LivingEntityRenderer.class)
public abstract class GiantCatRiderFacingMixin {
    @ModifyVariable(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("STORE"), ordinal = 2)
    private float laowu$alignGiantRider(float bodyYaw, LivingEntity entity, float yaw, float partialTick,
                                      PoseStack pose, MultiBufferSource buffers, int light) {
        return GiantCatRiderMotion.bodyYaw(entity, partialTick, bodyYaw);
    }
}
