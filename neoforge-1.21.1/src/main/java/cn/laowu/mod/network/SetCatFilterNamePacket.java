package cn.laowu.mod.network;

import cn.laowu.mod.CatFilterMenu;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.CatFilterRules;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Updates the staged name condition without mutating the held filter mid-screen. */
public record SetCatFilterNamePacket(int containerId, String name)
        implements CustomPacketPayload {
    public static final Type<SetCatFilterNamePacket> TYPE =
            new Type<>(LaoWuMod.id("set_cat_filter_name"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetCatFilterNamePacket> STREAM_CODEC =
            StreamCodec.of(SetCatFilterNamePacket::encode, SetCatFilterNamePacket::decode);

    private static void encode(RegistryFriendlyByteBuf buffer,
                               SetCatFilterNamePacket packet) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeUtf(packet.name, CatFilterRules.MAX_NAME_LENGTH);
    }

    private static SetCatFilterNamePacket decode(RegistryFriendlyByteBuf buffer) {
        return new SetCatFilterNamePacket(buffer.readVarInt(),
                buffer.readUtf(CatFilterRules.MAX_NAME_LENGTH));
    }

    public static void handle(SetCatFilterNamePacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof CatFilterMenu menu)
                || menu.containerId != packet.containerId) return;
        menu.setNameQuery(packet.name);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
