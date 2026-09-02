package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.MaterialDebugWandItem;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.io.IOException;

/** Client-only PNG export paired with the server-side block material selection. */
@EventBusSubscriber(modid = LaoWuMod.MOD_ID, value = Dist.CLIENT)
public final class MaterialDebugClientEvents {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getItemStack().getItem() instanceof MaterialDebugWandItem)
                || !event.getLevel().isClientSide) return;
        try {
            var output = CatGenomeTextureManager.exportBlockMaterial(
                    event.getLevel().getBlockState(event.getPos()));
            event.getEntity().displayClientMessage(Component.translatable(
                    "message.laowu.material_wand.exported", output.toAbsolutePath()), false);
        } catch (IOException exception) {
            event.getEntity().displayClientMessage(Component.translatable(
                    "message.laowu.material_wand.export_failed", exception.getMessage()), false);
        }
    }

    private MaterialDebugClientEvents() {}
}
