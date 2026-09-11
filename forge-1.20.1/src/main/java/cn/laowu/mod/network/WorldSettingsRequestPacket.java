package cn.laowu.mod.network;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record WorldSettingsRequestPacket(boolean apply, CompoundTag values) {
    public WorldSettingsRequestPacket { values = values.copy(); }
    public static void encode(WorldSettingsRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.apply());
        buffer.writeNbt(packet.values());
    }
    public static WorldSettingsRequestPacket decode(FriendlyByteBuf buffer) {
        boolean apply = buffer.readBoolean();
        CompoundTag values = buffer.readNbt();
        return new WorldSettingsRequestPacket(apply, values == null ? new CompoundTag() : values);
    }
    public static void handle(WorldSettingsRequestPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) cn.laowu.mod.ServerConfig.request(player, packet.apply(), packet.values());
        });
        context.setPacketHandled(true);
    }
}
