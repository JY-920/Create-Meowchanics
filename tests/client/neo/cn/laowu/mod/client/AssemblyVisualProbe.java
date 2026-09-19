package cn.laowu.mod.client;
import cn.laowu.mod.*;
import cn.laowu.mod.item.*;
import cn.laowu.mod.genetics.CatStat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import org.joml.Matrix4f;
import java.util.*;

/** Checks actual baked artist sprites and native progress bars on both ports. */
public final class AssemblyVisualProbe {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void progress(ItemStack stack,String name){
        stack.set(com.simibubi.create.AllDataComponents.SEQUENCED_ASSEMBLY,
                new com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe.SequencedAssembly(
                        LaoWuMod.id(name+"_sequenced_assembly"),1,.5F));
    }
    public static void verify(Minecraft mc,Player player)throws Exception{
        var names=new ArrayList<String>();
        for(var type:CatOutfitType.values())if(type!=CatOutfitType.NONE)names.add(type.id()+"_suit");
        names.add("cat_component");names.add("cat_grenade");
        var stacks=new ArrayList<ItemStack>();
        for(String name:names){
            var stack=new ItemStack(BuiltInRegistries.ITEM.get(LaoWuMod.id("incomplete_"+name)));
            check(stack.getItem() instanceof SequencedAssemblyItem,"Native intermediate "+name);
            check(mc.getItemRenderer().getModel(stack,null,null,0).getParticleIcon().contents().name()
                    .equals(LaoWuMod.id(name.equals("cat_grenade")?"item/cat_shell":"item/incomplete_"+name)),"Actual artist texture baked "+name);
            progress(stack,name);stacks.add(stack);
            check(stack.getItem().isBarVisible(stack)&&stack.getItem().getBarWidth(stack)==7,"Half-progress native item bar "+name);
        }
        int[] pixels=WishAdoptionVisualProbe.capture(mc,500,300,"assembly-intermediates.png",()->{
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,125,75,0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
            var g=new GuiGraphics(mc,mc.renderBuffers().bufferSource());g.pose().translate(0,0,-11000);
            g.fill(0,0,125,75,0xFF24282D);
            for(int i=0;i<stacks.size();i++){
                int x=4+(i%5)*25,y=4+(i/5)*25;
                g.renderItem(stacks.get(i),x,y);g.renderItemDecorations(mc.font,stacks.get(i),x,y);
            }
            g.flush();
        });
        int background=pixels[0];
        for(int i=0;i<15;i++){
            int changed=0,x0=(4+i%5*25)*4,y0=(4+i/5*25)*4;
            for(int x=x0;x<x0+64;x++)for(int y=y0;y<y0+64;y++)if(pixels[(299-y)*500+x]!=background)changed++;
            check(changed>120,"Every intermediate emits real visible GPU pixels "+names.get(i));
        }
        var filter=new ItemStack(LaoWuMod.CAT_FILTER.get());
        int[] minimum=new int[6],maximum=new int[6];Arrays.fill(maximum,300);
        minimum[CatStat.ATTACK.ordinal()]=40;maximum[CatStat.ATTACK.ordinal()]=90;
        CatFilterRules.fromValues(minimum,maximum,null,null,List.of())
                .withLogic(new CatFilterLogic(1<<CatStat.ATTACK.ordinal(),0,0)).withBaseCurrent(true).write(filter);
        var menu=new CatFilterMenu(98,player.getInventory(),filter);
        var screen=new CatFilterScreen(menu,player.getInventory(),Component.literal("Cat filter"));
        screen.init(mc,400,350);
        var field=CatFilterScreen.class.getDeclaredField("currentButton");field.setAccessible(true);
        check(((Button)field.get(screen)).getMessage().getString().equals(
                Component.translatable("gui.laowu.cat_filter.page.base_current").getString()),"Client explicitly labels imported raw NOW mode");
        check(CatFilterDescription.describe(menu.rules()).getString().contains(
                Component.translatable("gui.laowu.cat_filter.base_current").getString()),"Summary explicitly labels raw base mode");
        menu.clearContents();
        var update=CatFilterScreen.class.getDeclaredMethod("updatePageButtons");update.setAccessible(true);update.invoke(screen);
        check(((Button)field.get(screen)).getMessage().getString().equals(
                Component.translatable("gui.laowu.cat_filter.page.current").getString()),"Reset restores ordinary effective tab label");
        System.out.println("PASS: 15 artist intermediate sprites baked and GPU rendered with native progress bars; imported raw NOW labels and reset verified");
    }
    private AssemblyVisualProbe(){}
}
