package cn.laowu.mod.network;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WorldSettingsSyncPacket(CompoundTag values) implements CustomPacketPayload {
    public WorldSettingsSyncPacket { values = values.copy(); }
    public static final Type<WorldSettingsSyncPacket> TYPE = new Type<>(LaoWuMod.id("world_settings_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WorldSettingsSyncPacket> STREAM_CODEC =
            StreamCodec.of(WorldSettingsSyncPacket::encode, WorldSettingsSyncPacket::decode);
    private static void encode(RegistryFriendlyByteBuf buffer, WorldSettingsSyncPacket packet) {

        buffer.writeNbt(packet.values());
    }
    private static WorldSettingsSyncPacket decode(RegistryFriendlyByteBuf buffer) {

        CompoundTag values = buffer.readNbt();
        return new WorldSettingsSyncPacket(values == null ? new CompoundTag() : values);
    }
    public static void handle(WorldSettingsSyncPacket packet, IPayloadContext context) {
        cn.laowu.mod.client.ClientWorldSettings.receive(packet.values());
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
