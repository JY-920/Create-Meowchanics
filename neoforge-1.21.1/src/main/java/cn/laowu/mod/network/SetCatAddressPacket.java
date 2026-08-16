package cn.laowu.mod.network;

import cn.laowu.mod.CatChestData;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Saves the editable frogport-style address shown in the cat container GUI. */
public record SetCatAddressPacket(int catId, String address) implements CustomPacketPayload {
    public static final Type<SetCatAddressPacket> TYPE = new Type<>(LaoWuMod.id("set_cat_address"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetCatAddressPacket> STREAM_CODEC =
            StreamCodec.of(SetCatAddressPacket::encode, SetCatAddressPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, SetCatAddressPacket packet) {
        buffer.writeVarInt(packet.catId);
        buffer.writeUtf(packet.address, CatChestData.MAX_ADDRESS_LENGTH);
    }

    private static SetCatAddressPacket decode(RegistryFriendlyByteBuf buffer) {
        return new SetCatAddressPacket(buffer.readVarInt(),
                buffer.readUtf(CatChestData.MAX_ADDRESS_LENGTH));
    }

    public static void handle(SetCatAddressPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        Entity entity = player.level().getEntity(packet.catId);
        if (!(entity instanceof Cat cat) || !cat.isAlive() || !cat.isTame()
                || !cat.isOwnedBy(player) || !CatChestData.hasChest(cat)
                || player.distanceToSqr(cat) > 64.0D) return;
        CatChestData.setAddress(cat, packet.address);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
