package cn.laowu.mod.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import net.minecraft.world.entity.animal.Cat;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL11;
import java.util.*;
import com.mojang.blaze3d.shaders.BlendMode;
import cn.laowu.mod.mixin.PerformanceBlendModeAccessor;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Purple reinforcement rim, driven exclusively by the rendered performance pose. */
@Mod.EventBusSubscriber(modid="laowu", value=Dist.CLIENT)
public final class CatPerformanceOutline {
    private static final int LIMIT=12;
    private static final LinkedHashMap<UUID,Capture> CAPTURES=new LinkedHashMap<>();
    private static final BufferBuilder[] STORAGE=new BufferBuilder[LIMIT];
    private static final PerformanceSceneSnapshot SCENE=new PerformanceSceneSnapshot();
    private static ShaderInstance shader,maskShader;
    private static TextureTarget mask;
    private static boolean accepting;
    private static long lastDraw;
    private record Part(BufferBuilder.RenderedBuffer mesh,ResourceLocation texture) {}
    private record Capture(List<Part> parts,Matrix4f view,Matrix4f projection,float strength,float width,
                           float seed) {}
    static void setShader(ShaderInstance value){clear();shader=value;}
    static void setMask(ShaderInstance value){clear();maskShader=value;}
    static void clear(){
        accepting=false;clearMeshes();releaseTargets();
        for(int i=0;i<STORAGE.length;i++)if(STORAGE[i]!=null){STORAGE[i].discard();STORAGE[i]=null;}
    }
    private static void clearMeshes(){
        for(Capture capture:CAPTURES.values())for(Part part:capture.parts)part.mesh.release();
        CAPTURES.clear();
    }
    private static void releaseTargets(){SCENE.release();if(mask!=null){mask.destroyBuffers();mask=null;}}
    private static Object frameLevel;
    private static long ticks(){var level=Minecraft.getInstance().level;return level==null?0:level.getGameTime();}

    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event){
        if(Minecraft.getInstance().level==null && frameLevel!=null){
            clear();frameLevel=null;lastDraw=0;
        }
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event){
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_SKY){
            if(frameLevel!=Minecraft.getInstance().level){clear();frameLevel=Minecraft.getInstance().level;lastDraw=ticks();}
            clearMeshes();accepting=true;
        }else if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_ENTITIES){
            accepting=false;
        }else if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_LEVEL){
            // Match Gojo's outline pass: Iris/Oculus can replace earlier particle targets.
            render(event);
        }
    }

    static void capture(Cat cat,HissingCatModel model,PoseStack poses,ResourceLocation body){
        Minecraft mc=Minecraft.getInstance();
        if(!accepting||shader==null||maskShader==null||mc.level!=cat.level()
                ||!model.isPlayingPerformance()||!cat.isAlive()||cat.isInvisible())return;
        double distance=cat.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition());
        if(distance>96*96)return;
        int slot=0;
        for(UUID id:CAPTURES.keySet()){if(id.equals(cat.getUUID()))break;slot++;}
        if(slot>=LIMIT)return;
        Capture previous=CAPTURES.get(cat.getUUID());
        if(previous!=null){for(Part part:previous.parts)part.mesh.release();previous.parts.clear();}
        if(STORAGE[slot]==null)STORAGE[slot]=new BufferBuilder(32768);
        BufferBuilder vertices=STORAGE[slot];
        vertices.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.NEW_ENTITY);
        model.renderToBuffer(poses,vertices,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,1,1,1,1);
        BufferBuilder.RenderedBuffer data=vertices.endOrDiscardIfEmpty();
        if(data==null){CAPTURES.remove(cat.getUUID());return;}
        List<Part> parts=new ArrayList<>(1);parts.add(new Part(data,body));
        CAPTURES.put(cat.getUUID(),new Capture(parts,new Matrix4f(RenderSystem.getModelViewMatrix()),
                new Matrix4f(RenderSystem.getProjectionMatrix()),.92f,
                (float)Math.max(.5,Math.min(1.5,12/Math.sqrt(Math.max(1,distance)))),Math.floorMod(cat.getId(),251)/251f));
    }
    static void render(RenderLevelStageEvent event){
        accepting=false;
        Minecraft mc=Minecraft.getInstance();long now=ticks();
        if(shader==null||maskShader==null||mc.level==null||CAPTURES.isEmpty()){
            clearMeshes();if(now-lastDraw>40)releaseTargets();return;
        }
        boolean scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        double clearDepth=GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        float[] clear=new float[4];GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,clear);
        try(State ignored=new State();PerformanceSceneSnapshot.Target target=new PerformanceSceneSnapshot.Target()){
            if(!SCENE.capture())return;
            lastDraw=now;
            int[] viewport=new int[4];GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
            float downscale=Math.min(.5f,960f/viewport[2]);
            int width=Math.max(1,Math.round(viewport[2]*downscale)),height=Math.max(1,Math.round(viewport[3]*downscale));
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            try(PerformanceSceneSnapshot.Target original=new PerformanceSceneSnapshot.Target()){
                if(mask==null||mask.width!=width||mask.height!=height){
                    if(mask!=null)mask.destroyBuffers();
                    mask=new PerformanceTarget(width,height,true,Minecraft.ON_OSX);mask.setFilterMode(GL11.GL_LINEAR);
                }
                mask.bindWrite(true);RenderSystem.clearColor(0,0,0,0);RenderSystem.clearDepth(1);
                RenderSystem.depthMask(true);RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT,Minecraft.ON_OSX);
                // Rasterize a solid silhouette against its own depth. Comparing half-resolution
                // face fragments to full-resolution world depth punched moire holes into the mask.
                RenderSystem.enableDepthTest();RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.disableCull();RenderSystem.disableBlend();RenderSystem.setShader(()->maskShader);
                for(Capture capture:CAPTURES.values()){
                    maskShader.safeGetUniform("CaptureView").set(capture.view);
                    maskShader.safeGetUniform("CaptureProjection").set(capture.projection);
                    // Strength, distance-adjusted width and breathing phase.
                    maskShader.safeGetUniform("OutlineData").set(capture.strength,capture.width/2,
                            capture.seed);
                    for(var iterator=capture.parts.iterator();iterator.hasNext();){
                        Part part=iterator.next();iterator.remove();
                        maskShader.setSampler("SkinTexture",mc.getTextureManager().getTexture(part.texture).getId());
                        // drawWithShader consumes and closes the mesh. Remove it from cleanup first.
                        BufferBuilder.RenderedBuffer mesh=part.mesh;
                        BufferUploader.drawWithShader(mesh);
                    }
                }
            }
            shader.setSampler("Silhouette",mask.getColorTextureId());shader.setSampler("SilhouetteDepth",mask.getDepthTextureId());
            shader.setSampler("SceneDepth",SCENE.depth());
            shader.safeGetUniform("MaskSize").set((float)width,(float)height);
            shader.safeGetUniform("Time").set((now%24000+event.getPartialTick())*.05f);
            RenderSystem.setShader(()->shader);RenderSystem.disableDepthTest();RenderSystem.depthMask(false);
            RenderSystem.disableCull();RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);
            BufferBuilder b=Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
            b.vertex(-1,-1,0).uv(0,0).color(-1).endVertex();b.vertex(1,-1,0).uv(1,0).color(-1).endVertex();
            b.vertex(1,1,0).uv(1,1).color(-1).endVertex();b.vertex(-1,1,0).uv(0,1).color(-1).endVertex();
            BufferUploader.drawWithShader(b.end());
        }finally{
            clearMeshes();RenderSystem.clearColor(clear[0],clear[1],clear[2],clear[3]);RenderSystem.clearDepth(clearDepth);
            if(scissor)GL11.glEnable(GL11.GL_SCISSOR_TEST);else GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }
    static final class State implements AutoCloseable {
        private final BlendMode cachedBlend = PerformanceBlendModeAccessor.laowu$getLastApplied();
        private final PerformanceSamplers samplers = new PerformanceSamplers();
        final ShaderInstance shader=RenderSystem.getShader();
        final boolean blend=GL11.glIsEnabled(GL11.GL_BLEND),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE);
        final boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),write=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        final int func=GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        final int sr=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dr=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        final int sa=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),da=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        final boolean stencil=GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
        final int equation=GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_BLEND_EQUATION_RGB),
                alphaEquation=GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_BLEND_EQUATION_ALPHA);
        final int[] colorMask=new int[4];
        State(){
            // Reconcile cached GL state before a pass changes it. Other renderers can use raw GL.
            if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
            if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
            if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            RenderSystem.depthMask(write);RenderSystem.depthFunc(func);
            RenderSystem.blendFuncSeparate(sr,dr,sa,da);
            GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK,colorMask);
            RenderSystem.colorMask(true,true,true,true);GL11.glColorMask(true,true,true,true);
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            RenderSystem.blendEquation(org.lwjgl.opengl.GL14.GL_FUNC_ADD);
        }
        public void close() {
            RenderSystem.depthMask(write);RenderSystem.depthFunc(func);
            if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(sr,dr,sa,da);
            if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
            RenderSystem.setShader(()->shader);
            RenderSystem.colorMask(colorMask[0]!=0,colorMask[1]!=0,colorMask[2]!=0,colorMask[3]!=0);
            GL11.glColorMask(colorMask[0]!=0,colorMask[1]!=0,colorMask[2]!=0,colorMask[3]!=0);
            if(stencil)GL11.glEnable(GL11.GL_STENCIL_TEST);else GL11.glDisable(GL11.GL_STENCIL_TEST);
            RenderSystem.blendEquation(equation);org.lwjgl.opengl.GL20.glBlendEquationSeparate(equation,alphaEquation);
            samplers.close();
            // 1.20.1 tracks this separately from RenderSystem's GL state. Leaving an
            // additive effect cached makes the next GUI shader override the vignette's
            // ZERO / ONE_MINUS_SRC_COLOR blend and paint its opaque black center.
            PerformanceBlendModeAccessor.laowu$setLastApplied(cachedBlend);
        }
    }
}
