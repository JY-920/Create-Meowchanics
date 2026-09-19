package cn.laowu.mod.client;

import cn.laowu.mod.*;
import cn.laowu.mod.accessory.CatAccessoryRarity;
import net.minecraft.core.registries.BuiltInRegistries;
import cn.laowu.mod.create.*;
import cn.laowu.mod.genetics.CatStat;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import java.util.*;

/** Real client resource/model bake and GPU rendering; no saved world or network connection. */
public final class WishAdoptionVisualProbe {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static WishAdoptionOffer offer(boolean max,String reward) {
        var conditions=new ArrayList<WishAdoptionOffer.Condition>();
        int count=CatAccessoryRarity.requirements(new ItemStack(BuiltInRegistries.ITEM.get(LaoWuMod.id(reward.substring(6)))));
        conditions.add(new WishAdoptionOffer.Condition(CatStat.ATTACK,40,90));
        conditions.add(new WishAdoptionOffer.Condition(CatStat.HEALTH,60,-1));
        if(count>=3)conditions.add(new WishAdoptionOffer.Condition(CatStat.SPEED,-1,50));
        if(count>=4)conditions.add(new WishAdoptionOffer.Condition(CatStat.STAMINA,100,-1));
        return new WishAdoptionOffer(max,conditions,reward);
    }
    private static void load(WishAdoptionBoxBlockEntity box,CompoundTag tag){box.loadAdditional(tag,box.getLevel().registryAccess());}
    public static void verify(Minecraft mc){
        var previous=net.minecraft.locale.Language.getInstance();
        try{
            net.minecraft.locale.Language.inject(net.minecraft.client.resources.language.ClientLanguage.loadFrom(
                    mc.getResourceManager(),List.of("en_us","zh_cn"),false));
            var world=new AgentWatchVisualProbe.ProbeLevel();
            var player=new Player(world,BlockPos.ZERO,0,new GameProfile(UUID.randomUUID(),"wish-visual")){
                public boolean isSpectator(){return false;}
                public boolean isCreative(){return false;}
            };
            AccessoryTextVisualProbe.verify(mc);
            AssemblyVisualProbe.verify(mc,player);
            portableItem(mc,player);
            var offer=offer(true,"laowu:cat_rebirth_ootheca");
            captureGui(mc,player,offer,true,"wish-adoption-gui.png");
            captureGui(mc,player,offer(false,"laowu:cat_health_badge"),false,"wish-adoption-gui-common.png");
            captureGui(mc,player,offer(false,"laowu:cat_loot_magnet"),true,"wish-adoption-gui-rare.png");
            var itemModel=mc.getItemRenderer().getModel(new ItemStack(LaoWuMod.WISH_ADOPTION_BOX_ITEM.get()),null,null,0);
            check(itemModel.isCustomRenderer(),"Special adoption item uses custom Blockbench renderer");
            for(Direction facing:Direction.Plane.HORIZONTAL) {
                var box=new WishAdoptionBoxBlockEntity(BlockPos.ZERO,LaoWuMod.WISH_ADOPTION_BOX.get().defaultBlockState()
                        .setValue(WishAdoptionBoxBlock.FACING,facing));
                box.setLevel(world);
                var tag=new CompoundTag();tag.put("Offer",offer.save());tag.putBoolean("Locked",true);load(box,tag);
                var renderer=mc.getBlockEntityRenderDispatcher().getRenderer(box);
                check(renderer instanceof WishAdoptionBoxRenderer,"Dedicated renderer registered");
                int[][] pixels=new int[2][];
                for(int variant=0;variant<2;variant++) {
                    if(variant==1){tag.put("Offer",offer(false,"laowu:cat_blue_flame_nozzle").save());load(box,tag);}
                    pixels[variant]=capture(mc,384,384,"wish-adoption-"+facing.getName()+"-"+variant+".png",()->{
                        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(-.9F,.9F,-.9F,.9F,-10,10),VertexSorting.ORTHOGRAPHIC_Z);
                        var poses=new PoseStack();
                        poses.mulPose(Axis.XP.rotationDegrees(24));poses.mulPose(Axis.YP.rotationDegrees(205));
                        poses.translate(-.5,-.3,-.5);
                        var buffers=mc.renderBuffers().bufferSource();
                        poses.pushPose();poses.translate(0,-1,0);
                        mc.getBlockRenderer().renderSingleBlock(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),
                                poses,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
                        poses.popPose();
                        renderer.render(box,0,poses,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);buffers.endBatch();
                        check(poses.clear(),"Renderer restores incoming pose depth");
                    });
                }
                int changed=0,visible=0;
                for(int i=0;i<pixels[0].length;i++){if(pixels[0][i]!=pixels[1][i])changed++;if((pixels[0][i]>>>24)>0)visible++;}
                check(visible>3000,"Actual supplied model has visible geometry: "+facing+" "+visible);
                check(changed>20&&changed<12000,"Only board icon changes with reward, visible on both card sides: "+facing+" "+changed);
            }
            System.out.println("PASS: wish adoption: three rarity GUIs with six icons, pixel digits and dim unused rows, vanilla client data, custom item bake, four facing GPU models and two dynamic board rewards");
        }catch(Exception error){throw new IllegalStateException(error);}
        finally{net.minecraft.locale.Language.inject(previous);}
    }

    private static void portableItem(Minecraft mc,Player player)throws Exception {
        var renderer=new WishAdoptionBoxItemRenderer(mc.getBlockEntityRenderDispatcher(),mc.getEntityModels());
        int[][] pixels=new int[2][];
        for(int i=0;i<2;i++){
            var stack=new ItemStack(LaoWuMod.WISH_ADOPTION_BOX_ITEM.get());
            var card=offer(i==0,i==0?"laowu:cat_rebirth_ootheca":"laowu:cat_blue_flame_nozzle");
            cn.laowu.mod.item.WishAdoptionBoxBlockItem.writeCard(stack,card,i==0);
            var before=cn.laowu.mod.item.WishAdoptionBoxBlockItem.cardData(stack);
            var lines=new ArrayList<Component>();
            stack.getItem().appendHoverText(stack,Item.TooltipContext.of(player.level()),lines,TooltipFlag.NORMAL);
            var texts=lines.stream().map(Component::getString).toList();
            check(texts.contains(i==0?"MAX":"NOW")&&texts.stream().anyMatch(t->t.contains(card.rewardStack().getHoverName().getString())),
                    "Inventory tooltip exposes the actual mode and reward");
            check(texts.stream().anyMatch(t->t.contains("40≤")&&t.contains("≤90")),"Inventory tooltip includes exact range");
            check(texts.contains("交易已锁定")==(i==0),"Lock tooltip agrees with actual card");
            pixels[i]=capture(mc,384,384,"wish-portable35-"+i+".png",()->{
                RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,1,0,1,-10,10),VertexSorting.ORTHOGRAPHIC_Z);
                var poses=new PoseStack();var buffers=mc.renderBuffers().bufferSource();
                renderer.renderByItem(stack,ItemDisplayContext.GUI,poses,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
                buffers.endBatch();check(poses.clear(),"Portable item restores pose depth");
            });
            check(before.equals(cn.laowu.mod.item.WishAdoptionBoxBlockItem.cardData(stack)),"Tooltip and renderer are entirely read-only");
        }
        int changed=0,visible=0;
        for(int i=0;i<pixels[0].length;i++){if(pixels[0][i]!=pixels[1][i])changed++;if((pixels[0][i]>>>24)>0)visible++;}
        check(visible>3000&&changed>20&&changed<16000,"Portable model renders actual stored reward: "+changed+" / "+visible);
        var empty=new ItemStack(LaoWuMod.WISH_ADOPTION_BOX_ITEM.get());
        empty.getItem().appendHoverText(empty,Item.TooltipContext.of(player.level()),new ArrayList<>(),TooltipFlag.NORMAL);
        check(cn.laowu.mod.item.WishAdoptionBoxBlockItem.offer(empty)==null,"Legacy tooltip cannot generate offers client-side");
        System.out.println("PASS: portable Wish GPU reward, NOW/MAX/range/lock tooltips, no client-side mutations");
    }

    private static void captureGui(Minecraft mc,Player player,WishAdoptionOffer offer,boolean locked,String file)throws Exception {
        var menu=new WishAdoptionBoxMenu(99,player.getInventory(),(WishAdoptionBoxBlockEntity)null);
        menu.setData(0,offer.maximum()?2:1);menu.setData(1,locked?1:0);menu.setData(2,offer.conditions().size());
        int item=Item.getId(offer.rewardStack().getItem());menu.setData(3,item&65535);menu.setData(4,item>>>16);
        for(int i=0;i<offer.conditions().size();i++){
            var c=offer.conditions().get(i);menu.setData(5+i*3,c.stat().ordinal()+1);
            menu.setData(6+i*3,c.min());menu.setData(7+i*3,c.max());
        }
        check(menu.offer().equals(offer)&&menu.locked()==locked,"Client reconstructs complete card from vanilla menu data");
        menu.setData(6,-1);check(menu.offer().conditions().get(0).min()==-1,"Signed sentinel survives client data");
        menu.setData(6,40);
        var screen=new WishAdoptionBoxScreen(menu,player.getInventory(),Component.translatable("container.laowu.wish_adoption_box"));
        screen.init(mc,320,310);
        for(int i=0;i<18;i++){
            var slot=menu.getSlot(i);int index=i%9;
            check(slot.x==(i<9?27:92)+(index%3)*20&&slot.y==(i<9?91:97)+(index/3)*20,
                    "Real item positions and vanilla hover/click rectangles use requested offsets");
        }
        check(WishAdoptionBoxScreen.REWARD_X==116&&WishAdoptionBoxScreen.REWARD_Y==44,"Preview and tooltip move up one pixel");
        int[] guiPixels=capture(mc,640,620,file,()->{
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,320,310,0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
            var g=new GuiGraphics(mc,mc.renderBuffers().bufferSource());
            g.pose().translate(0,0,-11000);
            g.fill(0,0,320,310,0xFF24282D);
            screen.renderBg(g,0,-100,-100);
            g.pose().pushPose();g.pose().translate(72,(310-WishAdoptionBoxMenu.SCREEN_HEIGHT)/2,0);
            screen.renderLabels(g,-100,-100);g.pose().popPose();
            try{
                var field=WishAdoptionBoxScreen.class.getDeclaredField("lock");field.setAccessible(true);
                ((Button)field.get(screen)).render(g,-100,-100,0);
            }catch(Exception error){throw new IllegalStateException(error);}
            for(int i=9;i<18;i++){
                var slot=menu.getSlot(i);g.renderItem(offer.rewardStack(),72+slot.x,(310-WishAdoptionBoxMenu.SCREEN_HEIGHT)/2+slot.y);
            }
            g.flush();
        });
        // The former exterior title area must now match the cleared GUI background exactly.
        int background=guiPixels[0];
        for(int y=1;y<24;y++)for(int x=165;x<335;x++)
            check(guiPixels[(620-1-y)*640+x]==background,"No rendered exterior box title remains");
    }
    static int[] capture(Minecraft mc,int width,int height,String name,Runnable draw)throws Exception{
        TextureTarget output=null;int[] pixels=new int[width*height];
        var projection=new Matrix4f(RenderSystem.getProjectionMatrix());var sorting=RenderSystem.getVertexSorting();
        var view=RenderSystem.getModelViewStack();view.pushMatrix();view.identity();RenderSystem.applyModelViewMatrix();
        float[] clear=new float[4];GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,clear);
        double depth=GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        try(var guard=new CatPerformanceOutline.State();var framebuffer=new PerformanceSceneSnapshot.Target()){
            output=new TextureTarget(width,height,true,Minecraft.ON_OSX);output.bindWrite(true);
            RenderSystem.clearColor(0,0,0,0);RenderSystem.clearDepth(1);RenderSystem.depthMask(true);
            RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT,Minecraft.ON_OSX);
            RenderSystem.setShaderColor(1,1,1,1);RenderSystem.enableDepthTest();RenderSystem.enableCull();
            draw.run();
            try(var image=new NativeImage(width,height,false)){
                RenderSystem.bindTexture(output.getColorTextureId());image.downloadTexture(0,false);
                for(int x=0;x<width;x++)for(int y=0;y<height;y++)pixels[y*width+x]=image.getPixelRGBA(x,y);
                image.flipY();image.writeToFile(java.nio.file.Path.of(name));
            }
        }finally{
            if(output!=null)output.destroyBuffers();
            view.popMatrix();RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(projection,sorting);
            RenderSystem.clearColor(clear[0],clear[1],clear[2],clear[3]);RenderSystem.clearDepth(depth);
        }
        return pixels;
    }
    private WishAdoptionVisualProbe(){}
}
