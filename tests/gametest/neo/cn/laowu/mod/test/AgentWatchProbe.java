package cn.laowu.mod.test;

import cn.laowu.mod.*;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.network.AgentWatchPacket;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class AgentWatchProbe {
    private static Cat seated(GameTestHelper h,Vec3 at,int intelligence) {
        var level=h.getLevel();
        var cat=CareerSupportIntegrationProbe.cat(level,at,CatOutfitType.AGENT,true);
        CareerSupportIntegrationProbe.stat(cat,CatStat.INTELLIGENCE,intelligence);
        var pos=BlockPos.containing(at);
        level.setBlockAndUpdate(pos,AllBlocks.SEATS.get(DyeColor.WHITE).getDefaultState());
        SeatBlock.sitDown(level,pos,cat);cat.getVehicle().positionRider(cat);
        cat.setOrderedToSit(true);cat.setInSittingPose(true);
        CareerCatBehavior.tick(cat);
        h.assertTrue(CatAgentWatch.available(cat),"Real Create Seat enables watch");
        return cat;
    }
    private static <T extends Mob> T mob(ServerLevel level,EntityType<T> type,Vec3 at) {
        T mob=type.create(level);mob.setPos(at);mob.setNoAi(true);mob.setNoGravity(true);level.addFreshEntity(mob);return mob;
    }
    private static void finish(Cat cat) {
        var seat=CareerCatBehavior.findSeat(cat);cat.stopRiding();
        if(seat!=null)cat.level().setBlockAndUpdate(seat,Blocks.AIR.defaultBlockState());
        CatAgentWatch.stop(cat);cat.discard();
    }
    @GameTest(template="artillery_probe",batch="agent_watch_radius",timeoutTicks=95)
    public static void radiusHostilesAndWalls(GameTestHelper h) {
        var level=h.getLevel();var base=CareerSupportIntegrationProbe.floor(h).add(2,0,5);
        var cat=seated(h,base,50);var origin=cat.position();
        var near=mob(level,EntityType.HUSK,origin.add(31.9,0,0));
        var mid=mob(level,EntityType.HUSK,origin.add(32.1,0,0));
        var edge=mob(level,EntityType.HUSK,origin.add(64,0,0));
        var outside=mob(level,EntityType.HUSK,origin.add(65,0,0));
        var above=mob(level,EntityType.HUSK,origin.add(0,6,0));
        var invisible=mob(level,EntityType.HUSK,origin.add(4,0,0));invisible.setInvisible(true);
        var cow=mob(level,EntityType.COW,origin.add(3,0,2));
        var golem=mob(level,EntityType.IRON_GOLEM,origin.add(3,0,-2));golem.setTarget(cat);
        var friend=CareerSupportIntegrationProbe.cat(level,origin.add(3,0,1),CatOutfitType.AGENT,false);
        for(int y=0;y<4;y++)for(int z=-2;z<=2;z++)level.setBlockAndUpdate(BlockPos.containing(origin).offset(2,y,z),Blocks.STONE.defaultBlockState());
        h.assertTrue(!cat.hasLineOfSight(near),"Fixture has a real opaque wall");
        // Forge entity-section visibility can lag addFreshEntity in newly ticketed chunks.
        // Start the geometry assertions only after every long-distance fixture is searchable.
        h.startSequence().thenWaitUntil(()->h.assertTrue(
                level.getEntity(cat.getUUID())==cat && level.getEntity(near.getUUID())==near
                && level.getEntity(mid.getUUID())==mid && level.getEntity(edge.getUUID())==edge
                && level.getEntity(outside.getUUID())==outside,
                "Waiting for tracked watch fixture entities")).thenExecute(()->{
        CatAgentWatch.stop(cat);CatAgentWatch.tick(cat);CatAgentWatch.flush(level);
        h.assertTrue(CatAgentWatch.marked(near)&&!CatAgentWatch.marked(mid)&&!CatAgentWatch.marked(edge),
                "50 Intelligence has an exact 32-block radius, without a line-of-sight gate");
        h.assertTrue(CatAgentWatch.marked(above)&&CatAgentWatch.marked(invisible)&&CatAgentWatch.marked(golem)
                &&!CatAgentWatch.marked(cow)&&!CatAgentWatch.marked(friend),"3D/invisible threats and angry neutrals included; passive/friendly mobs excluded");
        h.assertTrue(CatAgentWatch.radius(0)==0&&CatAgentWatch.radius(50)==32&&CatAgentWatch.radius(100)==64
                &&CatAgentWatch.radius(200)==128&&CatAgentWatch.radius(999)==128&&CatAgentWatch.radius(-1)==0&&CatAgentWatch.radius(Double.NaN)==0,
                "Finite linear Intelligence radius capped at 128");
        var packet=new AgentWatchPacket(near.getId(),near.getUUID(),CatAgentWatch.VISUAL_TICKS);
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
        try {
            AgentWatchPacket.STREAM_CODEC.encode(buffer,packet);
            h.assertTrue(AgentWatchPacket.STREAM_CODEC.decode(buffer).equals(packet)&&!buffer.isReadable(),"Watch S2C packet roundtrip");
        }finally{buffer.release();}
        CareerSupportIntegrationProbe.stat(cat,CatStat.INTELLIGENCE,100);
        h.runAfterDelay(22,()->{
            h.assertTrue(CatAgentWatch.marked(mid)&&CatAgentWatch.marked(edge)&&!CatAgentWatch.marked(outside),
                    "Live Intelligence update reaches exactly 64, never 65: radius="+CatAgentWatch.radius(cat)
                            +", marked="+CatAgentWatch.marked(mid)+","+CatAgentWatch.marked(edge)+","+CatAgentWatch.marked(outside)
                            +", cat="+cat.position()+", origin="+origin+", edgeDistance="+Math.sqrt(cat.distanceToSqr(edge))
                            +", edgeVisible="+level.getEntitiesOfClass(Mob.class,cat.getBoundingBox().inflate(65)).contains(edge)
                            +", available="+CatAgentWatch.available(cat));
            CareerSupportIntegrationProbe.stat(cat,CatStat.INTELLIGENCE,10);
        });
        h.runAfterDelay(44,()->{
            h.assertTrue(!CatAgentWatch.marked(near)&&CatAgentWatch.marked(above),"Shrinking radius removes only distant marks");
            above.setPos(origin.add(3,6,0)); // 6.71 > 6.4 although inside the search AABB.
        });
        h.runAfterDelay(66,()->{
            h.assertTrue(!CatAgentWatch.marked(above),"Sphere excludes the vertical AABB corner");
            h.assertTrue(!near.hasGlowingTag()&&!near.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING),
                    "Never mutates server glow flags or adds potions");
            finish(cat);CatAgentWatch.flush(level);
            for(var e:List.of(near,mid,edge,outside,above,invisible,cow,golem,friend))e.discard();
            System.out.println("PASS: seated watch 32/64 sphere, live radius/cap, walls/invisibility, hostile filtering, removal and S2C serialization");
            h.succeed();
        });
        });
    }
    @GameTest(template="artillery_probe",batch="agent_watch_lifecycle",timeoutTicks=65)
    public static void overlappingWatchersAndCleanup(GameTestHelper h) {
        var level=h.getLevel();var base=CareerSupportIntegrationProbe.floor(h).add(3,0,4);
        var a=seated(h,base,50);var b=seated(h,base.add(4,0,0),50);
        var enemy=mob(level,EntityType.HUSK,base.add(5,0,2));
        enemy.setGlowingTag(true);
        var team=level.getScoreboard().addPlayerTeam("watch_"+enemy.getId());
        team.setColor(net.minecraft.ChatFormatting.BLUE);
        level.getScoreboard().addPlayerToTeam(enemy.getScoreboardName(),team);
        CatAgentWatch.flush(level);h.assertTrue(CatAgentWatch.marked(enemy),"Overlapping observers mark enemy");
        CatClothesData.unequip(a);CatAgentWatch.flush(level);
        h.assertTrue(CatAgentWatch.marked(enemy),"Removing one agent cannot clear another agent's mark");
        CatProfileData.beginViewing(b);CatAgentWatch.tick(b);CatAgentWatch.flush(level);
        h.assertTrue(!CatAgentWatch.marked(enemy),"Profile pause cancels last watch");
        CatProfileData.endViewing(b);CareerCatBehavior.tick(b);CatAgentWatch.flush(level);
        h.assertTrue(CatAgentWatch.marked(enemy),"Resuming seated cat restarts watch");
        b.stopRiding();b.setPos(base.add(7,0,4));b.setOrderedToSit(true);b.setInSittingPose(true);
        CareerCatBehavior.tick(b);CatAgentWatch.flush(level);
        h.assertTrue(!CatAgentWatch.marked(enemy)&&!CatAgentWatch.available(b),"Sitting on bare ground is not a workstation");
        h.assertTrue(enemy.hasGlowingTag()&&enemy.getTeam()==team&&enemy.getTeamColor()==net.minecraft.ChatFormatting.BLUE.getColor(),
                "Existing vanilla glow and scoreboard color remain untouched");
        var c=seated(h,base.add(9,0,0),100);
        CatAgentWatch.flush(level);h.assertTrue(CatAgentWatch.marked(enemy),"Third observer resumes coverage");
        var cSeat=CareerCatBehavior.findSeat(c);c.discard();
        h.runAfterDelay(22,()->{
            h.assertTrue(!CatAgentWatch.marked(enemy),"Removed/unloaded observer expires without a permanent mark");
            finish(a);finish(b);level.setBlockAndUpdate(cSeat,Blocks.AIR.defaultBlockState());
            level.getScoreboard().removePlayerTeam(team);enemy.discard();CatAgentWatch.flush(level);
            System.out.println("PASS: merged observers, unseat/unequip/profile/removal cleanup; pre-existing glow/team preserved");
            h.succeed();
        });
    }
}
