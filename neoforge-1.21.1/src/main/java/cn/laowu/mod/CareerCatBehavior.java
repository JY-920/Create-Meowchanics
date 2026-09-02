package cn.laowu.mod;

import cn.laowu.mod.entity.FishingRodProjectile;
import cn.laowu.mod.entity.MechanicalLaserProjectile;
import cn.laowu.mod.entity.HoneyMissileProjectile;
import cn.laowu.mod.entity.LogisticsSupportProjectile;
import cn.laowu.mod.entity.DynamiteProjectile;
import cn.laowu.mod.mixin.BlazeBurnerBlockEntityAccessor;
import cn.laowu.mod.genetics.CatAttributeEffects;
import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitData;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.foundation.fluid.FluidHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** Server-side attributes and jobs supplied by the cat career outfits. */
public final class CareerCatBehavior {
    public static final int FISHING_LUCK_BONUS = 10;
    public static final int FIRE_STAMINA_BONUS = 10;
    public static final int MECHANICAL_ATTACK_BONUS = 10;
    public static final int HONEY_SPEED_BONUS = 10;
    public static final int FLIGHT_ATTACK_BONUS = 10;
    public static final int TRANSPORT_SPEED_BONUS = 10;
    public static final int DYNAMITE_ATTACK_BONUS = 10;
    private static final double MAX_OWNER_DISTANCE_SQR = 32.0D * 32.0D;
    private static final double RANGED_MAX_DISTANCE = 12.0D;
    private static final double MECHANICAL_LASER_RANGE = 16.0D;
    private static final double HONEY_MISSILE_RANGE = 12.0D;
    private static final double DYNAMITE_RANGE = 8.0D;
    private static final double FIRE_BREATH_RANGE = 4.0D;
    private static final int COMPETENT_INTELLIGENCE = 40;
    private static final int TACTICAL_INTELLIGENCE = 80;
    private static final int ALLY_GUARD_INTELLIGENCE = 60;

    private static final net.minecraft.resources.ResourceLocation HEALTH_MODIFIER_ID =
            LaoWuMod.id("career_cat_health");
    private static final net.minecraft.resources.ResourceLocation ARMOR_MODIFIER_ID =
            LaoWuMod.id("career_cat_armor");
    private static final net.minecraft.resources.ResourceLocation TOUGHNESS_MODIFIER_ID =
            LaoWuMod.id("career_cat_toughness");
    private static final net.minecraft.resources.ResourceLocation ATTACK_MODIFIER_ID =
            LaoWuMod.id("career_cat_attack");
    private static final net.minecraft.resources.ResourceLocation KNOCKBACK_RESISTANCE_MODIFIER_ID =
            LaoWuMod.id("career_cat_knockback_resistance");

    private static final String NEXT_FISH_TAG = "LaoWuCareerNextFish";
    private static final String NEXT_WATER_SCAN_TAG = "LaoWuCareerNextWaterScan";
    private static final String WATER_ROOT_TAG = "LaoWuCareerWaterRoot";
    private static final String WATER_SIZE_TAG = "LaoWuCareerWaterSize";
    private static final String NEXT_HONEY_TAG = "LaoWuCareerNextHoney";
    private static final String NEXT_LOGISTICS_SUPPORT_TAG =
            "LaoWuCareerNextLogisticsSupport";

    private static final int WATER_SCAN_INTERVAL = 20 * 10;
    private static final int MAX_WATER_BLOCKS = 300;
    private static final int WATER_SEARCH_RANGE = 32;
    private static final int HONEY_INTERVAL = 20 * 10;
    private static final int SUPERHEAT_DURATION = 20 * 5;
    private static final int LOGISTICS_SUPPORT_DURATION = 20 * 10;
    private static final double LOGISTICS_MIN_SEARCH_RANGE = 8.0D;
    private static final double LOGISTICS_MAX_SEARCH_RANGE = 16.0D;
    private static final double LOGISTICS_MIN_CAST_RANGE = 6.0D;
    private static final double LOGISTICS_MAX_CAST_RANGE = 10.0D;
    private static final List<Holder<MobEffect>> LOGISTICS_SUPPORT_EFFECTS = List.of(
            MobEffects.DAMAGE_BOOST,
            MobEffects.MOVEMENT_SPEED,
            MobEffects.DAMAGE_RESISTANCE,
            MobEffects.REGENERATION,
            MobEffects.FIRE_RESISTANCE);

    /** Transient bookkeeping avoids serialising modifiers or rescanning goal lists. */
    private static final Map<Cat, CatOutfitType> APPLIED_OUTFITS = new WeakHashMap<>();
    private static final Set<Cat> COMBAT_GOALS_INSTALLED =
            Collections.newSetFromMap(new WeakHashMap<>());

    public static void tick(Cat cat) {
        if (!(cat.level() instanceof ServerLevel level)) return;

        CatOutfitType outfit = CatClothesData.getOutfit(cat);
        if (APPLIED_OUTFITS.get(cat) != outfit) {
            AttributeInstance healthAttribute = cat.getAttribute(Attributes.MAX_HEALTH);
            boolean newlyGranted = outfit != CatOutfitType.NONE && healthAttribute != null
                    && healthAttribute.getModifier(HEALTH_MODIFIER_ID) == null;
            applyAttributes(cat, outfit, newlyGranted);
            APPLIED_OUTFITS.put(cat, outfit);
        }
        if (outfit == CatOutfitType.NONE || CatPoseData.isPancake(cat)) return;

        ensureCareerCombat(cat);
        if (outfit == CatOutfitType.TRANSPORT) {
            // Logistics cats are pure support. Clear any stale target retained
            // from another outfit or a vanilla reaction without interrupting
            // their ordinary idle/follow navigation when no target exists.
            if (cat.getTarget() != null) {
                cat.setTarget(null);
                cat.getNavigation().stop();
            }
        } else {
            tickCareerCombat(level, cat);
        }

        switch (outfit) {
            case FISHING -> tickFishing(level, cat);
            case FIRE -> tickFire(cat);
            case HONEY -> tickHoney(level, cat);
            default -> {
                // Flight combat and logistics support are goal-driven.
            }
        }
    }

    /** Called on equipment changes so health and armour update before the next entity tick. */
    public static void onOutfitChanged(Cat cat, boolean preserveMissingHealth) {
        CatOutfitType outfit = CatClothesData.getOutfit(cat);
        applyAttributes(cat, outfit, preserveMissingHealth);
        CatAttributeEffects.refresh(cat);
        APPLIED_OUTFITS.put(cat, outfit);
        cat.setTarget(null);
        cat.getNavigation().stop();
    }

    private static void applyAttributes(Cat cat, CatOutfitType outfit,
                                        boolean preserveMissingHealth) {
        float oldMax = cat.getMaxHealth();
        float oldHealth = cat.getHealth();
        CareerProfile profile = profile(outfit);

        setModifier(cat, Attributes.MAX_HEALTH, HEALTH_MODIFIER_ID,
                "Lao Wu career cat health", profile.healthBonus(),
                AttributeModifier.Operation.ADD_VALUE);
        setModifier(cat, Attributes.ARMOR, ARMOR_MODIFIER_ID,
                "Lao Wu career cat armor", profile.armorBonus(),
                AttributeModifier.Operation.ADD_VALUE);
        setModifier(cat, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_MODIFIER_ID,
                "Lao Wu career cat toughness", profile.toughnessBonus(),
                AttributeModifier.Operation.ADD_VALUE);
        setModifier(cat, Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER_ID,
                "Lao Wu career damage multiplier",
                profile.damageMultiplier() - 1.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(cat, Attributes.KNOCKBACK_RESISTANCE,
                KNOCKBACK_RESISTANCE_MODIFIER_ID,
                "Lao Wu melee career knockback resistance",
                isMeleeOutfit(outfit) ? 1.0D : 0.0D,
                AttributeModifier.Operation.ADD_VALUE);

        float newMax = cat.getMaxHealth();
        if (preserveMissingHealth && newMax > oldMax) {
            // Equipping adds usable health without also erasing existing wounds.
            cat.setHealth(Math.min(newMax, oldHealth + newMax - oldMax));
        } else if (oldHealth > newMax) {
            cat.setHealth(newMax);
        }
    }

    private static void setModifier(Cat cat, Holder<Attribute> attribute,
                                    net.minecraft.resources.ResourceLocation id,
                                    String name, double amount,
                                    AttributeModifier.Operation operation) {
        AttributeInstance instance = cat.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier current = instance.getModifier(id);
        if (current != null && Double.compare(current.amount(), amount) == 0
                && current.operation() == operation) return;
        if (current != null) instance.removeModifier(id);
        if (amount != 0.0D) {
            // Career equipment is persistent NBT state, so save its stable-ID
            // modifiers with the cat as well. This prevents health from being
            // clamped to the vanilla maximum while an equipped cat is loading.
            instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static CareerProfile profile(CatOutfitType outfit) {
        return switch (outfit) {
            case TERMINATOR -> new CareerProfile(20.0D, 6.0D, 3.0D,
                    0.75D, 24.0D, 0.12D, 10);
            case FISHING -> new CareerProfile(20.0D, 5.0D, 2.0D,
                    0.55D, 36.0D, 0.10D, 20);
            case FLIGHT -> new CareerProfile(12.0D, 3.0D, 1.0D,
                    1.25D, 36.0D, 0.10D, 20);
            case FIRE -> new CareerProfile(40.0D, 10.0D, 4.0D,
                    0.60D, 14.0D, 0.08D, 5);
            case HONEY -> new CareerProfile(24.0D, 5.0D, 2.0D,
                    1.05D, 42.0D, 0.10D, 24);
            case TRANSPORT -> new CareerProfile(30.0D, 6.0D, 3.0D,
                    0.00D, 34.0D, 0.08D, 20);
            case DYNAMITE -> new CareerProfile(24.0D, 5.0D, 2.0D,
                    1.35D, 56.0D, 0.12D, 38);
            case NONE -> new CareerProfile(0.0D, 0.0D, 0.0D,
                    1.00D, 24.0D, 0.12D, 8);
        };
    }

    private static int careerAttackIntervalTicks(Cat cat) {
        int speed = Math.max(0, CatAttributeEffects.effectiveValue(cat, CatStat.SPEED));
        return careerAttackIntervalTicks(CatClothesData.getOutfit(cat), speed);
    }

    private static int careerAttackIntervalTicks(CatOutfitType outfit, int speed) {
        CareerProfile profile = profile(outfit);
        return Mth.clamp((int) Math.round(profile.intervalBase()
                        - profile.intervalPerSpeed() * speed),
                profile.minimumInterval(), 60);
    }

    /** Exact values used by the suit's Shift comparison tooltip. */
    public static CareerSnapshot snapshot(CatOutfitType outfit, int baseAttributeValue) {
        int base = Math.max(0, baseAttributeValue);
        int attack = base + switch (outfit) {
            case TERMINATOR -> MECHANICAL_ATTACK_BONUS;
            case FLIGHT -> FLIGHT_ATTACK_BONUS;
            case DYNAMITE -> DYNAMITE_ATTACK_BONUS;
            default -> 0;
        };
        int stamina = base + (outfit == CatOutfitType.FIRE
                ? FIRE_STAMINA_BONUS : 0);
        int speed = base + (outfit == CatOutfitType.HONEY
                ? HONEY_SPEED_BONUS
                : outfit == CatOutfitType.TRANSPORT ? TRANSPORT_SPEED_BONUS : 0);
        return snapshotEffective(outfit, base, attack, speed, stamina);
    }

    /**
     * Exact live conversion used by attribute-panel hover help. The supplied
     * values have already absorbed traits and outfit-specific attribute
     * bonuses, so this method only applies the career's equipment statistics
     * and combat coefficients.
     */
    public static CareerSnapshot snapshotEffective(CatOutfitType outfit,
                                                   int health, int attack,
                                                   int speed, int stamina) {
        CareerProfile profile = profile(outfit);
        boolean attacks = outfit != CatOutfitType.TRANSPORT && outfit != CatOutfitType.NONE;
        return new CareerSnapshot(
                CatAttributeEffects.maximumHealth(health) + profile.healthBonus(),
                CatAttributeEffects.armor(stamina) + profile.armorBonus(),
                CatAttributeEffects.armorToughness(stamina) + profile.toughnessBonus(),
                attacks ? CatAttributeEffects.attackDamage(attack)
                        * profile.damageMultiplier() : 0.0D,
                attacks ? careerAttackIntervalTicks(outfit, speed) : 0,
                attacks);
    }

    public record CareerSnapshot(double health, double armor, double toughness,
                                 double attackDamage, int attackIntervalTicks,
                                 boolean attacks) {
    }

    private record CareerProfile(double healthBonus, double armorBonus,
                                 double toughnessBonus, double damageMultiplier,
                                 double intervalBase, double intervalPerSpeed,
                                 int minimumInterval) {
    }

    private static void ensureCareerCombat(Cat cat) {
        if (!COMBAT_GOALS_INSTALLED.add(cat)) return;
        cat.goalSelector.addGoal(0, new DynamiteCatLastStand.ControlGoal(cat));
        cat.goalSelector.addGoal(3, new LogisticsSupportGoal(cat));
        cat.goalSelector.addGoal(4, new CareerMeleeGoal(cat));
        cat.goalSelector.addGoal(4, new MechanicalLaserGoal(cat));
        cat.goalSelector.addGoal(4, new HoneyMissileGoal(cat));
        cat.goalSelector.addGoal(4, new DynamiteThrowGoal(cat));
        cat.goalSelector.addGoal(4, new FishingRangedGoal(cat));
        cat.goalSelector.addGoal(4, new FlightDiveGoal(cat));
        cat.targetSelector.addGoal(1, new CareerOwnerHurtByGoal(cat));
        cat.targetSelector.addGoal(2, new CareerOwnerHurtTargetGoal(cat));
        cat.targetSelector.addGoal(3, new CareerHurtByGoal(cat));
    }

    private static void tickCareerCombat(ServerLevel level, Cat cat) {
        if (isResting(cat)) {
            if (cat.getTarget() != null) cat.setTarget(null);
            cat.getNavigation().stop();
            return;
        }

        LivingEntity target = cat.getTarget();
        if (target != null && !canTarget(cat, target)) {
            cat.setTarget(null);
            cat.getNavigation().stop();
            target = null;
        }
        int longFurLevel = CatTraitData.ensure(cat).level(CatTrait.LONG_FUR);
        if (target != null && longFurLevel > 0) {
            double sight = Math.max(1.0D,
                    cat.getAttributeValue(Attributes.FOLLOW_RANGE));
            if (cat.distanceToSqr(target) > sight * sight) {
                cat.setTarget(null);
                cat.getNavigation().stop();
                target = null;
            }
        }
        if (cat.getLastHurtByMob() != null
                && !canTarget(cat, cat.getLastHurtByMob())) {
            cat.setLastHurtByMob(null);
        }

        LivingEntity owner = cat.getOwner();
        boolean resting = cat.isInSittingPose() || findSeat(cat) != null;
        boolean activelyFighting = target != null && target.isAlive();
        if (owner == null || resting || !activelyFighting
                || cat.distanceToSqr(owner) <= MAX_OWNER_DISTANCE_SQR) return;

        // The 32-block limit is a combat leash, not an idle follow rule. Cats
        // stationed far away (including on Seats) stay there; only an active
        // chase can trigger disengagement and a return to the owner.
        cat.setTarget(null);
        cat.setLastHurtByMob(null);
        cat.getNavigation().stop();
        teleportNearOwner(level, cat, owner);
    }

    private static void teleportNearOwner(ServerLevel level, Cat cat, LivingEntity owner) {
        BlockPos origin = owner.blockPosition();
        for (int attempt = 0; attempt < 12; attempt++) {
            int offsetX = Mth.nextInt(cat.getRandom(), -2, 2);
            int offsetY = Mth.nextInt(cat.getRandom(), -1, 1);
            int offsetZ = Mth.nextInt(cat.getRandom(), -2, 2);
            BlockPos feet = origin.offset(offsetX, offsetY, offsetZ);
            BlockPos floor = feet.below();
            if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) continue;

            double x = feet.getX() + 0.5D;
            double y = feet.getY();
            double z = feet.getZ() + 0.5D;
            AABB destination = cat.getBoundingBox().move(
                    x - cat.getX(), y - cat.getY(), z - cat.getZ());
            if (!level.noCollision(cat, destination) || level.containsAnyLiquid(destination)) continue;

            cat.stopRiding();
            cat.teleportTo(x, y, z);
            cat.setDeltaMovement(Vec3.ZERO);
            cat.fallDistance = 0.0F;
            return;
        }

        // The owner's exact position is a last-resort destination for unusual
        // terrain; the distance guard is more important than preserving a
        // hostile target indefinitely.
        cat.stopRiding();
        cat.teleportTo(owner.getX(), owner.getY(), owner.getZ());
        cat.setDeltaMovement(Vec3.ZERO);
        cat.fallDistance = 0.0F;
    }

    /**
     * Performs the fire career's close-range cone attack. It uses the cat's
     * already career-scaled attack attribute, so combat-power traits and the
     * displayed suit formula remain the single source of damage truth.
     */
    private static void applyFireBreathDamage(Cat cat, LivingEntity primaryTarget) {
        if (!(cat.level() instanceof ServerLevel level)) return;

        Vec3 origin = new Vec3(cat.getX(), cat.getEyeY() - 0.08D, cat.getZ());
        Vec3 direction = primaryTarget.getEyePosition().subtract(origin);
        if (direction.lengthSqr() < 1.0E-5D) direction = cat.getLookAngle();
        direction = direction.normalize();
        Vec3 end = origin.add(direction.scale(FIRE_BREATH_RANGE));
        AABB affectedArea = new AABB(origin, end).inflate(1.4D);
        float damage = (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE);

        for (LivingEntity candidate : level.getEntitiesOfClass(
                LivingEntity.class, affectedArea,
                entity -> canTarget(cat, entity) && cat.getSensing().hasLineOfSight(entity))) {
            Vec3 relative = candidate.getBoundingBox().getCenter().subtract(origin);
            double forward = relative.dot(direction);
            if (forward < 0.0D
                    || forward > FIRE_BREATH_RANGE + candidate.getBbWidth() * 0.5D) continue;

            double lateralSqr = Math.max(0.0D,
                    relative.lengthSqr() - forward * forward);
            double coneRadius = 0.42D + forward * 0.22D
                    + candidate.getBbWidth() * 0.5D;
            if (lateralSqr > coneRadius * coneRadius) continue;

            if (candidate.hurt(level.damageSources().mobAttack(cat), damage)) {
                candidate.igniteForSeconds(3);
            }
        }

        level.playSound(null, cat.blockPosition(), SoundEvents.BLAZE_SHOOT,
                SoundSource.NEUTRAL, 0.75F,
                1.15F + cat.getRandom().nextFloat() * 0.15F);
    }

    /**
     * Keeps the flame visually continuous while a target is in reach. This is
     * deliberately separate from damage: it sends particles every other tick,
     * but never performs an entity query or bypasses the career attack timer.
     */
    private static void emitFireBreathParticles(Cat cat, LivingEntity primaryTarget) {
        if (!(cat.level() instanceof ServerLevel level) || cat.tickCount % 2 != 0) return;

        Vec3 origin = new Vec3(cat.getX(), cat.getEyeY() - 0.08D, cat.getZ());
        Vec3 direction = primaryTarget.getEyePosition().subtract(origin);
        if (direction.lengthSqr() < 1.0E-5D) direction = cat.getLookAngle();
        direction = direction.normalize();
        Vec3 end = origin.add(direction.scale(FIRE_BREATH_RANGE));
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-5D) side = new Vec3(1.0D, 0.0D, 0.0D);
        side = side.normalize();
        Vec3 up = side.cross(direction).normalize();
        for (int step = 1; step <= 4; step++) {
            double distance = FIRE_BREATH_RANGE * step / 4.0D;
            double spread = 0.04D + distance * 0.09D;
            Vec3 point = origin.add(direction.scale(distance))
                    .add(side.scale((cat.getRandom().nextDouble() * 2.0D - 1.0D) * spread))
                    .add(up.scale((cat.getRandom().nextDouble() * 2.0D - 1.0D) * spread));
            level.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z,
                    3, spread * 0.45D, spread * 0.28D, spread * 0.45D, 0.018D);
        }
        if (cat.tickCount % 4 == 0) {
            Vec3 moltenCenter = origin.add(direction.scale(FIRE_BREATH_RANGE * 0.72D));
            level.sendParticles(ParticleTypes.LAVA,
                    moltenCenter.x, moltenCenter.y, moltenCenter.z,
                    2, 0.35D, 0.22D, 0.35D, 0.015D);
            level.sendParticles(ParticleTypes.SMOKE, end.x, end.y, end.z,
                    3, 0.30D, 0.16D, 0.30D, 0.02D);
        }
    }

    public static boolean isForbiddenTerminatorTarget(LivingEntity target) {
        return isForbiddenCareerTarget(target);
    }

    private static boolean isForbiddenCareerTarget(LivingEntity target) {
        return target instanceof Player || target instanceof Cat;
    }

    private static boolean canFight(Cat cat) {
        CatOutfitType outfit = CatClothesData.getOutfit(cat);
        return cat.isTame() && cat.isAlive() && !CatPoseData.isPancake(cat)
                && !DynamiteCatLastStand.isActive(cat)
                && outfit != CatOutfitType.NONE && outfit != CatOutfitType.TRANSPORT
                && !isResting(cat);
    }

    public static boolean canParticipateInCombat(Cat cat) {
        return canFight(cat);
    }

    private static boolean canTarget(Cat cat, LivingEntity target) {
        return target != null && target.isAlive() && target != cat.getOwner()
                && !isForbiddenCareerTarget(target) && cat.canAttack(target);
    }

    private static boolean isRangedOutfit(CatOutfitType outfit) {
        return outfit == CatOutfitType.FISHING || outfit == CatOutfitType.TERMINATOR
                || outfit == CatOutfitType.HONEY || outfit == CatOutfitType.DYNAMITE;
    }

    private static boolean isMeleeOutfit(CatOutfitType outfit) {
        return outfit == CatOutfitType.FIRE || outfit == CatOutfitType.FLIGHT;
    }

    private static boolean isResting(Cat cat) {
        return cat.isOrderedToSit() || cat.isInSittingPose() || findSeat(cat) != null;
    }

    public static boolean isCombatResting(Cat cat) {
        return isResting(cat);
    }

    /**
     * Event-driven ally protection: scan once when a ranged cat is actually
     * hurt instead of polling every nearby melee cat each tick.
     */
    public static void alertMeleeProtectors(Cat rangedCat, Entity source) {
        if (!(rangedCat.level() instanceof ServerLevel level)
                || !(source instanceof LivingEntity attacker)
                || !rangedCat.isTame() || isResting(rangedCat)) return;
        CatOutfitType protectedOutfit = CatClothesData.getOutfit(rangedCat);
        if (!isRangedOutfit(protectedOutfit)
                && protectedOutfit != CatOutfitType.TRANSPORT) return;
        UUID ownerId = rangedCat.getOwnerUUID();
        if (ownerId == null) return;

        for (Cat defender : level.getEntitiesOfClass(Cat.class,
                rangedCat.getBoundingBox().inflate(16.0D), candidate -> candidate != rangedCat
                        && ownerId.equals(candidate.getOwnerUUID())
                        && canFight(candidate)
                        && isMeleeOutfit(CatClothesData.getOutfit(candidate))
                        && CatAttributeEffects.effectiveValue(candidate, CatStat.INTELLIGENCE)
                        >= ALLY_GUARD_INTELLIGENCE)) {
            if (canTarget(defender, attacker)) defender.setTarget(attacker);
        }
    }

    private static void tickFishing(ServerLevel level, Cat cat) {
        // Capabilities and the cached water calculation need no per-tick polling.
        if (cat.tickCount % 10 != 0) return;
        BlockPos seat = findSeat(cat);
        if (seat == null) {
            resetFishing(cat);
            return;
        }

        BlockPos waterRoot = findAdjacentWaterSource(level, seat.below());
        ContainerTarget container = findAdjacentContainer(level, seat);
        if (waterRoot == null || container == null) {
            resetFishing(cat);
            return;
        }

        CompoundTag data = cat.getPersistentData();
        long now = level.getGameTime();
        long root = waterRoot.asLong();
        if (data.getLong(WATER_ROOT_TAG) != root
                || now >= data.getLong(NEXT_WATER_SCAN_TAG)) {
            int waterBlocks = countConnectedWater(level, waterRoot);
            data.putLong(WATER_ROOT_TAG, root);
            data.putInt(WATER_SIZE_TAG, waterBlocks);
            data.putLong(NEXT_WATER_SCAN_TAG, now + WATER_SCAN_INTERVAL);
            if (waterBlocks <= 0) {
                resetFishing(cat);
                return;
            }
        }

        int waterBlocks = Mth.clamp(data.getInt(WATER_SIZE_TAG), 1, MAX_WATER_BLOCKS);
        if (!data.contains(NEXT_FISH_TAG, Tag.TAG_LONG)) {
            data.putLong(NEXT_FISH_TAG, now + nextFishingDelay(cat, waterBlocks));
            return;
        }
        if (now < data.getLong(NEXT_FISH_TAG)) return;

        catchFish(level, cat, waterRoot, container);
        data.putLong(NEXT_FISH_TAG, now + nextFishingDelay(cat, waterBlocks));
    }

    private static int nextFishingDelay(Cat cat, int waterBlocks) {
        double progress = (waterBlocks - 1.0D) / (MAX_WATER_BLOCKS - 1.0D);
        double speedMultiplier = 0.1D + 1.4D * Mth.clamp(progress, 0.0D, 1.0D);
        // Vanilla FishingHook selects a 100-600 tick lure delay. Scaling that
        // same range gives the requested 0.1x through 1.5x fishing rates.
        int vanillaDelay = Mth.nextInt(cat.getRandom(), 100, 600);
        return Math.max(10, Mth.ceil(vanillaDelay / speedMultiplier));
    }

    private static void catchFish(ServerLevel level, Cat cat, BlockPos water,
                                  ContainerTarget target) {
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(water))
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
                .withParameter(LootContextParams.THIS_ENTITY, cat)
                .withLuck(CatAttributeEffects.fishingLootLuck(cat))
                .create(LootContextParamSets.FISHING);
        var table = level.getServer().reloadableRegistries()
                .getLootTable(BuiltInLootTables.FISHING);
        for (ItemStack caught : table.getRandomItems(params)) {
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(
                    target.handler(), caught.copy(), false);
            if (!remainder.isEmpty()) {
                Containers.dropItemStack(level,
                        target.pos().getX() + 0.5D, target.pos().getY() + 1.0D,
                        target.pos().getZ() + 0.5D, remainder);
            }
        }
        level.sendParticles(ParticleTypes.SPLASH,
                water.getX() + 0.5D, water.getY() + 1.0D, water.getZ() + 0.5D,
                8, 0.32D, 0.04D, 0.32D, 0.12D);
        level.sendParticles(ParticleTypes.BUBBLE,
                water.getX() + 0.5D, water.getY() + 0.85D, water.getZ() + 0.5D,
                6, 0.3D, 0.05D, 0.3D, 0.04D);
        level.playSound(null, water, SoundEvents.FISHING_BOBBER_SPLASH,
                SoundSource.NEUTRAL, 0.8F, 1.05F);
    }

    private static void resetFishing(Cat cat) {
        cat.getPersistentData().remove(NEXT_FISH_TAG);
    }

    private static BlockPos findAdjacentWaterSource(ServerLevel level, BlockPos support) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = support.relative(direction);
            if (!level.hasChunkAt(candidate)) continue;
            var fluid = level.getFluidState(candidate);
            if (fluid.isSource() && fluid.is(FluidTags.WATER)) return candidate.immutable();
        }
        return null;
    }

    /**
     * Bounded version of Create's hose-pulley flood search: six directions,
     * no downward expansion, still/flowing fluid normalisation, unloaded-chunk
     * avoidance and a squared range limit. The requested 300-block cap makes
     * the scan substantially smaller than a hose pulley search.
     */
    private static int countConnectedWater(ServerLevel level, BlockPos root) {
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        frontier.add(root.immutable());
        int count = 0;
        int rangeSqr = WATER_SEARCH_RANGE * WATER_SEARCH_RANGE;

        while (!frontier.isEmpty() && count < MAX_WATER_BLOCKS) {
            BlockPos pos = frontier.removeFirst();
            if (!visited.add(pos) || root.distSqr(pos) > rangeSqr || !level.hasChunkAt(pos)) {
                continue;
            }
            var fluidState = level.getFluidState(pos);
            if (fluidState.isEmpty()
                    || !FluidHelper.convertToStill(fluidState.getType()).isSame(Fluids.WATER)) {
                continue;
            }
            count++;
            for (Direction direction : Direction.values()) {
                if (direction == Direction.DOWN) continue;
                BlockPos next = pos.relative(direction);
                if (!visited.contains(next) && root.distSqr(next) <= rangeSqr
                        && level.hasChunkAt(next)) {
                    frontier.addLast(next.immutable());
                }
            }
        }
        return count;
    }

    private static ContainerTarget findAdjacentContainer(ServerLevel level, BlockPos seat) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            ContainerTarget target = containerAt(level, seat.relative(direction),
                    direction.getOpposite());
            if (target != null) return target;
        }
        // Also accept a container touching the supporting block, useful for
        // low hoppers and compact shore-side builds.
        BlockPos support = seat.below();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            ContainerTarget target = containerAt(level, support.relative(direction),
                    direction.getOpposite());
            if (target != null) return target;
        }
        return null;
    }

    private static ContainerTarget containerAt(ServerLevel level, BlockPos pos, Direction side) {
        if (!level.hasChunkAt(pos)) return null;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return null;
        IItemHandler handler = Capabilities.ItemHandler.BLOCK.getCapability(
                level, pos, level.getBlockState(pos), blockEntity, side);
        return handler == null ? null : new ContainerTarget(pos.immutable(), handler);
    }

    private static void tickFire(Cat cat) {
        if (cat.tickCount % 10 != 0) return;
        BlockPos seat = findSeat(cat);
        if (seat == null) return;
        var traits = CatTraitData.ensure(cat);
        boolean sustainedSuperheat = traits.has(CatTrait.SUPERHEAT_GENE);
        int level = traits.level(CatTrait.BLAZING_FORM);
        boolean superheat = level > 0 && cat.tickCount % (20 * 10) == 0
                && cat.getRandom().nextInt(100)
                < CatTrait.BLAZING_FORM.blazingSuperheatChance(level);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!(cat.level().getBlockEntity(seat.relative(direction))
                    instanceof BlazeBurnerBlockEntity burner) || burner.isCreative()) continue;
            if (sustainedSuperheat || superheat) superheatBurner(burner);
            else keepBurnerKindled(burner);
        }
    }

    private static void superheatBurner(BlazeBurnerBlockEntity burner) {
        if (burner.getActiveFuel() == BlazeBurnerBlockEntity.FuelType.SPECIAL
                && burner.getRemainingBurnTime() > 40) return;
        boolean changedHeat = burner.getActiveFuel()
                != BlazeBurnerBlockEntity.FuelType.SPECIAL;
        BlazeBurnerBlockEntityAccessor accessor = (BlazeBurnerBlockEntityAccessor) burner;
        accessor.laowu$setActiveFuel(BlazeBurnerBlockEntity.FuelType.SPECIAL);
        accessor.laowu$setRemainingBurnTime(SUPERHEAT_DURATION);
        burner.setChanged();
        if (changedHeat) burner.updateBlockState();
    }

    private static void keepBurnerKindled(BlazeBurnerBlockEntity burner) {
        if (burner.getActiveFuel() == BlazeBurnerBlockEntity.FuelType.SPECIAL
                || burner.getRemainingBurnTime() > 20) return;
        boolean wasActive = burner.getActiveFuel() != BlazeBurnerBlockEntity.FuelType.NONE
                && burner.getRemainingBurnTime() > 0;
        BlazeBurnerBlockEntityAccessor accessor = (BlazeBurnerBlockEntityAccessor) burner;
        accessor.laowu$setActiveFuel(BlazeBurnerBlockEntity.FuelType.NORMAL);
        accessor.laowu$setRemainingBurnTime(40);
        burner.setChanged();
        if (!wasActive) burner.updateBlockState();
    }

    private static void tickHoney(ServerLevel level, Cat cat) {
        BlockPos seat = findSeat(cat);
        if (seat == null) {
            cat.getPersistentData().remove(NEXT_HONEY_TAG);
            return;
        }
        BlockPos hive = seat.below();
        BlockState state = level.getBlockState(hive);
        if (!(state.getBlock() instanceof BeehiveBlock)) {
            cat.getPersistentData().remove(NEXT_HONEY_TAG);
            return;
        }

        CompoundTag data = cat.getPersistentData();
        long now = level.getGameTime();
        int traitLevel = CatTraitData.ensure(cat).level(CatTrait.BEEBEE_GENE);
        int interval = traitLevel <= 0 ? HONEY_INTERVAL
                : CatTrait.BEEBEE_GENE.beebeeWorkIntervalSeconds(traitLevel) * 20;
        if (!data.contains(NEXT_HONEY_TAG, Tag.TAG_LONG)) {
            data.putLong(NEXT_HONEY_TAG, now + interval);
            return;
        }
        if (data.getLong(NEXT_HONEY_TAG) > now + interval) {
            data.putLong(NEXT_HONEY_TAG, now + interval);
        }
        if (now < data.getLong(NEXT_HONEY_TAG)) return;
        data.putLong(NEXT_HONEY_TAG, now + interval);

        int honey = state.getValue(BeehiveBlock.HONEY_LEVEL);
        if (honey >= BeehiveBlock.MAX_HONEY_LEVELS) return;
        level.setBlock(hive, state.setValue(BeehiveBlock.HONEY_LEVEL, honey + 1),
                Block.UPDATE_ALL);
        level.playSound(null, hive, SoundEvents.BEEHIVE_WORK,
                SoundSource.BLOCKS, 0.6F, 1.1F);
    }

    private static boolean canProvideLogisticsSupport(Cat cat) {
        return cat.isTame() && cat.isAlive() && !CatPoseData.isPancake(cat)
                && CatClothesData.getOutfit(cat) == CatOutfitType.TRANSPORT
                && !isResting(cat) && cat.getOwnerUUID() != null;
    }

    private static int logisticsIntelligence(Cat cat) {
        return Mth.clamp(CatAttributeEffects.effectiveValue(
                cat, CatStat.INTELLIGENCE), 0, 100);
    }

    private static double logisticsSearchRange(int intelligence) {
        return Mth.lerp(intelligence / 100.0D,
                LOGISTICS_MIN_SEARCH_RANGE, LOGISTICS_MAX_SEARCH_RANGE);
    }

    private static double logisticsCastRange(int intelligence) {
        return Mth.lerp(intelligence / 100.0D,
                LOGISTICS_MIN_CAST_RANGE, LOGISTICS_MAX_CAST_RANGE);
    }

    private static double logisticsMoveSpeed(int intelligence) {
        return 0.95D + intelligence * 0.0025D;
    }

    private static int logisticsDecisionInterval(int intelligence) {
        return Mth.clamp(40 - Math.round(intelligence * 0.25F), 15, 40);
    }

    private static int logisticsPathRefreshInterval(int intelligence) {
        return Mth.clamp(12 - intelligence / 20, 7, 12);
    }

    private static int logisticsSupportInterval(int intelligence) {
        // Ten seconds at Intelligence 0, eight at 50, six at 100.
        return Mth.clamp(200 - Math.round(intelligence * 0.8F), 120, 200);
    }

    private static boolean hasAllLogisticsSupportEffects(Cat cat) {
        for (Holder<MobEffect> effect : LOGISTICS_SUPPORT_EFFECTS) {
            if (!cat.hasEffect(effect)) return false;
        }
        return true;
    }

    private static boolean isLogisticsRecipient(Cat supporter, Cat candidate,
                                                double maximumDistanceSqr) {
        if (candidate == supporter || !candidate.isAlive()
                || CatPoseData.isPancake(candidate)
                || supporter.distanceToSqr(candidate) > maximumDistanceSqr
                || hasAllLogisticsSupportEffects(candidate)) return false;
        UUID ownerId = supporter.getOwnerUUID();
        if (ownerId == null || !ownerId.equals(candidate.getOwnerUUID())) return false;
        CatOutfitType outfit = CatClothesData.getOutfit(candidate);
        if (outfit == CatOutfitType.NONE || outfit == CatOutfitType.TRANSPORT) return false;
        LivingEntity enemy = candidate.getTarget();
        return enemy != null && enemy.isAlive();
    }

    private static boolean launchLogisticsSupport(ServerLevel level, Cat cat,
                                                  Cat recipient) {
        // Reservoir-sample only the effects this recipient is missing. This
        // prevents repeated rolls from wasting a delivery and allows all five
        // different effects to coexist without increasing their amplifier.
        Holder<MobEffect> selected = null;
        int missingEffects = 0;
        for (Holder<MobEffect> effect : LOGISTICS_SUPPORT_EFFECTS) {
            if (recipient.hasEffect(effect)) continue;
            missingEffects++;
            if (cat.getRandom().nextInt(missingEffects) == 0) selected = effect;
        }
        if (selected == null) return false;
        LogisticsSupportProjectile projectile = new LogisticsSupportProjectile(
                level, cat, recipient, selected, LOGISTICS_SUPPORT_DURATION);
        projectile.setPos(cat.getX(), cat.getEyeY() - 0.05D, cat.getZ());
        Vec3 aim = recipient.getBoundingBox().getCenter()
                .subtract(projectile.position());
        projectile.shoot(aim.x, aim.y, aim.z, 1.05F, 0.0F);
        level.addFreshEntity(projectile);
        cat.swing(InteractionHand.MAIN_HAND);
        level.playSound(null, cat.blockPosition(), SoundEvents.SNOWBALL_THROW,
                SoundSource.NEUTRAL, 0.65F,
                0.85F + cat.getRandom().nextFloat() * 0.15F);
        return true;
    }

    /**
     * Intelligence-scaled support behaviour. It temporarily outranks vanilla
     * owner following, approaches an unbuffed same-owner career cat that is
     * actively fighting, and stops as soon as it is within casting range.
     */
    private static final class LogisticsSupportGoal extends Goal {
        private final Cat cat;
        private Cat recipient;
        private int nextSearchTick;
        private int nextPathRefresh;

        private LogisticsSupportGoal(Cat cat) {
            this.cat = cat;
            nextSearchTick = cat.tickCount + cat.getRandom().nextInt(20);
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!canProvideLogisticsSupport(cat)) return false;
            if (cat.tickCount < nextSearchTick) return false;

            int intelligence = logisticsIntelligence(cat);
            nextSearchTick = cat.tickCount
                    + logisticsDecisionInterval(intelligence);
            recipient = findRecipient(intelligence);
            return recipient != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (recipient == null || !canProvideLogisticsSupport(cat)) return false;
            double retentionRange = logisticsSearchRange(
                    logisticsIntelligence(cat)) + 2.0D;
            return isLogisticsRecipient(cat, recipient,
                    retentionRange * retentionRange);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            nextPathRefresh = 0;
        }

        @Override
        public void stop() {
            recipient = null;
            cat.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (recipient == null || !(cat.level() instanceof ServerLevel level)) return;
            int intelligence = logisticsIntelligence(cat);
            cat.getLookControl().setLookAt(recipient, 35.0F, 35.0F);

            double castRange = logisticsCastRange(intelligence);
            if (cat.distanceToSqr(recipient) > castRange * castRange) {
                if (--nextPathRefresh <= 0 || cat.getNavigation().isDone()) {
                    nextPathRefresh = logisticsPathRefreshInterval(intelligence);
                    cat.getNavigation().moveTo(recipient,
                            logisticsMoveSpeed(intelligence));
                }
                return;
            }

            cat.getNavigation().stop();
            CompoundTag data = cat.getPersistentData();
            long now = level.getGameTime();
            int interval = logisticsSupportInterval(intelligence);
            long nextSupport = data.contains(NEXT_LOGISTICS_SUPPORT_TAG, Tag.TAG_LONG)
                    ? data.getLong(NEXT_LOGISTICS_SUPPORT_TAG) : now;
            if (nextSupport > now + interval) {
                nextSupport = now + interval;
                data.putLong(NEXT_LOGISTICS_SUPPORT_TAG, nextSupport);
            }
            if (now < nextSupport) return;

            if (launchLogisticsSupport(level, cat, recipient)) {
                data.putLong(NEXT_LOGISTICS_SUPPORT_TAG, now + interval);
            }
            recipient = null;
        }

        private Cat findRecipient(int intelligence) {
            if (!(cat.level() instanceof ServerLevel level)) return null;
            double range = logisticsSearchRange(intelligence);
            double rangeSqr = range * range;
            double intelligenceWeight = intelligence / 100.0D;
            Cat best = null;
            double bestScore = Double.MAX_VALUE;
            for (Cat candidate : level.getEntitiesOfClass(Cat.class,
                    cat.getBoundingBox().inflate(range), candidate ->
                            isLogisticsRecipient(cat, candidate, rangeSqr))) {
                double distanceScore = cat.distanceToSqr(candidate) / rangeSqr;
                double healthScore = candidate.getMaxHealth() <= 0.0F ? 1.0D
                        : candidate.getHealth() / candidate.getMaxHealth();
                // Low intelligence mostly picks the nearest ally. High
                // intelligence increasingly prioritises the most wounded one.
                double urgency = intelligenceWeight * 0.70D;
                double score = distanceScore * (1.0D - urgency)
                        + healthScore * urgency;
                if (score < bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }
            return best;
        }
    }

    public static BlockPos findSeat(Cat cat) {
        if (cat.getVehicle() instanceof SeatEntity seatEntity) {
            BlockPos pos = seatEntity.blockPosition();
            if (cat.level().getBlockState(pos).getBlock() instanceof SeatBlock) {
                return pos.immutable();
            }
        }
        BlockPos[] candidates = {cat.blockPosition(), cat.blockPosition().below(), cat.getOnPos()};
        for (BlockPos pos : candidates) {
            if (cat.level().getBlockState(pos).getBlock() instanceof SeatBlock
                    && (cat.isPassenger() || cat.isInSittingPose())) {
                return pos.immutable();
            }
        }
        return null;
    }

    private record ContainerTarget(BlockPos pos, IItemHandler handler) {
    }

    private static final class CareerMeleeGoal extends MeleeAttackGoal {
        private final Cat cat;
        private int attributeAttackCooldown;

        private CareerMeleeGoal(Cat cat) {
            super(cat, 1.25D, true);
            this.cat = cat;
        }

        @Override
        public void start() {
            super.start();
            attributeAttackCooldown = 0;
        }

        @Override
        public void stop() {
            super.stop();
            attributeAttackCooldown = 0;
        }

        @Override
        public void tick() {
            if (attributeAttackCooldown > 0) attributeAttackCooldown--;
            super.tick();
        }

        @Override
        protected void resetAttackCooldown() {
            attributeAttackCooldown = careerAttackIntervalTicks(cat);
        }

        @Override
        protected boolean isTimeToAttack() {
            return attributeAttackCooldown <= 0;
        }

        @Override
        protected int getTicksUntilNextAttack() {
            return attributeAttackCooldown;
        }

        @Override
        protected int getAttackInterval() {
            return careerAttackIntervalTicks(cat);
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            if (CatClothesData.getOutfit(cat) != CatOutfitType.FIRE) {
                super.checkAndPerformAttack(target);
                return;
            }
            if (cat.distanceToSqr(target) <= FIRE_BREATH_RANGE * FIRE_BREATH_RANGE
                    && cat.getSensing().hasLineOfSight(target)) {
                emitFireBreathParticles(cat, target);
                if (isTimeToAttack()) {
                    resetAttackCooldown();
                    cat.swing(InteractionHand.MAIN_HAND);
                    applyFireBreathDamage(cat, target);
                }
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public boolean canUse() {
            return canFight(cat) && CatClothesData.getOutfit(cat) == CatOutfitType.FIRE
                    && canTarget(cat, cat.getTarget())
                    && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return canFight(cat) && CatClothesData.getOutfit(cat) == CatOutfitType.FIRE
                    && canTarget(cat, cat.getTarget())
                    && super.canContinueToUse();
        }
    }

    /**
     * Low-altitude swoop combat inspired by vanilla Phantom phases. The cat
     * never stages more than roughly three blocks above its target, then dives
     * for one heavy melee hit and pulls up before beginning the next pass.
     */
    private static final class FlightDiveGoal extends Goal {
        private static final double STAGING_HEIGHT = 2.65D;
        private static final int MAX_CLIMB_TICKS = 24;
        private static final int MAX_DIVE_TICKS = 22;
        private static final int RECOVERY_TICKS = 11;
        private static final int EXPECTED_DIVE_TRAVEL_TICKS = 6;

        private final Cat cat;
        private FlightPhase phase = FlightPhase.CLIMB;
        private int phaseTicks;
        private int attackCooldown;
        private boolean orbitClockwise;
        private boolean previousNoGravity;

        private FlightDiveGoal(Cat cat) {
            this.cat = cat;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return canFight(cat)
                    && CatClothesData.getOutfit(cat) == CatOutfitType.FLIGHT
                    && !cat.isInWaterOrBubble()
                    && canTarget(cat, cat.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            previousNoGravity = cat.isNoGravity();
            cat.setNoGravity(true);
            cat.getNavigation().stop();
            phase = FlightPhase.CLIMB;
            phaseTicks = 0;
            attackCooldown = 0;
            orbitClockwise = cat.getRandom().nextBoolean();
        }

        @Override
        public void stop() {
            cat.setNoGravity(previousNoGravity);
            cat.fallDistance = 0.0F;
            Vec3 motion = cat.getDeltaMovement();
            cat.setDeltaMovement(motion.x * 0.45D,
                    Math.min(0.0D, motion.y * 0.35D), motion.z * 0.45D);
            phaseTicks = 0;
            attackCooldown = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = cat.getTarget();
            if (!canTarget(cat, target)) return;
            if (attackCooldown > 0) attackCooldown--;
            phaseTicks++;
            cat.fallDistance = 0.0F;
            cat.getLookControl().setLookAt(target, 50.0F, 50.0F);
            faceTarget(target);

            switch (phase) {
                case CLIMB -> tickClimb(target);
                case DIVE -> tickDive(target);
                case RECOVER -> tickRecover(target);
            }
        }

        private void faceTarget(LivingEntity target) {
            Vec3 aim = target.getEyePosition().subtract(cat.getEyePosition());
            double horizontal = Math.sqrt(aim.x * aim.x + aim.z * aim.z);
            if (horizontal < 1.0E-5D && Math.abs(aim.y) < 1.0E-5D) return;
            float targetYaw = (float) (Mth.atan2(aim.z, aim.x)
                    * (180.0D / Math.PI)) - 90.0F;
            float targetPitch = (float) -(Mth.atan2(aim.y, horizontal)
                    * (180.0D / Math.PI));
            float yaw = Mth.rotLerp(0.55F, cat.getYRot(), targetYaw);
            cat.setYRot(yaw);
            cat.yBodyRot = yaw;
            cat.setYHeadRot(yaw);
            cat.setXRot(Mth.rotLerp(0.45F, cat.getXRot(), targetPitch));
        }

        private void tickClimb(LivingEntity target) {
            Vec3 radial = horizontalAwayFrom(target);
            Vec3 tangent = new Vec3(-radial.z, 0.0D, radial.x)
                    .scale(orbitClockwise ? 1.15D : -1.15D);
            Vec3 staging = target.position().add(radial.scale(2.8D)).add(tangent)
                    .add(0.0D, STAGING_HEIGHT, 0.0D);
            flyToward(staging, 0.48D);

            boolean highEnough = cat.getY() >= target.getY() + 1.65D;
            boolean attackNearlyReady =
                    attackCooldown <= EXPECTED_DIVE_TRAVEL_TICKS;
            if ((phaseTicks >= 12 && highEnough && attackNearlyReady)
                    || phaseTicks >= MAX_CLIMB_TICKS) {
                phase = FlightPhase.DIVE;
                phaseTicks = 0;
                if (cat.level() instanceof ServerLevel level) {
                    level.playSound(null, cat.blockPosition(), SoundEvents.PHANTOM_SWOOP,
                            SoundSource.NEUTRAL, 0.45F,
                            1.35F + cat.getRandom().nextFloat() * 0.12F);
                }
            }
        }

        private void tickDive(LivingEntity target) {
            Vec3 predicted = target.getBoundingBox().getCenter()
                    .add(target.getDeltaMovement().scale(0.30D));
            flyToward(predicted, 0.82D);

            double reach = 0.85D + cat.getBbWidth() * 0.5D
                    + target.getBbWidth() * 0.5D;
            if (cat.distanceToSqr(target) <= reach * reach) {
                if (attackCooldown <= 0) {
                    cat.swing(InteractionHand.MAIN_HAND);
                    cat.doHurtTarget(target);
                    attackCooldown = careerAttackIntervalTicks(cat);
                    beginRecovery();
                } else {
                    // A very close or fast-moving target can be reached before
                    // the displayed attack interval expires. Hold the pass for
                    // the remaining ticks instead of silently wasting it.
                    Vec3 waitPoint = target.position()
                            .add(horizontalAwayFrom(target).scale(1.35D))
                            .add(0.0D, 0.55D, 0.0D);
                    flyToward(waitPoint, 0.34D);
                }
                return;
            }
            if (phaseTicks >= MAX_DIVE_TICKS) beginRecovery();
        }

        private void tickRecover(LivingEntity target) {
            Vec3 away = horizontalAwayFrom(target);
            Vec3 recovery = target.position().add(away.scale(3.2D))
                    .add(0.0D, 2.1D, 0.0D);
            flyToward(recovery, 0.52D);
            if (phaseTicks >= RECOVERY_TICKS) {
                phase = FlightPhase.CLIMB;
                phaseTicks = 0;
                orbitClockwise = cat.getRandom().nextBoolean();
            }
        }

        private void beginRecovery() {
            phase = FlightPhase.RECOVER;
            phaseTicks = 0;
        }

        private Vec3 horizontalAwayFrom(LivingEntity target) {
            Vec3 away = cat.position().subtract(target.position());
            away = new Vec3(away.x, 0.0D, away.z);
            if (away.lengthSqr() < 1.0E-5D) {
                double angle = cat.getRandom().nextDouble() * Math.PI * 2.0D;
                away = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            }
            return away.normalize();
        }

        private void flyToward(Vec3 destination, double maximumSpeed) {
            Vec3 offset = destination.subtract(cat.position());
            if (offset.lengthSqr() < 1.0E-5D) return;
            Vec3 desired = offset.normalize().scale(maximumSpeed);
            Vec3 blended = cat.getDeltaMovement().scale(0.52D)
                    .add(desired.scale(0.48D));
            if (blended.lengthSqr() > maximumSpeed * maximumSpeed) {
                blended = blended.normalize().scale(maximumSpeed);
            }
            cat.setDeltaMovement(blended);
        }

        private enum FlightPhase {
            CLIMB,
            DIVE,
            RECOVER
        }
    }

    /** Long-range, steady-output combat behaviour exclusive to the mechanical cat. */
    private static final class MechanicalLaserGoal extends Goal {
        private final Cat cat;
        private int attackCooldown;
        private int movementCooldown;
        private int strafeTicks;
        private boolean strafeClockwise;

        private MechanicalLaserGoal(Cat cat) {
            this.cat = cat;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return canFight(cat)
                    && CatClothesData.getOutfit(cat) == CatOutfitType.TERMINATOR
                    && canTarget(cat, cat.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            attackCooldown = 0;
            movementCooldown = 0;
            strafeTicks = 0;
            strafeClockwise = cat.getRandom().nextBoolean();
        }

        @Override
        public void stop() {
            cat.getNavigation().stop();
            attackCooldown = 0;
            movementCooldown = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = cat.getTarget();
            if (!canTarget(cat, target)) return;

            if (attackCooldown > 0) attackCooldown--;
            int intelligence = CatAttributeEffects.effectiveValue(cat, CatStat.INTELLIGENCE);
            double distanceSqr = cat.distanceToSqr(target);
            boolean canSee = cat.getSensing().hasLineOfSight(target);
            cat.getLookControl().setLookAt(target, 35.0F, 35.0F);

            if (movementCooldown-- <= 0) {
                movementCooldown = 6;
                updatePosition(target, intelligence, distanceSqr);
            }

            if (canSee && distanceSqr <= MECHANICAL_LASER_RANGE * MECHANICAL_LASER_RANGE
                    && attackCooldown <= 0) {
                launchLaser(target, intelligence);
                attackCooldown = careerAttackIntervalTicks(cat);
            }
        }

        private void updatePosition(LivingEntity target, int intelligence,
                                    double distanceSqr) {
            double distance = Math.sqrt(distanceSqr);
            if (intelligence < COMPETENT_INTELLIGENCE) {
                // An inexperienced ranged cat knows how to shoot, but still walks
                // much closer to danger than it needs to.
                if (distance > 7.0D) cat.getNavigation().moveTo(target, 0.95D);
                else cat.getNavigation().stop();
                return;
            }

            if (intelligence < TACTICAL_INTELLIGENCE) {
                if (distance < 7.0D) moveAwayFrom(target, 4.0D, 0.95D);
                else if (distance > 14.0D) cat.getNavigation().moveTo(target, 0.9D);
                else cat.getNavigation().stop();
                return;
            }

            if (++strafeTicks >= 36) {
                strafeTicks = 0;
                if (cat.getRandom().nextFloat() < 0.6F) {
                    strafeClockwise = !strafeClockwise;
                }
            }
            if (distance < 8.0D) {
                moveAwayFrom(target, 4.5D, 1.0D);
            } else if (distance > 14.5D) {
                cat.getNavigation().moveTo(target, 0.92D);
            } else {
                Vec3 radial = cat.position().subtract(target.position());
                radial = new Vec3(radial.x, 0.0D, radial.z);
                if (radial.lengthSqr() < 1.0E-5D) radial = new Vec3(1.0D, 0.0D, 0.0D);
                radial = radial.normalize();
                Vec3 tangent = new Vec3(-radial.z, 0.0D, radial.x)
                        .scale(strafeClockwise ? 3.5D : -3.5D);
                Vec3 destination = target.position().add(radial.scale(11.5D)).add(tangent);
                cat.getNavigation().moveTo(destination.x, cat.getY(), destination.z, 0.88D);
            }
        }

        private void moveAwayFrom(LivingEntity target, double distance, double speed) {
            Vec3 away = cat.position().subtract(target.position());
            away = new Vec3(away.x, 0.0D, away.z);
            if (away.lengthSqr() < 1.0E-5D) {
                away = new Vec3(Mth.sin(cat.getYRot() * Mth.DEG_TO_RAD), 0.0D,
                        -Mth.cos(cat.getYRot() * Mth.DEG_TO_RAD));
            }
            Vec3 destination = cat.position().add(away.normalize().scale(distance));
            cat.getNavigation().moveTo(destination.x, cat.getY(), destination.z, speed);
        }

        private void launchLaser(LivingEntity target, int intelligence) {
            if (!(cat.level() instanceof ServerLevel level)) return;
            MechanicalLaserProjectile projectile = new MechanicalLaserProjectile(level, cat,
                    (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE));
            projectile.setPos(cat.getX(), cat.getEyeY() - 0.03D, cat.getZ());

            Vec3 aim = target.getEyePosition().subtract(projectile.position());
            if (intelligence >= TACTICAL_INTELLIGENCE) {
                double flightTicks = Math.min(7.0D, aim.length() / 2.4D);
                aim = aim.add(target.getDeltaMovement().scale(flightTicks));
            }
            projectile.shoot(aim.x, aim.y, aim.z, 2.4F, 0.0F);
            level.addFreshEntity(projectile);
            cat.swing(InteractionHand.MAIN_HAND);
            level.playSound(null, cat.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                    SoundSource.NEUTRAL, 0.45F,
                    1.55F + cat.getRandom().nextFloat() * 0.2F);
        }
    }

    /** Slow-firing heavy ranged role exclusive to the honey-gathering cat. */
    private static final class HoneyMissileGoal extends Goal {
        private final Cat cat;
        private int attackCooldown;
        private int movementCooldown;
        private int strafeTicks;
        private boolean strafeClockwise;

        private HoneyMissileGoal(Cat cat) {
            this.cat = cat;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return canFight(cat) && CatClothesData.getOutfit(cat) == CatOutfitType.HONEY
                    && canTarget(cat, cat.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            attackCooldown = 0;
            movementCooldown = 0;
            strafeTicks = 0;
            strafeClockwise = cat.getRandom().nextBoolean();
        }

        @Override
        public void stop() {
            cat.getNavigation().stop();
            attackCooldown = 0;
            movementCooldown = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = cat.getTarget();
            if (!canTarget(cat, target)) return;

            if (attackCooldown > 0) attackCooldown--;
            int intelligence = CatAttributeEffects.effectiveValue(cat, CatStat.INTELLIGENCE);
            double distanceSqr = cat.distanceToSqr(target);
            boolean canSee = cat.getSensing().hasLineOfSight(target);
            cat.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (movementCooldown-- <= 0) {
                movementCooldown = 8;
                updatePosition(target, intelligence, distanceSqr);
            }

            if (canSee && distanceSqr <= HONEY_MISSILE_RANGE * HONEY_MISSILE_RANGE
                    && attackCooldown <= 0) {
                launchMissile(target, intelligence);
                attackCooldown = careerAttackIntervalTicks(cat);
            }
        }

        private void updatePosition(LivingEntity target, int intelligence,
                                    double distanceSqr) {
            double distance = Math.sqrt(distanceSqr);
            if (intelligence < COMPETENT_INTELLIGENCE) {
                if (distance > 5.0D) cat.getNavigation().moveTo(target, 0.92D);
                else cat.getNavigation().stop();
                return;
            }

            if (intelligence < TACTICAL_INTELLIGENCE) {
                if (distance < 5.0D) moveAwayFrom(target, 3.5D, 0.92D);
                else if (distance > 10.5D) cat.getNavigation().moveTo(target, 0.88D);
                else cat.getNavigation().stop();
                return;
            }

            if (++strafeTicks >= 42) {
                strafeTicks = 0;
                if (cat.getRandom().nextFloat() < 0.6F) {
                    strafeClockwise = !strafeClockwise;
                }
            }
            if (distance < 6.0D) {
                moveAwayFrom(target, 3.5D, 0.95D);
            } else if (distance > 11.0D) {
                cat.getNavigation().moveTo(target, 0.88D);
            } else {
                Vec3 radial = cat.position().subtract(target.position());
                radial = new Vec3(radial.x, 0.0D, radial.z);
                if (radial.lengthSqr() < 1.0E-5D) radial = new Vec3(1.0D, 0.0D, 0.0D);
                radial = radial.normalize();
                Vec3 tangent = new Vec3(-radial.z, 0.0D, radial.x)
                        .scale(strafeClockwise ? 2.5D : -2.5D);
                Vec3 destination = target.position().add(radial.scale(8.5D)).add(tangent);
                cat.getNavigation().moveTo(destination.x, cat.getY(), destination.z, 0.82D);
            }
        }

        private void moveAwayFrom(LivingEntity target, double distance, double speed) {
            Vec3 away = cat.position().subtract(target.position());
            away = new Vec3(away.x, 0.0D, away.z);
            if (away.lengthSqr() < 1.0E-5D) {
                away = new Vec3(Mth.sin(cat.getYRot() * Mth.DEG_TO_RAD), 0.0D,
                        -Mth.cos(cat.getYRot() * Mth.DEG_TO_RAD));
            }
            Vec3 destination = cat.position().add(away.normalize().scale(distance));
            cat.getNavigation().moveTo(destination.x, cat.getY(), destination.z, speed);
        }

        private void launchMissile(LivingEntity target, int intelligence) {
            if (!(cat.level() instanceof ServerLevel level)) return;
            HoneyMissileProjectile projectile = new HoneyMissileProjectile(level, cat,
                    (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE));
            projectile.setPos(cat.getX(), cat.getEyeY() - 0.04D, cat.getZ());

            Vec3 aim = target.getEyePosition().subtract(projectile.position());
            if (intelligence >= TACTICAL_INTELLIGENCE) {
                double flightTicks = Math.min(9.0D, aim.length() / 1.35D);
                aim = aim.add(target.getDeltaMovement().scale(flightTicks));
            }
            float inaccuracy = intelligence < COMPETENT_INTELLIGENCE ? 2.5F
                    : intelligence < TACTICAL_INTELLIGENCE ? 1.0F : 0.2F;
            projectile.shoot(aim.x, aim.y, aim.z, 1.35F, inaccuracy);
            level.addFreshEntity(projectile);
            cat.swing(InteractionHand.MAIN_HAND);
            level.playSound(null, cat.blockPosition(), SoundEvents.HONEY_BLOCK_SLIDE,
                    SoundSource.NEUTRAL, 0.8F,
                    1.05F + cat.getRandom().nextFloat() * 0.15F);
        }
    }

    /** High-damage, slow-firing ranged role exclusive to the dynamite cat. */
    private static final class DynamiteThrowGoal extends Goal {
        private final Cat cat;
        private int attackCooldown;
        private int movementCooldown;
        private int strafeTicks;
        private boolean strafeClockwise;

        private DynamiteThrowGoal(Cat cat) {
            this.cat = cat;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return canFight(cat)
                    && CatClothesData.getOutfit(cat) == CatOutfitType.DYNAMITE
                    && canTarget(cat, cat.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            attackCooldown = 0;
            movementCooldown = 0;
            strafeTicks = 0;
            strafeClockwise = cat.getRandom().nextBoolean();
        }

        @Override
        public void stop() {
            cat.getNavigation().stop();
            attackCooldown = 0;
            movementCooldown = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = cat.getTarget();
            if (!canTarget(cat, target)) return;

            if (attackCooldown > 0) attackCooldown--;
            int intelligence = CatAttributeEffects.effectiveValue(cat, CatStat.INTELLIGENCE);
            double distanceSqr = cat.distanceToSqr(target);
            boolean canSee = cat.getSensing().hasLineOfSight(target);
            cat.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (movementCooldown-- <= 0) {
                movementCooldown = 8;
                updatePosition(target, intelligence, distanceSqr);
            }

            if (canSee && distanceSqr <= DYNAMITE_RANGE * DYNAMITE_RANGE
                    && attackCooldown <= 0) {
                throwDynamite(target, intelligence);
                attackCooldown = careerAttackIntervalTicks(cat);
            }
        }

        private void updatePosition(LivingEntity target, int intelligence,
                                    double distanceSqr) {
            double distance = Math.sqrt(distanceSqr);
            if (intelligence < COMPETENT_INTELLIGENCE) {
                if (distance > 4.0D) cat.getNavigation().moveTo(target, 0.90D);
                else cat.getNavigation().stop();
                return;
            }

            if (intelligence < TACTICAL_INTELLIGENCE) {
                if (distance < 3.5D) moveAwayFrom(target, 3.0D, 0.92D);
                else if (distance > 7.0D) cat.getNavigation().moveTo(target, 0.86D);
                else cat.getNavigation().stop();
                return;
            }

            if (++strafeTicks >= 44) {
                strafeTicks = 0;
                if (cat.getRandom().nextFloat() < 0.6F) {
                    strafeClockwise = !strafeClockwise;
                }
            }
            if (distance < 4.0D) {
                moveAwayFrom(target, 3.0D, 0.94D);
            } else if (distance > 7.2D) {
                cat.getNavigation().moveTo(target, 0.86D);
            } else {
                Vec3 radial = cat.position().subtract(target.position());
                radial = new Vec3(radial.x, 0.0D, radial.z);
                if (radial.lengthSqr() < 1.0E-5D) radial = new Vec3(1.0D, 0.0D, 0.0D);
                radial = radial.normalize();
                Vec3 tangent = new Vec3(-radial.z, 0.0D, radial.x)
                        .scale(strafeClockwise ? 2.0D : -2.0D);
                Vec3 destination = target.position().add(radial.scale(5.7D)).add(tangent);
                cat.getNavigation().moveTo(destination.x, cat.getY(), destination.z, 0.80D);
            }
        }

        private void moveAwayFrom(LivingEntity target, double distance, double speed) {
            Vec3 away = cat.position().subtract(target.position());
            away = new Vec3(away.x, 0.0D, away.z);
            if (away.lengthSqr() < 1.0E-5D) {
                away = new Vec3(Mth.sin(cat.getYRot() * Mth.DEG_TO_RAD), 0.0D,
                        -Mth.cos(cat.getYRot() * Mth.DEG_TO_RAD));
            }
            Vec3 destination = cat.position().add(away.normalize().scale(distance));
            cat.getNavigation().moveTo(destination.x, cat.getY(), destination.z, speed);
        }

        private void throwDynamite(LivingEntity target, int intelligence) {
            if (!(cat.level() instanceof ServerLevel level)) return;
            DynamiteProjectile projectile = new DynamiteProjectile(level, cat,
                    (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE));
            projectile.setPos(cat.getX(), cat.getEyeY() - 0.02D, cat.getZ());

            Vec3 targetPoint = new Vec3(target.getX(), target.getY(0.55D), target.getZ());
            Vec3 aim = targetPoint.subtract(projectile.position());
            if (intelligence >= TACTICAL_INTELLIGENCE) {
                double flightTicks = Math.min(10.0D, aim.length());
                aim = aim.add(target.getDeltaMovement().scale(flightTicks));
            }
            double horizontal = Math.sqrt(aim.x * aim.x + aim.z * aim.z);
            aim = aim.add(0.0D, horizontal * 0.14D, 0.0D);
            float inaccuracy = intelligence < COMPETENT_INTELLIGENCE ? 4.0F
                    : intelligence < TACTICAL_INTELLIGENCE ? 1.5F : 0.4F;
            projectile.shoot(aim.x, aim.y, aim.z, 1.0F, inaccuracy);
            level.addFreshEntity(projectile);
            cat.swing(InteractionHand.MAIN_HAND);
            level.playSound(null, cat.blockPosition(), SoundEvents.TNT_PRIMED,
                    SoundSource.NEUTRAL, 0.65F,
                    1.15F + cat.getRandom().nextFloat() * 0.12F);
        }
    }

    private static final class FishingRangedGoal extends Goal {
        private final Cat cat;
        private int attackCooldown;
        private int movementCooldown;
        private int shotRecoveryTicks;
        private int strafeTicks;
        private boolean strafeClockwise;

        private FishingRangedGoal(Cat cat) {
            this.cat = cat;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return canFight(cat) && CatClothesData.getOutfit(cat) == CatOutfitType.FISHING
                    && canTarget(cat, cat.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            attackCooldown = 0;
            movementCooldown = 0;
            shotRecoveryTicks = 0;
            strafeTicks = 0;
            strafeClockwise = cat.getRandom().nextBoolean();
        }

        @Override
        public void stop() {
            cat.getNavigation().stop();
            attackCooldown = 0;
            movementCooldown = 0;
            shotRecoveryTicks = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = cat.getTarget();
            if (!canTarget(cat, target)) return;

            if (attackCooldown > 0) attackCooldown--;
            if (shotRecoveryTicks > 0) shotRecoveryTicks--;
            int intelligence = CatAttributeEffects.effectiveValue(cat, CatStat.INTELLIGENCE);
            double distanceSqr = cat.distanceToSqr(target);
            boolean canSee = cat.getSensing().hasLineOfSight(target);
            cat.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (shotRecoveryTicks <= 0 && movementCooldown-- <= 0) {
                movementCooldown = 8;
                updateRangedPosition(target, intelligence, distanceSqr);
            }

            double range = intelligence < COMPETENT_INTELLIGENCE ? 4.5D
                    : intelligence < TACTICAL_INTELLIGENCE ? 11.0D : RANGED_MAX_DISTANCE;
            if (canSee && distanceSqr <= range * range && attackCooldown <= 0) {
                launchFishingRod(target, intelligence);
                attackCooldown = careerAttackIntervalTicks(cat);
                shotRecoveryTicks = 6;
                cat.getNavigation().stop();
            }
        }

        private void updateRangedPosition(LivingEntity target, int intelligence,
                                          double distanceSqr) {
            double distance = Math.sqrt(distanceSqr);
            if (intelligence < COMPETENT_INTELLIGENCE) {
                if (distance > 3.5D) cat.getNavigation().moveTo(target, 0.95D);
                else cat.getNavigation().stop();
                return;
            }

            if (intelligence < TACTICAL_INTELLIGENCE) {
                if (distance < 4.5D) moveAwayFrom(target, 3.0D, 0.95D);
                else if (distance > 9.0D) cat.getNavigation().moveTo(target, 0.9D);
                else cat.getNavigation().stop();
                return;
            }

            if (++strafeTicks >= 40) {
                strafeTicks = 0;
                if (cat.getRandom().nextFloat() < 0.65F) strafeClockwise = !strafeClockwise;
            }
            if (distance < 5.5D) {
                moveAwayFrom(target, 3.0D, 0.95D);
            } else if (distance > 10.0D) {
                cat.getNavigation().moveTo(target, 0.9D);
            } else {
                Vec3 radial = cat.position().subtract(target.position());
                radial = new Vec3(radial.x, 0.0D, radial.z);
                if (radial.lengthSqr() < 1.0E-5D) radial = new Vec3(1.0D, 0.0D, 0.0D);
                radial = radial.normalize();
                Vec3 tangent = new Vec3(-radial.z, 0.0D, radial.x)
                        .scale(strafeClockwise ? 3.0D : -3.0D);
                Vec3 destination = target.position().add(radial.scale(7.5D)).add(tangent);
                cat.getNavigation().moveTo(destination.x, cat.getY(), destination.z, 0.85D);
            }
        }

        private void moveAwayFrom(LivingEntity target, double distance, double speed) {
            Vec3 away = cat.position().subtract(target.position());
            away = new Vec3(away.x, 0.0D, away.z);
            if (away.lengthSqr() < 1.0E-5D) {
                away = new Vec3(Mth.sin(cat.getYRot() * Mth.DEG_TO_RAD), 0.0D,
                        -Mth.cos(cat.getYRot() * Mth.DEG_TO_RAD));
            }
            Vec3 destination = cat.position().add(away.normalize().scale(distance));
            cat.getNavigation().moveTo(destination.x, cat.getY(), destination.z, speed);
        }

        private void launchFishingRod(LivingEntity target, int intelligence) {
            if (!(cat.level() instanceof ServerLevel level)) return;
            FishingRodProjectile projectile = new FishingRodProjectile(level, cat,
                    (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE));
            projectile.setPos(cat.getX(), cat.getEyeY() - 0.08D, cat.getZ());

            Vec3 aim = target.getEyePosition().subtract(projectile.position());
            if (intelligence >= TACTICAL_INTELLIGENCE) {
                aim = aim.add(target.getDeltaMovement().scale(0.45D));
            }
            double horizontal = Math.sqrt(aim.x * aim.x + aim.z * aim.z);
            float inaccuracy = intelligence < COMPETENT_INTELLIGENCE ? 8.0F
                    : intelligence < TACTICAL_INTELLIGENCE ? 4.0F : 1.5F;
            projectile.shoot(aim.x, aim.y + horizontal * 0.045D, aim.z,
                    1.65F, inaccuracy);
            level.addFreshEntity(projectile);
            cat.swing(InteractionHand.MAIN_HAND);
            level.playSound(null, cat.blockPosition(), SoundEvents.FISHING_BOBBER_THROW,
                    SoundSource.NEUTRAL, 0.9F, 0.9F + cat.getRandom().nextFloat() * 0.2F);
        }
    }

    private static final class CareerOwnerHurtByGoal extends OwnerHurtByTargetGoal {
        private final Cat cat;

        private CareerOwnerHurtByGoal(Cat cat) {
            super(cat);
            this.cat = cat;
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = cat.getOwner();
            LivingEntity attacker = owner == null ? null : owner.getLastHurtByMob();
            return canFight(cat) && canTarget(cat, attacker) && super.canUse();
        }
    }

    private static final class CareerOwnerHurtTargetGoal extends OwnerHurtTargetGoal {
        private final Cat cat;

        private CareerOwnerHurtTargetGoal(Cat cat) {
            super(cat);
            this.cat = cat;
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = cat.getOwner();
            LivingEntity target = owner == null ? null : owner.getLastHurtMob();
            return canFight(cat) && canTarget(cat, target) && super.canUse();
        }
    }

    private static final class CareerHurtByGoal extends HurtByTargetGoal {
        private final Cat cat;

        private CareerHurtByGoal(Cat cat) {
            super(cat);
            this.cat = cat;
        }

        @Override
        public boolean canUse() {
            LivingEntity attacker = cat.getLastHurtByMob();
            LivingEntity owner = cat.getOwner();
            return canFight(cat) && attacker != null && attacker != owner
                    && canTarget(cat, attacker) && super.canUse();
        }
    }

    private CareerCatBehavior() {
    }
}
