package cn.laowu.mod.client.painting;
import cn.laowu.mod.painting.PaintingImages;
import java.nio.file.*;import java.io.*;import java.util.*;
public final class PaintingImportFiles {
 private static Path directory(Path gameDir)throws IOException{
  Path parent=gameDir.toAbsolutePath().normalize().resolve("cat_paintings"),dir=parent.resolve("imports");
  if(Files.isSymbolicLink(parent)||Files.isSymbolicLink(dir))throw new IOException("import_directory");
  Files.createDirectories(dir);return dir.toRealPath();
 }
 public static Path folder(Path gameDir)throws IOException{return directory(gameDir);}
 private static boolean image(Path p){String name=p.getFileName().toString().toLowerCase(Locale.ROOT);return name.endsWith(".png")||name.endsWith(".jpg")||name.endsWith(".jpeg");}
 public static List<Path> list(Path gameDir)throws IOException{
  Path dir=directory(gameDir);
  try(var files=Files.list(dir)){return files.filter(p->image(p)&&Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)).sorted(Comparator.comparing(p->p.getFileName().toString(),String.CASE_INSENSITIVE_ORDER)).limit(1024).toList();}
 }
 public static byte[] read(Path gameDir,Path selected)throws IOException{
  Path dir=directory(gameDir),file=selected.toAbsolutePath().normalize();
  if(!file.getParent().equals(dir)||!image(file)||Files.isSymbolicLink(file)||!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))throw new IOException("import_path");
  long size=Files.size(file);if(size<1||size>PaintingImages.MAX_SOURCE_BYTES)throw new IOException("image_size");
  try(var in=Files.newInputStream(file,LinkOption.NOFOLLOW_LINKS)){
   byte[] bytes=in.readNBytes(PaintingImages.MAX_SOURCE_BYTES+1);
   if(bytes.length!=size||bytes.length>PaintingImages.MAX_SOURCE_BYTES)throw new IOException("image_size");return bytes;
  }
 }
}
