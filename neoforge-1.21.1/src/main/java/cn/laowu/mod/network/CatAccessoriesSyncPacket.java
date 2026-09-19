package cn.laowu.mod.network;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CatAccessoriesSyncPacket(int entityId, CompoundTag data) implements CustomPacketPayload {
    public CatAccessoriesSyncPacket { data = data.copy(); }
    public static final Type<CatAccessoriesSyncPacket> TYPE = new Type<>(LaoWuMod.id("cat_accessories_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CatAccessoriesSyncPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                buffer.writeVarInt(packet.entityId());
                buffer.writeNbt(packet.data());
            }, buffer -> {
                int id = buffer.readVarInt();
                CompoundTag data = buffer.readNbt();
                return new CatAccessoriesSyncPacket(id, data == null ? new CompoundTag() : data);
            });
    public static void handle(CatAccessoriesSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> cn.laowu.mod.client.CatAccessoryClient.receive(packet));
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
