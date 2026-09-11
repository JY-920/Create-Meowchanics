package cn.laowu.mod.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
public record LaserMarkPacket(int id, java.util.UUID uuid) {
    public static void encode(LaserMarkPacket p, FriendlyByteBuf b) { b.writeInt(p.id); b.writeUUID(p.uuid); }
    public static LaserMarkPacket decode(FriendlyByteBuf b) { return new LaserMarkPacket(b.readInt(), b.readUUID()); }
    public static void handle(LaserMarkPacket p, java.util.function.Supplier<NetworkEvent.Context> s) {
        var c=s.get();
        c.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> cn.laowu.mod.client.CatLaserEffects.mark(p.id, p.uuid)));
        c.setPacketHandled(true);
    }
}
