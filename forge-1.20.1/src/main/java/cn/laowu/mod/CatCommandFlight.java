package cn.laowu.mod;

import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.phys.Vec3;

/**
 * Laser orders previously bypassed the idle sky tick but kept its no-gravity
 * state and ground pathfinder. Use real 3-D pathfinding while commanded. Existing
 * combat goals still own targeting, damage, cooldowns and ranged positioning.
 */
public final class CatCommandFlight {
    public static void update(Cat cat, boolean ordered, boolean attackOrder) {
        boolean sky = ordered && CatTraitData.ensure(cat).has(CatTrait.SKY_CAT);
        // The pilot's dive goal already supplies its own flight controller.
        boolean pilotAttack = CatClothesData.getOutfit(cat) == CatOutfitType.FLIGHT
                && attackOrder;
        boolean artilleryAttack = CatClothesData.getOutfit(cat) == CatOutfitType.ENGINEERING
                && CatEngineeringCombat.validTarget(cat, cat.getTarget());
        boolean active = sky && !pilotAttack && !artilleryAttack;
        if (active && !(cat.getMoveControl() instanceof CommandMoveControl)) {
            PathNavigation previousNavigation = cat.getNavigation();
            MoveControl previousControl = cat.getMoveControl();
            previousNavigation.stop();
            FlyingPathNavigation navigation = new FlyingPathNavigation(cat, cat.level());
            navigation.setCanFloat(true);
            navigation.setCanOpenDoors(false);
            navigation.setCanPassDoors(true);
            var access = (CatNavigationAccessor) cat;
            access.laowu$setNavigation(navigation);
            access.laowu$setMoveControl(new CommandMoveControl(cat, previousNavigation,
                    previousControl, cat.isNoGravity()));
            cat.setNoGravity(true);
        } else if (!active && cat.getMoveControl() instanceof CommandMoveControl flight) {
            cat.getNavigation().stop();
            var access = (CatNavigationAccessor) cat;
            access.laowu$setNavigation(flight.previousNavigation);
            access.laowu$setMoveControl(flight.previousControl);
            boolean stillSky = CatTraitData.ensure(cat).has(CatTrait.SKY_CAT);
            cat.setNoGravity((stillSky || pilotAttack) && flight.previousNoGravity);
            cat.setXxa(0);
            cat.setYya(0);
            cat.setZza(0);
            cat.fallDistance = 0;
        }
        if (artilleryAttack) {
            cat.setNoGravity(false);
            cat.fallDistance = 0;
        }
    }

    private static final class CommandMoveControl extends MoveControl {
        private final Cat cat;
        private final PathNavigation previousNavigation;
        private final MoveControl previousControl;
        private final boolean previousNoGravity;

        private CommandMoveControl(Cat cat, PathNavigation navigation, MoveControl control, boolean noGravity) {
            super(cat);
            this.cat = cat;
            this.previousNavigation = navigation;
            this.previousControl = control;
            this.previousNoGravity = noGravity;
        }

        @Override public void tick() {
            cat.setNoGravity(true);
            cat.fallDistance = 0;
            Vec3 point = cat.position();
            if (operation == Operation.MOVE_TO) {
                point = new Vec3(wantedX, wantedY, wantedZ);
            } else if (operation == Operation.STRAFE) {
                // Keep ranged-career lateral dodges instead of disabling their AI.
                double radians = Math.toRadians(cat.getYRot());
                double sin = Math.sin(radians), cos = Math.cos(radians);
                point = point.add(strafeRight * cos - strafeForwards * sin, 0,
                        strafeForwards * cos + strafeRight * sin);
            }
            boolean moving = operation != Operation.WAIT;
            operation = Operation.WAIT;
            double speed = cat.getAttributeValue(Attributes.MOVEMENT_SPEED) * Math.max(0.25D, speedModifier);
            Vec3 motion = CatFlightMotion.step(cat.position(), point, cat.getDeltaMovement(), moving ? speed : 0);
            cat.setDeltaMovement(motion);
            // Our velocity replaces ground acceleration; vanilla still handles collisions.
            cat.setSpeed(0);
            cat.setXxa(0);
            cat.setYya(0);
            cat.setZza(0);
            if (motion.horizontalDistanceSqr() > 0.0001D) {
                float yaw = (float) (Mth.atan2(motion.z, motion.x) * 180.0D / Math.PI) - 90.0F;
                cat.setYRot(rotlerp(cat.getYRot(), yaw, 40.0F));
                cat.yBodyRot = cat.getYRot();
            }
        }
    }
    private CatCommandFlight() {}
}
