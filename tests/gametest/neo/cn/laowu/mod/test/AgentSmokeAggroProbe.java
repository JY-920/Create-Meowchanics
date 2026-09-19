package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.genetics.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class AgentSmokeAggroProbe {
    private static Vec3 floor(GameTestHelper h) {
        if (h.getLevel().getServer() instanceof GameTestServer) {
            var first=h.absolutePos(BlockPos.ZERO);
            var last=h.absolutePos(new BlockPos(71,11,13));
            for(int x=Math.floorDiv(first.getX(),16);x<=Math.floorDiv(last.getX(),16);x++)
                for(int z=Math.floorDiv(first.getZ(),16);z<=Math.floorDiv(last.getZ(),16);z++)
                    h.getLevel().setChunkForced(x,z,true);
        }
        for(int x=0;x<18;x++)for(int z=0;z<12;z++) {
            h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
            for(int y=1;y<8;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
        }
        return Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,1,3)));
    }
    private static Cat cat(ServerLevel level,Vec3 pos,CatOutfitType outfit) {
        var cat=EntityType.CAT.create(level);
        cat.setTame(true, false);
        cat.setOwnerUUID(UUID.fromString("fe0870ee-ea1c-4d43-8785-63dfd7b1a63b"));
        cat.setNoAi(true);cat.setNoGravity(true);cat.setPos(pos);
        level.addFreshEntity(cat);
        CatTraitData.set(cat,CatTraitProfile.EMPTY);
        cat.setAge(0);CatPoseData.setPose(cat,CatPoseData.NORMAL);
        var genes=CatAttributeData.ensure(cat);
        for(var stat:CatStat.values())genes=genes.withValues(stat,50,100);
        CatAttributeData.set(cat,genes);
        CatClothesData.equip(cat,outfit);CareerCatBehavior.tick(cat);
        return cat;
    }
    private static IronGolem golem(ServerLevel level,Vec3 pos) {
        var mob=EntityType.IRON_GOLEM.create(level);
        mob.setPos(pos);mob.setPlayerCreated(false);
        // Keep the real AI and its revenge-goal cache, but prevent collisions/attacks
        // from moving the fixture during the post-smoke regression window.
        mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        level.addFreshEntity(mob);
        return mob;
    }

    @GameTest(template="artillery_probe",batch="agent_smoke_selection",timeoutTicks=30)
    public static void concealmentBlocksSelectionAndTaunt(GameTestHelper h) {
        var level=h.getLevel();var base=floor(h);
        var agent=cat(level,base,CatOutfitType.AGENT);
        var ally=cat(level,base.add(3,0,0),CatOutfitType.ENGINEERING);
        var enemy=EntityType.HUSK.create(level);
        enemy.setNoAi(true);enemy.setPos(base.add(6,0,0));level.addFreshEntity(enemy);
        CatTraitData.set(agent,CatTraitProfile.EMPTY.withLevel(CatTrait.ATTENTION_MAGNET,1));
        h.runAfterDelay(5,()->{
            CatAgentSmoke.burst(agent,agent.position());
            h.assertTrue(!enemy.canAttack(agent),"The loaded mixin rejects hidden cats in vanilla target eligibility");
            enemy.setTarget(agent);
            h.assertTrue(enemy.getTarget()==null,"Explicit target setter cannot bypass concealment");
            enemy.setTarget(ally);
            h.assertTrue(enemy.getTarget()==ally,"Attention Magnet cannot redirect another target to a hidden agent");
            h.assertTrue(enemy.canAttack(ally),"Concealment does not disable attacks against other cats");
            enemy.setPos(base.add(40,0,0));
            h.assertTrue(!enemy.canAttack(agent),"Long-range observers cannot reacquire a concealed cat");
            agent.discard();ally.discard();enemy.discard();h.succeed();
            System.out.println("PASS: cached/normal target eligibility, target-event guard, Attention Magnet and distant observers");
        });
    }
    @GameTest(template="artillery_probe",batch="agent_smoke_brain",timeoutTicks=30)
    public static void brainMemoriesAreTargetScoped(GameTestHelper h) {
        var level=h.getLevel();var base=floor(h);
        var agent=cat(level,base,CatOutfitType.AGENT);
        var ally=cat(level,base.add(3,0,0),CatOutfitType.ENGINEERING);
        var enemy=EntityType.PIGLIN.create(level);
        enemy.setNoAi(true);enemy.setPos(base.add(6,0,0));level.addFreshEntity(enemy);
        var brain=enemy.getBrain();
        var attack=net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET;
        var angry=net.minecraft.world.entity.ai.memory.MemoryModuleType.ANGRY_AT;
        var look=net.minecraft.world.entity.ai.memory.MemoryModuleType.LOOK_TARGET;
        var walk=net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET;
        brain.setMemory(attack,agent);brain.setMemory(angry,agent.getUUID());
        brain.setMemory(look,new net.minecraft.world.entity.ai.behavior.EntityTracker(agent,true));
        brain.setMemory(walk,new net.minecraft.world.entity.ai.memory.WalkTarget(agent,1,1));
        h.assertTrue(brain.hasMemoryValue(attack)&&brain.hasMemoryValue(angry)
                &&brain.hasMemoryValue(look)&&brain.hasMemoryValue(walk),"Real Piglin Brain contains the attack, anger and tracking fixtures");
        CatAgentSmoke.clearAggro(enemy,agent);
        h.assertTrue(!brain.hasMemoryValue(attack)&&!brain.hasMemoryValue(angry)
                &&!brain.hasMemoryValue(look)&&!brain.hasMemoryValue(walk),"Agent-specific Brain pursuit memories cleared");
        brain.setMemory(attack,ally);brain.setMemory(angry,ally.getUUID());
        brain.setMemory(look,new net.minecraft.world.entity.ai.behavior.EntityTracker(ally,true));
        brain.setMemory(walk,new net.minecraft.world.entity.ai.memory.WalkTarget(ally,1,1));
        CatAgentSmoke.clearAggro(enemy,agent);
        h.assertTrue(brain.getMemory(attack).orElse(null)==ally
                &&brain.getMemory(angry).filter(ally.getUUID()::equals).isPresent()
                &&brain.hasMemoryValue(look)&&brain.hasMemoryValue(walk),"Unrelated Brain pursuit memories are preserved");
        agent.discard();ally.discard();enemy.discard();h.succeed();
        System.out.println("PASS: real Piglin Brain attack/anger/look/walk memories clear only for the smoke user");
    }
    @GameTest(template="artillery_probe",batch="agent_smoke_instant",timeoutTicks=30)
    public static void retreatReleasesSmokeImmediately(GameTestHelper h) {
        var level=h.getLevel();var base=floor(h);
        var agent=cat(level,base,CatOutfitType.AGENT);
        var ally=cat(level,base.add(3,0,0),CatOutfitType.ENGINEERING);
        var enemy=golem(level,base.add(6,0,0));
        CatAttributeData.set(agent,CatAttributeData.ensure(agent).withValues(CatStat.INTELLIGENCE,0,100));
        CatAttributeEffects.refresh(agent);
        h.runAfterDelay(5,()->{
            agent.setNoAi(false);agent.setOrderedToSit(false);agent.setInSittingPose(false);
            enemy.setTarget(agent);
            var goal=new CatAgentCombatGoal(agent);
            h.assertTrue(goal.canUse(),"Zero-Intelligence agent has a threat and a non-support ally");
            goal.tick();
            h.assertTrue(CatAgentSmoke.hiddenFrom(enemy,agent)&&enemy.getTarget()==null,"Retreat cloaks and clears aggro in the same tick");
            h.assertTrue(level.getEntitiesOfClass(cn.laowu.mod.entity.AgentSmokeBomb.class,
                    agent.getBoundingBox().inflate(16)).isEmpty(),"No throwable fire-charge entity is spawned");
            h.assertTrue(agent.getPersistentData().getLong(CatAgentCombatGoal.NEXT_SMOKE)==level.getGameTime()+200,
                    "Immediate release retains the ten-second cooldown");
            agent.discard();ally.discard();enemy.discard();h.succeed();
            System.out.println("PASS: immediate smoke at zero Intelligence without a visible projectile; cooldown retained");
        });
    }
    @GameTest(template="artillery_probe",batch="agent_smoke_golem",timeoutTicks=125)
    public static void ironGolemForgetsRevenge(GameTestHelper h) {
        var level=h.getLevel();var base=floor(h);
        var agent=cat(level,base,CatOutfitType.AGENT);
        var ally=cat(level,base.add(0,0,4),CatOutfitType.ENGINEERING);
        var enemy=golem(level,base.add(6,0,0));
        var other=golem(level,base.add(6,0,4));
        h.runAtTickTime(10,()->{
            h.assertTrue(enemy.hurt(level.damageSources().mobAttack(agent),1),"Real cat damage provokes the iron golem");
            h.assertTrue(other.hurt(level.damageSources().mobAttack(ally),1),"Control golem is independently provoked");
        });
        h.runAtTickTime(25,()->{
            h.assertTrue(!enemy.isNoAi()&&enemy.getTarget()==agent,"Live iron-golem revenge AI acquired the agent");
            h.assertTrue(enemy.targetSelector.getAvailableGoals().stream().anyMatch(g->g.isRunning()&&g.getGoal() instanceof HurtByTargetGoal),
                    "Vanilla revenge goal is actually running, with a cached target");
            CatAgentSmoke.burst(agent,agent.position());
            h.assertTrue(enemy.getTarget()==null&&enemy.getLastHurtByMob()==null,"Smoke immediately clears target and hurt source");
            h.assertTrue(enemy.getPersistentAngerTarget()==null&&enemy.getRemainingPersistentAngerTime()==0,"Anger UUID and timer cleared");
            h.assertTrue(other.getTarget()==ally&&other.getLastHurtByMob()==ally,"Other cats' aggro is unchanged");
        });
        for(int tick=26;tick<=73;tick++)h.runAtTickTime(tick,()->h.assertTrue(enemy.getTarget()!=agent,"Concealment prevents live AI reacquisition"));
        h.runAtTickTime(85,()->{
            h.assertTrue(!CatAgentSmoke.hiddenFrom(enemy,agent),"Concealment has really expired");
            h.assertTrue(enemy.getTarget()!=agent,"Expired smoke must not revive the cached revenge target");
            enemy.invulnerableTime=0;
            h.assertTrue(enemy.hurt(level.damageSources().mobAttack(agent),1),"New provocation remains possible");
        });
        h.runAtTickTime(105,()->{
            h.assertTrue(enemy.getTarget()==agent,"A new post-smoke attack can create fresh aggro");
            agent.discard();ally.discard();enemy.discard();other.discard();
            System.out.println("PASS: active iron-golem revenge, no stale post-smoke aggro, unrelated aggro preserved, fresh provocation allowed");
            h.succeed();
        });
    }
}
