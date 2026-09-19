package cn.laowu.mod;
import net.minecraft.core.registries.BuiltInRegistries;

import cn.laowu.mod.network.ModNetwork;
import cn.laowu.mod.item.CatPancakeItem;
import cn.laowu.mod.item.CatAttributeCanItem;
import cn.laowu.mod.item.CatTraitFishItem;
import cn.laowu.mod.item.BreedingOnlyCatCanItem;
import cn.laowu.mod.item.PheromoneCatFoodItem;
import cn.laowu.mod.item.CatScannerItem;
import cn.laowu.mod.item.CatToolBehavior;
import cn.laowu.mod.item.CatTotemItem;
import cn.laowu.mod.item.KimiArmorItem;
import cn.laowu.mod.item.TerminatorSuitItem;
import cn.laowu.mod.item.FusionDebugWandItem;
import cn.laowu.mod.item.AttributeDebugWandItem;
import cn.laowu.mod.item.TraitDebugWandItem;
import cn.laowu.mod.item.MaterialDebugWandItem;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatAttributeEffects;
import cn.laowu.mod.genetics.CatAttributeProfile;
import cn.laowu.mod.genetics.CatBreedingMode;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitEffects;
import cn.laowu.mod.genetics.CatBehaviorTraitEffects;
import cn.laowu.mod.genetics.CatTraitProfile;
import cn.laowu.mod.genetics.CatXiaotingRewards;
import cn.laowu.mod.genetics.CatGenome;
import cn.laowu.mod.genetics.CatGenomeData;
import cn.laowu.mod.genetics.CatMaterialRegistry;
import cn.laowu.mod.genetics.CatRegion;
import cn.laowu.mod.genetics.NaturalCatMaterialSpawner;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.sounds.SoundSource;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllDamageTypes;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ThrownPotion;
import com.simibubi.create.content.equipment.potatoCannon.PotatoProjectileEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerRecipeSearchEvent;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.WeakHashMap;

public final class CommonEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void cockroachHurt(LivingDamageEvent.Post event) {
        if (event.getNewDamage() > 0 && event.getEntity() instanceof Cat cat) CatCockroachSwarm.hurt(cat);
    }
    @SubscribeEvent
    public static void recordMusicDps(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        CatMusicDps.record(event.getSource(), event.getEntity(), event.getNewDamage());
    }

    @SubscribeEvent
    public static void pilotLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().getVehicle() instanceof cn.laowu.mod.entity.CatFlightCarrier carrier) carrier.release(true);
        if (event.getEntity().getVehicle() instanceof cn.laowu.mod.entity.CatDivingCarrier carrier) carrier.release();
    }
    private static final String CAT_GRENADE_EXPLODED_TAG = "LaoWuCatGrenadeExploded";
    private static final String TAME_PANIC_REMOVED_TAG = "LaoWuTamePanicRemoved";
    private static final Set<Cat> TAME_PANIC_DISABLED =
            Collections.newSetFromMap(new WeakHashMap<>());
    // Joining can happen inside ChunkMap's unfinished FULL promotion. Never
    // read weather/blocks there: Twilight Forest's rain hook requests that same
    // chunk and waits for the promotion that is still waiting for this event.
    // Weak entries also cover canceled joins/unloaded cats without retaining a world.
    private static final Set<Cat> PENDING_CAT_INITIALIZATION =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private static final float CAT_GRENADE_DAMAGE = 30.0F;
    private static final double CAT_GRENADE_RADIUS = 3.0D;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void suppressCareerProjectileKnockback(LivingKnockBackEvent event) {
        if (CatProjectileDamage.suppressesKnockback(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void preventForbiddenTargets(LivingChangeTargetEvent event) {
        if (CatAgentSmoke.hiddenFrom(event.getEntity(), event.getNewAboutToBeSetTarget())) {
            event.setNewAboutToBeSetTarget(null); return;
        }
        if (event.getEntity() instanceof Cat supporter && CatClothesData.getOutfit(supporter).isSupport()) {
            event.setNewAboutToBeSetTarget(null);
            return;
        }
        if (CatTeamRules.friendly(event.getEntity(), event.getNewAboutToBeSetTarget())) {
            event.setNewAboutToBeSetTarget(null);
            return;
        }
        LivingEntity proposed = event.getNewAboutToBeSetTarget();
        ItemStack helmet = proposed instanceof Player player
                ? player.getItemBySlot(EquipmentSlot.HEAD) : ItemStack.EMPTY;
        if (event.getEntity() instanceof Phantom
                && helmet.is(LaoWuMod.CAT_HELMET.get())
                && !CatToolBehavior.isExhausted(helmet)) {
            event.setNewAboutToBeSetTarget(null);
        }
        if (event.getEntity() instanceof Cat cat
                && CatClothesData.getOutfit(cat) == CatOutfitType.TERMINATOR
                && CareerCatBehavior.isForbiddenTerminatorTarget(cat, proposed)) {
            event.setNewAboutToBeSetTarget(null);
        }
        LivingEntity currentTarget = event.getNewAboutToBeSetTarget();
        if (!event.getEntity().level().isClientSide
                && event.getEntity() instanceof Enemy
                && currentTarget != null
                && !(currentTarget instanceof Cat targetCat
                && CatTraitData.ensure(targetCat).has(CatTrait.ATTENTION_MAGNET))) {
            Cat bait = event.getEntity().level().getEntitiesOfClass(Cat.class,
                            event.getEntity().getBoundingBox().inflate(16.0D),
                            candidate -> candidate.isAlive()
                                    && !CatPoseData.isPancake(candidate)
                                    && !CatAgentSmoke.hiddenFrom(event.getEntity(), candidate)
                                    && CatTraitData.ensure(candidate)
                                    .has(CatTrait.ATTENTION_MAGNET))
                    .stream()
                    .min(Comparator.comparingDouble(event.getEntity()::distanceToSqr))
                    .orElse(null);
            if (bait != null) event.setNewAboutToBeSetTarget(bait);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void preventConcealedAgentRetarget(LivingChangeTargetEvent event) {
        if (CatAgentSmoke.hiddenFrom(event.getEntity(), event.getNewAboutToBeSetTarget())) event.setNewAboutToBeSetTarget(null);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventPetTeamFriendlyFire(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Cat supporter && CatClothesData.getOutfit(supporter).isSupport()
                && !(event.getSource().is(DamageTypes.THORNS) && cn.laowu.mod.accessory.CatCommonAccessories.isReflecting())
                || CatTeamRules.friendly(event.getSource().getEntity(), event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void preventCatBootFallDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !event.getSource().is(DamageTypes.FALL)) return;
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (boots.is(LaoWuMod.CAT_BOOTS.get()) && !CatToolBehavior.isExhausted(boots)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventHeatResistantCatDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Cat cat && !cat.level().isClientSide
                && event.getSource().is(DamageTypeTags.IS_FIRE)
                && CatTraitEffects.isHeatResistant(cat)) {
            event.setCanceled(true);
            cat.setRemainingFireTicks(0);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void applyCainMarkAvoidance(LivingIncomingDamageEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof Cat cat
                && !cat.level().isClientSide
                && !DynamiteCatLastStand.isFinishing(cat)
                && event.getAmount() > 0.0F
                && CatTraitEffects.tryCainAvoid(cat, event.getSource())) {
            event.setCanceled(true);
        }
    }

    /** A charged Dynamite Cat remains at exactly one health until detonation. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void protectDynamiteCatLastStand(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Cat cat && !cat.level().isClientSide
                && DynamiteCatLastStand.isActive(cat)
                && !DynamiteCatLastStand.isFinishing(cat)
                && DynamiteCatLastStand.protectsFrom(event.getSource())) {
            event.setCanceled(true);
            cat.setHealth(1.0F);
        }
    }

    /** Exhausted armour remains equipped and rendered, but contributes no attributes. */
    @SubscribeEvent
    public static void disableExhaustedCatArmorAttributes(ItemAttributeModifierEvent event) {
        if (event.getItemStack().getItem() instanceof KimiArmorItem
                && CatToolBehavior.isExhausted(event.getItemStack())) {
            event.clearModifiers();
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        CatArmorPounceBehavior.tick(event.getServer());
        ServerConfig.tick(event.getServer());
        for (ServerLevel level : event.getServer().getAllLevels()) {
            NaturalCatMaterialSpawner.tick(level);
            CatHealingSmoke.flush(level);
            CatMedicalHealing.flush(level);
            CatMusicSupport.flush(level);
            CatAgentWatch.flush(level);
        }
    }

    /** Rarely maps ordinary natural cats to a block sampled from their dimension. */
    @SubscribeEvent
    public static void assignNaturalCatMaterial(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Cat cat)) return;
        MobSpawnType type = event.getSpawnType();
        if (type != MobSpawnType.NATURAL
                && type != MobSpawnType.CHUNK_GENERATION
                && type != MobSpawnType.STRUCTURE) return;
        NaturalCatMaterialSpawner.maybeMaterializeNaturalCat(cat, event.getLevel());
    }

    /**
     * Creepers already avoid cats through an AvoidEntityGoal. Register the same
     * kind of goal once when a creeper joins so cat boots work without a world
     * scan or per-tick player search owned by this mod.
     */
    @SubscribeEvent
    public static void makeCreepersFearCatBoots(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Creeper creeper)) return;
        creeper.goalSelector.addGoal(3, new AvoidEntityGoal<>(creeper, Player.class,
                6.0F, 1.0D, 1.2D,
                living -> living instanceof Player player
                        && player.getItemBySlot(EquipmentSlot.FEET).is(LaoWuMod.CAT_BOOTS.get())
                        && !CatToolBehavior.isExhausted(
                        player.getItemBySlot(EquipmentSlot.FEET))));
    }

    /** Queue only; both ensure() methods can indirectly refresh world-dependent attributes. */
    @SubscribeEvent
    public static void initializeCatTraits(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Cat cat) {
            PENDING_CAT_INITIALIZATION.add(cat);
        }
    }

    /** First normal tick after each join, including reused entities changing dimension. */
    private static void initializeCatAfterJoin(Cat cat) {
        if (!PENDING_CAT_INITIALIZATION.remove(cat)) return;
        CatProfileData.recoverInterruptedViewLock(cat);
        CatTraitData.ensure(cat);
        CatAttributeData.ensure(cat);
        CatAttributeEffects.refresh(cat);
    }

    /** Keeps Big Chonky Cat's collision and eye position aligned with its model. */
    @SubscribeEvent
    public static void resizeAppearanceTraitCat(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Cat cat)) return;
        int level = CatTraitData.read(cat)
                .map(profile -> profile.level(CatTrait.BIG_CHONKY_CAT))
                .orElse(0);
        if (level <= 0) return;
        float scale = CatTrait.BIG_CHONKY_CAT.bigCatScalePercent(level) / 100.0F;
        // In 1.21 EntityDimensions carries eye height, so scaling it adjusts both.
        event.setNewSize(event.getNewSize().scale(scale));
    }

    @SubscribeEvent
    public static void rewriteHissingPotionTooltip(ItemTooltipEvent event) {
        HissingPotionTooltip.rewrite(
                event.getItemStack().getOrDefault(DataComponents.POTION_CONTENTS,
                        net.minecraft.world.item.alchemy.PotionContents.EMPTY)
                        .getAllEffects(),
                event.getToolTip());
    }

    /**
     * Vanilla Strength installs a persistent ATTACK_DAMAGE attribute modifier.
     * Hissing Attack intentionally differs only in lifetime: the next accepted
     * direct melee hit receives +10 * level^2 damage and immediately consumes
     * the effect. Projectiles never have the player as their direct entity.
     */
    @SubscribeEvent
    public static void onHissingEmpoweredAttack(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)
                || event.getSource().getDirectEntity() != player
                || player.level().isClientSide
                || event.getAmount() <= 0.0F) return;

        var active = player.getEffect(LaoWuMod.HISSING_ATTACK);
        if (active == null) return;
        int level = active.getAmplifier() + 1;
        event.setAmount(event.getAmount() + 10.0F * level * level);
        player.removeEffect(LaoWuMod.HISSING_ATTACK);
    }

    /** Auto-attach cats react to an actual player attack instead of polling players. */
    @SubscribeEvent
    public static void notifyAutoAttachCats(LivingIncomingDamageEvent event) {
        if (event.getAmount() > 0.0F
                && event.getSource().getDirectEntity() instanceof Player player) {
            CatBehaviorTraitEffects.notifyPlayerAttack(player, event.getEntity());
        }
    }

    /** Child-eating cats deal double final melee damage to kittens. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void amplifyFilicideDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getDirectEntity() instanceof Cat attacker
                && event.getAmount() > 0.0F) {
            event.setAmount(ServerConfig.scaleDamage(event.getAmount(),
                    CatBehaviorTraitEffects.childAttackMultiplier(
                    attacker, event.getEntity())));
        }
    }

    /** Every third accepted enhanced-sword hit arms Hissing Attack I for the next hit. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEmpoweredCatSwordAttack(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)
                || event.getSource().getDirectEntity() != player
                || player.level().isClientSide || event.getAmount() <= 0.0F) return;
        ItemStack sword = player.getMainHandItem();
        if (!CatToolBehavior.recordSwordHit(sword)) return;
        player.addEffect(new MobEffectInstance(LaoWuMod.HISSING_ATTACK,
                20 * 60 * 3, 0, false, true, true));
    }

    /** Luck rolls a cat attack critical; Intelligence supplies its scaling multiplier. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void applyCatAttributeCriticalHit(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Cat cat)
                || cat.level().isClientSide
                || event.getSource().is(DamageTypes.THORNS)
                || event.getAmount() <= 0.0F
                || !(CatAgentCombatGoal.guaranteedCritical(cat, event.getEntity(), event.getSource().getDirectEntity())
                || CatAttributeEffects.rollCriticalHit(cat))) return;

        cn.laowu.mod.accessory.CatCommonAccessories.critical(cat, event.getEntity(), event.getSource());
        event.setAmount(CatAttributeEffects.criticalDamage(event.getAmount(), cat));
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
    public static void onExhaustedCatToolAttack(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)
                || event.getSource().getDirectEntity() != attacker) return;
        if (CatToolBehavior.isExhausted(attacker.getMainHandItem())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExhaustedCatToolBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack held = event.getEntity().getMainHandItem();
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
        if (isExhaustedToolInteraction(event)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExhaustedCatToolRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!isExhaustedToolInteraction(event)) return;
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExhaustedCatToolUse(PlayerInteractEvent.RightClickItem event) {
        if (!isExhaustedToolInteraction(event)) return;
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExhaustedCatToolEntityUse(PlayerInteractEvent.EntityInteract event) {
        if (!isExhaustedToolInteraction(event)) return;
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
    }

    private static boolean isExhaustedToolInteraction(PlayerInteractEvent event) {
        return CatToolBehavior.isExhausted(event.getItemStack());
    }

    @SubscribeEvent
    public static void onHissingGasBucketInteract(PlayerInteractEvent.RightClickBlock event) {
        // Intercept before the box opens its menu; other Create filter interactions are unchanged.
        if (event.getItemStack().getItem() instanceof cn.laowu.mod.item.CatFilterItem filter
                && event.getLevel().getBlockEntity(event.getPos()) instanceof cn.laowu.mod.create.WishAdoptionBoxBlockEntity) {
            event.setCancellationResult(filter.useOn(new net.minecraft.world.item.context.UseOnContext(
                    event.getEntity(), event.getHand(), event.getHitVec())));
            event.setCanceled(true);
            return;
        }
        if (event.getItemStack().getItem() instanceof cn.laowu.mod.item.CatStorageBoxItem box) {
            var result = box.useOn(new net.minecraft.world.item.context.UseOnContext(
                    event.getEntity(), event.getHand(), event.getHitVec()));
            if (result.consumesAction()) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
            return;
        }
        if (event.getEntity().isShiftKeyDown()
                && !(event.getItemStack().getItem() instanceof cn.laowu.mod.item.CatLaserPointerItem)
                && event.getLevel().getBlockEntity(event.getPos()) instanceof cn.laowu.mod.create.CatCarrierBlockEntity box) {
            event.setCancellationResult(box.interact(event.getEntity(), event.getHand()));
            event.setCanceled(true);
            return;
        }
        if (event.getItemStack().getItem() instanceof cn.laowu.mod.item.CatLaserPointerItem pointer) {
            event.setCancellationResult(pointer.use(event.getLevel(), event.getEntity(), event.getHand()).getResult());
            event.setCanceled(true);
            return;
        }
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

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onCardboardSwordAttack(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof Cat cat)
                || !isCardboardSword(event.getEntity().getMainHandItem())) return;

        // The cardboard sword can deal zero damage. NeoForge 1.21 may reject
        // that hit before LivingIncomingDamageEvent is posted, while the
        // player/deployer attack event still fires for the actual contact.
        event.setCanceled(true);
        if (!cat.level().isClientSide) CatPancakeBehavior.flatten(cat);
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
                || CatTraitData.read(projectile.getItem())
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
            target.igniteForSeconds(6);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z,
                8, 1.3D, 0.8D, 1.3D, 0.04D);
        level.sendParticles(ParticleTypes.FLAME, impact.x, impact.y, impact.z,
                45, 1.7D, 1.0D, 1.7D, 0.06D);
        level.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.6F, 0.7F);
    }

    @SubscribeEvent
    public static void onCatAttacked(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Cat cat) || cat.level().isClientSide) return;

        if (event.getSource().is(DamageTypes.FALLING_ANVIL)
                || event.getSource().is(AllDamageTypes.ROLLER)) {
            event.setCanceled(true);
            CatPancakeBehavior.flatten(cat);
            return;
        }

        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if (!isCardboardSword(attacker.getMainHandItem())) return;

        event.setCanceled(true);
        CatPancakeBehavior.flatten(cat);
    }

    private static boolean isCardboardSword(ItemStack stack) {
        return AllItems.CARDBOARD_SWORD.isIn(stack);
    }

    @SubscribeEvent
    public static void suppressVanillaCatSounds(PlayLevelSoundEvent.AtEntity event) {
        if (!(event.getEntity() instanceof Cat cat)
                || (!CatPoseData.isHissing(cat) && !CatPoseData.isPancake(cat)
                && !HissingCatBehavior.isFighting(cat))) return;

        var location = event.getSound().value().getLocation();
        if (location.getNamespace().equals("minecraft") && location.getPath().startsWith("entity.cat.")) {
            // NeoForge exposes a mutable replacement volume for this sound event.
            event.setNewVolume(0.0F);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Cat cat && !cat.level().isClientSide) {
            initializeCatAfterJoin(cat);
            if (CatTeamRules.friendly(cat, cat.getTarget())) cat.setTarget(null);
            if (DynamiteCatLastStand.tick(cat)) return;
            disableTamedCatPanic(cat);
            if (!ServerConfig.catsHiss()) HissingCatBehavior.stopDisabledHissing(cat);
            CatAttributeEffects.tick(cat);
            cn.laowu.mod.accessory.CatAccessories.tick(cat);
            CatTraitEffects.tick(cat);
            cn.laowu.mod.genetics.CatTraitHooks.tick(cat);
            CatCockroachSwarm.tick(cat);
            CatAgentSmoke.tick(cat);
            CatPilotFlight.recover(cat);
            CatDivingMount.recover(cat);
            CatMusicRecords.tick(cat);
            if (CatPilotFlight.carried(cat) || CatDivingMount.carried(cat)) {
                cat.setTarget(null);
                CareerCatBehavior.tick(cat);
                return;
            }
            if (CatProfileData.isBeingViewed(cat)) {
                cat.getNavigation().stop();
                cat.setDeltaMovement(0.0D, cat.getDeltaMovement().y, 0.0D);
                return;
            }
            CatCombatControl.tick(cat);
            CareerCatBehavior.tick(cat);
            if (CatMedicalHealing.casting(cat) || CatMusicSupport.performing(cat)) {
                CatMedicalHealing.holdStill(cat);
                return; // Keep hissing/behaviour traits from replacing the caster's healing pose.
            }
            CatLaserCommands.tick(cat);
            if (CatEngineeringCombat.deployed(cat)) return;
            if (CatLaserCommands.hasOrder(cat)) return;
            if (CatPancakeBehavior.tickPancake(cat)) return;
            if (CatEngineeringBehavior.findCrank(cat) != null) return;
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

    /** Remove vanilla's random flee goal once for each live tamed-cat instance. */
    private static void disableTamedCatPanic(Cat cat) {
        if (!cat.isTame() || !TAME_PANIC_DISABLED.add(cat)) return;
        cat.goalSelector.removeAllGoals(goal -> goal instanceof PanicGoal);
        cat.getPersistentData().remove(TAME_PANIC_REMOVED_TAG);
    }

    @SubscribeEvent
    public static void onCatHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Cat cat) || cat.level().isClientSide
                || CatLogisticsBehavior.isActive(cat)
                || HissingCatBehavior.isFighting(cat) || !CatPoseData.isHissing(cat)) return;
        Cat partner = HissingCatBehavior.getCurrentPartner(cat);
        if (partner != null && CatPoseData.isHissing(partner)) {
            HissingCatBehavior.startFight(cat, partner);
        }
    }

    /** Nine Lives must run in the mutable final-damage phase on NeoForge 21.1. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void applyCatTraitNineLives(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Cat cat) || cat.level().isClientSide
                || event.getNewDamage() <= 0.0F
                || DynamiteCatLastStand.isFinishing(cat)) return;
        // NeoForge's Pre event precedes absorption, unlike Forge 1.20's
        // LivingDamageEvent. Test only the pending health loss, and leave the
        // absorption-sized portion so absorption hearts are consumed as before.
        float absorbed = Math.min(event.getNewDamage(), cat.getAbsorptionAmount());
        float healthDamage = Math.max(0.0F, event.getNewDamage() - absorbed);
        if (healthDamage > 0.0F && CatTraitEffects.tryNineLives(cat, healthDamage)) {
            cn.laowu.mod.accessory.CatCommonAccessories.prevented(cat);
            event.setNewDamage(absorbed);
        }
    }

    /** Stop vanilla's chosen panic path after a tamed cat actually takes damage. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void stopTamedCatDamagePanic(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Cat cat) || cat.level().isClientSide
                || !cat.isTame() || event.getNewDamage() <= 0.0F) return;
        CatOutfitType outfit = CatClothesData.getOutfit(cat);
        boolean activeCareer = outfit != CatOutfitType.NONE
                && !CareerCatBehavior.isCombatResting(cat);
        boolean careerCanFight = CareerCatBehavior.canParticipateInCombat(cat);
        if (activeCareer) {
            CareerCatBehavior.alertMeleeProtectors(cat, event.getSource().getEntity());
        }
        if (!careerCanFight
                || CareerCatBehavior.isForbiddenTerminatorTarget(cat, cat.getLastHurtByMob())) {
            cat.setLastHurtByMob(null);
        }
        cat.getNavigation().stop();
    }

    /** Runs after accepted final damage; reactive traits observe actual health loss. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void applyCatTraitThorns(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof Cat cat && !cat.level().isClientSide
                && event.getNewDamage() > 0.0F) {
            if (DynamiteCatLastStand.isFinishing(cat)) return;
            CatBehaviorTraitEffects.onSelectedElderHurt(
                    cat, event.getSource().getEntity());
            CatTraitEffects.onAcceptedDamage(cat);
            CatTraitEffects.tryReflectDamage(cat, event.getSource());
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer observer && event.getTarget() instanceof LivingEntity watched)
            CatAgentWatch.syncTo(watched, observer);
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof Cat cat) {
            ModNetwork.syncToPlayer(player, cat, CatPoseData.getPose(cat));
            ModNetwork.syncCatAccessories(cat, player, cn.laowu.mod.accessory.CatAccessories.state(cat));
            ModNetwork.syncAudioToPlayer(player, cat);
            CatChestData.syncToPlayer(player, cat);
            CatMusicSupport.syncTo(cat, player);
            CatClothesData.syncToPlayer(player, cat);
            CatMusicRecords.syncTo(cat, player);
            CatCockroachSwarm.syncTo(cat, player);
            CatAgentMeleeMotion.syncTo(cat, player);
            if (CatGenomeData.has(cat)) {
                ModNetwork.syncCatGenomeToPlayer(player, cat);
            }
            CatAttributeData.ensure(cat);
            ModNetwork.syncCatAttributesToPlayer(player, cat);
            CatTraitData.ensure(cat);
            ModNetwork.syncCatTraitsToPlayer(player, cat);
            ModNetwork.syncCatTraitStateToPlayer(player, cat);
            ModNetwork.syncDynamiteLastStandToPlayer(player, cat);
        }
    }

    /** Natural kittens inherit genetics and traits through the normal contract. */
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
        CatGenome genome = CatGenome.fuse(CatGenomeData.ensure(first),
                CatGenomeData.ensure(second), CatMaterialRegistry.mutationMaterials(),
                child.getRandom());
        CatGenomeData.set(child, genome);
        BuiltInRegistries.CAT_VARIANT.getHolder(
                genome.material(CatRegion.BODY_FRONT)).ifPresent(child::setVariant);
    }

    @SubscribeEvent
    public static void onCatInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof Cat cat
                && event.getItemStack().getItem() instanceof cn.laowu.mod.item.CatPouchItem pouch) {
            event.setCancellationResult(pouch.interactLivingEntity(
                    event.getItemStack(), event.getEntity(), cat, event.getHand()));
            event.setCanceled(true);
            return;
        }
        if (event.getTarget() instanceof Cat cat
                && event.getItemStack().getItem() instanceof cn.laowu.mod.item.CatStorageBoxItem box) {
            event.setCancellationResult(box.interactLivingEntity(event.getItemStack(), event.getEntity(), cat, event.getHand()));
            event.setCanceled(true);
            return;
        }
        if (event.getItemStack().getItem() instanceof cn.laowu.mod.item.CatLaserPointerItem pointer) {
            event.setCancellationResult(pointer.use(event.getLevel(), event.getEntity(), event.getHand()).getResult());
            event.setCanceled(true);
            return;
        }
        if (event.getTarget() instanceof ItemEntity itemEntity
                && event.getItemStack().getItem() instanceof CatTraitFishItem fish
                && itemEntity.getItem().is(LaoWuMod.CAT_PANCAKE.get())) {
            InteractionResult result = fish.interactItemEntity(
                    event.getItemStack(), event.getEntity(), itemEntity);
            if (result.consumesAction()) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
            return;
        }
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
        InteractionResult pilotRide = CatPilotFlight.interact(cat, event.getEntity(), event.getHand());
        if (pilotRide == InteractionResult.PASS) pilotRide = CatDivingMount.interact(cat, event.getEntity(), event.getHand());
        if (pilotRide != InteractionResult.PASS) {
            event.setCancellationResult(pilotRide);
            event.setCanceled(true);
            return;
        }

        if (CatBehaviorTraitEffects.refusesFood(cat, event.getItemStack())
                || (!cat.isBaby() && (cat.isFood(event.getItemStack())
                || event.getItemStack().getItem() instanceof BreedingOnlyCatCanItem)
                && CatBehaviorTraitEffects.cannotBreed(cat))) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        if (event.getItemStack().getItem() instanceof CatScannerItem scanner) {
            InteractionResult result = scanner.interactLivingEntity(event.getItemStack(),
                    event.getEntity(), cat, event.getHand());
            if (result.consumesAction()) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
            return;
        }

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

        if (event.getItemStack().getItem() instanceof MaterialDebugWandItem wand) {
            InteractionResult result = wand.interactLivingEntity(event.getItemStack(),
                    event.getEntity(), cat, event.getHand());
            if (result.consumesAction()) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
            return;
        }

        if (event.getItemStack().getItem() instanceof PheromoneCatFoodItem food) {
            InteractionResult result = food.interactLivingEntity(event.getItemStack(),
                    event.getEntity(), cat, event.getHand());
            event.setCancellationResult(result);
            event.setCanceled(true);
            return;
        }

        if (event.getItemStack().getItem() instanceof BreedingOnlyCatCanItem can) {
            InteractionResult result = can.interactLivingEntity(event.getItemStack(),
                    event.getEntity(), cat, event.getHand());
            event.setCancellationResult(result);
            event.setCanceled(true);
            return;
        }

        if (event.getItemStack().getItem() instanceof CatAttributeCanItem can) {
            InteractionResult result = can.interactLivingEntity(event.getItemStack(),
                    event.getEntity(), cat, event.getHand());
            event.setCancellationResult(result);
            event.setCanceled(true);
            return;
        }

        if (event.getItemStack().getItem() instanceof CatTraitFishItem fish) {
            InteractionResult result = fish.interactLivingEntity(event.getItemStack(),
                    event.getEntity(), cat, event.getHand());
            event.setCancellationResult(result);
            event.setCanceled(true);
            return;
        }

        // Let Create's fake player reach the item hook so a deployer records a
        // genuine entity interaction, while normal players are handled here.
        if (event.getItemStack().getItem() instanceof TerminatorSuitItem suit) {
            if (event.getEntity() instanceof net.neoforged.neoforge.common.util.FakePlayer) return;
            InteractionResult result = TerminatorSuitItem.tryEquip(
                    event.getItemStack(), event.getEntity(), cat, suit.outfit());
            if (result.consumesAction()) {
                event.setCancellationResult(result);
                event.setCanceled(true);
                return;
            }
        }

        boolean fakePlayer = event.getEntity()
                instanceof net.neoforged.neoforge.common.util.FakePlayer;
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

        // A tame cat can otherwise consume the click before ShearsItem reaches
        // the standard NeoForge IShearable interface supplied by our mixin.
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
            if (CatTraitData.ensure(cat).has(CatTrait.LOLI)) return;
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
            EquipmentSlot slot = event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            event.getItemStack().hurtAndBreak(1, player, slot);
            player.playSound(SoundEvents.ITEM_PICKUP, 0.8F, 0.8F);
            return;
        }

        // Empty-hand sneak-use belongs to the transport cat logistics screen.
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
                serverPlayer.openMenu(new SimpleMenuProvider(
                                (containerId, playerInventory, ignored) ->
                                        new CatPackageMenu(containerId, playerInventory, cat),
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
        boolean flightOpening = CatClothesData.getOutfit(cat) == CatOutfitType.FLIGHT
                && player.isShiftKeyDown();
        if (flightOpening) {
            // Contents are server-only NBT. Consume the gesture on both sides,
            // then let the server decide whether a legacy backpack may open.
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            event.setCanceled(true);
            if (event.getLevel().isClientSide) return;
            if (player instanceof ServerPlayer serverPlayer) {
                var inventory = CatChestData.openContainer(cat);
                if (inventory.isEmpty()) return;
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (containerId, playerInventory, ignored) -> ChestMenu.threeRows(
                                containerId, playerInventory, inventory),
                        Component.translatable("container.laowu.flight_cat_chest")));
                cat.playSound(SoundEvents.CHEST_OPEN, 0.6F, 1.2F);
            }
            return;
        }
        // Other owned cats fall through to their ordinary vanilla interaction.
    }

    /** Supplies component-bearing results for deployer recipes whose state cannot be static JSON. */
    @SubscribeEvent
    public static void preserveStatefulDeployerApplicationNbt(DeployerRecipeSearchEvent event) {
        var inventory = event.getInventory();
        ItemStack pancake = inventory.getItem(0);
        ItemStack held = inventory.getItem(1);
        var holder = event.getRecipe();

        if (pancake.is(LaoWuMod.CAT_TOTEM.get()) && held.is(Items.TOTEM_OF_UNDYING)) {
            if (!CatTotemItem.canLoad(pancake)) {
                event.setCanceled(true);
                return;
            }
            if (holder != null && holder.value() instanceof ProcessingRecipe<?, ?> recipe
                    && holder.id().equals(LaoWuMod.id("cat_totem_charging"))) {
                ItemStack result = pancake.copyWithCount(1);
                CatTotemItem.addCharge(result);
                recipe.enforceNextResult(() -> result.copy());
            }
            return;
        }
        if (!pancake.is(LaoWuMod.CAT_PANCAKE.get())) return;

        var blockMaterial = CatMaterialRegistry.blockMaterial(held);
        if (blockMaterial.isPresent()) {
            var level = event.getBlockEntity().getLevel();
            if (level == null) return;
            event.addRecipe(() -> asDeployerRecipe(level.getRecipeManager().byKey(
                    LaoWuMod.id("cat_pancake_block_material_deploying"))), 200);
            holder = event.getRecipe();
            if (holder != null && holder.value() instanceof ProcessingRecipe<?, ?> recipe
                    && holder.id().equals(LaoWuMod.id(
                    "cat_pancake_block_material_deploying"))) {
                ItemStack result = pancake.copyWithCount(1);
                CatGenomeData.set(result, CatGenome.uniform(blockMaterial.get()));
                recipe.enforceNextResult(() -> result.copy());
            }
            return;
        }

        if (held.getItem() instanceof PheromoneCatFoodItem) {
            if (CatPancakeItem.hasOwner(pancake)) {
                event.setCanceled(true);
                return;
            }
            if (holder == null || !(holder.value() instanceof ProcessingRecipe<?, ?> recipe)
                    || !holder.id().equals(LaoWuMod.id(
                    "pheromone_cat_food_item_application"))) return;

            var level = event.getBlockEntity().getLevel();
            if (level == null) return;
            var ownerId = PheromoneCatFoodItem.resolveOwner(level.getServer(), held);
            if (ownerId.isEmpty()) {
                event.setCanceled(true);
                return;
            }

            ItemStack result = pancake.copyWithCount(1);
            CatPancakeItem.setOwner(result, ownerId.get(), PheromoneCatFoodItem.ownerName(held).orElse(""));
            CatPancakeItem.makeAdult(result);
            recipe.enforceNextResult(() -> result.copy());
            return;
        }

        if (held.getItem() instanceof CatAttributeCanItem can) {
            if (holder == null || !(holder.value() instanceof ProcessingRecipe<?, ?> recipe)) return;
            var itemId = BuiltInRegistries.ITEM.getKey(held.getItem());
            if (itemId == null || !itemId.getNamespace().equals(LaoWuMod.MOD_ID)
                    || !holder.id().equals(LaoWuMod.id(
                    itemId.getPath() + "_item_application"))) return;

            var level = event.getBlockEntity().getLevel();
            if (level == null) return;
            ItemStack result = pancake.copyWithCount(1);
            CatAttributeProfile profile = CatAttributeData.ensure(result, level.getRandom());
            var trained = can.train(profile);
            if (trained.isEmpty()) {
                event.setCanceled(true);
                return;
            }
            CatAttributeData.set(result, trained.get());
            recipe.enforceNextResult(() -> result.copy());
            return;
        }

        if (held.getItem() instanceof CatTraitFishItem fish) {
            if (holder == null || !(holder.value() instanceof ProcessingRecipe<?, ?> recipe)) return;
            var itemId = BuiltInRegistries.ITEM.getKey(held.getItem());
            if (itemId == null || !itemId.getNamespace().equals(LaoWuMod.MOD_ID)
                    || !holder.id().equals(LaoWuMod.id(
                    itemId.getPath() + "_item_application"))) return;

            var level = event.getBlockEntity().getLevel();
            if (level == null) return;
            ItemStack result = pancake.copyWithCount(1);
            CatTraitProfile profile = CatTraitData.ensure(result, level.getRandom());
            var upgraded = fish.upgrade(profile, level.getRandom());
            if (upgraded.isEmpty()) {
                event.setCanceled(true);
                return;
            }
            CatTraitData.set(result, upgraded.get());
            recipe.enforceNextResult(() -> result.copy());
            return;
        }

        if (held.is(LaoWuMod.CAT_FOOD.get())) {
            if (!CatPancakeItem.isBaby(pancake)
                    || CatTraitData.read(pancake)
                    .map(profile -> profile.has(CatTrait.LOLI)).orElse(false)) {
                event.setCanceled(true);
                return;
            }
            if (holder != null && holder.value() instanceof ProcessingRecipe<?, ?> recipe
                    && holder.id().equals(LaoWuMod.id("cat_food_growing"))) {
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
        if (applying && (!CatPancakeItem.isTamed(pancake) || fitted)
                || shearing && !fitted) {
            event.setCanceled(true);
            return;
        }

        if (holder == null || !(holder.value() instanceof ProcessingRecipe<?, ?> recipe)
                || !holder.id().getNamespace().equals(LaoWuMod.MOD_ID)) return;
        String expectedRecipe = applying
                ? applyingType.id() + "_suit_item_application"
                : fittedType.id() + "_suit_shearing";
        if (!holder.id().getPath().startsWith(expectedRecipe)) return;

        ItemStack result = pancake.copyWithCount(1);
        if (applying) CatPancakeItem.equipOutfit(result, applyingType);
        else CatPancakeItem.removeOutfit(result);
        recipe.enforceNextResult(() -> result.copy());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static java.util.Optional<? extends net.minecraft.world.item.crafting.RecipeHolder<? extends net.minecraft.world.item.crafting.Recipe<? extends net.minecraft.world.item.crafting.RecipeInput>>>
    asDeployerRecipe(java.util.Optional<net.minecraft.world.item.crafting.RecipeHolder<?>> recipe) {
        return (java.util.Optional) recipe;
    }

    /** Nine Lives/totems resolve first; only a genuine remaining death starts the charge. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beginDynamiteCatLastStand(LivingDeathEvent event) {
        if (event.getEntity() instanceof Cat cat && !cat.level().isClientSide
                && DynamiteCatLastStand.tryBegin(cat, event.getSource())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCatDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Cat cat && !cat.level().isClientSide) {
            CatBehaviorTraitEffects.applyMinorIllnessOnDeath(cat);
            CatOutfitType outfit = CatClothesData.getOutfit(cat);
            CatLogisticsBehavior.abort(cat);
            ModNetwork.setAudioSession(cat, false);
            boolean split = CatCockroachSplit.trySplit(cat,event.getSource());
            int outcome = cat.isTame() ? ServerConfig.deathOutcome() : ServerConfig.DEATH_ITEM;
            CatProfileData.dropOnDeath(cat,!split && outcome != ServerConfig.DEATH_NONE);
            CatChestData.dropOnDeath(cat);
            if (!split && outcome != ServerConfig.DEATH_NONE && (cat.isTame() || outfit != CatOutfitType.NONE)
                    && cat.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
                var pancake = CatPancakeItem.captureDeathDrop(cat);
                if (outcome != ServerConfig.DEATH_ENTITY || !CatPancakeItem.spawnDeathPancake(cat, pancake))
                    cat.spawnAtLocation(pancake);
            }
        }
    }

    /** Preserve vanilla quantities while replacing every cat string drop with fur. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void replaceCatStringDropsWithFur(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEntity() instanceof Cat cat) {
            for (var drop : event.getDrops()) {
                ItemStack stack = drop.getItem();
                if (stack.is(Items.STRING)) {
                    drop.setItem(new ItemStack(LaoWuMod.CAT_FUR.get(), stack.getCount()));
                }
            }
            CatXiaotingRewards.addDeathTemplate(cat, event.getDrops());
        }
        if (event.getSource().getEntity() instanceof Cat hunter) {
            CatBehaviorTraitEffects.collectHuntedDrops(hunter, event.getDrops());
        }
    }
    private CommonEvents() {}
}
