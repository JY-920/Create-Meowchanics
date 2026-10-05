package cn.laowu.mod.client;

import cn.laowu.mod.compat.create.CreateIntegration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Core menus use vanilla inventory art when Create is absent. */
public final class CatClientTextures {
    public static void renderPlayerInventory(GuiGraphics graphics, int x, int y) {
        if (CreateIntegration.isLoaded()) {
            cn.laowu.mod.compat.create.CreateClientEvents.renderPlayerInventory(graphics, x, y);
        } else {
            graphics.fill(x, y, x + 176, y + 4, 0xffc6c6c6);
            graphics.blit(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png"), x, y + 4, 0, 126, 176, 96, 256, 256);
        }
    }
    private CatClientTextures() {}
}
