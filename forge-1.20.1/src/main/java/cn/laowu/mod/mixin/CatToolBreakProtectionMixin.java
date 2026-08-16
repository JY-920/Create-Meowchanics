package cn.laowu.mod.mixin;

import cn.laowu.mod.item.CatToolBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Consumer;

/** Leaves an exhausted cat tool in its slot instead of running vanilla's break-and-shrink branch. */
@Mixin(value = ItemStack.class, remap = false)
abstract class CatToolBreakProtectionMixin {
    /** Enhanced tools pay three times the durability/hiss cost of the same vanilla action. */
    @ModifyVariable(method = {"hurtAndBreak", "m_41622_"}, at = @At("HEAD"),
            argsOnly = true, ordinal = 0, remap = false, require = 1)
    private int laowu$multiplyEmpoweredHissCost(int amount) {
        return CatToolBehavior.hissCost((ItemStack) (Object) this, amount);
    }

    @Inject(method = {"hurtAndBreak", "m_41622_"}, at = @At(value = "INVOKE",
            target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"),
            cancellable = true, remap = false, require = 1)
    private <T extends LivingEntity> void laowu$preserveExhaustedTool(
            int amount, T entity, Consumer<T> onBroken, CallbackInfo callback) {
        ItemStack self = (ItemStack) (Object) this;
        if (!CatToolBehavior.isCatTool(self)) return;
        self.setDamageValue(self.getMaxDamage());
        callback.cancel();
    }

    /** Hides vanilla's advanced "Durability" line; the custom Hiss line is authoritative. */
    @Inject(method = {"getTooltipLines", "m_41651_"}, at = @At("RETURN"),
            remap = false, require = 1)
    private void laowu$removeVanillaDurabilityTooltip(Player player, TooltipFlag flag,
                                                       CallbackInfoReturnable<List<Component>> callback) {
        ItemStack self = (ItemStack) (Object) this;
        if (!CatToolBehavior.isCatTool(self)) return;
        callback.getReturnValue().removeIf(component ->
                component.getContents() instanceof TranslatableContents translated
                        && "item.durability".equals(translated.getKey()));
    }
}
