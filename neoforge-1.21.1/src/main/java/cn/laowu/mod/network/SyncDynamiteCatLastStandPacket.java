package cn.laowu.mod.network;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Transition-only state for the Dynamite Cat's final charge and fuse. */
public record SyncDynamiteCatLastStandPacket(int entityId, boolean active,
                                             int fuseTicks)
        implements CustomPacketPayload {
    public static final Type<SyncDynamiteCatLastStandPacket> TYPE =
            new Type<>(LaoWuMod.id("sync_dynamite_cat_last_stand"));
    public static final StreamCodec<RegistryFriendlyByteBuf,
            SyncDynamiteCatLastStandPacket> STREAM_CODEC = StreamCodec.of(
            SyncDynamiteCatLastStandPacket::encode,
            SyncDynamiteCatLastStandPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buffer,
                               SyncDynamiteCatLastStandPacket packet) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeBoolean(packet.active);
        buffer.writeInt(packet.fuseTicks);
    }

    private static SyncDynamiteCatLastStandPacket decode(RegistryFriendlyByteBuf buffer) {
        return new SyncDynamiteCatLastStandPacket(buffer.readVarInt(),
                buffer.readBoolean(), buffer.readInt());
    }

    public static void handle(SyncDynamiteCatLastStandPacket packet,
                              IPayloadContext context) {
        ClientPacketHandler.handleDynamiteLastStand(packet);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
