package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.item.*;
import net.minecraft.core.registries.BuiltInRegistries;

/** Same 16px item silhouette, with front/back and pixel-edge thickness like generated item models. */
public final class KimiArmorDyeItemRenderer extends BlockEntityWithoutLevelRenderer {
    public KimiArmorDyeItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) { super(dispatcher,models); }
    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                      MultiBufferSource buffers, int light, int overlay) {
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        var original=LaoWuMod.id("textures/item/" + id.getPath() + ".png");
        var texture=KimiArmorDyeTextures.texture(original,stack);
        var out=ItemRenderer.getFoilBufferDirect(buffers,RenderType.entityCutoutNoCull(texture),true,stack.hasFoil());
        quad(pose,out,light,overlay,0,0,.53125F,1,1,.53125F,0,1,1,0,0,0,1,0);
        quad(pose,out,light,overlay,0,0,.46875F,1,1,.46875F,0,1,1,0,0,0,-1,0);
        try {
            NativeImage image=KimiArmorDyeTextures.source(original);
            int w=image.getWidth(),h=image.getHeight();
            for(int y=0;y<h;y++) for(int x=0;x<w;x++) {
                if(!solid(image,x,y)) continue;
                float x0=(float)x/w,x1=(float)(x+1)/w,y0=1F-(float)(y+1)/h,y1=1F-(float)y/h;
                float u=(x+.5F)/w,v=(y+.5F)/h;
                if(!solid(image,x-1,y)) quad(pose,out,light,overlay,x0,y0,.46875F,x0,y1,.53125F,u,v,u,v,-1,0,0,1);
                if(!solid(image,x+1,y)) quad(pose,out,light,overlay,x1,y0,.46875F,x1,y1,.53125F,u,v,u,v,1,0,0,1);
                if(!solid(image,x,y-1)) quad(pose,out,light,overlay,x0,y1,.46875F,x1,y1,.53125F,u,v,u,v,0,1,0,2);
                if(!solid(image,x,y+1)) quad(pose,out,light,overlay,x0,y0,.46875F,x1,y0,.53125F,u,v,u,v,0,-1,0,2);
            }
        } catch(java.io.IOException ignored) {}
    }
    private static boolean solid(NativeImage image,int x,int y) {
        return x>=0 && y>=0 && x<image.getWidth() && y<image.getHeight() && (image.getPixelRGBA(x,y)>>>24)!=0;
    }
    private static void quad(PoseStack p,VertexConsumer out,int light,int overlay,
            float x0,float y0,float z0,float x1,float y1,float z1,
            float u0,float v0,float u1,float v1,float nx,float ny,float nz,int plane) {
        float[][] points = plane==0 ? new float[][]{{x0,y0,z0,u0,v0},{x1,y0,z0,u1,v0},{x1,y1,z0,u1,v1},{x0,y1,z0,u0,v1}}
                : plane==1 ? new float[][]{{x0,y0,z0,u0,v0},{x0,y0,z1,u1,v0},{x0,y1,z1,u1,v1},{x0,y1,z0,u0,v1}}
                : new float[][]{{x0,y0,z0,u0,v0},{x1,y0,z0,u1,v0},{x1,y0,z1,u1,v1},{x0,y0,z1,u0,v1}};
        // Both faces are explicit; the cutout render type is intentionally no-cull.
        for(float[] q:points) {
            out.vertex(p.last().pose(),q[0],q[1],q[2]).color(255,255,255,255).uv(q[3],q[4])
                    .overlayCoords(overlay).uv2(light).normal(p.last().normal(),nx,ny,nz).endVertex();
        }
    }
}
