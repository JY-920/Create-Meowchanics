package cn.laowu.mod.mixin;
import cn.laowu.mod.create.*;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=BasinBlockEntity.class,remap=false)
public abstract class CatBasinParticlesMixin {
    @Redirect(method={"createFluidParticles","createMovingFluidParticles","lambda$createOutputFluidParticles$*"},
        at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;addAlwaysVisibleParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",remap=true))
    private void laowu$rotateParticle(Level level,ParticleOptions options,double x,double y,double z,double vx,double vy,double vz){
        if((Object)this instanceof HajiBasinBlockEntity basin){
            Vec3 center=Vec3.atCenterOf(basin.getBlockPos());
            Vec3 p=center.add(CatMachineOrientation.vector(basin.getBlockState(),new Vec3(x,y,z).subtract(center)));
            Vec3 v=CatMachineOrientation.vector(basin.getBlockState(),new Vec3(vx,vy,vz));
            level.addAlwaysVisibleParticle(options,p.x,p.y,p.z,v.x,v.y,v.z);
        }else level.addAlwaysVisibleParticle(options,x,y,z,vx,vy,vz);
    }
}

