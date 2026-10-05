package cn.laowu.mod.create;
import net.minecraft.world.entity.animal.Cat;
/** Short-lived launch priority: vanilla physics remains live; autonomous jobs resume on landing. */
public final class CatDeploymentFlight {
    private static final String KEY="laowu:deployment_flight";
    public static void begin(Cat cat){
        var state=new net.minecraft.nbt.CompoundTag();
        state.putLong("Until",cat.level().getGameTime()+200);
        state.putBoolean("Gravity",cat.isNoGravity());
        cat.getPersistentData().put(KEY,state);
        cat.setNoGravity(false);
    }
    public static boolean tick(Cat cat){
        var data=cat.getPersistentData();
        if(!data.contains(KEY))return false;
        var state=data.getCompound(KEY);
        long remaining=state.getLong("Until")-cat.level().getGameTime();
        if(cat.onGround()||cat.horizontalCollision||cat.isInWaterOrBubble()||cat.isPassenger()||remaining<=0||remaining>200){
            cat.setNoGravity(state.getBoolean("Gravity"));data.remove(KEY);return false;
        }
        cat.getNavigation().stop();cat.setNoGravity(false);
        return true;
    }
    public static void sanitizeSnapshot(net.minecraft.nbt.CompoundTag saved){
        var data=saved.getCompound("ForgeData");
        if(data.contains(KEY)){
            saved.putBoolean("NoGravity",data.getCompound(KEY).getBoolean("Gravity"));
            data.remove(KEY);
        }
    }
    private CatDeploymentFlight(){}
}
