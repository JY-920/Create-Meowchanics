package cn.laowu.mod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record LaserSettingsSyncPacket(boolean aggressive) {
    public static void encode(LaserSettingsSyncPacket packet, FriendlyByteBuf buffer) { buffer.writeBoolean(packet.aggressive()); }
    public static LaserSettingsSyncPacket decode(FriendlyByteBuf buffer) { return new LaserSettingsSyncPacket(buffer.readBoolean()); }
    public static void handle(LaserSettingsSyncPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> cn.laowu.mod.client.CatLaserWheelScreen.receive(packet.aggressive())));
        context.setPacketHandled(true);
    }
}
