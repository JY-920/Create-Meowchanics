package cn.laowu.mod.test;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.ChunkPos;

/** Isolated GameTestServer arena loading, matching StructureUtils.forceLoadChunks. */
final class CatAutoLaserTestArea {
    static void ready(GameTestHelper h,Runnable scene) {
        var level=h.getLevel();var first=new ChunkPos(h.absolutePos(BlockPos.ZERO));
        var last=new ChunkPos(h.absolutePos(new BlockPos(71,11,13)));
        List<ChunkPos> chunks=new ArrayList<>();
        for(int x=first.x;x<=last.x;x++)for(int z=first.z;z<=last.z;z++){
            var chunk=new ChunkPos(x,z);chunks.add(chunk);
            // Vanilla retains its test-structure chunks for the isolated server session.
            // Extend that region to the 72-block shooting arena. Never force player worlds.
            if(level.getServer() instanceof GameTestServer)level.setChunkForced(x,z,true);
        }
        boolean[] started={false};
        h.runAfterDelay(1000,()->h.assertTrue(started[0],"Automatic laser fixture did not become entity-ticking within1000ticks: "+
            chunks.stream().filter(c->!level.isPositionEntityTicking(new BlockPos(c.getMinBlockX(),h.absolutePos(BlockPos.ZERO).getY(),c.getMinBlockZ()))).toList()));
        h.startSequence().thenWaitUntil(()->{
            for(var chunk:chunks)h.assertTrue(level.isPositionEntityTicking(
                new BlockPos(chunk.getMinBlockX(),h.absolutePos(BlockPos.ZERO).getY(),chunk.getMinBlockZ())),
                "Automatic laser arena not entity-ticking yet: "+chunk);
        }).thenExecute(()->{started[0]=true;System.out.println("AUTO LASER fixture ready at tick="+h.getTick());scene.run();});
    }
}
