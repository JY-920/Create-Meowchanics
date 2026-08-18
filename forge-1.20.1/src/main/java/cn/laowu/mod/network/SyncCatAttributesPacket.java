package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncCatAttributesPacket(int entityId, CompoundTag attributes) {
    public SyncCatAttributesPacket {
        attributes = attributes.copy();
    }

    public static void encode(SyncCatAttributesPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeNbt(packet.attributes);
    }

    public static SyncCatAttributesPacket decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        CompoundTag attributes = buffer.readNbt();
        return new SyncCatAttributesPacket(entityId,
                attributes == null ? new CompoundTag() : attributes);
    }

    public static void handle(SyncCatAttributesPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleAttributes(packet)));
        context.setPacketHandled(true);
    }
}
