package cn.laowu.mod.network;
import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record AgentMeleePacket(int entityId, java.util.UUID uuid, int move, int duration, int age) {
    public static void encode(AgentMeleePacket p,FriendlyByteBuf b){
        b.writeVarInt(p.entityId);b.writeUUID(p.uuid);b.writeVarInt(p.move);b.writeVarInt(p.duration);b.writeVarInt(p.age);
    }
    public static AgentMeleePacket decode(FriendlyByteBuf b){
        return new AgentMeleePacket(b.readVarInt(),b.readUUID(),b.readVarInt(),b.readVarInt(),b.readVarInt());
    }
    public static void handle(AgentMeleePacket p,Supplier<NetworkEvent.Context> source){
        var context=source.get();
        context.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientPacketHandler.handleAgentMelee(p)));
        context.setPacketHandled(true);
    }
}
