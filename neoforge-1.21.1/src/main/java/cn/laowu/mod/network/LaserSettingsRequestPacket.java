package cn.laowu.mod.network;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LaserSettingsRequestPacket(int action) implements CustomPacketPayload {
    public static final Type<LaserSettingsRequestPacket> TYPE = new Type<>(LaoWuMod.id("laser_settings_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LaserSettingsRequestPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> buffer.writeByte(packet.action()),
                    buffer -> new LaserSettingsRequestPacket(buffer.readByte()));
    public static void handle(LaserSettingsRequestPacket packet, IPayloadContext context) {
        if (context.player() instanceof net.minecraft.server.level.ServerPlayer player)
            cn.laowu.mod.CatCombatPreferences.request(player, packet.action());
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
