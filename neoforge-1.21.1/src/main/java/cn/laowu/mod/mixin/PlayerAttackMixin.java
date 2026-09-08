package cn.laowu.mod.mixin;

import cn.laowu.mod.CatPancakeBehavior;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 纸棍拍猫修复：织入 {@link Player#attack(Entity)} HEAD。
 *
 * <p>Create 的纸棍（create:cardboard_sword）攻击生物时，在 {@code Player.attack} 内部
 * 中断了 {@code AttackEntityEvent} 的发布（纸棍"不能伤你"：只施加击退、不产生伤害事件），
 * 因此 laowu 用 {@code AttackEntityEvent}/{@code LivingIncomingDamageEvent} 都监听不到纸棍攻击。
 * 在 {@code attack} HEAD 织入本逻辑，使其先于 Create 的任何处理执行：目标为猫且主手为纸棍时，
 * 调用 {@link CatPancakeBehavior#flatten(Cat)} 把猫拍扁为猫饼。
 */
@Mixin(value = Player.class, remap = false)
public abstract class PlayerAttackMixin {

    @Inject(method = "attack", at = @At("HEAD"), remap = false, require = 1)
    private void laowu$flattenOnCardboardSword(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide) return;
        if (!(target instanceof Cat cat)) return;
        ItemStack held = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
        var itemKey = BuiltInRegistries.ITEM.getKey(held.getItem());
        if (itemKey == null || !itemKey.getNamespace().equals("create")
                || !itemKey.getPath().equals("cardboard_sword")) return;
        CatPancakeBehavior.flatten(cat);
    }
}
