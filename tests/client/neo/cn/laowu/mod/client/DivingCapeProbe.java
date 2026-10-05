package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.entity.CatDivingCarrier;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.*;
import org.joml.Vector3f;
import java.util.*;

/** Real CapeLayer vertices, with only the network-bound player/skin replaced by a test fixture. */
public final class DivingCapeProbe {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("minecraft","textures/entity/steve.png");
    public static void verify(Minecraft mc) throws Exception {
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        var unsafe=(sun.misc.Unsafe)field.get(null);
        var player=(CapePlayer)unsafe.allocateInstance(CapePlayer.class);
        var position=Entity.class.getDeclaredField("position");position.setAccessible(true);
        position.set(player,net.minecraft.world.phys.Vec3.ZERO);
        var level=new AgentWatchVisualProbe.ProbeLevel();
        var carrier=new CatDivingCarrier(LaoWuMod.CAT_DIVING_CARRIER.get(),level);
        var pose=CatDivingCarrier.class.getDeclaredField("swimPose");pose.setAccessible(true);
        var old=CatDivingCarrier.class.getDeclaredField("swimPoseOld");old.setAccessible(true);
        var root=LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE,false),64,64).bakeRoot();
        var model=new PlayerModel<AbstractClientPlayer>(root,false);
        var layer=new CapeLayer(new RenderLayerParent<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>>() {
            public PlayerModel<AbstractClientPlayer> getModel(){return model;}
            public ResourceLocation getTextureLocation(AbstractClientPlayer entity){return TEXTURE;}
        });
        var baseline=vertices(layer,player);
        check(baseline.size()==24,"Actual vanilla cape emitted its cube vertices");
        for(boolean crouch:new boolean[]{false,true})
        for(float amount:new float[]{0,.5F,1})for(float yaw:new float[]{0,90,-90,135}) {
            root.getAllParts().forEach(p->p.resetPose());
            // Vanilla crouching has already moved the body and the independent cloak before our hook.
            player.vehicle=null;player.crouched=crouch;
            if(crouch){model.body.y=3.2F;model.body.xRot=.5F;root.getChild("cloak").y=1.85F;root.getChild("cloak").z=1.4F;}
            var source=vertices(layer,player);
            pose.setFloat(carrier,amount);old.setFloat(carrier,amount);player.vehicle=carrier;
            CatDivingRiderPose.apply(model,0,amount,yaw);
            var actual=vertices(layer,player);
            float initialPitch=crouch?.5F:0,initialY=crouch?3.2F/16:0;
            float pitch=initialPitch+((float)Math.PI*.5F-initialPitch)*amount,turn=(float)Math.toRadians(yaw)*amount;
            for(int i=0;i<baseline.size();i++) {
                var v=source.get(i);
                float localY=(float)((v.y-initialY)*Math.cos(initialPitch)+v.z*Math.sin(initialPitch));
                float localZ=(float)(-(v.y-initialY)*Math.sin(initialPitch)+v.z*Math.cos(initialPitch));
                float y=(float)(localY*Math.cos(pitch)-localZ*Math.sin(pitch));
                float z=(float)(localY*Math.sin(pitch)+localZ*Math.cos(pitch));
                var expected=new Vector3f((float)(v.x*Math.cos(turn)+z*Math.sin(turn)),
                        initialY+(17F/16-initialY)*amount+y,(float)(-v.x*Math.sin(turn)+z*Math.cos(turn)));
                check(actual.get(i).distance(expected)<.0001F,
                        "Cape follows prone torso/heading at blend "+amount+", yaw "+yaw+": "+actual.get(i)+" != "+expected);
            }
        }
        player.vehicle=null;player.crouched=false;root.getChild("cloak").resetPose();
        var normal=vertices(layer,player);
        for(int i=0;i<baseline.size();i++)
            check(normal.get(i).distance(baseline.get(i))<.0001F,"No cape transform leaks to ordinary players");
        player.hidden=true;check(vertices(layer,player).isEmpty(),"Invisible cape remains hidden");
        player.hidden=false;player.elytra=true;check(vertices(layer,player).isEmpty(),"Elytra still suppresses cape");
        System.out.println("PASS: real CapeLayer follows prone body and headings, blends, preserves invisibility/elytra and restores render stack");
    }
    private static List<Vector3f> vertices(CapeLayer layer,CapePlayer player) {
        var points=new ArrayList<Vector3f>();
        var consumer=(VertexConsumer)java.lang.reflect.Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(),
                new Class<?>[]{VertexConsumer.class},(proxy,method,args)->{
                    if((method.getName().equals("vertex")||method.getName().equals("addVertex"))
                            &&args.length==3&&args[0] instanceof Number)
                        points.add(new Vector3f(((Number)args[0]).floatValue(),((Number)args[1]).floatValue(),((Number)args[2]).floatValue()));
                    else if(method.isDefault())return java.lang.reflect.InvocationHandler.invokeDefault(proxy,method,args);
                    return method.getReturnType()==void.class?null:proxy;
                });
        var stack=new PoseStack();
        layer.render(stack,type->consumer,15728880,player,0,0,1,0,0,0);
        check(stack.clear(),"Cape rendering restores the caller's pose stack");
        check(stack.last().pose().equals(new org.joml.Matrix4f()),"Cape rendering does not leak a transform");
        return points;
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static final class CapePlayer extends AbstractClientPlayer {
        private Entity vehicle;private boolean hidden,elytra,crouched;
        private CapePlayer(){super(null,null);}
        public boolean isInvisible(){return hidden;}
        public boolean isCrouching(){return crouched;}
        public boolean isModelPartShown(PlayerModelPart part){return true;}
        public Entity getVehicle(){return vehicle;}
        public ItemStack getItemBySlot(EquipmentSlot slot){return elytra?new ItemStack(Items.ELYTRA):ItemStack.EMPTY;}
        public net.minecraft.client.resources.PlayerSkin getSkin() {
            return new net.minecraft.client.resources.PlayerSkin(TEXTURE,null,TEXTURE,null,
                    net.minecraft.client.resources.PlayerSkin.Model.WIDE,true);
        }
    }
}
