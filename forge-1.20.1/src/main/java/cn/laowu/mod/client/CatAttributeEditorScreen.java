package cn.laowu.mod.client;

import cn.laowu.mod.CatAttributeEditorMenu;
import cn.laowu.mod.genetics.CatStat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compact vanilla-widget editor opened by the attribute debug wand. */
public final class CatAttributeEditorScreen extends AbstractContainerScreen<CatAttributeEditorMenu> {
    private static final int CURRENT_X = 118;
    private static final int POTENTIAL_X = 238;
    private static final int FIRST_ROW_Y = 46;
    private static final int ROW_HEIGHT = 25;

    public CatAttributeEditorScreen(CatAttributeEditorMenu menu, Inventory inventory,
                                    Component title) {
        super(menu, inventory, title);
        imageWidth = 360;
        imageHeight = 225;
    }

    @Override
    protected void init() {
        super.init();
        for (CatStat stat : CatStat.values()) {
            int y = topPos + FIRST_ROW_Y + stat.ordinal() * ROW_HEIGHT;
            addEditorButtons(stat, CURRENT_X, y, 0, 1);
            addEditorButtons(stat, POTENTIAL_X, y, 2, 3);
        }
    }

    private void addEditorButtons(CatStat stat, int localX, int y,
                                  int decreaseBand, int increaseBand) {
        addRenderableWidget(Button.builder(Component.literal("−"), button ->
                        sendButton(decreaseBand * 10 + stat.ordinal()))
                .bounds(leftPos + localX, y, 18, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button ->
                        sendButton(increaseBand * 10 + stat.ordinal()))
                .bounds(leftPos + localX + 74, y, 18, 18).build());
    }

    private void sendButton(int id) {
        if (minecraft == null || minecraft.gameMode == null) return;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                hasShiftDown() ? id + 100 : id);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth,
                topPos + imageHeight, 0xFF202020);
        graphics.fill(leftPos + 2, topPos + 2,
                leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFFC6C6C6);
        graphics.fill(leftPos + 6, topPos + 38,
                leftPos + imageWidth - 6, topPos + 199, 0xFF777777);
        graphics.fill(leftPos + 8, topPos + 40,
                leftPos + imageWidth - 8, topPos + 197, 0xFF8B8B8B);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 9, 0x404040);
        graphics.drawCenteredString(font,
                Component.translatable("gui.laowu.cat_stats.current"),
                CURRENT_X + 46, 27, 0x404040);
        graphics.drawCenteredString(font,
                Component.translatable("gui.laowu.cat_stats.limit"),
                POTENTIAL_X + 46, 27, 0x404040);

        for (CatStat stat : CatStat.values()) {
            int y = FIRST_ROW_Y + stat.ordinal() * ROW_HEIGHT;
            graphics.drawString(font,
                    Component.translatable("attribute.laowu.cat." + stat.serializedName()),
                    12, y + 5, 0xFFFFFF, false);
            graphics.drawCenteredString(font, Integer.toString(menu.current(stat)),
                    CURRENT_X + 46, y + 5, 0xFFFFFF);
            graphics.drawCenteredString(font, Integer.toString(menu.potential(stat)),
                    POTENTIAL_X + 46, y + 5, 0xFFFFFF);
        }
        graphics.drawCenteredString(font,
                Component.translatable("screen.laowu.cat_attribute_editor.shift_hint"),
                imageWidth / 2, imageHeight - 17, 0x404040);
    }
}
