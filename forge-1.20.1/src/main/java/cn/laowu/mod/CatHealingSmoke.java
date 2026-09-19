package cn.laowu.mod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.phys.*;
import java.lang.ref.WeakReference;
import java.util.*;

/** A stationary five-second medicine cloud, sharing the medic's non-stacking healing pulse. */
public final class CatHealingSmoke {
    public static final double RADIUS=3;
    public static final int DURATION=100;
    public static final float HEAL_PER_SECOND=(float)CatSupportRules.healingPerSecond(50);
    private record Cloud(WeakReference<Cat> owner,Vec3 center,long until) {}
    private static final Map<ServerLevel,List<Cloud>> CLOUDS=new WeakHashMap<>();
    public static void create(Cat cat,Vec3 center){
        if(!(cat.level() instanceof ServerLevel level))return;
        var clouds=CLOUDS.computeIfAbsent(level,ignored->new ArrayList<>());
        clouds.removeIf(c->c.until<=level.getGameTime()||c.owner.get()==null);
        if(clouds.size()>=128)clouds.remove(0);
        clouds.add(new Cloud(new WeakReference<>(cat),center,level.getGameTime()+DURATION));
        level.sendParticles(LaoWuMod.CAT_HEALING_SMOKE.get(),center.x,center.y+.5,center.z,96,1.4,.5,1.4,.025);
    }
    public static void flush(ServerLevel level){
        var clouds=CLOUDS.get(level);if(clouds==null)return;
        long now=level.getGameTime();
        for(var it=clouds.iterator();it.hasNext();){
            var cloud=it.next();var owner=cloud.owner.get();
            if(now>=cloud.until||owner==null||owner.isRemoved()||owner.level()!=level){it.remove();continue;}
            Vec3 p=cloud.center;
            if(now%5==0)level.sendParticles(LaoWuMod.CAT_HEALING_SMOKE.get(),p.x,p.y+.35,p.z,12,1.3,.35,1.3,.018);
            for(Cat patient:level.getEntitiesOfClass(Cat.class,new AABB(p,p).inflate(RADIUS),
                    cat->CatMedicalHealing.injured(cat)&&!CatPoseData.isPancake(cat)
                            &&(cat==owner||CatTeamRules.friendly(owner,cat))&&cat.distanceToSqr(p)<=RADIUS*RADIUS)){
                if(level.clip(new net.minecraft.world.level.ClipContext(p.add(0,.2,0),patient.getBoundingBox().getCenter(),
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,owner)).getType()!=HitResult.Type.MISS)continue;
                CatMedicalHealing.illuminate(patient);
                CatMedicalHealing.offerHealing(patient,HEAL_PER_SECOND);
            }
        }
        if(clouds.isEmpty())CLOUDS.remove(level);
    }
    private CatHealingSmoke(){}
}
