package cn.laowu.mod.entity;
import cn.laowu.mod.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import java.util.UUID;

/** Temporary honey decal. Never replaces terrain or drops blocks/items. */
public final class CatHoneyPatch extends Entity {
    public static final float RADIUS=1.25F;
    private int remaining=100;
    private UUID owner;
    public CatHoneyPatch(EntityType<? extends CatHoneyPatch> type,Level level){super(type,level);noPhysics=true;setNoGravity(true);}
    @Override protected void defineSynchedData(){}
    public static CatHoneyPatch create(Cat cat,LivingEntity victim){
        if(!(cat.level() instanceof ServerLevel level))return null;
        Vec3 top=victim.position().add(0,.3,0);
        var hit=level.clip(new ClipContext(top,top.add(0,-4,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,cat));
        if(hit.getType()!=HitResult.Type.BLOCK||hit.getDirection()!=net.minecraft.core.Direction.UP)return null;
        Vec3 point=hit.getLocation().add(0,.012,0);
        for(var patch:level.getEntitiesOfClass(CatHoneyPatch.class,new AABB(point,point).inflate(.6))){
            if(cat.getUUID().equals(patch.owner)&&patch.distanceToSqr(point)<.36){patch.remaining=100;return patch;}
        }
        var patch=new CatHoneyPatch(LaoWuMod.CAT_HONEY_PATCH.get(),level);
        patch.owner=cat.getUUID();patch.setPos(point);
        return level.addFreshEntity(patch)?patch:null;
    }
    @Override public void tick(){
        super.tick();
        if(!(level() instanceof ServerLevel level))return;
        if(--remaining<=0||level.getBlockState(BlockPos.containing(getX(),getY()-.05,getZ())).isAir()){discard();return;}
        if(tickCount%5!=0)return;
        for(LivingEntity other:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(0,.18,0),
                e->e.isAlive()&&!e.isSpectator()&&!(e instanceof net.minecraft.world.entity.decoration.ArmorStand))){
            if(other.position().subtract(position()).multiply(1,0,1).lengthSqr()>RADIUS*RADIUS
                    ||level.clip(new ClipContext(position().add(0,.12,0),other.position().add(0,.15,0),
                    ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()!=HitResult.Type.MISS)continue;
            other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,30,1,false,true,true));
        }
        if(tickCount%10==0)level.sendParticles(ParticleTypes.LANDING_HONEY,getX(),getY()+.03,getZ(),3,.65,0,.65,0);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){tag.putInt("Remaining",remaining);if(owner!=null)tag.putUUID("Owner",owner);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){remaining=Math.max(1,Math.min(100,tag.getInt("Remaining")));owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }
}
