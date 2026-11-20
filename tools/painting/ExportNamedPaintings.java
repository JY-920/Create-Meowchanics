import cn.laowu.mod.painting.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Asset import using the same bounded photo processor as the editor, no redraw. */
public final class ExportNamedPaintings {
 public static void main(String[] args)throws Exception{
  Path sources=Path.of(args[0]),dest=Path.of(args[1]);Files.createDirectories(dest);
  for(var entry:PaintingBuiltinCatalog.namedEntries()){
   byte[] source=Files.readAllBytes(sources.resolve(entry.id()+".jpg"));
   byte[] png=PaintingImages.validateUpload(PaintingImages.prepare(source,entry.spec()),entry.spec());
   Path target=dest.resolve(entry.id()+".png");
   if(args.length>2&&args[2].equals("--check")){
    if(!java.util.Arrays.equals(png,Files.readAllBytes(target)))throw new AssertionError("Named photo differs from source/pipeline: "+entry.id());
   }else Files.write(target,png);
   var image=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
   System.out.println(entry.id()+" "+image.getWidth()+"x"+image.getHeight()+" bytes="+png.length+" sha256="+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(png)));
  }
 }
}
