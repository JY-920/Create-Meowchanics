package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Transition-only sync for transient trait conditions used by the cat panel. */
public record SyncCatTraitStatePacket(int entityId, boolean rageActive,
                                      boolean luBuOutnumbered,
                                      boolean timidOutnumbered,
                                      boolean combatActive) implements CustomPacketPayload {
    public static final Type<SyncCatTraitStatePacket> TYPE =
            new Type<>(LaoWuMod.id("sync_cat_trait_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCatTraitStatePacket> STREAM_CODEC =
            StreamCodec.of(SyncCatTraitStatePacket::encode, SyncCatTraitStatePacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncCatTraitStatePacket packet) {
        buf.writeVarInt(packet.entityId);
        buf.writeBoolean(packet.rageActive);
        buf.writeBoolean(packet.luBuOutnumbered);
        buf.writeBoolean(packet.timidOutnumbered);
        buf.writeBoolean(packet.combatActive);
    }

    private static SyncCatTraitStatePacket decode(RegistryFriendlyByteBuf buf) {
        return new SyncCatTraitStatePacket(buf.readVarInt(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean());
    }

    public static void handle(SyncCatTraitStatePacket packet, IPayloadContext context) {
        ClientPacketHandler.handleTraitState(packet);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
