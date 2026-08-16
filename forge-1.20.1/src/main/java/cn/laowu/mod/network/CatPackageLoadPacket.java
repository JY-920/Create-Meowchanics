package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Starts the same kind of client-only package motion used by Create's frogport renderer. */
public record CatPackageLoadPacket(int catId, BlockPos seat, ItemStack parcel, int duration) {
    public static void encode(CatPackageLoadPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.catId);
        buffer.writeBlockPos(packet.seat);
        buffer.writeItem(packet.parcel);
        buffer.writeVarInt(packet.duration);
    }

    public static CatPackageLoadPacket decode(FriendlyByteBuf buffer) {
        return new CatPackageLoadPacket(buffer.readVarInt(), buffer.readBlockPos(),
                buffer.readItem(), buffer.readVarInt());
    }

    public static void handle(CatPackageLoadPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handlePackageLoad(packet)));
        context.setPacketHandled(true);
    }
}
