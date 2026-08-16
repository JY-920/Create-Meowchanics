package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AudioSessionPacket(int entityId, boolean active) implements CustomPacketPayload {
    public static final Type<AudioSessionPacket> TYPE = new Type<>(LaoWuMod.id("audio_session"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AudioSessionPacket> STREAM_CODEC =
            StreamCodec.of(AudioSessionPacket::encode, AudioSessionPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, AudioSessionPacket packet) {
        buf.writeVarInt(packet.entityId); buf.writeBoolean(packet.active);
    }

    private static AudioSessionPacket decode(RegistryFriendlyByteBuf buf) {
        return new AudioSessionPacket(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(AudioSessionPacket packet, IPayloadContext context) {
        ClientPacketHandler.handleAudio(packet);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
