import cn.laowu.mod.client.painting.PaintingDiskCache;import cn.laowu.mod.painting.PaintingAssetStore;
import java.nio.file.*;import java.util.*;import java.io.*;import java.awt.image.BufferedImage;import javax.imageio.ImageIO;
public class PaintingDiskRegression {
 static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory("painting-disk-cache");var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(16,16,BufferedImage.TYPE_INT_RGB),"png",out);byte[] png=out.toByteArray();String hash=PaintingAssetStore.hash(png),a="a".repeat(64),b="b".repeat(64);
  var cache=new PaintingDiskCache(root,png.length+1);
  cache.put(a,hash,png);check(Arrays.equals(cache.read(a,hash).orElseThrow(),png),"Disk hit returns exact verified bytes");
  check(cache.read(b,hash).isEmpty(),"World namespaces cannot leak cached pictures");
  check(new PaintingDiskCache(root,png.length+1).read(a,hash).isPresent(),"Disk hit survives process restart");
  Files.setLastModifiedTime(root.resolve(a).resolve(hash+".png"),java.nio.file.attribute.FileTime.fromMillis(1));
  cache.put(b,hash,png);check(cache.read(a,hash).isEmpty()&&cache.read(b,hash).isPresent(),"Budget is global across worlds");
  Files.write(root.resolve(b).resolve(hash+".png"),new byte[]{8});check(cache.read(b,hash).isEmpty(),"Corruption never becomes a texture");
  boolean rejected=false;try{cache.put("../outside",hash,png);}catch(IOException|IllegalArgumentException e){rejected=true;}check(rejected,"Reject namespace escape");
  System.out.println("PASS: scoped, persistent, hash-checked global LRU disk cache");
 }
}
