package cn.laowu.mod.client;
import cn.laowu.mod.create.*;
import cn.laowu.mod.compat.jei.LaoWuJeiPlugin;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import org.joml.Matrix4f;
import java.util.*;
final class CatSixWayClientProbe {

    private static int cancelStage, cancelTicks;
    private static BlockPos cancelBase;
    private static final java.util.concurrent.atomic.AtomicBoolean fixtureReady=new java.util.concurrent.atomic.AtomicBoolean();
    /** Real integrated-server packets: obstruct a running mixer and require its client animation to stop. */
    static boolean cancellationSynced(Minecraft mc){
        mc.options.pauseOnLostFocus=false;
        if(cancelStage==0){
            cancelBase=mc.player.blockPosition().offset(4,4,4);
            var dimension=mc.level.dimension();var base=cancelBase;
            mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().getLevel(dimension);
                level.setBlockAndUpdate(base,CatMachineBlocks.HAJI_BASIN.get().defaultBlockState());
                level.setBlockAndUpdate(base.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(base.above(2),CatMachineBlocks.CAT_MIXER.get().defaultBlockState().setValue(CatProcessorBlock.SHAFT_OPEN,true));
                level.setBlockAndUpdate(base.below(),com.simibubi.create.AllBlocks.BLAZE_BURNER.getDefaultState().setValue(com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL,com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.KINDLED));
                ((com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity)level.getBlockEntity(base.below())).isCreative=true;
                level.setBlockAndUpdate(base.above(2).west(),com.simibubi.create.AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(com.simibubi.create.content.kinetics.motor.CreativeMotorBlock.FACING,Direction.EAST));
                ((com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity)level.getBlockEntity(base.above(2).west())).generatedSpeed.setValue(32);
                var basin=(HajiBasinBlockEntity)level.getBlockEntity(base);
                basin.inputInventory.insertItem(0,new ItemStack(Items.COPPER_INGOT),false);
                basin.inputInventory.insertItem(1,com.simibubi.create.AllItems.ZINC_INGOT.asStack(),false);
                basin.notifyChangeOfContents();fixtureReady.set(true);
            });
            cancelStage=1;return false;
        }
        if(!fixtureReady.get())return false;
        cancelTicks++;
        var be=mc.level.getBlockEntity(cancelBase.above(2));
        if(cancelStage==1&&be instanceof CatMixerBlockEntity mixer&&mixer.running){
            var dimension=mc.level.dimension();var base=cancelBase;
            mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getLevel(dimension).setBlockAndUpdate(base.above(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()));
            cancelStage=2;cancelTicks=0;
        }else if(cancelStage==2&&mc.level.getBlockState(cancelBase.above()).is(net.minecraft.world.level.block.Blocks.STONE)
                &&be instanceof CatMixerBlockEntity mixer&&!mixer.running){
            System.out.println("PASS: real server interruption packet stops client mixer animation");return true;
        }
        if(cancelTicks>200)throw new AssertionError("Mixer interruption did not synchronize; stage="+cancelStage);
        return false;
    }

    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static void verify(Minecraft mc)throws Exception{
        CatMachinesClientProbe.verify(mc);
        CatMixerIdleProbe.verify(mc);

        for(Direction bottom:Direction.values()){
            var state=CatMachineBlocks.HAJI_BASIN.get().defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom);
            var filter=new CatBasinFilter();
            for(Direction face:Direction.values()){
                if(face.getAxis()==bottom.getAxis())continue;
                filter.fromSide(face);
                var expected=new PoseStack();filter.transform(mc.level,BlockPos.ZERO,state,expected);
                var actual=new PoseStack();
                actual.translate(.5,.5,.5);CatMachineOrientation.rotatePose(actual,bottom);actual.translate(-.5,-.5,-.5);
                filter.renderInLocalFrame(()->filter.transform(mc.level,BlockPos.ZERO,state,actual));
                check(actual.last().pose().equals(expected.last().pose(),.0001f),"Basin filter rotated twice "+bottom+"/"+face);
                var restored=new PoseStack();filter.transform(mc.level,BlockPos.ZERO,state,restored);
                check(restored.last().pose().equals(expected.last().pose(),.0001f),"Basin filter render frame leaked into world interaction");
            }
        }
        System.out.println("PASS: filter plates render on all local sides without double rotation; world hit frame restored");

        var manager=com.simibubi.create.compat.jei.CreateJEI.runtime.getRecipeManager();
        String[][] categories={{"pressing","packing","automatic_packing"},{"mixing","automatic_shapeless","automatic_brewing"},{"mixing","packing","automatic_shapeless","automatic_brewing","automatic_packing"},{"pressing","deploying","spout_filling"}};
        Item[] items={CatMachineBlocks.CAT_PRESS_ITEM.get(),CatMachineBlocks.CAT_MIXER_ITEM.get(),CatMachineBlocks.HAJI_BASIN_ITEM.get(),CatDepotRegistration.CAT_DEPOT_ITEM.get()};
        for(int i=0;i<items.length;i++)for(String category:categories[i]){
            Item item=items[i];var type=LaoWuJeiPlugin.catMachineCategory(category);
            check(manager.createRecipeCatalystLookup(type).getItemStack().anyMatch(stack->stack.is(item)),"Missing actual JEI catalyst "+item+"/"+category);
            check(manager.getRecipeType(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create",category)).orElseThrow().equals(type),"Wrong loader-specific JEI type "+category);
        }
        for(String category:new String[]{"pressing","mixing","packing"})
            check(manager.createRecipeLookup(LaoWuJeiPlugin.catMachineCategory(category)).get().findAny().isPresent(),"Native recipes absent "+category);
        for(var block:List.of(CatMachineBlocks.CAT_PRESS.get(),CatMachineBlocks.CAT_MIXER.get(),CatMachineBlocks.HAJI_BASIN.get(),CatDepotRegistration.CAT_DEPOT.get()))
            for(var state:block.getStateDefinition().getPossibleStates())
                check(mc.getBlockRenderer().getBlockModel(state)!=mc.getModelManager().getMissingModel(),"State failed to bake "+state);
        for(boolean extended:new boolean[]{false,true})capture(mc,extended);
        capturePorts(mc);
        GiantCatVisualProbe.verify(mc);
        System.out.println("PASS: 14 JEI catalyst mappings, real Create recipes, six-way GPU shells/shafts/rod/blade/items/fluid at two cycle poses");
    }
static void capturePorts(Minecraft mc)throws Exception{
        var projection=new Matrix4f(RenderSystem.getProjectionMatrix());var sorting=RenderSystem.getVertexSorting();
        var view=RenderSystem.getModelViewStack();view.pushMatrix();view.identity();RenderSystem.applyModelViewMatrix();
        TextureTarget output=null;
        try(var guard=new CatPerformanceOutline.State();var framebuffer=new PerformanceSceneSnapshot.Target()){
            output=new TextureTarget(900,520,true,Minecraft.ON_OSX);output.bindWrite(true);
            RenderSystem.clearColor(.10f,.12f,.15f,1);RenderSystem.clearDepth(1);RenderSystem.depthMask(true);
            RenderSystem.clear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT,Minecraft.ON_OSX);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,900,520,0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.setShaderColor(1,1,1,1);RenderSystem.enableDepthTest();RenderSystem.enableCull();
            var buffers=mc.renderBuffers().bufferSource();
            for(int i=0;i<2;i++){
                var state=(i==0?CatMachineBlocks.CAT_PRESS.get():CatMachineBlocks.CAT_MIXER.get()).defaultBlockState();
                var pose=new PoseStack();pose.translate(225+i*450,205,-11000);pose.scale(200,-200,200);
                pose.mulPose(Axis.XP.rotationDegrees(20));pose.mulPose(Axis.YP.rotationDegrees(225));pose.translate(-.5,-.5,-.5);
                mc.getBlockRenderer().renderSingleBlock(state,pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
                com.simibubi.create.content.kinetics.base.KineticBlockEntity be=i==0?new CatPressBlockEntity(BlockPos.ZERO,state):new CatMixerBlockEntity(BlockPos.ZERO,state);
                be.setLevel(mc.level);be.setSpeed(64);
                new CatProcessorRenderer<com.simibubi.create.content.kinetics.base.KineticBlockEntity>(null).renderSafe(be,.25f,pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            }
            buffers.endBatch();
            try(var image=new NativeImage(900,520,false)){
                RenderSystem.bindTexture(output.getColorTextureId());image.downloadTexture(0,false);image.flipY();
                image.writeToFile(java.nio.file.Path.of("cat-machine-recessed-ports.png"));
            }
        }finally{if(output!=null)output.destroyBuffers();view.popMatrix();RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(projection,sorting);}
    }
    static void capture(Minecraft mc,boolean extended)throws Exception{
        var projection=new Matrix4f(RenderSystem.getProjectionMatrix());var sorting=RenderSystem.getVertexSorting();
        var view=RenderSystem.getModelViewStack();view.pushMatrix();view.identity();RenderSystem.applyModelViewMatrix();
        TextureTarget output=null;
        try(var guard=new CatPerformanceOutline.State();var framebuffer=new PerformanceSceneSnapshot.Target()){
            output=new TextureTarget(1200,1040,true,Minecraft.ON_OSX);output.bindWrite(true);
            RenderSystem.clearColor(.10f,.12f,.15f,1);RenderSystem.clearDepth(1);RenderSystem.depthMask(true);
            RenderSystem.clear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT,Minecraft.ON_OSX);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,1200,1040,0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.setShaderColor(1,1,1,1);RenderSystem.enableDepthTest();RenderSystem.enableCull();
            var buffers=mc.renderBuffers().bufferSource();
            for(Direction bottom:Direction.values())for(int kind=0;kind<4;kind++){
                var block=switch(kind){case 0->CatMachineBlocks.CAT_PRESS.get();case 1->CatMachineBlocks.CAT_MIXER.get();case 2->CatMachineBlocks.HAJI_BASIN.get();default->CatDepotRegistration.CAT_DEPOT.get();};
                var state=block.defaultBlockState().setValue(CatMachineOrientation.BOTTOM,bottom);
                if(kind<2)state=state.setValue(CatProcessorBlock.SHAFT_OPEN,true).setValue(CatProcessorBlock.SHAFT_AXIS,bottom.getAxis()==Direction.Axis.X?Direction.Axis.Z:Direction.Axis.X);
                var pose=new PoseStack();pose.translate(100+bottom.ordinal()*200,130+kind*260,-11000);
                pose.scale(65,-65,65);pose.mulPose(Axis.XP.rotationDegrees(25));pose.mulPose(Axis.YP.rotationDegrees(225));pose.translate(-.5,-.5,-.5);
                mc.getBlockRenderer().renderSingleBlock(state,pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
                if(kind==0){
                    var be=new CatPressBlockEntity(BlockPos.ZERO,state);be.setLevel(mc.level);
                    be.pressingBehaviour.running=extended;be.pressingBehaviour.prevRunningTicks=be.pressingBehaviour.runningTicks=120;
                    new CatProcessorRenderer<CatPressBlockEntity>(null).renderSafe(be,0,pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
                }else if(kind==1){
                    var be=new CatMixerBlockEntity(BlockPos.ZERO,state);be.setLevel(mc.level);be.running=extended;be.runningTicks=20;
                    new CatProcessorRenderer<CatMixerBlockEntity>(null).renderSafe(be,0,pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
                }else if(kind==2){
                    var be=new HajiBasinBlockEntity(BlockPos.ZERO,state);be.setLevel(mc.level);
                    be.inputInventory.insertItem(0,new ItemStack(Items.COPPER_INGOT),false);
                    be.getFilter().setFilter(new ItemStack(Items.DIAMOND));
                   be.inputTank.getPrimaryHandler().fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                    be.inputTank.getPrimaryTank().getFluidLevel().startWithValue(1);
                    var tank=be.inputTank.getPrimaryTank();
                    tank.readNBT(tank.writeNBT(mc.level.registryAccess()),mc.level.registryAccess(),true);
                    check(!be.inputTank.getPrimaryTank().getRenderedFluid().isEmpty(),"Water must actually be visible in basin probe");
                    new CatBasinRenderer(null).renderSafe(be,1,pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
                }else{
                    var be=new CatDepotBlockEntity(BlockPos.ZERO,state);be.setLevel(mc.level);be.setHeldItem(new ItemStack(Items.IRON_INGOT));
                    new CatDepotRenderer(null).renderSafe(be,0,pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
                }
            }
            buffers.endBatch();
            try(var image=new NativeImage(1200,1040,false)){
                RenderSystem.bindTexture(output.getColorTextureId());image.downloadTexture(0,false);
                int background=image.getPixelRGBA(0,0);
                for(int i=0;i<24;i++){
                    int visible=0;
                    for(int y=(i/6)*260;y<(i/6+1)*260;y++)for(int x=(i%6)*200;x<(i%6+1)*200;x++)if(image.getPixelRGBA(x,y)!=background)visible++;
                    check(visible>1000,"Empty six-way preview tile "+i);
                }
                image.flipY();image.writeToFile(java.nio.file.Path.of("cat-sixway-"+(extended?"extended":"idle")+".png"));
            }
        }finally{if(output!=null)output.destroyBuffers();view.popMatrix();RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(projection,sorting);}
    }
}
