import cn.laowu.mod.painting.*;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;
/** Integration contract: bundled art must equal the editor's output from the ORIGINAL photos. */
public final class PaintingBuiltinPixelsCheck {
 private record Picture(String id,int width,int height,String source){}
 private static final List<Picture> PICTURES=List.of(
  new Picture("silver_loaf",2,2,"0f59f915ebfee34718027b69450e0bb9.jpg"),
  new Picture("white_stare",2,2,"IMG_20261107_160138.jpg"),
  new Picture("grass_cat",2,2,"222715eee6855007c853ab5838dc1a9d.jpg"),
  new Picture("angry_kitten",2,2,"PixPin_2026-11-07_15-54-24.png"),
  new Picture("teary_cat",2,2,"PixPin_2026-11-07_15-54-53.png"),
  new Picture("banana_cat",2,1,"PixPin_2026-11-07_15-54-59.png"),
  new Picture("side_eye",2,2,"PixPin_2026-11-07_15-55-14.png"),
  new Picture("cap_cat",2,2,"PixPin_2026-11-07_15-55-47.png"),
  new Picture("folded_ears",3,3,"PixPin_2026-11-07_15-57-55.png"),
  new Picture("round_tabby",1,1,"1727594607229.png"),
  new Picture("wide_eyes",2,2,"1778413440676.png"),
  new Picture("cowboy_cat",3,3,"2b9a803263a9f36fda92db8c8aea38d3.jpg")
 );
 public static void main(String[] args)throws Exception{
  Path root=Path.of(args[0]),sources=root.resolve("assets-source/cat-painting");
  Path textures=root.resolve(args[1]+"/src/main/resources/assets/laowu/textures/painting/builtin");
  List<String> failures=new ArrayList<>();int pixels=0;
  for(var p:PICTURES){
   byte[] source=Files.readAllBytes(sources.resolve(p.source()));
   var crop=p.id().equals("grass_cat")?PaintingSpec.Crop.KEEP_HEIGHT:PaintingSpec.Crop.KEEP_WIDTH;
   var spec=new PaintingSpec(p.width(),p.height(),PaintingSpec.Mode.PIXEL,crop);
   var expected=PaintingImages.preview(source,spec);
   byte[] prepared=PaintingImages.prepare(source,spec);
   byte[] stored=PaintingImages.validateUpload(prepared,spec);
   byte[] actualBytes=Files.readAllBytes(textures.resolve(p.id()+".png"));
   var actual=ImageIO.read(new ByteArrayInputStream(actualBytes));
   if(actual==null||actual.getWidth()!=expected.getWidth()||actual.getHeight()!=expected.getHeight()){
    failures.add(p.id()+": wrong dimensions");continue;
   }
   int different=0;
   for(int y=0;y<expected.getHeight();y++)for(int x=0;x<expected.getWidth();x++){
    pixels++;if(expected.getRGB(x,y)!=actual.getRGB(x,y))different++;
   }
   if(different>0)failures.add(p.id()+": "+different+" pixels differ from in-game PIXEL preview");
   if(!Arrays.equals(actualBytes,stored))failures.add(p.id()+": PNG differs from the editor's prepared/server-validated PNG");
   expected.flush();actual.flush();
  }
  if(!failures.isEmpty())throw new AssertionError(String.join("\n",failures));
  System.out.println("PASS "+args[1]+": 12 original photos match in-game preview pixel-for-pixel and printed PNG byte-for-byte ("+pixels+" pixels)");
 }
}

