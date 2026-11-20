import cn.laowu.mod.painting.*;import java.util.*;import java.io.*;
public class PaintingWireRegression {
 static void check(boolean v){if(!v)throw new AssertionError("wire mismatch");}
 public static void main(String[] a)throws Exception{
  var spec=new PaintingSpec(2,3,PaintingSpec.Mode.ORIGINAL,PaintingSpec.Crop.KEEP_HEIGHT);
  var id=UUID.randomUUID();
  var messages=List.of(new PaintingWire.Begin(5,12,spec,100,"a".repeat(64)),new PaintingWire.Chunk(id,0,new byte[24576]),
   new PaintingWire.Commit(id),new PaintingWire.Cancel(id),new PaintingWire.Fetch(id,"b".repeat(64)),
   new PaintingWire.Manifest(id,"b".repeat(64),900),new PaintingWire.Ack(id,24),
   new PaintingWire.Result(id,true,"c".repeat(64),""),new PaintingWire.DownloadChunk(id,0,new byte[]{7}),new PaintingWire.Hello(UUID.randomUUID()));
  for(var m:messages){var bytes=PaintingWire.encode(m);check(bytes.length<=25600);check(PaintingWire.decode(bytes).getClass()==m.getClass());}
  boolean rejected=false;try{PaintingWire.decode(new byte[]{100});}catch(IOException e){rejected=true;}check(rejected);
  rejected=false;try{PaintingWire.encode(new PaintingWire.Chunk(id,0,new byte[24577]));}catch(IOException e){rejected=true;}check(rejected);
  var bytes=PaintingWire.encode(new PaintingWire.Commit(id));rejected=false;try{PaintingWire.decode(Arrays.copyOf(bytes,bytes.length-1));}catch(IOException e){rejected=true;}check(rejected);
  check(PaintingWire.clientToServer(messages.get(0)));check(!PaintingWire.clientToServer(messages.get(5)));
  System.out.println("PASS: bounded messages and invalid wire inputs");
 }
}
