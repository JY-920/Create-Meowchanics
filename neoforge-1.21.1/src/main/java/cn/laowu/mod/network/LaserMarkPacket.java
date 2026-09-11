package cn.laowu.mod.network;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
public record LaserMarkPacket(int id, java.util.UUID uuid) implements CustomPacketPayload {
    public static final Type<LaserMarkPacket> TYPE = new Type<>(LaoWuMod.id("laser_mark"));
    public static final StreamCodec<RegistryFriendlyByteBuf,LaserMarkPacket> STREAM_CODEC =
        StreamCodec.of((b,p) -> { b.writeInt(p.id); b.writeUUID(p.uuid); }, b -> new LaserMarkPacket(b.readInt(), b.readUUID()));
    public static void handle(LaserMarkPacket p, IPayloadContext c) { cn.laowu.mod.client.CatLaserEffects.mark(p.id, p.uuid); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
