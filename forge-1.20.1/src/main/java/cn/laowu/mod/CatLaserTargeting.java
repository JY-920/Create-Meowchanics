package cn.laowu.mod;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Shared prediction and authoritative server raycast; expanded hitboxes never bypass walls. */
public final class CatLaserTargeting {
    public record Aim(Vec3 point, LivingEntity target) {}
    public static Aim aim(Player player) {
        return aim(player, player.getEyePosition(), player.getLookAngle());
    }
    /** Render-time interpolated inputs are local only; commands use the authoritative overload. */
    public static Aim aim(Player player, Vec3 start, Vec3 look) {
        Vec3 end = start.add(look.scale(32));
        var block = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        Vec3 stop = block.getLocation();
        LivingEntity target = null;
        double best = start.distanceToSqr(stop);
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(start, stop).inflate(0.75), e -> e != player && e.isAlive() && !e.isSpectator())) {
            var hit = entity.getBoundingBox().inflate(0.6).clip(start, stop);
            if (hit.isEmpty()) continue;
            double distance = start.distanceToSqr(hit.get());
            if (distance >= best || !player.hasLineOfSight(entity)) continue;
            best = distance;
            stop = hit.get();
            target = entity;
        }
        return new Aim(stop, target);
    }
    public static boolean allowed(Player owner, LivingEntity target) {
        if (target == null || target == owner || !target.isAlive() || target.isSpectator()
                || target instanceof Player) return false;
        if (target instanceof TamableAnimal pet && pet.isTame()) return true;
        if (owner.isAlliedTo(target)) return false;
        return !(target instanceof TamableAnimal pet && owner.getUUID().equals(pet.getOwnerUUID()));
    }
    private CatLaserTargeting() {}
}
