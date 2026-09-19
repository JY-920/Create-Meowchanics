package cn.laowu.mod.network;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
public record AgentWatchPacket(int entityId, java.util.UUID uuid, int duration) implements CustomPacketPayload {
    public static final Type<AgentWatchPacket> TYPE = new Type<>(LaoWuMod.id("agent_watch"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AgentWatchPacket> STREAM_CODEC = StreamCodec.of(
            (b,p)->{b.writeVarInt(p.entityId);b.writeUUID(p.uuid);b.writeVarInt(p.duration);},
            b->new AgentWatchPacket(b.readVarInt(),b.readUUID(),b.readVarInt()));
    public static void handle(AgentWatchPacket p,IPayloadContext context){ClientPacketHandler.handleAgentWatch(p);}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
