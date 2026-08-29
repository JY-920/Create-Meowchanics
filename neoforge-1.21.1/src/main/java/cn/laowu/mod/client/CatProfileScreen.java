package cn.laowu.mod.client;

import cn.laowu.mod.CatProfileData;
import cn.laowu.mod.CatProfileMenu;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatTraitProfile;
import cn.laowu.mod.network.ModNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Inventory;

/** Pixel-authored cat profile, storage and naming screen. */
public final class CatProfileScreen extends AbstractContainerScreen<CatProfileMenu> {
    private static final ResourceLocation BACKGROUND =
            LaoWuMod.id("textures/gui/cat_profile_background.png");
    private static final ResourceLocation COMPONENTS =
            LaoWuMod.id("textures/gui/cat_profile_components.png");
    private static final int ATLAS_SIZE = 512;

    private static final int BACKGROUND_SOURCE_X = 11;
    private static final int BACKGROUND_SOURCE_Y = 14;
    private static final int BACKGROUND_WIDTH = 218;
    private static final int BACKGROUND_HEIGHT = 209;

    private static final int PREVIEW_SOURCE_X = 116;
    private static final int PREVIEW_SOURCE_Y = 231;
    private static final int PREVIEW_X = 12;
    private static final int PREVIEW_Y = 42;
    private static final int PREVIEW_WIDTH = 64;
    private static final int PREVIEW_HEIGHT = 53;

    private static final int ACCESSORY_SOURCE_X = 194;
    private static final int ACCESSORY_SOURCE_Y = 233;
    private static final int ACCESSORY_X = 210;
    private static final int ACCESSORY_Y = 40;
    private static final int ACCESSORY_WIDTH = 40;
    private static final int ACCESSORY_HEIGHT = 92;

    private static final int INVENTORY_SOURCE_X = 257;
    private static final int INVENTORY_SOURCE_Y = 233;
    private static final int CAT_INVENTORY_X = 210;
    private static final int CAT_INVENTORY_Y = 135;
    private static final int CAT_INVENTORY_WIDTH = 80;
    private static final int CAT_INVENTORY_HEIGHT = 72;

    private static final int STATS_X = 12;
    private static final int STATS_Y = 100;
    private static final int TRAITS_X = 105;
    private static final int TRAITS_Y = 52;
    private static final int TRAIT_SPACING = 29;

    private static final int NAME_PLATE_X = 30;
    private static final int NAME_PLATE_WIDTH = 140;
    private static final int NAME_TEXT_X = NAME_PLATE_X + 10;
    private static final int NAME_Y = 187;
    private static final int NAME_TEXT_WIDTH = NAME_PLATE_WIDTH - 10;
    private static final int NAME_HEIGHT = 9;
    private static final int NAME_PLATE_Y = 182;
    private static final int NAME_PLATE_HEIGHT = 19;
    private static final int NAME_COLOUR = 0x6A513D;

    private EditBox nameBox;
    private boolean nameSent;

    public CatProfileScreen(CatProfileMenu menu, Inventory inventory,
                            Component title) {
        super(menu, inventory, title);
        imageWidth = CatProfileMenu.MAIN_WIDTH;
        imageHeight = CatProfileMenu.SCREEN_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        // The authored 218-pixel main panel, rather than the right-hand slot
        // extension, is the screen's horizontal anchor.
        leftPos = (width - BACKGROUND_WIDTH) / 2;
        nameSent = false;
        nameBox = new EditBox(font, leftPos + NAME_TEXT_X, topPos + NAME_Y,
                NAME_TEXT_WIDTH, NAME_HEIGHT,
                Component.translatable("gui.laowu.cat_profile.name"));
        nameBox.setBordered(false);
        nameBox.setTextColor(NAME_COLOUR);
        nameBox.setTextColorUneditable(NAME_COLOUR);
        nameBox.setMaxLength(CatProfileData.MAX_NAME_LENGTH);
        nameBox.setValue(menu.getInitialName());
        addRenderableWidget(nameBox);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                       float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        Cat cat = currentCat();
        if (cat != null) {
            CatTraitCardRenderer.renderTooltip(graphics, font,
                    CatTraitData.read(cat).orElse(CatTraitProfile.EMPTY),
                    leftPos + TRAITS_X, topPos + TRAITS_Y,
                    TRAIT_SPACING, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick,
                            int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(BACKGROUND, leftPos, topPos,
                BACKGROUND_SOURCE_X, BACKGROUND_SOURCE_Y,
                BACKGROUND_WIDTH, BACKGROUND_HEIGHT,
                ATLAS_SIZE, ATLAS_SIZE);
        graphics.blit(COMPONENTS, leftPos + PREVIEW_X, topPos + PREVIEW_Y,
                PREVIEW_SOURCE_X, PREVIEW_SOURCE_Y,
                PREVIEW_WIDTH, PREVIEW_HEIGHT, ATLAS_SIZE, ATLAS_SIZE);
        graphics.blit(COMPONENTS, leftPos + ACCESSORY_X, topPos + ACCESSORY_Y,
                ACCESSORY_SOURCE_X, ACCESSORY_SOURCE_Y,
                ACCESSORY_WIDTH, ACCESSORY_HEIGHT, ATLAS_SIZE, ATLAS_SIZE);
        graphics.blit(COMPONENTS,
                leftPos + CAT_INVENTORY_X, topPos + CAT_INVENTORY_Y,
                INVENTORY_SOURCE_X, INVENTORY_SOURCE_Y,
                CAT_INVENTORY_WIDTH, CAT_INVENTORY_HEIGHT,
                ATLAS_SIZE, ATLAS_SIZE);
        AllGuiTextures.PLAYER_INVENTORY.render(graphics,
                leftPos + CatProfileMenu.PLAYER_PANEL_X,
                topPos + CatProfileMenu.PLAYER_PANEL_Y);

        Cat cat = currentCat();
        if (cat == null) return;
        renderCat(graphics, cat, mouseX, mouseY, partialTick);
        CatTraitProfile traits = CatTraitData.read(cat).orElse(CatTraitProfile.EMPTY);
        CatTraitCardRenderer.renderCards(graphics, font, traits,
                leftPos + TRAITS_X, topPos + TRAITS_Y, TRAIT_SPACING);
    }

    private void renderCat(GuiGraphics graphics, Cat cat,
                           int mouseX, int mouseY, float partialTick) {
        int x = leftPos + PREVIEW_X + PREVIEW_WIDTH / 2;
        int y = topPos + PREVIEW_Y + PREVIEW_HEIGHT - 13;
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                x - 32, y - 26, x + 32, y + 26, 30, partialTick,
                (float) x - mouseX, (float) y - 30.0F - mouseY, cat);
    }

    private Cat currentCat() {
        return minecraft == null || minecraft.player == null
                ? null : menu.getCat(minecraft.player);
    }

    @Override
    public void onClose() {
        sendName();
        super.onClose();
    }

    private void sendName() {
        if (nameSent || nameBox == null) return;
        nameSent = true;
        ModNetwork.setCatProfileName(menu.getCatId(), nameBox.getValue());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == 257 || keyCode == 335)
                && nameBox != null && nameBox.isFocused()) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && nameBox != null) {
            boolean insideNamePlate = mouseX >= leftPos + NAME_PLATE_X
                    && mouseX < leftPos + NAME_PLATE_X + NAME_PLATE_WIDTH
                    && mouseY >= topPos + NAME_PLATE_Y
                    && mouseY < topPos + NAME_PLATE_Y + NAME_PLATE_HEIGHT;
            if (insideNamePlate) {
                setFocused(nameBox);
                nameBox.setFocused(true);
                double textX = Math.max(nameBox.getX(),
                        Math.min(mouseX, nameBox.getX() + NAME_TEXT_WIDTH - 1));
                double textY = Math.max(nameBox.getY(),
                        Math.min(mouseY, nameBox.getY() + NAME_HEIGHT - 1));
                nameBox.mouseClicked(textX, textY, button);
                return true;
            }
            if (nameBox.isFocused()) {
                nameBox.setFocused(false);
                setFocused(null);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // All headings and frames are authored into the supplied pixel assets.
    }
}
