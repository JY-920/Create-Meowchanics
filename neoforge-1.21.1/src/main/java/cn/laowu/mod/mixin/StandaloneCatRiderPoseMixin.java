package cn.laowu.mod.mixin;

import cn.laowu.mod.compat.create.CreateIntegration;
import cn.laowu.mod.entity.CatFlightCarrier;
import cn.laowu.mod.entity.CatDivingCarrier;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Standalone posing only; Create keeps its original skyhook renderer and animation. */
@Mixin(HumanoidModel.class)
public abstract class StandaloneCatRiderPoseMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("RETURN"))
    private void laowu$standaloneRide(LivingEntity entity, float limbSwing, float limbAmount,
                                     float age, float headYaw, float headPitch, CallbackInfo ci) {
        if (CreateIntegration.isLoaded() || !(entity instanceof Player player)) return;
        var model = (HumanoidModel<?>)(Object)this;
        if (player.getVehicle() instanceof CatDivingCarrier diving) {
            float partial = net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
            float amount = diving.swimmingPose(partial);
            if (amount > 0) {
                float heading = cn.laowu.mod.client.CatPilotFlightClient.viewYaw(diving, partial);
                float body = net.minecraft.util.Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
                cn.laowu.mod.client.CatDivingRiderPose.apply(model, age, amount,
                        net.minecraft.util.Mth.wrapDegrees(heading - body));
            }
        } else if (player.getVehicle() instanceof CatFlightCarrier) {
            // Hold the cat harness overhead without needing Create's wrench or renderer.
            model.leftArm.xRot = model.rightArm.xRot = -(float)Math.PI;
            model.leftArm.yRot = 0.1F; model.rightArm.yRot = -0.1F;
            model.leftArm.zRot = -0.1F; model.rightArm.zRot = 0.1F;
            model.leftLeg.xRot = model.rightLeg.xRot = 0.1F;
            model.leftLeg.yRot = model.rightLeg.yRot = 0;
        }
    }
}
