package cn.laowu.mod.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(net.minecraft.client.renderer.entity.layers.ItemInHandLayer.class)
public abstract class LaserPointerOwnerMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void laowu$owner(net.minecraft.world.entity.LivingEntity owner, net.minecraft.world.item.ItemStack stack,
            net.minecraft.world.item.ItemDisplayContext context, net.minecraft.world.entity.HumanoidArm arm,
            com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers,
            int light, CallbackInfo ci) {
        cn.laowu.mod.client.LaserPointerAim.renderingOwner = owner;
    }
    @Inject(method = "renderArmWithItem", at = @At("RETURN"))
    private void laowu$clearOwner(net.minecraft.world.entity.LivingEntity owner, net.minecraft.world.item.ItemStack stack,
            net.minecraft.world.item.ItemDisplayContext context, net.minecraft.world.entity.HumanoidArm arm,
            com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers,
            int light, CallbackInfo ci) {
        cn.laowu.mod.client.LaserPointerAim.renderingOwner = null;
    }
}
