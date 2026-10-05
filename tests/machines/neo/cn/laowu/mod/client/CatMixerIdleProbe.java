package cn.laowu.mod.client;
import cn.laowu.mod.create.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
/** Capture only the actual moving assembly: a rotating shaft alone must not make this pass. */
final class CatMixerIdleProbe {
    static void verify(Minecraft mc){
        verifyNoFlatPortOverlay(mc);
        for(Direction bottom:Direction.values()){
            var state=CatMachineBlocks.CAT_MIXER.get().defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom)
                .setValue(CatProcessorBlock.SHAFT_AXIS,bottom.getAxis()==Direction.Axis.X?Direction.Axis.Z:Direction.Axis.X);
            var be=new CatMixerBlockEntity(BlockPos.ZERO,state);be.setLevel(mc.level);be.running=false;be.setSpeed(64);
            var first=sample(be,0);var second=sample(be,.75f);
            check(first.size()>30&&first.size()==second.size(),"Missing moving geometry");
            int moving=0;
            for(int i=0;i<first.size();i++){
                if(first.get(i).distanceToSqr(second.get(i))>1e-8)moving++;
                double a=component(first.get(i),bottom.getAxis()),b=component(second.get(i),bottom.getAxis());
                check(Math.abs(a-b)<1e-5,"Powered idle mixer extended along working axis "+bottom);
            }
            check(moving>8,"Powered idle mixer blades do not rotate "+bottom);
            be.setSpeed(0);
            var stopped=sample(be,0);var stoppedLater=sample(be,.75f);
            for(int i=0;i<stopped.size();i++)check(stopped.get(i).distanceToSqr(stoppedLater.get(i))<1e-10,"Unpowered blades keep spinning");
        }
        System.out.println("PASS: actual mixer blade vertices rotate while powered idle, never extend, and stop without power in all six directions");
    }
static void verifyNoFlatPortOverlay(Minecraft mc){
        for(boolean mixer:new boolean[]{false,true})for(Direction.Axis axis:new Direction.Axis[]{Direction.Axis.X,Direction.Axis.Z}){
            var state=(mixer?CatMachineBlocks.CAT_MIXER.get():CatMachineBlocks.CAT_PRESS.get()).defaultBlockState().setValue(CatProcessorBlock.SHAFT_AXIS,axis);
            var vertices=new Capture();
            mc.getBlockRenderer().renderSingleBlock(state,new PoseStack(),type->vertices,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            com.simibubi.create.content.kinetics.base.KineticBlockEntity be=mixer?new CatMixerBlockEntity(BlockPos.ZERO,state):new CatPressBlockEntity(BlockPos.ZERO,state);
            be.setLevel(mc.level);
            new CatProcessorRenderer<com.simibubi.create.content.kinetics.base.KineticBlockEntity>(null).renderSafe(be,0,new PoseStack(),type->vertices,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            for(int i=0;i+3<vertices.points.size();i+=4){
                double lo=Double.POSITIVE_INFINITY,hi=Double.NEGATIVE_INFINITY,minU=1e9,maxU=-1e9,minY=1e9,maxY=-1e9;
                for(int j=0;j<4;j++){
                    var p=vertices.points.get(i+j);double depth=component(p,axis),u=axis==Direction.Axis.X?p.z:p.x;
                    lo=Math.min(lo,depth);hi=Math.max(hi,depth);minU=Math.min(minU,u);maxU=Math.max(maxU,u);minY=Math.min(minY,p.y);maxY=Math.max(maxY,p.y);
                }
                boolean boundary=Math.abs(hi-lo)<1e-5&&(Math.abs(lo)<1e-5||Math.abs(lo-1)<1e-5);
                check(!(boundary&&(maxU-minU)*(maxY-minY)>.3),"Flat renderer overlay still covers the recessed shaft opening "+mixer+"/"+axis);
            }
        }
        System.out.println("PASS: actual block + BE port quads have no flat facade covering the recessed shaft opening");
    }
    static double component(net.minecraft.world.phys.Vec3 p,Direction.Axis a){return a==Direction.Axis.X?p.x:a==Direction.Axis.Y?p.y:p.z;}
    static java.util.List<net.minecraft.world.phys.Vec3> sample(CatMixerBlockEntity be,float partial){
        var vertices=new Capture();var ignored=new Capture();
        var moving=RenderType.entityCutoutNoCull(cn.laowu.mod.LaoWuMod.id("textures/block/cat_mixer.png"));
        new CatProcessorRenderer<CatMixerBlockEntity>(null).renderSafe(be,partial,new PoseStack(),type->type==moving?vertices:ignored,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
        return vertices.points;
    }
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static final class Capture implements com.mojang.blaze3d.vertex.VertexConsumer{
        final java.util.List<net.minecraft.world.phys.Vec3> points=new java.util.ArrayList<>();
        final java.util.List<Integer> alphas=new java.util.ArrayList<>();
        final java.util.List<net.minecraft.world.phys.Vec3> normals=new java.util.ArrayList<>();

        public com.mojang.blaze3d.vertex.VertexConsumer addVertex(float x,float y,float z){points.add(new net.minecraft.world.phys.Vec3(x,y,z));return this;}
        public com.mojang.blaze3d.vertex.VertexConsumer setColor(int r,int g,int b,int a){alphas.add(a);return this;}
        public com.mojang.blaze3d.vertex.VertexConsumer setUv(float u,float v){return this;}
        public com.mojang.blaze3d.vertex.VertexConsumer setUv1(int u,int v){return this;}
        public com.mojang.blaze3d.vertex.VertexConsumer setUv2(int u,int v){return this;}
        public com.mojang.blaze3d.vertex.VertexConsumer setNormal(float x,float y,float z){normals.add(new net.minecraft.world.phys.Vec3(x,y,z));return this;}

    }
}
