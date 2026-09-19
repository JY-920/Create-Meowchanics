package cn.laowu.mod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.Vec3;

/** Break only this agent's aggro. No blindness, player controls, terrain edits or shared invulnerability. */
public final class CatAgentSmoke {
    private static final String UNTIL = "LaoWuAgentSmokeUntil";
    private static final double CLEAR_RADIUS = 32;
    public static final int DURATION = 50;
    public static boolean hiddenFrom(LivingEntity observer, LivingEntity candidate) {
        return candidate instanceof Cat cat && !cat.level().isClientSide
                && CatClothesData.getOutfit(cat) == CatOutfitType.AGENT
                && cat.getPersistentData().getLong(UNTIL) > cat.level().getGameTime()
                && observer.level() == cat.level();
    }
    public static void burst(Cat cat, Vec3 center) {
        burst(cat,center,cn.laowu.mod.accessory.CatAccessories.value(cat,"healing_smoke")>0);
    }
    public static void burst(Cat cat, Vec3 center, boolean healing) {
        if (!(cat.level() instanceof ServerLevel level) || !cat.isAlive()
                || CatClothesData.getOutfit(cat) != CatOutfitType.AGENT) return;
        cat.getPersistentData().putLong(UNTIL, level.getGameTime() + DURATION);
        if(healing)CatHealingSmoke.create(cat,center);
        else level.sendParticles(LaoWuMod.CAT_AGENT_SMOKE.get(), center.x, center.y + .5, center.z, 128, 1.6, .5, 1.6, .035);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, .7F, .7F);
        clearNearby(level, cat);
    }
    public static void clearAggro(Mob mob, Cat cat) {
        var brain = mob.getBrain();
        boolean brainTarget = brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                && brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) == cat;
        boolean targeted = mob.getTarget() == cat || brainTarget;
        if (targeted) {
            // TargetGoal keeps a second targetMob reference even after Mob#setTarget(null).
            // Stop, do not remove, running goals so stale revenge cannot restart after the cloak.
            mob.targetSelector.getAvailableGoals().stream().filter(WrappedGoal::isRunning).toList().forEach(WrappedGoal::stop);
            mob.setTarget(null);
            mob.goalSelector.getAvailableGoals().stream().filter(WrappedGoal::isRunning)
                    .filter(goal -> goal.getFlags().contains(Goal.Flag.MOVE) || goal.getFlags().contains(Goal.Flag.LOOK))
                    .toList().forEach(WrappedGoal::stop);
            mob.getNavigation().stop();
        }
        if (mob.getLastHurtByMob() == cat) mob.setLastHurtByMob(null);
        if (mob.getLastHurtMob() == cat) mob.setLastHurtMob(null);
        if (brainTarget) brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        if (brain.hasMemoryValue(MemoryModuleType.ANGRY_AT)
                && brain.getMemory(MemoryModuleType.ANGRY_AT).filter(cat.getUUID()::equals).isPresent())
            brain.eraseMemory(MemoryModuleType.ANGRY_AT);
        if (brain.hasMemoryValue(MemoryModuleType.HURT_BY_ENTITY)
                && brain.getMemory(MemoryModuleType.HURT_BY_ENTITY).orElse(null) == cat)
            brain.eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
        if (brain.hasMemoryValue(MemoryModuleType.HURT_BY)
                && brain.getMemory(MemoryModuleType.HURT_BY).filter(source -> source.getEntity() == cat).isPresent())
            brain.eraseMemory(MemoryModuleType.HURT_BY);
        if (brain.hasMemoryValue(MemoryModuleType.LOOK_TARGET)
                && brain.getMemory(MemoryModuleType.LOOK_TARGET)
                .filter(tracker -> tracker instanceof EntityTracker entity && entity.getEntity() == cat).isPresent())
            brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
        if (brain.hasMemoryValue(MemoryModuleType.WALK_TARGET)
                && brain.getMemory(MemoryModuleType.WALK_TARGET)
                .filter(walk -> walk.getTarget() instanceof EntityTracker entity && entity.getEntity() == cat).isPresent()) {
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            brain.eraseMemory(MemoryModuleType.PATH);
            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            mob.getNavigation().stop();
        }
        if (mob instanceof NeutralMob neutral && cat.getUUID().equals(neutral.getPersistentAngerTarget())) {
            neutral.setPersistentAngerTarget(null); neutral.setRemainingPersistentAngerTime(0);
        }
    }
    private static void clearNearby(ServerLevel level, Cat cat) {
        // Entity queries only: never request chunks or scan the whole world.
        for (Mob mob : level.getEntitiesOfClass(Mob.class, cat.getBoundingBox().inflate(CLEAR_RADIUS),
                other -> other != cat && other.distanceToSqr(cat) <= CLEAR_RADIUS * CLEAR_RADIUS))
            clearAggro(mob, cat);
    }
    public static void tick(Cat cat) {
        if (!(cat.level() instanceof ServerLevel level) || CatClothesData.getOutfit(cat) != CatOutfitType.AGENT) return;
        long until = cat.getPersistentData().getLong(UNTIL);
        if (until <= level.getGameTime()) { if (until != 0) cat.getPersistentData().remove(UNTIL); return; }
        // Brain-based mobs can bypass Mob#setTarget; clear only memories referring to this cat.
        if (cat.tickCount % 5 == 0) clearNearby(level, cat);
    }
    private CatAgentSmoke() {}
}
