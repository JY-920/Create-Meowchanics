package cn.laowu.mod.create;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Server-authoritative acquisition, with interpolated angles retained when a target is lost. */
public final class CatAutoLaserBlockEntity extends KineticBlockEntity {
    private static final float TURN_STEP=(float)Math.toRadians(8);
    private static final float AIM_TOLERANCE=(float)Math.toRadians(.5);
    private static final int ACQUIRE_TICKS=6;
    private float extension,previousExtension;
    private float yaw,pitch,previousYaw,previousPitch,receivedYaw,receivedPitch;
    private boolean anglesInitialized,aimLocked;
    private int alignedTicks;
    private java.util.UUID targetUuid;
    private int targetEntityId=-1;
    private long nextScan,nextOffer;
    private net.minecraft.world.item.ItemStack filter=net.minecraft.world.item.ItemStack.EMPTY;
    public net.minecraft.world.item.ItemStack getFilter(){return filter.copy();}
    public boolean hasFilter(){return !filter.isEmpty();}
    public net.minecraft.world.item.ItemStack removeFilterOnBreak(){
        var result=filter;filter=net.minecraft.world.item.ItemStack.EMPTY;return result;
    }
    public void setFilter(net.minecraft.world.item.ItemStack stack){
        filter=stack.is(cn.laowu.mod.LaoWuMod.CREATURE_FILTER.get())?stack.copyWithCount(1):net.minecraft.world.item.ItemStack.EMPTY;
        target(null);nextScan=0;setChanged();sendData();
    }
    public boolean matchesFilter(net.minecraft.world.entity.Mob mob){
        return hasFilter()&&cn.laowu.mod.item.CreatureFilterRules.read(filter).matches(mob);
    }
    public java.util.UUID getTargetUuid(){return targetUuid;}
    public int getTargetEntityId(){return targetEntityId;}
    public boolean isAimLocked(){return isWorking()&&aimLocked&&targetUuid!=null;}
    private static float delta(float target,float current){
        return (float)Math.toRadians(Mth.wrapDegrees((float)Math.toDegrees(target-current)));
    }
    private static float turn(float current,float target){
        return current+Mth.clamp(delta(target,current),-TURN_STEP,TURN_STEP);
    }
    public float getAimYaw(float partial){return previousYaw+delta(yaw,previousYaw)*Mth.clamp(partial,0,1);}
    public float getAimPitch(float partial){return Mth.lerp(Mth.clamp(partial,0,1),previousPitch,pitch);}
    public boolean validWork(net.minecraft.world.entity.animal.Cat cat,LivingEntity target){
        if(!(level instanceof ServerLevel server)||!isAimLocked()||target==null
            ||!targetUuid.equals(target.getUUID())||!CatAutoLaserTargets.available(cat))return false;
        var seat=CatAutoLaserTargets.seat(cat);
        return seat!=null&&net.minecraft.world.phys.Vec3.atCenterOf(seat).distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))<=25
            &&CatAutoLaserTargets.validTarget(server,worldPosition,cat,target)
            &&CatAutoLaserTargets.visible(server,new net.minecraft.world.phys.Vec3(cat.getX(),cat.getEyeY()-.03,cat.getZ()),target.getEyePosition(),null);
    }
    private void target(LivingEntity entity){
        java.util.UUID uuid=entity==null?null:entity.getUUID();int id=entity==null?-1:entity.getId();
        if(!java.util.Objects.equals(uuid,targetUuid)||id!=targetEntityId){
            targetUuid=uuid;targetEntityId=id;alignedTicks=0;aimLocked=false;nextOffer=0;sendData();
        }
    }
    private void serverWork(ServerLevel server){
        if(!isWorking()){target(null);alignedTicks=0;aimLocked=false;return;}
        var entity=targetUuid==null?null:server.getEntity(targetUuid);
        if(!(entity instanceof LivingEntity living)||!CatAutoLaserTargets.validMark(server,worldPosition,living)
            ||!CatAutoLaserTargets.eligible(server,worldPosition,living,CatAutoLaserTargets.participants(server,worldPosition))){
            if(targetUuid!=null){target(null);nextScan=0;}
        }
        long now=server.getGameTime();
        if(now>=nextScan){
            nextScan=now+10;
            target(CatAutoLaserTargets.nearest(server,worldPosition,CatAutoLaserTargets.participants(server,worldPosition)));
        }
        entity=targetUuid==null?null:server.getEntity(targetUuid);
        if(!(entity instanceof LivingEntity living))return;
        var aim=CatAutoLaserAim.aim(getBlockState(),worldPosition,extension,living.getEyePosition());
        yaw=turn(yaw,aim.yaw());pitch=turn(pitch,aim.pitch());
        boolean aligned=Math.abs(delta(aim.yaw(),yaw))<=AIM_TOLERANCE&&Math.abs(delta(aim.pitch(),pitch))<=AIM_TOLERANCE;
        alignedTicks=aligned?Math.min(ACQUIRE_TICKS,alignedTicks+1):0;
        aimLocked=alignedTicks>=ACQUIRE_TICKS;
        // Advertise candidates before acquisition finishes so all stations enter
        // the same arbitration window. validWork still forbids unaimed shots.
        if(now>=nextOffer){
            nextOffer=now+5;
            for(var cat:CatAutoLaserTargets.participants(server,worldPosition))cn.laowu.mod.CatLaserWork.offer(cat,this,living);
        }
    }
    @Override protected net.minecraft.world.phys.AABB createRenderBoundingBox(){return new net.minecraft.world.phys.AABB(worldPosition).inflate(26);}
    public CatAutoLaserBlockEntity(BlockPos pos,BlockState state){super(CatMachineBlocks.AUTO_LASER_BE.get(),pos,state);}
    private boolean powered(){return getSpeed()!=0&&!isOverStressed();}
    public boolean isWorking(){return powered()&&extension>=1;}
    public float getExtension(float partial){return Mth.lerp(Mth.clamp(partial,0,1),previousExtension,extension);}
    @Override public void tick(){
        super.tick();previousExtension=extension;previousYaw=yaw;previousPitch=pitch;
        boolean wasLocked=aimLocked;
        extension=Mth.clamp(extension+(powered()?1f:-1f)/25f,0,1);
        if(extension>.9999f)extension=1;
        if(level instanceof ServerLevel server){
            serverWork(server);
            if(previousExtension!=extension||previousYaw!=yaw||previousPitch!=pitch||wasLocked!=aimLocked){setChanged();sendData();}
        }else if(level!=null){
            yaw=turn(yaw,receivedYaw);pitch=turn(pitch,receivedPitch);
        }
    }
    @Override protected void write(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket){
        super.write(tag,registries,clientPacket);
        tag.putFloat("Extension",extension);tag.putFloat("AimYaw",yaw);tag.putFloat("AimPitch",pitch);
        if(!filter.isEmpty())tag.put("CreatureFilter",filter.save(registries));
        if(clientPacket){
            tag.putBoolean("AimLocked",aimLocked);
            if(targetUuid!=null){tag.putUUID("TargetUuid",targetUuid);tag.putInt("TargetId",targetEntityId);}
        }
    }
    @Override protected void read(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket){
        super.read(tag,registries,clientPacket);
        var savedFilter=net.minecraft.world.item.ItemStack.parseOptional(registries,tag.getCompound("CreatureFilter"));
        filter=savedFilter.is(cn.laowu.mod.LaoWuMod.CREATURE_FILTER.get())?savedFilter.copyWithCount(1):net.minecraft.world.item.ItemStack.EMPTY;
        float value=tag.getFloat("Extension");extension=Float.isFinite(value)?Mth.clamp(value,0,1):0;
        float savedYaw=tag.getFloat("AimYaw"),savedPitch=tag.getFloat("AimPitch");
        receivedYaw=Float.isFinite(savedYaw)?delta(savedYaw,0):0;
        receivedPitch=Float.isFinite(savedPitch)?Mth.clamp(savedPitch,-Mth.PI,Mth.PI):0;
        if(!clientPacket||!anglesInitialized){previousYaw=yaw=receivedYaw;previousPitch=pitch=receivedPitch;previousExtension=extension;anglesInitialized=true;}
        targetUuid=clientPacket&&tag.hasUUID("TargetUuid")?tag.getUUID("TargetUuid"):null;
        targetEntityId=targetUuid==null?-1:tag.getInt("TargetId");
        aimLocked=clientPacket&&targetUuid!=null&&tag.getBoolean("AimLocked");
        if(!clientPacket){alignedTicks=0;nextScan=nextOffer=0;}
    }
}
