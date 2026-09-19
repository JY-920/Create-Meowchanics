package cn.laowu.mod.network;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
public record AgentMeleePacket(int entityId, java.util.UUID uuid, int move, int duration, int age) implements CustomPacketPayload {
    public static final Type<AgentMeleePacket> TYPE = new Type<>(LaoWuMod.id("agent_melee"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AgentMeleePacket> STREAM_CODEC = StreamCodec.of(
            (b,p)->{b.writeVarInt(p.entityId);b.writeUUID(p.uuid);b.writeVarInt(p.move);b.writeVarInt(p.duration);b.writeVarInt(p.age);},
            b->new AgentMeleePacket(b.readVarInt(),b.readUUID(),b.readVarInt(),b.readVarInt(),b.readVarInt()));
    public static void handle(AgentMeleePacket p,IPayloadContext context){ClientPacketHandler.handleAgentMelee(p);}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
