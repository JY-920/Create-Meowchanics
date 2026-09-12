package cn.laowu.mod.network;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LaserSettingsSyncPacket(boolean aggressive) implements CustomPacketPayload {
    public static final Type<LaserSettingsSyncPacket> TYPE = new Type<>(LaoWuMod.id("laser_settings_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LaserSettingsSyncPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> buffer.writeBoolean(packet.aggressive()),
                    buffer -> new LaserSettingsSyncPacket(buffer.readBoolean()));
    public static void handle(LaserSettingsSyncPacket packet, IPayloadContext context) {
        cn.laowu.mod.client.CatLaserWheelScreen.receive(packet.aggressive());
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
