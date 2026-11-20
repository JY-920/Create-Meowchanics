package cn.laowu.mod.painting;
import java.io.*;import java.util.*;
public final class PaintingWire {
 public sealed interface Message permits Begin,Chunk,Commit,Cancel,Fetch,Manifest,Ack,Result,DownloadChunk,Hello{}
 public record Begin(UUID request,int menuId,long revision,PaintingSpec spec,int totalBytes,String hash)implements Message{
  public Begin(int menuId,long revision,PaintingSpec spec,int totalBytes,String hash){this(UUID.randomUUID(),menuId,revision,spec,totalBytes,hash);}
 }
 public record Chunk(UUID session,int offset,byte[] bytes)implements Message{}
 public record Commit(UUID session)implements Message{}
 public record Cancel(UUID session)implements Message{}
 public record Fetch(UUID session,String hash)implements Message{}
 public record Manifest(UUID session,String hash,int totalBytes)implements Message{}
 public record Ack(UUID session,int nextOffset)implements Message{}
 public record Result(UUID session,boolean success,String hash,String error)implements Message{}
 public record DownloadChunk(UUID session,int offset,byte[] bytes)implements Message{}
 public record Hello(UUID world)implements Message{}
 public static boolean clientToServer(Message m){return m instanceof Begin || m instanceof Chunk || m instanceof Commit || m instanceof Cancel || m instanceof Fetch;}
 private static void uuid(DataOutputStream o,UUID id)throws IOException{if(id==null)throw new IOException("session");o.writeLong(id.getMostSignificantBits());o.writeLong(id.getLeastSignificantBits());}
 private static UUID uuid(DataInputStream i)throws IOException{return new UUID(i.readLong(),i.readLong());}
 private static void hash(String s)throws IOException{if(!PaintingAssetStore.validHash(s))throw new IOException("hash");}
 private static void size(int n)throws IOException{if(n<1 || n>PaintingImages.MAX_PNG_BYTES)throw new IOException("size");}
 private static void chunk(DataOutputStream o,int offset,byte[] b)throws IOException{
  if(offset<0 || offset>PaintingImages.MAX_PNG_BYTES || b==null || b.length<1 || b.length>24576 || (long)offset+b.length>PaintingImages.MAX_PNG_BYTES)throw new IOException("chunk");
  o.writeInt(offset);o.writeInt(b.length);o.write(b);
 }
 private static byte[] chunk(DataInputStream i)throws IOException{
  int n=i.readInt();if(n<1 || n>24576 || n>i.available())throw new IOException("chunk");return i.readNBytes(n);
 }
 public static byte[] encode(Message m)throws IOException{
  var b=new ByteArrayOutputStream();var o=new DataOutputStream(b);
  if(m instanceof Begin v){size(v.totalBytes());hash(v.hash());if(v.menuId()<0 || v.revision()<0)throw new IOException("menu");
   o.writeByte(0);uuid(o,v.request());o.writeInt(v.menuId());o.writeLong(v.revision());o.writeByte(v.spec().widthBlocks());o.writeByte(v.spec().heightBlocks());
   o.writeByte(v.spec().mode().ordinal());o.writeByte(v.spec().crop().ordinal());o.writeInt(v.totalBytes());o.writeUTF(v.hash());}
  else if(m instanceof Chunk v){o.writeByte(1);uuid(o,v.session());chunk(o,v.offset(),v.bytes());}
  else if(m instanceof Commit v){o.writeByte(2);uuid(o,v.session());}
  else if(m instanceof Cancel v){o.writeByte(3);uuid(o,v.session());}
  else if(m instanceof Fetch v){hash(v.hash());o.writeByte(4);uuid(o,v.session());o.writeUTF(v.hash());}
  else if(m instanceof Manifest v){size(v.totalBytes());hash(v.hash());o.writeByte(5);uuid(o,v.session());o.writeUTF(v.hash());o.writeInt(v.totalBytes());}
  else if(m instanceof Ack v){if(v.nextOffset()<0 || v.nextOffset()>PaintingImages.MAX_PNG_BYTES)throw new IOException("offset");o.writeByte(6);uuid(o,v.session());o.writeInt(v.nextOffset());}
  else if(m instanceof Result v){if(v.hash().length()>64 || v.error().length()>160)throw new IOException("result");o.writeByte(7);uuid(o,v.session());o.writeBoolean(v.success());o.writeUTF(v.hash());o.writeUTF(v.error());}
  else if(m instanceof DownloadChunk v){o.writeByte(8);uuid(o,v.session());chunk(o,v.offset(),v.bytes());}
  else if(m instanceof Hello v){o.writeByte(9);uuid(o,v.world());}
  else throw new IOException("message");
  o.flush();if(b.size()>25600)throw new IOException("message_size");return b.toByteArray();
 }
 public static Message decode(byte[] b)throws IOException{
  if(b==null || b.length<1 || b.length>25600)throw new IOException("message_size");
  var i=new DataInputStream(new ByteArrayInputStream(b));Message m;
  try{
   int type=i.readUnsignedByte();
   m=switch(type){
    case 0 -> new Begin(uuid(i),i.readInt(),i.readLong(),new PaintingSpec(i.readUnsignedByte(),i.readUnsignedByte(),PaintingSpec.Mode.values()[i.readUnsignedByte()],PaintingSpec.Crop.values()[i.readUnsignedByte()]),i.readInt(),i.readUTF());
    case 1 -> {UUID id=uuid(i);int offset=i.readInt();yield new Chunk(id,offset,chunk(i));}
    case 2 -> new Commit(uuid(i));case 3 -> new Cancel(uuid(i));case 4 -> new Fetch(uuid(i),i.readUTF());
    case 5 -> new Manifest(uuid(i),i.readUTF(),i.readInt());case 6 -> new Ack(uuid(i),i.readInt());
    case 7 -> new Result(uuid(i),i.readBoolean(),i.readUTF(),i.readUTF());
    case 8 -> {UUID id=uuid(i);int offset=i.readInt();yield new DownloadChunk(id,offset,chunk(i));}
    case 9 -> new Hello(uuid(i));default -> throw new IOException("message_type");
   };
   if(i.available()!=0)throw new IOException("message_trailing");
   encode(m);return m;
  }catch(RuntimeException e){throw new IOException("message_invalid",e);}
 }
}
