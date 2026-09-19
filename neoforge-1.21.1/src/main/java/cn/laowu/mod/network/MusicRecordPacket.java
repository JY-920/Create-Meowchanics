package cn.laowu.mod.network;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.client.ClientPacketHandler;
import cn.laowu.mod.compat.netmusic.NetMusicDiscCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MusicRecordPacket(int entityId, java.util.UUID uuid, String sound, long sequence, int remaining,
                                String networkUrl, String title) implements CustomPacketPayload {
    public MusicRecordPacket(int entityId, java.util.UUID uuid, String sound, long sequence, int remaining) {
        this(entityId, uuid, sound, sequence, remaining, "", "");
    }
    public static final Type<MusicRecordPacket> TYPE = new Type<>(LaoWuMod.id("music_record"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MusicRecordPacket> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeVarInt(p.entityId); b.writeUUID(p.uuid); b.writeUtf(p.sound,256); b.writeLong(p.sequence); b.writeVarInt(p.remaining);
                b.writeUtf(p.networkUrl, NetMusicDiscCompat.MAX_URL); b.writeUtf(p.title, NetMusicDiscCompat.MAX_TITLE); },
            b -> new MusicRecordPacket(b.readVarInt(), b.readUUID(), b.readUtf(256), b.readLong(), b.readVarInt(),
                    b.readUtf(NetMusicDiscCompat.MAX_URL), b.readUtf(NetMusicDiscCompat.MAX_TITLE)));
    public static void handle(MusicRecordPacket p, IPayloadContext context) { ClientPacketHandler.handleMusicRecord(p); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
