package cn.laowu.mod.network;

import cn.laowu.mod.CatFilterMenu;
import cn.laowu.mod.item.CatFilterRules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Updates the staged name condition without mutating the held filter mid-screen. */
public record SetCatFilterNamePacket(int containerId, String name) {
    public static void encode(SetCatFilterNamePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeUtf(packet.name, CatFilterRules.MAX_NAME_LENGTH);
    }

    public static SetCatFilterNamePacket decode(FriendlyByteBuf buffer) {
        return new SetCatFilterNamePacket(buffer.readVarInt(),
                buffer.readUtf(CatFilterRules.MAX_NAME_LENGTH));
    }

    public static void handle(SetCatFilterNamePacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null
                    || !(player.containerMenu instanceof CatFilterMenu menu)
                    || menu.containerId != packet.containerId) return;
            menu.setNameQuery(packet.name);
        });
        context.setPacketHandled(true);
    }
}
