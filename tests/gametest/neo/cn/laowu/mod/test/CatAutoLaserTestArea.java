package cn.laowu.mod.test;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.ChunkPos;

/** Test-only chunk readiness. No production chunk loading or widened behavior assertions. */
final class CatAutoLaserTestArea {
    private static final net.minecraft.server.level.TicketType<UUID> TICKET=net.minecraft.server.level.TicketType.create("laowu_auto_laser_test",Comparator.comparing(UUID::toString));
    static void ready(GameTestHelper h,Runnable scene) {
        var level=h.getLevel();var first=new ChunkPos(h.absolutePos(BlockPos.ZERO));
        var last=new ChunkPos(h.absolutePos(new BlockPos(71,11,13)));
        List<ChunkPos> chunks=new ArrayList<>(),added=new ArrayList<>();UUID ticketId=UUID.randomUUID();
        Runnable cleanup=()->{for(var chunk:added)level.getChunkSource().removeRegionTicket(TICKET,chunk,2,ticketId);added.clear();};
        h.testInfo.addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo info){}
            public void testPassed(GameTestInfo info,GameTestRunner runner){cleanup.run();}
            public void testFailed(GameTestInfo info,GameTestRunner runner){cleanup.run();}
            public void testAddedForRerun(GameTestInfo oldInfo,GameTestInfo newInfo,GameTestRunner runner){cleanup.run();}
        });
        for(int x=first.x;x<=last.x;x++)for(int z=first.z;z<=last.z;z++){
            var chunk=new ChunkPos(x,z);chunks.add(chunk);
            if(level.getServer() instanceof GameTestServer){
                level.getChunkSource().addRegionTicket(TICKET,chunk,2,ticketId);added.add(chunk);
            }
        }
        boolean[] started={false};
        h.runAfterDelay(100,()->h.assertTrue(started[0],"Automatic laser fixture did not become entity-ticking within100ticks"));
        h.startSequence().thenWaitUntil(()->{
            for(var chunk:chunks)h.assertTrue(level.isPositionEntityTicking(
                new BlockPos(chunk.getMinBlockX(),h.absolutePos(BlockPos.ZERO).getY(),chunk.getMinBlockZ())),
                "Automatic laser arena not entity-ticking yet: "+chunk);
        }).thenExecute(()->{started[0]=true;scene.run();});
    }
}
