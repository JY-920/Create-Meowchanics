package cn.laowu.mod;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.Enemy;

/** Ordinary combat policy; unusual trait behaviours retain their own documented rules. */
public final class CatCombatControl {
    private static final Set<Cat> INSTALLED = Collections.newSetFromMap(new WeakHashMap<>());

    public static void tick(Cat cat) {
        if (cat.level().isClientSide || !cat.isTame()) return;
        if (INSTALLED.add(cat)) install(cat);
        if (!canAct(cat) && cat.getTarget() != null) cat.setTarget(null);
    }

    private static boolean canAct(Cat cat) {
        return cat.isAlive() && cat.isTame() && !cat.isNoAi() && !cat.isPassenger()
                && !CatPoseData.isPancake(cat) && !CatProfileData.isBeingViewed(cat)
                && !cat.isOrderedToSit() && !cat.isInSittingPose()
                && CatClothesData.getOutfit(cat) != CatOutfitType.TRANSPORT
                && !DynamiteCatLastStand.isActive(cat);
    }

    private static boolean ordinary(Cat cat) {
        return canAct(cat) && CatClothesData.getOutfit(cat) == CatOutfitType.NONE;
    }

    private static boolean allowed(Cat cat, LivingEntity target) {
        return target != null && CatTeamRules.canHarm(cat, target);
    }

    private static void install(Cat cat) {
        cat.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(cat) {
            @Override public boolean canUse() {
                return ordinary(cat) && cat.getOwner() != null
                        && allowed(cat, cat.getOwner().getLastHurtByMob()) && super.canUse();
            }
        });
        cat.targetSelector.addGoal(2, new OwnerHurtTargetGoal(cat) {
            @Override public boolean canUse() {
                return ordinary(cat) && cat.getOwner() != null
                        && allowed(cat, cat.getOwner().getLastHurtMob()) && super.canUse();
            }
        });
        cat.targetSelector.addGoal(3, new HurtByTargetGoal(cat) {
            @Override public boolean canUse() {
                return ordinary(cat) && allowed(cat, cat.getLastHurtByMob()) && super.canUse();
            }
        });
        cat.targetSelector.addGoal(4, new AutoTargetGoal(cat));
        cat.goalSelector.addGoal(4, new MeleeAttackGoal(cat, 1.15, true) {
            @Override public boolean canUse() { return ordinary(cat) && allowed(cat, cat.getTarget()) && super.canUse(); }
            @Override public boolean canContinueToUse() {
                return ordinary(cat) && allowed(cat, cat.getTarget()) && super.canContinueToUse();
            }
        });
    }

    private static boolean aggressive(Cat cat) {
        return cat.getServer() != null && CatCombatPreferences.get(cat.getServer()).aggressive(cat.getOwnerUUID());
    }

    private static double range(Cat cat) {
        return Math.max(1, Math.min(16, cat.getAttributeValue(Attributes.FOLLOW_RANGE)));
    }

    private static boolean recent(int now, int then) { return then > 0 && now >= then && now - then <= 200; }

    /** Do not abandon an attacker which became a legitimate defence or explicit-order target. */
    private static boolean nowDefending(Cat cat, LivingEntity target) {
        if (CatLaserCommands.isAttackOrderTarget(cat, target)) return true;
        if (cat.getLastHurtByMob() == target && recent(cat.tickCount, cat.getLastHurtByMobTimestamp())) return true;
        LivingEntity owner = cat.getOwner();
        if (owner != null && (owner.getLastHurtByMob() == target
                && recent(owner.tickCount, owner.getLastHurtByMobTimestamp())
                || owner.getLastHurtMob() == target && recent(owner.tickCount, owner.getLastHurtMobTimestamp()))) return true;
        return target instanceof Mob mob && (mob.getTarget() == cat || owner != null && mob.getTarget() == owner
                || CatTeamRules.friendly(cat, mob.getTarget()));
    }

    private static final class AutoTargetGoal extends Goal {
        private final Cat cat;
        private LivingEntity selected;
        private int nextSearch;

        private AutoTargetGoal(Cat cat) {
            this.cat = cat;
            nextSearch = cat.tickCount + Math.floorMod(cat.getId(), 20);
            setFlags(EnumSet.of(Flag.TARGET));
        }

        private boolean available() {
            return canAct(cat) && aggressive(cat) && !CatLaserCommands.hasOrder(cat)
                    && !CatPoseData.isHissing(cat) && !HissingCatBehavior.isFighting(cat);
        }

        private boolean valid(LivingEntity target) {
            LivingEntity owner = cat.getOwner();
            double range = range(cat);
            return target instanceof Enemy && allowed(cat, target) && !target.isInvisible()
                    && (!(target instanceof NeutralMob neutral) || neutral.isAngryAt(cat))
                    && cat.distanceToSqr(target) <= range * range
                    && (owner == null || cat.distanceToSqr(owner) <= CareerCatBehavior.MAX_OWNER_DISTANCE_SQR)
                    && cat.hasLineOfSight(target);
        }

        @Override public boolean canUse() {
            if (!available() || cat.getTarget() != null || cat.tickCount < nextSearch) return false;
            nextSearch = cat.tickCount + 20;
            selected = null;
            double best = Double.MAX_VALUE;
            for (LivingEntity candidate : cat.level().getEntitiesOfClass(LivingEntity.class,
                    cat.getBoundingBox().inflate(range(cat)), this::valid)) {
                double distance = cat.distanceToSqr(candidate);
                if (distance < best) { selected = candidate; best = distance; }
            }
            return selected != null;
        }

        @Override public boolean canContinueToUse() {
            return available() && selected != null && cat.getTarget() == selected && valid(selected);
        }
        @Override public void start() { cat.setTarget(selected); }
        @Override public void stop() {
            if (selected != null && cat.getTarget() == selected && !nowDefending(cat, selected)) {
                cat.setTarget(null);
                cat.getNavigation().stop();
            }
            selected = null;
        }
    }

    private CatCombatControl() {}
}
