package cn.laowu.mod.mixin;

import cn.laowu.mod.client.GiantCatRiderMotion;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Scope only GUI entity drawing; the world behind an open screen still animates normally. */
@Mixin(InventoryScreen.class)
public abstract class GiantCatRiderInventoryMixin {
    @ModifyArg(method = "renderEntityInInventory(Lnet/minecraft/client/gui/GuiGraphics;FFFLorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;runAsFancy(Ljava/lang/Runnable;)V", remap = false), index = 0)
    private static Runnable laowu$isolateGiantPreview(Runnable draw) {
        return GiantCatRiderMotion.preview(draw);
    }
}
