package cn.laowu.mod;

import cn.laowu.mod.network.ModNetwork;
import cn.laowu.mod.item.CatToolBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative cat-leggings pounce. */
public final class CatArmorPounceBehavior {
    private static final double MAX_RANGE = 8.0D;
    private static final double MAX_RANGE_SQR = MAX_RANGE * MAX_RANGE;
    private static final int WEAKNESS_TICKS = 2 * 20;
    private static final double LANDING_RADIUS = 2.0D;
    private static final int COOLDOWN_TICKS = 7 * 20;
    private static final Map<UUID, Pounce> ACTIVE = new HashMap<>();

    public static void tryStart(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || !hasCatLeggings(player)
                || ACTIVE.containsKey(player.getUUID())
                || player.getCooldowns().isOnCooldown(LaoWuMod.CAT_LEGGINGS.get())) return;
        LivingEntity target = findTarget(player);
        if (target == null) return;

        ModNetwork.playLogisticsSound(player.serverLevel(), player.blockPosition(), false);
        Vec3 start = player.position();
        Vec3 destination = landingPosition(target, start);
        double horizontalDistance = horizontalDistance(start, destination);
        ACTIVE.put(player.getUUID(), new Pounce(player.getUUID(), target.getUUID(), start));
        // A valid target is required before the seven-second cooldown begins.
        player.getCooldowns().addCooldown(LaoWuMod.CAT_LEGGINGS.get(), COOLDOWN_TICKS);
        Vec3 initial = destination.subtract(start);
        double horizontal = Math.sqrt(initial.x * initial.x + initial.z * initial.z);
        double speed = Math.min(1.05D, 0.62D + horizontal * 0.055D);
        Vec3 horizontalMotion = new Vec3(initial.x, 0.0D, initial.z).normalize().scale(speed);
        double lift = 0.24D + Math.min(0.28D, horizontal * 0.035D);
        player.setDeltaMovement(horizontalMotion.x, lift, horizontalMotion.z);
        player.hurtMarked = true;
        player.hasImpulse = true;
    }

    public static void tick(net.minecraft.server.MinecraftServer server) {
        Iterator<Pounce> iterator = ACTIVE.values().iterator();
        while (iterator.hasNext()) {
            Pounce pounce = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(pounce.playerId);
            if (player == null || !player.isAlive() || !hasCatLeggings(player)) {
                iterator.remove();
                continue;
            }
            ServerLevel level = player.serverLevel();
            Entity rawTarget = level.getEntity(pounce.targetId);
            if (!(rawTarget instanceof LivingEntity target) || !target.isAlive()) {
                iterator.remove();
                continue;
            }

            pounce.tick++;
            Vec3 end = landingPosition(target, pounce.start);
            Vec3 remaining = end.subtract(player.position());
            double horizontalRemaining = Math.sqrt(remaining.x * remaining.x + remaining.z * remaining.z);
            boolean reached = horizontalRemaining <= 0.65D
                    && Math.abs(remaining.y) <= 1.35D;
            if (reached || pounce.tick >= 24) {
                land(player, player.position());
                iterator.remove();
                continue;
            }

            // Preserve real player physics and gravity, applying only a smooth
            // horizontal steering correction toward the moving target.
            Vec3 current = player.getDeltaMovement();
            Vec3 desiredDirection = new Vec3(remaining.x, 0.0D, remaining.z).normalize();
            double desiredSpeed = Math.min(1.05D, 0.42D + horizontalRemaining * 0.08D);
            double steer = 0.28D;
            double x = current.x + (desiredDirection.x * desiredSpeed - current.x) * steer;
            double z = current.z + (desiredDirection.z * desiredSpeed - current.z) * steer;
            player.setDeltaMovement(x, current.y, z);
            player.hurtMarked = true;
            player.resetFallDistance();
            player.hasImpulse = true;
        }
    }

    private static LivingEntity findTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB area = player.getBoundingBox().inflate(MAX_RANGE);
        LivingEntity best = null;
        double bestAngle = Double.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;

        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, area,
                candidate -> isValidTarget(player, candidate))) {
            Vec3 toTarget = candidate.getBoundingBox().getCenter().subtract(eye);
            double distanceSqr = toTarget.lengthSqr();
            if (distanceSqr > MAX_RANGE_SQR || distanceSqr < 0.0001D) continue;
            double dot = look.dot(toTarget.normalize());
            if (dot <= 0.0D || !player.hasLineOfSight(candidate)) continue;
            // 1-dot measures distance from the crosshair; physical distance
            // only resolves targets at practically the same screen position.
            double angle = 1.0D - dot;
            if (angle < bestAngle - 0.0001D
                    || (Math.abs(angle - bestAngle) <= 0.0001D && distanceSqr < bestDistance)) {
                best = candidate;
                bestAngle = angle;
                bestDistance = distanceSqr;
            }
        }
        return best;
    }

    private static boolean isValidTarget(ServerPlayer player, LivingEntity target) {
        if (target == player || !target.isAlive() || target.isSpectator()) return false;
        return target instanceof Enemy || target instanceof ServerPlayer;
    }

    private static Vec3 landingPosition(LivingEntity target, Vec3 from) {
        Vec3 horizontal = new Vec3(from.x - target.getX(), 0.0D, from.z - target.getZ());
        if (horizontal.lengthSqr() < 0.0001D) horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        horizontal = horizontal.normalize().scale(target.getBbWidth() * 0.5D + 0.35D);
        return new Vec3(target.getX() + horizontal.x, target.getY(), target.getZ() + horizontal.z);
    }

    private static void land(ServerPlayer player, Vec3 position) {
        ServerLevel level = player.serverLevel();
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.resetFallDistance();
        level.sendParticles(ParticleTypes.POOF, position.x, position.y + 0.15D, position.z,
                24, LANDING_RADIUS * 0.55D, 0.18D, LANDING_RADIUS * 0.55D, 0.08D);
        level.sendParticles(ParticleTypes.CLOUD, position.x, position.y + 0.08D, position.z,
                14, LANDING_RADIUS * 0.45D, 0.08D, LANDING_RADIUS * 0.45D, 0.035D);
        level.playSound(null, position.x, position.y, position.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.55F, 1.45F);

        AABB effectArea = new AABB(position.x - LANDING_RADIUS, position.y - 1.0D,
                position.z - LANDING_RADIUS, position.x + LANDING_RADIUS,
                position.y + 2.5D, position.z + LANDING_RADIUS);
        float impactDamage = (float) Math.max(4.0D,
                player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, effectArea,
                living -> living != player && living.isAlive()
                        && living.position().distanceToSqr(position) <= LANDING_RADIUS * LANDING_RADIUS)) {
            living.hurt(level.damageSources().playerAttack(player), impactDamage);
            double ratioX = position.x - living.getX();
            double ratioZ = position.z - living.getZ();
            if (ratioX * ratioX + ratioZ * ratioZ < 0.0001D) {
                ratioX = -player.getLookAngle().x;
                ratioZ = -player.getLookAngle().z;
            }
            living.knockback(0.35D, ratioX, ratioZ);
            if (!(living instanceof ServerPlayer)) {
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, 0));
            }
        }
    }

    private static boolean hasCatLeggings(ServerPlayer player) {
        var leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        return leggings.is(LaoWuMod.CAT_LEGGINGS.get())
                && !CatToolBehavior.isExhausted(leggings);
    }

    private static double horizontalDistance(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static final class Pounce {
        private final UUID playerId;
        private final UUID targetId;
        private final Vec3 start;
        private int tick;

        private Pounce(UUID playerId, UUID targetId, Vec3 start) {
            this.playerId = playerId;
            this.targetId = targetId;
            this.start = start;
        }
    }

    private CatArmorPounceBehavior() {
    }
}
