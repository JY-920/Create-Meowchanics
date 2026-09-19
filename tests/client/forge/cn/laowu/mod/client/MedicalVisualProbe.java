package cn.laowu.mod.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/** Actual loaded shaders + evaluated cat mesh on the GPU, with no game world or save. */
public final class MedicalVisualProbe {
    public static void verify(Minecraft mc) {
        TextureTarget mask = null, scene = null, output = null;
        float[] clear = new float[4]; GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,clear);
        double clearDepth = GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        try (var state = new CatPerformanceOutline.State(); var target = new PerformanceSceneSnapshot.Target()) {
            ShaderInstance maskShader = shader(CatPerformanceOutline.class,"maskShader");
            ShaderInstance outline = shader(CatPerformanceOutline.class,"shader");
            ShaderInstance cross = shader(CatMedicalEffects.class,"crossShader");
            ShaderInstance haste = shader(CatMusicEffects.class,"hasteShader");
            mask = new TextureTarget(512,384,true,Minecraft.ON_OSX);
            scene = new TextureTarget(512,384,true,Minecraft.ON_OSX);
            output = new TextureTarget(512,384,true,Minecraft.ON_OSX);
            clear(scene,0,0,0);
            var root = HissingCatModel.createLayer().bakeRoot();
            var model = new HissingCatModel(root);
            var head=root.getChild("head"); var body=root.getChild("body");
            var lf=root.getChild("left_front_leg"); var rf=root.getChild("right_front_leg");
            var lh=root.getChild("left_hind_leg"); var rh=root.getChild("right_hind_leg");
            var tail=root.getChild("tail1"); var tip=root.getChild("tail2");
            CatMedicalAnimation.apply(40,head,body,lh,rh,lf,rf,tail,tip);
            if (!(lf.zRot<-.39F && rf.zRot>.39F) || Math.abs(head.xRot-.1F)>1e-5)
                throw new AssertionError("Recovery open-paw/head pose did not reach donor targets");
            if (Math.abs(lf.y-14.1F)>1e-5 || Math.abs(rf.y-14.1F)>1e-5 || Math.abs(body.xRot-(float)Math.PI/2)>.026F
                    || Math.abs(lh.y-18)>1e-5 || Math.abs(lh.xRot-.18F)>1e-5)
                throw new AssertionError("Combat medic must spread its paws on standing hips, never crouch");
            verifyPatientLayers(mc);
            var pose = new PoseStack();
            pose.translate(0,1,0); pose.scale(1,-1,-1);
            pose.mulPose(new org.joml.Quaternionf().rotationY(-.6F));
            Matrix4f projection = new Matrix4f().setOrtho(-2,2,-1.5F,1.5F,-10,10);
            var skin=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft","textures/entity/cat/tabby.png");
            int green = 0, purple = 0;
            for (int mode=0;mode<3;mode++) {
                boolean medical=mode!=1, music=mode!=0;
                clear(mask,0,0,0);
                RenderSystem.enableDepthTest(); RenderSystem.depthMask(true);
                RenderSystem.disableBlend(); RenderSystem.disableCull();
                RenderSystem.setShader(()->maskShader);
                maskShader.safeGetUniform("CaptureView").set(new Matrix4f());
                maskShader.safeGetUniform("CaptureProjection").set(projection);
                maskShader.safeGetUniform("OutlineData").set(.92F,.75F,mode==2?.99F:medical?.65F:.1F);
                maskShader.setSampler("SkinTexture",mc.getTextureManager().getTexture(skin).getId());
                BufferBuilder vertices=Tesselator.getInstance().getBuilder();
                vertices.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.NEW_ENTITY);
                model.renderToBuffer(pose,vertices,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,1,1,1,1);
                BufferUploader.drawWithShader(vertices.end());
                clear(output,.035F,.05F,.07F);
                RenderSystem.disableDepthTest();RenderSystem.depthMask(false);
                RenderSystem.enableBlend();RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);
                RenderSystem.setShader(()->outline);
                outline.setSampler("Silhouette",mask.getColorTextureId());
                outline.setSampler("SilhouetteDepth",mask.getDepthTextureId());
                outline.setSampler("SceneDepth",scene.getDepthTextureId());
                outline.safeGetUniform("MaskSize").set(512F,384F);
                outline.safeGetUniform("Time").set(2F);
                quad(outline,-1,-1,1,1,0,0,1,1);
                if(medical) {
                    RenderSystem.setShader(()->cross);
                    RenderSystem.blendFuncSeparate(GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);
                    for(int i=0;i<5;i++)quad(cross,-.55F+i*.25F,.35F+(i%2)*.15F,-.45F+i*.25F,.48F+(i%2)*.15F,-1,-1,1,1);
                }
                if(music) {
                    RenderSystem.setShader(()->haste);
                    RenderSystem.blendFuncSeparate(GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);
                    for(int i=0;i<3;i++)quad(haste,.4F+i*.2F,.6F+(i%2)*.1F,.5F+i*.2F,.73F+(i%2)*.1F,-1,-1,1,1);
                }
                int caseGreen=0,casePurple=0;
                try(var pixels=new NativeImage(512,384,false)) {
                    RenderSystem.bindTexture(output.getColorTextureId());pixels.downloadTexture(0,false);
                    for(int x=0;x<512;x++)for(int y=0;y<384;y++) {
                        int c=pixels.getPixelRGBA(x,y),r=c&255,g=(c>>>8)&255,b=(c>>>16)&255;
                        if(g>80 && g>r*1.2 && g>b*1.15) caseGreen++;
                        if(b>80 && b>g*1.2) casePurple++;
                    }
                    var path=java.nio.file.Path.of(mode==2?"music-visual-dual.png":"medical-visual-"+(medical?"healing":"performance")+".png");
                    pixels.flipY();pixels.writeToFile(path);
                }
                if(medical&&caseGreen<100||music&&casePurple<100)
                    throw new AssertionError("Missing individual/combined palette: mode="+mode+", green="+caseGreen+", purple="+casePurple);
                green+=caseGreen;purple+=casePurple;
            }
            perspectiveGlyphs(output);
            perspectiveMusicGlyphs(output);
            if(green<100 || purple<100)throw new AssertionError("Missing shader pixels: green="+green+", purple="+purple);
            System.out.println("PASS: actual GPU medical silhouette + crosses, donor paw pose, preserved purple performance palette: green="+green+", purple="+purple);
        } catch(Exception failure) {throw new IllegalStateException("Medical visual probe failed",failure);}
        finally {
            if(mask!=null)mask.destroyBuffers();if(scene!=null)scene.destroyBuffers();if(output!=null)output.destroyBuffers();
            RenderSystem.clearColor(clear[0],clear[1],clear[2],clear[3]);RenderSystem.clearDepth(clearDepth);
        }
    }
    private static void verifyPatientLayers(Minecraft mc) throws Exception {
        var layers=net.minecraft.client.renderer.entity.LivingEntityRenderer.class.getDeclaredField("layers");
        layers.setAccessible(true);
        int entities=0,players=0;
        var seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Object,Boolean>());
        var dispatcher=mc.getEntityRenderDispatcher();
        for(var field:dispatcher.getClass().getDeclaredFields()) {
            if(!java.util.Map.class.isAssignableFrom(field.getType()))continue;
            field.setAccessible(true);
            var map=(java.util.Map<?,?>)field.get(dispatcher);
            if(map==null)continue;
            for(var renderer:map.values()) {
                if(!(renderer instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer living)||!seen.add(renderer))continue;
                var list=(java.util.List<?>)layers.get(living);
                if(list.stream().anyMatch(MedicalPatientLayer.class::isInstance)) {
                    entities++;
                    if(living instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer)players++;
                }
            }
        }
        if(entities<10||players!=2)throw new AssertionError("Missing clinic patient layers: entities="+entities+", player skins="+players);
        System.out.println("PASS: medical outline layers installed on "+entities+" living renderers, including both player skin models");
    }
    private static Matrix4f capturedWorldView(Matrix4f expected,Matrix4f projection) throws Exception {
        var poses=new PoseStack();poses.last().pose().set(expected);
        CatPerformanceOutline.onRenderStage(new net.minecraftforge.client.event.RenderLevelStageEvent(net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_ENTITIES,null,poses,new Matrix4f(projection),1,0,null,null));
        // Forge's final-stage stack is projection-like; NeoForge's optional entity PoseStack is empty.
        // Neither may replace the real entity-stage world camera.
        poses.last().pose().zero();
        var poison=new PoseStack();poison.last().pose().set(projection).translate(50,50,50);
        CatPerformanceOutline.onRenderStage(new net.minecraftforge.client.event.RenderLevelStageEvent(net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_LEVEL,null,poison,new Matrix4f(projection),1,0,null,null));
        var field=CatPerformanceOutline.class.getDeclaredField("worldView");field.setAccessible(true);
        Matrix4f actual=(Matrix4f)field.get(null);
        var p=CatPerformanceOutline.class.getDeclaredField("worldProjection");p.setAccessible(true);
        if(!actual.equals(expected,1e-6F)||!((Matrix4f)p.get(null)).equals(projection,1e-6F))
            throw new AssertionError("World-stage camera overwritten, aliased or taken from the wrong event matrix");
        return new Matrix4f(actual);
    }
    private static void perspectiveGlyphs(TextureTarget output) throws Exception {
        Matrix4f projection=new Matrix4f().setPerspective((float)Math.toRadians(70),512F/384F,.05F,100F);
        var camera=new net.minecraft.world.phys.Vec3(11,6,-3);
        int cases=0;
        for(float yaw:new float[]{0,.9F,-1.4F}) {
            Matrix4f view=capturedWorldView(new Matrix4f().rotateX(.2F).rotateY(yaw),projection);
            var relative=new Matrix4f(view).invert().transformPosition(new org.joml.Vector3f(0,-.4F,-5));
            var position=camera.add(relative.x,relative.y,relative.z);
            var targets=java.util.List.of(new CatMedicalEffects.Target(position,.6F,.7F,true,.24F,6,false));
            clear(output,.035F,.05F,.07F);
            RenderSystem.enableDepthTest();RenderSystem.depthFunc(GL11.GL_LEQUAL);RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.enableBlend();
            int quads=CatMedicalEffects.drawGlyphs(targets,2,camera,view,projection);
            int green=greenPixels(output,cases==0?"medical-perspective-crosses.png":null);
            if(quads<4||green<40)throw new AssertionError("Production world-space crosses invisible: quads="+quads+", green="+green+", yaw="+yaw);
            clear(output,.035F,.05F,.07F);
            RenderSystem.depthMask(true);RenderSystem.clearDepth(.1);RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT,Minecraft.ON_OSX);RenderSystem.depthMask(false);
            CatMedicalEffects.drawGlyphs(targets,2,camera,view,projection);
            if(greenPixels(output,null)!=0)throw new AssertionError("Medical crosses show through occluding terrain");
            cases++;
        }
        System.out.println("PASS: "+cases+" actual perspective/camera-offset medical cross projections and terrain-depth occlusion cases");
    }

    private static void perspectiveMusicGlyphs(TextureTarget output) throws Exception {
        Matrix4f projection=new Matrix4f().setPerspective((float)Math.toRadians(70),512F/384F,.05F,100F);
        var camera=new net.minecraft.world.phys.Vec3(11,6,-3);
        int cases=0;
        for(float yaw:new float[]{0,.9F,-1.4F}) {
            Matrix4f view=capturedWorldView(new Matrix4f().rotateX(.2F).rotateY(yaw),projection);
            var relative=new Matrix4f(view).invert().transformPosition(new org.joml.Vector3f(0,-.4F,-5));
            var position=camera.add(relative.x,relative.y,relative.z);
            var targets=java.util.List.of(new CatMusicEffects.Target(position,.6F,.7F,.24F));
            clear(output,.035F,.05F,.07F);
            RenderSystem.enableDepthTest();RenderSystem.depthFunc(GL11.GL_LEQUAL);RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.enableBlend();
            int quads=CatMusicEffects.drawGlyphs(targets,2,camera,view,projection);
            int purple=purplePixels(output,cases==0?"music-perspective-haste.png":null);
            if(quads<3||purple<30)throw new AssertionError("Production haste icons invisible: quads="+quads+", purple="+purple+", yaw="+yaw);
            clear(output,.035F,.05F,.07F);
            RenderSystem.depthMask(true);RenderSystem.clearDepth(.1);RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT,Minecraft.ON_OSX);RenderSystem.depthMask(false);
            CatMusicEffects.drawGlyphs(targets,2,camera,view,projection);
            if(purplePixels(output,null)!=0)throw new AssertionError("Haste icons show through occluding terrain");
            cases++;
        }
        System.out.println("PASS: "+cases+" actual perspective/camera-offset haste projections and terrain-depth occlusion cases; green/purple dual aura");
    }
    private static int purplePixels(TextureTarget target,String name) throws Exception {
        int purple=0;
        try(var pixels=new NativeImage(512,384,false)) {
            RenderSystem.bindTexture(target.getColorTextureId());pixels.downloadTexture(0,false);
            for(int x=0;x<512;x++)for(int y=0;y<384;y++) {
                int c=pixels.getPixelRGBA(x,y),g=(c>>>8)&255,b=(c>>>16)&255;
                if(b>80&&b>g*1.2)purple++;
            }
            if(name!=null){pixels.flipY();pixels.writeToFile(java.nio.file.Path.of(name));}
        }
        return purple;
    }
    private static int greenPixels(TextureTarget target,String name) throws Exception {
        int green=0;
        try(var pixels=new NativeImage(512,384,false)) {
            RenderSystem.bindTexture(target.getColorTextureId());pixels.downloadTexture(0,false);
            for(int x=0;x<512;x++)for(int y=0;y<384;y++) {
                int c=pixels.getPixelRGBA(x,y),r=c&255,g=(c>>>8)&255,b=(c>>>16)&255;
                if(g>80&&g>r*1.2&&g>b*1.15)green++;
            }
            if(name!=null){pixels.flipY();pixels.writeToFile(java.nio.file.Path.of(name));}
        }
        return green;
    }
    private static ShaderInstance shader(Class<?> type,String name)throws Exception {
        var field=type.getDeclaredField(name);field.setAccessible(true);
        var shader=(ShaderInstance)field.get(null);
        if(shader==null)throw new AssertionError("Shader did not load: "+name);
        return shader;
    }
    private static void clear(TextureTarget target,float r,float g,float b) {
        target.bindWrite(true);RenderSystem.depthMask(true);RenderSystem.clearDepth(1);
        RenderSystem.clearColor(r,g,b,1);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT,Minecraft.ON_OSX);
    }
    private static void quad(ShaderInstance shader,float x0,float y0,float x1,float y1,float u0,float v0,float u1,float v1) {
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        b.vertex(x0,y0,0).uv(u0,v0).color(-1).endVertex();
        b.vertex(x1,y0,0).uv(u1,v0).color(-1).endVertex();
        b.vertex(x1,y1,0).uv(u1,v1).color(-1).endVertex();
        b.vertex(x0,y1,0).uv(u0,v1).color(-1).endVertex();
        BufferUploader.drawWithShader(b.end());
    }
}
