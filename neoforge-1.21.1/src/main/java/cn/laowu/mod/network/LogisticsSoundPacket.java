package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A short positional takeoff/arrival sound; unlike hissing audio it never loops. */
public record LogisticsSoundPacket(double x, double y, double z, boolean arrival) implements CustomPacketPayload {
    public static final Type<LogisticsSoundPacket> TYPE = new Type<>(LaoWuMod.id("logistics_sound"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LogisticsSoundPacket> STREAM_CODEC =
            StreamCodec.of(LogisticsSoundPacket::encode, LogisticsSoundPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, LogisticsSoundPacket packet) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeBoolean(packet.arrival);
    }

    private static LogisticsSoundPacket decode(RegistryFriendlyByteBuf buffer) {
        return new LogisticsSoundPacket(buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readBoolean());
    }

    public static void handle(LogisticsSoundPacket packet, IPayloadContext context) {
        ClientPacketHandler.handleLogisticsSound(packet);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
