import cn.laowu.mod.painting.*;
import java.nio.file.*;import java.util.*;import java.awt.image.BufferedImage;import java.io.*;import javax.imageio.ImageIO;
public class PaintingStorageRegression {
 static void check(boolean v,String s){if(!v)throw new AssertionError(s);}
 static void reject(Run r)throws Exception{try{r.run();}catch(IOException|IllegalArgumentException e){return;}throw new AssertionError("Must reject invalid asset or chunk");}
 interface Run{void run()throws Exception;}
 public static void main(String[] a)throws Exception{
  var spec=new PaintingSpec(1,1,PaintingSpec.Mode.PIXEL,PaintingSpec.Crop.KEEP_WIDTH);
  var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(16,16,BufferedImage.TYPE_INT_RGB),"png",out);byte[] png=out.toByteArray();
  Path world=Files.createTempDirectory("cat-painting-test");
  var store=new PaintingAssetStore(world,1024*1024);UUID owner=UUID.randomUUID();
  var first=store.put(png,spec,owner);store.put(png,spec,owner);
  check(store.usedBytes()==png.length,"deduplicate");
  check(Arrays.equals(new PaintingAssetStore(world,1024*1024).read(first.hash()),png),"restart persistence");
  var reopened=new PaintingAssetStore(world,1024*1024);
  check(reopened.put(png,spec,UUID.randomUUID()).owner().equals(owner),"Asset attribution survives restart and deduplication");
  Path asset=world.resolve("laowu/cat_paintings/"+first.hash()+".png");Files.write(asset,new byte[]{9});reject(()->reopened.read(first.hash()));Files.write(asset,png);
  reject(()->store.read("../x"));
  var tiny=new PaintingAssetStore(Files.createTempDirectory("cat-painting-quota"),1);reject(()->tiny.put(png,spec,owner));check(tiny.usedBytes()==0,"no partial asset");
  byte[] data=new byte[24580];Arrays.fill(data,(byte)7);
  var assembler=new PaintingChunkAssembler(UUID.randomUUID(),data.length,0);
  assembler.accept(0,Arrays.copyOf(data,24576),1);assembler.accept(0,Arrays.copyOf(data,24576),2);
  check(!assembler.complete(),"duplicate not counted");reject(()->assembler.finish());
  reject(()->assembler.accept(24577,new byte[1],3));reject(()->assembler.accept(0,new byte[]{3},3));
  assembler.accept(24576,new byte[]{7,7,7,7},4);
  check(Arrays.equals(assembler.finish(),data),"complete chunks");
  check(assembler.expired(30005),"timeout");
  reject(()->new PaintingChunkAssembler(UUID.randomUUID(),8388609,0));
  System.out.println("PASS: storage, quota, persistence, sequential chunks");
 }
}
