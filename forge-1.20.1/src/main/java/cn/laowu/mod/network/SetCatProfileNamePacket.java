package cn.laowu.mod.network;

import cn.laowu.mod.CatProfileData;
import cn.laowu.mod.CatProfileMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Commits the profile screen's name field when the client closes it. */
public record SetCatProfileNamePacket(int catId, String name) {
    public static void encode(SetCatProfileNamePacket packet,
                              FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.catId);
        buffer.writeUtf(packet.name, CatProfileData.MAX_NAME_LENGTH);
    }

    public static SetCatProfileNamePacket decode(FriendlyByteBuf buffer) {
        return new SetCatProfileNamePacket(buffer.readVarInt(),
                buffer.readUtf(CatProfileData.MAX_NAME_LENGTH));
    }

    public static void handle(SetCatProfileNamePacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null
                    || !(player.containerMenu instanceof CatProfileMenu menu)
                    || menu.getCatId() != packet.catId) return;
            Entity entity = player.level().getEntity(packet.catId);
            if (!(entity instanceof Cat cat) || !CatProfileData.canOpen(player, cat)) return;
            CatProfileData.setName(cat, packet.name);
        });
        context.setPacketHandled(true);
    }
}
