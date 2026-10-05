package cn.laowu.mod.client;
import cn.laowu.mod.create.CatAutoLaserAim;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
public final class CatAutoLaserPose {
    public static CatAutoLaserAim.Aim aim(BlockState state,BlockPos pos,float extension,Vec3 target) {return CatAutoLaserAim.aim(state,pos,extension,target);}
    public static Vec3 muzzle(BlockState state,BlockPos pos,float extension,float yaw,float pitch) {return CatAutoLaserAim.muzzle(state,pos,extension,yaw,pitch);}
    public static Map<String,RuntimeBlockbenchModel.GroupTransform> sample(float extension,float yaw,float pitch) {
        float lift=CatAutoLaserAim.lift(extension),lid=CatAutoLaserAim.lid(extension);
        return Map.of(
            "bone",RuntimeBlockbenchModel.GroupTransform.position(0,8*lift,0),
            "bone3",new RuntimeBlockbenchModel.GroupTransform(0,7*lift,0,0,yaw,0),
            "bone7",RuntimeBlockbenchModel.GroupTransform.position(0,3*lift,0),
            "bone4",RuntimeBlockbenchModel.GroupTransform.scaled(2*lid,0,0,0,0,0,1-.7f*lid,1,1),
            "bone5",RuntimeBlockbenchModel.GroupTransform.scaled(-2*lid,0,0,0,0,0,1-.7f*lid,1,1),
            "bb_main",RuntimeBlockbenchModel.GroupTransform.rotation(-pitch,0,0));
    }
    private CatAutoLaserPose() {}
}
