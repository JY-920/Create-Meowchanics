package cn.laowu.mod;

import cn.laowu.mod.genetics.CatAttributeEffects;
import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.network.AgentWatchPacket;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.monster.Enemy;
import java.util.*;

/** Seated agents reveal loaded hostiles. All observers are merged before synchronizing each target. */
public final class CatAgentWatch {
    public static final int SCAN_TICKS = 20, VISUAL_TICKS = 40;
    public static final double MAX_RADIUS = 128;
    private static final Map<ServerLevel, Watch> WORLDS = new WeakHashMap<>();
    private static final class Watch {
        final Set<UUID> sources = new HashSet<>();
        Set<UUID> targets = new HashSet<>();
        long nextScan;
    }
    public static double radius(double intelligence) {
        return Double.isFinite(intelligence) ? Math.min(MAX_RADIUS, Math.max(0, intelligence) * .64) : 0;
    }
    public static double radius(Cat cat) {
        return radius(CatAttributeEffects.effectiveValue(cat, CatStat.INTELLIGENCE));
    }
    public static boolean available(Cat cat) {
        return CatClothesData.getOutfit(cat) == CatOutfitType.AGENT && CatSupportRules.canAssist(cat)
                && CareerCatBehavior.findSeat(cat) != null && !cat.isInWaterOrBubble() && !cat.isInLava()
                && (!cat.isPassenger() || cat.getVehicle() instanceof com.simibubi.create.content.contraptions.actors.seat.SeatEntity)
                && radius(cat) > 0;
    }
    public static boolean hostile(Cat cat, Mob target) {
        if (!target.isAlive() || target.isRemoved() || target.isSpectator() || !CatTeamRules.canHarm(cat, target)) return false;
        if (target instanceof Enemy || target.getType().getCategory() == MobCategory.MONSTER) return true;
        LivingEntity victim = target.getTarget();
        // Angry neutral creatures qualify only when attacking this cat, its owner or a friendly pet.
        return victim != null && (victim == cat || victim.getUUID().equals(cat.getOwnerUUID())
                || CatTeamRules.friendly(cat, victim));
    }
    public static void tick(Cat cat) {
        if (!(cat.level() instanceof ServerLevel level)) return;
        if (!available(cat)) { stop(cat); return; }
        var watch = WORLDS.computeIfAbsent(level, ignored -> new Watch());
        if (watch.sources.add(cat.getUUID())) watch.nextScan = 0;
    }
    public static void stop(Cat cat) {
        if (!(cat.level() instanceof ServerLevel level)) return;
        var watch = WORLDS.get(level);
        if (watch != null && watch.sources.remove(cat.getUUID())) watch.nextScan = 0;
    }
    public static boolean marked(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level) || !target.isAlive()) return false;
        var watch = WORLDS.get(level);
        return watch != null && watch.targets.contains(target.getUUID());
    }
    /** Tick END: one scan per second per observer, no forced chunks, no per-target source races. */
    public static void flush(ServerLevel level) {
        var watch = WORLDS.get(level);
        if (watch == null || level.getGameTime() < watch.nextScan) return;
        watch.nextScan = level.getGameTime() + SCAN_TICKS;
        var detected = new HashMap<UUID, Mob>();
        watch.sources.removeIf(uuid -> !(level.getEntity(uuid) instanceof Cat cat) || !available(cat));
        for (UUID uuid : watch.sources) {
            if (!(level.getEntity(uuid) instanceof Cat cat)) continue;
            double radius = radius(cat);
            for (Mob target : level.getEntitiesOfClass(Mob.class, cat.getBoundingBox().inflate(radius),
                    mob -> hostile(cat, mob) && cat.distanceToSqr(mob) <= radius * radius)) {
                detected.put(target.getUUID(), target);
            }
        }
        for (UUID uuid : watch.targets) {
            if (!detected.containsKey(uuid) && level.getEntity(uuid) instanceof LivingEntity target)
                send(target, null, 0);
        }
        for (Mob target : detected.values()) send(target, null, VISUAL_TICKS);
        watch.targets = new HashSet<>(detected.keySet());
        if (watch.sources.isEmpty() && watch.targets.isEmpty()) WORLDS.remove(level);
    }
    public static void syncTo(LivingEntity target, ServerPlayer player) {
        if (marked(target)) send(target, player, VISUAL_TICKS);
    }
    private static void send(LivingEntity target, ServerPlayer player, int duration) {
        ModNetwork.agentWatch(target, player, new AgentWatchPacket(target.getId(), target.getUUID(), duration));
    }
    private CatAgentWatch() {}
}
