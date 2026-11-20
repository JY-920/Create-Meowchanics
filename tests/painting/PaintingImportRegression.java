import cn.laowu.mod.client.painting.PaintingImportFiles;import java.nio.file.*;import java.io.*;
public class PaintingImportRegression {
 public static void main(String[] a)throws Exception{
  Path game=Files.createTempDirectory("painting-import");Path dir=game.resolve("cat_paintings/imports");Files.createDirectories(dir);
  Path png=dir.resolve("A.PNG");Files.write(png,new byte[]{1});Files.write(dir.resolve("ignore.txt"),new byte[]{2});Files.createDirectories(dir.resolve("sub"));Files.write(dir.resolve("sub/nested.png"),new byte[]{3});
  if(PaintingImportFiles.list(game).size()!=1)throw new AssertionError("Direct png files only");
  if(PaintingImportFiles.read(game,png).length!=1)throw new AssertionError("Read selected file");
  try{PaintingImportFiles.read(game,game.resolve("outside.png"));throw new AssertionError("Escape accepted");}catch(IOException expected){}
  System.out.println("PASS: direct imports and directory escape rejection");
 }
}
