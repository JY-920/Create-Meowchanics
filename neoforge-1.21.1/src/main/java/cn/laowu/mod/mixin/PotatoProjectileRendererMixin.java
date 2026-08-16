package cn.laowu.mod.mixin;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.client.CatGrenadeProjectileModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.equipment.potatoCannon.PotatoProjectileEntity;
import com.simibubi.create.content.equipment.potatoCannon.PotatoProjectileRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the inventory icon a normal, crisp generated item while retaining the
 * supplied 3-D model for the flying Create projectile.
 */
@Mixin(value = PotatoProjectileRenderer.class, remap = false)
abstract class PotatoProjectileRendererMixin {
    @Inject(method = "render(Lcom/simibubi/create/content/equipment/potatoCannon/PotatoProjectileEntity;"
            + "FFLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void laowu$renderCatGrenade(PotatoProjectileEntity entity, float yaw,
                                        float partialTicks, PoseStack pose,
                                        MultiBufferSource buffers, int light,
                                        CallbackInfo callback) {
        if (!entity.getItem().is(LaoWuMod.CAT_GRENADE.get())) return;

        pose.pushPose();
        pose.translate(0.0D, entity.getBoundingBox().getYsize() / 2.0D - 0.125D, 0.0D);
        entity.getRenderMode().transform(pose, entity, partialTicks);
        CatGrenadeProjectileModel.render(pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        callback.cancel();
    }
}
