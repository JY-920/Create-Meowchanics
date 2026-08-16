package cn.laowu.mod.network;

import cn.laowu.mod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Replaces the vanilla totem overlay with the Cat Totem for its owning player. */
public record CatTotemActivationPacket() {
    public static void encode(CatTotemActivationPacket packet, FriendlyByteBuf buffer) {
    }

    public static CatTotemActivationPacket decode(FriendlyByteBuf buffer) {
        return new CatTotemActivationPacket();
    }

    public static void handle(CatTotemActivationPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleCatTotemActivation()));
        context.setPacketHandled(true);
    }
}
