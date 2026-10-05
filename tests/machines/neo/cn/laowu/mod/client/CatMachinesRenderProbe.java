package cn.laowu.mod.client;

import cn.laowu.mod.create.CatMachineBlocks;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import java.util.*;

final class CatMachinesRenderProbe {
    static void verify(Minecraft mc) throws Exception {
        for(var block:List.of(CatMachineBlocks.CAT_ENCASED_SHAFT.get(),CatMachineBlocks.CAT_ENCASED_COGWHEEL.get(),CatMachineBlocks.CAT_ENCASED_LARGE_COGWHEEL.get()))
            for(var state:block.getStateDefinition().getPossibleStates()) {
                var skin=mc.getBlockRenderer().getBlockModel(state);
                var quads=new ArrayList<>(skin.getQuads(state,null,RandomSource.create(0)));
                for(var face:Direction.values())quads.addAll(skin.getQuads(state,face,RandomSource.create(0)));
                CatMachinesClientProbe.check(quads.stream().noneMatch(q->{var s=q.getSprite().contents().name().toString();
                    return s.equals("create:block/gearbox")||s.startsWith("create:block/andesite_encased_cogwheel_side")||s.contains("missingno");}),
                    "Gray fallback or missing texture must not appear on user-authored casing: "+state);
                String expected=block==CatMachineBlocks.CAT_ENCASED_SHAFT.get()
                        ?"laowu:block/cat_casing_shaft_opening"
                        :block==CatMachineBlocks.CAT_ENCASED_LARGE_COGWHEEL.get()
                        ?"laowu:block/cat_encased_cogwheel_side_connected":"laowu:block/cat_encased_cogwheel_side";
                CatMachinesClientProbe.check(quads.stream().anyMatch(q->q.getSprite().contents().name().toString().equals(expected)),
                        "User-authored sprite must be baked into every axis/cap variant: "+state);
            }
        var casing=CatMachineBlocks.CAT_CASING.get().defaultBlockState();
        var model=mc.getBlockRenderer().getBlockModel(casing);
        var isolated=model.getModelData(view(casing,Blocks.AIR.defaultBlockState()),BlockPos.ZERO,casing,ModelData.EMPTY);
        var joined=model.getModelData(view(casing,casing),BlockPos.ZERO,casing,ModelData.EMPTY);
        var unrelated=model.getModelData(view(casing,com.simibubi.create.AllBlocks.ANDESITE_CASING.getDefaultState()),BlockPos.ZERO,casing,ModelData.EMPTY);
        var a=model.getQuads(casing,Direction.UP,RandomSource.create(1),isolated,null).get(0).getVertices();
        var b=model.getQuads(casing,Direction.UP,RandomSource.create(1),joined,null).get(0).getVertices();
        var c=model.getQuads(casing,Direction.UP,RandomSource.create(1),unrelated,null).get(0).getVertices();
        CatMachinesClientProbe.check(!Arrays.equals(a,b),"Actual CT model must change UVs beside matching casing");
        CatMachinesClientProbe.check(Arrays.equals(a,c),"Cat casing must not connect to andesite casing texture");
        for(var cog:List.of(CatMachineBlocks.CAT_ENCASED_COGWHEEL.get(),CatMachineBlocks.CAT_ENCASED_LARGE_COGWHEEL.get())) {
            var closed=cog.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,Direction.Axis.Y);
            for(var face:List.of(Direction.UP,Direction.DOWN)) {
                var open=closed.setValue(face==Direction.UP
                        ?com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock.TOP_SHAFT
                        :com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock.BOTTOM_SHAFT,true);
                var emptyData=model.getModelData(view(casing,Blocks.AIR.defaultBlockState()),BlockPos.ZERO,casing,ModelData.EMPTY);
                var closedData=model.getModelData(view(casing,closed),BlockPos.ZERO,casing,ModelData.EMPTY);
                var openData=model.getModelData(view(casing,open),BlockPos.ZERO,casing,ModelData.EMPTY);
                var emptyVertices=model.getQuads(casing,face,RandomSource.create(1),emptyData,null).get(0).getVertices();
                var closedVertices=model.getQuads(casing,face,RandomSource.create(1),closedData,null).get(0).getVertices();
                var openVertices=model.getQuads(casing,face,RandomSource.create(1),openData,null).get(0).getVertices();
                CatMachinesClientProbe.check(!Arrays.equals(emptyVertices,closedVertices),"Closed cog cap connects to casing");
                CatMachinesClientProbe.check(Arrays.equals(emptyVertices,openVertices),"Open cog axle must not remove casing border: "+face);
            }
        }
        var small=CatMachineBlocks.CAT_ENCASED_COGWHEEL.get().defaultBlockState();
        var smallModel=mc.getBlockRenderer().getBlockModel(small);
        var smallData=smallModel.getModelData(view(small,small),BlockPos.ZERO,small,ModelData.EMPTY);
        var sideQuads=new ArrayList<net.minecraft.client.renderer.block.model.BakedQuad>();
        sideQuads.addAll(smallModel.getQuads(small,null,RandomSource.create(1),smallData,null));
        sideQuads.addAll(smallModel.getQuads(small,Direction.NORTH,RandomSource.create(1),smallData,null));
        var smallIsolated=smallModel.getModelData(view(small,Blocks.AIR.defaultBlockState()),BlockPos.ZERO,small,ModelData.EMPTY);
        var isolatedSides=new ArrayList<net.minecraft.client.renderer.block.model.BakedQuad>();
        isolatedSides.addAll(smallModel.getQuads(small,null,RandomSource.create(1),smallIsolated,null));
        isolatedSides.addAll(smallModel.getQuads(small,Direction.NORTH,RandomSource.create(1),smallIsolated,null));
        boolean changedSide=false;
        for(int i=0;i<sideQuads.size();i++) {
            var q=sideQuads.get(i);
            if(q.getDirection()==Direction.NORTH && q.getSprite().contents().name().getPath().contains("encased_cogwheel_side")
                    && !Arrays.equals(q.getVertices(),isolatedSides.get(i).getVertices()))changedSide=true;
        }
        CatMachinesClientProbe.check(changedSide,"Small cog slotted-side UVs must connect to an adjacent matching cog");
        capture(mc);
    }
    static BlockAndTintGetter view(BlockState center,BlockState east) {
        return (BlockAndTintGetter)java.lang.reflect.Proxy.newProxyInstance(BlockAndTintGetter.class.getClassLoader(),
                new Class[]{BlockAndTintGetter.class},(proxy,method,args)->switch(method.getName()) {
                    case "getBlockState" -> ((BlockPos)args[0]).equals(BlockPos.ZERO)?center:
                            ((BlockPos)args[0]).equals(BlockPos.ZERO.east())?east:Blocks.AIR.defaultBlockState();
                    case "getFluidState" -> Fluids.EMPTY.defaultFluidState();
                    case "getBlockEntity","getLightEngine" -> null;
                    case "getHeight" -> 384;
                    case "getMinBuildHeight" -> -64;
                    case "getShade" -> 1f;
                    case "getBlockTint" -> 0xffffff;
                    case "getModelData" -> ModelData.EMPTY;
                    case "toString" -> "cat-casing-CT-fixture";
                    default -> throw new AssertionError("Unexpected world query: "+method);
                });
    }
    static void capture(Minecraft mc) throws Exception {
        var projection=new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting=RenderSystem.getVertexSorting();
        var view=RenderSystem.getModelViewStack();view.pushMatrix();view.identity();RenderSystem.applyModelViewMatrix();
        TextureTarget output=null;
        try(var guard=new CatPerformanceOutline.State();var framebuffer=new PerformanceSceneSnapshot.Target()) {
            output=new TextureTarget(800,400,true,Minecraft.ON_OSX);output.bindWrite(true);
            RenderSystem.clearColor(.10f,.12f,.15f,1);RenderSystem.clearDepth(1);RenderSystem.depthMask(true);
            RenderSystem.clear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT,Minecraft.ON_OSX);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,800,400,0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.setShaderColor(1,1,1,1);RenderSystem.enableDepthTest();RenderSystem.enableCull();
            var basin=CatMachineBlocks.HAJI_BASIN.get().defaultBlockState();
            var states=List.of(CatMachineBlocks.CAT_CASING.get().defaultBlockState(),
                    CatMachineBlocks.CAT_ENCASED_SHAFT.get().defaultBlockState(),
                    CatMachineBlocks.CAT_ENCASED_COGWHEEL.get().defaultBlockState(),
                    CatMachineBlocks.CAT_ENCASED_LARGE_COGWHEEL.get().defaultBlockState(),basin,
                    basin.setValue(com.simibubi.create.content.processing.basin.BasinBlock.FACING,Direction.SOUTH),
                    basin.setValue(com.simibubi.create.content.processing.basin.BasinBlock.FACING,Direction.NORTH),
                    basin.setValue(com.simibubi.create.content.processing.basin.BasinBlock.FACING,Direction.EAST),
                    basin.setValue(com.simibubi.create.content.processing.basin.BasinBlock.FACING,Direction.WEST),basin);
            var buffers=mc.renderBuffers().bufferSource();
            for(int i=0;i<states.size();i++) {
                var pose=new PoseStack();pose.translate(80+(i%5)*160,100+(i/5)*200,-11000);
                pose.scale(78,-78,78);pose.mulPose(Axis.XP.rotationDegrees(25));pose.mulPose(Axis.YP.rotationDegrees(225));
                pose.translate(-.5,-.5,-.5);
                mc.getBlockRenderer().renderSingleBlock(states.get(i),pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            }
            buffers.endBatch();
            try(var image=new NativeImage(800,400,false)) {
                RenderSystem.bindTexture(output.getColorTextureId());image.downloadTexture(0,false);
                int background=image.getPixelRGBA(0,0);
                for(int i=0;i<10;i++) {
                    int visible=0;
                    for(int y=(i/5)*200;y<(i/5+1)*200;y++)for(int x=(i%5)*160;x<(i%5+1)*160;x++)
                        if(image.getPixelRGBA(x,y)!=background)visible++;
                    CatMachinesClientProbe.check(visible>500,"Every block preview emits GPU pixels, tile "+i);
                }
                image.flipY();image.writeToFile(java.nio.file.Path.of("cat-machines-preview.png"));
            }
        } finally {
            if(output!=null)output.destroyBuffers();
            view.popMatrix();RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(projection,sorting);
        }
        System.out.println("PASS: real CT adjacency UVs and ten GPU block previews");
    }
}
