package cn.laowu.mod.client;

import cn.laowu.mod.CatMaterialEditorMenu;
import cn.laowu.mod.genetics.CatGenome;
import cn.laowu.mod.genetics.CatGenomeData;
import cn.laowu.mod.genetics.CatRegion;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Inventory;

import java.util.Arrays;

/** Live 3D material editor with all semantic regions visible at once. */
public final class CatMaterialEditorScreen
        extends AbstractContainerScreen<CatMaterialEditorMenu> {
    private static final int PREVIEW_X = 10;
    private static final int PREVIEW_Y = 31;
    private static final int PREVIEW_WIDTH = 110;
    private static final int PREVIEW_HEIGHT = 208;
    private static final int FIRST_REGION_Y = 77;
    private static final int REGION_ROW_HEIGHT = 27;
    private static final int LEFT_COLUMN_X = 128;
    private static final int RIGHT_COLUMN_X = 362;
    private static final int REGIONS_PER_COLUMN = 6;

    private final int[] regionMaterialIndices = new int[CatRegion.values().length];
    private final Button[] regionValues = new Button[CatRegion.values().length];
    private int wholeMaterialIndex;
    private Button wholeValue;
    private Cat previewCat;

    public CatMaterialEditorScreen(CatMaterialEditorMenu menu, Inventory inventory,
                                   Component title) {
        super(menu, inventory, title);
        imageWidth = 600;
        imageHeight = 272;
    }

    @Override
    protected void init() {
        super.init();
        wholeMaterialIndex = menu.initialMaterialIndex();
        CatGenome initial = menu.initialGenome();
        for (CatRegion region : CatRegion.values()) {
            regionMaterialIndices[region.ordinal()] = menu.materialIndex(
                    initial.material(region));
        }

        addRenderableWidget(Button.builder(Component.literal("‹"), button -> cycleWhole(-1))
                .bounds(leftPos + 196, topPos + 40, 20, 20).build());
        wholeValue = addRenderableWidget(Button.builder(wholeMaterialName(),
                        button -> applyWholeMaterial())
                .bounds(leftPos + 220, topPos + 40, 332, 20).build());
        addRenderableWidget(Button.builder(Component.literal("›"), button -> cycleWhole(1))
                .bounds(leftPos + 556, topPos + 40, 20, 20).build());

        for (CatRegion region : CatRegion.values()) addRegionSelector(region);

        addRenderableWidget(Button.builder(Component.translatable(
                        "gui.laowu.cat_material_editor.apply"), button -> apply())
                .bounds(leftPos + 250, topPos + 239, 100, 20).build());
        createPreviewCat();
    }

    private void addRegionSelector(CatRegion region) {
        int ordinal = region.ordinal();
        int column = ordinal / REGIONS_PER_COLUMN;
        int row = ordinal % REGIONS_PER_COLUMN;
        int x = column == 0 ? LEFT_COLUMN_X : RIGHT_COLUMN_X;
        int y = FIRST_REGION_Y + row * REGION_ROW_HEIGHT;

        addRenderableWidget(Button.builder(Component.literal("‹"),
                        button -> cycleRegion(region, -1))
                .bounds(leftPos + x + 82, topPos + y, 20, 20).build());
        regionValues[ordinal] = addRenderableWidget(Button.builder(
                        regionMaterialName(region), button -> cycleRegion(region, 1))
                .bounds(leftPos + x + 106, topPos + y, 94, 20).build());
        addRenderableWidget(Button.builder(Component.literal("›"),
                        button -> cycleRegion(region, 1))
                .bounds(leftPos + x + 204, topPos + y, 20, 20).build());
    }

    private void cycleWhole(int amount) {
        wholeMaterialIndex = cycleIndex(wholeMaterialIndex, amount);
        applyWholeMaterial();
    }

    private void applyWholeMaterial() {
        Arrays.fill(regionMaterialIndices, wholeMaterialIndex);
        wholeValue.setMessage(wholeMaterialName());
        for (CatRegion region : CatRegion.values()) {
            regionValues[region.ordinal()].setMessage(regionMaterialName(region));
        }
        stageSelection(0, wholeMaterialIndex);
        refreshPreview();
    }

    private void cycleRegion(CatRegion region, int amount) {
        int ordinal = region.ordinal();
        regionMaterialIndices[ordinal] = cycleIndex(
                regionMaterialIndices[ordinal], amount);
        regionValues[ordinal].setMessage(regionMaterialName(region));
        stageSelection(ordinal + 1, regionMaterialIndices[ordinal]);
        refreshPreview();
    }

    private int cycleIndex(int current, int amount) {
        return Math.floorMod(current + amount, menu.materialCount());
    }

    private Component wholeMaterialName() {
        return menu.materialName(wholeMaterialIndex);
    }

    private Component regionMaterialName(CatRegion region) {
        return menu.materialName(regionMaterialIndices[region.ordinal()]);
    }

    private void stageSelection(int regionIndex, int materialIndex) {
        if (minecraft == null || minecraft.gameMode == null) return;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                menu.encodeSelection(regionIndex, materialIndex));
    }

    private void apply() {
        if (minecraft == null || minecraft.gameMode == null) return;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                CatMaterialEditorMenu.COMMIT_BUTTON_ID);
    }

    private void createPreviewCat() {
        if (minecraft == null || minecraft.level == null) return;
        previewCat = EntityType.CAT.create(minecraft.level);
        if (previewCat == null) return;
        previewCat.setAge(0);
        previewCat.setOrderedToSit(false);
        refreshPreview();
    }

    private CatGenome selectedGenome() {
        CatGenome genome = menu.initialGenome();
        for (CatRegion region : CatRegion.values()) {
            genome = genome.withMaterial(region,
                    menu.material(regionMaterialIndices[region.ordinal()]));
        }
        return genome;
    }

    private void refreshPreview() {
        if (previewCat == null) return;
        CatGenome genome = selectedGenome();
        CatGenomeData.set(previewCat, genome);
        BuiltInRegistries.CAT_VARIANT.getHolder(
                genome.material(CatRegion.BODY_FRONT))
                .ifPresent(previewCat::setVariant);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF202020);
        graphics.fill(leftPos + 2, topPos + 2,
                leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFFC6C6C6);
        drawInsetPanel(graphics, PREVIEW_X, PREVIEW_Y, PREVIEW_WIDTH, PREVIEW_HEIGHT);
        drawInsetPanel(graphics, 128, 31, 462, 38);
        drawInsetPanel(graphics, 128, 72, 228, 164);
        drawInsetPanel(graphics, 362, 72, 228, 164);

        if (previewCat != null) {
            int x = leftPos + PREVIEW_X + PREVIEW_WIDTH / 2;
            int y = topPos + PREVIEW_Y + PREVIEW_HEIGHT - 18;
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                    x - PREVIEW_WIDTH / 2, y - PREVIEW_HEIGHT,
                    x + PREVIEW_WIDTH / 2, y, 52, partialTick,
                    (float) x - mouseX,
                    (float) y - 52.0F - mouseY, previewCat);
        }
    }

    private void drawInsetPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(leftPos + x, topPos + y,
                leftPos + x + width, topPos + y + height, 0xFF777777);
        graphics.fill(leftPos + x + 2, topPos + y + 2,
                leftPos + x + width - 2, topPos + y + height - 2, 0xFF929292);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 10, 0x404040);
        graphics.drawString(font, CatMaterialEditorMenu.regionName(0),
                134, 46, 0xFFFFFF, false);

        for (CatRegion region : CatRegion.values()) {
            int ordinal = region.ordinal();
            int column = ordinal / REGIONS_PER_COLUMN;
            int row = ordinal % REGIONS_PER_COLUMN;
            int x = column == 0 ? LEFT_COLUMN_X : RIGHT_COLUMN_X;
            int y = FIRST_REGION_Y + row * REGION_ROW_HEIGHT;
            graphics.drawString(font, CatMaterialEditorMenu.regionName(ordinal + 1),
                    x + 4, y + 6, 0xFFFFFF, false);
        }

        graphics.drawCenteredString(font, Component.translatable(
                        "gui.laowu.cat_material_editor.hint"),
                imageWidth / 2, imageHeight - 10, 0x505050);
    }
}
