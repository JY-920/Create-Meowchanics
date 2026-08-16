package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncCatPosePacket(int entityId, int pose) {
    public static void encode(SyncCatPosePacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId); buf.writeVarInt(packet.pose);
    }
    public static SyncCatPosePacket decode(FriendlyByteBuf buf) {
        return new SyncCatPosePacket(buf.readVarInt(), buf.readVarInt());
    }
    public static void handle(SyncCatPosePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handle(packet)));
        context.setPacketHandled(true);
    }
}
