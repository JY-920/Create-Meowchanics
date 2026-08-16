package cn.laowu.mod.network;

import cn.laowu.mod.CatArmorPounceBehavior;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client intent only; all equipment, target and cooldown checks run server-side. */
public final class CatArmorPouncePacket {
    public static void encode(CatArmorPouncePacket packet, FriendlyByteBuf buffer) {
    }

    public static CatArmorPouncePacket decode(FriendlyByteBuf buffer) {
        return new CatArmorPouncePacket();
    }

    public static void handle(CatArmorPouncePacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                CatArmorPounceBehavior.tryStart(context.getSender());
            }
        });
        context.setPacketHandled(true);
    }
}
