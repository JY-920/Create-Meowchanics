package cn.laowu.mod.client;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.create.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import java.util.Map;

/** Retraction follows Create cycle progress; mixer blades also rotate at native idle speed. */
public final class CatProcessorRenderer<T extends KineticBlockEntity> extends KineticBlockEntityRenderer<T> {
    public CatProcessorRenderer(BlockEntityRendererProvider.Context context){super(context);}
    @Override protected void renderSafe(T be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        boolean mixer=be instanceof CatMixerBlockEntity;
        String id=mixer?"cat_mixer":"cat_press";
        float progress=mixer?((CatMixerBlockEntity)be).getRenderedHeadOffset(partial)-7/16f:((CatPressBlockEntity)be).pressingBehaviour.getRenderedHeadOffset(partial);
        float angle=mixer?(float)((((be.getLevel().getGameTime()+(double)partial)*((CatMixerBlockEntity)be).getRenderedHeadRotationSpeed(partial)*.6)%360)*Math.PI/180):0;
        Map<String,RuntimeBlockbenchModel.GroupTransform> motion=Map.of(
            "bone",RuntimeBlockbenchModel.GroupTransform.position(0,-(mixer?16:18)*progress,0),
            "bone2",RuntimeBlockbenchModel.GroupTransform.position(0,5*progress,0),
            "bone5",RuntimeBlockbenchModel.GroupTransform.rotation(0,angle,0));
        pose.pushPose();
        pose.translate(.5,.5,.5);CatMachineOrientation.rotatePose(pose,CatMachineOrientation.bottom(be.getBlockState()));pose.translate(-.5,-.5,-.5);
        pose.translate(.5,1.5,.5);pose.scale(1,-1,1);
        RuntimeBlockbenchModel.get(LaoWuMod.id("models/blockbench/"+id+"_moving.bbmodel")).render(pose,
            buffers.getBuffer(RenderType.entityCutoutNoCull(LaoWuMod.id("textures/block/"+id+".png"))),light,overlay,
            RuntimeBlockbenchModel.GroupSelection.ALL,RuntimeBlockbenchModel.HeadMotion.NONE,motion);
        // Recessed shaft mouths and their inner walls belong to the baked shell.
        // The old outline cuboids had a second outer cap which covered the shaft.
        pose.popPose();
        // These BEs deliberately have no Flywheel visual. Always render their shaft,
        // including when Flywheel is available globally for other Create machines.
        if(be.getBlockState().getValue(CatProcessorBlock.SHAFT_AXIS)!=CatMachineOrientation.bottom(be.getBlockState()).getAxis())
            renderRotatingKineticBlock(be,shaft(be.getBlockState().getValue(CatProcessorBlock.SHAFT_AXIS)),pose,buffers.getBuffer(RenderType.solid()),light);
    }
}
