package cn.laowu.mod.client;
import cn.laowu.mod.create.*;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import java.util.List;
public final class CreatureTransmitterClientProbe {
    private static int ticks;
    private static boolean finished;
    private static boolean physicallyRemoved;
    private static BlockPos pos;
    public static boolean tick(Minecraft mc)throws Exception{
        if(finished)return true;
        if(ticks++==0){
            mc.options.pauseOnLostFocus=false;
            pos=mc.player.blockPosition().offset(2,0,0);
            mc.getSingleplayerServer().submit(()->{
                var level=mc.getSingleplayerServer().overworld();
                level.setBlockAndUpdate(pos,CreatureTransmitterRegistration.BLOCK.get().defaultBlockState());
            }).join();
            mc.level.setBlock(pos,CreatureTransmitterRegistration.BLOCK.get().defaultBlockState(),3);
            check(mc.getBlockRenderer().getBlockModel(mc.level.getBlockState(pos))!=mc.getModelManager().getMissingModel(),"Block model bakes");
            mc.player.setItemInHand(InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
            CreatureTransmitterRegistration.BLOCK.get().interact(mc.level,pos,mc.player,InteractionHand.MAIN_HAND);
        }
        if(ticks==5){
            check(mc.screen instanceof CreatureTransmitterScreen,"Right-click opens screen");
            var screen=(CreatureTransmitterScreen)mc.screen;screen.init(mc,600,400);
            screen.tick();
            check(net.createmod.catnip.outliner.Outliner.getInstance().getOutlines().size()>=96,"Range preview is a spherical wireframe, not a bounding cube");
            check(mc.getBlockEntityRenderDispatcher().getRenderer(mc.level.getBlockEntity(pos))!=null,"Transmitter has an indicator light renderer");
            var inputs=screen.children().stream().filter(ScrollInput.class::isInstance).map(ScrollInput.class::cast).toList();
            check(inputs.size()==4,"Native threshold/radius and output-mode inputs");
            check(inputs.get(3).getHeight()==18&&inputs.get(3).getY()<inputs.get(1).getY(),
                "One independent mode selector above both count endpoints");
            inputs.get(1).setState(4);inputs.get(1).onChanged();
            inputs.get(2).setState(5);inputs.get(2).onChanged();
            capture(mc,screen,"transmitter-torch-threshold-high.png",600,400);
            capture(mc,screen,"transmitter-torch-threshold-low.png",600,400);
            inputs.get(3).setState(1);inputs.get(3).onChanged();
            capture(mc,screen,"transmitter-torch-analog-high.png",600,400);
            capture(mc,screen,"creature-transmitter-native-screen.png",600,400);
            screen.onClose();
            var local=(CreatureTransmitterBlockEntity)mc.level.getBlockEntity(pos);
            var oldHit=mc.hitResult;
            try {
                mc.hitResult=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0,.5,0),
                    net.minecraft.core.Direction.UP,pos,false);
                check(CreatureTransmitterFilterSlot.hits(local.getBlockState(),(net.minecraft.world.phys.BlockHitResult)mc.hitResult),
                    "Top slot hit matches authored centre");
                check(CreatureTransmitterFilterSlot.INSTANCE.getLocalOffset(mc.level,pos,local.getBlockState())
                    .distanceTo(new net.minecraft.world.phys.Vec3(.5,15.5/16,.5))<1e-6,"Native render transform is centred on top");
                com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer.tick();
                var outliner=net.createmod.catnip.outliner.Outliner.getInstance();
                var entry=outliner.getOutlines().values().stream().filter(e->e.getOutline() instanceof
                    com.simibubi.create.foundation.blockEntity.behaviour.ValueBox.ItemValueBox).findFirst().orElse(null);
                check(entry!=null&&entry.getOutline() instanceof com.simibubi.create.foundation.blockEntity.behaviour.ValueBox,
                    "Top slot gets native white hover plate");
                var box=(com.simibubi.create.foundation.blockEntity.behaviour.ValueBox)entry.getOutline();
                check(!box.isPassive&&box.getOutline()==com.simibubi.create.foundation.gui.AllIcons.VALUE_BOX_HOVER_4PX,
                    "Top slot native plate is active and four pixels wide");
                mc.hitResult=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos).add(.4,.5,0),
                    net.minecraft.core.Direction.UP,pos,false);
                com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer.tick();
                check(outliner.getOutlines().values().stream().filter(e->e.getOutline() instanceof
                    com.simibubi.create.foundation.blockEntity.behaviour.ValueBox.ItemValueBox)
                    .allMatch(e->((com.simibubi.create.foundation.blockEntity.behaviour.ValueBox)e.getOutline()).isPassive),
                    "Outside top slot leaves only passive native outlines");
            } finally {mc.hitResult=oldHit;}
            mc.player.input.shiftKeyDown=true;
            CreatureTransmitterRegistration.BLOCK.get().interact(mc.level,pos,mc.player,InteractionHand.MAIN_HAND);
            mc.player.input.shiftKeyDown=false;
            check(CreatureTransmitterRange.isPinned(pos),"Sneak click pins sphere after UI closes");
            CreatureTransmitterRange.update(net.minecraft.Util.getMillis()+179_000);
            check(CreatureTransmitterRange.isPinned(pos),"Range remains before three minutes");
            CreatureTransmitterRange.update(net.minecraft.Util.getMillis()+180_001);
            check(!CreatureTransmitterRange.isPinned(pos),"Range expires at three minutes");
            CreatureTransmitterRange.toggle(local);CreatureTransmitterRange.toggle(local);
            check(!CreatureTransmitterRange.isPinned(pos),"Second toggle removes range");
        }
        if(ticks==6){
            mc.getSingleplayerServer().submit(()->{
                var be=(CreatureTransmitterBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(pos);
                var filter=new net.minecraft.world.item.ItemStack(cn.laowu.mod.LaoWuMod.CREATURE_FILTER.get());
                cn.laowu.mod.item.CreatureFilterRules.grouped(java.util.List.of(new cn.laowu.mod.item.CreatureFilterRules.Group(null,
                    cn.laowu.mod.item.CreatureFilterRules.Mode.WHITELIST_ALL,java.util.List.of()))).write(filter);
                be.setFilter(filter);
            }).join();
        }
        if(ticks>=15&&!physicallyRemoved){
            var local=(CreatureTransmitterBlockEntity)mc.level.getBlockEntity(pos);
            if(local.getFilter().isEmpty()){check(ticks<160,"Installed filter never synchronized");return false;}
            mc.getSingleplayerServer().submit(()->{
                var level=mc.getSingleplayerServer().overworld();
                var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                player.setItemInHand(InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
                player.setShiftKeyDown(false);
                CreatureTransmitterRegistration.BLOCK.get().interact(level,pos,player,InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0,.5,0),
                        net.minecraft.core.Direction.UP,pos,false));
            }).join();
            physicallyRemoved=true;
        }
        if(ticks>=30){
            if(!((CreatureTransmitterBlockEntity)mc.level.getBlockEntity(pos)).getFilter().isEmpty()){
                check(ticks<160,"Physically removed filter remains rendered after server synchronization");return false;
            }
            mc.getSingleplayerServer().submit(()->{
                var be=(CreatureTransmitterBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(pos);
                check(be.getRadius()==5&&be.getUpper()==4,"Live packet applies server configuration");
                check(be.getOutputMode()==CreatureTransmitterBlockEntity.OutputMode.ANALOG,"Live packet saves analog mode");
            }).join();
            var be=(CreatureTransmitterBlockEntity)mc.level.getBlockEntity(pos);
            if((be.getRadius()!=5||be.getUpper()!=4)&&ticks<160)return false;
            check(be.getRadius()==5&&be.getUpper()==4,"Server synchronization reaches client: "+be.getRadius()+"/"+be.getUpper());
            for(int level:new int[]{0,1,7,15}){
                var sample=lightEntity(mc,level);
                var vertices=new CatMixerIdleProbe.Capture();
                mc.getBlockEntityRenderDispatcher().getRenderer(sample).render(sample,0,new com.mojang.blaze3d.vertex.PoseStack(),t->vertices,0,0);
                check(vertices.points.size()==(level==0?0:16),"Only four red faces emit indicator quads");
                check(vertices.alphas.stream().allMatch(a->Math.abs(a-level*17)<=1),"Indicator brightness tracks actual signal");
                if(level>0)check(vertices.normals.stream().map(v->new net.minecraft.core.BlockPos((int)Math.round(v.x),(int)Math.round(v.y),(int)Math.round(v.z))).distinct().count()==4,"Indicator faces point in all four directions");
            }
            capture(mc,null,"creature-transmitter-lights.png",600,250);
            var screen=new CreatureTransmitterScreen(be);mc.setScreen(screen);screen.init(mc,320,240);
            for(var child:screen.children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget w)
                check(w.getX()>=0&&w.getY()>=0&&w.getX()+w.getWidth()<=320&&w.getY()+w.getHeight()<=240,"Compact widget bounds");
            capture(mc,screen,"creature-transmitter-compact-screen.png",320,240);screen.onClose();
            CreatureTransmitterRange.toggle(be);
            var previousLevel=mc.level;
            try{mc.level=null;CreatureTransmitterRange.update(net.minecraft.Util.getMillis());
                check(!CreatureTransmitterRange.isPinned(pos),"World disconnect clears persistent ranges");}
            finally{mc.level=previousLevel;}
            CreatureTransmitterRange.toggle(be);
            mc.level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            CreatureTransmitterRange.update(net.minecraft.Util.getMillis());
            check(!CreatureTransmitterRange.isPinned(pos),"Destroyed block removes persistent range");
            System.out.println("PASS: CREATURE TRANSMITTER CLIENT native GUI, model, live packet/save/reopen and compact layout");
            finished=true;
            return true;
        }
        return false;
    }
    private static void check(boolean c,String m){if(!c)throw new AssertionError(m);}
    private static CreatureTransmitterBlockEntity lightEntity(Minecraft mc,int count){
        var be=new CreatureTransmitterBlockEntity(pos,CreatureTransmitterRegistration.BLOCK.get().defaultBlockState());be.setLevel(mc.level);
        var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("Upper",15);tag.putInt("Radius",8);tag.putInt("Count",count);tag.putString("OutputMode","analog");
        be.readClient(tag);return be;
    }
    private static void lights(Minecraft mc,GuiGraphics g){
        int[] levels={0,1,7,15};var buffers=mc.renderBuffers().bufferSource();
        for(int i=0;i<4;i++){
            var be=lightEntity(mc,levels[i]);
            g.drawString(mc.font,"Signal "+levels[i],45+i*150,215,0xffffff,false);
            var pose=g.pose();pose.pushPose();pose.translate(75+i*150,175,0);pose.scale(95,-95,95);
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(25));pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(225));pose.translate(-.5,0,-.5);
            mc.getBlockRenderer().renderSingleBlock(be.getBlockState(),pose,buffers,0,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            buffers.endBatch();
            mc.getBlockEntityRenderDispatcher().getRenderer(be).render(be,0,pose,buffers,0,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            buffers.endBatch();
            pose.popPose();
        }
        buffers.endBatch();
    }
    private static void capture(Minecraft mc, CreatureTransmitterScreen screen, String filename, int width, int height) throws Exception {
        var projection = new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix());
        var sorting = com.mojang.blaze3d.systems.RenderSystem.getVertexSorting();
        float fogStart=com.mojang.blaze3d.systems.RenderSystem.getShaderFogStart(),fogEnd=com.mojang.blaze3d.systems.RenderSystem.getShaderFogEnd();
        var view = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        view.pushPose(); view.setIdentity();
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        com.mojang.blaze3d.pipeline.TextureTarget output = null;
        try (var guard = new CatPerformanceOutline.State(); var framebuffer = new PerformanceSceneSnapshot.Target()) {
            output = new com.mojang.blaze3d.pipeline.TextureTarget(width * 2, height * 2, true, Minecraft.ON_OSX);
            output.bindWrite(true);
            com.mojang.blaze3d.systems.RenderSystem.clearColor(.08f, .1f, .12f, 1);
            com.mojang.blaze3d.systems.RenderSystem.clearDepth(1);
            com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
            com.mojang.blaze3d.systems.RenderSystem.clear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT | org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0, width, height, 0, 1000, 21000),
                    com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1, 1, 1, 1);
            // GUI orthographic geometry sits at z=-11000, outside the world's fog range.
            com.mojang.blaze3d.systems.RenderSystem.setShaderFogStart(30000);
            com.mojang.blaze3d.systems.RenderSystem.setShaderFogEnd(40000);
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            var tooltipLines=new java.util.ArrayList<net.minecraft.network.chat.Component>();
            var graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource()) {
                @Override public void renderComponentTooltip(net.minecraft.client.gui.Font font,java.util.List<net.minecraft.network.chat.Component> lines,int x,int y) {
                    tooltipLines.addAll(lines);super.renderComponentTooltip(font,lines,x,y);
                }
            };
            graphics.pose().translate(0, 0, -11000);
            if(screen==null)lights(mc,graphics);
            else if(filename.contains("-torch-")) {
                var inputs=screen.children().stream().filter(ScrollInput.class::isInstance).map(ScrollInput.class::cast).toList();
                boolean high=filename.contains("-high");
                var input=inputs.get(high?1:0);
                screen.render(graphics,input.getX()-22,input.getY()+5,0);
                var wanted=net.minecraft.network.chat.Component.translatable(filename.contains("-analog-")?
                    "gui.laowu.creature_transmitter.endpoint":"create.gui.threshold_switch.power_"+(high?"on":"off")+"_when",high?15:0).getString();
                check(tooltipLines.stream().anyMatch(c->c.getString().equals(wanted)),"Torch tooltip follows current output mode: "+wanted);
            } else screen.render(graphics,-100,-100,0);
            graphics.flush();
            try (var image = new com.mojang.blaze3d.platform.NativeImage(width * 2, height * 2, false)) {
                com.mojang.blaze3d.systems.RenderSystem.bindTexture(output.getColorTextureId());
                image.downloadTexture(0, false); image.flipY();
                image.writeToFile(java.nio.file.Path.of(filename));
                if(screen==null){
                    int r0=image.getPixelRGBA(86,270)&255,r1=image.getPixelRGBA(386,270)&255;
                    int r7=image.getPixelRGBA(686,270)&255,r15=image.getPixelRGBA(986,270)&255;
                    check(r0<=r1&&r1<r7&&r7<r15&&r15-r0>25,"Dark GPU indicator brightness must increase 0/1/7/15: "+r0+","+r1+","+r7+","+r15);
                }

            }
        } finally {
            if (output != null) output.destroyBuffers();
            com.mojang.blaze3d.systems.RenderSystem.setShaderFogStart(fogStart);
            com.mojang.blaze3d.systems.RenderSystem.setShaderFogEnd(fogEnd);
            view.popPose();
            com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(projection, sorting);
        }
    }
}
