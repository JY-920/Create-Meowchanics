package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncCatChestPacket(int entityId, boolean hasChest) implements CustomPacketPayload {
    public static final Type<SyncCatChestPacket> TYPE = new Type<>(LaoWuMod.id("sync_cat_chest"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCatChestPacket> STREAM_CODEC =
            StreamCodec.of(SyncCatChestPacket::encode, SyncCatChestPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncCatChestPacket packet) {
        buf.writeVarInt(packet.entityId);
        buf.writeBoolean(packet.hasChest);
    }

    private static SyncCatChestPacket decode(RegistryFriendlyByteBuf buf) {
        return new SyncCatChestPacket(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(SyncCatChestPacket packet, IPayloadContext context) {
        ClientPacketHandler.handleChest(packet);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
