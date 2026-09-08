package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Compact tracking update for the cat's saved attribute loci. */
public record SyncCatAttributesPacket(int entityId, CompoundTag attributes) implements CustomPacketPayload {
    public SyncCatAttributesPacket {
        attributes = attributes.copy();
    }

    public static final Type<SyncCatAttributesPacket> TYPE =
            new Type<>(LaoWuMod.id("sync_cat_attributes"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCatAttributesPacket> STREAM_CODEC =
            StreamCodec.of(SyncCatAttributesPacket::encode, SyncCatAttributesPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncCatAttributesPacket packet) {
        buf.writeVarInt(packet.entityId);
        buf.writeNbt(packet.attributes);
    }

    private static SyncCatAttributesPacket decode(RegistryFriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        CompoundTag attributes = buf.readNbt();
        return new SyncCatAttributesPacket(entityId,
                attributes == null ? new CompoundTag() : attributes);
    }

    public static void handle(SyncCatAttributesPacket packet, IPayloadContext context) {
        ClientPacketHandler.handleAttributes(packet);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
