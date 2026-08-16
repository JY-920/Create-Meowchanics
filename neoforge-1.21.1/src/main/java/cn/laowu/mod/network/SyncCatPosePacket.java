package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncCatPosePacket(int entityId, int pose) implements CustomPacketPayload {
    public static final Type<SyncCatPosePacket> TYPE = new Type<>(LaoWuMod.id("sync_cat_pose"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCatPosePacket> STREAM_CODEC =
            StreamCodec.of(SyncCatPosePacket::encode, SyncCatPosePacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncCatPosePacket packet) {
        buf.writeVarInt(packet.entityId); buf.writeVarInt(packet.pose);
    }

    private static SyncCatPosePacket decode(RegistryFriendlyByteBuf buf) {
        return new SyncCatPosePacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(SyncCatPosePacket packet, IPayloadContext context) {
        ClientPacketHandler.handle(packet);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
