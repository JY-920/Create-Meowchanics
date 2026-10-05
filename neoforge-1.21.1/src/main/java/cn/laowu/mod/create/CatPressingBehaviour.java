package cn.laowu.mod.create;
import com.simibubi.create.content.kinetics.press.PressingBehaviour;
import net.minecraft.core.particles.*;
import net.minecraft.world.phys.Vec3;
public final class CatPressingBehaviour extends PressingBehaviour {
    public CatPressingBehaviour(CatPressBlockEntity be){super(be);}

    private boolean isTarget(com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour inventory){
        var state=blockEntity.getBlockState();
        return inventory!=null&&inventory.getPos().equals(getPos().relative(CatMachineOrientation.bottom(state),2))
            &&inventory.blockEntity.getBlockState().getBlock() instanceof com.simibubi.create.content.logistics.depot.DepotBlock
            &&CatProcessorPlacement.validTarget(getWorld(),getPos(),state,false);
    }
    @Override public ProcessingResult handleReceivedItem(com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack stack,
            com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour inventory){
        return isTarget(inventory)?super.handleReceivedItem(stack,inventory):ProcessingResult.PASS;
    }
    @Override public ProcessingResult handleHeldItem(com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack stack,
            com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour inventory){
        return isTarget(inventory)?super.handleHeldItem(stack,inventory):ProcessingResult.PASS;
    }

    @Override public void tick(){if(running)super.tick();}
    @Override protected void spawnParticles(){
        var level=getWorld();
        if(level==null||!level.isClientSide){particleItems.clear();return;}
        var state=blockEntity.getBlockState();
        Vec3 pos=Vec3.atCenterOf(getPos()).add(CatMachineOrientation.vector(state,new Vec3(0,mode==Mode.BASIN?-2:-1.3125,0)));
        for(var stack:particleItems)for(int i=0;i<(mode==Mode.BASIN?20:specifics.getParticleAmount());i++){
            Vec3 v=CatMachineOrientation.vector(state,new Vec3((level.random.nextFloat()-.5)*.25,.125,(level.random.nextFloat()-.5)*.25));
            level.addParticle(new ItemParticleOption(ParticleTypes.ITEM,stack),pos.x,pos.y,pos.z,v.x,v.y,v.z);
        }
        particleItems.clear();
    }
}
