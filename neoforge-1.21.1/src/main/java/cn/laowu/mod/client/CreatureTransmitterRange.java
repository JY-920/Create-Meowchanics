package cn.laowu.mod.client;

import cn.laowu.mod.create.CreatureTransmitterBlockEntity;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Client-local sphere outlines. No world data or server entity references are retained. */
public final class CreatureTransmitterRange {
    private static final long DURATION=180_000L;
    private static ClientLevel world;
    private static final Map<BlockPos,Long> pinned=new HashMap<>();
    private static BlockPos preview;
    private static int previewRadius;
    private static final Set<LineKey> shown=new HashSet<>();
    private record LineKey(BlockPos pos,int segment) {}
    @net.neoforged.bus.api.SubscribeEvent
    public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        
        update(Util.getMillis());
    }
    private static void checkWorld(){
        var current=Minecraft.getInstance().level;
        if(world==current)return;
        for(var key:shown)Outliner.getInstance().remove(key);
        shown.clear();pinned.clear();preview=null;world=current;
    }
    public static void toggle(CreatureTransmitterBlockEntity be){
        checkWorld();
        if(world==null)return;
        var pos=be.getBlockPos().immutable();
        if(pinned.containsKey(pos))pinned.remove(pos);
        else pinned.put(pos,Util.getMillis()+DURATION);
        update(Util.getMillis());
    }
    public static boolean isPinned(BlockPos pos){return pinned.containsKey(pos);}
    public static void preview(CreatureTransmitterBlockEntity be,int radius){
        checkWorld();preview=be.getBlockPos().immutable();previewRadius=radius;update(Util.getMillis());
    }
    public static void clearPreview(){preview=null;update(Util.getMillis());}
    public static void update(long now){
        checkWorld();
        var next=new HashSet<LineKey>();
        if(world!=null){
            pinned.entrySet().removeIf(entry->now>=entry.getValue()||!(world.getBlockEntity(entry.getKey()) instanceof CreatureTransmitterBlockEntity));
            if(preview!=null&&!(world.getBlockEntity(preview) instanceof CreatureTransmitterBlockEntity))preview=null;
            for(var pos:pinned.keySet()){
                var be=(CreatureTransmitterBlockEntity)world.getBlockEntity(pos);
                draw(pos,pos.equals(preview)?previewRadius:be.getRadius(),next);
            }
            if(preview!=null&&!pinned.containsKey(preview))draw(preview,previewRadius,next);
        }
        for(var key:shown)if(!next.contains(key))Outliner.getInstance().remove(key);
        shown.clear();shown.addAll(next);
    }
    private static void draw(BlockPos pos,int radius,Set<LineKey> keys){
        Vec3 center=Vec3.atCenterOf(pos);
        for(int plane=0;plane<3;plane++)for(int segment=0;segment<64;segment++){
            var key=new LineKey(pos,plane*64+segment);keys.add(key);
            double a=segment*Math.PI/32,b=(segment+1)*Math.PI/32;
            Outliner.getInstance().showLine(key,point(center,radius,plane,a),point(center,radius,plane,b))
                .colored(0xEACB78).lineWidth(1f/32);
        }
    }
    private static Vec3 point(Vec3 c,int r,int plane,double a){
        double u=Math.cos(a)*r,v=Math.sin(a)*r;
        return c.add(plane==2?0:u,plane==0?0:plane==1?v:u,plane==0?v:plane==2?v:0);
    }
    private CreatureTransmitterRange(){}
}
