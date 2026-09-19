package cn.laowu.mod;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

/** A short grounded pounce; wing animation and ordinary bites share one damage cooldown. */
public final class CatCockroachCombat extends Goal {
    public static final String NEXT_LEAP = "LaoWuCockroachNextLeap", NEXT_ATTACK = "LaoWuCockroachNextAttack";
    private final Cat cat;
    private LivingEntity target;
    private int airborneTicks;
    private boolean attacked;
    public CatCockroachCombat(Cat cat) { this.cat = cat; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }
    private boolean available() {
        return CatClothesData.getOutfit(cat) == CatOutfitType.COCKROACH
                && CareerCatBehavior.canParticipateInCombat(cat) && !cat.isNoAi()
                && !CatProfileData.isBeingViewed(cat) && !cat.isPassenger()
                && !cat.isInWaterOrBubble() && !cat.isInLava();
    }
    @Override public boolean canUse() {
        target = cat.getTarget();
        if (!available() || !cat.onGround() || cat.isNoGravity() || target == null || !target.isAlive()
                || !CatTeamRules.canHarm(cat, target) || !cat.hasLineOfSight(target)
                || cat.level().getGameTime() < cat.getPersistentData().getLong(NEXT_LEAP)) return false;
        double distance = cat.position().subtract(target.position()).multiply(1,0,1).lengthSqr();
        return distance >= 4 && distance <= 30.25 && Math.abs(cat.getY()-target.getY()) < 1.5;
    }
    @Override public void start() {
        airborneTicks=0;attacked=false;
        Vec3 toward=target.position().subtract(cat.position()).multiply(1,0,1);
        Vec3 motion=toward.normalize().scale(Math.min(.95, toward.length()*.20));
        cat.getNavigation().stop();
        cat.setDeltaMovement(motion.x,.46,motion.z);
        cat.hasImpulse=true;cat.hurtMarked=true;
        cat.getPersistentData().putLong(NEXT_LEAP,cat.level().getGameTime()+60);
        CatCockroachSwarm.leap(cat,true);
    }
    @Override public boolean canContinueToUse() {
        return available() && target != null && target.isAlive() && airborneTicks < 24
                && (airborneTicks < 2 || !cat.onGround());
    }
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void tick(){
        airborneTicks++;
        cat.getLookControl().setLookAt(target,45,45);
        if(!attacked)attacked=tryAttack(cat,target);
    }
    @Override public void stop(){CatCockroachSwarm.leap(cat,false);target=null;}
    public static boolean tryAttack(Cat cat,LivingEntity target){
        long now=cat.level().getGameTime();
        double reach=Math.max(1.3,cat.getBbWidth()*2+target.getBbWidth()*.5);
        if(cat.level().isClientSide || CatClothesData.getOutfit(cat)!=CatOutfitType.COCKROACH
                || !target.isAlive() || !CatTeamRules.canHarm(cat,target) || !cat.hasLineOfSight(target)
                || cat.distanceToSqr(target)>reach*reach || now<cat.getPersistentData().getLong(NEXT_ATTACK))return false;
        cat.getPersistentData().putLong(NEXT_ATTACK,now+CareerCatBehavior.careerAttackIntervalTicks(cat));
        cat.swing(InteractionHand.MAIN_HAND);
        return cat.doHurtTarget(target);
    }
}
