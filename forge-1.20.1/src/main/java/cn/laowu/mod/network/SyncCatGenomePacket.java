package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncCatGenomePacket(int entityId, CompoundTag genome) {
    public SyncCatGenomePacket {
        genome = genome.copy();
    }

    public static void encode(SyncCatGenomePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeNbt(packet.genome);
    }

    public static SyncCatGenomePacket decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        CompoundTag genome = buffer.readNbt();
        return new SyncCatGenomePacket(entityId,
                genome == null ? new CompoundTag() : genome);
    }

    public static void handle(SyncCatGenomePacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleGenome(packet)));
        context.setPacketHandled(true);
    }
}
