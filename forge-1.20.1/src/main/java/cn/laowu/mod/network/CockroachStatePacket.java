package cn.laowu.mod.network;
import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record CockroachStatePacket(int entityId, java.util.UUID uuid, int allies, int mode, int age) {
    public static void encode(CockroachStatePacket p,FriendlyByteBuf b){
        b.writeVarInt(p.entityId);b.writeUUID(p.uuid);b.writeVarInt(p.allies);b.writeVarInt(p.mode);b.writeVarInt(p.age);
    }
    public static CockroachStatePacket decode(FriendlyByteBuf b){
        return new CockroachStatePacket(b.readVarInt(),b.readUUID(),b.readVarInt(),b.readVarInt(),b.readVarInt());
    }
    public static void handle(CockroachStatePacket p,Supplier<NetworkEvent.Context> source){
        var context=source.get();
        context.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientPacketHandler.handleCockroachState(p)));
        context.setPacketHandled(true);
    }
}
