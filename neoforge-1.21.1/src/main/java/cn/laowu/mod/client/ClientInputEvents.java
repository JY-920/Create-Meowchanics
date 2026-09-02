package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = LaoWuMod.MOD_ID, value = Dist.CLIENT)
public final class ClientInputEvents {
    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (ClientModEvents.OPEN_HELD_ITEM_TRANSFORM.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                ItemStack held = minecraft.player.getMainHandItem();
                if (held.isEmpty()) {
                    held = minecraft.player.getOffhandItem();
                }
                if (held.isEmpty()) {
                    minecraft.player.displayClientMessage(
                            Component.translatable("message.laowu.held_item_transform.empty"), true);
                } else {
                    minecraft.setScreen(new HeldItemTransformScreen(held.copy()));
                }
            }
        }
        while (ClientModEvents.CAT_ARMOR_POUNCE.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                ModNetwork.requestCatArmorPounce();
            }
        }
        while (ClientModEvents.CAT_TOOL_EMPOWER.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                ModNetwork.requestCatToolEmpowerToggle();
            }
        }
        while (ClientModEvents.HISSING_VOLUME.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                minecraft.setScreen(new HissingVolumeScreen());
            }
        }
    }

    private ClientInputEvents() {}
}
