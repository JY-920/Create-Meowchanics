package cn.laowu.mod.client;

import cn.laowu.mod.*;
import cn.laowu.mod.entity.*;
import cn.laowu.mod.genetics.*;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.render.PlayerSkyhookRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;
import java.util.*;

/** Exercises the registered cat model and transformed Create player-pose hook. */
public final class RidePresentationProbe {
    public static void verify(Minecraft mc) throws Exception {
        var level=new AgentWatchVisualProbe.ProbeLevel();level.clock=1000;
        var plain=cat(level,CatOutfitType.FLIGHT);
        var pilot=cat(level,CatOutfitType.FLIGHT);
        var carrier=new CatFlightCarrier(LaoWuMod.CAT_FLIGHT_CARRIER.get(),level);
        check(pilot.startRiding(carrier,true),"Pilot fixture mounted");
        var ordinary=HissingCatModel.createLayer().bakeRoot();
        var mounted=HissingCatModel.createLayer().bakeRoot();
        var normalModel=new HissingCatModel(ordinary);var pilotModel=new HissingCatModel(mounted);
        frame(pilotModel,pilot,0);level.clock+=12;
        frame(normalModel,plain,0);frame(pilotModel,pilot,0);
        for(var limb:List.of("left_front_leg","right_front_leg","left_hind_leg","right_hind_leg")) {
            check(same(ordinary.getChild(limb),mounted.getChild(limb)),"Flying pilot preserves normal limb pose: "+limb);
        }
        var endpoints=new ArrayList<Vector3f>();
        for(int age:new int[]{0,1,2,3,4}){
            frame(pilotModel,pilot,age);
            var tail=mounted.getChild("tail1");var tip=mounted.getChild("tail2");
            var joint=point(tail,0,8,0);
            check(joint.y<tail.y-7.9F&&Math.abs(joint.x-tail.x)<.03F&&Math.abs(joint.z-tail.z)<.03F,
                    "Pilot tail base points straight upward like a rotor mast");
            check(point(tail,0,8,0).distance(new Vector3f(tip.x,tip.y,tip.z))<.03F,"Tail tip stays attached to base");
            var end=point(tip,0,8,0);
            check(Math.abs(end.y-joint.y)<.03F,"Tail tip spins in a horizontal rotor plane");
            endpoints.add(end);
        }
        check(endpoints.get(0).distance(endpoints.get(4))<.03F,"Tail circle loops continuously");
        check(endpoints.get(0).distance(endpoints.get(2))>15.5&&endpoints.get(1).distance(endpoints.get(3))>15.5,
                "Full-length rotor makes a fast complete revolution in four ticks");
        for(float blend:new float[]{.001F,.25F,.5F,.75F,.999F})for(float boundary:new float[]{2,4,6})
            check(rotorPoint(ordinary,boundary-.0001F,blend,8).distance(rotorPoint(ordinary,boundary+.0001F,blend,8))<.02F,
                    "Spinning tail must not jump at half/full-turn phase boundaries while blending");
        for(float length:new float[]{0,8})check(rotorPoint(ordinary,2,0,length).distance(rotorPoint(ordinary,2,.000001F,length))<.01F,
                "Zero-weight rotor boundary must not snap the ordinary tail anchor");
        var entering=cat(level,CatOutfitType.FLIGHT);
        var enteringCarrier=new CatFlightCarrier(LaoWuMod.CAT_FLIGHT_CARRIER.get(),level);
        entering.startRiding(enteringCarrier,true);
        var enteringRoot=HissingCatModel.createLayer().bakeRoot();var enteringModel=new HissingCatModel(enteringRoot);
        frame(enteringModel,entering,0);
        for(int age=1;age<=8;age++){
            level.clock++;frame(enteringModel,entering,age);
            var tail=enteringRoot.getChild("tail1");var tip=enteringRoot.getChild("tail2");
            check(point(tail,0,8,0).distance(new Vector3f(tip.x,tip.y,tip.z))<.28F,
                    "Tail joint remains connected while blending into rotor pose");
        }
        var diving=cat(level,CatOutfitType.DIVING);
        var diver=new CatDivingCarrier(LaoWuMod.CAT_DIVING_CARRIER.get(),level);
        var player=new Player(level,BlockPos.ZERO,0,new GameProfile(UUID.randomUUID(),"dive-visual")){
            public boolean isSpectator(){return false;}public boolean isCreative(){return false;}
        };
        diving.setOwnerUUID(player.getUUID());
        check(diving.startRiding(diver,true)&&player.startRiding(diver,true),"Diver fixture with real passengers");
        var swim=CatDivingCarrier.class.getDeclaredField("SWIMMING");swim.setAccessible(true);
        diver.getEntityData().set((EntityDataAccessor<Boolean>)swim.get(null),true);
        settle(diver,1);
        diver.positionRider(diving);diver.positionRider(player);
        check(player.getZ()<diving.getZ()-.9,"Water rider is behind the cat instead of sitting on its back");
        var root=HissingCatModel.createLayer().bakeRoot();var model=new HissingCatModel(root);
        frame(model,diving,20);level.clock+=12;frame(model,diving,20);
        for(String name:List.of("left_front_leg","right_front_leg")) {
            var leg=root.getChild(name);
            check(Math.abs(leg.x)<=3F,"Diver shoulder must still touch the torso, not float beside it");
            check(Math.abs(point(leg,0,9,0).x)>4&&leg.xRot> -1.4F,"Diver paws fan out beyond the head without lowered shoulders");
        }
        var human=new HumanoidModel<>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE,0),64,64).bakeRoot());
        PlayerSkyhookRenderer.afterSetupAnim(player,human);
        check(human.body.xRot>1.4&&human.leftArm.xRot< -1.6&&human.rightArm.xRot< -1.6,
                "Real Create hook gives horizontal body and two forward-reaching hands");
        check(Math.abs(human.leftArm.yRot+human.rightArm.yRot)<.01,"Hands grip symmetrically");
        for(float yaw:new float[]{-180,-90,90,179})for(float oldBody:new float[]{0,45}){
            diver.setYRot(yaw);diver.yRotO=yaw;
            player.yBodyRot=player.yBodyRotO=oldBody;
            var turning=new HumanoidModel<>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE,0),64,64).bakeRoot());
            PlayerSkyhookRenderer.afterSetupAnim(player,turning);
            var direction=point(turning.body,0,12,0).sub(point(turning.body,0,0,0)).normalize();
            new org.joml.Matrix4f().rotationY((float)Math.toRadians(180-oldBody)).scale(-1,-1,1).transformDirection(direction);
            var back=net.minecraft.world.phys.Vec3.directionFromRotation(0,yaw).scale(-1);
            check(direction.dot(new Vector3f((float)back.x,(float)back.y,(float)back.z))>.999,
                    "Prone player and cat keep the same heading during a turn: "+yaw+" / "+oldBody);
            var actualRoot=new org.joml.Matrix4f().rotationY((float)Math.toRadians(180-oldBody)).scale(-1,-1,1);
            var canonicalRoot=new org.joml.Matrix4f().rotationY((float)Math.toRadians(180-yaw)).scale(-1,-1,1);
            var actualParts=new ModelPart[]{turning.head,turning.leftArm,turning.rightArm};
            var canonicalParts=new ModelPart[]{human.head,human.leftArm,human.rightArm};
            for(int i=0;i<3;i++){
                var actual=actualRoot.transformPosition(point(actualParts[i],0,i==0?0:10,0));
                var expected=canonicalRoot.transformPosition(point(canonicalParts[i],0,i==0?0:10,0));
                check(actual.distance(expected)<.03,"Head and both hand pivots turn together with the carrier");
            }
        }
        var land=new HumanoidModel<>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE,0),64,64).bakeRoot());
        diver.getEntityData().set((EntityDataAccessor<Boolean>)swim.get(null),false);
        settle(diver,0);
        PlayerSkyhookRenderer.afterSetupAnim(player,land);
        check(Math.abs(land.body.xRot)<.01,"Dry land receives no underwater player pose");
        pilot.stopRiding();level.clock+=12;frame(pilotModel,pilot,30);level.clock+=12;frame(pilotModel,pilot,30);
        frame(normalModel,plain,30);
        check(same(mounted.getChild("tail2"),ordinary.getChild("tail2")),"Dismount blends back to ordinary tail pose");
        check(same(mounted.getChild("tail1"),ordinary.getChild("tail1")),"Dismount also restores the raised tail base");
        System.out.println("PASS: distinct pilot/diver poses, attached circular tail, actual two-hand player hook, land and dismount recovery");
    }
    private static Cat cat(AgentWatchVisualProbe.ProbeLevel level,CatOutfitType outfit){
        var cat=new Cat(EntityType.CAT,level);cat.setTame(true,true);cat.setAge(0);
        CatTraitData.set(cat,CatTraitProfile.EMPTY);
        cat.getPersistentData().putBoolean(CatClothesData.EQUIPPED_TAG,true);
        cat.getPersistentData().putString(CatClothesData.OUTFIT_TAG,outfit.id());return cat;
    }
    private static void settle(CatDivingCarrier diver,float value) throws Exception {
        // No chunk simulation in the renderer fixture. The server GameTest exercises actual ticks.
        for(String name:List.of("swimPose","swimPoseOld"))try {
            var field=CatDivingCarrier.class.getDeclaredField(name);field.setAccessible(true);field.setFloat(diver,value);
        }catch(NoSuchFieldException oldImplementation) { /* Old renderer has no transition state. */ }
    }
    private static void frame(HissingCatModel model,Cat cat,int age){
        cat.tickCount=age;model.prepareMobModel(cat,0,0,0);model.setupAnim(cat,0,0,age,0,0);
    }
    private static Vector3f point(ModelPart part,float x,float y,float z){
        var pose=new PoseStack();part.translateAndRotate(pose);
        return pose.last().pose().transformPosition(new Vector3f(x/16,y/16,z/16)).mul(16);
    }
    private static Vector3f rotorPoint(ModelPart normal,float age,float blend,float length){
        var root=HissingCatModel.createLayer().bakeRoot();
        var tail=root.getChild("tail1");var tip=root.getChild("tail2");
        tail.copyFrom(normal.getChild("tail1"));tip.copyFrom(normal.getChild("tail2"));
        CatRideAnimation.pilot(age,blend,tail,tip);
        return point(tip,0,length,0);
    }
    private static boolean same(ModelPart a,ModelPart b){
        return Math.abs(a.x-b.x)+Math.abs(a.y-b.y)+Math.abs(a.z-b.z)+Math.abs(a.xRot-b.xRot)
                +Math.abs(a.yRot-b.yRot)+Math.abs(a.zRot-b.zRot)<.001;
    }
    private static void check(boolean okay,String message){if(!okay)throw new AssertionError(message);}
}
