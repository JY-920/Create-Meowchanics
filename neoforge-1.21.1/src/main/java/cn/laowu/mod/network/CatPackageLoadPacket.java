package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Starts the same kind of client-only package motion used by Create's frogport renderer. */
public record CatPackageLoadPacket(int catId, BlockPos seat, ItemStack parcel, int duration)
        implements CustomPacketPayload {
    public static final Type<CatPackageLoadPacket> TYPE = new Type<>(LaoWuMod.id("cat_package_load"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CatPackageLoadPacket> STREAM_CODEC =
            StreamCodec.of(CatPackageLoadPacket::encode, CatPackageLoadPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, CatPackageLoadPacket packet) {
        buffer.writeVarInt(packet.catId);
        buffer.writeBlockPos(packet.seat);
        ItemStack.STREAM_CODEC.encode(buffer, packet.parcel);
        buffer.writeVarInt(packet.duration);
    }

    private static CatPackageLoadPacket decode(RegistryFriendlyByteBuf buffer) {
        return new CatPackageLoadPacket(buffer.readVarInt(), buffer.readBlockPos(),
                ItemStack.STREAM_CODEC.decode(buffer), buffer.readVarInt());
    }

    public static void handle(CatPackageLoadPacket packet, IPayloadContext context) {
        ClientPacketHandler.handlePackageLoad(packet);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
