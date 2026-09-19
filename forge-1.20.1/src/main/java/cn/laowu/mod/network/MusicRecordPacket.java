package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import cn.laowu.mod.compat.netmusic.NetMusicDiscCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record MusicRecordPacket(int entityId, java.util.UUID uuid, String sound, long sequence, int remaining,
                                String networkUrl, String title) {
    public MusicRecordPacket(int entityId, java.util.UUID uuid, String sound, long sequence, int remaining) {
        this(entityId, uuid, sound, sequence, remaining, "", "");
    }
    public static void encode(MusicRecordPacket p, FriendlyByteBuf b) {
        b.writeVarInt(p.entityId); b.writeUUID(p.uuid); b.writeUtf(p.sound,256); b.writeLong(p.sequence); b.writeVarInt(p.remaining);
        b.writeUtf(p.networkUrl, NetMusicDiscCompat.MAX_URL); b.writeUtf(p.title, NetMusicDiscCompat.MAX_TITLE);
    }
    public static MusicRecordPacket decode(FriendlyByteBuf b) {
        return new MusicRecordPacket(b.readVarInt(), b.readUUID(), b.readUtf(256), b.readLong(), b.readVarInt(),
                b.readUtf(NetMusicDiscCompat.MAX_URL), b.readUtf(NetMusicDiscCompat.MAX_TITLE));
    }
    public static void handle(MusicRecordPacket p, Supplier<NetworkEvent.Context> source) {
        var context = source.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleMusicRecord(p)));
        context.setPacketHandled(true);
    }
}
