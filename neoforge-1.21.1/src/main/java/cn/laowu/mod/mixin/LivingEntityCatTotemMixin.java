package cn.laowu.mod.mixin;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.CatTotemItem;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extends vanilla's own death-protection path with the rechargeable cat totem. */
@Mixin(value = LivingEntity.class, remap = false)
public abstract class LivingEntityCatTotemMixin {
    @Inject(method = {"checkTotemDeathProtection", "m_21262_"}, at = @At("RETURN"),
            cancellable = true, remap = false, require = 1)
    private void laowu$tryCatTotem(DamageSource source,
                                   CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        LivingEntity entity = (LivingEntity) (Object) this;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = entity.getItemInHand(hand);
            if (!stack.is(LaoWuMod.CAT_TOTEM.get()) || CatTotemItem.charges(stack) <= 0) continue;
            if (NeoForge.EVENT_BUS.post(new LivingUseTotemEvent(
                    entity, source, stack, hand)).isCanceled()) continue;

            ItemStack usedStack = stack.copyWithCount(1);
            if (!CatTotemItem.consumeCharge(stack)) return;

            if (entity instanceof ServerPlayer player) {
                player.awardStat(Stats.ITEM_USED.get(LaoWuMod.CAT_TOTEM.get()), 1);
                CriteriaTriggers.USED_TOTEM.trigger(player, usedStack);
            }

            // This is deliberately identical to vanilla's totem branch so
            // effect durations, particles, sounds and activation animation stay intact.
            entity.setHealth(1.0F);
            entity.removeAllEffects();
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
            entity.level().broadcastEntityEvent(entity, (byte) 35);
            // Event 35 keeps vanilla particles and sound. This later packet
            // replaces the hard-coded vanilla Totem item in the local overlay.
            if (entity instanceof ServerPlayer player) {
                ModNetwork.showCatTotemActivation(player);
            }
            callback.setReturnValue(true);
            return;
        }
    }
}
