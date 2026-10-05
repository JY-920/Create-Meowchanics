package cn.laowu.mod.client;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.create.CreatureTransmitterBlockEntity;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.blockentity.*;
/** Samples only the authored red indicator pixels; the casing texture remains unchanged. */
public final class CreatureTransmitterRenderer implements BlockEntityRenderer<CreatureTransmitterBlockEntity> {
    public CreatureTransmitterRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(CreatureTransmitterBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer.renderOnBlockEntity(be,partial,pose,buffers,light,overlay);
        int signal=be.getSignal();if(signal<=0)return;
        var vertices=buffers.getBuffer(RenderType.entityTranslucentEmissive(LaoWuMod.id("textures/block/creature_transmitter.png")));
        for(int side=0;side<4;side++){
            pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(side*90));pose.translate(-.5,0,-.5);
            float alpha=signal/15f;
            vertex(vertices,pose.last(),6/16f,3/16f,6/32f,13/32f,alpha);
            vertex(vertices,pose.last(),10/16f,3/16f,10/32f,13/32f,alpha);
            vertex(vertices,pose.last(),10/16f,13/16f,10/32f,3/32f,alpha);
            vertex(vertices,pose.last(),6/16f,13/16f,6/32f,3/32f,alpha);
            pose.popPose();
        }
    }
    private static void vertex(VertexConsumer v,PoseStack.Pose pose,float x,float y,float u,float w,float alpha){
        v.addVertex(pose.pose(),x,y,1.0005f).setColor(1f,1f,1f,alpha).setUv(u,w).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose,0,0,1);
    }
}
