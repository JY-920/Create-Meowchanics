package cn.laowu.mod.client.painting;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.network.ModNetwork;
import cn.laowu.mod.painting.*;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Connection-scoped downloads and render-thread-owned textures. */
public final class PaintingClientCache {
 public static final ResourceLocation PLACEHOLDER=ResourceLocation.fromNamespaceAndPath("minecraft","textures/misc/white.png");
 private static final Minecraft MC=Minecraft.getInstance();
 private static final ExecutorService IO=new ThreadPoolExecutor(2,2,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(32),r->{Thread t=new Thread(r,"laowu-painting-client");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
 private static final LinkedHashMap<String,Texture> TEXTURES=new LinkedHashMap<>(16,.75f,true);
 private static final LinkedHashSet<String> QUEUE=new LinkedHashSet<>();
 private static final Map<String,Long> RETRY=new HashMap<>();
 private record Texture(ResourceLocation id,long bytes){}
 private static long generation,gpuBytes;
 private static UUID world,downloadId,uploadId;
 private static Path cacheScope;
 private static PaintingDiskCache diskCache;
 private static synchronized PaintingDiskCache disk(Path scope)throws IOException{
  if(diskCache==null)diskCache=new PaintingDiskCache(scope.getParent(),256L*1024*1024);return diskCache;
 }
 private static final PaintingUploadTracker UPLOADS=new PaintingUploadTracker();
 private static String downloading;
 private static PaintingChunkAssembler chunks;
 private static long lastActivity;
 private static byte[] upload;
 private static boolean committing;
 private static String status="";
 private static int progress;
 private PaintingClientCache(){}
 public static boolean busy(){return upload!=null;}
 public static String status(){return status;}
 public static int progress(){return progress;}
 public static boolean execute(Runnable task){try{IO.execute(task);return true;}catch(RejectedExecutionException e){MC.execute(()->status="图片任务繁忙，请稍后重试");return false;}}
 public static boolean ready(){return world!=null;}
 public static void reload(){
  generation++;cancelUpload();
  if(downloadId!=null && MC.getConnection()!=null)ModNetwork.painting(new PaintingWire.Cancel(downloadId));
  downloadId=null;downloading=null;chunks=null;QUEUE.clear();RETRY.clear();clearTextures();
 }
 public static ResourceLocation resolve(String hash){
  var builtin=PaintingBuiltinCatalog.find(hash);
  if(builtin.isPresent())return LaoWuMod.id(builtin.get().texturePath());
  Texture texture=TEXTURES.get(hash);if(texture!=null)return texture.id();
  if(world!=null && PaintingAssetStore.validHash(hash) && QUEUE.size()<128 && System.currentTimeMillis()>=RETRY.getOrDefault(hash,0L))QUEUE.add(hash);
  return PLACEHOLDER;
 }
 public static void upload(int menu,long revision,PaintingSpec spec,byte[] png){
  if(upload!=null)return;if(world==null){status="服务器图片库尚未就绪";return;}upload=png.clone();uploadId=UPLOADS.start();committing=false;progress=0;status="等待服务器…";
  ModNetwork.painting(new PaintingWire.Begin(uploadId,menu,revision,spec,png.length,PaintingAssetStore.hash(png)));
 }
 public static void cancelUpload(){
  if(uploadId!=null && MC.getConnection()!=null)ModNetwork.painting(new PaintingWire.Cancel(uploadId));
  upload=null;uploadId=null;UPLOADS.clear();committing=false;
 }
 public static void disconnect(){
  generation++;world=null;cacheScope=null;cancelLocal();QUEUE.clear();RETRY.clear();clearTextures();status="";
 }
 private static void cancelLocal(){upload=null;uploadId=null;UPLOADS.clear();downloadId=null;downloading=null;chunks=null;committing=false;}
 public static void clearTextures(){
  for(var texture:TEXTURES.values())MC.getTextureManager().release(texture.id());
  TEXTURES.clear();gpuBytes=0;
 }
 public static void receive(PaintingWire.Message message){
  if(message instanceof PaintingWire.Hello hello){
   if(!hello.world().equals(world)){generation++;clearTextures();QUEUE.clear();RETRY.clear();cancelLocal();world=hello.world();cacheScope=cacheDirectory(world);}
  }else if(message instanceof PaintingWire.Ack ack){
   if(upload==null)return;
   if(!UPLOADS.accepts(ack.session())||committing)return;
   int offset=ack.nextOffset();if(offset<0||offset>upload.length){cancelUpload();return;}
   progress=(int)(100L*offset/upload.length);status="上传 "+progress+"%";
   if(offset==upload.length){committing=true;status="保存到世界…";ModNetwork.painting(new PaintingWire.Commit(uploadId));}
   else ModNetwork.painting(new PaintingWire.Chunk(uploadId,offset,Arrays.copyOfRange(upload,offset,Math.min(offset+24576,upload.length))));
  }else if(message instanceof PaintingWire.Result result){
   if(result.session().equals(new UUID(0,0)) && result.error().equals("store_unavailable"))status="服务器图片库不可用，请联系服主";
   if(upload!=null && UPLOADS.end(result.session())){
    status=result.success()?"印制完成":"印制失败："+result.error();upload=null;uploadId=null;committing=false;
   }
   if(result.session().equals(downloadId)){downloadId=null;chunks=null;downloading=null;}
  }else if(message instanceof PaintingWire.Manifest manifest){
   if(!manifest.session().equals(downloadId)||!manifest.hash().equals(downloading))return;
   try{chunks=new PaintingChunkAssembler(downloadId,manifest.totalBytes(),System.currentTimeMillis());}
   catch(IllegalArgumentException e){downloadId=null;downloading=null;}
   if(downloadId!=null){
    UUID requestId=downloadId;String hash=downloading;long expected=generation;Path scope=cacheScope;
    execute(()->{
     try{
      var cached=disk(scope).read(scope.getFileName().toString(),hash);if(cached.isEmpty()||cached.get().length!=manifest.totalBytes())return;
      NativeImage image=decodeTexture(cached.get());
      MC.execute(()->{
       if(expected!=generation || !requestId.equals(downloadId)){image.close();return;}
       install(hash,image);ModNetwork.painting(new PaintingWire.Cancel(requestId));downloadId=null;downloading=null;chunks=null;
      });
     }catch(Exception ignored){}
    });
   }
  }else if(message instanceof PaintingWire.DownloadChunk chunk){
   if(chunks==null || !chunk.session().equals(downloadId))return;
   try{
    chunks.accept(chunk.offset(),chunk.bytes(),System.currentTimeMillis());lastActivity=System.currentTimeMillis();
    if(chunks.complete()){
     byte[] bytes=chunks.finish();String hash=downloading;long expected=generation;UUID scope=world;Path folder=cacheScope;
     downloadId=null;downloading=null;chunks=null;
     execute(()->{
      try{
       if(!PaintingAssetStore.hash(bytes).equals(hash))return;
       try{disk(folder).put(folder.getFileName().toString(),hash,bytes);}catch(IOException ignored){}
       NativeImage image=decodeTexture(bytes);
       MC.execute(()->{if(expected==generation && Objects.equals(world,scope))install(hash,image);else image.close();});
      }catch(Exception ignored){}
     });
    }
   }catch(IOException e){downloadId=null;downloading=null;chunks=null;}
  }
 }
 private static Path cacheDirectory(UUID scope){
  String address=MC.getCurrentServer()==null?"local":MC.getCurrentServer().ip;
  String namespace=PaintingAssetStore.hash((address+"|"+scope).getBytes(java.nio.charset.StandardCharsets.UTF_8));
  return MC.gameDirectory.toPath().resolve("cat_paintings/cache").resolve(namespace);
 }
 private static void trimDisk(Path folder)throws IOException{
  try(var files=Files.walk(folder.getParent(),2)){
   var png=files.filter(p->p.getFileName().toString().matches("[0-9a-f]{64}\\.png")&&Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)).sorted(Comparator.comparingLong(p->{try{return Files.getLastModifiedTime(p).toMillis();}catch(IOException e){return 0L;}})).toList();
   long bytes=0;for(var p:png)bytes+=Files.size(p);
   for(var p:png){if(bytes<=256L*1024*1024)break;long size=Files.size(p);Files.delete(p);bytes-=size;}
  }
 }
 public static NativeImage decodeTexture(byte[] bytes)throws IOException{
  if(bytes.length<24 || bytes.length>PaintingImages.MAX_PNG_BYTES)throw new IOException("texture_size");
  var header=java.nio.ByteBuffer.wrap(bytes);int w=header.getInt(16),h=header.getInt(20);
  if(w<1||h<1||w>2048||h>2048||(long)w*h>4194304)throw new IOException("texture_dimensions");
  // The byte[] overload copies onto LWJGL's small thread-local MemoryStack.
  return NativeImage.read(new ByteArrayInputStream(bytes));
 }
 public static ResourceLocation install(String hash,NativeImage image){
  try{
   long size=(long)image.getWidth()*image.getHeight()*4;
   ResourceLocation id=LaoWuMod.id("cat_paintings/"+hash);
   var old=TEXTURES.remove(hash);if(old!=null){MC.getTextureManager().release(old.id());gpuBytes-=old.bytes();}
   while(gpuBytes+size>64L*1024*1024 && !TEXTURES.isEmpty()){
    var first=TEXTURES.entrySet().iterator().next();MC.getTextureManager().release(first.getValue().id());gpuBytes-=first.getValue().bytes();TEXTURES.remove(first.getKey());
   }
   DynamicTexture texture=new DynamicTexture(image);texture.setFilter(true,false);MC.getTextureManager().register(id,texture);
   TEXTURES.put(hash,new Texture(id,size));gpuBytes+=size;return id;
  }catch(Exception e){image.close();return PLACEHOLDER;}
 }
 public static void tick(){
  if(MC.getConnection()==null){if(world!=null)disconnect();return;}
  long now=System.currentTimeMillis();
  if(downloadId!=null && now-lastActivity>30000){downloadId=null;chunks=null;downloading=null;}
  if(world!=null && downloadId==null && !QUEUE.isEmpty()){
   String hash=QUEUE.iterator().next();QUEUE.remove(hash);
   if(TEXTURES.containsKey(hash))return;
   downloading=hash;downloadId=UUID.randomUUID();lastActivity=now;
   if(RETRY.size()>=4096)RETRY.remove(RETRY.keySet().iterator().next());RETRY.put(hash,now+60000);
   ModNetwork.painting(new PaintingWire.Fetch(downloadId,hash));
  }
 }
}

