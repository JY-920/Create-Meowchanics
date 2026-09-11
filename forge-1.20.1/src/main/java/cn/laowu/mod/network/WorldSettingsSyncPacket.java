package cn.laowu.mod.network;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record WorldSettingsSyncPacket(CompoundTag values) {
    public WorldSettingsSyncPacket { values = values.copy(); }
    public static void encode(WorldSettingsSyncPacket packet, FriendlyByteBuf buffer) {

        buffer.writeNbt(packet.values());
    }
    public static WorldSettingsSyncPacket decode(FriendlyByteBuf buffer) {

        CompoundTag values = buffer.readNbt();
        return new WorldSettingsSyncPacket(values == null ? new CompoundTag() : values);
    }
    public static void handle(WorldSettingsSyncPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> { cn.laowu.mod.client.ClientWorldSettings.receive(packet.values()); });
        });
        context.setPacketHandled(true);
    }
}
