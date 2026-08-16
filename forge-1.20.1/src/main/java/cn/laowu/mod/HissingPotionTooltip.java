package cn.laowu.mod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Rewords the vanilla attack-damage line without changing its vanilla styling. */
public final class HissingPotionTooltip {
    public static void rewrite(List<MobEffectInstance> effects, List<Component> tooltip) {
        MobEffectInstance hissing = effects.stream()
                .filter(effect -> effect.getEffect() == LaoWuMod.HISSING_ATTACK.get())
                .findFirst()
                .orElse(null);
        if (hissing == null) return;

        int level = hissing.getAmplifier() + 1;
        String amount = ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(10.0D * level * level);
        String attackDamageName = Component.translatable(
                Attributes.ATTACK_DAMAGE.getDescriptionId()).getString();

        for (int index = 0; index < tooltip.size(); index++) {
            Component line = tooltip.get(index);
            if (!(line.getContents() instanceof TranslatableContents translated)
                    || !translated.getKey().equals("attribute.modifier.plus.0")) continue;
            Object[] arguments = translated.getArgs();
            if (arguments.length < 2
                    || !amount.equals(String.valueOf(arguments[0]))
                    || !(arguments[1] instanceof Component attributeName)
                    || !attackDamageName.equals(attributeName.getString())) continue;

            tooltip.set(index, Component.translatable(
                    "tooltip.laowu.next_melee_attack_damage", amount)
                    .withStyle(ChatFormatting.BLUE));
            return;
        }
    }

    private HissingPotionTooltip() { }
}
