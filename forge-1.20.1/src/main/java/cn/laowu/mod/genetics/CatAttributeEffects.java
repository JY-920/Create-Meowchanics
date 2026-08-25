package cn.laowu.mod.genetics;

import cn.laowu.mod.CatClothesData;
import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.CatPoseData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cat;

import java.util.UUID;

/**
 * Single conversion layer from the six displayed integer attributes to
 * Minecraft entity mechanics. Raw genes remain untouched; traits are folded
 * into an effective value before every formula is evaluated.
 */
public final class CatAttributeEffects {
    private static final double VANILLA_CAT_MAX_HEALTH = 10.0D;
    private static final double VANILLA_CAT_ATTACK_DAMAGE = 3.0D;
    private static final double CRITICAL_DAMAGE_MULTIPLIER = 1.5D;

    private static final UUID HEALTH_MODIFIER =
            UUID.fromString("ab5961b3-c3db-45a8-bdf1-82a88dc89208");
    private static final UUID ATTACK_MODIFIER =
            UUID.fromString("291152f7-61e6-42e7-878c-ebd9275f831c");
    private static final UUID ARMOR_MODIFIER =
            UUID.fromString("5bc0c02f-a7ab-4384-bab7-f28662d4d557");
    private static final UUID TOUGHNESS_MODIFIER =
            UUID.fromString("78a4c79e-3e7b-4919-b3af-88bd1ae68869");
    private static final UUID SPEED_MODIFIER =
            UUID.fromString("e06e40b1-91d8-433b-a440-d07654346791");
    private static final UUID FOLLOW_RANGE_MODIFIER =
            UUID.fromString("0f431d3b-6db5-474c-a776-196fac96450e");
    private static final UUID LU_BU_HEALTH_MODIFIER =
            UUID.fromString("63f04289-cacc-4c79-b255-406f27535eeb");

    public static int effectiveValue(CatAttributeProfile attributes,
                                     CatTraitProfile traits, CatStat stat,
                                     boolean nightOwlActive) {
        return effectiveValue(attributes, traits, stat,
                TraitContext.onlyNight(nightOwlActive));
    }

    public static int effectiveValue(CatAttributeProfile attributes,
                                     CatTraitProfile traits, CatStat stat,
                                     boolean night, boolean day) {
        return effectiveValue(attributes, traits, stat,
                TraitContext.onlyTime(night, day));
    }

    private static int effectiveValue(CatAttributeProfile attributes,
                                      CatTraitProfile traits, CatStat stat,
                                      TraitContext context) {
        if (attributes == null) return -1;
        CatTraitProfile resolved = traits == null ? CatTraitProfile.EMPTY : traits;
        int value = attributes.current(stat);
        for (CatTraitInstance instance : resolved.traits()) {
            if (instance.trait().attributeStat() == stat) {
                value += instance.trait().attributeBonus(instance.level());
            }
        }
        if (resolved.has(CatTrait.DOUGHY)) value -= 20;

        if (stat == CatStat.STAMINA) {
            int level = resolved.level(CatTrait.LONG_FUR);
            if (level > 0) value += CatTrait.LONG_FUR.longFurStaminaBonus(level);
            int chonkyLevel = resolved.level(CatTrait.CHONKY_PRESENCE);
            if (chonkyLevel > 0) {
                value += CatTrait.CHONKY_PRESENCE.chonkyStaminaBonus(chonkyLevel);
            }
            int loafLevel = resolved.level(CatTrait.LOAF_THOUGHTS);
            if (context.sitting && loafLevel > 0) {
                value += CatTrait.LOAF_THOUGHTS.loafStaminaBonus(loafLevel);
            }
            int attentionLevel = resolved.level(CatTrait.ATTENTION_MAGNET);
            if (attentionLevel > 0) {
                value += CatTrait.ATTENTION_MAGNET
                        .attentionMagnetStaminaBonus(attentionLevel);
            }
        } else if (stat == CatStat.HEALTH) {
            int level = resolved.level(CatTrait.HEALING_PURR);
            if (level > 0) value += CatTrait.HEALING_PURR.healingPurrHealthBonus(level);
            int chonkyLevel = resolved.level(CatTrait.CHONKY_PRESENCE);
            if (chonkyLevel > 0) {
                value += CatTrait.CHONKY_PRESENCE.chonkyHealthBonus(chonkyLevel);
            }
            int glassLevel = resolved.level(CatTrait.GLASS_CLAWS);
            if (glassLevel > 0) {
                value -= CatTrait.GLASS_CLAWS.glassClawsHealthPenalty(glassLevel);
            }
        } else if (stat == CatStat.ATTACK) {
            int elderLevel = resolved.level(CatTrait.SELECTED_ELDER);
            if (elderLevel > 0) {
                value += CatTrait.SELECTED_ELDER.selectedElderAttackBonus(elderLevel);
            }
            if (resolved.has(CatTrait.LU_BU_REBORN)) {
                value += CatTrait.LU_BU_REBORN.luBuAttackBonus();
            }
            int rageLevel = resolved.level(CatTrait.BRISTLING_RAGE);
            if (context.bristlingRage && rageLevel > 0) {
                value += CatTrait.BRISTLING_RAGE.bristlingAttackBonus(rageLevel);
            }
            int blazingLevel = resolved.level(CatTrait.BLAZING_FORM);
            if (context.blazingForm && blazingLevel > 0) {
                value += CatTrait.BLAZING_FORM.blazingAttackBonus(blazingLevel);
            }
            int protectiveLevel = resolved.level(CatTrait.PROTECTIVE_INSTINCT);
            if (context.protectiveInstinct && protectiveLevel > 0) {
                value += CatTrait.PROTECTIVE_INSTINCT
                        .protectiveAttackBonus(protectiveLevel);
            }
            int wetLevel = resolved.level(CatTrait.WET_FURY);
            if (context.wet && wetLevel > 0) {
                value += CatTrait.WET_FURY.wetFuryAttackBonus(wetLevel);
            }
            if (context.wet && resolved.has(CatTrait.WATER_SHY)) {
                value -= CatTrait.WATER_SHY.waterShyAttackPenalty();
            }
            if (context.timid && resolved.has(CatTrait.TIMID)) {
                value -= CatTrait.TIMID.timidAttackPenalty();
            }
            int glassLevel = resolved.level(CatTrait.GLASS_CLAWS);
            if (glassLevel > 0) {
                value += CatTrait.GLASS_CLAWS.glassClawsAttackBonus(glassLevel);
            }
        } else if (stat == CatStat.SPEED) {
            int chonkyLevel = resolved.level(CatTrait.CHONKY_PRESENCE);
            if (chonkyLevel > 0) {
                value -= CatTrait.CHONKY_PRESENCE.chonkySpeedPenalty(chonkyLevel);
            }
            int wetLevel = resolved.level(CatTrait.WET_FURY);
            if (context.wet && wetLevel > 0) {
                value -= CatTrait.WET_FURY.wetFurySpeedPenalty(wetLevel);
            }
            if (context.wet && resolved.has(CatTrait.WATER_SHY)) {
                value -= CatTrait.WATER_SHY.waterShySpeedPenalty();
            }
            if (context.day && resolved.has(CatTrait.DAY_DROWSY)) {
                value -= CatTrait.DAY_DROWSY.dayDrowsySpeedPenalty();
            }
            if (context.timid && resolved.has(CatTrait.TIMID)) {
                value += CatTrait.TIMID.timidSpeedBonus();
            }
            int tailLevel = resolved.level(CatTrait.TAIL_HELD_HIGH);
            if (context.fullHealth && tailLevel > 0) {
                value += CatTrait.TAIL_HELD_HIGH.highTailSpeedBonus(tailLevel);
            }
        } else if (stat == CatStat.LUCK) {
            int fishingLevel = resolved.level(CatTrait.ANGLERS_FORTUNE);
            if (context.fishing && fishingLevel > 0) {
                value += CatTrait.ANGLERS_FORTUNE.anglersLuckBonus(fishingLevel);
            }
            int tailLevel = resolved.level(CatTrait.TAIL_HELD_HIGH);
            if (context.fullHealth && tailLevel > 0) {
                value += CatTrait.TAIL_HELD_HIGH.highTailLuckBonus(tailLevel);
            }
            int cainLevel = resolved.level(CatTrait.CAIN_MARK);
            if (cainLevel > 0) {
                value += CatTrait.CAIN_MARK.cainLuckBonus(cainLevel);
            }
        } else if (stat == CatStat.INTELLIGENCE) {
            int loafLevel = resolved.level(CatTrait.LOAF_THOUGHTS);
            if (context.sitting && loafLevel > 0) {
                value += CatTrait.LOAF_THOUGHTS.loafIntelligenceBonus(loafLevel);
            }
            if (context.day && resolved.has(CatTrait.DAY_DROWSY)) {
                value -= CatTrait.DAY_DROWSY.dayDrowsyIntelligencePenalty();
            }
        }

        int nightLevel = resolved.level(CatTrait.NIGHT_OWL);
        if (context.night && nightLevel > 0) {
            if (stat == CatStat.ATTACK) {
                value += CatTrait.NIGHT_OWL.nightAttackBonus(nightLevel);
            } else if (stat == CatStat.SPEED) {
                value += CatTrait.NIGHT_OWL.nightSpeedBonus(nightLevel);
            }
        }
        return Mth.clamp(value, 0, 999);
    }

    public static int effectiveValue(Cat cat, CatStat stat) {
        return effectiveValue(cat, CatAttributeData.ensure(cat),
                CatTraitData.ensure(cat), stat);
    }

    /** Living-panel path; uses the same transient conditions as server attributes. */
    public static int effectiveValue(Cat cat, CatAttributeProfile attributes,
                                     CatTraitProfile traits, CatStat stat) {
        return effectiveValue(attributes, traits, stat, context(cat));
    }

    /** Maintains derived modifiers once per second without scanning the world. */
    public static void tick(Cat cat) {
        if (cat.level().isClientSide || cat.tickCount % 20 != 0) return;
        refresh(cat);
    }

    public static void refresh(Cat cat) {
        if (cat.level().isClientSide) return;
        refresh(cat, CatAttributeData.ensure(cat), CatTraitData.ensure(cat));
    }

    static void refresh(Cat cat, CatAttributeProfile attributes,
                        CatTraitProfile traits) {
        if (cat.level().isClientSide || attributes == null) return;
        TraitContext context = context(cat);
        int health = effectiveValue(attributes, traits, CatStat.HEALTH, context);
        int attack = effectiveValue(attributes, traits, CatStat.ATTACK, context);
        int stamina = effectiveValue(attributes, traits, CatStat.STAMINA, context);
        int speed = effectiveValue(attributes, traits, CatStat.SPEED, context);

        float oldHealth = cat.getHealth();
        float oldMaximum = cat.getMaxHealth();
        boolean maximumChanged = setModifier(cat, Attributes.MAX_HEALTH, HEALTH_MODIFIER,
                "Create Meowchanics health", maximumHealth(health) - VANILLA_CAT_MAX_HEALTH,
                AttributeModifier.Operation.ADDITION);
        maximumChanged |= setModifier(cat, Attributes.MAX_HEALTH, LU_BU_HEALTH_MODIFIER,
                "Create Meowchanics Lu Bu health",
                traits.has(CatTrait.LU_BU_REBORN)
                        && CatTraitEffects.isLuBuOutnumbered(cat) ? 1.0D : 0.0D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        if (maximumChanged && cat.isAlive()) {
            float ratio = oldMaximum <= 0.0F ? 1.0F
                    : Mth.clamp(oldHealth / oldMaximum, 0.0F, 1.0F);
            cat.setHealth(Math.min(cat.getMaxHealth(), cat.getMaxHealth() * ratio));
        }

        setModifier(cat, Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER,
                "Create Meowchanics attack",
                attackDamage(attack) - VANILLA_CAT_ATTACK_DAMAGE,
                AttributeModifier.Operation.ADDITION);
        setModifier(cat, Attributes.ARMOR, ARMOR_MODIFIER,
                "Create Meowchanics armor", armor(stamina),
                AttributeModifier.Operation.ADDITION);
        setModifier(cat, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_MODIFIER,
                "Create Meowchanics armor toughness", armorToughness(stamina),
                AttributeModifier.Operation.ADDITION);
        setModifier(cat, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER,
                "Create Meowchanics movement speed", movementMultiplier(speed) - 1.0D,
                AttributeModifier.Operation.MULTIPLY_BASE);
        int longFurLevel = traits.level(CatTrait.LONG_FUR);
        setModifier(cat, Attributes.FOLLOW_RANGE, FOLLOW_RANGE_MODIFIER,
                "Create Meowchanics long fur vision",
                longFurLevel <= 0 ? 0.0D
                        : -CatTrait.LONG_FUR.longFurVisionPenalty(longFurLevel),
                AttributeModifier.Operation.ADDITION);
    }

    public static double maximumHealth(int effectiveHealth) {
        return 10.0D + 0.4D * nonNegative(effectiveHealth);
    }

    public static double attackDamage(int effectiveAttack) {
        return 2.0D + 0.08D * nonNegative(effectiveAttack);
    }

    public static double armor(int effectiveStamina) {
        return 2.0D + 0.16D * nonNegative(effectiveStamina);
    }

    public static double armorToughness(int effectiveStamina) {
        return 0.05D * nonNegative(effectiveStamina);
    }

    public static double movementMultiplier(int effectiveSpeed) {
        return 0.75D + 0.005D * nonNegative(effectiveSpeed);
    }

    public static int attackIntervalTicks(int effectiveSpeed) {
        return Mth.clamp((int) Math.round(24.0D
                - 0.12D * nonNegative(effectiveSpeed)), 1, 24);
    }

    public static int attackIntervalTicks(Cat cat) {
        return attackIntervalTicks(effectiveValue(cat, CatStat.SPEED));
    }

    public static double trainingMultiplier(int effectiveIntelligence) {
        return 0.6D + 0.009D * nonNegative(effectiveIntelligence);
    }

    /** Future training systems must use this entry point rather than raw NBT. */
    public static double trainingMultiplier(Cat cat) {
        return trainingMultiplier(effectiveValue(cat, CatStat.INTELLIGENCE));
    }

    public static double criticalChance(int effectiveLuck) {
        return Mth.clamp(0.02D + 0.0018D * nonNegative(effectiveLuck), 0.0D, 1.0D);
    }

    /** Converts the displayed Luck scale into Create/vanilla loot-table luck. */
    public static float fishingLootLuck(Cat cat) {
        return Mth.clamp(effectiveValue(cat, CatStat.LUCK) / 50.0F,
                0.0F, 5.0F);
    }

    public static boolean rollCriticalHit(Cat cat) {
        return cat.getRandom().nextDouble()
                < criticalChance(effectiveValue(cat, CatStat.LUCK));
    }

    public static float criticalDamage(float ordinaryDamage) {
        return (float) (ordinaryDamage * CRITICAL_DAMAGE_MULTIPLIER);
    }

    private static int nonNegative(int value) {
        return Math.max(0, value);
    }

    private static TraitContext context(Cat cat) {
        boolean activeBody = !CatPoseData.isPancake(cat);
        CatOutfitType outfit = CatClothesData.getOutfit(cat);
        var owner = cat.getOwner();
        boolean ownerInDanger = owner != null && owner.isAlive()
                && owner.getHealth() <= owner.getMaxHealth() * 0.5F;
        return new TraitContext(
                activeBody && CatTraitEffects.isNight(cat.level()),
                activeBody && CatTraitEffects.isDay(cat.level()),
                activeBody && CatTraitEffects.isBristlingRageActive(cat),
                activeBody && outfit == CatOutfitType.FIRE,
                activeBody && outfit == CatOutfitType.FISHING,
                activeBody && ownerInDanger,
                activeBody && cat.isInWaterOrRain(),
                activeBody && cat.getHealth() >= cat.getMaxHealth() - 0.001F,
                activeBody && (cat.isInSittingPose() || cat.isPassenger()),
                activeBody && CatTraitEffects.isTimidOutnumbered(cat));
    }

    private record TraitContext(boolean night, boolean day, boolean bristlingRage,
                                boolean blazingForm, boolean fishing,
                                boolean protectiveInstinct, boolean wet,
                                boolean fullHealth, boolean sitting,
                                boolean timid) {
        private static TraitContext onlyNight(boolean night) {
            return new TraitContext(night, false, false, false, false,
                    false, false, false, false, false);
        }

        private static TraitContext onlyTime(boolean night, boolean day) {
            return new TraitContext(night, day, false, false, false,
                    false, false, false, false, false);
        }
    }

    private static boolean setModifier(Cat cat, Attribute attribute, UUID id,
                                       String name, double amount,
                                       AttributeModifier.Operation operation) {
        AttributeInstance instance = cat.getAttribute(attribute);
        if (instance == null) return false;
        AttributeModifier existing = instance.getModifier(id);
        if (amount == 0.0D) {
            if (existing == null) return false;
            instance.removeModifier(existing);
            return true;
        }
        if (existing != null && Double.compare(existing.getAmount(), amount) == 0
                && existing.getOperation() == operation) return false;
        if (existing != null) instance.removeModifier(existing);
        // These modifiers are deterministic projections of saved genes. Saving
        // them prevents max-health clamping while a cat entity is loading; the
        // stable IDs let this method safely replace stale formula revisions.
        instance.addPermanentModifier(new AttributeModifier(id, name, amount, operation));
        return true;
    }

    private CatAttributeEffects() {}
}
