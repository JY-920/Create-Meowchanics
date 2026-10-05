package cn.laowu.mod.create;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Exact authored bone pivots, shared by aiming and client rendering. Angles are radians. */
public final class CatAutoLaserAim {
    private static final Vec3 TIP=new Vec3(0,8.5,7.9);
    private static final Vec3 PEN_PIVOT=new Vec3(.01363,8.48232,-.4);
    private static final Vec3 YAW_PIVOT=new Vec3(0,8.5,0);
    public record Aim(float yaw,float pitch,Vec3 muzzle,Vec3 direction) {}
    public static float curve(float t) {
        t=Math.max(0,Math.min(1,t));return .5f*t+1.5f*t*t-t*t*t;
    }
    public static float lift(float extension) {return curve((extension*1.25f-.25f)/.5f);}
    public static float lid(float extension) {return curve(extension*1.25f/.5f);}
    private static Vec3 rx(Vec3 v,double angle) {
        double c=Math.cos(angle),s=Math.sin(angle);return new Vec3(v.x,v.y*c-v.z*s,v.y*s+v.z*c);
    }
    private static Vec3 ry(Vec3 v,double angle) {
        double c=Math.cos(angle),s=Math.sin(angle);return new Vec3(v.x*c+v.z*s,v.y,-v.x*s+v.z*c);
    }
    public static Vec3 muzzle(BlockState state,BlockPos pos,float extension,float yaw,float pitch) {
        Vec3 p=rx(TIP.subtract(PEN_PIVOT),pitch).add(PEN_PIVOT);
        p=ry(p.subtract(YAW_PIVOT),yaw).add(YAW_PIVOT).add(0,15*lift(extension),0);
        return CatMachineOrientation.position(state,p.scale(1/16d).add(.5,0,.5)).add(Vec3.atLowerCornerOf(pos));
    }
    private static Vec3 localVector(BlockState state,Vec3 world) {
        var bottom=CatMachineOrientation.bottom(state);
        Vec3 x=CatMachineOrientation.toWorld(bottom,new Vec3(1,0,0));
        Vec3 y=CatMachineOrientation.toWorld(bottom,new Vec3(0,1,0));
        Vec3 z=CatMachineOrientation.toWorld(bottom,new Vec3(0,0,1));
        return new Vec3(world.dot(x),world.dot(y),world.dot(z));
    }
    public static Aim aim(BlockState state,BlockPos pos,float extension,Vec3 target) {
        Vec3 pivot=CatMachineOrientation.position(state,new Vec3(.5,(8.5+15*lift(extension))/16d,.5)).add(Vec3.atLowerCornerOf(pos));
        Vec3 d=localVector(state,target.subtract(pivot));
        float yaw=(float)Math.atan2(d.x,d.z);
        // In authored pixels the +Z barrel line has a 0.01768px perpendicular
        // offset from its pitch pivot. Solve A*cos(pitch)+B*sin(pitch)=offset
        // analytically; fixed-point iteration diverges beside the machine.
        double a=d.y*16+.01768,b=Math.hypot(d.x,d.z)*16+.4;
        double r=Math.hypot(a,b);
        float pitch=(float)(Math.asin(Math.max(-1,Math.min(1,.01768/Math.max(1e-8,r))))-Math.atan2(a,b));
        Vec3 direction=CatMachineOrientation.vector(state,ry(rx(new Vec3(0,0,1),pitch),yaw));
        return new Aim(yaw,pitch,muzzle(state,pos,extension,yaw,pitch),direction);
    }
    private CatAutoLaserAim() {}
}
