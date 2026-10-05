package cn.laowu.mod.client;
import cn.laowu.mod.create.*;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.*;

final class CatAutoLaserClientProbe {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static Class<?> poseType,rendererType;
    static Object call(String name,Class<?>[] types,Object... args)throws Exception{return poseType.getMethod(name,types).invoke(null,args);}
    static void model(BlockState state,float extension,float yaw,float pitch,PoseStack pose,MultiBufferSource buffers)throws Exception {
        rendererType.getMethod("renderModel",BlockState.class,float.class,float.class,float.class,boolean.class,PoseStack.class,MultiBufferSource.class,int.class,int.class)
            .invoke(null,state,extension,yaw,pitch,true,pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
    }
    static void verify(Minecraft mc)throws Exception {
        if(Boolean.getBoolean("laowu.auto_laser_world_probe"))check(mc.level!=null&&mc.player!=null,"World laser probe cannot skip the moving-target regression without a loaded player/world");
        poseType=Class.forName("cn.laowu.mod.client.CatAutoLaserPose");rendererType=Class.forName("cn.laowu.mod.client.CatAutoLaserRenderer");
        for(Direction bottom:Direction.values())for(float extension:new float[]{0,.5f,1})for(Vec3 target:new Vec3[]{new Vec3(8,5,8),new Vec3(-8,-5,-8),new Vec3(.01,2,-10),new Vec3(-.01,2,-10)}) {
            var state=CatMachineBlocks.CAT_AUTO_LASER.get().defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom);
            Object aim=call("aim",new Class<?>[]{BlockState.class,BlockPos.class,float.class,Vec3.class},state,BlockPos.ZERO,extension,target);
            float yaw=((Number)aim.getClass().getMethod("yaw").invoke(aim)).floatValue(),pitch=((Number)aim.getClass().getMethod("pitch").invoke(aim)).floatValue();
            Vec3 muzzle=(Vec3)aim.getClass().getMethod("muzzle").invoke(aim),direction=(Vec3)aim.getClass().getMethod("direction").invoke(aim);
            check(direction.dot(target.subtract(muzzle).normalize())>.999999,"Six-frame barrel misses target "+bottom);
            var capture=new CatMixerIdleProbe.Capture();
            model(state,extension,yaw,pitch,new PoseStack(),type->capture);
            check(capture.points.size()>100,"Author geometry did not render");
            boolean tip=false;
            for(int i=0;i+3<capture.points.size();i+=4) {
                Vec3 center=Vec3.ZERO;for(int j=0;j<4;j++)center=center.add(capture.points.get(i+j));
                if(center.scale(.25).distanceTo(muzzle)<.0001)tip=true;
            }
            check(tip,"Calculated muzzle differs from actual author tip vertices "+bottom+"/"+extension);
        }
        for(Direction bottom:Direction.values())for(Vec3 local:new Vec3[]{new Vec3(1.7,1.74,.5),new Vec3(1.5,1.74,.5)}) {
            var state=CatMachineBlocks.CAT_AUTO_LASER.get().defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom);
            Vec3 target=CatMachineOrientation.position(state,local);
            Object aim=call("aim",new Class<?>[]{BlockState.class,BlockPos.class,float.class,Vec3.class},state,BlockPos.ZERO,1f,target);
            Vec3 muzzle=(Vec3)aim.getClass().getMethod("muzzle").invoke(aim),direction=(Vec3)aim.getClass().getMethod("direction").invoke(aim);
            check(Math.abs(direction.dot(target.subtract(muzzle).normalize()))>.999999,"Adjacent target makes barrel diverge "+bottom);
            if(local.x>1.6)check(direction.dot(target.subtract(muzzle))>0,"Reachable adjacent target was aimed backwards");
        }
        var regressions=new java.util.ArrayList<String>();
        try{surfaceLighting();}catch(AssertionError failure){regressions.add(failure.getMessage());}
        try{itemContexts(mc);}catch(AssertionError failure){regressions.add(failure.getMessage());}
        if(mc.level!=null){
            try{world(mc);}catch(AssertionError failure){regressions.add(failure.getMessage());}
            movingTargetKeepsAcquiredBeam(mc);CatAutoLaserFilterSlotProbe.verify(mc);CreatureFilterClientProbe.verify(mc);
        }
        check(regressions.isEmpty(),"Display regressions: "+regressions);
        var down=CatMachineBlocks.CAT_AUTO_LASER.get().defaultBlockState();
        var base=(Vec3)call("muzzle",new Class<?>[]{BlockState.class,BlockPos.class,float.class,float.class,float.class},down,BlockPos.ZERO,0f,0f,0f);
        check(base.distanceTo(new Vec3(.5,.53125,.99375))<.0001,"Beam must start at pen mouth, not removed solid light tip");
        var capture=new CatMixerIdleProbe.Capture();
        mc.getItemRenderer().renderStatic(new ItemStack(CatMachineBlocks.CAT_AUTO_LASER_ITEM.get()),ItemDisplayContext.GUI,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,new PoseStack(),t->capture,null,0);
        check(capture.points.size()>100,"Workstation item missing complete authored geometry");
        for(var p:capture.points)check(Math.abs(p.x)<=.501&&Math.abs(p.y)<=.501,"Workstation item spills outside GUI slot "+p);
        capture(mc);captureSlot(mc);captureLids(mc);
        System.out.println("PASS: AUTO LASER CLIENT - six frames, 72 authored muzzle/aim/vertex cases, GUI bounds and real GPU snapshot");
    }
    static void packet(CatAutoLaserBlockEntity be,net.minecraft.nbt.CompoundTag tag)throws Exception {
        be.readClient(tag);
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    static CatMixerIdleProbe.Capture beam(Minecraft mc,CatAutoLaserBlockEntity be,CatMixerIdleProbe.Capture geometry) {
        return beam(mc,be,geometry,1f);
    }
    static CatMixerIdleProbe.Capture beam(Minecraft mc,CatAutoLaserBlockEntity be,CatMixerIdleProbe.Capture geometry,float partial) {
        var ray=new CatMixerIdleProbe.Capture();
        var renderer=mc.getBlockEntityRenderDispatcher().getRenderer(be);
        check(renderer!=null,"Automatic laser renderer was not registered");
        renderer.render(be,partial,new PoseStack(),t->t==RenderType.debugQuads()?ray:geometry,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
        return ray;
    }
    /** Catches acquired beams disappearing when network/angle interpolation trails a moving enemy. */
    static void movingTargetKeepsAcquiredBeam(Minecraft mc)throws Exception {
        var level=mc.level;var origin=mc.player.blockPosition().above(12).offset(0,0,24);
        for(Direction bottom:Direction.values()) {
            var p=origin.offset(bottom.ordinal()*16,0,0);
            for(int x=-6;x<=6;x++)for(int y=-6;y<=6;y++)for(int z=-6;z<=6;z++)
                level.setBlock(p.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            var state=CatMachineBlocks.CAT_AUTO_LASER.get().defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom);
            level.setBlock(p,state,3);
            var be=(CatAutoLaserBlockEntity)level.getBlockEntity(p);
            var mob=net.minecraft.world.entity.EntityType.HUSK.create(level);
            Vec3 previousTarget=CatMachineOrientation.position(state,new Vec3(3.5,1.6,4.1)).add(Vec3.atLowerCornerOf(p));
            mob.setPos(previousTarget.subtract(0,mob.getEyeHeight(),0));mob.xo=mob.getX();mob.yo=mob.getY();mob.zo=mob.getZ();
            level.putNonPlayerEntity(mob.getId(),mob);
            try {
                var tag=new net.minecraft.nbt.CompoundTag();tag.putFloat("Extension",1);
                tag.putUUID("TargetUuid",mob.getUUID());tag.putInt("TargetId",mob.getId());
                var initial=CatAutoLaserAim.aim(state,p,1,previousTarget);
                tag.putFloat("AimYaw",initial.yaw());tag.putFloat("AimPitch",initial.pitch());tag.putBoolean("AimLocked",false);
                packet(be,tag);be.setSpeed(16);
                check(beam(mc,be,new CatMixerIdleProbe.Capture()).points.isEmpty(),"Moving-target fixture drew before server acquisition "+bottom);
                for(int tick=1;tick<=80;tick++) {
                    // A real entity moves while the newest server aim still refers to its preceding tick.
                    Vec3 target=CatMachineOrientation.position(state,new Vec3(3.5+.6*Math.sin(tick*.22),1.6+.25*Math.sin(tick*.14),3.5+.6*Math.cos(tick*.22))).add(Vec3.atLowerCornerOf(p));
                    mob.xo=mob.getX();mob.yo=mob.getY();mob.zo=mob.getZ();mob.setPos(target.subtract(0,mob.getEyeHeight(),0));
                    var delayed=CatAutoLaserAim.aim(state,p,1,previousTarget);
                    tag.putFloat("AimYaw",delayed.yaw());tag.putFloat("AimPitch",delayed.pitch());tag.putBoolean("AimLocked",true);
                    packet(be,tag);be.setSpeed(16);be.tick();
                    check(be.isAimLocked(),"Moving-target fixture unexpectedly lost its authoritative acquisition");
                    for(float partial:new float[]{.25f,.5f,.75f,1f}) {
                        var ray=beam(mc,be,new CatMixerIdleProbe.Capture(),partial);
                        check(ray.points.size()==24,"Acquired beam disappeared while real enemy moved: bottom="+bottom+", tick="+tick+", partial="+partial+", vertices="+ray.points.size());
                        Vec3 end=Vec3.ZERO;for(int i=4;i<8;i++)end=end.add(ray.points.get(i));
                        Vec3 eye=mob.getPosition(partial).add(0,mob.getEyeHeight(),0).subtract(Vec3.atLowerCornerOf(p));
                        check(end.scale(.25).distanceTo(eye)<.0001,"Moving beam did not reach interpolated enemy eye "+bottom+"/"+tick);
                        check(ray.alphas.stream().allMatch(a->a==89),"Moving acquired beam changed its translucent alpha");
                    }
                    previousTarget=target;
                }
                tag.putBoolean("AimLocked",false);packet(be,tag);
                check(beam(mc,be,new CatMixerIdleProbe.Capture()).points.isEmpty(),"Acquisition revocation retained moving beam "+bottom);
                tag.remove("TargetUuid");tag.remove("TargetId");packet(be,tag);
                check(beam(mc,be,new CatMixerIdleProbe.Capture()).points.isEmpty(),"Cleared moving target retained beam "+bottom);
            }finally{mob.discard();level.setBlock(p,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);}
        }
        System.out.println("PASS: AUTO LASER MOVING BEAM - real moving entities, lagging aim packets, six frames, 80 ticks, four partial frames, alpha and acquisition/clear guards");
    }
    static void world(Minecraft mc)throws Exception {
        var level=mc.level;var origin=mc.player.blockPosition().above(5);
        for(Direction bottom:Direction.values()) {
            var p=origin.offset(bottom.ordinal()*8,0,0);
            for(int x=-4;x<=4;x++)for(int y=-4;y<=4;y++)for(int z=-4;z<=4;z++)
                level.setBlock(p.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            var state=CatMachineBlocks.CAT_AUTO_LASER.get().defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom);
            level.setBlock(p,state,3);
            var baked=mc.getBlockRenderer().getBlockModel(state);
            var faces=new java.util.ArrayList<net.minecraft.core.Direction>();faces.add(null);faces.addAll(java.util.List.of(Direction.values()));
            for(var face:faces)for(var quad:baked.getQuads(state,face,net.minecraft.util.RandomSource.create(0))){
                int[] data=quad.getVertices();int stride=data.length/4;
                for(int v=0;v<4;v++){
                    var point=new Vec3(Float.intBitsToFloat(data[v*stride]),Float.intBitsToFloat(data[v*stride+1]),Float.intBitsToFloat(data[v*stride+2]));
                    double depth=point.subtract(.5,.5,.5).dot(Vec3.atLowerCornerOf(bottom.getNormal()));
                    check(depth>=.4374&&depth<=.5001,"Static shaft mouth rotated onto a different face: "+bottom+" point="+point);
                }
            }
            var be=(CatAutoLaserBlockEntity)level.getBlockEntity(p);
            check(be!=null,"Client six-way block entity missing");
            // Native half-shaft: flush outer end and full-size geometry/UVs
            // continuing to the center, not a flattened full-length shaft.
            var shaftVertices=new CatMixerIdleProbe.Capture();
            var ignoredVertices=new CatMixerIdleProbe.Capture();
            mc.getBlockEntityRenderDispatcher().getRenderer(be).render(be,1f,new PoseStack(),
                type->type==RenderType.solid()?shaftVertices:ignoredVertices,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            check(!shaftVertices.points.isEmpty(),"Missing real kinetic shaft geometry");
            double outer=shaftVertices.points.stream().mapToDouble(point->
                point.subtract(.5,.5,.5).dot(Vec3.atLowerCornerOf(bottom.getNormal()))).max().orElseThrow();
            check(Math.abs(outer-.5)<.0001,"Shaft end must be flush with Create block boundary: "+bottom+" depth="+outer);
            double inner=shaftVertices.points.stream().mapToDouble(point->
                point.subtract(.5,.5,.5).dot(Vec3.atLowerCornerOf(bottom.getNormal()))).min().orElseThrow();
            check(Math.abs(inner)<.0001,"Use an unscaled Create half-shaft reaching the center, not a compressed shaft: "+bottom+" depth="+inner);
            var mob=net.minecraft.world.entity.EntityType.HUSK.create(level);
            Vec3 target=CatMachineOrientation.position(state,new Vec3(3.5,1.6,3.5)).add(Vec3.atLowerCornerOf(p));
            mob.setPos(target.subtract(0,mob.getEyeHeight(),0));mob.xo=mob.getX();mob.yo=mob.getY();mob.zo=mob.getZ();
            level.putNonPlayerEntity(mob.getId(),mob);
            var tag=new net.minecraft.nbt.CompoundTag();tag.putFloat("Extension",1);tag.putUUID("TargetUuid",mob.getUUID());tag.putInt("TargetId",mob.getId());
            var trackedAim=CatAutoLaserAim.aim(state,p,1,target);
            tag.putFloat("AimYaw",trackedAim.yaw());tag.putFloat("AimPitch",trackedAim.pitch());tag.putBoolean("AimLocked",true);
            packet(be,tag);be.setSpeed(16);
            var bare=new CatMixerIdleProbe.Capture();beam(mc,be,bare);
            var installed=new ItemStack(LaoWuMod.CREATURE_FILTER.get());
            tag.put("CreatureFilter",installed.save(new net.minecraft.nbt.CompoundTag()));packet(be,tag);be.setSpeed(16);
            var filled=new CatMixerIdleProbe.Capture();beam(mc,be,filled);
            check(filled.points.size()>bare.points.size(),"Installed filter icon emitted no geometry in "+bottom);
            boolean rearIcon=filled.points.stream().anyMatch(point->{
                Vec3 v=point.subtract(.5,.5,.5);
                double x=v.dot(CatMachineOrientation.toWorld(bottom,new Vec3(1,0,0)))+.5;
                double y=v.dot(CatMachineOrientation.toWorld(bottom,new Vec3(0,1,0)))+.5;
                double z=v.dot(CatMachineOrientation.toWorld(bottom,new Vec3(0,0,1)))+.5;
                return x>=.36&&x<=.64&&y>=.05&&y<=.32&&z<-.0001&&z>-.06;
            });
            check(rearIcon,"Installed icon is not at rotated rear filter panel "+bottom);
            var expectedIcon=new CatMixerIdleProbe.Capture();
            var expectedPose=new PoseStack();
            expectedPose.translate(.5,.5,.5);CatMachineOrientation.rotatePose(expectedPose,bottom);
            expectedPose.translate(0,-5d/16,-.5-1d/512);expectedPose.scale(.5f,.5f,.5f);
            com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer.renderItemIntoValueBox(
                installed,expectedPose,t->expectedIcon,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            check(expectedIcon.points.stream().allMatch(a->filled.points.stream().anyMatch(b->a.distanceTo(b)<.00001)),
                "Every icon vertex must match native slot size/rotation at authored Y3/16 "+bottom);
            var geometry=new CatMixerIdleProbe.Capture();var ray=beam(mc,be,geometry);
            check(ray.points.size()==24,"Actual BE renderer did not emit six solid beam quads "+bottom+" count="+ray.points.size());
            check(ray.alphas.size()==24&&ray.alphas.stream().allMatch(a->a>=85&&a<=95),"Workstation beam must be about 35 percent opaque");
            Object aim=call("aim",new Class<?>[]{BlockState.class,BlockPos.class,float.class,Vec3.class},state,p,1f,target);
            Vec3 muzzle=((Vec3)aim.getClass().getMethod("muzzle").invoke(aim)).subtract(Vec3.atLowerCornerOf(p));
            Vec3 start=Vec3.ZERO,end=Vec3.ZERO;
            for(int i=0;i<4;i++){start=start.add(ray.points.get(i));end=end.add(ray.points.get(i+4));}
            check(start.scale(.25).distanceTo(muzzle)<.0001,"Actual beam start disagrees with authored muzzle");
            check(end.scale(.25).distanceTo(target.subtract(Vec3.atLowerCornerOf(p)))<.0001,"Actual beam misses tracked enemy");
            Vec3 midpoint=start.scale(.25).add(end.scale(.25)).scale(.5).add(Vec3.atLowerCornerOf(p));
            var wall=BlockPos.containing(midpoint);level.setBlock(wall,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            var blocked=beam(mc,be,new CatMixerIdleProbe.Capture());Vec3 blockedEnd=Vec3.ZERO;
            for(int i=4;i<8;i++)blockedEnd=blockedEnd.add(blocked.points.get(i));
            check(blockedEnd.scale(.25).distanceTo(muzzle)<end.scale(.25).distanceTo(muzzle)-.1,"Red beam penetrates wall");
            level.setBlock(wall,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            // True Create client packet reading, same entity ID with a wrong UUID.
            tag.putUUID("TargetUuid",java.util.UUID.randomUUID());packet(be,tag);
            check(beam(mc,be,new CatMixerIdleProbe.Capture()).points.isEmpty(),"Entity-ID reuse produced ghost beam");
            tag.remove("TargetUuid");tag.remove("TargetId");packet(be,tag);
            check(beam(mc,be,new CatMixerIdleProbe.Capture()).points.isEmpty(),"Clear-target packet retained beam");
            check(Math.abs(be.getAimYaw(1)-trackedAim.yaw())<.0001&&Math.abs(be.getAimPitch(1)-trackedAim.pitch())<.0001,"Clear-target packet reset held angles");
            tag.putUUID("TargetUuid",mob.getUUID());tag.putInt("TargetId",mob.getId());packet(be,tag);be.setSpeed(0);
            check(beam(mc,be,new CatMixerIdleProbe.Capture()).points.isEmpty(),"Stopped machine retained beam");
            be.setSpeed(16);
            var camera=Vec3.atCenterOf(p).add(18,0,0);
            var frustum=new net.minecraft.client.renderer.culling.Frustum(new org.joml.Matrix4f(),new org.joml.Matrix4f().perspective((float)Math.toRadians(70),1,.05f,5));
            frustum.prepare(camera.x,camera.y,camera.z);
            check(!frustum.isVisible(new net.minecraft.world.phys.AABB(p))&&frustum.isVisible(be.getRenderBoundingBox()),"Beam-only view culls workstation bounds");
            new CatAutoLaserItemRenderer(mc.getBlockEntityRenderDispatcher(),mc.getEntityModels()).onResourceManagerReload(mc.getResourceManager());
            check(beam(mc,be,new CatMixerIdleProbe.Capture()).points.size()==24,"Resource reload broke runtime geometry/beam");
            tag.remove("TargetUuid");tag.remove("TargetId");tag.putBoolean("AimLocked",false);
            tag.putFloat("AimYaw",(float)Math.toRadians(179));packet(be,tag);
            for(int step=0;step<30;step++){
                float before=be.getAimYaw(1);be.tick();
                float change=net.minecraft.util.Mth.wrapDegrees((float)Math.toDegrees(be.getAimYaw(1)-before));
                check(Math.abs(change)<=8.01,"Client packet caused an instantaneous yaw jump");
            }
            float beforeBoundary=be.getAimYaw(1);tag.putFloat("AimYaw",(float)Math.toRadians(-179));packet(be,tag);be.tick();
            float crossed=net.minecraft.util.Mth.wrapDegrees((float)Math.toDegrees(be.getAimYaw(1)-beforeBoundary));
            check(Math.abs(crossed-2)<.01,"Client crossed the long way around the half-turn boundary");
            check(Math.abs(net.minecraft.util.Mth.wrapDegrees((float)Math.toDegrees(be.getAimYaw(.5f))))>178,"Partial-tick yaw interpolation swung through forward");
            level.setBlock(p,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            check(beam(mc,be,new CatMixerIdleProbe.Capture()).points.isEmpty(),"Removed block entity retained beam");
            mob.discard();
        }
        System.out.println("PASS: AUTO LASER WORLD - actual six-way BE renderer, beam endpoints/wall, UUID reuse, clear/stop/remove, frustum and resource reload");
    }
    static void surfaceLighting()throws Exception {
        for(Direction bottom:Direction.values()){
            var state=CatMachineBlocks.CAT_AUTO_LASER.get().defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom);
            var vertices=new CatMixerIdleProbe.Capture();
            var types=new java.util.HashSet<RenderType>();
            model(state,0,0,0,new PoseStack(),type->{types.add(type);return vertices;});
            Vec3 up=CatMachineOrientation.vector(state,new Vec3(0,1,0));
            int topQuads=0;
            for(int i=0;i+3<vertices.points.size();i+=4){
                boolean rim=true;
                for(int j=0;j<4;j++)rim &= Math.abs(vertices.points.get(i+j).subtract(.5,.5,.5).dot(up)-.25)<.0001;
                if(rim){topQuads++;check(vertices.normals.get(i).dot(up)>.99,"Top rim normal faces inward: "+bottom+" normal="+vertices.normals.get(i));}
            }
            check(topQuads==4,"Missing four top rim strips "+bottom);
            var open=new CatMixerIdleProbe.Capture();
            model(state,1,0,0,new PoseStack(),type->open);
            Vec3 right=CatMachineOrientation.vector(state,new Vec3(1,0,0));
            for(int i=0;i+3<open.points.size();i+=4){
                boolean lid=true;double minX=10,maxX=-10;
                for(int j=0;j<4;j++){
                    var p=open.points.get(i+j).subtract(.5,.5,.5);
                    lid &= Math.abs(p.dot(up)-.24375)<.0001;
                    minX=Math.min(minX,p.dot(right));maxX=Math.max(maxX,p.dot(right));
                }
                // Original 2px translation / 0.3 scale: the complete 7px right
                // lid ends at 5.9..8, including its 0.1px tooth tip in the opening.
                // Do not require cropping that tooth merely to clear the aperture.
                if(lid)check(Math.abs(minX-(minX<0?-8d/16:5.9/16))<.0001
                        &&Math.abs(maxX-(minX<0?-6.2/16:8d/16))<.0001,
                    "Open lids must preserve the authored outward animation bounds "+bottom);
            }
            // Lids have opposite coplanar authored faces: only the camera-facing
            // side may write depth, otherwise the underside darkens the top.
            for(var type:types){
                type.setupRenderState();
                boolean culled=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_CULL_FACE);
                type.clearRenderState();
                check(culled,"Coplanar lid backs render over their lit front faces "+bottom);
            }
        }
        System.out.println("PASS: AUTO LASER SURFACE LIGHTING - six-way outward rim normals and hidden coplanar backs");
    }
    static void itemContexts(Minecraft mc)throws Exception {
        var state=CatMachineBlocks.CAT_AUTO_LASER.get().defaultBlockState();
        var vanilla=mc.getBlockRenderer().getBlockModel(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        for(var context:new ItemDisplayContext[]{ItemDisplayContext.GROUND,ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                ItemDisplayContext.FIRST_PERSON_LEFT_HAND,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                ItemDisplayContext.THIRD_PERSON_LEFT_HAND,ItemDisplayContext.FIXED}) {
            var actual=new CatMixerIdleProbe.Capture();
            mc.getItemRenderer().renderStatic(new ItemStack(CatMachineBlocks.CAT_AUTO_LASER_ITEM.get()),context,
                LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,new PoseStack(),t->actual,null,0);
            var expected=new CatMixerIdleProbe.Capture();var pose=new PoseStack();
            vanilla.getTransforms().getTransform(context).apply(false,pose);pose.translate(-.5,-.5,-.5);
            mc.getBlockRenderer().renderSingleBlock(state,pose,t->expected,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            model(state,0,0,0,pose,t->expected);
            check(actual.points.size()==expected.points.size(),"Item geometry count differs "+context);
            for(int i=0;i<actual.points.size();i++)check(actual.points.get(i).distanceTo(expected.points.get(i))<.0001,
                "Held/dropped item does not use vanilla block transform: "+context+" vertex="+i);
        }
        System.out.println("PASS: AUTO LASER ITEM CONTEXTS - ground, first/third person both hands and fixed match vanilla block transforms");
    }
    static void captureSlot(Minecraft mc)throws Exception {
        var projection=new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix());
        var sorting=com.mojang.blaze3d.systems.RenderSystem.getVertexSorting();
        var view=com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();view.pushPose();view.setIdentity();
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        com.mojang.blaze3d.pipeline.TextureTarget output=null;
        try(var guard=new CatPerformanceOutline.State();var framebuffer=new PerformanceSceneSnapshot.Target()){
            output=new com.mojang.blaze3d.pipeline.TextureTarget(960,360,true,Minecraft.ON_OSX);output.bindWrite(true);
            com.mojang.blaze3d.systems.RenderSystem.clearColor(.08f,.1f,.12f,1);
            com.mojang.blaze3d.systems.RenderSystem.clearDepth(1);com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
            com.mojang.blaze3d.systems.RenderSystem.clear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT,Minecraft.ON_OSX);
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0,960,360,0,1000,21000),com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            var buffers=mc.renderBuffers().bufferSource();
            for(int i=0;i<3;i++){
                var state=CatMachineBlocks.CAT_AUTO_LASER.get().defaultBlockState();
                var p=new PoseStack();p.translate(160+i*320,200,-11000);p.scale(220,-220,220);
                p.mulPose(com.mojang.math.Axis.XP.rotationDegrees(12));p.mulPose(com.mojang.math.Axis.YP.rotationDegrees(165));p.translate(-.5,-.4,-.5);
                mc.getBlockRenderer().renderSingleBlock(state,p,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
                var be=new CatAutoLaserBlockEntity(BlockPos.ZERO,state);be.setLevel(mc.level);
                if(i==1){
                    var tag=new net.minecraft.nbt.CompoundTag();
                    tag.put("CreatureFilter",new ItemStack(LaoWuMod.CREATURE_FILTER.get()).save(new net.minecraft.nbt.CompoundTag()));
                    packet(be,tag);
                }
                mc.getBlockEntityRenderDispatcher().getRenderer(be).render(be,1,p,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
                if(i==2) {
                    var box=new com.simibubi.create.foundation.blockEntity.behaviour.ValueBox(net.minecraft.network.chat.Component.empty(),
                        new net.minecraft.world.phys.AABB(-.25,-.25,-.025,.25,.25,.025),BlockPos.ZERO,state)
                        .transform(CatAutoLaserFilterSlot.INSTANCE).passive(false);
                    box.render(p,new net.createmod.catnip.render.SuperRenderTypeBuffer(){
                        public com.mojang.blaze3d.vertex.VertexConsumer getBuffer(RenderType type){return buffers.getBuffer(type);}
                        public com.mojang.blaze3d.vertex.VertexConsumer getEarlyBuffer(RenderType type){return buffers.getBuffer(type);}
                        public com.mojang.blaze3d.vertex.VertexConsumer getLateBuffer(RenderType type){return buffers.getBuffer(type);}
                        public void draw(){buffers.endBatch();}
                        public void draw(RenderType type){buffers.endBatch(type);}
                    },Vec3.ZERO,1);
                }
            }
            buffers.endBatch();
            try(var image=new com.mojang.blaze3d.platform.NativeImage(960,360,false)){
                com.mojang.blaze3d.systems.RenderSystem.bindTexture(output.getColorTextureId());image.downloadTexture(0,false);image.flipY();
                image.writeToFile(java.nio.file.Path.of("cat-auto-laser-filter-slot.png"));
            }
        }finally{
            if(output!=null)output.destroyBuffers();view.popPose();
            com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(projection,sorting);
        }
    }
    static void captureLids(Minecraft mc)throws Exception {
        var projection=new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix());
        var sorting=com.mojang.blaze3d.systems.RenderSystem.getVertexSorting();
        var view=com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();view.pushPose();view.setIdentity();
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        com.mojang.blaze3d.pipeline.TextureTarget output=null;
        try(var guard=new CatPerformanceOutline.State();var framebuffer=new PerformanceSceneSnapshot.Target()){
            output=new com.mojang.blaze3d.pipeline.TextureTarget(960,360,true,Minecraft.ON_OSX);output.bindWrite(true);
            com.mojang.blaze3d.systems.RenderSystem.clearColor(.08f,.1f,.12f,1);
            com.mojang.blaze3d.systems.RenderSystem.clearDepth(1);com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
            com.mojang.blaze3d.systems.RenderSystem.clear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT,Minecraft.ON_OSX);
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0,960,360,0,1000,21000),com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            var buffers=mc.renderBuffers().bufferSource();
            for(int i=0;i<3;i++){
                var state=CatMachineBlocks.CAT_AUTO_LASER.get().defaultBlockState();
                var p=new PoseStack();p.translate(160+i*320,180,-11000);p.scale(280,-280,280);
                p.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));p.translate(-.5,-.75,-.5);
                mc.getBlockRenderer().renderSingleBlock(state,p,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
                model(state,new float[]{0,.15f,.4f}[i],0,0,p,buffers);
            }
            buffers.endBatch();
            try(var image=new com.mojang.blaze3d.platform.NativeImage(960,360,false)){
                com.mojang.blaze3d.systems.RenderSystem.bindTexture(output.getColorTextureId());image.downloadTexture(0,false);image.flipY();
                image.writeToFile(java.nio.file.Path.of("cat-auto-laser-lid-interlock.png"));
            }
        }finally{
            if(output!=null)output.destroyBuffers();view.popPose();
            com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(projection,sorting);
        }
    }
    static void capture(Minecraft mc)throws Exception {
        var projection=new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix());
        var sorting=com.mojang.blaze3d.systems.RenderSystem.getVertexSorting();
        var view=com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();view.pushPose();view.setIdentity();
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        com.mojang.blaze3d.pipeline.TextureTarget output=null;
        try(var guard=new CatPerformanceOutline.State();var framebuffer=new PerformanceSceneSnapshot.Target()){
            output=new com.mojang.blaze3d.pipeline.TextureTarget(960,720,true,Minecraft.ON_OSX);output.bindWrite(true);
            com.mojang.blaze3d.systems.RenderSystem.clearColor(.08f,.1f,.12f,1);
            com.mojang.blaze3d.systems.RenderSystem.clearDepth(1);com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
            com.mojang.blaze3d.systems.RenderSystem.clear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT,Minecraft.ON_OSX);
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0,960,720,0,1000,21000),com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            var buffers=mc.renderBuffers().bufferSource();
            for(Direction bottom:Direction.values()){
                var state=CatMachineBlocks.CAT_AUTO_LASER.get().defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom);
                var p=new PoseStack();int i=bottom.ordinal();p.translate(160+(i%3)*320,100+(i/3)*180,-11000);p.scale(64,-64,64);
                p.mulPose(com.mojang.math.Axis.XP.rotationDegrees(25));p.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-35));p.translate(-.5,-.7,-.5);
                mc.getBlockRenderer().renderSingleBlock(state,p,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
                model(state,1,.6f,-.2f,p,buffers);
                Vec3 muzzle=(Vec3)call("muzzle",new Class<?>[]{BlockState.class,BlockPos.class,float.class,float.class,float.class},state,BlockPos.ZERO,1f,.6f,-.2f);
                Vec3 direction=CatMachineOrientation.vector(state,new Vec3(Math.sin(.6)*Math.cos(-.2),-Math.sin(-.2),Math.cos(.6)*Math.cos(-.2)));
                CatAutoLaserRenderer.renderBeam(muzzle,muzzle.add(direction.scale(1.3)),p,buffers);
            }
            for(int i=0;i<2;i++){
                var p=new PoseStack();p.translate(380+i*160,425,-11000);p.scale(64,-64,64);
                mc.getItemRenderer().renderStatic(new ItemStack(CatMachineBlocks.CAT_AUTO_LASER_ITEM.get()),ItemDisplayContext.GUI,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,p,buffers,null,0);
            }
            int closeup=0;
            for(Direction bottom:new Direction[]{Direction.DOWN,Direction.UP,Direction.NORTH}){
                var state=CatMachineBlocks.CAT_AUTO_LASER.get().defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom);
                var p=new PoseStack();p.translate(160+closeup++*320,600,-11000);p.scale(120,-120,120);
                p.mulPose(com.mojang.math.Axis.XP.rotationDegrees(30));p.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-35));p.translate(-.5,-.4,-.5);
                mc.getBlockRenderer().renderSingleBlock(state,p,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
                var be=new CatAutoLaserBlockEntity(BlockPos.ZERO,state);be.setLevel(mc.level);
                mc.getBlockEntityRenderDispatcher().getRenderer(be).render(be,1,p,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            }
            buffers.endBatch();
            try(var image=new com.mojang.blaze3d.platform.NativeImage(960,720,false)){
                com.mojang.blaze3d.systems.RenderSystem.bindTexture(output.getColorTextureId());image.downloadTexture(0,false);image.flipY();
                image.writeToFile(java.nio.file.Path.of("cat-auto-laser-sixway.png"));
            }
        }finally{
            if(output!=null)output.destroyBuffers();view.popPose();
            com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(projection,sorting);
        }
    }
}
