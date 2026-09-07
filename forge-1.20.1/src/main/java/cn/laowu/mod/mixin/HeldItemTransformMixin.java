package cn.laowu.mod.mixin;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.client.HeldItemTransformState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemRenderer.class, remap = false)
public abstract class HeldItemTransformMixin {
    @Inject(method = {
            "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
            "m_115143_(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V"
    }, at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/client/ForgeHooksClient;handleCameraTransforms(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/item/ItemDisplayContext;Z)Lnet/minecraft/client/resources/model/BakedModel;",
            shift = At.Shift.AFTER), remap = false, require = 1)
    private void laowu$applyHeldItemTransform(ItemStack stack, ItemDisplayContext context,
                                              boolean leftHanded, PoseStack poseStack,
                                              MultiBufferSource buffers, int packedLight, int packedOverlay,
                                              net.minecraft.client.resources.model.BakedModel model,
                                              CallbackInfo callbackInfo) {
        boolean leftHand = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        boolean held = leftHand
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        // FIXED (item frames and Create value boxes) has its own model-space
        // centring and must not inherit the inventory editor's large offsets.
        boolean gui = context == ItemDisplayContext.GUI;
        if ((!held && !gui) || stack.isEmpty()) return;
        // The scanner deliberately uses a supplied flat 16x16 sprite in GUI.
        // Legacy saves may still contain the old 3D scanner's GUI transform;
        // applying it after model selection rotates that flat icon. Keep only
        // the independently saved hand transform for this item.
        if (gui && stack.is(LaoWuMod.CAT_SCANNER.get())) return;

        HeldItemTransformState.Target target;
        if (gui) {
            target = HeldItemTransformState.Target.GUI;
        } else if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            target = HeldItemTransformState.Target.FIRST_PERSON;
        } else {
            target = HeldItemTransformState.Target.THIRD_PERSON;
        }
        HeldItemTransformState.Values transform = HeldItemTransformState.current(stack, target);
        float side = leftHand ? -1.0F : 1.0F;
        poseStack.translate(transform.offsetX() * side, transform.offsetY(), transform.offsetZ());
        poseStack.mulPose(Axis.YP.rotationDegrees((float) transform.rotationY() * side));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) transform.rotationX()));
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) transform.rotationZ() * side));
        float scale = (float) transform.scale();
        poseStack.scale(scale, scale, scale);
    }
}
