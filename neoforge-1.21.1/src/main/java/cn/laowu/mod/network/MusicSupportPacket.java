package cn.laowu.mod.network;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MusicSupportPacket(int entityId, java.util.UUID uuid, int pose, int age, double bonus, float radius) implements CustomPacketPayload {
    public static final Type<MusicSupportPacket> TYPE = new Type<>(LaoWuMod.id("music_support"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MusicSupportPacket> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeVarInt(p.entityId); b.writeUUID(p.uuid); b.writeVarInt(p.pose); b.writeVarInt(p.age); b.writeDouble(p.bonus); b.writeFloat(p.radius); },
            b -> new MusicSupportPacket(b.readVarInt(), b.readUUID(), b.readVarInt(), b.readVarInt(), b.readDouble(), b.readFloat()));
    public static void handle(MusicSupportPacket p, IPayloadContext context) { ClientPacketHandler.handleMusicSupport(p); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
