package cn.laowu.mod.client.painting;
import cn.laowu.mod.LaoWuMod;import cn.laowu.mod.entity.CatPaintingEntity;
import com.mojang.blaze3d.vertex.*;import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;import net.minecraft.client.renderer.entity.*;import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;import net.minecraft.core.BlockPos;

public class CatPaintingRenderer extends EntityRenderer<CatPaintingEntity> {
 public static final ResourceLocation CASING=LaoWuMod.id("textures/block/cat_casing.png");
 public CatPaintingRenderer(EntityRendererProvider.Context context){super(context);}
 @Override public ResourceLocation getTextureLocation(CatPaintingEntity entity){return entity.getPaintingData().map(d->PaintingClientCache.resolve(d.hash())).orElse(PaintingClientCache.PLACEHOLDER);}
 @Override public void render(CatPaintingEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int packed){
  pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(180-entity.getYRot()));
  int w=entity.widthBlocks(),h=entity.heightBlocks();float left=-w/2f,bottom=-h/2f;
  var image=getTextureLocation(entity);
  boolean placeholder=image.equals(PaintingClientCache.PLACEHOLDER);
  if(!placeholder)entity.getPaintingData().ifPresent(d->net.minecraft.client.Minecraft.getInstance().getTextureManager().getTexture(image).setFilter(d.spec().mode()==cn.laowu.mod.painting.PaintingSpec.Mode.ORIGINAL,false));
  int ink=placeholder?cn.laowu.mod.painting.PaintingImages.MATTE:0xFFFFFFFF;
  boolean smooth=entity.getPaintingData().map(d->d.spec().mode()==cn.laowu.mod.painting.PaintingSpec.Mode.ORIGINAL).orElse(false);
  var front=buffers.getBuffer(PaintingRenderTypes.image(image,smooth));
  var matrix=pose.last();
  for(int x=0;x<w;x++)for(int y=0;y<h;y++){
   var side=entity.getDirection().getCounterClockWise();
   BlockPos at=BlockPos.containing(entity.getX()+side.getStepX()*(left+x+.5),entity.getY()+bottom+y+.5,entity.getZ()+side.getStepZ()*(left+x+.5));
   int light=LevelRenderer.getLightColor(entity.level(),at);
   float x1=Math.max(x,.0625f),x2=Math.min(x+1,w-.0625f),y1=Math.max(y,.0625f),y2=Math.min(y+1,h-.0625f);
   quad(front,matrix,left+x1,bottom+y1,left+x2,bottom+y2,-.033f,1-(x1-.0625f)/(w-.125f),1-(y1-.0625f)/(h-.125f),1-(x2-.0625f)/(w-.125f),1-(y2-.0625f)/(h-.125f),light,ink);
  }
  var frame=buffers.getBuffer(RenderType.entityCutoutNoCull(CASING));
  final float cap=PaintingFrameUv.CAP,frontZ=-.034f,backZ=.032f;
  for(int x=0;x<w;x++)for(int y=0;y<h;y++)
   quad(frame,matrix,left+x,bottom+y,left+x+1,bottom+y+1,backZ,
     x/(float)w,1-y/(float)h,(x+1)/(float)w,1-(y+1)/(float)h,frameLight(entity,left,bottom,x,y));
  for(var span:PaintingFrameUv.spans(w)){
   float x0=left+span.from(),x1=left+span.to(),u0=span.u0(),u1=span.u1();
   quad(frame,matrix,x0,bottom,x1,bottom+cap,frontZ,u0,1,u1,1-cap,frameLight(entity,left,bottom,span.cell(),0));
   quad(frame,matrix,x0,bottom+h-cap,x1,bottom+h,frontZ,u0,cap,u1,0,frameLight(entity,left,bottom,span.cell(),h-1));
   side(frame,matrix,x0,bottom,x1,backZ,frontZ,u0,u1,1-cap,1,packed);
   side(frame,matrix,x0,bottom+h,x1,frontZ,backZ,u0,u1,0,cap,packed);
  }
  for(var span:PaintingFrameUv.spans(h)){
   float y0=bottom+span.from(),y1=bottom+span.to(),v0=1-span.u0(),v1=1-span.u1();
   // Horizontal rails own the four front corners; don't draw a second coplanar face.
   if(span.from()!=0&&span.to()!=h){
    quad(frame,matrix,left,y0,left+cap,y1,frontZ,0,v0,cap,v1,frameLight(entity,left,bottom,0,span.cell()));
    quad(frame,matrix,left+w-cap,y0,left+w,y1,frontZ,1-cap,v0,1,v1,frameLight(entity,left,bottom,w-1,span.cell()));
   }
   edge(frame,matrix,left,y0,y1,frontZ,backZ,0,cap,v0,v1,packed);
   edge(frame,matrix,left+w,y0,y1,backZ,frontZ,1-cap,1,v0,v1,packed);
  }
  pose.popPose();super.render(entity,yaw,partial,pose,buffers,packed);
 }
 private static int frameLight(CatPaintingEntity entity,float left,float bottom,int x,int y){
  var side=entity.getDirection().getCounterClockWise();
  BlockPos at=BlockPos.containing(entity.getX()+side.getStepX()*(left+x+.5),entity.getY()+bottom+y+.5,entity.getZ()+side.getStepZ()*(left+x+.5));
  return LevelRenderer.getLightColor(entity.level(),at);
 }
 private static void quad(VertexConsumer v,PoseStack.Pose p,float x1,float y1,float x2,float y2,float z,float u1,float v1,float u2,float v2,int light){
  quad(v,p,x1,y1,x2,y2,z,u1,v1,u2,v2,light,0xFFFFFFFF);
 }
 private static void quad(VertexConsumer v,PoseStack.Pose p,float x1,float y1,float x2,float y2,float z,float u1,float v1,float u2,float v2,int light,int color){
  vertex(v,p,x1,y1,z,u1,v1,light,color);vertex(v,p,x1,y2,z,u1,v2,light,color);vertex(v,p,x2,y2,z,u2,v2,light,color);vertex(v,p,x2,y1,z,u2,v1,light,color);
 }
 private static void side(VertexConsumer v,PoseStack.Pose p,float x0,float y,float x1,float z0,float z1,float u0,float u1,float v0,float v1,int light){
  vertex(v,p,x0,y,z0,u0,v0,light);vertex(v,p,x0,y,z1,u0,v1,light);vertex(v,p,x1,y,z1,u1,v1,light);vertex(v,p,x1,y,z0,u1,v0,light);
 }
 private static void edge(VertexConsumer v,PoseStack.Pose p,float x,float y0,float y1,float z0,float z1,float u0,float u1,float v0,float v1,int light){
  vertex(v,p,x,y0,z0,u0,v0,light);vertex(v,p,x,y1,z0,u0,v1,light);vertex(v,p,x,y1,z1,u1,v1,light);vertex(v,p,x,y0,z1,u1,v0,light);
 }
 private static void vertex(VertexConsumer v,PoseStack.Pose p,float x,float y,float z,float u,float t,int light){
  vertex(v,p,x,y,z,u,t,light,0xFFFFFFFF);
 }
 private static void vertex(VertexConsumer v,PoseStack.Pose p,float x,float y,float z,float u,float t,int light,int color){
  v.vertex(p.pose(),x,y,z).color((color>>>16)&255,(color>>>8)&255,color&255,255).uv(u,t).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(),0,0,-1).endVertex();
 }
}
