package cn.laowu.mod.client;

import cn.laowu.mod.*;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.item.CatTraitTokenItem;
import cn.laowu.mod.network.ModNetwork;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Inventory;
import java.util.*;

/** Pixel-authored tabs above the standard player inventory; preview is always a detached cat. */
public final class CatEditorScreen extends AbstractContainerScreen<CatEditorMenu> {
    private static final ResourceLocation ATLAS=LaoWuMod.id("textures/gui/cat_editor.png");
    private static final int INK=0x604832;
    private final List<IconButton> traitButtons=new ArrayList<>();
    private final List<Button> appearanceButtons=new ArrayList<>();
    private Cat previewCat;
    private IconButton reset, confirm, traitsTab, appearanceTab, close;
    private float uiScale=1;
    private List<FormattedCharSequence> controlTooltip;
    private ClientTooltipPositioner controlTooltipPositioner;
    private static int rowY(int row){return row==0?27:28+row*29;}

    public CatEditorScreen(CatEditorMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);imageWidth=408;imageHeight=303;
    }
    private static Component text(String key){return Component.translatable("gui.laowu.cat_editor."+key);}
    @Override protected void init() {
        uiScale=Math.min(1F,Math.min((width-8)/408F,(height-8)/303F));
        width=(int)Math.ceil(width/uiScale);height=(int)Math.ceil(height/uiScale);
        super.init();traitButtons.clear();appearanceButtons.clear();
        traitsTab=icon(62,38,23,24,text("traits"),b->send(0),-1,0);
        appearanceTab=icon(62,73,23,24,text("appearance"),b->send(1),-1,0);
        close=icon(330,25,10,10,text("close"),b->onClose(),-1,0);
        for(int row=0;row<4;row++) {
            final int r=row;
            var b=icon(181,rowY(row),28,27,text("extract"),button->{
                int index=menu.installedIndex(r);send(index>=0?1000+index:20+r);
            },133,58);
            traitButtons.add(b);
        }
        for(int region=0;region<12;region++) {
            int x=61+(region/6)*144,y=15+(region%6)*25;
            final int r=region;
            Button b=addRenderableWidget(new ChoiceButton(leftPos+x,topPos+y,83,23,r));
            appearanceButtons.add(b);
        }
        reset=icon(331,138,28,26,text("reset"),b->send(CatEditorMenu.RESET),352,403);
        confirm=icon(367,138,28,26,text("confirm"),b->send(CatEditorMenu.COMMIT),388,403);
        refreshPreview();refreshButtons();
    }
    private IconButton icon(int x,int y,int w,int h,Component title,Button.OnPress press,int u,int v) {
        var b=addRenderableWidget(new IconButton(leftPos+x,topPos+y,w,h,title,press,u,v));
        b.setTooltip(Tooltip.create(title));return b;
    }
    private void send(int action) {
        if(minecraft==null||minecraft.player==null||minecraft.gameMode==null)return;
        boolean accepted=menu.clickMenuButton(minecraft.player,action);
        if((action>=CatEditorMenu.SELECT||action==CatEditorMenu.RESET||action==CatEditorMenu.COMMIT)&&!accepted)return;
        ModNetwork.sendCatEditorAction(menu.containerId,action);
        refreshButtons();refreshPreview();
    }
    private void refreshButtons() {
        boolean traits=menu.page()==0;
        traitsTab.setX(leftPos+(traits?62:0));traitsTab.setY(topPos+38);
        appearanceTab.setX(leftPos+(traits?62:0));appearanceTab.setY(topPos+73);
        close.setX(leftPos+(traits?330:393));close.setY(topPos+25);
        for(int row=0;row<4;row++) {
            var b=traitButtons.get(row);boolean occupied=menu.installedIndex(row)>=0;
            b.visible=traits&&menu.traitsReady()&&row<=menu.installedCount();
            b.u=occupied?133:445;b.v=occupied?58:117;
            b.setMessage(text(occupied?"extract":"install"));
            var stack=menu.input.getItem(row);
            b.active=occupied?stack.isEmpty():minecraft!=null&&minecraft.player!=null
                    &&CatEditorTraits.canInstall(menu.cat(minecraft.player),stack);
            b.setTooltip(Tooltip.create(occupied?text("extract_hint"):riskHint(row)));
        }
        for(var b:appearanceButtons){b.visible=!traits;b.active=menu.appearanceReady();}
        reset.visible=confirm.visible=!traits;
        reset.active=confirm.active=menu.appearanceReady();
    }
    private Component riskHint(int row) {
        var rarity=bottleRarity(row);
        return text(rarity==null?"install_hint":"risk."+rarity.serializedName());
    }
    private CatTraitRarity bottleRarity(int row) {
        var id=CatTraitTokenItem.traitId(menu.input.getItem(row));
        if(id==null)return null;
        var trait=CatTraitRegistry.resolve(id,true);
        return trait==null?null:trait.rarity();
    }
    @Override protected void containerTick(){super.containerTick();refreshPreview();refreshButtons();}
    private void refreshPreview() {
        if(minecraft==null||minecraft.level==null||minecraft.player==null)return;
        Cat source=menu.cat(minecraft.player);
        if(source==null){previewCat=null;return;}
        menu.observeAppearance(source);
        if(previewCat==null)previewCat=EntityType.CAT.create(minecraft.level);
        if(previewCat==null)return;
        previewCat.setVariant(source.getVariant());previewCat.setAge(source.getAge());
        previewCat.setTame(source.isTame(),false);previewCat.setOwnerUUID(source.getOwnerUUID());
        var appearance=new net.minecraft.nbt.CompoundTag();previewCat.addAdditionalSaveData(appearance);appearance.putByte("CollarColor",(byte)source.getCollarColor().getId());previewCat.readAdditionalSaveData(appearance);
        CatGenomeData.set(previewCat,menu.pendingGenome());
        CatTraitData.set(previewCat,CatTraitData.read(source).orElse(CatTraitProfile.EMPTY));
        previewCat.setOrderedToSit(false);previewCat.setInSittingPose(false);previewCat.setOnGround(true);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        // NeoForge's container render draws its background itself, under the same scale.
        g.pose().pushPose();g.pose().scale(uiScale,uiScale,1);
        int mx=(int)(mouseX/uiScale),my=(int)(mouseY/uiScale);
        super.render(g,mx,my,partial);renderTooltip(g,mx,my);
        if(menu.page()==0)CatTraitCardRenderer.renderTooltip(g,font,menu.displayedTraits(),
                leftPos+104,topPos+27,29,mx,my);
        if(controlTooltip!=null) {
            g.renderTooltip(font,controlTooltip,(w,h,x,y,tw,th)->
                    controlTooltipPositioner.positionTooltip(width,height,x,y,tw,th),mx,my);
            controlTooltip=null;
        }
        g.pose().popPose();
    }
    /** Screen's deferred tooltip pass runs after our pose is popped; draw it inside this pass instead. */
    @Override public void setTooltipForNextRenderPass(List<FormattedCharSequence> lines,ClientTooltipPositioner positioner,boolean priority) {
        if(controlTooltip==null||priority){controlTooltip=lines;controlTooltipPositioner=positioner;}
    }
    @Override public boolean mouseClicked(double x,double y,int button){return super.mouseClicked(x/uiScale,y/uiScale,button);}
    @Override public boolean mouseReleased(double x,double y,int button){return super.mouseReleased(x/uiScale,y/uiScale,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){return super.mouseDragged(x/uiScale,y/uiScale,button,dx/uiScale,dy/uiScale);}
    @Override public void mouseMoved(double x,double y){super.mouseMoved(x/uiScale,y/uiScale);}
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){return super.mouseScrolled(x/uiScale,y/uiScale,dx,dy);}
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY) {
        refreshButtons();
        if(menu.page()==0) {
            // Preserve the authored page, but never submit its example cards/arrows to the GPU.
            stretch(g,leftPos+62,topPos,283,26,14,31,283,26);
            stretch(g,leftPos+62,topPos+26,41,119,14,57,41,119);
            stretch(g,leftPos+252,topPos+26,93,119,204,57,93,119);
            stretch(g,leftPos+62,topPos+145,283,56,14,176,283,56);
            stretch(g,leftPos+103,topPos+26,149,119,210,180,1,1);
            CatTraitCardRenderer.renderCards(g,font,menu.displayedTraits(),leftPos+104,topPos+27,29);
            Cat source=minecraft==null||minecraft.player==null?null:menu.cat(minecraft.player);
            boolean maskNow=false,maskMax=false;
            int row=menu.installedCount();
            if(row<4&&traitButtons.get(row).isMouseOver(mouseX,mouseY)) {
                var rarity=bottleRarity(row);maskNow=rarity==CatTraitRarity.EXCELLENT||rarity==CatTraitRarity.GOOD;
                maskMax=rarity==CatTraitRarity.EXCELLENT;
            }
            // Keep the authored noticeboard header and border. Only its dynamic values change.
            CatStatsGoggleOverlay.renderEditorPanel(g,source,leftPos+281,topPos+38,maskNow,maskMax);
            for(int i=0;i<4;i++)if(menu.getSlot(i).isActive())
                stretch(g,leftPos+231,topPos+rowY(i)+5,20,21,183,63,20,21);
        } else {
            stretch(g,leftPos,topPos,408,201,21,265,408,201);
            if(previewCat!=null) {
                int scale=Math.max(8,(int)(38/Math.max(1,previewCat.getBbHeight()/.7F)));
                float yaw=(float)Math.atan((leftPos+363-mouseX)/40F);
                float pitch=(float)Math.atan((topPos+76-mouseY)/40F);
                var camera=new org.joml.Quaternionf().rotationX(pitch*20*(float)Math.PI/180);
                previewCat.yBodyRot=180+yaw*20;previewCat.setYRot(180+yaw*40);
                previewCat.setXRot(-pitch*20);previewCat.yHeadRot=previewCat.getYRot();previewCat.yHeadRotO=previewCat.yHeadRot;
                InventoryScreen.renderEntityInInventory(g,leftPos+363,topPos+82,scale,
                        new org.joml.Vector3f(0,previewCat.getBbHeight()/2+.0625F,0),
                        new org.joml.Quaternionf().rotationZ((float)Math.PI).mul(camera),camera,previewCat);
            }
        }
        AllGuiTextures.PLAYER_INVENTORY.render(g,leftPos+116,topPos+207);
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) {
        if(menu.page()==1) {
            for(int i=0;i<12;i++) {
                int x=61+(i/6)*144,y=15+(i%6)*25;
                String material=menu.selection(i)<0&&menu.input.getItem(4+i).isEmpty()
                        ?text("original_short").getString():menu.materialName(i).getString();
                g.drawString(font,text("region_short."+i),x-30,y+8,INK,false);
                g.drawString(font,font.plainSubstrByWidth(material,55),x+13,y+8,INK,false);
            }
        }
        String status=menu.statusMessage().getString();
        g.drawString(font,font.plainSubstrByWidth(status,menu.page()==0?44:70),
                menu.page()==0?285:322,25,INK,false);
    }
    private static void stretch(GuiGraphics g,int x,int y,int w,int h,int u,int v,int sw,int sh) {
        g.blit(ATLAS,x,y,w,h,u,v,sw,sh,630,512);
    }
    private class IconButton extends Button {
        int u,v;
        IconButton(int x,int y,int w,int h,Component title,OnPress press,int u,int v) {
            super(x,y,w,h,title,press,DEFAULT_NARRATION);this.u=u;this.v=v;
        }
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial) {
            if(u<0)return;
            int su=u,sv=v;
            if(!active&&(u==133||u==445)){su=u==133?471:503;sv=234;}
            stretch(g,getX(),getY(),width,height,su,sv,width,height);

        }
    }
    private final class ChoiceButton extends Button {
        final int region;
        ChoiceButton(int x,int y,int w,int h,int region) {
            super(x,y,w,h,CatEditorMenu.regionName(region),b->{},DEFAULT_NARRATION);this.region=region;
        }
        @Override public void onPress() {
            int direction=hasShiftDown()?-1:1;
            int next=Math.floorMod(menu.selection(region)+1+direction,menu.materialCount()+1)-1;
            send(menu.selectionAction(region,next));
        }
        @Override public boolean mouseClicked(double mx,double my,int button) {
            if(button==0&&visible&&active&&isMouseOver(mx,my)) {
                int direction=mx<getX()+width/2?-1:1;
                int next=Math.floorMod(menu.selection(region)+1+direction,menu.materialCount()+1)-1;
                send(menu.selectionAction(region,next));return true;
            }
            return super.mouseClicked(mx,my,button);
        }
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial) {
            // The supplied page already contains the complete selector artwork.
            setTooltip(Tooltip.create(CatEditorMenu.regionName(region).copy().append(": ").append(menu.materialName(region))));
        }
    }
}
