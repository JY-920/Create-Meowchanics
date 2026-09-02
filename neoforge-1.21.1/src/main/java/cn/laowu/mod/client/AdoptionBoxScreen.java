package cn.laowu.mod.client;

import cn.laowu.mod.AdoptionBoxMenu;
import cn.laowu.mod.LaoWuMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Pixel-aligned adoption GUI using the supplied two-box artwork. */
public final class AdoptionBoxScreen extends AbstractContainerScreen<AdoptionBoxMenu> {
    private static final ResourceLocation TEXTURE =
            LaoWuMod.id("textures/gui/adoption_box.png");
    private static final int SOURCE_X = 17;
    private static final int SOURCE_Y = 11;
    private static final int PANEL_X = 16;
    private static final int PANEL_WIDTH = 145;
    private static final int PANEL_HEIGHT = 78;
    private static final int TEXTURE_SIZE = 256;

    public AdoptionBoxScreen(AdoptionBoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = AdoptionBoxMenu.SCREEN_WIDTH;
        imageHeight = AdoptionBoxMenu.SCREEN_HEIGHT;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(TEXTURE, leftPos + PANEL_X, topPos,
                SOURCE_X, SOURCE_Y, PANEL_WIDTH, PANEL_HEIGHT,
                TEXTURE_SIZE, TEXTURE_SIZE);
        AllGuiTextures.PLAYER_INVENTORY.render(
                graphics, leftPos, topPos + AdoptionBoxMenu.PLAYER_PANEL_Y);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Both panels use their authored icon labels; no vanilla text overlays them.
    }
}
