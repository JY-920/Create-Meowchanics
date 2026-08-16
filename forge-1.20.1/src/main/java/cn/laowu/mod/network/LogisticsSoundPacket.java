package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** A short positional takeoff/arrival sound; unlike hissing audio it never loops. */
public record LogisticsSoundPacket(double x, double y, double z, boolean arrival) {
    public static void encode(LogisticsSoundPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeBoolean(packet.arrival);
    }

    public static LogisticsSoundPacket decode(FriendlyByteBuf buffer) {
        return new LogisticsSoundPacket(buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readBoolean());
    }

    public static void handle(LogisticsSoundPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleLogisticsSound(packet)));
        context.setPacketHandled(true);
    }
}
