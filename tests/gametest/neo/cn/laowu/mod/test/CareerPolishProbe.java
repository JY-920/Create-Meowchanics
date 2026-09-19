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
public final class CareerPolishProbe {
    @GameTest(template="artillery_probe",batch="farm23",timeoutTicks=30)
    public static void cockroachFarming(GameTestHelper h) {
        var level=h.getLevel();var at=CareerSupportIntegrationProbe.floor(h).add(7,0,5);
        var cat=CareerSupportIntegrationProbe.cat(level,at,CatOutfitType.COCKROACH,true);
        cat.setOrderedToSit(true);cat.setInSittingPose(true);
        // This reused underground GameTest arena needs real light propagation before planting.
        for(int x=-4;x<=4;x+=4)for(int z=-4;z<=4;z+=4)
            level.setBlockAndUpdate(cat.blockPosition().offset(x,3,z),Blocks.GLOWSTONE.defaultBlockState());
        h.runAfterDelay(5,()->{
        var center=cat.blockPosition().below();
        var edge=center.offset(4,0,0);var outside=center.offset(5,0,0);
        var mature=center.offset(-1,0,0);var dirt=center.offset(1,0,0);
        soil(level,outside,false);soil(level,mature,true);
        level.setBlockAndUpdate(dirt,Blocks.DIRT.defaultBlockState());
        level.setBlockAndUpdate(dirt.above(),Blocks.WHEAT.defaultBlockState());
        h.assertTrue(!CatCockroachFarming.fertilize(cat),"Exclude x+5, mature crops and crops without farmland");
        soil(level,edge,false);soil(level,center,false);
        var inventory=CatProfileData.openContainer(cat);inventory.setItem(CatProfileData.ACCESSORY_SLOTS,new ItemStack(Items.BONE_MEAL,7));
        h.assertTrue(CatCockroachFarming.available(cat),"Farm available; noAI="+cat.isNoAi()+", target="+cat.getTarget()+", viewed="+CatProfileData.isBeingViewed(cat));
        h.assertTrue(level.getBlockState(edge.above()).is(Blocks.WHEAT)&&level.getBlockState(center.above()).is(Blocks.WHEAT),
                "Fixture retains both crops before dose: edge="+level.getBlockState(edge.above())+", center="+level.getBlockState(center.above()));
        h.assertTrue(CatCockroachFarming.fertilize(cat),"9-cube includes x+4 farmland; center="+cat.blockPosition()+", edge="+edge);
        int changed=(age(level,edge)>0?1:0)+(age(level,center)>0?1:0);
        h.assertTrue(changed==1&&age(level,outside)==0&&age(level,mature)==7&&(level.getBlockState(dirt.above()).isAir()||age(level,dirt)==0),
                "A dose affects exactly one eligible crop; not every crop in the cube");
        h.assertTrue(inventory.getItem(CatProfileData.ACCESSORY_SLOTS).getCount()==7,"No inventory consumption");
        h.assertTrue(CatCockroachFarming.chance(0)==0&&CatCockroachFarming.chance(50)==.25
                &&CatCockroachFarming.chance(100)==.5&&CatCockroachFarming.chance(999)==1,
                "Health-based probability is monotonic and bounded");
        var above=center.offset(0,6,2);soil(level,above,false);
        soil(level,center,true);soil(level,edge,true);
        h.assertTrue(!CatCockroachFarming.fertilize(cat)&&age(level,above)==0,"Vertical outside farmland remains untouched");
        soil(level,center,false);
        var threat=enemy(level,at.add(2,0,2));
        cat.setTarget(threat);
        h.assertTrue(!CatCockroachFarming.fertilize(cat),"Combat pauses farm work");
        cat.setTarget(null);
        CatProfileData.beginViewing(cat);
        h.assertTrue(!CatCockroachFarming.fertilize(cat),"Profile editing pauses farm work");
        CatProfileData.endViewing(cat);
        cat.setNoAi(false);
        CareerSupportIntegrationProbe.stat(cat,CatStat.HEALTH,100);
        long seed=0;
        while(net.minecraft.util.RandomSource.create(seed).nextDouble()>=.5)seed++;
        cat.getRandom().setSeed(seed);
        cat.getPersistentData().putLong("LaoWuCockroachNextFertilize",level.getGameTime());
        CatCockroachFarming.tick(cat);
        int grown=age(level,center);
        h.assertTrue(grown>0,"Real work tick applies successful Health roll");
        CatCockroachFarming.tick(cat);
        h.assertTrue(age(level,center)==grown&&cat.getPersistentData().getLong("LaoWuCockroachNextFertilize")==level.getGameTime()+100,
                "Five-second cooldown prevents per-tick growth");
        CatClothesData.unequip(cat);
        h.assertTrue(!CatCockroachFarming.fertilize(cat),"Unequipping disables farming");
        cat.discard();threat.discard();h.succeed();
        System.out.println("PASS: 9-cube farm eligibility/bounds, one-dose growth, Health chance, cooldown, no item consumption");
        });
    }
    private static void soil(ServerLevel level,BlockPos pos,boolean mature) {
        level.setBlockAndUpdate(pos,Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE,7));
        level.setBlockAndUpdate(pos.above(),Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,mature?7:0));
    }
    private static int age(ServerLevel level,BlockPos soil){return level.getBlockState(soil.above()).getValue(CropBlock.AGE);}
    private static net.minecraft.world.entity.monster.Husk enemy(ServerLevel level,Vec3 at) {
        var mob=EntityType.HUSK.create(level);mob.setPos(at);mob.setNoAi(true);mob.setNoGravity(true);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);mob.getAttribute(Attributes.ARMOR).setBaseValue(0);mob.setHealth(1000);
        level.addFreshEntity(mob);
        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,600,1));
        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,600,0));
        return mob;
    }
    @GameTest(template="artillery_probe",batch="spray23",timeoutTicks=85)
    public static void divingWaterSpray(GameTestHelper h) {
        var level=h.getLevel();var at=CareerSupportIntegrationProbe.floor(h).add(5,0,5);
        var cat=CareerSupportIntegrationProbe.cat(level,at,CatOutfitType.DIVING,false);
        var primary=enemy(level,at.add(2,0,0));var second=enemy(level,at.add(3,0,.6));var rear=enemy(level,at.add(-2,0,0));
        var ally=CareerSupportIntegrationProbe.cat(level,at.add(2,0,-.5),CatOutfitType.COCKROACH,false);
        ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,600,1));float allyHealth=ally.getHealth();
        h.assertTrue(CatDivingAttack.spray(cat,primary)==2,"Water cone damages multiple enemies");
        h.assertTrue(primary.getHealth()<1000&&second.getHealth()<1000&&!primary.hasEffect(MobEffects.MOVEMENT_SPEED)
                &&!second.hasEffect(MobEffects.MOVEMENT_SPEED)&&primary.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                "Water removes positive potions, retaining negative effects");
        h.assertTrue(rear.getHealth()==1000&&rear.hasEffect(MobEffects.MOVEMENT_SPEED)
                &&ally.getHealth()==allyHealth&&ally.hasEffect(MobEffects.MOVEMENT_SPEED),"Rear targets and friendly cats untouched");
        primary.invulnerableTime=0;primary.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,600,1));primary.setInvulnerable(true);
        CatDivingAttack.spray(cat,primary);
        h.assertTrue(primary.hasEffect(MobEffects.MOVEMENT_SPEED),"Cancelled damage does not dispel buffs");
        primary.setInvulnerable(false);primary.invulnerableTime=0;
        for(int y=0;y<3;y++)for(int z=-1;z<=1;z++)level.setBlockAndUpdate(BlockPos.containing(at).offset(1,y,z),Blocks.STONE.defaultBlockState());
        float before=primary.getHealth();CatDivingAttack.spray(cat,primary);
        h.assertTrue(primary.getHealth()==before&&primary.hasEffect(MobEffects.MOVEMENT_SPEED),"Solid walls block both damage and dispel");
        for(int y=0;y<3;y++)for(int z=-1;z<=1;z++)level.setBlockAndUpdate(BlockPos.containing(at).offset(1,y,z),Blocks.AIR.defaultBlockState());
        second.discard();rear.discard();ally.discard();
        primary.setPos(at.add(3,0,0));primary.invulnerableTime=0;
        cat.setNoAi(false);cat.setNoGravity(false);cat.setTarget(primary);
        h.runAfterDelay(60,()->{
            h.assertTrue(primary.getHealth()<before&&!primary.hasEffect(MobEffects.MOVEMENT_SPEED),
                    "Installed career goal uses water spray, not an old melee bite");
            cat.discard();primary.discard();h.succeed();
            System.out.println("PASS: installed diving goal, cone damage, beneficial-only dispel, friendly fire, invulnerability and wall checks");
        });
    }
    @GameTest(template="artillery_probe",batch="agent_motion23",timeoutTicks=35)
    public static void agentAnimationSync(GameTestHelper h) {
        var level=h.getLevel();var at=CareerSupportIntegrationProbe.floor(h).add(5,0,4);
        var cat=CareerSupportIntegrationProbe.cat(level,at,CatOutfitType.AGENT,false);
        cat.setOrderedToSit(false);cat.setInSittingPose(false);
        CatAgentMeleeMotion.begin(cat);var strike=CatAgentMeleeMotion.current(cat);
        h.assertTrue(strike!=null&&strike.move()>=0&&strike.move()<3&&strike.duration()>=6&&strike.duration()<=18,
                "Server selects bounded strike and speed-linked duration");
        var packet=new cn.laowu.mod.network.AgentMeleePacket(cat.getId(),cat.getUUID(),strike.move(),strike.duration(),3);
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
        try {
            cn.laowu.mod.network.AgentMeleePacket.STREAM_CODEC.encode(buffer,packet);
            h.assertTrue(cn.laowu.mod.network.AgentMeleePacket.STREAM_CODEC.decode(buffer).equals(packet)&&!buffer.isReadable(),"S2C attack packet roundtrip");
        } finally {buffer.release();}
        h.runAfterDelay(20,()->{
            h.assertTrue(CatAgentMeleeMotion.current(cat)==null,"Cosmetic pose expires independently of gameplay");
            CatClothesData.unequip(cat);CatAgentMeleeMotion.begin(cat);
            h.assertTrue(CatAgentMeleeMotion.current(cat)==null,"Wrong outfit cannot start combat animation");
            cat.discard();h.succeed();
            System.out.println("PASS: server attack pose selection/timing, S2C roundtrip, expiry and outfit gate");
        });
    }
}
