package cn.laowu.mod.network;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.CatToolBehavior;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client intent only; the authoritative held stack is selected and changed server-side. */
public record ToggleCatToolEmpowerPacket() implements CustomPacketPayload {
    public static final Type<ToggleCatToolEmpowerPacket> TYPE =
            new Type<>(LaoWuMod.id("toggle_cat_tool_empower"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleCatToolEmpowerPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> { }, buffer -> new ToggleCatToolEmpowerPacket());

    public static void handle(ToggleCatToolEmpowerPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;

        InteractionHand hand = InteractionHand.MAIN_HAND;
        ItemStack stack = player.getMainHandItem();
        if (!CatToolBehavior.isCatHandTool(stack)) {
            hand = InteractionHand.OFF_HAND;
            stack = player.getOffhandItem();
        }
        if (!CatToolBehavior.isCatHandTool(stack)) {
            player.displayClientMessage(
                    Component.translatable("message.laowu.cat_tool_empower.no_tool"), true);
            return;
        }
        if (!CatToolBehavior.isEmpowermentMarked(stack) && CatToolBehavior.isExhausted(stack)) {
            player.displayClientMessage(
                    Component.translatable("message.laowu.cat_tool_empower.exhausted"), true);
            return;
        }

        boolean enabled = CatToolBehavior.toggleEmpowered(stack);
        player.setItemInHand(hand, stack);
        player.displayClientMessage(Component.translatable(enabled
                ? "message.laowu.cat_tool_empower.on"
                : "message.laowu.cat_tool_empower.off"), true);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
