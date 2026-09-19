package cn.laowu.mod.network;

import cn.laowu.mod.CatAttributeEditorMenu;
import cn.laowu.mod.CatTraitEditorMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Vanilla container buttons are signed bytes; editor actions require full integers. */
public record CatEditorActionPacket(int containerId, int actionId) {
    public static void encode(CatEditorActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeVarInt(packet.actionId);
    }

    public static CatEditorActionPacket decode(FriendlyByteBuf buffer) {
        return new CatEditorActionPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(CatEditorActionPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> packet.apply(context.getSender()));
        context.setPacketHandled(true);
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
}
