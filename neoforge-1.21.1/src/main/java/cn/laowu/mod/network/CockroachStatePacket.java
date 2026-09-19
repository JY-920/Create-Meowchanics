package cn.laowu.mod.network;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
public record CockroachStatePacket(int entityId, java.util.UUID uuid, int allies, int mode, int age) implements CustomPacketPayload {
    public static final Type<CockroachStatePacket> TYPE = new Type<>(LaoWuMod.id("cockroach_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CockroachStatePacket> STREAM_CODEC = StreamCodec.of(
            (b,p)->{b.writeVarInt(p.entityId);b.writeUUID(p.uuid);b.writeVarInt(p.allies);b.writeVarInt(p.mode);b.writeVarInt(p.age);},
            b->new CockroachStatePacket(b.readVarInt(),b.readUUID(),b.readVarInt(),b.readVarInt(),b.readVarInt()));
    public static void handle(CockroachStatePacket p,IPayloadContext context){ClientPacketHandler.handleCockroachState(p);}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
