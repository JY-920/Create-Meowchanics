package cn.laowu.mod;

import cn.laowu.mod.network.ModNetwork;
import cn.laowu.mod.item.CatPancakeItem;
import cn.laowu.mod.item.CatToolBehavior;
import cn.laowu.mod.item.CatTotemItem;
import cn.laowu.mod.item.KimiArmorItem;
import cn.laowu.mod.item.TerminatorSuitItem;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.sounds.SoundSource;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllDamageTypes;
import com.simibubi.create.AllBlocks;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
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
import com.simibubi.create.content.equipment.potatoCannon.PotatoProjectileEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerRecipeSearchEvent;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import java.util.Collections;
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
                && CareerCatBehavior.isForbiddenTerminatorTarget(proposed)) {
            event.setNewAboutToBeSetTarget(null);
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
            disableTamedCatPanic(cat);
            CareerCatBehavior.tick(cat);
            if (CatPancakeBehavior.tickPancake(cat)) return;
            if (CatLogisticsBehavior.tick(cat)) {
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

    /** Stop vanilla's chosen panic path after a tamed cat actually takes damage. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void stopTamedCatDamagePanic(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Cat cat) || cat.level().isClientSide
                || !cat.isTame() || event.getNewDamage() <= 0.0F) return;
        if (CatClothesData.getOutfit(cat) != CatOutfitType.TERMINATOR
                || CareerCatBehavior.isForbiddenTerminatorTarget(cat.getLastHurtByMob())) {
            cat.setLastHurtByMob(null);
        }
        cat.getNavigation().stop();
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof Cat cat) {
            ModNetwork.syncToPlayer(player, cat, CatPoseData.getPose(cat));
            ModNetwork.syncAudioToPlayer(player, cat);
            CatChestData.syncToPlayer(player, cat);
            CatClothesData.syncToPlayer(player, cat);
        }
    }

    @SubscribeEvent
    public static void onCatInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Cat cat)) return;

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

        if (!cat.isTame() || !cat.isOwnedBy(event.getEntity())) return;

        var player = event.getEntity();
        var held = event.getItemStack();
        boolean flightOpening = CatClothesData.getOutfit(cat) == CatOutfitType.FLIGHT
                && player.isShiftKeyDown();
        if (flightOpening) {
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            event.setCanceled(true);
            if (event.getLevel().isClientSide) return;
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (containerId, playerInventory, ignored) -> ChestMenu.threeRows(
                                containerId, playerInventory, CatChestData.openContainer(cat)),
                        Component.translatable("container.laowu.flight_cat_chest")));
                cat.playSound(SoundEvents.CHEST_OPEN, 0.6F, 1.2F);
            }
            return;
        }
        // Only an empty-hand sneak interaction opens the inventory. Every other
        // interaction is left to vanilla so the owner can toggle sitting normally.
        boolean opening = CatChestData.hasChest(cat) && held.isEmpty() && player.isShiftKeyDown();
        if (!opening) return;

        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
        if (event.getLevel().isClientSide) return;

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                            (containerId, playerInventory, ignored) ->
                                    new CatPackageMenu(containerId, playerInventory, cat),
                            Component.translatable("container.laowu.cat_chest")),
                    buffer -> {
                        buffer.writeVarInt(cat.getId());
                        buffer.writeUtf(CatChestData.getAddress(cat), CatChestData.MAX_ADDRESS_LENGTH);
                    });
            cat.playSound(SoundEvents.CHEST_OPEN, 0.6F, 1.2F);
        }
    }

    /** Supplies component-bearing results for deployer recipes whose state cannot be static JSON. */
    @SubscribeEvent
    public static void preserveTerminatorPancakeApplicationNbt(DeployerRecipeSearchEvent event) {
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCatDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Cat cat && !cat.level().isClientSide) {
            CatOutfitType outfit = CatClothesData.getOutfit(cat);
            CatLogisticsBehavior.abort(cat);
            ModNetwork.setAudioSession(cat, false);
            CatChestData.dropOnDeath(cat);
            if (outfit != CatOutfitType.NONE
                    && cat.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
                cat.spawnAtLocation(CatPancakeItem.captureDeathDrop(cat));
            }
        }
    }

    /** Preserve vanilla quantities while replacing every cat string drop with fur. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void replaceCatStringDropsWithFur(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Cat cat) || cat.level().isClientSide) return;
        for (var drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.is(Items.STRING)) {
                drop.setItem(new ItemStack(LaoWuMod.CAT_FUR.get(), stack.getCount()));
            }
        }
    }
    private CommonEvents() {}
}
