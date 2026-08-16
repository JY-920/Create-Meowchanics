package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncCatChestPacket(int entityId, boolean hasChest) {
    public static void encode(SyncCatChestPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeBoolean(packet.hasChest);
    }

    public static SyncCatChestPacket decode(FriendlyByteBuf buf) {
        return new SyncCatChestPacket(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(SyncCatChestPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleChest(packet)));
        context.setPacketHandled(true);
    }
}
