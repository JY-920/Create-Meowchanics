package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AudioSessionPacket(int entityId, boolean active) {
    public static void encode(AudioSessionPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId); buf.writeBoolean(packet.active);
    }
    public static AudioSessionPacket decode(FriendlyByteBuf buf) {
        return new AudioSessionPacket(buf.readVarInt(), buf.readBoolean());
    }
    public static void handle(AudioSessionPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleAudio(packet)));
        context.setPacketHandled(true);
    }
}
