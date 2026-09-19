package cn.laowu.mod.client;

import cn.laowu.mod.*;
import cn.laowu.mod.genetics.*;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

/** Receive real music state -> shared registered cat model -> actual appearance layer -> GPU pixels. */
public final class SupportPresentationProbe {
    public static void verify(Minecraft mc) {
        TextureTarget output=null;
        var projection=new Matrix4f(RenderSystem.getProjectionMatrix());var sorting=RenderSystem.getVertexSorting();
        var stack=RenderSystem.getModelViewStack();stack.pushMatrix();stack.identity();RenderSystem.applyModelViewMatrix();
        float[] clear=new float[4];GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,clear);
        double depth=GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        try(var guard=new CatPerformanceOutline.State();var framebuffer=new PerformanceSceneSnapshot.Target()){
            output=new TextureTarget(384,384,true,Minecraft.ON_OSX);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(-1.4F,1.4F,-1.4F,1.4F,-10,10),VertexSorting.ORTHOGRAPHIC_Z);
            var world=new AgentWatchVisualProbe.ProbeLevel();world.clock=1000;
            var root=HissingCatModel.createLayer().bakeRoot();var model=new HissingCatModel(root);
            var skin=ResourceLocation.withDefaultNamespace("textures/entity/cat/tabby.png");
            var parent=new RenderLayerParent<Cat,CatModel<Cat>>(){
                public CatModel<Cat> getModel(){return model;}
                public ResourceLocation getTextureLocation(Cat cat){return skin;}
            };
            var layer=new CatAppearanceLayer(parent,new CatAppearanceModel(CatAppearanceModel.createLayer().bakeRoot()));
            for(int mode=0;mode<6;mode++){
                var cat=new Cat(EntityType.CAT,world){@Override public boolean onGround(){return true;}};
                cat.setId(98500+mode);
                CatTraitData.set(cat,mode==5?CatTraitProfile.EMPTY.withLevel(CatTrait.PIPA_PERFORMANCE,1):CatTraitProfile.EMPTY);
                cat.setAge(mode==4?-24000:0);
                cat.getPersistentData().putBoolean(CatClothesData.EQUIPPED_TAG,mode!=5);
                cat.getPersistentData().putString(CatClothesData.OUTFIT_TAG,mode==5?"none":"music");
                CatMusicSupport.receive(cat,mode==1?1:mode==2||mode==5?-1:0,40,0,8);
                cat.setInvisible(mode==3);
                model.young=cat.isBaby();model.prepareMobModel(cat,0,0,0);model.setupAnim(cat,0,0,40,0,0);
                boolean expected=mode==0||mode==4||mode==5;
                if(mode==0 && (!model.isPlayingPipa()||!CatTraitData.read(cat).orElseThrow().traits().isEmpty()))
                    throw new AssertionError("Career pipa pose must not require any trait");
                output.bindWrite(true);RenderSystem.clearColor(0,0,0,0);RenderSystem.clearDepth(1);RenderSystem.depthMask(true);
                RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT,Minecraft.ON_OSX);
                RenderSystem.setShaderColor(1,1,1,1);RenderSystem.enableDepthTest();
                var poses=new PoseStack();poses.translate(0,1,0);poses.scale(1,-1,-1);
                poses.mulPose(new Quaternionf().rotationX(-.12F).rotateY(.4F));
                var buffers=mc.renderBuffers().bufferSource();
                layer.render(poses,buffers,LightTexture.FULL_BRIGHT,cat,0,0,0,40,0,0);buffers.endBatch();
                int visible=0;
                try(var pixels=new NativeImage(384,384,false)){
                    RenderSystem.bindTexture(output.getColorTextureId());pixels.downloadTexture(0,false);
                    for(int x=0;x<384;x++)for(int y=0;y<384;y++)if((pixels.getPixelRGBA(x,y)&0xffffff)!=0)visible++;
                    pixels.flipY();pixels.writeToFile(java.nio.file.Path.of("music-pipa-case-"+mode+".png"));
                }
                if(expected?visible<80:visible!=0)throw new AssertionError("Pipa visibility mode "+mode+": "+visible);
                if(mode==0){
                    model.renderToBuffer(poses,buffers.getBuffer(RenderType.entityCutoutNoCull(skin)),
                            LightTexture.FULL_BRIGHT,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,0xffffffff);
                    buffers.endBatch();
                    try(var pixels=new NativeImage(384,384,false)){
                        RenderSystem.bindTexture(output.getColorTextureId());pixels.downloadTexture(0,false);
                        pixels.flipY();pixels.writeToFile(java.nio.file.Path.of("music-pipa-restored.png"));
                    }
                }
            }
            System.out.println("PASS: 6 real pipa layer GPU cases: traitless musician, dance/off/invisibility, kitten, original trait performance");
        }catch(Exception e){throw new IllegalStateException("Support presentation probe failed",e);}
        finally{
            if(output!=null)output.destroyBuffers();
            stack.popMatrix();RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(projection,sorting);
            RenderSystem.clearColor(clear[0],clear[1],clear[2],clear[3]);RenderSystem.clearDepth(depth);
        }
    }
    private SupportPresentationProbe(){}
}
