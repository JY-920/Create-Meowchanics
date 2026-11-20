package cn.laowu.mod.network;
import cn.laowu.mod.LaoWuMod;import cn.laowu.mod.painting.*;import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
public record PaintingPacket(byte[] bytes) implements CustomPacketPayload {
 public static final Type<PaintingPacket> TYPE=new Type<>(LaoWuMod.id("painting"));
 public static final StreamCodec<RegistryFriendlyByteBuf,PaintingPacket> STREAM_CODEC=StreamCodec.of((b,p)->b.writeByteArray(p.bytes),b->new PaintingPacket(b.readByteArray(25600)));
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
 public static void handle(PaintingPacket packet,IPayloadContext context){
  try{var m=PaintingWire.decode(packet.bytes);
   if(context.player() instanceof net.minecraft.server.level.ServerPlayer player){if(PaintingWire.clientToServer(m))PaintingService.get(player.server).handle(player,m);}
   else if(!PaintingWire.clientToServer(m))cn.laowu.mod.client.painting.PaintingClientCache.receive(m);
  }catch(java.io.IOException ignored){}
 }
}
