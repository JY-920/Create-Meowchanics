package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Transition-only sync for transient trait conditions used by the cat panel. */
public record SyncCatTraitStatePacket(int entityId, boolean rageActive,
                                      boolean luBuOutnumbered,
                                      boolean timidOutnumbered) {
    public static void encode(SyncCatTraitStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeBoolean(packet.rageActive);
        buffer.writeBoolean(packet.luBuOutnumbered);
        buffer.writeBoolean(packet.timidOutnumbered);
    }

    public static SyncCatTraitStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncCatTraitStatePacket(buffer.readVarInt(),
                buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(SyncCatTraitStatePacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleTraitState(packet)));
        context.setPacketHandled(true);
    }
}
