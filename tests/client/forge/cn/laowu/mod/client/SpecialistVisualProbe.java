package cn.laowu.mod.client;

import cn.laowu.mod.CatOutfitType;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import java.util.*;

public final class SpecialistVisualProbe {
    public static void verify(Minecraft mc) {
        TextureTarget output=null;
        float[] clear=new float[4];GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,clear);
        double clearDepth=GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        RenderSystem.backupProjectionMatrix();
        var view=RenderSystem.getModelViewStack();view.pushPose();view.setIdentity();RenderSystem.applyModelViewMatrix();
        try(var guard=new CatPerformanceOutline.State();var target=new PerformanceSceneSnapshot.Target()) {
            output=new TextureTarget(768,384,true,Minecraft.ON_OSX);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(-4.5F,4.5F,-2.25F,2.25F,-20,20),VertexSorting.ORTHOGRAPHIC_Z);
            var root=HissingCatModel.createLayer().bakeRoot();var model=new HissingCatModel(root);
            var skin=ResourceLocation.fromNamespaceAndPath("minecraft","textures/entity/cat/tabby.png");
            int count=0,agentMove=0;
            for(var outfit:new CatOutfitType[]{CatOutfitType.AGENT,CatOutfitType.AGENT,CatOutfitType.AGENT,CatOutfitType.DIVING,CatOutfitType.COCKROACH,CatOutfitType.MEDICAL,CatOutfitType.FLIGHT}) {
                var def=CatOutfitModels.get(outfit);
                mc.getResourceManager().getResourceOrThrow(def.model());
                mc.getResourceManager().getResourceOrThrow(def.texture());
                output.bindWrite(true);RenderSystem.depthMask(true);
                RenderSystem.clearDepth(1);RenderSystem.clearColor(.12F,.15F,.19F,1);
                RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT,Minecraft.ON_OSX);
                int[] vertices=new int[2];
                for(int poseIndex=0;poseIndex<2;poseIndex++) {
                    root.getAllParts().forEach(net.minecraft.client.model.geom.ModelPart::resetPose);
                    model.young=false;
                    var body=root.getChild("body");body.xRot=(float)Math.PI/2;
                    if(poseIndex==1) {
                        // Same offsets as vanilla CatModel's seated preparation.
                        body.xRot=.7853982F;body.y-=4;body.z+=5;
                        var head=root.getChild("head");head.y-=3.3F;head.z+=1;head.yRot=.35F;
                        var liveYaw=HissingCatModel.class.getDeclaredField("liveHeadYRot");
                        liveYaw.setAccessible(true);liveYaw.setFloat(model,.35F);
                        var tail1=root.getChild("tail1");tail1.y+=8;tail1.z-=2;tail1.xRot=1.7278761F;
                        var tail2=root.getChild("tail2");tail2.y+=2;tail2.z-=.8F;tail2.xRot=2.670354F;
                        for(var name:new String[]{"left_front_leg","right_front_leg"}) {
                            var leg=root.getChild(name);leg.xRot=-.15707964F;leg.y=16.1F;leg.z=-7;
                        }
                        for(var name:new String[]{"left_hind_leg","right_hind_leg"}) {
                            var leg=root.getChild(name);leg.xRot=-(float)Math.PI/2;leg.y=21;leg.z=1;
                        }
                    } else {
                        var liveYaw=HissingCatModel.class.getDeclaredField("liveHeadYRot");
                        liveYaw.setAccessible(true);liveYaw.setFloat(model,0);
                    }
                    var head=root.getChild("head");var lf=root.getChild("left_front_leg");var rf=root.getChild("right_front_leg");
                    var lh=root.getChild("left_hind_leg");var rh=root.getChild("right_hind_leg");
                    var tail=root.getChild("tail1");var tip=root.getChild("tail2");
                    if(outfit==CatOutfitType.MEDICAL) {
                        if(poseIndex==0)CatMedicalAnimation.apply(40,head,body,lh,rh,lf,rf,tail,tip);
                        else CatMedicalAnimation.applyStationed(40,head,body,lh,rh,lf,rf,tail,tip);
                        if(Math.abs(body.z-(poseIndex==0?-10:-5))>1e-5||Math.abs(body.y-(poseIndex==0?12:8))>1e-5)
                            throw new AssertionError("Medical torso leaves its standing/cushion-specific origin");
                        if(poseIndex==1&&Math.abs(body.xRot-(float)Math.PI/4)>1e-5)
                            throw new AssertionError("Clinic must retain the cushion's seated torso angle");
                    }
                    if((outfit==CatOutfitType.DIVING||outfit==CatOutfitType.FLIGHT)&&poseIndex==1) {
                        CatRideAnimation.apply(40,head,body,lh,rh,lf,rf,tail,tip);
                        if(lf.xRot>-1.5||rf.xRot>-1.5||lh.xRot<1.5||rh.xRot<1.5)
                            throw new AssertionError("Streamlined limbs do not extend in opposite directions");
                    }
                    if(outfit==CatOutfitType.COCKROACH){
                        int mode=poseIndex==0?1:2;
                        var modeField=HissingCatModel.class.getDeclaredField("cockroachMode");modeField.setAccessible(true);modeField.setInt(model,mode);
                        var wingWeight=HissingCatModel.class.getDeclaredField("cockroachWingWeight");wingWeight.setAccessible(true);wingWeight.setFloat(model,1);
                        var ageField=HissingCatModel.class.getDeclaredField("cockroachAge");ageField.setAccessible(true);ageField.setFloat(model,5);
                        float oldFront=lf.xRot;
                        if(mode==2){body.resetPose();body.xRot=(float)Math.PI/2;head.resetPose();CatCockroachAnimation.dash(5,lh,rh,lf,rf);}
                        var transforms=model.catOutfitTransforms(outfit);
                        if(!transforms.containsKey("group10")||!transforms.containsKey("group11"))
                            throw new AssertionError("Both original wing tracks must be present");
                        if(mode==1&&lf.xRot!=oldFront)throw new AssertionError("Hurt wings cannot move limbs");
                        if(mode==2&&(lf.xRot>-1.5||lh.xRot<1.5))throw new AssertionError("Full original dash limb tracks missing");
                    }
                    if(outfit==CatOutfitType.AGENT&&poseIndex==1)
                        CatAgentAttackAnimation.apply(agentMove,.36F,head,body,lh,rh,lf,rf,tail,tip);
                    if(poseIndex==1&&(outfit==CatOutfitType.DIVING||outfit==CatOutfitType.FLIGHT||outfit==CatOutfitType.COCKROACH))
                        checkOriginalAnchors(head,lf,rf,outfit==CatOutfitType.COCKROACH?3:0);
                    if(outfit==CatOutfitType.AGENT||outfit==CatOutfitType.DIVING||outfit==CatOutfitType.COCKROACH)
                        verifyPivot(model,body,outfit);
                    var pose=new PoseStack();pose.translate(poseIndex==0?-2:2,1.3,0);
                    pose.scale(1.8F,-1.8F,-1.8F);
                    pose.mulPose(new Quaternionf().rotationX(-.2F).rotateY(.65F));
                    var buffers=mc.renderBuffers().bufferSource();
                    model.renderToBuffer(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(skin)),
                            LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,1,1,1,1);
                    var runtime=RuntimeBlockbenchModel.getCatOutfit(def.model());
                    runtime.renderTexture(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(def.texture())),
                            LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,RuntimeBlockbenchModel.GroupSelection.ALL_GROUPS,
                            RuntimeBlockbenchModel.HeadMotion.NONE,model.catOutfitTransforms(outfit),1);
                    if(outfit==CatOutfitType.DIVING&&poseIndex==1){
                        var player=new net.minecraft.client.model.HumanoidModel<net.minecraft.world.entity.LivingEntity>(
                                net.minecraft.client.model.geom.builders.LayerDefinition.create(
                                net.minecraft.client.model.HumanoidModel.createMesh(net.minecraft.client.model.geom.builders.CubeDeformation.NONE,0),64,64).bakeRoot());
                        player.young=false;
                        player.leftArm.xRot=player.rightArm.xRot=-(float)Math.PI/5;
                        player.leftLeg.xRot=player.rightLeg.xRot=-1.4137167F;
                        player.rightLeg.yRot=(float)Math.PI/10;player.leftLeg.yRot=-(float)Math.PI/10;
                        player.rightLeg.zRot=.07853982F;player.leftLeg.zRot=-.07853982F;
                        pose.pushPose();pose.translate(0,-cn.laowu.mod.entity.CatDivingCarrier.RIDER_HEIGHT,0);
                        player.renderToBuffer(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(
                                ResourceLocation.fromNamespaceAndPath("minecraft","textures/entity/player/wide/steve.png"))),
                                LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,1,1,1,1);
                        pose.popPose();
                    }
                    buffers.endBatch();
                    count++;
                }
                try(var pixels=new NativeImage(768,384,false)) {
                    RenderSystem.bindTexture(output.getColorTextureId());pixels.downloadTexture(0,false);
                    for(int x=0;x<768;x++)for(int y=0;y<384;y++) {
                        int c=pixels.getPixelRGBA(x,y),r=c&255,g=(c>>>8)&255,b=(c>>>16)&255;
                        if(Math.abs(r-31)+Math.abs(g-38)+Math.abs(b-48)>35)vertices[x<384?0:1]++;
                    }
                    if(vertices[0]<1000||vertices[1]<1000)throw new AssertionError("Missing model pixels "+outfit+" "+Arrays.toString(vertices));
                    pixels.flipY();pixels.writeToFile(java.nio.file.Path.of("specialist-"+outfit.id()+(outfit==CatOutfitType.AGENT?"-"+agentMove:"")+".png"));
                }
                if(outfit==CatOutfitType.AGENT)agentMove++;
            }
            verifyAnimationFrames(mc);
            verifySuitTooltips();
            System.out.println("PASS: "+count+" real GPU career renders: clinic anchored pose, split wings/dash, pilot/diver ride limbs, original body pivots");
        } catch(Exception e) {throw new IllegalStateException("Specialist visual probe failed",e);}
        finally {
            if(output!=null)output.destroyBuffers();
            RenderSystem.restoreProjectionMatrix();view.popPose();RenderSystem.applyModelViewMatrix();
            RenderSystem.clearColor(clear[0],clear[1],clear[2],clear[3]);RenderSystem.clearDepth(clearDepth);
        }
    }
    private static void verifySuitTooltips() {
        int checked=0;
        for(var outfit:CatOutfitType.values()) {
            if(outfit==CatOutfitType.NONE)continue;
            var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath("laowu",outfit.id()+"_suit"));
            var stack=new net.minecraft.world.item.ItemStack(item);
            for(int mode=0;mode<3;mode++) {
                var lines=new java.util.ArrayList<net.minecraft.network.chat.Component>();
                lines.add(stack.getHoverName());
                var event=new net.minecraftforge.event.entity.player.ItemTooltipEvent(
                        stack,null,lines,net.minecraft.world.item.TooltipFlag.Default.NORMAL);
                CareerSuitTooltip.modify(event,item,outfit,mode==2,mode==1);
                String text=lines.stream().map(net.minecraft.network.chat.Component::getString).collect(java.util.stream.Collectors.joining(""));
                if(text.contains(".tooltip.")||text.contains("item.laowu.")||text.contains("screen.laowu."))
                    throw new AssertionError("Untranslated suit tooltip "+outfit+" mode "+mode+": "+text);
                if(mode==0) {
                    String combat=net.minecraft.network.chat.Component.translatable("item.laowu.career_suit.combat","").getString().trim();
                    String work=net.minecraft.network.chat.Component.translatable("item.laowu.career_suit.work","").getString().trim();
                    if(!text.contains(combat)||!text.contains(work)||!text.contains("Ctrl")||!text.contains("Shift"))
                        throw new AssertionError("Default suit sections missing "+outfit+": "+text);
                    if(text.contains("0.08")||text.contains("round(")||text.contains("max(")||text.contains("128 SU"))
                        throw new AssertionError("Formula leaked into default tooltip "+outfit);
                } else if(lines.size()<5)throw new AssertionError("Empty detailed suit tooltip "+outfit);
                if(mode==0) {
                    String role=net.minecraft.network.chat.Component.translatable("item.laowu.career_suit.role",
                            net.minecraft.network.chat.Component.translatable("item.laowu.career_suit.role."+outfit.role().name().toLowerCase(java.util.Locale.ROOT))).getString();
                    if(!lines.get(1).getString().equals(role))throw new AssertionError("Role must be the first suit description: "+outfit);
                }
                if(mode==1&&!outfit.isSupport()){
                    String damage=net.minecraft.network.chat.Component.translatable("item.laowu.career_suit.damage_detail","").getString().trim();
                    if(!text.contains(damage))throw new AssertionError("Attack damage subtitle missing: "+outfit);
                }
                if((outfit==CatOutfitType.FLIGHT||outfit==CatOutfitType.DIVING)&&(text.contains("旧背包")||text.contains("100封顶")||text.contains("legacy backpack")||text.contains("100-stat cap")))
                    throw new AssertionError("Removed legacy/cap copy remains in suit tooltip");
                checked++;
            }
        }
        System.out.println("PASS: "+checked+" real localized suit tooltip builds, default/Ctrl/Shift with no missing keys or default formulas");
    }
    /** User requested restoring accessories.22 height, not the lowered/splayed .23 pose. */
    private static void checkOriginalAnchors(net.minecraft.client.model.geom.ModelPart head,
                                            net.minecraft.client.model.geom.ModelPart lf,
                                            net.minecraft.client.model.geom.ModelPart rf) {
        checkOriginalAnchors(head,lf,rf,0);
    }
    private static void checkOriginalAnchors(net.minecraft.client.model.geom.ModelPart head,
                                            net.minecraft.client.model.geom.ModelPart lf,
                                            net.minecraft.client.model.geom.ModelPart rf,float originalOffset) {
        for(var leg:new net.minecraft.client.model.geom.ModelPart[]{lf,rf}) {
            if(Math.abs(leg.y-leg.getInitialPose().y-originalOffset)>.001 || Math.abs(leg.x-leg.getInitialPose().x)>.001
                    ||Math.abs(leg.z-leg.getInitialPose().z)>.001||Math.abs(leg.yRot)>.001)
                throw new AssertionError("Original forelimb height/anchor was not restored");
        }
    }
    private static void verifyAnimationFrames(Minecraft mc) throws Exception {
        var root=HissingCatModel.createLayer().bakeRoot();
        var head=root.getChild("head");var body=root.getChild("body");var lf=root.getChild("left_front_leg");var rf=root.getChild("right_front_leg");
        var lh=root.getChild("left_hind_leg");var rh=root.getChild("right_hind_leg");var tail=root.getChild("tail1");var tip=root.getChild("tail2");
        for(int i=0;i<=720;i++) {
            float yaw=i*(float)Math.PI/180;
            var q=new Quaternionf().rotationYXZ(yaw,-.18F,.22F).rotateX((float)Math.PI/4);
            var e=CatAgentAttackAnimation.eulerZYX(new Quaternionf(q));
            var recovered=new Quaternionf().rotationZYX(e.z,e.y,e.x);
            if(Math.abs(q.dot(recovered))<.99999F)throw new AssertionError("Rigid cat rotation lost at yaw "+yaw);
        }
        for(int frame=0;frame<400;frame++){
            CatRideAnimation.apply(frame,head,body,lh,rh,lf,rf,tail,tip);
            checkOriginalAnchors(head,lf,rf);
        }
        for(int move=0;move<3;move++)for(int frame=0;frame<=100;frame++){
            root.getAllParts().forEach(net.minecraft.client.model.geom.ModelPart::resetPose);
            body.xRot=(float)Math.PI/2;
            CatAgentAttackAnimation.apply(move,frame/100F,head,body,lh,rh,lf,rf,tail,tip);
            if(move<2 && frame==36 && (Math.abs(body.y-12)>.02 || Math.abs(body.xRot-(float)Math.PI/2)>.02
                    || lh.y<17.5 || lh.z<4 || Math.abs(lh.xRot)>1 || lf.x<=2 || rf.x>=-2))
                throw new AssertionError("Agent punches must use spread standing limbs, not seated hips");
            for(var part:root.getAllParts().toList())
                if(!Float.isFinite(part.x+part.y+part.z+part.xRot+part.yRot+part.zRot))
                    throw new AssertionError("Invalid agent pose "+move+" frame "+frame);
            if((frame==0||frame==100)&&(Math.abs(body.y-12)>.001||Math.abs(body.xRot-(float)Math.PI/2)>.001))
                throw new AssertionError("Attack must blend back to locomotion");
        }
        System.out.println("PASS: 400 original-height breathing frames; 303 finite agent frames and idle recovery");
    }
    private static void verifyPivot(HissingCatModel model,net.minecraft.client.model.geom.ModelPart body,CatOutfitType outfit) {
        var pivot=outfit==CatOutfitType.DIVING?new Vector3f(2.25F,17.5F,1.25F):new Vector3f(0,18.6F,-9.5F);
        var expected=new Vector3f(pivot).sub(0,12,-10)
                .rotate(new Quaternionf().rotationX(-(float)Math.PI/2))
                .rotate(new Quaternionf().rotationZYX(body.zRot,body.yRot,body.xRot))
                .add(body.x,body.y,body.z);
        var t=model.catOutfitTransforms(outfit).get("group");
        var actual=new Vector3f(pivot).add(t.x(),-t.y(),t.z());
        if(actual.distance(expected)>1e-4)throw new AssertionError("Outfit root lost body pivot "+outfit+": "+actual+" vs "+expected);
    }
}
