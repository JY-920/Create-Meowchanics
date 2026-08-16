package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncCatClothesPacket(int entityId, int outfit) {
    public static void encode(SyncCatClothesPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeVarInt(packet.outfit);
    }

    public static SyncCatClothesPacket decode(FriendlyByteBuf buffer) {
        return new SyncCatClothesPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(SyncCatClothesPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleClothes(packet)));
        context.setPacketHandled(true);
    }
}
