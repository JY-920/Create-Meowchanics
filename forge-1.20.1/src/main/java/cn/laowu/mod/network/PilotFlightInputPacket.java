package cn.laowu.mod.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record PilotFlightInputPacket(float forward, float side, float yaw, float pitch, boolean up, boolean down) {
    public static void encode(PilotFlightInputPacket p, FriendlyByteBuf b) { b.writeFloat(p.forward); b.writeFloat(p.side); b.writeFloat(p.yaw); b.writeFloat(p.pitch); b.writeBoolean(p.up); b.writeBoolean(p.down); }
    public static PilotFlightInputPacket decode(FriendlyByteBuf b) { return new PilotFlightInputPacket(b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readBoolean(), b.readBoolean()); }
    public static void handle(PilotFlightInputPacket p, Supplier<NetworkEvent.Context> source) {
        var context = source.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null && player.getVehicle() instanceof cn.laowu.mod.entity.CatFlightCarrier carrier)
                carrier.input(player, p.forward, p.side, p.yaw, p.pitch, p.up, p.down);
            else if (player != null && player.getVehicle() instanceof cn.laowu.mod.entity.CatDivingCarrier carrier)
                carrier.input(player, p.forward, p.side, p.yaw, p.pitch, p.up, p.down);
        });
        context.setPacketHandled(true);
    }
}
