package cn.laowu.mod.network;

import cn.laowu.mod.CatProfileData;
import cn.laowu.mod.CatProfileMenu;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Commits the profile screen's name field when the client closes it. */
public record SetCatProfileNamePacket(int catId, String name) implements CustomPacketPayload {
    public static final Type<SetCatProfileNamePacket> TYPE =
            new Type<>(LaoWuMod.id("set_cat_profile_name"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetCatProfileNamePacket> STREAM_CODEC =
            StreamCodec.of(SetCatProfileNamePacket::encode, SetCatProfileNamePacket::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, SetCatProfileNamePacket packet) {
        buffer.writeVarInt(packet.catId);
        buffer.writeUtf(packet.name, CatProfileData.MAX_NAME_LENGTH);
    }

    private static SetCatProfileNamePacket decode(RegistryFriendlyByteBuf buffer) {
        return new SetCatProfileNamePacket(buffer.readVarInt(),
                buffer.readUtf(CatProfileData.MAX_NAME_LENGTH));
    }

    public static void handle(SetCatProfileNamePacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!(player.containerMenu instanceof CatProfileMenu menu)
                || menu.getCatId() != packet.catId) return;
        Entity entity = player.level().getEntity(packet.catId);
        if (!(entity instanceof Cat cat) || !CatProfileData.canOpen(player, cat)) return;
        CatProfileData.setName(cat, packet.name);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
