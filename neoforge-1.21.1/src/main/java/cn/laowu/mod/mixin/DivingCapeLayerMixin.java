package cn.laowu.mod.mixin;

import cn.laowu.mod.client.CatDivingRiderPose;
import cn.laowu.mod.entity.CatDivingCarrier;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserve vanilla skin/visibility/swing while attaching the cape to the prone torso. */
@Mixin(CapeLayer.class)
public abstract class DivingCapeLayerMixin extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
    protected DivingCapeLayerMixin(RenderLayerParent<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }
    @Inject(method="render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
            at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",shift=At.Shift.AFTER))
    private void laowu$divingCape(PoseStack stack,MultiBufferSource buffers,int light,AbstractClientPlayer player,
                                 float walk,float walkAmount,float partial,float age,float yaw,float pitch,CallbackInfo ci) {
        if(player.getVehicle() instanceof CatDivingCarrier carrier&&carrier.swimmingPose(partial)>0)
            CatDivingRiderPose.applyCape(stack,getParentModel());
    }
}
