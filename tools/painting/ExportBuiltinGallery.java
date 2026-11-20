import cn.laowu.mod.painting.*;
import java.nio.file.*;
/** Calls the actual game implementation; never maintains a second pixel-conversion algorithm. */
public final class ExportBuiltinGallery {
 public static void main(String[] args)throws Exception{
  if(args.length<7||(args.length-2)%5!=0)throw new IllegalArgumentException("sourceDir outputDir [id width height crop sourceFile]...");
  Path sourceDir=Path.of(args[0]).toAbsolutePath().normalize(),outputDir=Path.of(args[1]);
  Files.createDirectories(outputDir);
  for(int i=2;i<args.length;i+=5){
   String id=args[i];if(!id.matches("[a-z0-9_]+"))throw new IllegalArgumentException("asset_id");
   Path source=sourceDir.resolve(args[i+4]).normalize();
   if(!source.startsWith(sourceDir)||!Files.isRegularFile(source))throw new IllegalArgumentException("source_path");
   var spec=new PaintingSpec(Integer.parseInt(args[i+1]),Integer.parseInt(args[i+2]),PaintingSpec.Mode.PIXEL,PaintingSpec.Crop.valueOf(args[i+3]));
   // Same client conversion and server normalization used when printing at the table.
   byte[] printed=PaintingImages.prepare(Files.readAllBytes(source),spec);
   byte[] stored=PaintingImages.validateUpload(printed,spec);
   Files.write(outputDir.resolve(id+".png"),stored);
  }
 }
}

