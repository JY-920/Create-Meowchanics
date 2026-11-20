package cn.laowu.mod.network;
import cn.laowu.mod.painting.*;import net.minecraft.network.FriendlyByteBuf;import net.minecraftforge.network.NetworkEvent;import java.util.function.Supplier;
public record PaintingPacket(byte[] bytes) {
 public static void encode(PaintingPacket p,FriendlyByteBuf b){b.writeByteArray(p.bytes);}
 public static PaintingPacket decode(FriendlyByteBuf b){return new PaintingPacket(b.readByteArray(25600));}
 public static void handle(PaintingPacket p,Supplier<NetworkEvent.Context> supplier){
  var context=supplier.get();context.enqueueWork(()->{
   try{var m=PaintingWire.decode(p.bytes);
    if(context.getSender()!=null){if(PaintingWire.clientToServer(m))PaintingService.get(context.getSender().server).handle(context.getSender(),m);}
    else if(!PaintingWire.clientToServer(m))net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->cn.laowu.mod.client.painting.PaintingClientCache.receive(m));
   }catch(java.io.IOException ignored){}
  });context.setPacketHandled(true);
 }
}
