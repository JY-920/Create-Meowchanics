package cn.laowu.mod.client.painting;
import cn.laowu.mod.painting.*;
import java.io.*;import java.nio.file.*;import java.nio.file.attribute.FileTime;import java.util.*;

/** Content-verified cache. Call only after the server grants access to the scoped hash. */
public final class PaintingDiskCache {
 private final Path root;private final long quota;
 public PaintingDiskCache(Path root,long quota)throws IOException{
  if(quota<0)throw new IllegalArgumentException("cache_quota");this.root=root.toAbsolutePath().normalize();this.quota=quota;
  if(Files.isSymbolicLink(this.root)||Files.isSymbolicLink(this.root.getParent()))throw new IOException("cache_path");
  Files.createDirectories(this.root);
 }
 private Path path(String scope,String hash)throws IOException{
  if(!PaintingAssetStore.validHash(scope)||!PaintingAssetStore.validHash(hash))throw new IOException("cache_key");
  Path folder=root.resolve(scope),file=folder.resolve(hash+".png");
  if(Files.isSymbolicLink(folder)||Files.isSymbolicLink(file))throw new IOException("cache_path");return file;
 }
 public synchronized Optional<byte[]> read(String scope,String hash)throws IOException{
  Path file=path(scope,hash);if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))return Optional.empty();
  long size=Files.size(file);
  if(size<1||size>PaintingImages.MAX_PNG_BYTES){Files.delete(file);return Optional.empty();}
  byte[] bytes;
  try(var in=Files.newInputStream(file,LinkOption.NOFOLLOW_LINKS)){bytes=in.readNBytes(PaintingImages.MAX_PNG_BYTES+1);}
  if(bytes.length!=size||!PaintingAssetStore.hash(bytes).equals(hash)){Files.delete(file);return Optional.empty();}
  Files.setLastModifiedTime(file,FileTime.fromMillis(System.currentTimeMillis()));return Optional.of(bytes);
 }
 public synchronized void put(String scope,String hash,byte[] bytes)throws IOException{
  if(bytes==null||bytes.length<1||bytes.length>PaintingImages.MAX_PNG_BYTES||!PaintingAssetStore.hash(bytes).equals(hash))throw new IOException("cache_hash");
  Path file=path(scope,hash);Files.createDirectories(file.getParent());
  Path temp=Files.createTempFile(file.getParent(),"download-",".tmp");
  try{Files.write(temp,bytes);try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}}
  finally{Files.deleteIfExists(temp);}
  trim();
 }
 private void trim()throws IOException{
  try(var files=Files.walk(root,2)){
   var png=files.filter(p->p.getFileName().toString().matches("[0-9a-f]{64}\\.png")&&p.getParent()!=null&&PaintingAssetStore.validHash(p.getParent().getFileName().toString())&&Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS))
    .sorted(Comparator.comparingLong(p->{try{return Files.getLastModifiedTime(p).toMillis();}catch(IOException e){return 0L;}})).toList();
   long bytes=0;for(Path p:png)bytes+=Files.size(p);
   for(Path p:png){if(bytes<=quota)break;long size=Files.size(p);Files.delete(p);bytes-=size;}
  }
 }
}
