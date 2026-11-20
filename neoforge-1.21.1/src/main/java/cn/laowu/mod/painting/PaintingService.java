package cn.laowu.mod.painting;

import cn.laowu.mod.ServerConfig;
import cn.laowu.mod.entity.CatPaintingEntity;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.entity.Entity;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Server-thread transaction ownership; only immutable bytes cross the bounded IO executor. */
public final class PaintingService implements AutoCloseable {
 private static final Map<MinecraftServer,PaintingService> SERVICES=new WeakHashMap<>();
 public static PaintingService get(MinecraftServer server){return SERVICES.computeIfAbsent(server,PaintingService::new);}
 public static void stop(MinecraftServer server){var service=SERVICES.remove(server);if(service!=null)service.close();}
 private final MinecraftServer server;
 private final ThreadPoolExecutor io=new ThreadPoolExecutor(2,2,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(16),r->{Thread t=new Thread(r,"laowu-painting-io");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
 private final Map<UUID,Upload> uploads=new HashMap<>();
 private final Map<UUID,Deque<Long>> attempts=new HashMap<>(),fetches=new HashMap<>();
 private final Map<UUID,Map<UUID,String>> tracked=new HashMap<>();
 private final Map<UUID,Download> downloads=new HashMap<>();
 private final Set<UUID> fetching=new HashSet<>();
 private PaintingAssetStore store;
 private UUID world;
 private boolean closed,unavailable;
 private static final UUID NONE=new UUID(0,0);
 private static final class Upload {
  final ServerPlayer player;final PaintingEditorAccess editor;final int menu;final long revision;final PaintingSpec spec;final String hash;final PaintingChunkAssembler chunks;boolean writing;
  Upload(ServerPlayer p,PaintingEditorAccess e,int menu,long revision,PaintingSpec spec,String hash,PaintingChunkAssembler chunks){player=p;editor=e;this.menu=menu;this.revision=revision;this.spec=spec;this.hash=hash;this.chunks=chunks;}
 }
 private static final class Download {
  final UUID session;final String hash;final byte[] bytes;int offset;
  Download(UUID session,String hash,byte[] bytes){this.session=session;this.hash=hash;this.bytes=bytes;}
 }
 private PaintingService(MinecraftServer server){
  this.server=server;Path root=server.getWorldPath(LevelResource.ROOT);long quota=ServerConfig.PAINTING_QUOTA_BYTES.get();
  work(()->{
   try{
    var loaded=new PaintingAssetStore(root,quota);
    Path file=root.resolve("laowu/cat_paintings/world-id.txt");
    UUID id;
    if(Files.exists(file)){if(Files.isSymbolicLink(file)||Files.size(file)>80)throw new IOException("world_id");id=UUID.fromString(Files.readString(file).trim());}
    else{id=UUID.randomUUID();Files.writeString(file,id.toString(),StandardOpenOption.CREATE_NEW);}
    UUID result=id;
    server.execute(()->{if(closed)return;store=loaded;world=result;for(var p:server.getPlayerList().getPlayers())hello(p);});
   }catch(Exception e){
    com.mojang.logging.LogUtils.getLogger().error("Cat painting world store unavailable",e);
    server.execute(()->{if(!closed){unavailable=true;for(var p:server.getPlayerList().getPlayers())hello(p);}});
   }
  });
 }
 private boolean work(Runnable task){try{io.execute(task);return true;}catch(RejectedExecutionException e){return false;}}
 public void hello(ServerPlayer player){if(world!=null)send(player,new PaintingWire.Hello(world));else if(unavailable)send(player,new PaintingWire.Result(NONE,false,"","store_unavailable"));}
 private void send(ServerPlayer p,PaintingWire.Message m){if(!closed && server.getPlayerList().getPlayer(p.getUUID())==p)ModNetwork.painting(p,m);}
 private boolean rate(Map<UUID,Deque<Long>> map,UUID player,int max){
  long now=System.currentTimeMillis();var list=map.computeIfAbsent(player,k->new ArrayDeque<>());
  while(!list.isEmpty() && now-list.peekFirst()>60000)list.removeFirst();
  if(list.size()>=max)return false;list.addLast(now);return true;
 }
 public UUID begin(ServerPlayer p,int menuId,long revision,PaintingSpec spec,int totalBytes,String hash){
  return begin(p,UUID.randomUUID(),menuId,revision,spec,totalBytes,hash);
 }
 private UUID begin(ServerPlayer p,UUID token,int menuId,long revision,PaintingSpec spec,int totalBytes,String hash){
  if(closed || store==null || totalBytes<1 || totalBytes>PaintingImages.MAX_PNG_BYTES || !PaintingAssetStore.validHash(hash) ||
   p.containerMenu.containerId!=menuId || !(p.containerMenu instanceof PaintingEditorAccess editor) || !p.containerMenu.stillValid(p) ||
   uploads.containsKey(p.getUUID()) || uploads.size()>=8 || !rate(attempts,p.getUUID(),4)){
    send(p,new PaintingWire.Result(token,false,"","upload_unavailable"));return NONE;
  }
  if(!editor.lock(token,revision)){send(p,new PaintingWire.Result(token,false,"","slot_changed"));return NONE;}
  uploads.put(p.getUUID(),new Upload(p,editor,menuId,revision,spec,hash,new PaintingChunkAssembler(token,totalBytes,System.currentTimeMillis())));
  send(p,new PaintingWire.Ack(token,0));return token;
 }
 private boolean valid(Upload u){
  return !closed && server.getPlayerList().getPlayer(u.player.getUUID())==u.player && u.player.containerMenu==u.editor &&
   u.player.containerMenu.containerId==u.menu && u.player.containerMenu.stillValid(u.player) && u.editor.revision()==u.revision;
 }
 private Upload upload(ServerPlayer p,UUID token){
  Upload u=uploads.get(p.getUUID());return u!=null && u.player==p && u.chunks.session().equals(token)?u:null;
 }
 public void accept(ServerPlayer p,UUID token,int offset,byte[] bytes){
  Upload u=upload(p,token);if(u==null)return;
  if(!valid(u)||u.writing){fail(p,u,"slot_changed");return;}
  try{u.chunks.accept(offset,bytes,System.currentTimeMillis());send(p,new PaintingWire.Ack(token,u.chunks.nextOffset()));}
  catch(IOException e){fail(p,u,e.getMessage());}
 }
 public void commit(ServerPlayer p,UUID token){
  Upload u=upload(p,token);if(u==null || u.writing)return;
  if(!valid(u) || !u.chunks.complete() || u.chunks.expired(System.currentTimeMillis())){fail(p,u,"upload_expired");return;}
  byte[] bytes;
  try{bytes=u.chunks.finish();}catch(IOException e){fail(p,u,"upload_incomplete");return;}
  u.writing=true;
  if(!work(()->{
   try{
    if(!PaintingAssetStore.hash(bytes).equals(u.hash))throw new IOException("upload_hash");
    byte[] canonical=PaintingImages.validateUpload(bytes,u.spec);
    var asset=store.put(canonical,u.spec,p.getUUID());
    server.execute(()->{
     if(uploads.get(p.getUUID())!=u)return;
     if(valid(u) && u.editor.commit(token,u.revision,new CatPaintingData(1,asset.hash(),u.spec,p.getUUID()))){
      uploads.remove(p.getUUID());u.editor.unlock(token);send(p,new PaintingWire.Result(token,true,asset.hash(),""));
     }else fail(p,u,"slot_changed");
    });
   }catch(Exception e){server.execute(()->{if(uploads.get(p.getUUID())==u)fail(p,u,"image_rejected");});}
  }))fail(p,u,"server_busy");
 }
 private void fail(ServerPlayer p,Upload u,String error){
  uploads.remove(p.getUUID(),u);u.editor.unlock(u.chunks.session());send(p,new PaintingWire.Result(u.chunks.session(),false,"",error));
 }
 public void cancel(ServerPlayer p,UUID token){
  Upload u=upload(p,token);if(u!=null)fail(p,u,"cancelled");
  var download=downloads.get(p.getUUID());if(download!=null && download.session.equals(token))downloads.remove(p.getUUID());
 }
 public void disconnected(ServerPlayer p){
  Upload u=uploads.get(p.getUUID());if(u!=null && u.player==p)fail(p,u,"disconnected");
  tracked.remove(p.getUUID());downloads.remove(p.getUUID());fetching.remove(p.getUUID());attempts.remove(p.getUUID());fetches.remove(p.getUUID());
 }
 public void track(ServerPlayer p,Entity entity,boolean start){
  if(!(entity instanceof CatPaintingEntity painting))return;
  var map=tracked.computeIfAbsent(p.getUUID(),k->new HashMap<>());
  if(start)painting.getPaintingData().ifPresent(data->map.put(entity.getUUID(),data.hash()));else map.remove(entity.getUUID());
 }
 private boolean authorized(ServerPlayer p,String hash){
  var map=tracked.get(p.getUUID());
  if(map!=null && map.containsValue(hash))return true;
  for(var slot:p.containerMenu.slots)if(CatPaintingData.read(slot.getItem()).map(d->d.hash().equals(hash)).orElse(false))return true;
  for(var stack:p.getInventory().items)if(CatPaintingData.read(stack).map(d->d.hash().equals(hash)).orElse(false))return true;
  return CatPaintingData.read(p.getOffhandItem()).map(d->d.hash().equals(hash)).orElse(false);
 }
 public void request(ServerPlayer p,UUID token,String hash){
  if(closed||store==null||downloads.size()+fetching.size()>=8||!PaintingAssetStore.validHash(hash)||!authorized(p,hash)||downloads.containsKey(p.getUUID())||
    fetching.contains(p.getUUID())||!rate(fetches,p.getUUID(),24))return;
  fetching.add(p.getUUID());
  if(!work(()->{
   try{byte[] bytes=store.read(hash);server.execute(()->{
    fetching.remove(p.getUUID());if(closed||server.getPlayerList().getPlayer(p.getUUID())!=p||!authorized(p,hash))return;
    downloads.put(p.getUUID(),new Download(token,hash,bytes));send(p,new PaintingWire.Manifest(token,hash,bytes.length));
   });}catch(IOException e){server.execute(()->{fetching.remove(p.getUUID());send(p,new PaintingWire.Result(token,false,"","asset_missing"));});}
  }))fetching.remove(p.getUUID());
 }
 public void handle(ServerPlayer p,PaintingWire.Message m){
  if(m instanceof PaintingWire.Begin v)begin(p,v.request(),v.menuId(),v.revision(),v.spec(),v.totalBytes(),v.hash());
  else if(m instanceof PaintingWire.Chunk v)accept(p,v.session(),v.offset(),v.bytes());
  else if(m instanceof PaintingWire.Commit v)commit(p,v.session());
  else if(m instanceof PaintingWire.Cancel v)cancel(p,v.session());
  else if(m instanceof PaintingWire.Fetch v)request(p,v.session(),v.hash());
 }
 public void tick(){
  for(Upload u:new ArrayList<>(uploads.values()))if(!valid(u)||u.chunks.expired(System.currentTimeMillis()))fail(u.player,u,"upload_expired");
  for(var entry:new ArrayList<>(downloads.entrySet())){
   var p=server.getPlayerList().getPlayer(entry.getKey());var d=entry.getValue();
   if(p==null||!authorized(p,d.hash)){downloads.remove(entry.getKey());continue;}
   int count=Math.min(24576,d.bytes.length-d.offset);
   send(p,new PaintingWire.DownloadChunk(d.session,d.offset,Arrays.copyOfRange(d.bytes,d.offset,d.offset+count)));
   d.offset+=count;if(d.offset==d.bytes.length)downloads.remove(entry.getKey());
  }
 }
 public long usedBytes(){return store==null?0:store.usedBytes();}
 @Override public void close(){
  closed=true;for(var u:uploads.values())u.editor.unlock(u.chunks.session());uploads.clear();downloads.clear();tracked.clear();io.shutdownNow();
 }
}

