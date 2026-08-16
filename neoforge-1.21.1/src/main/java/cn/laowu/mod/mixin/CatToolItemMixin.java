package cn.laowu.mod.mixin;

import cn.laowu.mod.item.CatToolBehavior;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Replaces cat tools' and cat armor's vanilla durability presentation with hiss value. */
@Mixin(value = Item.class, remap = false)
abstract class CatToolItemMixin {
    @Inject(method = {"isBarVisible", "m_142522_"}, at = @At("HEAD"),
            cancellable = true, remap = false, require = 1)
    private void laowu$showHissBar(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (CatToolBehavior.isCatTool(stack)) callback.setReturnValue(stack.isDamaged());
    }

    @Inject(method = {"getBarWidth", "m_142158_"}, at = @At("HEAD"),
            cancellable = true, remap = false, require = 1)
    private void laowu$getHissBarWidth(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (CatToolBehavior.isCatTool(stack)) callback.setReturnValue(CatToolBehavior.barWidth(stack));
    }

    @Inject(method = {"getBarColor", "m_142159_"}, at = @At("HEAD"),
            cancellable = true, remap = false, require = 1)
    private void laowu$getHissBarColor(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (CatToolBehavior.isCatTool(stack)) callback.setReturnValue(CatToolBehavior.BAR_COLOR);
    }

    @Inject(method = {"appendHoverText", "m_7373_"}, at = @At("TAIL"),
            remap = false, require = 1)
    private void laowu$appendHissValue(ItemStack stack, Item.TooltipContext context,
                                      List<Component> tooltip, TooltipFlag flag,
                                      CallbackInfo callback) {
        if (!CatToolBehavior.isCatTool(stack)) return;
        tooltip.add(Component.translatable("tooltip.laowu.hiss_value",
                CatToolBehavior.remaining(stack), stack.getMaxDamage()).withStyle(ChatFormatting.GRAY));
    }
}
