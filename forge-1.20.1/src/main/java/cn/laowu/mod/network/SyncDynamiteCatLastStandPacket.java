package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Transition-only state for the Dynamite Cat's final charge and fuse. */
public record SyncDynamiteCatLastStandPacket(int entityId, boolean active,
                                             int fuseTicks) {
    public static void encode(SyncDynamiteCatLastStandPacket packet,
                              FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeBoolean(packet.active);
        buffer.writeInt(packet.fuseTicks);
    }

    public static SyncDynamiteCatLastStandPacket decode(FriendlyByteBuf buffer) {
        return new SyncDynamiteCatLastStandPacket(buffer.readVarInt(),
                buffer.readBoolean(), buffer.readInt());
    }

    public static void handle(SyncDynamiteCatLastStandPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleDynamiteLastStand(packet)));
        context.setPacketHandled(true);
    }
}
