package cn.laowu.mod.client;

/** Frame-rate independent angular spring without overshoot or wrap-around jumps. */
public final class GiantCatFacing {
    /** Lock unmounted rest/get-up poses; a rider takes heading control immediately. */
    public static float bodyYaw(float current,float target,float ticks,boolean riding,
                                boolean resting,float restWeight) {
        if (!riding && (resting || restWeight > .002F)) return current;
        return approach(current,target,ticks,riding?18:10);
    }
    public static float approach(float current,float target,float ticks,float maxDegreesPerTick) {
        float delta=(target-current)%360;
        if(delta>=180)delta-=360;
        if(delta< -180)delta+=360;
        float time=Math.max(0,Math.min(2,ticks));
        float step=delta*(1-(float)Math.exp(-.32*time));
        float bound=maxDegreesPerTick*time;
        return current+Math.max(-bound,Math.min(bound,step));
    }
    private GiantCatFacing() {}
}
