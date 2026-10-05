package cn.laowu.mod.create;
import java.util.Optional;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
public final class CatMixerBlockEntity extends MechanicalMixerBlockEntity {
    public CatMixerBlockEntity(BlockPos pos,BlockState state){super(CatMachineBlocks.MIXER_BE.get(),pos,state);}
    @Override protected Block getStressConfigKey(){return AllBlocks.MECHANICAL_MIXER.get();}
    @Override protected Optional<BasinBlockEntity> getBasin() {
        if(level==null||!CatProcessorPlacement.validTarget(level,worldPosition,getBlockState(),true))return Optional.empty();
        var be=level.getBlockEntity(worldPosition.relative(CatMachineOrientation.bottom(getBlockState()),2));
        return be instanceof BasinBlockEntity basin?Optional.of(basin):Optional.empty();
    }
    @Override public void tick() {
        // The superclass removal branch also sends the authoritative stop packet.
        // Calling onBasinRemoved directly leaves clients stuck at the mixing pose.
        if(level!=null&&!level.isClientSide&&running&&(!CatProcessorPlacement.validTarget(level,worldPosition,getBlockState(),true)||getSpeed()==0))basinRemoved=true;
        super.tick();
    }
    @Override protected void spillParticle(ParticleOptions data) {
        if(level==null)return;
        double angle=level.random.nextDouble()*Math.PI*2;
        Vec3 offset=new Vec3(Math.cos(angle)*.25,-1.75,Math.sin(angle)*.25);
        Vec3 pos=Vec3.atCenterOf(worldPosition).add(CatMachineOrientation.vector(getBlockState(),offset));
        Vec3 motion=CatMachineOrientation.vector(getBlockState(),new Vec3(-Math.sin(angle)*.1,.1,Math.cos(angle)*.1));
        level.addParticle(data,pos.x,pos.y,pos.z,motion.x,motion.y,motion.z);
    }
    @Override protected AABB createRenderBoundingBox(){return new AABB(worldPosition).inflate(2);}
}
