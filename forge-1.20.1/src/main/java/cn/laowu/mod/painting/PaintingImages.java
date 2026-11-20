package cn.laowu.mod.painting;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

/** Bounded image processing. Call on an IO worker, never the render or tick thread. */
public final class PaintingImages {
    public static final int MAX_SOURCE_BYTES=16*1024*1024, MAX_PNG_BYTES=8*1024*1024, MATTE=0xFFF3E5C8;
    private PaintingImages() {}
    private static BufferedImage decode(byte[] bytes,int limit,boolean upload)throws IOException {
        if(bytes==null || bytes.length==0 || bytes.length>limit)throw new IOException("image_size");
        try(var in=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
            var readers=ImageIO.getImageReaders(in);
            if(!readers.hasNext())throw new IOException("image_format");
            ImageReader reader=readers.next();
            try{
                String format=reader.getFormatName();
                if(!(format.equalsIgnoreCase("png") || (!upload && format.equalsIgnoreCase("jpeg"))))throw new IOException("image_format");
                reader.setInput(in,true,true);
                int w=reader.getWidth(0),h=reader.getHeight(0),side=upload?2048:8192;
                if(w<1 || h<1 || w>side || h>side || (long)w*h>(upload?4194304L:16000000L))throw new IOException("image_dimensions");
                BufferedImage result=reader.read(0);
                if(result==null)throw new IOException("image_decode");
                return result;
            }finally{reader.dispose();}
        }catch(RuntimeException e){throw new IOException("image_decode",e);}
    }
    public static byte[] prepare(byte[] source,PaintingSpec spec)throws IOException {
        BufferedImage output=raster(source,spec,2048);
        try{return encode(output);}finally{output.flush();}
    }
    /** Pixel mode uses the final texel grid; HD mode only needs a small local thumbnail. */
    public static BufferedImage preview(byte[] source,PaintingSpec spec)throws IOException {
        return raster(source,spec,256);
    }
    private static BufferedImage raster(byte[] source,PaintingSpec spec,int maxEdge)throws IOException {
        BufferedImage input=decode(source,MAX_SOURCE_BYTES,false);
        int bw=spec.widthBlocks(),bh=spec.heightBlocks();
        int unit=spec.mode()==PaintingSpec.Mode.PIXEL?16:Math.max(1,Math.min(maxEdge/Math.max(bw,bh),(int)Math.ceil(Math.max(input.getWidth()/(double)bw,input.getHeight()/(double)bh))));
        BufferedImage output=new BufferedImage(bw*unit,bh*unit,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=output.createGraphics();
        try{
            g.setColor(new Color(MATTE));g.fillRect(0,0,output.getWidth(),output.getHeight());
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,spec.mode()==PaintingSpec.Mode.PIXEL?RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR:RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            double scale=spec.crop()==PaintingSpec.Crop.KEEP_WIDTH?output.getWidth()/(double)input.getWidth():output.getHeight()/(double)input.getHeight();
            AffineTransform transform=AffineTransform.getTranslateInstance((output.getWidth()-input.getWidth()*scale)/2,(output.getHeight()-input.getHeight()*scale)/2);
            transform.scale(scale,scale);g.drawRenderedImage(input,transform);return output;
        }finally{g.dispose();input.flush();}
    }
    public static byte[] validateUpload(byte[] png,PaintingSpec spec)throws IOException {
        BufferedImage input=decode(png,MAX_PNG_BYTES,true);
        try{
            int w=input.getWidth(),h=input.getHeight();
            if(w%spec.widthBlocks()!=0 || h%spec.heightBlocks()!=0 || w/spec.widthBlocks()!=h/spec.heightBlocks() ||
                (spec.mode()==PaintingSpec.Mode.PIXEL && w!=spec.widthBlocks()*16))throw new IOException("image_dimensions");
            BufferedImage output=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=output.createGraphics();
            try{g.setColor(new Color(MATTE));g.fillRect(0,0,w,h);g.drawImage(input,0,0,null);return encode(output);}
            finally{g.dispose();output.flush();}
        }finally{input.flush();}
    }
    private static byte[] encode(BufferedImage image)throws IOException {
        var bytes=new ByteArrayOutputStream();
        OutputStream bounded=new OutputStream(){
            private void check(int n)throws IOException{if((long)bytes.size()+n>MAX_PNG_BYTES)throw new IOException("image_size");}
            public void write(int b)throws IOException{check(1);bytes.write(b);}
            public void write(byte[] b,int off,int len)throws IOException{check(len);bytes.write(b,off,len);}
        };
        if(!ImageIO.write(image,"png",bounded))throw new IOException("image_encode");
        return bytes.toByteArray();
    }
}
