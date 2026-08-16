package cn.laowu.mod.network;

import cn.laowu.mod.CatChestData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Saves the editable frogport-style address shown in the cat container GUI. */
public record SetCatAddressPacket(int catId, String address) {
    public static void encode(SetCatAddressPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.catId);
        buffer.writeUtf(packet.address, CatChestData.MAX_ADDRESS_LENGTH);
    }

    public static SetCatAddressPacket decode(FriendlyByteBuf buffer) {
        return new SetCatAddressPacket(buffer.readVarInt(),
                buffer.readUtf(CatChestData.MAX_ADDRESS_LENGTH));
    }

    public static void handle(SetCatAddressPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null) return;
            Entity entity = player.level().getEntity(packet.catId);
            if (!(entity instanceof Cat cat) || !cat.isAlive() || !cat.isTame()
                    || !cat.isOwnedBy(player) || !CatChestData.hasChest(cat)
                    || player.distanceToSqr(cat) > 64.0D) return;
            CatChestData.setAddress(cat, packet.address);
        });
        context.setPacketHandled(true);
    }
}
