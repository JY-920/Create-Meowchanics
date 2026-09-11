package cn.laowu.mod.network;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WorldSettingsRequestPacket(boolean apply, CompoundTag values) implements CustomPacketPayload {
    public WorldSettingsRequestPacket { values = values.copy(); }
    public static final Type<WorldSettingsRequestPacket> TYPE = new Type<>(LaoWuMod.id("world_settings_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WorldSettingsRequestPacket> STREAM_CODEC =
            StreamCodec.of(WorldSettingsRequestPacket::encode, WorldSettingsRequestPacket::decode);
    private static void encode(RegistryFriendlyByteBuf buffer, WorldSettingsRequestPacket packet) {
        buffer.writeBoolean(packet.apply());
        buffer.writeNbt(packet.values());
    }
    private static WorldSettingsRequestPacket decode(RegistryFriendlyByteBuf buffer) {
        boolean apply = buffer.readBoolean();
        CompoundTag values = buffer.readNbt();
        return new WorldSettingsRequestPacket(apply, values == null ? new CompoundTag() : values);
    }
    public static void handle(WorldSettingsRequestPacket packet, IPayloadContext context) {
        if (context.player() instanceof net.minecraft.server.level.ServerPlayer player)
            cn.laowu.mod.ServerConfig.request(player, packet.apply(), packet.values());
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
