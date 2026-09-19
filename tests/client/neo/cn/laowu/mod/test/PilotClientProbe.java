package cn.laowu.mod.test;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid="laowu",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)

public final class PilotClientProbe {
    private static boolean verified;
    @EventBusSubscriber(modid="laowu",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.GAME)
    public static final class Finish {
        @SubscribeEvent
        public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
            var mc=Minecraft.getInstance();
            // Wait for resource loading to finish before closing its texture manager.
            if(verified&&mc.getOverlay()==null&&mc.screen!=null) {
                verified=false;
                verifyWrenchContact(mc);
                cn.laowu.mod.client.MedicalVisualProbe.verify(mc);
                cn.laowu.mod.client.SpecialistVisualProbe.verify(mc);
                cn.laowu.mod.client.AgentWatchVisualProbe.verify(mc);
                cn.laowu.mod.client.CareerFeedbackVisualProbe.verify(mc);
                cn.laowu.mod.client.CareerAccessoryVisualProbe.verify(mc);
                cn.laowu.mod.client.SupportPresentationProbe.verify(mc);
                cn.laowu.mod.client.WishAdoptionVisualProbe.verify(mc);
                mc.stop();
            }
        }
    }
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(()->{
            Minecraft mc=Minecraft.getInstance();
            org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
            try {
                // Loading the real target runs Mixin's shadow / injection validation.
                Class<?> target=Class.forName("com.simibubi.create.foundation.render.PlayerSkyhookRenderer");
                if(java.util.Arrays.stream(target.getDeclaredMethods()).noneMatch(m->m.getName().contains("laowu$pilotPose")))
                    throw new AssertionError("Pilot pose injection missing from transformed Create class");
                var method=target.getDeclaredMethod("setHangingPose",boolean.class,HumanoidModel.class);
                method.setAccessible(true);
                for(boolean left:new boolean[]{false,true}) {
                    var mesh=HumanoidModel.createMesh(net.minecraft.client.model.geom.builders.CubeDeformation.NONE,0);
                    var model=new HumanoidModel<net.minecraft.world.entity.LivingEntity>(
                            net.minecraft.client.model.geom.builders.LayerDefinition.create(mesh,64,64).bakeRoot());
                    method.invoke(null,left,model);
                    var held=left?model.leftArm:model.rightArm;
                    if(!Float.isFinite(held.xRot)||Math.abs(held.xRot)<.5)
                        throw new AssertionError("Original Create hanging pose failed for hand="+left);
                }
                var harness=cn.laowu.mod.client.CatPilotHarnessLayer.createRoot();
                if(!harness.hasChild("body_band")||harness.hasChild("hanger")||harness.hasChild("grip"))
                    throw new AssertionError("Only the torso band may remain; remove the lower ring and connector");
                int[] cubes={0};
                harness.visit(new com.mojang.blaze3d.vertex.PoseStack(), (pose,path,index,cube)->{
                    if(!Float.isFinite(cube.minY)||cube.minY<13||cube.maxY>24.125F)
                        throw new AssertionError("Single band must end at Y24.125, with no low dangling ring");
                    cubes[0]++;
                });
                if(cubes[0]!=4) throw new AssertionError("Single torso band requires exactly four connected bars");
                verifyCenteredHarness(harness);
                System.out.println("PASS: client: single torso band, three-pixel belly clearance, no lower ring or connector");
                verified=true;
            } catch(Throwable error) {
                System.err.println("FAIL: pilot client pose smoke test");
                throw new IllegalStateException(error);
            }
        });
    }
    private static void verifyCenteredHarness(net.minecraft.client.model.geom.ModelPart harness) {
        var catRoot=cn.laowu.mod.client.HissingCatModel.createLayer().bakeRoot();
        var model=new cn.laowu.mod.client.HissingCatModel(catRoot);
        var body=catRoot.getChild("body");
        var bodyCubes=new java.util.ArrayList<net.minecraft.client.model.geom.ModelPart.Cube>();
        body.visit(new com.mojang.blaze3d.vertex.PoseStack(),(pose,path,index,cube)->bodyCubes.add(cube));
        var cube=bodyCubes.get(0);
        var standing=new com.mojang.blaze3d.vertex.PoseStack();
        body.translateAndRotate(standing);
        var torsoCenter=standing.last().pose().transformPosition(new org.joml.Vector3f(
                (cube.minX+cube.maxX)/32,(cube.minY+cube.maxY)/32,(cube.minZ+cube.maxZ)/32));
        var centers=new java.util.HashMap<String,org.joml.Vector3f>();
        for(String part:new String[]{"body_band"}) {
            float[] bounds={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,
                    Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
            harness.getChild(part).visit(new com.mojang.blaze3d.vertex.PoseStack(),(pose,path,index,c)->{
                bounds[0]=Math.min(bounds[0],c.minX);bounds[1]=Math.min(bounds[1],c.minY);bounds[2]=Math.min(bounds[2],c.minZ);
                bounds[3]=Math.max(bounds[3],c.maxX);bounds[4]=Math.max(bounds[4],c.maxY);bounds[5]=Math.max(bounds[5],c.maxZ);
            });
            var center=new org.joml.Vector3f((bounds[0]+bounds[3])/32,(bounds[1]+bounds[4])/32,(bounds[2]+bounds[5])/32);
            if(Math.abs(center.x-torsoCenter.x)>1e-5||Math.abs(center.z-torsoCenter.z)>1e-5)
                throw new AssertionError(part+" is not directly below actual vanilla torso: "+center+" vs "+torsoCenter);
            centers.put(part,center);
        }
        verifyWrappedTorso(harness,standing.last().pose(),cube,torsoCenter);
        var band=cubes(harness.getChild("body_band"));
        centers.put("back_anchor",cubeCenter(band.get(0)));
        centers.put("grip_anchor",cubeCenter(band.get(3)));
        var standingInverse=new org.joml.Matrix4f(standing.last().pose()).invert();
        int checks=0;
        for(int yaw=-180;yaw<=180;yaw+=15) for(float scale:new float[]{.8F,1,2})
                for(boolean angled:new boolean[]{false,true}) {
            body.resetPose();
            if(angled) { body.x+=2;body.y-=4;body.z+=5;body.xRot-=.4F;body.yRot=.2F;body.zRot=-.1F; }
            body.xScale=1.1F;body.yScale=.9F;body.zScale=1.2F;
            var actual=new com.mojang.blaze3d.vertex.PoseStack();
            actual.mulPose(new org.joml.Quaternionf().rotationY((float)Math.toRadians(yaw)));
            actual.scale(scale,scale,scale);
            var expected=new com.mojang.blaze3d.vertex.PoseStack();
            expected.mulPose(new org.joml.Quaternionf().rotationY((float)Math.toRadians(yaw)));
            expected.scale(scale,scale,scale);
            model.applyBodyPoseDelta(actual);
            body.translateAndRotate(expected);
            for(var center:centers.values()) {
                var local=standingInverse.transformPosition(new org.joml.Vector3f(center));
                var a=actual.last().pose().transformPosition(new org.joml.Vector3f(center));
                var b=expected.last().pose().transformPosition(local);
                if(a.distance(b)>1e-5) throw new AssertionError("Harness detached from torso for yaw="+yaw+" / scale="+scale);
                checks++;
            }
        }
        System.out.println("PASS: "+checks+" real ModelPart torso-centered attachment checks");
    }

    private static void verifyWrappedTorso(net.minecraft.client.model.geom.ModelPart harness,
            org.joml.Matrix4f standing,net.minecraft.client.model.geom.ModelPart.Cube body,org.joml.Vector3f center) {
        // Compute the ACTUAL rotated cat silhouette rather than checking
        // arbitrary harness bounds (which previously allowed both rings below).
        float[] bounds={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
        for(float x:new float[]{body.minX,body.maxX}) for(float y:new float[]{body.minY,body.maxY})
                for(float z:new float[]{body.minZ,body.maxZ}) {
            var v=standing.transformPosition(new org.joml.Vector3f(x/16,y/16,z/16));
            bounds[0]=Math.min(bounds[0],v.x*16);bounds[1]=Math.min(bounds[1],v.y*16);
            bounds[2]=Math.max(bounds[2],v.x*16);bounds[3]=Math.max(bounds[3],v.y*16);
        }
        var band=cubes(harness.getChild("body_band"));
        if(band.size()!=4||cubes(harness).size()!=4)
            throw new AssertionError("Only one four-sided band may remain");
        var rail=band.get(3);
        float clearance=rail.minY-bounds[3];
        if(Math.abs(clearance-3)>1e-5)
            throw new AssertionError("Belly-to-rail opening must be three pixels, actual="+clearance);
        float z=center.z*16;
        int checks=0;
        for(int i=0;i<=32;i++) {
            float x=bounds[0]-.5F+(bounds[2]-bounds[0]+1)*i/32;
            float y=bounds[1]-.5F+(rail.minY-bounds[1]+1)*i/32;
            for(float[] point:new float[][]{{x,bounds[1]-.5F},{x,rail.minY+.5F},
                    {bounds[0]-.5F,y},{bounds[2]+.5F,y}}) {
                if(!contains(band,point[0],point[1],z))
                    throw new AssertionError("Upper ring leaves the torso's back / side / belly unwrapped");
                checks++;
            }
        }
        if(contains(band,center.x*16,center.y*16,z))
            throw new AssertionError("The torso band must have an open center");
        for(float x=-1.75F;x<=1.75F;x+=.25F)
            for(float y=bounds[3]+.125F;y<rail.minY;y+=.125F)
                if(contains(band,x,y,z)) throw new AssertionError("A connector or plate blocks the belly-to-rail opening");
        var all=cubes(harness);
        var reached=new java.util.HashSet<Integer>();reached.add(0);
        for(int pass=0;pass<all.size();pass++) for(int a=0;a<all.size();a++) for(int b=0;b<all.size();b++)
            if(reached.contains(a)&&touches(all.get(a),all.get(b))) reached.add(b);
        if(reached.size()!=all.size()) throw new AssertionError("Floating or disconnected harness piece");
        System.out.println("PASS: "+checks+" single-band perimeter checks, clear three-pixel opening and connected geometry");
    }
    /** Runs after resource reload so this uses the REAL baked Create item transforms. */
    private static void verifyWrenchContact(Minecraft mc) {
        try {
            com.google.gson.JsonObject item;
            try(var reader=mc.getResourceManager().getResourceOrThrow(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create","models/item/wrench/item.json")).openAsReader()) {
                item=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            }
            com.google.gson.JsonObject top=null,bottom=null;
            for(var element:item.getAsJsonArray("elements")) {
                var object=element.getAsJsonObject();
                if(!object.has("name")) continue;
                if(object.get("name").getAsString().equals("top thing")) top=object;
                if(object.get("name").getAsString().equals("bottom thing")) bottom=object;
            }
            if(top==null||bottom==null) throw new AssertionError("Cannot locate the real wrench jaws");
            var topFrom=top.getAsJsonArray("from");var topTo=top.getAsJsonArray("to");
            var bottomFrom=bottom.getAsJsonArray("from");var bottomTo=bottom.getAsJsonArray("to");
            // Center of the open mouth BETWEEN the two jaw blocks, not the
            // player's hand or the full wrench bounding box.
            var mouth=new org.joml.Vector3f(
                    (Math.max(topFrom.get(0).getAsFloat(),bottomFrom.get(0).getAsFloat())
                            +Math.min(topTo.get(0).getAsFloat(),bottomTo.get(0).getAsFloat()))/32,
                    (bottomTo.get(1).getAsFloat()+topFrom.get(1).getAsFloat())/32,
                    (topFrom.get(2).getAsFloat()+topTo.get(2).getAsFloat())/32);
            var wrench=com.simibubi.create.AllItems.WRENCH.asStack();
            var baked=mc.getItemRenderer().getModel(wrench,null,null,0);
            var method=Class.forName("com.simibubi.create.foundation.render.PlayerSkyhookRenderer")
                    .getDeclaredMethod("setHangingPose",boolean.class,HumanoidModel.class);
            method.setAccessible(true);
            var harness=cn.laowu.mod.client.CatPilotHarnessLayer.createRoot();
            var rail=cubes(harness.getChild("body_band")).get(3);
            // Same LivingEntityRenderer origin/axis conversion and pilot
            // passenger offset as the real scene, with standard adult scale.
            var catPose=new com.mojang.blaze3d.vertex.PoseStack();
            catPose.translate(0,cn.laowu.mod.entity.CatFlightCarrier.CAT_HEIGHT,0);
            catPose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
            catPose.scale(-1,-1,1);
            catPose.translate(0,-1.501,0);
            var intoCat=new org.joml.Matrix4f(catPose.last().pose()).invert();
            int checks=0;
            for(boolean slim:new boolean[]{false,true}) for(boolean left:new boolean[]{false,true}) {
                var root=net.minecraft.client.model.geom.builders.LayerDefinition.create(
                        net.minecraft.client.model.PlayerModel.createMesh(
                                net.minecraft.client.model.geom.builders.CubeDeformation.NONE,slim),64,64).bakeRoot();
                var player=new net.minecraft.client.model.PlayerModel<net.minecraft.world.entity.LivingEntity>(root,slim);
                method.invoke(null,left,player);
                var pose=new com.mojang.blaze3d.vertex.PoseStack();
                pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
                pose.scale(-1,-1,1);
                pose.scale(.9375F,.9375F,.9375F);
                pose.translate(0,-1.501,0);
                player.translateToHand(left?net.minecraft.world.entity.HumanoidArm.LEFT:net.minecraft.world.entity.HumanoidArm.RIGHT,pose);
                // Actual vanilla ItemInHandLayer transform, followed by the
                // loaded item's own third-person transform (including handedness).
                pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));
                pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
                pose.translate((left?-1:1)/16F,.125F,-.625F);
                baked.applyTransform(left?net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND:
                        net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,pose,left);
                pose.translate(-.5,-.5,-.5);
                var actual=pose.last().pose().transformPosition(new org.joml.Vector3f(mouth));
                var pixels=intoCat.transformPosition(actual).mul(16);
                if(!contains(java.util.List.of(rail),pixels.x,pixels.y,pixels.z))
                    throw new AssertionError("Wrench mouth misses rail, slim="+slim+" left="+left+" cat pixels="+pixels);
                System.out.println("PASS: real wrench mouth intersects rail, slim="+slim+" left="+left+" cat pixels="+pixels);
                checks++;
            }
            System.out.println("PASS: "+checks+" actual baked-wrench contact checks across both hands and skin models");
        } catch(Exception error) {
            throw new IllegalStateException("Real wrench contact probe failed",error);
        }
    }

    private static org.joml.Vector3f cubeCenter(net.minecraft.client.model.geom.ModelPart.Cube cube) {
        return new org.joml.Vector3f((cube.minX+cube.maxX)/32,(cube.minY+cube.maxY)/32,(cube.minZ+cube.maxZ)/32);
    }
    private static java.util.List<net.minecraft.client.model.geom.ModelPart.Cube> cubes(
            net.minecraft.client.model.geom.ModelPart part) {
        var result=new java.util.ArrayList<net.minecraft.client.model.geom.ModelPart.Cube>();
        part.visit(new com.mojang.blaze3d.vertex.PoseStack(),(pose,path,index,cube)->result.add(cube));
        return result;
    }
    private static boolean contains(java.util.List<net.minecraft.client.model.geom.ModelPart.Cube> cubes,float x,float y,float z) {
        return cubes.stream().anyMatch(c->x>=c.minX-1e-5&&x<=c.maxX+1e-5
                &&y>=c.minY-1e-5&&y<=c.maxY+1e-5&&z>=c.minZ-1e-5&&z<=c.maxZ+1e-5);
    }
    private static boolean touches(net.minecraft.client.model.geom.ModelPart.Cube a,net.minecraft.client.model.geom.ModelPart.Cube b) {
        return a.minX<=b.maxX&&a.maxX>=b.minX&&a.minY<=b.maxY&&a.maxY>=b.minY&&a.minZ<=b.maxZ&&a.maxZ>=b.minZ;
    }
}
