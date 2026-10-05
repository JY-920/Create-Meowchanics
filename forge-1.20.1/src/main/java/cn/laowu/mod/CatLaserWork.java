package cn.laowu.mod;

import cn.laowu.mod.create.*;
import cn.laowu.mod.entity.MechanicalLaserProjectile;
import cn.laowu.mod.accessory.CatAccessoryHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Independent seat work: never unsit, navigate, or mutate the normal combat target. */
public final class CatLaserWork {
    private static final Map<Cat,Work> WORK=new WeakHashMap<>();
    private static final class Work {
        final Map<CatAutoLaserBlockEntity,Lease> sources=new WeakHashMap<>();
        ResourceKey<Level> dimension;
        long nextShot;
    }
    private record Lease(BlockPos pos,ResourceKey<Level> dimension,UUID target,long until,long ready) {}
    public static void offer(Cat cat,CatAutoLaserBlockEntity machine,LivingEntity target) {
        if(!(cat.level() instanceof ServerLevel level)||!CatAutoLaserTargets.available(cat))return;
        Work work=WORK.computeIfAbsent(cat,c->new Work());
        long now=level.getGameTime();Lease old=work.sources.get(machine);
        work.sources.put(machine,new Lease(machine.getBlockPos().immutable(),level.dimension(),target.getUUID(),now+12,
            old!=null?old.ready:now+1));
    }
    public static void stop(Cat cat) {WORK.remove(cat);}
    public static void tick(Cat cat) {
        if(!(cat.level() instanceof ServerLevel level))return;
        Work work=WORK.get(cat);if(work==null)return;
        if(!CatAutoLaserTargets.available(cat)){stop(cat);return;}
        long now=level.getGameTime();
        if(work.dimension!=level.dimension()){work.dimension=level.dimension();work.nextShot=now;}
        work.sources.entrySet().removeIf(entry->{
            Lease lease=entry.getValue();var machine=entry.getKey();
            return lease.until<now || lease.dimension!=level.dimension() || machine.isRemoved()
                || !level.hasChunkAt(lease.pos) || level.getBlockEntity(lease.pos)!=machine || !machine.isWorking();
        });
        BlockPos seat=CatAutoLaserTargets.seat(cat);if(seat==null)return;
        CatAutoLaserBlockEntity selected=null;LivingEntity target=null;double best=Double.POSITIVE_INFINITY;
        for(var entry:work.sources.entrySet()) {
            Lease lease=entry.getValue();if(lease.ready>now)continue;
            if(!(level.getEntity(lease.target) instanceof LivingEntity enemy))continue;
            var machine=entry.getKey();if(!machine.validWork(cat,enemy))continue;
            double distance=Vec3.atCenterOf(seat).distanceToSqr(Vec3.atCenterOf(lease.pos));
            if(distance<best || (distance==best && (selected==null||lease.pos.compareTo(selected.getBlockPos())<0))) {
                best=distance;selected=machine;target=enemy;
            }
        }
        if(selected==null || now<work.nextShot)return;
        work.nextShot=now+Math.max(1,CareerCatBehavior.careerAttackIntervalTicks(cat));
        // Conditions checked again immediately before constructing the actual shot.
        if(!selected.validWork(cat,target))return;
        var projectile=new MechanicalLaserProjectile(level,cat,(float)cat.getAttributeValue(Attributes.ATTACK_DAMAGE));
        projectile.setPos(cat.getX(),cat.getEyeY()-.03,cat.getZ());
        Vec3 aim=target.getEyePosition().subtract(projectile.position());
        if(aim.lengthSqr()<1e-8)return;
        projectile.shoot(aim.x,aim.y,aim.z,2.4f,0);
        if(!CatAccessoryHooks.projectile(cat,target,projectile))return;
        double speed=projectile.getDeltaMovement().length();
        if(!Double.isFinite(speed)||speed<=1e-6)return;
        double distance=Math.max(32,aim.length()+2);
        projectile.setWorkFlightLimits(distance,(int)Math.min(Integer.MAX_VALUE-1,Math.ceil(distance/speed)+5));
        level.addFreshEntity(projectile);
        cat.swing(InteractionHand.MAIN_HAND);
        level.playSound(null,cat.blockPosition(),SoundEvents.BEACON_POWER_SELECT,SoundSource.NEUTRAL,.45f,1.6f);
    }
    private CatLaserWork() {}
}

