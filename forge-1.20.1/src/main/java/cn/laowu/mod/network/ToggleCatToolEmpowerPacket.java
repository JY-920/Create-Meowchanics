package cn.laowu.mod.network;

import cn.laowu.mod.item.CatToolBehavior;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client intent only; the authoritative held stack is selected and changed server-side. */
public final class ToggleCatToolEmpowerPacket {
    public static void encode(ToggleCatToolEmpowerPacket packet, FriendlyByteBuf buffer) {
    }

    public static ToggleCatToolEmpowerPacket decode(FriendlyByteBuf buffer) {
        return new ToggleCatToolEmpowerPacket();
    }

    public static void handle(ToggleCatToolEmpowerPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null) return;

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
        });
        context.setPacketHandled(true);
    }
}
