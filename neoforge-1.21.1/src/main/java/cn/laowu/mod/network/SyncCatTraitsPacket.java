package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Compact tracking update for the cat's at-most-four saved traits. */
public record SyncCatTraitsPacket(int entityId, CompoundTag traits) implements CustomPacketPayload {
    public SyncCatTraitsPacket {
        traits = traits.copy();
    }

    public static final Type<SyncCatTraitsPacket> TYPE =
            new Type<>(LaoWuMod.id("sync_cat_traits"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCatTraitsPacket> STREAM_CODEC =
            StreamCodec.of(SyncCatTraitsPacket::encode, SyncCatTraitsPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncCatTraitsPacket packet) {
        buf.writeVarInt(packet.entityId);
        buf.writeNbt(packet.traits);
    }

    private static SyncCatTraitsPacket decode(RegistryFriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        CompoundTag traits = buf.readNbt();
        return new SyncCatTraitsPacket(entityId,
                traits == null ? new CompoundTag() : traits);
    }

    public static void handle(SyncCatTraitsPacket packet, IPayloadContext context) {
        ClientPacketHandler.handleTraits(packet);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
