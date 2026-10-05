package cn.laowu.mod.compat.create;
import cn.laowu.mod.*;
import cn.laowu.mod.network.ModNetwork;
import cn.laowu.mod.item.CatPancakeItem;
import cn.laowu.mod.item.FusionDebugWandItem;
import cn.laowu.mod.item.AttributeDebugWandItem;
import cn.laowu.mod.item.CatAttributeCanItem;
import cn.laowu.mod.item.CatTraitFishItem;
import cn.laowu.mod.item.BreedingOnlyCatCanItem;
import cn.laowu.mod.item.PheromoneCatFoodItem;
import cn.laowu.mod.item.TraitDebugWandItem;
import cn.laowu.mod.item.MaterialDebugWandItem;
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
import cn.laowu.mod.genetics.CatXiaotingRewards;
import cn.laowu.mod.genetics.CatGenome;
import cn.laowu.mod.genetics.CatGenomeData;
import cn.laowu.mod.genetics.CatMaterialRegistry;
import cn.laowu.mod.genetics.CatRegion;
import cn.laowu.mod.genetics.NaturalCatMaterialSpawner;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundSource;
import com.simibubi.create.AllDamageTypes;
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
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityEvent;
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
import net.minecraft.world.entity.MobSpawnType;
import com.simibubi.create.content.equipment.potatoCannon.PotatoProjectileEntity;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.WeakHashMap;

/** Event bodies that require Create classes are never loaded in standalone mode. */
public final class CreateCommonEvents {
    private static final String CAT_GRENADE_EXPLODED_TAG = "LaoWuCatGrenadeExploded";
    private static final float CAT_GRENADE_DAMAGE = 30.0F;
    private static final double CAT_GRENADE_RADIUS = 3.0D;
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
    private CreateCommonEvents() {}
}
