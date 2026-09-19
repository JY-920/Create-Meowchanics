package cn.laowu.mod.client;

import cn.laowu.mod.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.pipeline.TextureTarget;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import java.util.*;

/** Real client state -> registered renderer/model -> indicator collection -> production GPU ring path. */
public final class CareerFeedbackVisualProbe {
    private static Cat cat(AgentWatchVisualProbe.ProbeLevel level, int id, CatOutfitType outfit) {
        var cat=new Cat(EntityType.CAT,level);
        cat.setId(id);
        cat.getPersistentData().putBoolean(CatClothesData.EQUIPPED_TAG,true);
        cat.getPersistentData().putString(CatClothesData.OUTFIT_TAG,outfit.id());
        return cat;
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void verify(Minecraft mc){
        var a=new AgentWatchVisualProbe.ProbeLevel();a.clock=1000;
        var b=new AgentWatchVisualProbe.ProbeLevel();b.clock=2000;
        var medic=cat(a,93400,CatOutfitType.MEDICAL);
        var mirror=cat(b,medic.getId(),CatOutfitType.MEDICAL);mirror.setUUID(medic.getUUID());
        check(medic.equals(mirror),"Fixture reproduces vanilla runtime-ID equality across worlds");
        CatMedicalHealing.receive(medic,true,true,5,6,false);
        CatMedicalHealing.receive(mirror,false,false,0,0,false);
        check(CatMedicalHealing.casting(medic)&&!CatMedicalHealing.casting(mirror),"Medical visuals never collide across worlds/authorities");
        var preview=cat(a,medic.getId(),CatOutfitType.MEDICAL);preview.setUUID(medic.getUUID());
        CatMedicalHealing.stop(preview);
        check(CatMedicalHealing.casting(medic)&&!CatMedicalHealing.casting(preview),"Preview identity cannot clear the actual medic");
        var musician=cat(a,93401,CatOutfitType.MUSIC);musician.setPos(4,0,0);
        var otherMusic=cat(b,musician.getId(),CatOutfitType.MUSIC);otherMusic.setUUID(musician.getUUID());
        CatMusicSupport.receive(musician,0,30,0,10);
        CatMusicSupport.receive(otherMusic,-1,0,0,0);
        check(CatMusicSupport.performing(musician)&&!CatMusicSupport.performing(otherMusic),"Music visuals never collide across worlds");
        var fighter=cat(a,93402,CatOutfitType.AGENT);fighter.setPos(2,0,0);
        CatMusicSupport.receive(fighter,-1,0,.25,0);
        var indicators=CatPerformanceOutline.collectIndicators(List.of(medic,musician,fighter),new Vec3(0,2,8),0);
        check(indicators.healing().size()==1&&indicators.music().size()==1&&indicators.areas().size()==1,
                "Caster circles/recipient glyphs exist without any silhouette capture or entity render-layer callback");
        check(indicators.areas().get(0).radius()==10,"Music circle uses synchronized radius");
        var renderer=mc.getEntityRenderDispatcher().getRenderer(medic);
        check(renderer instanceof HissingCatRenderer,"Actual registered cat renderer supports career animation");
        var root=HissingCatModel.createLayer().bakeRoot();var model=new HissingCatModel(root);
        model.prepareMobModel(medic,0,0,0);model.setupAnim(medic,0,0,medic.tickCount+.5F,0,0);
        check(Math.abs(root.getChild("left_front_leg").zRot)>.15,"Real medic setupAnim opens the paws");
        for(int pose=0;pose<2;pose++){
            CatMusicSupport.receive(musician,pose,30,0,10);
            model.prepareMobModel(musician,0,0,0);model.setupAnim(musician,0,0,musician.tickCount+.5F,0,0);
            check(pose==0?model.isPlayingPipa():(model.isPlayingPerformance()&&!model.isPlayingPipa()),"Real musician setupAnim selects its synchronized performance");
        }
        var poses=new ArrayList<String>();
        for(int move=0;move<3;move++){
            CatAgentMeleeMotion.receive(fighter,move,18,7);
            model.prepareMobModel(fighter,0,0,0);model.setupAnim(fighter,0,0,fighter.tickCount+.5F,0,0);
            String pose="";
            for(String name:List.of("body","left_front_leg","right_front_leg","left_hind_leg","right_hind_leg")){
                var part=root.getChild(name);pose+=part.xRot+","+part.yRot+","+part.zRot+";";
            }
            poses.add(pose);
        }
        check(new HashSet<>(poses).size()==3,"All three real setupAnim attack poses differ");
        a.clock+=40;
        check(!CatMedicalHealing.casting(medic)&&!CatMusicSupport.performing(musician)
                &&CatAgentMeleeMotion.current(fighter)==null,"Client visuals expire and do not stick");
        drawRings(mc);
        System.out.println("PASS: real client medical/music/agent states, world/preview isolation, registered renderer, setupAnim and capture-independent indicators");
    }
    private static void drawRings(Minecraft mc){
        TextureTarget output=null;
        float[] oldClear=new float[4];GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,oldClear);
        double oldDepth=GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        var originalProjection=new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting=RenderSystem.getVertexSorting();
        var stack=RenderSystem.getModelViewStack();stack.pushPose();
        try(var guard=new CatPerformanceOutline.State();var framebuffer=new PerformanceSceneSnapshot.Target()){
            output=new TextureTarget(640,400,true,Minecraft.ON_OSX);
            Vec3[] cameras={new Vec3(10,9,14),new Vec3(-14,7,4),new Vec3(0,10,-14),new Vec3(4,14,1)};
            for(int test=0;test<cameras.length;test++){
                output.bindWrite(true);RenderSystem.depthMask(true);RenderSystem.clearDepth(1);
                RenderSystem.clearColor(0,0,0,0);RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT,Minecraft.ON_OSX);
                Vec3 camera=cameras[test],direction=new Vec3(2,0,0).subtract(camera);
                var view=new Matrix4f().lookAt(new Vector3f(),new Vector3f((float)direction.x,(float)direction.y,(float)direction.z),new Vector3f(0,1,0));
                var projection=new Matrix4f().perspective((float)Math.toRadians(70),1.6F,.1F,80);
                // Deliberately unrelated AFTER_LEVEL/GUI matrices: ring draw must use only its supplied camera.
                stack.setIdentity();stack.translate(70,40,-30);RenderSystem.applyModelViewMatrix();
                var gui=new Matrix4f().setOrtho(0,640,400,0,1,1000);
                RenderSystem.setProjectionMatrix(gui,VertexSorting.ORTHOGRAPHIC_Z);
                var savedView=new Matrix4f(RenderSystem.getModelViewMatrix());
                CatMedicalEffects.drawFrame(List.of(new CatMedicalEffects.Target(Vec3.ZERO,.6F,.7F,true,.2F,3,false)),
                        2,camera,view,projection);
                CatSupportAreas.draw(List.of(new CatSupportAreas.Area(new Vec3(4,0,0),4,false,.4F)),
                        2,camera,view,projection,.65F,.24F,1F);
                check(gui.equals(RenderSystem.getProjectionMatrix(),1e-6F)&&savedView.equals(RenderSystem.getModelViewMatrix(),1e-6F),
                        "Ring pass restores GUI projection and model-view state");
                int green=0,purple=0;
                try(var pixels=new NativeImage(640,400,false)){
                    RenderSystem.bindTexture(output.getColorTextureId());pixels.downloadTexture(0,false);
                    for(int x=0;x<640;x++)for(int y=0;y<400;y++){
                        int c=pixels.getPixelRGBA(x,y),r=c&255,g=(c>>>8)&255,blue=(c>>>16)&255;
                        if(g>50&&g>r*1.4&&g>blue*1.2)green++;
                        if(blue>60&&blue>g*1.5&&r>30)purple++;
                    }
                    pixels.flipY();pixels.writeToFile(java.nio.file.Path.of("career-feedback-areas-"+test+".png"));
                }
                check(green>250&&purple>300,"Visible green healing and purple music circles across camera angles: "+test+" "+green+"/"+purple);
                System.out.println("PASS: production support circles angle "+test+", "+green+" green / "+purple+" purple pixels; no double camera transform");
            }
        }catch(Exception e){throw new IllegalStateException(e);}
        finally{
            if(output!=null)output.destroyBuffers();
            stack.popPose();RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(originalProjection,sorting);
            RenderSystem.clearColor(oldClear[0],oldClear[1],oldClear[2],oldClear[3]);RenderSystem.clearDepth(oldDepth);
        }
    }
}
