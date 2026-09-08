package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Compact tracking update for the cat's visible genome. */
public record SyncCatGenomePacket(int entityId, CompoundTag genome) implements CustomPacketPayload {
    public SyncCatGenomePacket {
        genome = genome.copy();
    }

    public static final Type<SyncCatGenomePacket> TYPE =
            new Type<>(LaoWuMod.id("sync_cat_genome"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCatGenomePacket> STREAM_CODEC =
            StreamCodec.of(SyncCatGenomePacket::encode, SyncCatGenomePacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncCatGenomePacket packet) {
        buf.writeVarInt(packet.entityId);
        buf.writeNbt(packet.genome);
    }

    private static SyncCatGenomePacket decode(RegistryFriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        CompoundTag genome = buf.readNbt();
        return new SyncCatGenomePacket(entityId,
                genome == null ? new CompoundTag() : genome);
    }

    public static void handle(SyncCatGenomePacket packet, IPayloadContext context) {
        ClientPacketHandler.handleGenome(packet);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
