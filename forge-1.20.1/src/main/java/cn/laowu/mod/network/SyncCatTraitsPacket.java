package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Compact tracking update for the cat's at-most-four saved traits. */
public record SyncCatTraitsPacket(int entityId, CompoundTag traits) {
    public SyncCatTraitsPacket {
        traits = traits.copy();
    }

    public static void encode(SyncCatTraitsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeNbt(packet.traits);
    }

    public static SyncCatTraitsPacket decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        CompoundTag traits = buffer.readNbt();
        return new SyncCatTraitsPacket(entityId,
                traits == null ? new CompoundTag() : traits);
    }

    public static void handle(SyncCatTraitsPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleTraits(packet)));
        context.setPacketHandled(true);
    }
}
