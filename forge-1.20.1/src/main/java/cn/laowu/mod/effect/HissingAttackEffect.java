package cn.laowu.mod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Marker effect for one empowered melee attack. Unlike vanilla Strength this
 * deliberately has no persistent attack-damage attribute modifier; the bonus
 * is applied and consumed by the living-hurt event.
 */
public final class HissingAttackEffect extends MobEffect {
    public HissingAttackEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xD9D6CB);
        // Supply vanilla's blue "When Applied: +X Attack Damage" tooltip
        // entry. Attribute installation itself is intentionally suppressed
        // below because this bonus belongs to exactly one successful hit.
        addAttributeModifier(Attributes.ATTACK_DAMAGE,
                "9E86D9BD-6E36-4C08-BBE8-EA5D6D744E71", 0.0D,
                AttributeModifier.Operation.ADDITION);
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        int level = amplifier + 1;
        return 10.0D * level * level;
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        // Tooltip-only modifier; CommonEvents applies it to the next hit.
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        // No persistent modifier was installed.
    }
}
