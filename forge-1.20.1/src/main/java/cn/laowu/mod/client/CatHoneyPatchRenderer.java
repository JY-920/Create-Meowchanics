package cn.laowu.mod.client;
import cn.laowu.mod.entity.CatHoneyPatch;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
/** Ground-only decal, reusing the vanilla honey block top texture. */
public final class CatHoneyPatchRenderer extends EntityRenderer<CatHoneyPatch> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/honey_block_top.png");
    public CatHoneyPatchRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
    @Override public ResourceLocation getTextureLocation(CatHoneyPatch entity){return TEXTURE;}
    @Override public void render(CatHoneyPatch entity,float yaw,float partial,PoseStack poses,MultiBufferSource buffers,int light){
        var vertex=buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        var pose=poses.last();
        for(int i=0;i<24;i++){
            double a=i*Math.PI/12,b=(i+1)*Math.PI/12;
            point(vertex,pose,0,0,light);
            point(vertex,pose,(float)Math.cos(b)*CatHoneyPatch.RADIUS,(float)Math.sin(b)*CatHoneyPatch.RADIUS,light);
            point(vertex,pose,(float)Math.cos(a)*CatHoneyPatch.RADIUS,(float)Math.sin(a)*CatHoneyPatch.RADIUS,light);
            point(vertex,pose,0,0,light);
        }
        super.render(entity,yaw,partial,poses,buffers,light);
    }
    private static void point(VertexConsumer vertex,PoseStack.Pose pose,float x,float z,int light){
        vertex.vertex(pose.pose(),x,.01F,z).color(1F,.85F,.55F,.72F)
                .uv(.5F+x/(CatHoneyPatch.RADIUS*2),.5F+z/(CatHoneyPatch.RADIUS*2))
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),0,1,0).endVertex();
    }
}
