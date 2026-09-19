package cn.laowu.mod.network;
import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record AgentWatchPacket(int entityId, java.util.UUID uuid, int duration) {
    public static void encode(AgentWatchPacket p,FriendlyByteBuf b){
        b.writeVarInt(p.entityId);b.writeUUID(p.uuid);b.writeVarInt(p.duration);
    }
    public static AgentWatchPacket decode(FriendlyByteBuf b){
        return new AgentWatchPacket(b.readVarInt(),b.readUUID(),b.readVarInt());
    }
    public static void handle(AgentWatchPacket p,Supplier<NetworkEvent.Context> source){
        var context=source.get();
        context.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientPacketHandler.handleAgentWatch(p)));
        context.setPacketHandled(true);
    }
}
