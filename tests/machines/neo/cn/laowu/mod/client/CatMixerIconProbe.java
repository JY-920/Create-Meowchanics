package cn.laowu.mod.client;
import cn.laowu.mod.create.CatMachineBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
/** Tests actual ItemRenderer output, including inherited GUI transforms, not JSON constants. */
final class CatMixerIconProbe {
    static void verify(Minecraft mc) throws Exception {
        for(var item:java.util.List.of(CatMachineBlocks.CAT_PRESS_ITEM.get(),CatMachineBlocks.CAT_MIXER_ITEM.get())) {
            var mesh=mc.getItemRenderer().getModel(new ItemStack(item),null,null,0);
            var sprites=new java.util.HashSet<String>();
            for(var q:mesh.getQuads(null,null,net.minecraft.util.RandomSource.create(0)))sprites.add(q.getSprite().contents().name().toString());
            CatMachinesClientProbe.check(sprites.contains("create:block/axis")&&sprites.contains("create:block/axis_top"),
                "Inventory processor needs baked native shaft sides and end caps: "+item);
        }
        var vertices=new Bounds();
        mc.getItemRenderer().renderStatic(new ItemStack(CatMachineBlocks.CAT_MIXER_ITEM.get()),ItemDisplayContext.GUI,
            LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,new PoseStack(),type->vertices,null,0);
        CatMachinesClientProbe.check(vertices.count>40,"Mixer GUI must render its complete mesh");
        CatMachinesClientProbe.check(vertices.minX>=-.5 && vertices.maxX<=.5 && vertices.minY>=-.5 && vertices.maxY<=.5,
            "Mixer inventory icon spills outside 16px slot: x="+vertices.minX+".."+vertices.maxX+", y="+vertices.minY+".."+vertices.maxY);
        CatMachinesClientProbe.check(vertices.maxY-vertices.minY>.6,"Mixer icon was reduced to an unreadable speck");
        System.out.println("PASS: actual mixer GUI vertices fit inside a 16px item slot");
        capture(mc);
    }

    static void capture(Minecraft mc) throws Exception {
        var projection=new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix());
        var sorting=com.mojang.blaze3d.systems.RenderSystem.getVertexSorting();
        var view=com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        view.pushMatrix();view.identity();
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        com.mojang.blaze3d.pipeline.TextureTarget output=null;
        try(var guard=new CatPerformanceOutline.State();var framebuffer=new PerformanceSceneSnapshot.Target()){
            output=new com.mojang.blaze3d.pipeline.TextureTarget(320,160,true,Minecraft.ON_OSX);output.bindWrite(true);
            com.mojang.blaze3d.systems.RenderSystem.clearColor(.1f,.12f,.15f,1);
            com.mojang.blaze3d.systems.RenderSystem.clearDepth(1);com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
            com.mojang.blaze3d.systems.RenderSystem.clear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT,Minecraft.ON_OSX);
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0,320,160,0,1000,21000),com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            var items=new ItemStack[]{new ItemStack(CatMachineBlocks.CAT_PRESS_ITEM.get()),new ItemStack(CatMachineBlocks.CAT_MIXER_ITEM.get())};
            var buffers=mc.renderBuffers().bufferSource();
            for(int i=0;i<2;i++){
                var pose=new PoseStack();pose.translate(80+i*160,80,-11000);pose.scale(128,-128,128);
                mc.getItemRenderer().renderStatic(items[i],ItemDisplayContext.GUI,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,pose,buffers,null,0);
            }
            buffers.endBatch();
            try(var image=new com.mojang.blaze3d.platform.NativeImage(320,160,false)){
                com.mojang.blaze3d.systems.RenderSystem.bindTexture(output.getColorTextureId());image.downloadTexture(0,false);image.flipY();
                image.writeToFile(java.nio.file.Path.of("cat-mixer-gui-comparison.png"));
            }
        }finally{
            if(output!=null)output.destroyBuffers();
            view.popMatrix();
            com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(projection,sorting);
        }
    }

    private static final class Bounds implements com.mojang.blaze3d.vertex.VertexConsumer {
        int count; double minX=Double.POSITIVE_INFINITY,minY=Double.POSITIVE_INFINITY,maxX=Double.NEGATIVE_INFINITY,maxY=Double.NEGATIVE_INFINITY;
        void point(double x,double y){count++;minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);}
        public com.mojang.blaze3d.vertex.VertexConsumer addVertex(float x,float y,float z){point(x,y);return this;}
        public com.mojang.blaze3d.vertex.VertexConsumer setColor(int r,int g,int b,int a){return this;}
        public com.mojang.blaze3d.vertex.VertexConsumer setUv(float u,float v){return this;}
        public com.mojang.blaze3d.vertex.VertexConsumer setUv1(int u,int v){return this;}
        public com.mojang.blaze3d.vertex.VertexConsumer setUv2(int u,int v){return this;}
        public com.mojang.blaze3d.vertex.VertexConsumer setNormal(float x,float y,float z){return this;}
    }
}
