package cn.laowu.mod.client;

import cn.laowu.mod.CatPackageMenu;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.CatChestData;
import cn.laowu.mod.network.ModNetwork;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** The Create frogport inventory layout with a cat-chest colour theme. */
public final class CatPackageScreen extends AbstractContainerScreen<CatPackageMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            LaoWuMod.MOD_ID, "textures/gui/cat_package_chest.png");
    private static final ResourceLocation EDIT_ICON = ResourceLocation.fromNamespaceAndPath(
            LaoWuMod.MOD_ID, "textures/gui/cat_edit_name.png");
    private static final int SOURCE_X = 62;
    private static final int SOURCE_Y = 24;
    private static final int PANEL_WIDTH = 228;
    private static final int PANEL_HEIGHT = 106;
    private static final int TEXTURE_SIZE = 512;
    private static final int PLAYER_PANEL_Y = 114;
    private static final int TITLE_COLOR = 0x6A513D;
    private EditBox addressBox;
    private boolean addressSent;

    public CatPackageScreen(CatPackageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PANEL_WIDTH;
        imageHeight = PLAYER_PANEL_Y + 108;
    }

    @Override
    protected void init() {
        super.init();
        addressBox = new EditBox(font, leftPos + 23, topPos + 12, PANEL_WIDTH - 20, 10,
                Component.translatable("gui.laowu.cat_address"));
        addressBox.setBordered(false);
        addressBox.setTextColor(TITLE_COLOR);
        addressBox.setTextColorUneditable(TITLE_COLOR);
        addressBox.setMaxLength(CatChestData.MAX_ADDRESS_LENGTH);
        addressBox.setValue(menu.getAddress());
        addressBox.setFocused(false);
        addressBox.setResponder(value -> addressBox.setX(nameBoxX(value.isEmpty()
                ? title.getString() : value)));
        addressBox.setX(nameBoxX(addressBox.getValue().isEmpty()
                ? title.getString() : addressBox.getValue()));
        addRenderableWidget(addressBox);
        addRenderableWidget(new AbstractButton(leftPos + 195, topPos + 82,
                18, 18, Component.translatable("gui.laowu.save_address")) {
            @Override
            public void onPress() {
                CatPackageScreen.this.onClose();
            }

            @Override
            protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                if (isHoveredOrFocused()) {
                    graphics.fill(getX() + 2, getY() + 2,
                            getX() + 16, getY() + 16, 0x30FFFFFF);
                }
            }

            @Override
            protected void updateWidgetNarration(NarrationElementOutput output) {
                defaultButtonNarrationText(output);
            }
        });
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
        graphics.blit(TEXTURE, leftPos, topPos,
                SOURCE_X, SOURCE_Y, PANEL_WIDTH, PANEL_HEIGHT,
                TEXTURE_SIZE, TEXTURE_SIZE);
        AllGuiTextures.PLAYER_INVENTORY.render(graphics, leftPos + 26, topPos + PLAYER_PANEL_Y);

        if (addressBox != null && !addressBox.isFocused()) {
            String displayName = addressBox.getValue().isEmpty()
                    ? title.getString() : addressBox.getValue();
            int nameX = nameBoxX(displayName);
            if (addressBox.getValue().isEmpty()) {
                graphics.drawString(font, displayName, nameX, topPos + 12,
                        TITLE_COLOR, false);
            }
            graphics.blit(EDIT_ICON, nameX + font.width(displayName) + 5, topPos + 10,
                    0, 0, 13, 13, 13, 13);
        }

    }

    private int nameBoxX(String value) {
        return leftPos + PANEL_WIDTH / 2
                - (Math.min(font.width(value), addressBox.getWidth()) + 10) / 2;
    }

    private boolean isOverNameEditor(double mouseX, double mouseY) {
        String displayName = addressBox.getValue().isEmpty()
                ? title.getString() : addressBox.getValue();
        int x = nameBoxX(displayName);
        int width = font.width(displayName) + 5 + 13;
        return mouseX >= x && mouseX < x + width
                && mouseY >= topPos + 9 && mouseY < topPos + 24;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && addressBox != null) {
            if (isOverNameEditor(mouseX, mouseY)) {
                addressBox.setFocused(true);
                setFocused(addressBox);
                addressBox.mouseClicked(mouseX, mouseY, button);
                return true;
            }
            if (addressBox.isFocused()) {
                addressBox.setFocused(false);
                setFocused(null);
                String displayName = addressBox.getValue().isEmpty()
                        ? title.getString() : addressBox.getValue();
                addressBox.setX(nameBoxX(displayName));
            }
            // Prevent the wide EditBox bounds from activating elsewhere in the header.
            if (mouseX >= leftPos && mouseX < leftPos + PANEL_WIDTH
                    && mouseY >= topPos && mouseY < topPos + 28) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (!addressSent && addressBox != null) {
            addressSent = true;
            ModNetwork.setCatAddress(menu.getCatId(), addressBox.getValue());
        }
        super.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == 257 || keyCode == 335) && addressBox != null && addressBox.isFocused()) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // The frogport-style header owns the only label on this screen.
    }
}
