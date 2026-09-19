package cn.laowu.mod.network;

import cn.laowu.mod.client.CatAccessoryClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Negative entity ID: registry; nonnegative ID: derived state for one tracked cat. Client-bound only. */
public record CatAccessoriesSyncPacket(int entityId, CompoundTag data) {
    public CatAccessoriesSyncPacket { data = data.copy(); }
    public static void encode(CatAccessoriesSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId());
        buffer.writeNbt(packet.data());
    }
    public static CatAccessoriesSyncPacket decode(FriendlyByteBuf buffer) {
        int id = buffer.readVarInt();
        CompoundTag data = buffer.readNbt();
        return new CatAccessoriesSyncPacket(id, data == null ? new CompoundTag() : data);
    }
    public static void handle(CatAccessoriesSyncPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> CatAccessoryClient.receive(packet)));
        context.setPacketHandled(true);
    }
}
