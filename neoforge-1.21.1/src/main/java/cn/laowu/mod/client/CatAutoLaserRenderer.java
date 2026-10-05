package cn.laowu.mod.client;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.create.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;

public final class CatAutoLaserRenderer extends KineticBlockEntityRenderer<CatAutoLaserBlockEntity> {
    public CatAutoLaserRenderer(BlockEntityRendererProvider.Context context){super(context);}
    public static void renderModel(BlockState state,float extension,float yaw,float pitch,boolean lit,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose();
        pose.translate(.5,.5,.5);CatMachineOrientation.rotatePose(pose,CatMachineOrientation.bottom(state));pose.translate(-.5,-.5,-.5);
        pose.translate(.5,1.5,.5);pose.scale(1,-1,1);
        RuntimeBlockbenchModel.get(LaoWuMod.id("models/entity/cat_auto_laser.geo.json")).render(pose,
            buffers.getBuffer(RenderType.entityCutout(LaoWuMod.id("textures/block/cat_auto_laser"+(lit?"":"_off")+".png"))),
            light,overlay,RuntimeBlockbenchModel.GroupSelection.ALL,RuntimeBlockbenchModel.HeadMotion.NONE,CatAutoLaserPose.sample(extension,yaw,pitch));
        pose.popPose();
    }
    public static void renderBeam(Vec3 muzzle,Vec3 target,PoseStack pose,MultiBufferSource buffers) {
        Vec3 delta=target.subtract(muzzle);
        if(delta.lengthSqr()<1e-8)return;
        pose.pushPose();pose.translate(muzzle.x,muzzle.y,muzzle.z);
        pose.mulPose(new org.joml.Quaternionf().rotationTo(new org.joml.Vector3f(0,0,1),new org.joml.Vector3f((float)delta.x,(float)delta.y,(float)delta.z).normalize()));
        CatLaserEffects.solidBeam(pose,buffers.getBuffer(RenderType.debugQuads()),(float)delta.length(),0xff2020,89);
        pose.popPose();
    }
    @Override protected void renderSafe(CatAutoLaserBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(be.isRemoved()||be.getLevel()==null)return;
        float extension=be.getExtension(partial),yaw=be.getAimYaw(partial),pitch=be.getAimPitch(partial);
        Vec3 target=null,muzzle=null;
        var level=be.getLevel();
        var entity=level==null?null:level.getEntity(be.getTargetEntityId());
        // Entity IDs are reused after unload: UUID must match before drawing anything.
        if(be.isAimLocked()&&entity!=null&&entity.isAlive()&&entity.getUUID().equals(be.getTargetUuid())) {
            target=entity.getPosition(partial).add(0,entity.getEyeHeight(),0);
            muzzle=CatAutoLaserPose.muzzle(be.getBlockState(),be.getBlockPos(),extension,yaw,pitch);
            Vec3 barrel=CatMachineOrientation.vector(be.getBlockState(),new Vec3(Math.sin(yaw)*Math.cos(pitch),-Math.sin(pitch),Math.cos(yaw)*Math.cos(pitch)));
            // Server acquisition is authoritative. Smoothed client angles trail moving
            // targets; a second angular lock here makes an acquired beam disappear.
            // A nearby target may physically be inside the long authored pen.
            // It remains marked/attackable; never draw a backwards beam through the pen.
            if(target!=null&&barrel.dot(target.subtract(muzzle))<=.001)target=null;
        }
        renderModel(be.getBlockState(),extension,yaw,pitch,be.isWorking(),pose,buffers,light,overlay);
        if(be.hasFilter()){
            pose.pushPose();
            CatAutoLaserFilterSlot.INSTANCE.transform(level,be.getBlockPos(),be.getBlockState(),pose);
            com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer.renderItemIntoValueBox(
                be.getFilter(),pose,buffers,light,overlay);
            pose.popPose();
        }
        if(target!=null&&muzzle!=null) {
            var hit=level.clip(new ClipContext(muzzle,target,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,entity));
            if(hit.getType()!=HitResult.Type.MISS)target=hit.getLocation();
            renderBeam(muzzle.subtract(Vec3.atLowerCornerOf(be.getBlockPos())),target.subtract(Vec3.atLowerCornerOf(be.getBlockPos())),pose,buffers);
        }
        // Native Create half-shaft: full-size UVs, flush outer end, hidden end
        // at the center. Never shrink the entire shaft to simulate an inset.
        pose.pushPose();pose.translate(.5,.5,.5);CatMachineOrientation.rotatePose(pose,CatMachineOrientation.bottom(be.getBlockState()));
        pose.translate(-.5,-.5,-.5);
        var axis=CatMachineOrientation.bottom(be.getBlockState()).getAxis();
        float angle=getAngleForBe(be,be.getBlockPos(),axis);
        if(CatMachineOrientation.toWorld(CatMachineOrientation.bottom(be.getBlockState()),net.minecraft.core.Direction.UP).getAxisDirection()==net.minecraft.core.Direction.AxisDirection.NEGATIVE)angle=-angle;
        var shaftBuffer=net.createmod.catnip.render.CachedBuffers.partialFacing(
            com.simibubi.create.AllPartialModels.SHAFT_HALF,be.getBlockState(),net.minecraft.core.Direction.DOWN);
        kineticRotationTransform(shaftBuffer,be,net.minecraft.core.Direction.Axis.Y,angle,light).renderInto(pose,buffers.getBuffer(RenderType.solid()));
        pose.popPose();
    }
}
