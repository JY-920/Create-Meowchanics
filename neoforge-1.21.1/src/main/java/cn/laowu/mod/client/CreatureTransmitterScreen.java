package cn.laowu.mod.client;
import cn.laowu.mod.create.*;
import cn.laowu.mod.network.ModNetwork;
import com.simibubi.create.foundation.gui.*;
import com.simibubi.create.foundation.gui.widget.*;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import java.util.List;
public final class CreatureTransmitterScreen extends AbstractSimiScreen {
    private final CreatureTransmitterBlockEntity be;
    private final AllGuiTextures background=AllGuiTextures.THRESHOLD_SWITCH;
    private ScrollInput lower,upper,radius;
    private SelectionScrollInput mode;
    private boolean inverted,dirty;
    private int pending=-1;
    public CreatureTransmitterScreen(CreatureTransmitterBlockEntity be){
        super(Component.translatable("block.laowu.creature_transmitter"));this.be=be;inverted=be.isInverted();
    }
    @Override protected void init(){
        setWindowSize(background.getWidth(),background.getHeight()+24);setWindowOffset(-20,0);super.init();
        int x=guiLeft,y=guiTop;
        int lo=lower==null?be.getLower():lower.getState(),hi=upper==null?be.getUpper():upper.getState();
        int r=radius==null?be.getRadius():radius.getState();
        int m=mode==null?(be.getOutputMode()==CreatureTransmitterBlockEntity.OutputMode.ANALOG?1:0):mode.getState();
        lower=new ScrollInput(x+48,y+71,110,18).withRange(0,CreatureTransmitterBlockEntity.MAX_THRESHOLD)
            .titled(Component.translatable("create.gui.threshold_switch.lower_threshold")).setState(lo);
        upper=new ScrollInput(x+48,y+47,110,18).withRange(1,CreatureTransmitterBlockEntity.MAX_THRESHOLD+1)
            .titled(Component.translatable("create.gui.threshold_switch.upper_threshold")).setState(hi);
        lower.calling(v->{if(upper.getState()<=v)upper.setState(v+1);modified();});
        upper.calling(v->{if(lower.getState()>=v)lower.setState(v-1);modified();});
        radius=new ScrollInput(x+12,y+113,87,11).withRange(1,17)
            .titled(Component.translatable("gui.laowu.creature_transmitter.radius_hint")).setState(r).calling(v->modified());
        addRenderableWidget(lower);addRenderableWidget(upper);addRenderableWidget(radius);
        mode=new SelectionScrollInput(x+48,y+23,110,18);
        mode.forOptions(List.of(Component.translatable("gui.laowu.creature_transmitter.threshold"),
            Component.translatable("gui.laowu.creature_transmitter.analog")));
        mode.titled(Component.translatable("gui.laowu.creature_transmitter.mode")).setState(m).calling(v->modified());
        addRenderableWidget(mode);
        var flip=new IconButton(x+background.getWidth()-62,y+background.getHeight(),AllIcons.I_FLIP);
        flip.setToolTip(Component.translatable("create.gui.threshold_switch.invert_signal"));
        flip.withCallback(()->{inverted=!inverted;modified();send();});addRenderableWidget(flip);
        var confirm=new IconButton(x+background.getWidth()-33,y+background.getHeight(),AllIcons.I_CONFIRM);
        confirm.withCallback(this::onClose);addRenderableWidget(confirm);
    }
    private void modified(){dirty=true;pending=0;}
    private void send(){
        if(!dirty||radius==null)return;
        ModNetwork.configureCreatureTransmitter(be.getBlockPos(),radius.getState(),lower.getState(),upper.getState(),inverted,mode.getState()==1);
        dirty=false;pending=-1;
    }
    @Override public void removed(){send();CreatureTransmitterRange.clearPreview();super.removed();}
    @Override public void tick(){
        super.tick();
        if(be.isRemoved()||minecraft.player==null||minecraft.player.distanceToSqr(Vec3.atCenterOf(be.getBlockPos()))>64){onClose();return;}
        if(pending>=0&&++pending>=10)send();
        CreatureTransmitterRange.preview(be,radius.getState());
    }
    @Override protected void renderWindow(GuiGraphics g,int mx,int my,float partial){
        int x=guiLeft,y=guiTop;
        g.blit(background.location,x,y,0,0,background.getWidth(),20,256,256);
        for(int row=20;row<44;row++)g.blit(background.location,x,y+row,0,18,background.getWidth(),1,256,256);
        g.blit(background.location,x,y+44,0,20,background.getWidth(),background.getHeight()-20,256,256);
        g.blit(background.location,x+11,y+102,34,78,22,22,256,256);
        g.drawString(font,title,x+(background.getWidth()-font.width(title))/2,y+4,0x592424,false);
        AllGuiTextures.THRESHOLD_SWITCH_MISC_INPUTS.render(g,x+44,y+21);
        AllGuiTextures.THRESHOLD_SWITCH_MISC_INPUTS.render(g,x+44,y+45);
        AllGuiTextures.THRESHOLD_SWITCH_MISC_INPUTS.render(g,x+44,y+69);
        g.drawString(font,"≥ "+upper.getState(),x+53,y+52,0xffffff,true);
        g.drawString(font,"≤ "+lower.getState(),x+53,y+76,0xffffff,true);
        var modeName=Component.translatable("gui.laowu.creature_transmitter."+(mode.getState()==1?"analog":"threshold"));
        g.drawString(font,modeName,x+53,y+28,0xffffff,true);
        if(mode.getState()==0||be.getCount()<=lower.getState()||be.getCount()>=upper.getState())
            AllGuiTextures.THRESHOLD_SWITCH_CURRENT_STATE.render(g,x+20,y+44+((mode.getState()==0?be.isActive():be.getCount()>=upper.getState())?0:24));
        var pose=g.pose();pose.pushPose();pose.translate(x+18,y+62,200);
        dev.engine_room.flywheel.lib.transform.TransformStack.of(pose).rotateXDegrees(-22.5f).rotateYDegrees(45);
        for(int i=0;i<2;i++){
            GuiGameElement.of(Blocks.REDSTONE_TORCH.defaultBlockState().setValue(RedstoneTorchBlock.LIT,inverted^(i==0))).scale(20).render(g);
            pose.translate(0,26,0);
        }
        pose.popPose();
        var reading=Component.translatable("gui.laowu.creature_transmitter.count",be.getCount()).append(" → "+be.getSignal());
        float readingScale=Math.min(1,97f/Math.max(1,font.width(reading)));
        pose.pushPose();pose.translate(x+13,y+103,0);pose.scale(readingScale,readingScale,1);
        g.drawString(font,reading,0,0,0x592424,false);pose.popPose();
        g.drawString(font,Component.translatable("gui.laowu.creature_transmitter.radius",radius.getState()),x+13,y+114,0x592424,false);
        GuiGameElement.of(new ItemStack(CreatureTransmitterRegistration.ITEM.get())).scale(5).at(x+background.getWidth()+6,y+background.getHeight()-56,-200).render(g);
        if(mx>=x+13&&mx<x+110&&my>=y+101&&my<y+113)
            g.renderComponentTooltip(font,List.of(Component.translatable("gui.laowu.creature_transmitter.signal",be.getSignal())),mx,my);
        if(mx>=x+13&&mx<x+42&&my>=y+44&&my<y+91){
            boolean high=my<y+68;
            int signal=(high!=inverted)?15:0;
            g.renderComponentTooltip(font,List.of(
                mode.getState()==1?Component.translatable("gui.laowu.creature_transmitter.endpoint",signal):
                    Component.translatable("create.gui.threshold_switch."+(signal>0?"power_on_when":"power_off_when")),
                Component.translatable("gui.laowu.creature_transmitter."+(high?"at_least":"at_most"),high?upper.getState():lower.getState()),
                Component.translatable("gui.laowu.creature_transmitter."+(mode.getState()==1?"analog_hint":"threshold_hint"))),mx,my);
        }
    }
}
