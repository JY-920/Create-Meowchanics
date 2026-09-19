package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.entity.*;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.network.CockroachStatePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import java.util.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class SpecialistAbilitiesProbe {
    @GameTest(template="artillery_probe",batch="swarm_abilities",timeoutTicks=75)
    public static void swarmStatsAndSmoke(GameTestHelper h) {
        var level=h.getLevel();var at=CareerSupportIntegrationProbe.floor(h).add(5,0,4);
        Cat cat=CareerSupportIntegrationProbe.cat(level,at,CatOutfitType.COCKROACH,false);
        Cat a=CareerSupportIntegrationProbe.cat(level,at.add(2,0,0),CatOutfitType.COCKROACH,false);
        Cat b=CareerSupportIntegrationProbe.cat(level,at.add(0,2,0),CatOutfitType.COCKROACH,false);
        Cat foreign=CareerSupportIntegrationProbe.cat(level,at.add(0,0,2),CatOutfitType.COCKROACH,false);
        var collar=foreign.saveWithoutId(new CompoundTag());collar.putByte("CollarColor",(byte)net.minecraft.world.item.DyeColor.BLUE.getId());foreign.load(collar);
        var genes=CatAttributeData.ensure(cat).save();
        float health=cat.getMaxHealth();
        CatCockroachSwarm.clear(cat);CatCockroachSwarm.tick(cat);
        h.assertTrue(CatCockroachSwarm.allies(cat)==2,"Sphere includes vertical allies, excludes other collar and self");
        for(var stat:new CatStat[]{CatStat.HEALTH,CatStat.ATTACK})
            h.assertTrue(CatAttributeEffects.effectiveValue(cat,stat)==62,"Two allies add twelve effective "+stat);
        h.assertTrue(cat.getMaxHealth()>health&&CatAttributeEffects.effectiveValue(cat,CatStat.STAMINA)==50
                &&CatAttributeEffects.effectiveValue(cat,CatStat.SPEED)==50
                &&genes.equals(CatAttributeData.ensure(cat).save()),"Swarm updates live health, not speed or inherited genes");
        cat.hurt(level.damageSources().generic(),1);CatCockroachSwarm.tick(cat);
        h.assertTrue(CatCockroachSwarm.mode(cat)==1,"Actual damage event starts wings-only reaction");
        a.discard();
        var agent=CareerSupportIntegrationProbe.cat(level,at.add(-2,0,0),CatOutfitType.AGENT,false);
        var mob=EntityType.HUSK.create(level);mob.setNoAi(true);mob.setPos(at.add(-2,0,2));level.addFreshEntity(mob);
        var untouched=EntityType.HUSK.create(level);untouched.setNoAi(true);untouched.setPos(at.add(-3,0,2));level.addFreshEntity(untouched);
        mob.setTarget(agent);mob.setLastHurtByMob(agent);untouched.setTarget(cat);
        // Ordinary goal-based mobs have no registered ATTACK_TARGET / ANGRY_AT memories.
        CatAgentSmoke.burst(agent,agent.position());
        h.assertTrue(mob.getTarget()==null&&mob.getLastHurtByMob()==null&&untouched.getTarget()==cat,
                "Smoke clears only the agent's aggro, with unregistered Brain memories safe");
        mob.setTarget(agent);
        h.assertTrue(mob.getTarget()==null&&CatAgentSmoke.hiddenFrom(mob,agent),"Short cloak prevents immediate reacquisition");
        h.runAfterDelay(25,()->{
            h.assertTrue(CatCockroachSwarm.allies(cat)==1&&CatAttributeEffects.effectiveValue(cat,CatStat.HEALTH)==56
                    &&CatCockroachSwarm.mode(cat)==0,"Ally removal and wings reaction expire without permanent bonuses");
            CatClothesData.unequip(cat);
            h.assertTrue(CatAttributeEffects.effectiveValue(cat,CatStat.HEALTH)==50,"Removing outfit releases all swarm stats");
        });
        h.runAfterDelay(55,()->{
            mob.setTarget(agent);h.assertTrue(mob.getTarget()==agent,"Smoke is temporary, not permanent invisibility");
            for(var e:List.of(cat,b,foreign,agent,mob,untouched))e.discard();
            System.out.println("PASS: live 3D swarm stats/genes, hurt animation state, ally expiry and scoped smoke aggro/expiry");
            h.succeed();
        });
    }
    @GameTest(template="artillery_probe",batch="agent_abilities",timeoutTicks=160)
    public static void agentBackstabAndRetreat(GameTestHelper h) {
        var level=h.getLevel();var base=CareerSupportIntegrationProbe.floor(h);
        // Extra retreat room stays inside this isolated fixture.
        for(int x=18;x<35;x++)for(int z=0;z<12;z++)h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
        var cat=CareerSupportIntegrationProbe.cat(level,base.add(12,0,4),CatOutfitType.AGENT,true);
        CareerSupportIntegrationProbe.stat(cat,CatStat.INTELLIGENCE,100);
        var mob=EntityType.HUSK.create(level);mob.setNoAi(true);mob.setPos(base.add(15,0,4));
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);mob.setHealth(1000);
        mob.getAttribute(Attributes.ARMOR).setBaseValue(0);level.addFreshEntity(mob);
        for(int yaw=-180;yaw<=180;yaw+=15){
            mob.yBodyRot=yaw;cat.setPos(CatAgentCombatGoal.rearPosition(mob));
            h.assertTrue(CatAgentCombatGoal.guaranteedCritical(cat,mob,cat),"Every rear body heading guarantees a direct critical");
            h.assertTrue(!CatAgentCombatGoal.guaranteedCritical(cat,mob,mob),"A projectile/other source is not a melee backstab");
            cat.setPos(mob.position().add(Vec3.directionFromRotation(0,yaw).scale(1.5)));
            h.assertTrue(!CatAgentCombatGoal.behind(cat,mob),"Front sector is not a rear critical");
        }
        mob.yBodyRot=0;cat.setPos(CatAgentCombatGoal.rearPosition(mob));
        for(int i=0;i<12;i++){
            mob.invulnerableTime=0;mob.setHealth(1000);
            mob.hurt(level.damageSources().mobAttack(cat),5);
            h.assertTrue(Math.abs(mob.getHealth()-990)<.001,"Real damage event applies exactly one intelligence critical multiplier");
        }
        mob.setHealth(1000);cat.setPos(base.add(12,0,4));mob.setYRot(90);mob.yBodyRot=90;
        cat.setTarget(mob);CatCombatControl.tick(cat);mob.setTarget(cat);
        h.assertTrue(!CatAgentCombatGoal.hasNearbyAlly(cat),"Alone does not qualify for smoke retreat");
        var rearReached=new java.util.concurrent.atomic.AtomicBoolean();
        for(int tick=1;tick<=65;tick++)h.runAtTickTime(tick,()->{
            if(CatAgentCombatGoal.behind(cat,mob))rearReached.set(true);
        });
        h.runAfterDelay(70,()->{
            h.assertTrue(rearReached.get(),"Intelligent registered AI really circles to the rear, not just a rear destination");
            h.assertTrue(!cat.getPersistentData().contains(CatAgentCombatGoal.NEXT_SMOKE),"No smoke retreat while alone");
            h.assertTrue(cat.getPersistentData().getLong(CatAgentCombatGoal.NEXT_ATTACK)>0&&mob.getHealth()<1000,
                    "Actual registered agent AI approaches and attacks");
            var ally=CareerSupportIntegrationProbe.cat(level,cat.position().add(0,0,2),CatOutfitType.COCKROACH,false);
            mob.setTarget(cat);cat.setTarget(mob);
            Vec3 before=cat.position();
            h.runAfterDelay(30,()->{
                h.assertTrue(cat.getPersistentData().getLong(CatAgentCombatGoal.NEXT_SMOKE)>level.getGameTime(),
                        "Intelligent threatened agent with an ally throws smoke");
                h.assertTrue(cat.position().distanceToSqr(before)>1&&mob.getTarget()!=cat,
                        "Retreat actually moves, grenade actually breaks aggro");
                cat.setOrderedToSit(true);cat.discard();ally.discard();mob.discard();
                System.out.println("PASS: all rear headings, 12 real guaranteed backstabs, registered attack AI, alone gate and actual smoke retreat");
                h.succeed();
            });
        });
    }
    @GameTest(template="artillery_probe",batch="diving_abilities",timeoutTicks=80)
    public static void divingMountAndControls(GameTestHelper h) {
        var level=h.getLevel();var base=CareerSupportIntegrationProbe.floor(h);
        for(int x=3;x<=13;x++)for(int y=1;y<=6;y++)for(int z=3;z<=9;z++)
            h.setBlock(new BlockPos(x,y,z),Blocks.WATER);
        var at=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(7,3,6)));
        var stub=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"dive-stub"));
        var owner=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,
                new com.mojang.authlib.GameProfile(UUID.randomUUID(),"diver-probe"),net.minecraft.server.level.ClientInformation.createDefault());
        owner.connection=stub.connection;owner.setPos(at);
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,com.simibubi.create.AllItems.WRENCH.asStack());
        var cat=CareerSupportIntegrationProbe.cat(level,at,CatOutfitType.DIVING,true,owner);
        for(int stat:new int[]{0,50,100,999999})
            h.assertTrue(Math.abs(CatDivingRules.speed(stat,true)/CatPilotFlightRules.speedPerTick(stat)-1.3)<1e-8
                    &&Math.abs(CatDivingRules.speed(stat,false)/CatDivingRules.speed(stat,true)-.2)<1e-8,
                    "Unbounded water speed +30%, land -80%");
        h.assertTrue(CatDivingMount.duration(cat)==CatPilotFlight.duration(cat),"Diver uses the same effective stamina duration");
        var down=CatDivingRules.swim(Vec3.ZERO,1,0,0,-90,false,true,false,1);
        var exhausted=CatDivingRules.swim(new Vec3(0,-2,0),1,0,0,90,false,true,true,1);
        h.assertTrue(down.y<0&&exhausted.y>=.12,"Ctrl dives; exhausted movement always surfaces");
        stub.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,com.simibubi.create.AllItems.WRENCH.asStack());
        h.assertTrue(!CatDivingMount.start(cat,stub),"Other owner cannot mount");
        long capacity=CatDivingMount.duration(cat);cat.getPersistentData().putLong(CatDivingMount.USED,capacity-8);
        h.assertTrue(CatDivingMount.interact(cat,owner,net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction(),"Wrench mounts real owned diver");
        var carrier=(CatDivingCarrier)cat.getVehicle();
        h.assertTrue(owner.getVehicle()==carrier&&Math.abs(owner.getY()-cat.getY()-CatDivingCarrier.RIDER_HEIGHT)<1e-6
                &&carrier.getControllingPassenger()==null&&!CareerCatBehavior.canParticipateInCombat(cat),"Back-riding offsets and server authority");
        var saved=new CompoundTag();h.assertTrue(carrier.saveAsPassenger(saved),"Mount root is saveable");
        var restored=EntityType.loadEntityRecursive(saved,level,e->e);
        var loaded=((CatDivingCarrier)restored).cat();restored.tick();
        h.assertTrue(!loaded.isRemoved()&&!loaded.isPassenger(),"Orphan saved carrier safely releases the original cat");
        h.runAtTickTime(1,()->{cat.setAirSupply(5);owner.setAirSupply(5);carrier.input(owner,1,0,0,-90,false,true);});
        h.runAtTickTime(4,()->{
            h.assertTrue(carrier.swimming()&&!carrier.surfacing()&&carrier.getDeltaMovement().y<0,
                    "Server movement really follows Ctrl underwater");
            h.assertTrue(cat.getAirSupply()>200&&owner.getAirSupply()>200,"Powered dive supports cat and rider breathing");
            carrier.input(stub,1,0,0,0,true,false);carrier.input(owner,Float.NaN,0,0,0,true,false);
        });
        h.runAtTickTime(14,()->{
            h.assertTrue(carrier.surfacing()&&carrier.getDeltaMovement().y>0&&carrier.seconds()==0,"Real stamina exhaustion starts ascent");
            owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
        });
        h.runAtTickTime(17,()->{
            h.assertTrue(carrier.isRemoved()&&!owner.isPassenger()&&!cat.isPassenger()&&!cat.isNoGravity(),
                    "Putting away wrench safely releases both riders and normal gravity");
            cat.discard();owner.discard();stub.discard();
            System.out.println("PASS: actual underwater mount, ownership, 3D controls, +30/-80 speed, air, exhausted ascent, orphan reload and clean release");
            h.succeed();
        });
    }
}
