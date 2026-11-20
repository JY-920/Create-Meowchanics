import cn.laowu.mod.painting.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import static cn.laowu.mod.painting.PaintingSpec.Mode.*;
import static cn.laowu.mod.painting.PaintingSpec.Crop.*;
public final class PaintingImagesRegression {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static void rejects(Throwing action,String why)throws Exception{checks++;try{action.run();}catch(IOException|IllegalArgumentException expected){return;}throw new AssertionError(why);}
    interface Throwing {void run()throws Exception;}
    static byte[] encode(BufferedImage image,String format)throws Exception{var out=new ByteArrayOutputStream();ImageIO.write(image,format,out);return out.toByteArray();}
    public static void main(String[] args)throws Exception{
        rejects(()->new PaintingSpec(0,1,PIXEL,KEEP_WIDTH),"width must be restricted to 1..8");
        rejects(()->new PaintingSpec(1,9,PIXEL,KEEP_WIDTH),"height must be restricted to 1..8");
        var source=new BufferedImage(32,16,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<16;y++)for(int x=0;x<32;x++)source.setRGB(x,y,x<16?0xFFFF0000:0xFF0000FF);
        byte[] png=encode(source,"png");byte[] before=png.clone();
        for(var crop:PaintingSpec.Crop.values()){
            var spec=new PaintingSpec(1,1,PIXEL,crop);
            var result=ImageIO.read(new ByteArrayInputStream(PaintingImages.prepare(png,spec)));
            var preview=PaintingImages.preview(png,spec);
            check(preview.getWidth()==16&&preview.getHeight()==16,"preview preserves one pixel per intended pixel-mode texel");
            check(java.util.Arrays.equals(result.getRGB(0,0,16,16,null,0,16),
                preview.getRGB(0,0,16,16,null,0,16)),"preview and print use the same pixel crop and matte");
            check(result.getWidth()==16&&result.getHeight()==16,"one block is 16 by 16");
            check((result.getRGB(0,0)==0xFFF3E5C8)==(crop==KEEP_WIDTH),"width fits with padding, height crops sides");
            check(result.getRGB(3,8)==0xFFFF0000&&result.getRGB(12,8)==0xFF0000FF,"centered mapping preserves colors");
            byte[] processed=PaintingImages.prepare(png,spec);
            check(ImageIO.read(new ByteArrayInputStream(PaintingImages.validateUpload(processed,spec))).getWidth()==16,"processed PNG accepted");
        }
        check(java.util.Arrays.equals(png,before),"source bytes are immutable");
        var transparent=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
        var matte=ImageIO.read(new ByteArrayInputStream(PaintingImages.prepare(encode(transparent,"png"),new PaintingSpec(1,1,PIXEL,KEEP_WIDTH))));
        check(matte.getRGB(8,8)==0xFFF3E5C8,"transparent pixels flatten on cream");
        for(int w=1;w<=8;w++)for(int h=1;h<=8;h++){
            var result=ImageIO.read(new ByteArrayInputStream(PaintingImages.prepare(png,new PaintingSpec(w,h,PIXEL,KEEP_HEIGHT))));
            check(result.getWidth()==w*16&&result.getHeight()==h*16,"pixel dimensions "+w+"x"+h);
        }
        byte[] jpg=encode(source,"jpg");
        check(PaintingImages.prepare(jpg,new PaintingSpec(2,1,ORIGINAL,KEEP_WIDTH)).length>0,"JPEG decodes to PNG");
        rejects(()->PaintingImages.prepare(new byte[]{1,2,3},new PaintingSpec(1,1,PIXEL,KEEP_WIDTH)),"garbage rejected");
        rejects(()->PaintingImages.prepare(new byte[16*1024*1024+1],new PaintingSpec(1,1,PIXEL,KEEP_WIDTH)),"source byte limit");
        rejects(()->PaintingImages.validateUpload(jpg,new PaintingSpec(2,1,ORIGINAL,KEEP_WIDTH)),"server only accepts PNG");
        rejects(()->PaintingImages.validateUpload(png,new PaintingSpec(1,1,PIXEL,KEEP_WIDTH)),"server rejects wrong pixel dimensions");
        rejects(()->PaintingImages.validateUpload(new byte[8*1024*1024+1],new PaintingSpec(1,1,PIXEL,KEEP_WIDTH)),"upload byte limit");
        byte[] huge=png.clone();java.nio.ByteBuffer.wrap(huge).putInt(16,9000).putInt(20,9000);
        rejects(()->PaintingImages.prepare(huge,new PaintingSpec(1,1,ORIGINAL,KEEP_WIDTH)),"huge PNG header rejected before allocation");
        var large=new BufferedImage(2100,2,BufferedImage.TYPE_INT_RGB);
        var thumbnail=PaintingImages.preview(encode(large,"png"),new PaintingSpec(2,1,ORIGINAL,KEEP_WIDTH));
        check(thumbnail.getWidth()<=256&&thumbnail.getWidth()==2*thumbnail.getHeight(),"HD preview is bounded without stretching its canvas");
        check(thumbnail.getRGB(0,0)==0xFFF3E5C8,"HD thumbnail shows the same letterboxing");
        rejects(()->PaintingImages.preview(new byte[]{1,2,3},new PaintingSpec(1,1,ORIGINAL,KEEP_WIDTH)),"invalid local source reports an error");
        var bounded=ImageIO.read(new ByteArrayInputStream(PaintingImages.prepare(encode(large,"png"),new PaintingSpec(8,8,ORIGINAL,KEEP_WIDTH))));
        check(bounded.getWidth()==2048&&bounded.getHeight()==2048,"original output bounded at 2048");
        System.out.println("PASS: "+checks+" painting image checks");
    }
}
