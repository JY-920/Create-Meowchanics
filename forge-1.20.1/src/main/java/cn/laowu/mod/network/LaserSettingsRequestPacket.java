package cn.laowu.mod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record LaserSettingsRequestPacket(int action) {
    public static void encode(LaserSettingsRequestPacket packet, FriendlyByteBuf buffer) { buffer.writeByte(packet.action()); }
    public static LaserSettingsRequestPacket decode(FriendlyByteBuf buffer) { return new LaserSettingsRequestPacket(buffer.readByte()); }
    public static void handle(LaserSettingsRequestPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) cn.laowu.mod.CatCombatPreferences.request(player, packet.action());
        });
        context.setPacketHandled(true);
    }
}
