package cn.laowu.mod.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Uses the actual renderer entry transform, including shader/first-person render passes. */
public final class LaserPointerAim {
    public static LivingEntity renderingOwner;
    private static final Matrix4f handBase = new Matrix4f(), entityBase = new Matrix4f();
    private static LivingEntity entityOwner;
    private static float handPartial, entityPartial;
    public static void beginHand(PoseStack pose, float delta) {
        handBase.set(pose.last().pose()); handPartial=delta;
    }
    public static void beginEntity(LivingEntity owner, PoseStack pose, float delta) {
        entityOwner=owner; entityBase.set(pose.last().pose()); entityPartial=delta;
    }
    public static void endEntity() { entityOwner=null; }
    public static boolean firstPerson(ItemDisplayContext context) {
        return context==ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context==ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }
    public static boolean local(ItemDisplayContext context) {
        return firstPerson(context) || renderingOwner==Minecraft.getInstance().player;
    }
    public static Vector3f target(ItemDisplayContext context, PoseStack pose) { return target(context,pose,false); }
    public static Vector3f orientationTarget(ItemDisplayContext context, PoseStack pose) { return target(context,pose,true); }
    public static boolean orient(ItemDisplayContext context, PoseStack pose) {
        Vector3f local=orientationTarget(context,pose);
        if(local==null) return false;
        Matrix4f base=firstPerson(context)?handBase:entityBase;
        Vector3f up=base.transformDirection(new Vector3f(0,1,0)).normalize();
        Vector3f fallback=base.transformDirection(new Vector3f(0,0,1)).normalize();
        if(!LaserPointerFrame.orient(pose.last().pose(),local,up,fallback)) return false;
        pose.last().normal().set(pose.last().pose()).invert().transpose();
        return true;
    }
    private static Vector3f target(ItemDisplayContext context, PoseStack pose, boolean orientation) {
        boolean hand=firstPerson(context);
        Player player=hand?Minecraft.getInstance().player:renderingOwner instanceof Player p?p:null;
        if(player==null || !hand && (player!=entityOwner ||
                context!=ItemDisplayContext.THIRD_PERSON_LEFT_HAND && context!=ItemDisplayContext.THIRD_PERSON_RIGHT_HAND))
            return null;
        float partial=hand?handPartial:entityPartial;
        var eye=player.getEyePosition(partial);
        var look=player.getViewVector(partial);
        var hit=orientation?eye.add(look.scale(32)):cn.laowu.mod.CatLaserTargeting.aim(player,eye,look).point();
        Vector3f point;
        if(hand) {
            point=handBase.transformPosition(new Vector3f(0,0,-(float)Math.max(.6,hit.distanceTo(eye))));
        } else {
            // EntityRenderDispatcher has already translated this base to the interpolated entity.
            point=entityBase.transformPosition(hit.subtract(player.getPosition(partial)).toVector3f());
        }
        return new Matrix4f(pose.last().pose()).invert().transformPosition(point);
    }
    private LaserPointerAim() {}
}
