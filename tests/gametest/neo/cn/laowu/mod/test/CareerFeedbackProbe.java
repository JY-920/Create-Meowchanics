package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.genetics.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CareerFeedbackProbe {
    @GameTest(template="artillery_probe",batch="motion25",timeoutTicks=85)
    public static void completeFastAgentPoses(GameTestHelper h){
        var level=h.getLevel();var base=CareerSupportIntegrationProbe.floor(h);
        var cat=CareerSupportIntegrationProbe.cat(level,base.add(5,0,4),CatOutfitType.AGENT,false);
        CareerSupportIntegrationProbe.stat(cat,CatStat.SPEED,100);
        var moves=new java.util.BitSet(3);
        var previous=new CatAgentMeleeMotion.Strike[1];
        for(int tick=1;tick<=65;tick++)h.runAtTickTime(tick,()->{
            CatAgentMeleeMotion.begin(cat);
            var strike=CatAgentMeleeMotion.current(cat);
            h.assertTrue(strike!=null&&strike.duration()>=12,"Pose has enough visible playback time");
            if(previous[0]!=null&&level.getGameTime()<previous[0].started()+previous[0].duration())
                h.assertTrue(strike.started()==previous[0].started()&&strike.move()==previous[0].move(),"Rapid attacks cannot restart an unfinished pose");
            moves.set(strike.move());previous[0]=strike;
        });
        h.runAfterDelay(67,()->{
            h.assertTrue(moves.cardinality()==3,"All three complete martial poses are used");
            cat.discard();h.succeed();
            System.out.println("PASS: rapid agent attacks retain complete poses and cycle all three motions");
        });
    }
    @GameTest(template="artillery_probe",batch="smoke25",timeoutTicks=100)
    public static void trappedThreatenedAgent(GameTestHelper h){
        var level=h.getLevel();var base=CareerSupportIntegrationProbe.floor(h);
        var cat=CareerSupportIntegrationProbe.cat(level,base.add(7,0,5),CatOutfitType.AGENT,true);
        CareerSupportIntegrationProbe.stat(cat,CatStat.INTELLIGENCE,0);
        var pos=cat.blockPosition();
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(x!=0||z!=0)
            for(int y=0;y<=2;y++)level.setBlockAndUpdate(pos.offset(x,y,z),Blocks.STONE.defaultBlockState());
        var enemy=EntityType.HUSK.create(level);enemy.setNoAi(true);enemy.setPos(base.add(11,0,5));level.addFreshEntity(enemy);
        cat.setTarget(null);enemy.setTarget(cat);
        var goal=new CatAgentCombatGoal(cat);
        h.assertTrue(!goal.canUse(),"Zero-Intelligence agent still needs a nearby ally to use smoke");
        var ally=CareerSupportIntegrationProbe.cat(level,base.add(4,0,5),CatOutfitType.ENGINEERING,false);
        enemy.setTarget(null);
        h.assertTrue(!goal.canUse(),"An ally alone is not enough: an enemy must be targeting this agent");
        enemy.setTarget(cat);
        for(int intelligence:new int[]{0,59,60,100}){
            CareerSupportIntegrationProbe.stat(cat,CatStat.INTELLIGENCE,intelligence);
            h.assertTrue(CatAttributeEffects.effectiveValue(cat,CatStat.INTELLIGENCE)==intelligence,
                    "Fixture really has the requested effective Intelligence");
            h.assertTrue(goal.canUse(),"Smoke has no Intelligence threshold, including either side of the old 60 gate");
            cat.getPersistentData().putLong(CatAgentCombatGoal.NEXT_SMOKE,level.getGameTime()+200);
            h.assertTrue(!goal.canUse(),"Intelligence never bypasses the ten-second smoke cooldown");
            cat.getPersistentData().putLong(CatAgentCombatGoal.NEXT_SMOKE,level.getGameTime());
            h.assertTrue(goal.canUse(),"Smoke becomes available exactly when the cooldown expires");
            cat.getPersistentData().remove(CatAgentCombatGoal.NEXT_SMOKE);
        }
        CareerSupportIntegrationProbe.stat(cat,CatStat.INTELLIGENCE,0);
        h.runAfterDelay(20,()->{
            h.assertTrue(cat.getPersistentData().getLong(CatAgentCombatGoal.NEXT_SMOKE)>level.getGameTime(),"Zero-Intelligence registered AI throws smoke even with no retreat path or own attack target");
            h.assertTrue(enemy.getTarget()!=cat&&CatAgentSmoke.hiddenFrom(enemy,cat),"Grenade really breaks the threatening enemy's aggro");
            long nextSmoke=cat.getPersistentData().getLong(CatAgentCombatGoal.NEXT_SMOKE);
            h.runAfterDelay(60,()->{
                h.assertTrue(!CatAgentSmoke.hiddenFrom(enemy,cat),"Zero Intelligence does not alter the cloak duration");
                cat.setTarget(null);enemy.setTarget(cat);
                h.assertTrue(enemy.getTarget()==cat,"Expired concealment permits normal target acquisition");
                h.runAfterDelay(5,()->{
                    h.assertTrue(cat.getPersistentData().getLong(CatAgentCombatGoal.NEXT_SMOKE)==nextSmoke,
                            "Renewed aggro cannot restart smoke during its original cooldown");
                    cat.discard();ally.discard();enemy.discard();h.succeed();
                    System.out.println("PASS: smoke at Intelligence 0/59/60/100; real zero-Intelligence AI, ally/threat gates, cloak expiry and cooldown retained");
                });
            });
        });
    }
    @GameTest(template="artillery_probe",batch="smoke_support",timeoutTicks=80)
    public static void supportAlliesDoNotEnableSmoke(GameTestHelper h){
        var level=h.getLevel();var base=CareerSupportIntegrationProbe.floor(h);
        var cat=CareerSupportIntegrationProbe.cat(level,base.add(7,0,5),CatOutfitType.AGENT,true);
        CareerSupportIntegrationProbe.stat(cat,CatStat.INTELLIGENCE,0);
        var pos=cat.blockPosition();
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(x!=0||z!=0)
            for(int y=0;y<=2;y++)level.setBlockAndUpdate(pos.offset(x,y,z),Blocks.STONE.defaultBlockState());
        var enemy=EntityType.HUSK.create(level);enemy.setNoAi(true);enemy.setPos(base.add(11,0,5));level.addFreshEntity(enemy);
        cat.setTarget(null);enemy.setTarget(cat);
        var ally=CareerSupportIntegrationProbe.cat(level,base.add(4,0,5),CatOutfitType.MEDICAL,false);
        var goal=new CatAgentCombatGoal(cat);
        for(var outfit:CatOutfitType.values()){
            CatClothesData.equip(ally,outfit);
            boolean expected=!outfit.isSupport();
            h.assertTrue(CatAgentCombatGoal.hasNearbyAlly(cat)==expected&&goal.canUse()==expected,
                    "Smoke ally eligibility follows combat role for "+outfit+", including unchanged no-outfit cats");
        }
        CatClothesData.equip(ally,CatOutfitType.MEDICAL);
        var musician=CareerSupportIntegrationProbe.cat(level,base.add(3,0,5),CatOutfitType.MUSIC,false);
        var courier=CareerSupportIntegrationProbe.cat(level,base.add(4,0,4),CatOutfitType.TRANSPORT,false);
        h.runAfterDelay(20,()->{
            h.assertTrue(!cat.getPersistentData().contains(CatAgentCombatGoal.NEXT_SMOKE)
                    &&enemy.getTarget()==cat&&!CatAgentSmoke.hiddenFrom(enemy,cat),
                    "Real AI cannot conceal itself with only medical, music and logistics allies");
            CatClothesData.equip(ally,CatOutfitType.ENGINEERING);
            h.assertTrue(CatAgentCombatGoal.hasNearbyAlly(cat),"A ranged ally enables smoke even with support cats still nearby");
            h.runAfterDelay(20,()->{
                h.assertTrue(cat.getPersistentData().getLong(CatAgentCombatGoal.NEXT_SMOKE)>level.getGameTime()
                        &&enemy.getTarget()!=cat&&CatAgentSmoke.hiddenFrom(enemy,cat),
                        "Changing one ally to a non-support career lets zero-Intelligence AI really throw smoke");
                cat.discard();ally.discard();musician.discard();courier.discard();enemy.discard();h.succeed();
                System.out.println("PASS: all outfit roles checked; support-only team blocks smoke, mixed team permits real zero-Intelligence smoke");
            });
        });
    }
    @GameTest(template="artillery_probe",batch="pounce25",timeoutTicks=60)
    public static void wingedPounce(GameTestHelper h){
        var level=h.getLevel();var base=CareerSupportIntegrationProbe.floor(h);
        var cat=CareerSupportIntegrationProbe.cat(level,base.add(4,0,5),CatOutfitType.COCKROACH,true);
        var enemy=EntityType.HUSK.create(level);enemy.setNoAi(true);enemy.setPos(base.add(8,0,5));
        enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);enemy.setHealth(1000);level.addFreshEntity(enemy);
        cat.setTarget(enemy);
        var winged=new java.util.concurrent.atomic.AtomicBoolean();
        var airHit=new java.util.concurrent.atomic.AtomicBoolean();
        double originalY=cat.getY();float[] health={1000};
        for(int tick=1;tick<=35;tick++)h.runAtTickTime(tick,()->{
            if(cat.getY()>originalY+.3&&CatCockroachSwarm.mode(cat)==2)winged.set(true);
            if(enemy.getHealth()<health[0]&&!cat.onGround())airHit.set(true);
            health[0]=enemy.getHealth();
            if(cat.getPersistentData().getLong(CatCockroachCombat.NEXT_ATTACK)>level.getGameTime()){
                enemy.invulnerableTime=0;float before=enemy.getHealth();
                h.assertTrue(!CatCockroachCombat.tryAttack(cat,enemy)&&enemy.getHealth()==before,"Pounce and bite cannot bypass shared attack cooldown");
            }
        });
        h.runAfterDelay(38,()->{
            h.assertTrue(winged.get(),"Real goal-driven jump displays spread wings");
            h.assertTrue(airHit.get(),"Pounce actually attacks while airborne");
            h.assertTrue(cat.getPersistentData().getLong(CatCockroachCombat.NEXT_LEAP)>level.getGameTime(),"Pounce has bounded three-second pacing");
            cat.discard();enemy.discard();h.succeed();
            System.out.println("PASS: real winged jump, airborne hit, and shared ordinary attack cooldown");
        });
    }
    @GameTest(template="artillery_probe",batch="water25",timeoutTicks=10)
    public static void waterFlowerPacket(GameTestHelper h){
        var data=new cn.laowu.mod.particle.NozzleFluidPuffData(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1));
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try{cn.laowu.mod.particle.NozzleFluidPuffData.STREAM_CODEC.encode(buffer,data);
            var copy=cn.laowu.mod.particle.NozzleFluidPuffData.STREAM_CODEC.decode(buffer);
            h.assertTrue(copy.fluid().getFluid()==net.minecraft.world.level.material.Fluids.WATER&&!buffer.isReadable(),"Water flower S2C codec");}finally{buffer.release();}
        h.assertTrue(data.getType()==LaoWuMod.NOZZLE_FLUID_PUFF.get(),"Water flowers reuse the registered collector particle, no extra mixin");
        h.succeed();
        System.out.println("PASS: water flower particle server/client codec and original registered type");
    }
}
