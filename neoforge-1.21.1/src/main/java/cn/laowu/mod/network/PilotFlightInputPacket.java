package cn.laowu.mod.network;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PilotFlightInputPacket(float forward, float side, float yaw, float pitch, boolean up, boolean down) implements CustomPacketPayload {
    public static final Type<PilotFlightInputPacket> TYPE = new Type<>(LaoWuMod.id("pilot_flight_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PilotFlightInputPacket> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeFloat(p.forward); b.writeFloat(p.side); b.writeFloat(p.yaw); b.writeFloat(p.pitch); b.writeBoolean(p.up); b.writeBoolean(p.down); },
            b -> new PilotFlightInputPacket(b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readBoolean(), b.readBoolean()));
    public static void handle(PilotFlightInputPacket p, IPayloadContext context) {
        if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
            if (player.getVehicle() instanceof cn.laowu.mod.entity.CatFlightCarrier carrier)
                carrier.input(player, p.forward, p.side, p.yaw, p.pitch, p.up, p.down);
            else if (player.getVehicle() instanceof cn.laowu.mod.entity.CatDivingCarrier carrier)
                carrier.input(player, p.forward, p.side, p.yaw, p.pitch, p.up, p.down);
        }
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
