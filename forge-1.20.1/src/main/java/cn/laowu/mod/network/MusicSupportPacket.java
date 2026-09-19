package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record MusicSupportPacket(int entityId, java.util.UUID uuid, int pose, int age, double bonus, float radius) {
    public static void encode(MusicSupportPacket p, FriendlyByteBuf b) {
        b.writeVarInt(p.entityId); b.writeUUID(p.uuid); b.writeVarInt(p.pose); b.writeVarInt(p.age); b.writeDouble(p.bonus); b.writeFloat(p.radius);
    }
    public static MusicSupportPacket decode(FriendlyByteBuf b) {
        return new MusicSupportPacket(b.readVarInt(), b.readUUID(), b.readVarInt(), b.readVarInt(), b.readDouble(), b.readFloat());
    }
    public static void handle(MusicSupportPacket p, Supplier<NetworkEvent.Context> source) {
        var context = source.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleMusicSupport(p)));
        context.setPacketHandled(true);
    }
}
