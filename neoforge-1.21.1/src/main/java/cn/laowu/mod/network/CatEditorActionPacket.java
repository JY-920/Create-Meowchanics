package cn.laowu.mod.network;

import cn.laowu.mod.CatAttributeEditorMenu;
import cn.laowu.mod.CatTraitEditorMenu;
import cn.laowu.mod.LaoWuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Keep editor actions independent of vanilla's small container-button encoding. */
public record CatEditorActionPacket(int containerId, int actionId) implements CustomPacketPayload {
    public static final Type<CatEditorActionPacket> TYPE = new Type<>(LaoWuMod.id("cat_editor_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CatEditorActionPacket> STREAM_CODEC =
            StreamCodec.of(CatEditorActionPacket::encode, CatEditorActionPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, CatEditorActionPacket packet) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeVarInt(packet.actionId);
    }

    private static CatEditorActionPacket decode(RegistryFriendlyByteBuf buffer) {
        return new CatEditorActionPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(CatEditorActionPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) packet.apply(player);
    }

    /** Only the player's currently open, reachable editor may receive these actions. */
    public void apply(ServerPlayer player) {
        if (player == null || player.isSpectator()) return;
        var menu = player.containerMenu;
        if (menu.containerId != containerId
                || !(menu instanceof CatTraitEditorMenu || menu instanceof CatAttributeEditorMenu)
                || !menu.stillValid(player)) return;
        player.resetLastActionTime();
        menu.clickMenuButton(player, actionId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
