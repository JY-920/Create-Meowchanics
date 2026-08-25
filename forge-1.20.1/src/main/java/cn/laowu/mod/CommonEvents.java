package cn.laowu.mod;

import cn.laowu.mod.network.ModNetwork;
import cn.laowu.mod.item.CatPancakeItem;
import cn.laowu.mod.item.FusionDebugWandItem;
import cn.laowu.mod.item.AttributeDebugWandItem;
import cn.laowu.mod.item.TraitDebugWandItem;
import cn.laowu.mod.item.CatToolBehavior;
import cn.laowu.mod.item.CatTotemItem;
import cn.laowu.mod.item.KimiArmorItem;
import cn.laowu.mod.item.TerminatorSuitItem;
import cn.laowu.mod.item.CatScannerItem;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatAttributeEffects;
import cn.laowu.mod.genetics.CatAttributeProfile;
import cn.laowu.mod.genetics.CatBreedingMode;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitEffects;
import cn.laowu.mod.genetics.CatBehaviorTraitEffects;
import cn.laowu.mod.genetics.CatTraitProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundSource;
import com.simibubi.create.AllDamageTypes;
import com.simibubi.create.content.kinetics.deployer.DeployerRecipeSearchEvent;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ThrownPotion;
import com.simibubi.create.content.equipment.potatoCannon.PotatoProjectileEntity;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.WeakHashMap;


public final class CommonEvents {
    private static final String CAT_GRENADE_EXPLODED_TAG = "LaoWuCatGrenadeExploded";
    private static final String TAME_PANIC_REMOVED_TAG = "LaoWuTamePanicRemoved";
    private static final Set<Cat> TAME_PANIC_DISABLED =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final float CAT_GRENADE_DAMAGE = 30.0F;
    private static final double CAT_GRENADE_RADIUS = 3.0D;

    @SubscribeEvent
    public static void preventForbiddenTargets(LivingChangeTargetEvent event) {
        var helmet = event.getNewTarget() instanceof Player player
                ? player.getItemBySlot(EquipmentSlot.HEAD) : net.minecraft.world.item.ItemStack.EMPTY;
        if (event.getEntity() instanceof Phantom
                && helmet.is(LaoWuMod.CAT_HELMET.get())
                && !CatToolBehavior.isExhausted(helmet)) {
            event.setNewTarget(null);
        }
        if (event.getEntity() instanceof Cat cat
                && CatClothesData.getOutfit(cat) == CatOutfitType.TERMINATOR
                && CareerCatBehavior.isForbiddenTerminatorTarget(event.getNewTarget())) {
            event.setNewTarget(null);
        }
        if (!event.getEntity().level().isClientSide
                && event.getEntity() instanceof Enemy
                && event.getNewTarget() != null
                && !(event.getNewTarget() instanceof Cat targetCat
                && CatTraitData.ensure(targetCat).has(CatTrait.ATTENTION_MAGNET))) {
            Cat bait = event.getEntity().level().getEntitiesOfClass(Cat.class,
                            event.getEntity().getBoundingBox().inflate(16.0D),
                            candidate -> candidate.isAlive()
                                    && !CatPoseData.isPancake(candidate)
                                    && CatTraitData.ensure(candidate)
                                    .has(CatTrait.ATTENTION_MAGNET))
                    .stream()
                    .min(Comparator.comparingDouble(
                            event.getEntity()::distanceToSqr))
                    .orElse(null);
            if (bait != null) event.setNewTarget(bait);
        }
    }

    @SubscribeEvent
    public static void preventCatBootFallDamage(LivingAttackEvent event) {
        var boots = event.getEntity() instanceof Player player
                ? player.getItemBySlot(EquipmentSlot.FEET) : net.minecraft.world.item.ItemStack.EMPTY;
        if (event.getEntity() instanceof Player player
                && event.getSource().is(DamageTypes.FALL)
                && boots.is(LaoWuMod.CAT_BOOTS.get())
                && !CatToolBehavior.isExhausted(boots)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventHeatResistantCatDamage(LivingAttackEvent event) {
        if (event.getEntity() instanceof Cat cat && !cat.level().isClientSide
                && event.getSource().is(DamageTypeTags.IS_FIRE)
                && CatTraitEffects.isHeatResistant(cat)) {
            event.setCanceled(true);
            cat.setRemainingFireTicks(0);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void applyCainMarkAvoidance(LivingAttackEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof Cat cat
                && !cat.level().isClientSide && event.getAmount() > 0.0F
                && CatTraitEffects.tryCainAvoid(cat, event.getSource())) {
            event.setCanceled(true);
        }
    }

    /** Exhausted armour remains equipped and visible, but contributes no armour attributes. */
    @SubscribeEvent
    public static void disableExhaustedCatArmorAttributes(ItemAttributeModifierEvent event) {
        if (event.getItemStack().getItem() instanceof KimiArmorItem
                && CatToolBehavior.isExhausted(event.getItemStack())) {
            event.clearModifiers();
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            CatArmorPounceBehavior.tick(event.getServer());
        }
    }

    /**
     * Creepers normally register this exact AvoidEntityGoal for Cat.class.
     * Add the same goal for players wearing cat boots when each creeper joins,
     * avoiding a separate world scan or tick handler.
     */
    @SubscribeEvent
    public static void makeCreepersFearCatBoots(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Creeper creeper)) return;
        creeper.goalSelector.addGoal(3, new AvoidEntityGoal<>(creeper, Player.class,
                6.0F, 1.0D, 1.2D,
                living -> {
                    if (!(living instanceof Player player)) return false;
                    var boots = player.getItemBySlot(EquipmentSlot.FEET);
                    return boots.is(LaoWuMod.CAT_BOOTS.get())
                            && !CatToolBehavior.isExhausted(boots);
                }));
    }

    /** Assign genetics once and materialise their entity attributes on joining. */
    @SubscribeEvent
    public static void initializeCatTraits(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Cat cat) {
            CatProfileData.recoverInterruptedViewLock(cat);
            CatTraitData.ensure(cat);
            CatAttributeData.ensure(cat);
            CatAttributeEffects.refresh(cat);
        }
    }

    @SubscribeEvent
    public static void rewriteHissingPotionTooltip(ItemTooltipEvent event) {
        HissingPotionTooltip.rewrite(
                net.minecraft.world.item.alchemy.PotionUtils.getMobEffects(event.getItemStack()),
                event.getToolTip());
    }

    /**
     * Vanilla Strength installs a persistent ATTACK_DAMAGE attribute modifier.
     * Hissing Attack intentionally differs only in lifetime: the next accepted
     * direct melee hit receives +10 * level^2 damage and immediately consumes
     * the effect. Projectiles never have the player as their direct entity.
     */
    @SubscribeEvent
    public static void onHissingEmpoweredAttack(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)
                || event.getSource().getDirectEntity() != player
                || player.level().isClientSide
                || event.getAmount() <= 0.0F) return;

        var active = player.getEffect(LaoWuMod.HISSING_ATTACK.get());
        if (active == null) return;
        int level = active.getAmplifier() + 1;
        event.setAmount(event.getAmount() + 10.0F * level * level);
        player.removeEffect(LaoWuMod.HISSING_ATTACK.get());
    }

    /** Auto-attach cats react to an actual player attack instead of polling players. */
    @SubscribeEvent
    public static void notifyAutoAttachCats(LivingAttackEvent event) {
        if (event.getAmount() > 0.0F
                && event.getSource().getDirectEntity() instanceof Player player) {
            CatBehaviorTraitEffects.notifyPlayerAttack(player);
        }
    }

    /** Child-eating cats deal double final melee damage to kittens. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void amplifyFilicideDamage(LivingHurtEvent event) {
        if (event.getSource().getDirectEntity() instanceof Cat attacker
                && event.getAmount() > 0.0F) {
            event.setAmount(event.getAmount()
                    * CatBehaviorTraitEffects.childAttackMultiplier(
                    attacker, event.getEntity()));
        }
    }

    /** Every third accepted enhanced-sword hit arms Hissing Attack I for the next hit. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEmpoweredCatSwordAttack(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)
                || event.getSource().getDirectEntity() != player
                || player.level().isClientSide
                || event.getAmount() <= 0.0F) return;
        var sword = player.getMainHandItem();
        if (!CatToolBehavior.recordSwordHit(sword)) return;

        player.addEffect(new MobEffectInstance(LaoWuMod.HISSING_ATTACK.get(),
                20 * 60 * 3, 0, false, true, true));
    }

    /** Luck gives cats vanilla-style 150% critical melee hits. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void applyCatAttributeCriticalHit(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Cat cat)
                || event.getSource().getDirectEntity() != cat
                || cat.level().isClientSide
                || event.getSource().is(DamageTypes.THORNS)
                || event.getAmount() <= 0.0F
                || !CatAttributeEffects.rollCriticalHit(cat)) return;

        event.setAmount(CatAttributeEffects.criticalDamage(event.getAmount()));
        if (cat.level() instanceof ServerLevel level) {
            LivingEntity target = event.getEntity();
            level.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY(0.6D), target.getZ(),
                    10, target.getBbWidth() * 0.35D,
                    target.getBbHeight() * 0.2D,
                    target.getBbWidth() * 0.35D, 0.08D);
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.NEUTRAL,
                    0.75F, 1.05F);
        }
    }

    @SubscribeEvent
    public static void onExhaustedCatToolAttack(LivingAttackEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)
                || event.getSource().getDirectEntity() != attacker) return;
        if (CatToolBehavior.isExhausted(attacker.getMainHandItem())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExhaustedCatToolBreakSpeed(PlayerEvent.BreakSpeed event) {
        var held = event.getEntity().getMainHandItem();
        if (CatToolBehavior.isExhausted(held)) {
            event.setNewSpeed(0.0F);
            return;
        }
        if (held.is(LaoWuMod.CAT_SHOVEL.get())
                && CatToolBehavior.isEmpowered(held)
                && event.getState().is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            event.setNewSpeed(Math.max(event.getNewSpeed(), 100.0F));
        }
    }

    @SubscribeEvent
    public static void onExhaustedCatToolBreak(BlockEvent.BreakEvent event) {
        if (CatToolBehavior.isExhausted(event.getPlayer().getMainHandItem())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExhaustedCatToolLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        failExhaustedToolInteraction(event);
    }

    @SubscribeEvent
    public static void onExhaustedCatToolRightClick(PlayerInteractEvent.RightClickBlock event) {
        failExhaustedToolInteraction(event);
    }

    @SubscribeEvent
    public static void onExhaustedCatToolUse(PlayerInteractEvent.RightClickItem event) {
        failExhaustedToolInteraction(event);
    }

    @SubscribeEvent
    public static void onExhaustedCatToolEntityUse(PlayerInteractEvent.EntityInteract event) {
        failExhaustedToolInteraction(event);
    }

    private static void failExhaustedToolInteraction(PlayerInteractEvent event) {
        if (!CatToolBehavior.isExhausted(event.getItemStack())) return;
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onHissingGasBucketInteract(PlayerInteractEvent.RightClickBlock event) {
        var held = event.getItemStack();
        boolean gasBucket = held.is(LaoWuMod.HISSING_GAS_BUCKET.get());
        boolean emptyBucket = held.is(Items.BUCKET);
        if (!gasBucket && !emptyBucket) return;

        // Creative tanks use their own setContainedFluid exchange path. Their
        // capability deliberately reports every fill as accepted without
        // mutating, so our generic handler must not consume the click first.
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof
                com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity) return;

        // The collector is deliberately pipe-only even though its outward face
        // exposes a fluid capability to Create's pipe network.
        if (event.getLevel().getBlockEntity(event.getPos())
                instanceof cn.laowu.mod.create.HissingCollectorBlockEntity) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        IFluidHandler handler = FluidUtil.getFluidHandler(
                event.getLevel(), event.getPos(), event.getFace()).orElse(null);
        if (handler == null) return;

        if (gasBucket) {
            FluidStack gas = new FluidStack(LaoWuMod.HISSING_GAS.get(), 1000);
            if (handler.fill(gas, IFluidHandler.FluidAction.SIMULATE) < 1000) return;
        } else {
            FluidStack drained = handler.drain(1000, IFluidHandler.FluidAction.SIMULATE);
            if (drained.getAmount() < 1000
                    || !drained.getFluid().isSame(LaoWuMod.HISSING_GAS.get())) return;
        }

        if (gasBucket && !event.getLevel().isClientSide) {
            FluidStack gas = new FluidStack(LaoWuMod.HISSING_GAS.get(), 1000);
            if (handler.fill(gas, IFluidHandler.FluidAction.EXECUTE) != 1000) return;

            var player = event.getEntity();
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(event.getHand(), new net.minecraft.world.item.ItemStack(Items.BUCKET));
            }
            event.getLevel().playSound(null, event.getPos(), SoundEvents.BUCKET_EMPTY,
                    SoundSource.BLOCKS, 1.0F, 1.15F);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (gasBucket && event.getLevel().isClientSide) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (FluidUtil.interactWithFluidHandler(event.getEntity(), event.getHand(), handler)) {
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCatGrenadeImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof PotatoProjectileEntity projectile)
                || !projectile.getItem().is(LaoWuMod.CAT_GRENADE.get())
                || !(projectile.level() instanceof ServerLevel serverLevel)) return;
        if (projectile.getPersistentData().getBoolean(CAT_GRENADE_EXPLODED_TAG)) return;
        projectile.getPersistentData().putBoolean(CAT_GRENADE_EXPLODED_TAG, true);

        Vec3 impact = event.getRayTraceResult().getLocation();
        Entity owner = projectile.getOwner();
        Entity directHit = event.getRayTraceResult() instanceof EntityHitResult entityHit
                ? entityHit.getEntity() : null;
        AABB searchArea = new AABB(impact, impact).inflate(CAT_GRENADE_RADIUS);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, searchArea,
                target -> target.isAlive() && target != owner && target != directHit)) {
            AABB box = target.getBoundingBox();
            double nearestX = Math.max(box.minX, Math.min(impact.x, box.maxX));
            double nearestY = Math.max(box.minY, Math.min(impact.y, box.maxY));
            double nearestZ = Math.max(box.minZ, Math.min(impact.z, box.maxZ));
            if (impact.distanceToSqr(nearestX, nearestY, nearestZ)
                    > CAT_GRENADE_RADIUS * CAT_GRENADE_RADIUS) continue;

            target.hurt(serverLevel.damageSources().explosion(projectile, owner), CAT_GRENADE_DAMAGE);
            Vec3 push = target.position().subtract(impact);
            if (push.lengthSqr() > 1.0E-6D) {
                push = push.normalize().scale(0.65D);
                target.push(push.x, 0.22D, push.z);
            }
        }

        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                impact.x, impact.y + 0.1D, impact.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.POOF,
                impact.x, impact.y + 0.1D, impact.z, 55,
                CAT_GRENADE_RADIUS * 0.42D, CAT_GRENADE_RADIUS * 0.25D,
                CAT_GRENADE_RADIUS * 0.42D, 0.08D);
        serverLevel.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.35F, 0.85F);
    }

    @SubscribeEvent
    public static void notifyCodeConflictCatsOfSplash(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof ThrownPotion)
                || !(event.getProjectile().level() instanceof ServerLevel level)) return;
        Vec3 impact = event.getRayTraceResult().getLocation();
        for (Cat cat : level.getEntitiesOfClass(Cat.class,
                new AABB(impact, impact).inflate(4.0D), Cat::isAlive)) {
            CatBehaviorTraitEffects.notifyFluidSplash(cat, impact);
        }
    }

    /** Create's Cat Cannon keeps the complete pancake stack in its projectile. */
    @SubscribeEvent
    public static void explodeHighFuelCannonPancake(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof PotatoProjectileEntity projectile)
                || !projectile.getItem().is(LaoWuMod.CAT_PANCAKE.get())
                || cn.laowu.mod.genetics.CatTraitData.read(projectile.getItem())
                .filter(profile -> profile.has(CatTrait.HIGH_EXPLOSIVE_FUEL)).isEmpty()
                || !(projectile.level() instanceof ServerLevel level)) return;
        String explodedTag = "LaoWuHighFuelPancakeExploded";
        if (projectile.getPersistentData().getBoolean(explodedTag)) return;
        projectile.getPersistentData().putBoolean(explodedTag, true);
        Vec3 impact = event.getRayTraceResult().getLocation();
        Entity owner = projectile.getOwner();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(impact, impact).inflate(3.5D),
                target -> target.isAlive() && target != owner)) {
            target.hurt(level.damageSources().explosion(projectile, owner), 20.0F);
            target.setSecondsOnFire(6);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z,
                8, 1.3D, 0.8D, 1.3D, 0.04D);
        level.sendParticles(ParticleTypes.FLAME, impact.x, impact.y, impact.z,
                45, 1.7D, 1.0D, 1.7D, 0.06D);
        level.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.6F, 0.7F);
    }

    @SubscribeEvent
    public static void onCatAttacked(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Cat cat) || cat.level().isClientSide) return;

        if (event.getSource().is(DamageTypes.FALLING_ANVIL)
                || event.getSource().is(AllDamageTypes.ROLLER)) {
            event.setCanceled(true);
            CatPancakeBehavior.flatten(cat);
            return;
        }

        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        var held = attacker.getMainHandItem();
        var itemKey = ForgeRegistries.ITEMS.getKey(held.getItem());
        if (itemKey == null || !itemKey.getNamespace().equals("create")
                || !itemKey.getPath().equals("cardboard_sword")) return;

        event.setCanceled(true);
        CatPancakeBehavior.flatten(cat);
    }

    @SubscribeEvent
    public static void suppressVanillaCatSounds(PlayLevelSoundEvent.AtEntity event) {
        if (!(event.getEntity() instanceof Cat cat)
                || (!CatPoseData.isHissing(cat) && !CatPoseData.isPancake(cat)
                && !HissingCatBehavior.isFighting(cat))) return;

        var location = event.getSound().value().getLocation();
        if (location.getNamespace().equals("minecraft") && location.getPath().startsWith("entity.cat.")) {
            // PlayLevelSoundEvent is mutable rather than cancellable in Forge 1.20.1.
            event.setNewVolume(0.0F);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Cat cat && !cat.level().isClientSide) {
            disableTamedCatPanic(cat);
            CatAttributeEffects.tick(cat);
            CatTraitEffects.tick(cat);
            if (CatProfileData.isBeingViewed(cat)) {
                cat.getNavigation().stop();
                cat.setDeltaMovement(0.0D, cat.getDeltaMovement().y, 0.0D);
                return;
            }
            CareerCatBehavior.tick(cat);
            if (CatPancakeBehavior.tickPancake(cat)) return;
            if (CatLogisticsBehavior.tick(cat)) {
                HissingGasProduction.tick(cat);
                return;
            }
            if (CatBehaviorTraitEffects.tick(cat)) {
                HissingGasProduction.tick(cat);
                return;
            }
            HissingCatBehavior.tick(cat);
            HissingGasProduction.tick(cat);
        }
    }

    /**
     * Vanilla gives every cat a priority-1 PanicGoal. It is useful for wild
     * cats, but makes a pet sprint randomly after taking damage. Remove only
     * that goal once the cat is tamed; following, sitting and combat AI remain
     * untouched. The marker must be tied to the live entity instance: goal
     * selectors are rebuilt after loading, while persistent NBT is not.
     */
    private static void disableTamedCatPanic(Cat cat) {
        if (!cat.isTame() || !TAME_PANIC_DISABLED.add(cat)) return;
        cat.goalSelector.removeAllGoals(goal -> goal instanceof PanicGoal);
        // Remove the broken marker written by earlier builds so old worlds are
        // migrated as soon as each pet is loaded.
        cat.getPersistentData().remove(TAME_PANIC_REMOVED_TAG);
    }

    @SubscribeEvent
    public static void onCatHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Cat cat) || cat.level().isClientSide
                || CatLogisticsBehavior.isActive(cat)
                || HissingCatBehavior.isFighting(cat) || !CatPoseData.isHissing(cat)) return;
        Cat partner = HissingCatBehavior.getCurrentPartner(cat);
        if (partner != null && CatPoseData.isHissing(partner)) {
            HissingCatBehavior.startFight(cat, partner);
        }
    }

    /** Stop the path selected by vanilla PanicGoal as soon as pet damage lands. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void stopTamedCatDamagePanic(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Cat cat) || cat.level().isClientSide
                || !cat.isTame() || event.getAmount() <= 0.0F) return;
        // Ordinary pets forget the attacker so no vanilla reaction survives.
        // A Terminator cat keeps it as the target for its wolf-style defence AI;
        // its PanicGoal has already been removed, so this does not reintroduce
        // the old random sprinting behaviour.
        if (CatClothesData.getOutfit(cat) != CatOutfitType.TERMINATOR
                || CareerCatBehavior.isForbiddenTerminatorTarget(cat.getLastHurtByMob())) {
            cat.setLastHurtByMob(null);
        }
        cat.getNavigation().stop();
    }

    /** Runs on accepted final damage; Nine Lives resolves before reactive traits. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void applyCatTraitThorns(LivingDamageEvent event) {
        if (event.getEntity() instanceof Cat cat && !cat.level().isClientSide
                && event.getAmount() > 0.0F) {
            if (CatTraitEffects.tryNineLives(cat, event.getAmount())) {
                event.setAmount(0.0F);
                return;
            }
            CatTraitEffects.onAcceptedDamage(cat);
            CatTraitEffects.tryReflectDamage(cat, event.getSource());
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof Cat cat) {
            ModNetwork.syncToPlayer(player, cat, CatPoseData.getPose(cat));
            ModNetwork.syncAudioToPlayer(player, cat);
            CatChestData.syncToPlayer(player, cat);
            CatClothesData.syncToPlayer(player, cat);
            if (cn.laowu.mod.genetics.CatGenomeData.has(cat)) {
                ModNetwork.syncCatGenomeToPlayer(player, cat);
            }
            CatAttributeData.ensure(cat);
            ModNetwork.syncCatAttributesToPlayer(player, cat);
            CatTraitData.ensure(cat);
            ModNetwork.syncCatTraitsToPlayer(player, cat);
            ModNetwork.syncCatTraitStateToPlayer(player, cat);
        }
    }

    /**
     * Natural kittens use the normal three-locus contract and a small 5%
     * trait-mutation chance. Trait levels are never inherited.
     */
    @SubscribeEvent
    public static void onCatBred(BabyEntitySpawnEvent event) {
        if (!(event.getParentA() instanceof Cat first)
                || !(event.getParentB() instanceof Cat second)
                || !(event.getChild() instanceof Cat child)
                || child.level().isClientSide) return;

        if (CatBehaviorTraitEffects.cannotBreed(first)
                || CatBehaviorTraitEffects.cannotBreed(second)) {
            event.setCanceled(true);
            return;
        }

        CatAttributeData.set(child, CatAttributeProfile.breed(
                CatAttributeData.ensure(first),
                CatAttributeData.ensure(second),
                CatBreedingMode.NORMAL, 0.0F,
                child.getRandom()));
        CatTraitData.set(child, CatTraitProfile.breed(
                CatTraitData.ensure(first), CatTraitData.ensure(second),
                0.05F, child.getRandom()));
    }

    @SubscribeEvent
    public static void onCatInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof ItemEntity itemEntity
                && event.getItemStack().getItem() instanceof AttributeDebugWandItem wand
                && itemEntity.getItem().is(LaoWuMod.CAT_PANCAKE.get())) {
            InteractionResult result = wand.interactItemEntity(
                    event.getEntity(), itemEntity, event.getHand());
            if (result.consumesAction()) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
            return;
        }
        if (event.getTarget() instanceof ItemEntity itemEntity
                && event.getItemStack().getItem() instanceof TraitDebugWandItem wand
                && itemEntity.getItem().is(LaoWuMod.CAT_PANCAKE.get())) {
            InteractionResult result = wand.interactItemEntity(
                    event.getEntity(), itemEntity, event.getHand());
            if (result.consumesAction()) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
            return;
        }
        if (!(event.getTarget() instanceof Cat cat)) return;

        if (CatBehaviorTraitEffects.refusesFood(cat, event.getItemStack())
                || (!cat.isBaby() && cat.isFood(event.getItemStack())
                && CatBehaviorTraitEffects.cannotBreed(cat))) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        // Tamed cats normally consume the interaction to toggle sitting before
        // Item#interactLivingEntity runs. Route the scanner first so every cat,
        // including a living pancake, can open the same profile screen.
        if (event.getItemStack().getItem() instanceof CatScannerItem scanner) {
            InteractionResult result = scanner.interactLivingEntity(event.getItemStack(),
                    event.getEntity(), cat, event.getHand());
            if (result.consumesAction()) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
            return;
        }

        // A tamed cat consumes ordinary interaction before Item#interactLivingEntity
        // (it toggles sitting), so route the debug wand through the Forge hook first.
        if (event.getItemStack().getItem() instanceof FusionDebugWandItem wand) {
            InteractionResult result = wand.interactLivingEntity(event.getItemStack(),
                    event.getEntity(), cat, event.getHand());
            if (result.consumesAction()) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
            return;
        }

        if (event.getItemStack().getItem() instanceof AttributeDebugWandItem wand) {
            InteractionResult result = wand.interactLivingEntity(event.getItemStack(),
                    event.getEntity(), cat, event.getHand());
            if (result.consumesAction()) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
            return;
        }

        if (event.getItemStack().getItem() instanceof TraitDebugWandItem wand) {
            InteractionResult result = wand.interactLivingEntity(event.getItemStack(),
                    event.getEntity(), cat, event.getHand());
            if (result.consumesAction()) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
            return;
        }

        // This runs before vanilla cat interaction, so it works for seated pets
        // and inert living cat pancakes alike. Create's stationary
        // and contraption deployers both invoke the same Forge entity-interact
        // hook through their fake player, requiring no entity polling or mixin.
        if (event.getItemStack().getItem() instanceof TerminatorSuitItem suit) {
            // Let Create's fake player reach ItemStack#interactLivingEntity so
            // its deployer records a genuine successful entity interaction.
            if (event.getEntity() instanceof net.minecraftforge.common.util.FakePlayer) return;
            InteractionResult result = TerminatorSuitItem.tryEquip(
                    event.getItemStack(), event.getEntity(), cat, suit.outfit());
            if (result.consumesAction()) {
                event.setCancellationResult(result);
                event.setCanceled(true);
                return;
            }
        }

        boolean fakePlayer = event.getEntity() instanceof net.minecraftforge.common.util.FakePlayer;
        boolean mayRemoveOutfit = CatPoseData.isPancake(cat) || !fakePlayer;
        if (mayRemoveOutfit && CatClothesData.isEquipped(cat)
                && event.getItemStack().is(Items.SHEARS)) {
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            event.setCanceled(true);
            if (event.getLevel().isClientSide) return;

            CatClothesData.unequip(cat);
            cat.playSound(SoundEvents.SHEEP_SHEAR, 1.0F, 1.0F);
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.POOF,
                        cat.getX(), cat.getY() + (CatPoseData.isPancake(cat)
                                ? 0.12D : cat.getBbHeight() * 0.55D), cat.getZ(),
                        8, 0.22D, 0.05D, 0.22D, 0.02D);
            }
            return;
        }

        // Cat implements Forge's standard shearable interface via a mixin.
        // Invoke the item hook here because a tame cat's own interaction can
        // otherwise consume the click before ShearsItem sees it. Create's
        // FakePlayer/captureDrops wrapper remains intact, just like for sheep.
        if (event.getItemStack().is(Items.SHEARS)) {
            InteractionResult result = event.getItemStack().interactLivingEntity(
                    event.getEntity(), cat, event.getHand());
            if (result.consumesAction()) {
                event.setCancellationResult(result);
                event.setCanceled(true);
                return;
            }
        }

        if (cat.isBaby() && event.getItemStack().is(LaoWuMod.CAT_FOOD.get())) {
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            event.setCanceled(true);
            if (event.getLevel().isClientSide) return;

            cat.setAge(0);
            if (!event.getEntity().getAbilities().instabuild) event.getItemStack().shrink(1);
            cat.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.15F);
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        cat.getX(), cat.getY() + cat.getBbHeight() * 0.65D, cat.getZ(),
                        12, 0.35D, 0.35D, 0.35D, 0.05D);
            }
            return;
        }

        if (CatPoseData.isPancake(cat)) {
            if (!(event.getItemStack().getItem() instanceof ShovelItem)) {
                // A flattened cat is inert, but cannot be picked up bare-handed
                // or interacted with as an ordinary cat.
                event.setCancellationResult(InteractionResult.PASS);
                event.setCanceled(true);
                return;
            }

            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            event.setCanceled(true);
            if (event.getLevel().isClientSide) return;

            var player = event.getEntity();
            var pancake = CatPancakeItem.capture(cat);
            if (event.getItemStack().isEmpty()) {
                player.setItemInHand(event.getHand(), pancake);
            } else if (!player.addItem(pancake)) {
                player.drop(pancake, false);
            }
            cat.discard();
            event.getItemStack().hurtAndBreak(1, player,
                    brokenPlayer -> brokenPlayer.broadcastBreakEvent(event.getHand()));
            player.playSound(SoundEvents.ITEM_PICKUP, 0.8F, 0.8F);
            return;
        }

        // The scanner owns the profile gesture. Sneak-use with an empty hand is
        // therefore dedicated to the owned transport cat's logistics screen,
        // whether or not the cat is currently stationed on a Seat.
        var profilePlayer = event.getEntity();
        boolean transportOpening = event.getItemStack().isEmpty()
                && profilePlayer.isShiftKeyDown()
                && CatChestData.hasChest(cat)
                && cat.isTame() && cat.isOwnedBy(profilePlayer);
        if (transportOpening) {
            event.setCancellationResult(InteractionResult.sidedSuccess(
                    event.getLevel().isClientSide));
            event.setCanceled(true);
            if (event.getLevel().isClientSide) return;
            if (profilePlayer instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                                (containerId, playerInventory, ignored) ->
                                        new CatPackageMenu(containerId,
                                                playerInventory, cat),
                                Component.translatable("container.laowu.cat_chest")),
                        buffer -> {
                            buffer.writeVarInt(cat.getId());
                            buffer.writeUtf(CatChestData.getAddress(cat),
                                    CatChestData.MAX_ADDRESS_LENGTH);
                        });
                cat.playSound(SoundEvents.CHEST_OPEN, 0.6F, 1.2F);
            }
            return;
        }

        if (!cat.isTame() || !cat.isOwnedBy(event.getEntity())) return;

        var player = event.getEntity();
        var held = event.getItemStack();
        boolean flightOpening = CatClothesData.getOutfit(cat) == CatOutfitType.FLIGHT
                && !held.isEmpty() && player.isShiftKeyDown();
        if (flightOpening) {
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            event.setCanceled(true);
            if (event.getLevel().isClientSide) return;
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                        (containerId, playerInventory, ignored) -> ChestMenu.threeRows(
                                containerId, playerInventory, CatChestData.openContainer(cat)),
                        Component.translatable("container.laowu.flight_cat_chest")));
                cat.playSound(SoundEvents.CHEST_OPEN, 0.6F, 1.2F);
            }
            return;
        }
        // Other owned cats now fall through to their ordinary vanilla interaction.
    }

    /** Supplies stateful results for recipes whose NBT cannot be expressed by static JSON. */
    @SubscribeEvent
    public static void preserveTerminatorPancakeApplicationNbt(DeployerRecipeSearchEvent event) {
        var inventory = event.getInventory();
        var pancake = inventory.getItem(0);
        var held = inventory.getItem(1);
        if (pancake.is(LaoWuMod.CAT_TOTEM.get()) && held.is(Items.TOTEM_OF_UNDYING)) {
            if (!CatTotemItem.canLoad(pancake)) {
                event.setCanceled(true);
                return;
            }
            if (event.getRecipe() instanceof ProcessingRecipe<?> recipe
                    && recipe.getId().equals(LaoWuMod.id("cat_totem_charging"))) {
                ItemStack result = pancake.copyWithCount(1);
                CatTotemItem.addCharge(result);
                recipe.enforceNextResult(() -> result.copy());
            }
            return;
        }
        if (!pancake.is(LaoWuMod.CAT_PANCAKE.get())) return;
        if (held.is(LaoWuMod.CAT_FOOD.get())) {
            if (!CatPancakeItem.isBaby(pancake)) {
                event.setCanceled(true);
                return;
            }
            if (event.getRecipe() instanceof ProcessingRecipe<?> recipe
                    && recipe.getId().equals(LaoWuMod.id("cat_food_growing"))) {
                ItemStack result = pancake.copyWithCount(1);
                CatPancakeItem.makeAdult(result);
                recipe.enforceNextResult(() -> result.copy());
            }
            return;
        }
        CatOutfitType applyingType = held.getItem() instanceof TerminatorSuitItem suit
                ? suit.outfit() : CatOutfitType.NONE;
        boolean applying = applyingType != CatOutfitType.NONE;
        boolean shearing = held.is(Items.SHEARS);
        if (!applying && !shearing) return;

        CatOutfitType fittedType = CatPancakeItem.getOutfit(pancake);
        boolean fitted = fittedType != CatOutfitType.NONE;
        // The custom recipe ingredient intentionally ignores NBT so every cat
        // variant appears in JEI. Enforce tame/equipped state at recipe search.
        if (applying && (!CatPancakeItem.isTamed(pancake) || fitted)
                || shearing && !fitted) {
            event.setCanceled(true);
            return;
        }

        // Create has already selected the matching outfit application recipe
        // by the time this event is posted. Only replace its first rolled
        // result so the source cat variant, owner data and custom name survive.
        if (!(event.getRecipe() instanceof ProcessingRecipe<?> recipe)
                || !recipe.getId().getNamespace().equals(LaoWuMod.MOD_ID)) return;
        String expectedRecipe = applying
                ? applyingType.id() + "_suit_item_application"
                : fittedType.id() + "_suit_shearing";
        if (!recipe.getId().getPath().startsWith(expectedRecipe)) return;

        var result = pancake.copyWithCount(1);
        if (applying) CatPancakeItem.equipOutfit(result, applyingType);
        else CatPancakeItem.removeOutfit(result);
        recipe.enforceNextResult(() -> result.copy());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCatDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Cat cat && !cat.level().isClientSide) {
            CatBehaviorTraitEffects.applyMinorIllnessOnDeath(cat);
            CatOutfitType outfit = CatClothesData.getOutfit(cat);
            CatLogisticsBehavior.abort(cat);
            ModNetwork.setAudioSession(cat, false);
            CatProfileData.dropOnDeath(cat);
            CatChestData.dropOnDeath(cat);
            if (outfit != CatOutfitType.NONE
                    && cat.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
                cat.spawnAtLocation(CatPancakeItem.captureDeathDrop(cat));
            }
        }
    }

    /** Preserve vanilla quantity/looting behaviour, but turn every cat string drop into fur. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void replaceCatStringDropsWithFur(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Cat cat) || cat.level().isClientSide) return;
        for (var drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (!stack.is(Items.STRING)) continue;
            drop.setItem(new ItemStack(LaoWuMod.CAT_FUR.get(), stack.getCount()));
        }
        if (CatTraitData.ensure(cat).has(CatTrait.XIAOTING)
                && cat.getRandom().nextFloat() < 0.2F) {
            var templates = ForgeRegistries.ITEMS.getValues().stream()
                    .filter(SmithingTemplateItem.class::isInstance).toList();
            if (!templates.isEmpty()) {
                ItemStack template = new ItemStack(
                        templates.get(cat.getRandom().nextInt(templates.size())));
                event.getDrops().add(new ItemEntity(cat.level(), cat.getX(),
                        cat.getY() + 0.25D, cat.getZ(), template));
            }
        }
        if (event.getSource().getEntity() instanceof Cat hunter) {
            CatBehaviorTraitEffects.collectHuntedDrops(hunter, event.getDrops());
        }
    }
    private CommonEvents() {}
}
