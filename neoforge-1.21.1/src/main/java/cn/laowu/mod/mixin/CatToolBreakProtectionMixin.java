package cn.laowu.mod.mixin;

import cn.laowu.mod.item.CatToolBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Consumer;

/** Leaves exhausted cat equipment in its slot instead of running vanilla's break-and-shrink branch. */
@Mixin(value = ItemStack.class, remap = false)
abstract class CatToolBreakProtectionMixin {
    @Inject(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;"
            + "Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"),
            cancellable = true, remap = false, require = 1)
    private void laowu$preserveExhaustedTool(int amount, ServerLevel level,
            LivingEntity entity, Consumer<Item> onBroken, CallbackInfo callback) {
        ItemStack self = (ItemStack) (Object) this;
        if (!CatToolBehavior.isCatTool(self)) return;
        self.setDamageValue(self.getMaxDamage());
        callback.cancel();
    }

    /** Hides vanilla's advanced "Durability" line; the custom Hiss line is authoritative. */
    @Inject(method = "getTooltipLines", at = @At("RETURN"),
            remap = false, require = 1)
    private void laowu$removeVanillaDurabilityTooltip(Item.TooltipContext context,
                                                       Player player, TooltipFlag flag,
                                                       CallbackInfoReturnable<List<Component>> callback) {
        ItemStack self = (ItemStack) (Object) this;
        if (!CatToolBehavior.isCatTool(self)) return;
        callback.getReturnValue().removeIf(component ->
                component.getContents() instanceof TranslatableContents translated
                        && "item.durability".equals(translated.getKey()));
    }
}
