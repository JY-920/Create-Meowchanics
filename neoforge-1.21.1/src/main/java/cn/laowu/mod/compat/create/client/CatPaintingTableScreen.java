package cn.laowu.mod.compat.create.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.client.painting.*;
import cn.laowu.mod.create.CatPaintingTableMenu;
import cn.laowu.mod.painting.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.simibubi.create.foundation.gui.*;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.*;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

/** Create file controls around the vanilla cartography table parchment. */
public class CatPaintingTableScreen extends AbstractSimiContainerScreen<CatPaintingTableMenu> {
    private static final int BODY_WIDTH=206, WINDOW_WIDTH=224, INVENTORY_TOP=CatPaintingTableMenu.INVENTORY_Y-18;
    private static final int FILE_X=42, FILE_Y=23, FILE_W=144, CANVAS_X=128, CANVAS_Y=44, CANVAS_SIZE=66;
    private static final int MODE_Y=47, SIZE_Y=71, FOOTER_Y=CatPaintingTableMenu.FOOTER_Y;
    private static final int INK=0x404040, LIGHT=0xEEE9DD;
    private static final ResourceLocation SHEET=ResourceLocation.fromNamespaceAndPath("create","textures/gui/schematics.png");
    private static final ResourceLocation PARCHMENT=ResourceLocation.withDefaultNamespace("container/cartography_table/map");
    private static final ResourceLocation PREVIEW_TEXTURE=LaoWuMod.id("cat_paintings/editor_preview");
    private static final long PREVIEW_TIMEOUT=10_000, PRINT_TIMEOUT=30_000;
    private record PreviewInput(byte[] source, PaintingSpec spec) {}

    private List<Path> files=List.of();
    private final List<ScrollInput> settings=new ArrayList<>();
    private final PaintingPreviewQueue previews=new PaintingPreviewQueue(job->{
        if(!PaintingClientCache.execute(job))throw new RejectedExecutionException();
    });
    private SelectionScrollInput fileChoice;
    private ScrollInput width,height,mode,crop;
    private IconButton confirm,refresh;
    private PreviewInput previewInput;
    private DynamicTexture preview;
    private int request,refreshRequest;
    private long deadline;
    private boolean initialized,printing,refreshing,showUploadResult,removed;
    private String notice="";

    public CatPaintingTableScreen(CatPaintingTableMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title);
        setWindowSize(WINDOW_WIDTH,INVENTORY_TOP+AllGuiTextures.PLAYER_INVENTORY.getHeight());
    }

    @Override protected void init(){
        super.init();
        boolean first=!initialized;
        Path selected=selectedFile();
        PaintingSpec initial=first?new PaintingSpec(1,1,PaintingSpec.Mode.ORIGINAL,PaintingSpec.Crop.KEEP_WIDTH):spec();
        settings.clear();
        icon(20,FILE_Y,AllIcons.I_OPEN_FOLDER,"打开本地图片目录",()->{
            var gameDir=minecraft.gameDirectory.toPath();
            if(!PaintingClientCache.execute(()->{
                try{
                    Path folder=PaintingImportFiles.folder(gameDir);
                    minecraft.execute(()->{if(isOpen())Util.getPlatform().openFile(folder.toFile());});
                }catch(Exception e){minecraft.execute(()->{if(isOpen())notice="无法打开图片目录";});}
            }))notice="图片任务繁忙，请重试";
        });
        refresh=icon(BODY_WIDTH,FILE_Y,AllIcons.I_REFRESH,"刷新 PNG / JPG 列表",()->refresh(selectedFile()));
        fileChoice=new SelectionScrollInput(leftPos+FILE_X,topPos+FILE_Y,FILE_W,18);
        // A decorative label must not intercept the selector's wheel events.
        var fileLabel=addRenderableOnly(new Label(leftPos+FILE_X+6,topPos+FILE_Y+5,Component.literal("读取目录…")).colored(LIGHT));
        fileChoice.writingTo(fileLabel);
        updateFileChoices(selected);
        fileChoice.calling(i->preparePreview());
        addRenderableWidget(fileChoice);
        settings.add(fileChoice);

        mode=selection(12,MODE_Y,53,List.of("原生","像素"),"显示模式：原生高清 / 像素（每格 16 像素）",initial.mode().ordinal());
        crop=selection(70,MODE_Y,53,List.of("保留宽","保留高"),"保留宽度或高度，其余部分居中裁剪或留边",initial.crop().ordinal());
        width=number(12,SIZE_Y,53,"宽",initial.widthBlocks());
        height=number(70,SIZE_Y,53,"高",initial.heightBlocks());
        confirm=icon(44,FOOTER_Y+9,AllIcons.I_CONFIRM,"印制到猫画；确认后才向服务器上传",this::print);
        confirm.green=true;
        confirm.active=false;
        initialized=true;
        if(first)refresh(selected);
    }

    private boolean isOpen(){return !removed&&minecraft.screen==this;}
    private IconButton icon(int x,int y,AllIcons graphic,String tip,Runnable action){
        var button=addRenderableWidget(new IconButton(leftPos+x,topPos+y,graphic));
        button.setToolTip(Component.literal(tip));
        button.withCallback(action);
        return button;
    }
    private ScrollInput selection(int x,int y,int w,List<String> options,String tip,int initial){
        var input=new SelectionScrollInput(leftPos+x,topPos+y,w,18);
        input.forOptions(options.stream().map(Component::literal).toList());
        bind(input,x,y,tip,initial);
        return input;
    }
    private ScrollInput number(int x,int y,int w,String axis,int initial){
        var input=new ScrollInput(leftPos+x,topPos+y,w,18).withRange(1,9)
            .format(i->Component.literal(axis+" "+i+" 格"));
        bind(input,x,y,axis+"度：1–8 格",initial);
        return input;
    }
    private void bind(ScrollInput input,int x,int y,String tip,int initial){
        var label=addRenderableOnly(new Label(leftPos+x+5,topPos+y+5,Component.empty()).colored(LIGHT));
        input.writingTo(label);
        input.titled(Component.literal(tip));
        input.setState(initial);
        input.onChanged();
        input.calling(i->preparePreview());
        settings.add(addRenderableWidget(input));
    }
    private Path selectedFile(){
        if(fileChoice==null||files.isEmpty())return null;
        return files.get(Math.min(Math.max(0,fileChoice.getState()),files.size()-1));
    }
    private String ellipsize(String value,int max){
        return font.width(value)<=max?value:font.plainSubstrByWidth(value,max-font.width("…"))+"…";
    }
    private void updateFileChoices(Path selected){
        fileChoice.forOptions(files.isEmpty()?List.of(Component.literal("目录中没有 PNG / JPG")):
            files.stream().map(path->Component.literal(ellipsize(path.getFileName().toString(),FILE_W-12))).toList());
        fileChoice.setState(selected==null?0:Math.max(0,files.indexOf(selected)));
        fileChoice.onChanged();
    }

    private void refresh(Path selected){
        if(printing||PaintingClientCache.busy())return;
        invalidatePreview();
        showUploadResult=false;
        refreshing=true;
        int version=++refreshRequest;
        deadline=Util.getMillis()+PREVIEW_TIMEOUT;
        notice="读取图片目录…";
        var gameDir=minecraft.gameDirectory.toPath();
        if(!PaintingClientCache.execute(()->{
            try{
                List<Path> paths=PaintingImportFiles.list(gameDir);
                minecraft.execute(()->{
                    if(!isOpen()||version!=refreshRequest)return;
                    refreshing=false;
                    deadline=0;
                    files=paths;
                    updateFileChoices(selected);
                });
            }catch(Exception e){minecraft.execute(()->{
                if(isOpen()&&version==refreshRequest){refreshing=false;deadline=0;notice="无法读取图片目录";}
            });}
        })){
            refreshing=false;
            deadline=0;
            notice="目录任务繁忙，请刷新重试";
        }
    }

    private PaintingSpec spec(){
        return new PaintingSpec(width.getState(),height.getState(),PaintingSpec.Mode.values()[mode.getState()],PaintingSpec.Crop.values()[crop.getState()]);
    }
    private void invalidatePreview(){
        request++;
        printing=false;
        deadline=0;
        previewInput=null;
        if(preview!=null){
            minecraft.getTextureManager().release(PREVIEW_TEXTURE);
            preview=null;
        }
    }
    private void preparePreview(){
        if(!initialized||refreshing||printing||PaintingClientCache.busy())return;
        invalidatePreview();
        showUploadResult=false;
        Path file=selectedFile();
        if(file==null){notice="将 PNG / JPG 放入目录，再点击刷新";return;}

        int version=request;
        PaintingSpec chosen=spec();
        var gameDir=minecraft.gameDirectory.toPath();
        deadline=Util.getMillis()+PREVIEW_TIMEOUT;
        notice="正在生成预览…";
        if(!previews.submit(()->{
            try{
                byte[] source=PaintingImportFiles.read(gameDir,file);
                BufferedImage raster=PaintingImages.preview(source,chosen);
                minecraft.execute(()->finishPreview(version,new PreviewInput(source,chosen),raster));
            }catch(Exception e){minecraft.execute(()->fail(version,"预览失败",e));}
        })){
            deadline=0;
            notice="预览任务繁忙，请刷新重试";
        }
    }
    private void finishPreview(int version,PreviewInput input,BufferedImage raster){
        try{
            if(!isOpen()||version!=request)return;
            NativeImage image=new NativeImage(raster.getWidth(),raster.getHeight(),false);
            DynamicTexture texture=null;
            try{
                for(int y=0;y<raster.getHeight();y++)for(int x=0;x<raster.getWidth();x++){
                    int argb=raster.getRGB(x,y);
                    image.setPixelRGBA(x,y,argb&0xff00ff00 | (argb&0xff)<<16 | (argb>>>16)&0xff);
                }
                texture=new DynamicTexture(image);
                texture.setFilter(input.spec().mode()==PaintingSpec.Mode.ORIGINAL,false);
                minecraft.getTextureManager().register(PREVIEW_TEXTURE,texture);
                preview=texture;
                previewInput=input;
                deadline=0;
                notice="";
            }catch(RuntimeException e){
                if(texture==null)image.close();else texture.close();
                fail(version,"无法显示预览",e);
            }
        }finally{raster.flush();}
    }

    private void print(){
        PreviewInput input=previewInput;
        if(input==null||!menu.canPrint()||!PaintingClientCache.ready()||printing||PaintingClientCache.busy())return;
        int version=++request;
        long revision=menu.revision();
        printing=true;
        deadline=Util.getMillis()+PRINT_TIMEOUT;
        notice="正在生成印制文件…";
        if(!PaintingClientCache.execute(()->{
            try{
                byte[] png=PaintingImages.prepare(input.source(),input.spec());
                minecraft.execute(()->{
                    if(!isOpen()||version!=request)return;
                    printing=false;
                    deadline=0;
                    if(menu.revision()!=revision||!menu.canPrint()){
                        notice="槽位已变化，请重新确认";
                        return;
                    }
                    PaintingClientCache.upload(menu.containerId,revision,input.spec(),png);
                    showUploadResult=true;
                    notice="";
                });
            }catch(Exception e){minecraft.execute(()->fail(version,"印制失败",e));}
        })){
            printing=false;
            deadline=0;
            notice="印制任务繁忙，请重试";
        }
    }
    private void fail(int version,String action,Exception e){
        if(!isOpen()||version!=request)return;
        deadline=0;
        printing=false;
        String detail=switch(String.valueOf(e.getMessage())){
            case "image_size"->"文件或生成的图片过大";
            case "image_dimensions"->"图片尺寸超出限制";
            case "import_path"->"图片已移动或删除，请刷新";
            case "image_format"->"文件不是可读取的 PNG 或 JPG";
            default->"无法读取或处理图片";
        };
        notice=action+"："+detail;
    }
    @Override protected void containerTick(){
        super.containerTick();
        if(deadline!=0&&Util.getMillis()>=deadline){
            notice=refreshing?"目录读取超时，请刷新重试":printing?"印制处理超时，请重试":"预览处理超时，请刷新重试";
            deadline=0;
            request++;
            refreshRequest++;
            printing=false;
            refreshing=false;
        }
        boolean busy=printing||PaintingClientCache.busy();
        confirm.active=!busy&&!refreshing&&previewInput!=null&&menu.canPrint()&&PaintingClientCache.ready();
        confirm.setToolTip(Component.literal(status()));
        refresh.active=!busy&&!refreshing;
        for(var control:settings)control.active=!busy&&!refreshing;
    }

    private void field(GuiGraphics g,int x,int y,int w){
        // The pointer's blue pixels match the native sheet when x is even and y is odd.
        x+=leftPos;y+=topPos;
        g.blit(SHEET,x,y,52,162,6,18,256,256);
        g.blit(SHEET,x+6,y,w-7,18,58,162,137,18,256,256);
        g.blit(SHEET,x+w-1,y,195,162,1,18,256,256);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        int x=leftPos,y=topPos;
        // Keep the unscaled title, file field and edges from Create's schematic table.
        g.blit(SHEET,x,y,10,139,BODY_WIDTH,48,256,256);
        // Extend only an uninterrupted two-row checker pair (including the side rails).
        for(int row=48;row<FOOTER_Y-1;row+=2)
            g.blit(SHEET,x,y+row,10,181,BODY_WIDTH,2,256,256);
        g.blit(SHEET,x,y+FOOTER_Y-1,10,187,BODY_WIDTH,1,256,256);
        g.blit(SHEET,x,y+FOOTER_Y,10,188,214,36,256,256);
        renderProcessing(g,x,y+FOOTER_Y);
        int inventoryX=x+CatPaintingTableMenu.INVENTORY_X-8;
        AllGuiTextures.PLAYER_INVENTORY.render(g,inventoryX,y+INVENTORY_TOP);
        g.drawString(font,title,x+(BODY_WIDTH-font.width(title))/2,y+3,INK,false);

        field(g,12,MODE_Y,53);field(g,70,MODE_Y,53);
        field(g,12,SIZE_Y,53);field(g,70,SIZE_Y,53);

        int px=x+CANVAS_X,py=y+CANVAS_Y;
        g.blitSprite(PARCHMENT,px,py,CANVAS_SIZE,CANVAS_SIZE);
        if(preview!=null&&previewInput!=null){
            PaintingSpec shown=previewInput.spec();
            float scale=56f/Math.max(shown.widthBlocks(),shown.heightBlocks());
            int w=Math.round(scale*shown.widthBlocks()),h=Math.round(scale*shown.heightBlocks());
            g.blit(PREVIEW_TEXTURE,px+(CANVAS_SIZE-w)/2,py+(CANVAS_SIZE-h)/2,w,h,0,0,1,1,1,1);
        }else{
            g.fill(px+5,py+5,px+CANVAS_SIZE-5,py+CANVAS_SIZE-5,PaintingImages.MATTE);
        }
    }
    private void renderProcessing(GuiGraphics g,int x,int y){
        var bar=AllGuiTextures.SCHEMATIC_TABLE_PROGRESS;
        int percent=PaintingClientCache.busy()?PaintingClientCache.progress():menu.hasOutput()?100:0;
        int width=bar.getWidth()*Math.max(0,Math.min(percent,100))/100;
        if(width>0)g.blit(bar.location,x+70,y+10,bar.getStartX(),bar.getStartY(),width,bar.getHeight(),256,256);

    }
    private boolean printSucceeded(){return showUploadResult&&!printing&&"印制完成".equals(PaintingClientCache.status());}
    private String status(){
        if(PaintingClientCache.busy())return PaintingClientCache.status();
        if(!notice.isEmpty())return notice;
        if(!PaintingClientCache.ready())return PaintingClientCache.status().isEmpty()?"等待服务器图片库…":PaintingClientCache.status();
        if(menu.hasOutput())return "从右侧取出已印制的猫画";
        if(showUploadResult&&!printSucceeded())return PaintingClientCache.status();
        return !menu.hasPainting()?"请先放入猫画":"印制到猫画；确认后才向服务器上传";
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){}
    @Override public void removed(){
        removed=true;
        refreshRequest++;
        invalidatePreview();
        previews.close();
        PaintingClientCache.cancelUpload();
        super.removed();
    }
}
