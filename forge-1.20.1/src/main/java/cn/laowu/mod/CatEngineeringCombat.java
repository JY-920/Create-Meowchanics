package cn.laowu.mod;

import cn.laowu.mod.entity.EngineeringCannon;
import cn.laowu.mod.genetics.CatAttributeEffects;
import cn.laowu.mod.genetics.CatStat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

/** A stationary gun with bounded, intelligence-driven redeployment instead of continuous strafing. */
public final class CatEngineeringCombat extends Goal {
    public static final double RANGE = CatArtilleryTactics.RANGE;
    public static final int DEPLOY_TICKS = 16;
    private final Cat cat;
    private int cooldown, pathDelay, blockedTicks, deployTicks, repositionTicks, packDelay;

    public CatEngineeringCombat(Cat cat) {
        this.cat = cat;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    public static boolean deployed(Cat cat) { return cat.getVehicle() instanceof EngineeringCannon; }
    public static boolean canReceiveOrders(Cat cat) { return !cat.isPassenger() || deployed(cat); }
    public static boolean validTarget(Cat cat, LivingEntity target) {
        return target != null && target.isAlive() && target.level() == cat.level()
                && !(target instanceof Player) && CatTeamRules.canHarm(cat, target);
    }
    public static void release(Cat cat) {
        if (!cat.level().isClientSide && cat.getVehicle() instanceof EngineeringCannon cannon) cannon.release();
    }
    @Override public boolean canUse() {
        return CatClothesData.getOutfit(cat) == CatOutfitType.ENGINEERING
                && CareerCatBehavior.canParticipateInCombat(cat) && canReceiveOrders(cat)
                && !cat.isNoAi() && !CatProfileData.isBeingViewed(cat) && validTarget(cat, cat.getTarget());
    }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() { pathDelay = 0; blockedTicks = 0; deployTicks = 0; repositionTicks = 0; }
    @Override public void stop() {
        release(cat);
        cat.getNavigation().stop();
        // Switching targets must not reset the reload.
        blockedTicks = deployTicks = repositionTicks = packDelay = 0;
    }
    @Override public void tick() {
        if (!(cat.level() instanceof ServerLevel level) || !canUse()) return;
        LivingEntity target = cat.getTarget();
        if (cooldown > 0) cooldown--;
        if (packDelay > 0) packDelay--;
        int intelligence = CatAttributeEffects.effectiveValue(cat, CatStat.INTELLIGENCE);
        cat.getLookControl().setLookAt(target, 30, 30);
        EngineeringCannon cannon = cat.getVehicle() instanceof EngineeringCannon c ? c : null;
        double distance = (cannon == null ? deploymentPosition(cat) : cannon.position()).distanceTo(target.position());
        if (cannon != null) {
            cat.getNavigation().stop();
            cat.setDeltaMovement(Vec3.ZERO);
            cannon.aimAt(target);
            if (cannon.hasClearShot(target)) blockedTicks = 0; else blockedTicks++;
            deployTicks++;
            // Fire a ready shot before choosing to reposition; preserve cooldown while packed.
            if (distance <= RANGE && blockedTicks == 0 && deployTicks >= DEPLOY_TICKS && cooldown <= 0
                    && cannon.fire(cat, target)) {
                int speed = CatAttributeEffects.effectiveValue(cat, CatStat.SPEED);
                cooldown = CatMusicSupport.attackInterval(cat, CatSuitSettings.current(CatOutfitType.ENGINEERING)
                        .intervalTicks(ServerConfig.scale(CatStat.SPEED, speed)));
            }
            if (distance > RANGE || blockedTicks >= 20
                    || CatArtilleryTactics.relocate(intelligence, distance, deployTicks)) {
                release(cat);
                deployTicks = blockedTicks = repositionTicks = pathDelay = 0;
                packDelay = 8;
            } else return;
        }
        distance = deploymentPosition(cat).distanceTo(target.position());
        boolean seeking = CatArtilleryTactics.smart(intelligence) && !CatArtilleryTactics.preferred(distance);
        if (seeking) repositionTicks++; else repositionTicks = 0;
        // A blocked escape or faster enemy must not keep the cat running forever without firing.
        boolean settle = !seeking || repositionTicks >= 48;
        if (settle && packDelay == 0 && distance <= RANGE && cat.hasLineOfSight(target) && canDeploy(cat)) {
            var placed = new EngineeringCannon(LaoWuMod.ENGINEERING_CANNON.get(), level);
            placed.setPos(deploymentPosition(cat));
            placed.aimAt(target);
            if (placed.hasClearShot(target) && level.addFreshEntity(placed)) {
                if (cat.startRiding(placed, true)) {
                    cat.getNavigation().stop();
                    cat.setDeltaMovement(Vec3.ZERO);
                    deployTicks = blockedTicks = repositionTicks = 0;
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                            placed.getX(), placed.getY() + 0.25, placed.getZ(), 8, 0.35, 0.12, 0.35, 0.015);
                    return;
                }
                placed.discard();
            }
        }
        if (pathDelay-- <= 0) {
            pathDelay = 8;
            if (seeking && distance < 14) {
                Vec3 away = cat.position().subtract(target.position()).multiply(1, 0, 1).normalize();
                if (away.lengthSqr() < 0.001) away = Vec3.directionFromRotation(0, cat.getYRot()).scale(-1);
                boolean found = false;
                for (int turn : new int[]{0, 45, -45, 90, -90}) {
                    Vec3 point = target.position().add(away.yRot((float)Math.toRadians(turn))
                            .scale(CatArtilleryTactics.IDEAL + EngineeringCannon.SEAT_BACK));
                    var path = cat.getNavigation().createPath(point.x, cat.getY(), point.z, 0);
                    if (path != null && path.canReach() && cat.getNavigation().moveTo(path, 1.05D)) { found = true; break; }
                }
                // A newly spawned or just-dismounted cat may still be falling. Vanilla
                // navigation rejects paths until grounded; that is not a blocked escape.
                if (!found && cat.onGround()) repositionTicks = 48;
            } else {
                // Near waypoint also works when the target is outside vanilla's path search radius.
                Vec3 step = target.position().subtract(cat.position());
                if (step.lengthSqr() > 12 * 12) step = step.normalize().scale(12);
                Vec3 point = cat.position().add(step);
                cat.getNavigation().moveTo(point.x, point.y, point.z, 1.05D);
            }
        }
    }
    public static Vec3 deploymentPosition(Cat cat) {
        Vec3 forward = validTarget(cat, cat.getTarget())
                ? cat.getTarget().position().subtract(cat.position()).multiply(1, 0, 1).normalize()
                : Vec3.directionFromRotation(0, cat.getYRot());
        return cat.position().add(forward.scale(EngineeringCannon.SEAT_BACK));
    }
    public static boolean canDeploy(Cat cat) {
        if (!cat.onGround() || cat.isPassenger() || cat.isInWaterOrBubble() || cat.isInLava()) return false;
        Vec3 centre = deploymentPosition(cat);
        AABB space = new AABB(Math.min(cat.getX(), centre.x) - 0.55, cat.getY() + 0.02,
                Math.min(cat.getZ(), centre.z) - 0.55, Math.max(cat.getX(), centre.x) + 0.55,
                cat.getY() + 2, Math.max(cat.getZ(), centre.z) + 0.55);
        return !cat.level().getBlockCollisions(cat, space).iterator().hasNext();
    }
}
