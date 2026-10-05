package cn.laowu.mod.test;

import cn.laowu.mod.create.*;
import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("laowu")
@PrefixGameTestTemplate(false)
public final class CatBasinSpoutDiagnostic {
    @GameTestGenerator
    public static Collection<TestFunction> repeatColdSpouts() {
        var tests=new ArrayList<TestFunction>();
        for(int i=0;i<24;i++) {
            int sample=i;
            tests.add(new TestFunction("basin_spout_diagnostic","spout_diagnostic_"+i,"laowu:artillery_probe",400,0,true,
                h->{if(Boolean.getBoolean("laowu.spout_diagnostic_warmup"))runWarm(h,sample);else run(h,sample);}));
        }
        return tests;
    }

    private static void run(GameTestHelper h,int sample) {
        for(Direction bottom:Direction.values())for(Direction local:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}) {
            int i=local.ordinal()-2;
            var p=new BlockPos(5+bottom.ordinal()*11,3+(i/2)*5,3+(i%2)*7);
            var side=CatMachineOrientation.toWorld(bottom,local);
            var output=p.relative(bottom).relative(side);
            var state=CatMachineBlocks.HAJI_BASIN.get().defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom);
            h.setBlock(p,state);h.setBlock(output,state);
            var source=(BasinBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p));
            h.runAfterDelay(10,()-> {
                if(h.getBlockState(p).getValue(BasinBlock.FACING)!=local)source.onWrenched(local);
                h.assertTrue(source.acceptOutputs(List.of(new ItemStack(Items.IRON_INGOT,3)),List.of(),false),"Diagnostic output rejected");
            });
            for(int tick:new int[]{11,20,60,100,140})h.runAfterDelay(tick,()->{
                var target=(BasinBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(output));
                int count=count(target);
                var nbt=source.saveWithFullMetadata();
                if(tick==11||count!=3)System.out.println("SPOUT_DIAG sample="+sample+" tick="+tick+" bottom="+bottom+" local="+local
                    +" pos="+h.absolutePos(p)+" targetPos="+h.absolutePos(output)+" count="+count+" overflow="+nbt.getList("Overflow",10)
                    +" lazy="+field(source,"lazyTickCounter",SmartBlockEntity.class)
                    +" backup="+field(source,"recipeBackupCheck",BasinBlockEntity.class)
                    +" sourceEntityTick="+h.getLevel().isPositionEntityTicking(h.absolutePos(p))
                    +" targetEntityTick="+h.getLevel().isPositionEntityTicking(h.absolutePos(output))
                    +" sourceBlockTick="+h.getLevel().shouldTickBlocksAt(new net.minecraft.world.level.ChunkPos(h.absolutePos(p)).toLong())
                    +" state="+source.getBlockState()+" sameSource="+(source==h.getLevel().getBlockEntity(h.absolutePos(p)))
                    +" removed="+source.isRemoved()+" targetCap="+target.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,side.getOpposite()).isPresent());
                if(tick==140)h.assertTrue(count==3,"Spout remained stalled sample="+sample+" "+bottom+"/"+local+" count="+count);
            });
        }
        h.runAfterDelay(150,h::succeed);
    }
    private static int count(BasinBlockEntity target) {
        int count=0;for(var inv:target.getInvs())for(int slot=0;slot<inv.getSlots();slot++)count+=inv.getStackInSlot(slot).getCount();
        return count;
    }
    // Causal control: only change fixture readiness, never inject machine ticks or modify production.
    private record Fixture(BlockPos source,BlockPos target,Direction local) {}
    private static void runWarm(GameTestHelper h,int sample) {
        var fixtures=new ArrayList<Fixture>();
        for(Direction bottom:Direction.values())for(Direction local:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}) {
            int i=local.ordinal()-2;
            var p=new BlockPos(5+bottom.ordinal()*11,3+(i/2)*5,3+(i%2)*7);
            var output=p.relative(bottom).relative(CatMachineOrientation.toWorld(bottom,local));
            var state=CatMachineBlocks.HAJI_BASIN.get().defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom);
            h.setBlock(p,state);h.setBlock(output,state);
            fixtures.add(new Fixture(h.absolutePos(p),h.absolutePos(output),local));
            for(var pos:List.of(p,output)) {
                var chunk=new net.minecraft.world.level.ChunkPos(h.absolutePos(pos));
                h.getLevel().setChunkForced(chunk.x,chunk.z,true);
            }
        }
        h.startSequence().thenWaitUntil(()->{
            for(var fixture:fixtures)h.assertTrue(h.getLevel().isPositionEntityTicking(fixture.source)&&h.getLevel().isPositionEntityTicking(fixture.target),
                "Fixture is not actually ticking yet");
        }).thenExecute(()->{
            for(var fixture:fixtures) {
                var source=(BasinBlockEntity)h.getLevel().getBlockEntity(fixture.source);
                if(source.getBlockState().getValue(BasinBlock.FACING)!=fixture.local)source.onWrenched(fixture.local);
                h.assertTrue(source.acceptOutputs(List.of(new ItemStack(Items.IRON_INGOT,3)),List.of(),false),"Warm output rejected");
            }
        }).thenExecuteAfter(60,()->{
            for(var fixture:fixtures) {
                var target=(BasinBlockEntity)h.getLevel().getBlockEntity(fixture.target);
                h.assertTrue(count(target)==3,"Warm fixture still stalled "+fixture+" count="+count(target));
                var source=(BasinBlockEntity)h.getLevel().getBlockEntity(fixture.source);
                h.assertTrue(source.saveWithFullMetadata().getList("Overflow",10).isEmpty(),"Warm fixture retained overflow");
            }
            System.out.println("SPOUT_WARM_CONTROL sample="+sample+" passed all 24 directions in 60 real world ticks");
        }).thenSucceed();
    }
    private static Object field(Object source,String name,Class<?> type) {
        try {var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(source);}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
}
