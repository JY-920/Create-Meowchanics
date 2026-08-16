package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncCatClothesPacket(int entityId, int outfit) implements CustomPacketPayload {
    public static final Type<SyncCatClothesPacket> TYPE =
            new Type<>(LaoWuMod.id("sync_cat_clothes"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCatClothesPacket> STREAM_CODEC =
            StreamCodec.of(SyncCatClothesPacket::encode, SyncCatClothesPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, SyncCatClothesPacket packet) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeVarInt(packet.outfit);
    }

    private static SyncCatClothesPacket decode(RegistryFriendlyByteBuf buffer) {
        return new SyncCatClothesPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(SyncCatClothesPacket packet, IPayloadContext context) {
        ClientPacketHandler.handleClothes(packet);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
