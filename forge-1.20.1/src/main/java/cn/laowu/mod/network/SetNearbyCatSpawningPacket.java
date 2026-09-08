package cn.laowu.mod.network;
import cn.laowu.mod.genetics.NaturalCatMaterialSpawner;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record SetNearbyCatSpawningPacket(boolean enabled) {
    public static void encode(SetNearbyCatSpawningPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.enabled());
    }
    public static SetNearbyCatSpawningPacket decode(FriendlyByteBuf buffer) {
        return new SetNearbyCatSpawningPacket(buffer.readBoolean());
    }
    public static void handle(SetNearbyCatSpawningPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) player.getPersistentData().putBoolean(
                    NaturalCatMaterialSpawner.DISABLED_FOR_PLAYER_TAG, !packet.enabled());
        });
        context.setPacketHandled(true);
    }
}
