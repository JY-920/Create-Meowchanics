package cn.laowu.mod.client;
import cn.laowu.mod.create.CatMachineOrientation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.processing.basin.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
public final class CatBasinRenderer extends BasinRenderer {
    public CatBasinRenderer(BlockEntityRendererProvider.Context context){super(context);}
    @Override protected void renderSafe(BasinBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();
        try{
            pose.translate(.5,.5,.5);CatMachineOrientation.rotatePose(pose,CatMachineOrientation.bottom(be.getBlockState()));pose.translate(-.5,-.5,-.5);
            ((cn.laowu.mod.create.CatBasinFilter)be.getFilter().getSlotPositioning()).renderInLocalFrame(()->super.renderSafe(be,partial,pose,buffers,light,overlay));
        }finally{pose.popPose();}
    }
}
