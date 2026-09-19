package cn.laowu.mod.accessory;

import cn.laowu.mod.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cat;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Loader adapter only. All mechanic rules live in CatAccessories. */
public final class CatAccessoryEvents {
    @SubscribeEvent public static void reload(AddReloadListenerEvent event) {
        event.addListener(new CatAccessoryRegistry());
        event.addListener(new cn.laowu.mod.genetics.CatTraitRegistry());
    }
    @SubscribeEvent public static void sync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            ModNetwork.sendAccessoryDefinitions(event.getPlayer());
            ModNetwork.sendTraitDefinitions(event.getPlayer());
        } else for (ServerPlayer player : event.getPlayerList().getPlayers()) {
            ModNetwork.sendAccessoryDefinitions(player);
            ModNetwork.sendTraitDefinitions(player);
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        CatAccessories.reset();
        cn.laowu.mod.genetics.CatTraitHooks.reset();
        cn.laowu.mod.genetics.CatTraitRegistry.resetServer();
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tooltip(ItemTooltipEvent event) {
        CatAccessoryTooltip.append(event.getItemStack(), event.getToolTip());
    }
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void avoid(LivingAttackEvent event) {
        if (event.getAmount() > 0 && event.getEntity() instanceof Cat cat && !cat.level().isClientSide
                && CatAccessories.avoidDamage(cat, event.getSource())) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void knockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof Cat cat && CatAccessories.knockbackImmune(cat)) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void target(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob)
            event.setNewTarget(CatAccessories.preferTarget(mob, event.getNewTarget()));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void attack(LivingDamageEvent event) {
        CatAccessoryHooks.afterDamage(event.getEntity(), event.getSource(), event.getAmount());
        cn.laowu.mod.genetics.CatTraitHooks.afterDamage(event.getEntity(), event.getSource(), event.getAmount());
        CatAccessories.acceptedAttack(event.getEntity(), event.getSource(), event.getAmount());
    }
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void generalProtection(LivingDamageEvent event) {
        event.setAmount(CatCommonAccessories.finalDamage(event.getEntity(), event.getSource(), event.getAmount(), () -> !event.isCanceled()));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void generalHealing(LivingHealEvent event) {
        event.setAmount(CatCommonAccessories.receivedHealing(event.getEntity(), event.getAmount()));
        if (event.getEntity() instanceof Cat cat)
            event.setAmount(cn.laowu.mod.genetics.CatTraitHooks.beforeHeal(cat, event.getAmount()));
    }
    private CatAccessoryEvents() {}
    @SubscribeEvent
    public static void scriptTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            CatAccessoryHooks.flushAfterDamage();
            cn.laowu.mod.genetics.CatTraitHooks.flush();
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void scriptBeforeDamage(LivingHurtEvent event) {
        float amount = CatCommonAccessories.beforeDamage(event.getEntity(), event.getSource(), event.getAmount());
        amount = CatAccessories.mitigateMovingDamage(event.getEntity(), event.getSource(), amount);
        amount = CatAccessoryHooks.beforeDamage(event.getEntity(), event.getSource(), amount);
        amount = cn.laowu.mod.genetics.CatTraitHooks.beforeDamage(event.getEntity(), event.getSource(), amount);
        if (amount <= 0) event.setCanceled(true); else event.setAmount(amount);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void scriptKill(LivingDeathEvent event) {
        CatAccessoryHooks.kill(event.getEntity(), event.getSource());
        cn.laowu.mod.genetics.CatTraitHooks.death(event.getEntity(), event.getSource());
    }
}
