package cn.laowu.mod.client;

import cn.laowu.mod.CatTraitEditorMenu;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitSlot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/** Scrollable development editor for selecting and managing every registered trait. */
public final class CatTraitEditorScreen extends AbstractContainerScreen<CatTraitEditorMenu> {
    private static final int INSTALLED_X = 10;
    private static final int INSTALLED_WIDTH = 190;
    private static final int CATALOG_X = 210;
    private static final int CATALOG_WIDTH = 200;
    private static final int FIRST_ROW_Y = 52;
    private static final int ROW_HEIGHT = 27;
    private static final int INSTALLED_ROWS = 4;
    private static final int CATALOG_ROWS = 6;
    private static final int SCROLLBAR_WIDTH = 5;

    private static final List<CatTrait> CATALOG = Arrays.stream(CatTrait.values())
            .sorted(Comparator.comparingInt(CatTraitEditorScreen::rarityOrder)
                    .thenComparingInt(Enum::ordinal))
            .toList();

    private final List<Button> installedMinus = new ArrayList<>();
    private final List<Button> installedPlus = new ArrayList<>();
    private final List<Button> catalogAdd = new ArrayList<>();
    private Button scrollUp;
    private Button scrollDown;
    private EditBox searchBox;
    private List<CatTrait> filteredCatalog = CATALOG;
    private int catalogOffset;
    private boolean draggingScrollbar;
    private double scrollbarGrabOffset;

    public CatTraitEditorScreen(CatTraitEditorMenu menu, Inventory inventory,
                                Component title) {
        super(menu, inventory, title);
        imageWidth = 420;
        imageHeight = 240;
    }

    @Override
    protected void init() {
        super.init();
        installedMinus.clear();
        installedPlus.clear();
        catalogAdd.clear();
        filteredCatalog = CATALOG;
        catalogOffset = 0;
        draggingScrollbar = false;

        searchBox = new EditBox(font, leftPos + CATALOG_X + 4, topPos + 27,
                137, 18, Component.translatable(
                "gui.laowu.cat_trait_editor.search"));
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.translatable(
                "gui.laowu.cat_trait_editor.search_hint"));
        searchBox.setResponder(this::updateSearch);
        addRenderableWidget(searchBox);

        for (int slot = 0; slot < INSTALLED_ROWS; slot++) {
            int row = slot;
            int y = topPos + FIRST_ROW_Y + slot * ROW_HEIGHT + 4;
            installedMinus.add(addRenderableWidget(Button.builder(Component.literal("−"), button -> {
                        CatTrait trait = installedTrait(row);
                        if (trait != null) sendButton(trait, false);
                    }).bounds(leftPos + INSTALLED_X + 151, y, 17, 18).build()));
            installedPlus.add(addRenderableWidget(Button.builder(Component.literal("+"), button -> {
                        CatTrait trait = installedTrait(row);
                        if (trait != null) sendButton(trait, true);
                    }).bounds(leftPos + INSTALLED_X + 171, y, 17, 18).build()));
        }

        for (int slot = 0; slot < CATALOG_ROWS; slot++) {
            int row = slot;
            int y = topPos + FIRST_ROW_Y + slot * ROW_HEIGHT + 4;
            catalogAdd.add(addRenderableWidget(Button.builder(Component.literal("+"), button -> {
                        CatTrait trait = catalogTrait(row);
                        if (trait != null) sendButton(trait, true);
                    }).bounds(leftPos + CATALOG_X + 174, y, 17, 18).build()));
        }

        scrollUp = addRenderableWidget(Button.builder(Component.literal("▲"), button -> scroll(-1))
                .bounds(leftPos + CATALOG_X + 145, topPos + 27, 17, 18).build());
        scrollDown = addRenderableWidget(Button.builder(Component.literal("▼"), button -> scroll(1))
                .bounds(leftPos + CATALOG_X + 165, topPos + 27, 17, 18).build());
        setInitialFocus(searchBox);
        refreshButtons();
    }

    private void updateSearch(String query) {
        String needle = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        filteredCatalog = needle.isEmpty() ? CATALOG : CATALOG.stream()
                .filter(trait -> matchesSearch(trait, needle)).toList();
        catalogOffset = 0;
        draggingScrollbar = false;
        refreshButtons();
    }

    private static boolean matchesSearch(CatTrait trait, String needle) {
        StringBuilder searchable = new StringBuilder(trait.title().getString());
        for (int level = 1; level <= trait.maxLevel(); level++) {
            searchable.append('\n').append(trait.description(level).getString());
        }
        return searchable.toString().toLowerCase(Locale.ROOT).contains(needle);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshButtons();
    }

    private void sendButton(CatTrait trait, boolean increase) {
        if (minecraft == null || minecraft.gameMode == null || trait == null) return;
        int id = (increase ? CatTrait.values().length : 0) + trait.ordinal();
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                hasShiftDown() ? id + 1_000 : id);
    }

    private void scroll(int amount) {
        int maximum = Math.max(0, filteredCatalog.size() - CATALOG_ROWS);
        catalogOffset = Mth.clamp(catalogOffset + amount, 0, maximum);
        refreshButtons();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int localX = (int) mouseX - leftPos;
        int localY = (int) mouseY - topPos;
        if (localX >= CATALOG_X && localX < CATALOG_X + CATALOG_WIDTH
                && localY >= FIRST_ROW_Y
                && localY < FIRST_ROW_Y + CATALOG_ROWS * ROW_HEIGHT) {
            scroll(delta > 0.0D ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverScrollbar(mouseX, mouseY)
                && maximumCatalogOffset() > 0) {
            int thumbY = scrollbarThumbY();
            int thumbHeight = scrollbarThumbHeight();
            if (mouseY >= thumbY && mouseY < thumbY + thumbHeight) {
                scrollbarGrabOffset = mouseY - thumbY;
            } else {
                scrollbarGrabOffset = thumbHeight / 2.0D;
                updateScrollbarDrag(mouseY);
            }
            draggingScrollbar = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (button == 0 && draggingScrollbar) {
            updateScrollbarDrag(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        int trackX = scrollbarTrackX();
        int trackY = scrollbarTrackY();
        return mouseX >= trackX - 2 && mouseX < trackX + SCROLLBAR_WIDTH + 2
                && mouseY >= trackY && mouseY < trackY + scrollbarTrackHeight();
    }

    private void updateScrollbarDrag(double mouseY) {
        int maximum = maximumCatalogOffset();
        int travel = scrollbarTrackHeight() - scrollbarThumbHeight();
        if (maximum <= 0 || travel <= 0) {
            catalogOffset = 0;
        } else {
            double thumbTop = mouseY - scrollbarGrabOffset - scrollbarTrackY();
            catalogOffset = Mth.clamp((int) Math.round(
                    Mth.clamp(thumbTop / travel, 0.0D, 1.0D) * maximum),
                    0, maximum);
        }
        refreshButtons();
    }

    private void refreshButtons() {
        if (installedMinus.size() != INSTALLED_ROWS
                || catalogAdd.size() != CATALOG_ROWS) return;

        for (int slot = 0; slot < INSTALLED_ROWS; slot++) {
            CatTrait trait = installedTrait(slot);
            boolean present = trait != null;
            Button minus = installedMinus.get(slot);
            Button plus = installedPlus.get(slot);
            minus.visible = present;
            minus.active = present;
            plus.visible = present;
            plus.active = present && trait.upgradable()
                    && menu.level(trait) < trait.maxLevel();
        }

        for (int slot = 0; slot < CATALOG_ROWS; slot++) {
            CatTrait trait = catalogTrait(slot);
            Button add = catalogAdd.get(slot);
            add.visible = trait != null;
            add.active = trait != null && menu.level(trait) <= 0 && canAdd(trait);
        }
        int maximum = Math.max(0, filteredCatalog.size() - CATALOG_ROWS);
        scrollUp.active = catalogOffset > 0;
        scrollDown.active = catalogOffset < maximum;
    }

    private boolean canAdd(CatTrait candidate) {
        List<CatTrait> installed = installedTraits();
        if (installed.size() >= 4) return false;
        EnumSet<CatTraitSlot> occupied = EnumSet.noneOf(CatTraitSlot.class);
        for (CatTrait trait : installed) occupied.addAll(trait.occupiedSlots());
        for (CatTraitSlot slot : candidate.occupiedSlots()) {
            if (occupied.contains(slot)) return false;
        }
        return true;
    }

    private List<CatTrait> installedTraits() {
        return CATALOG.stream().filter(trait -> menu.level(trait) > 0).toList();
    }

    private CatTrait installedTrait(int slot) {
        List<CatTrait> installed = installedTraits();
        return slot >= 0 && slot < installed.size() ? installed.get(slot) : null;
    }

    private CatTrait catalogTrait(int visibleSlot) {
        int index = catalogOffset + visibleSlot;
        return index >= 0 && index < filteredCatalog.size()
                ? filteredCatalog.get(index) : null;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderTraitDescription(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth,
                topPos + imageHeight, 0xFF202020);
        graphics.fill(leftPos + 2, topPos + 2,
                leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFFC6C6C6);
        drawPanel(graphics, INSTALLED_X, FIRST_ROW_Y,
                INSTALLED_WIDTH, INSTALLED_ROWS * ROW_HEIGHT);
        drawPanel(graphics, CATALOG_X, FIRST_ROW_Y,
                CATALOG_WIDTH, CATALOG_ROWS * ROW_HEIGHT);

        for (int row = 1; row < INSTALLED_ROWS; row++) {
            int y = topPos + FIRST_ROW_Y + row * ROW_HEIGHT;
            graphics.fill(leftPos + INSTALLED_X + 2, y,
                    leftPos + INSTALLED_X + INSTALLED_WIDTH - 2, y + 1, 0xFF686868);
        }
        for (int row = 1; row < CATALOG_ROWS; row++) {
            int y = topPos + FIRST_ROW_Y + row * ROW_HEIGHT;
            graphics.fill(leftPos + CATALOG_X + 2, y,
                    leftPos + CATALOG_X + CATALOG_WIDTH - 2, y + 1, 0xFF686868);
        }
        renderScrollbar(graphics);
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(leftPos + x, topPos + y,
                leftPos + x + width, topPos + y + height, 0xFF4A4A4A);
        graphics.fill(leftPos + x + 2, topPos + y + 2,
                leftPos + x + width - 2, topPos + y + height - 2, 0xFF858585);
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int trackX = scrollbarTrackX();
        int trackY = scrollbarTrackY();
        int trackHeight = scrollbarTrackHeight();
        graphics.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH,
                trackY + trackHeight, 0xFF4A4A4A);
        int thumbY = scrollbarThumbY();
        int thumbHeight = scrollbarThumbHeight();
        graphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH,
                thumbY + thumbHeight, draggingScrollbar ? 0xFFFFFFFF : 0xFFD8D8D8);
    }

    private int maximumCatalogOffset() {
        return Math.max(0, filteredCatalog.size() - CATALOG_ROWS);
    }

    private int scrollbarTrackX() {
        return leftPos + CATALOG_X + CATALOG_WIDTH - 8;
    }

    private int scrollbarTrackY() {
        return topPos + FIRST_ROW_Y + 3;
    }

    private int scrollbarTrackHeight() {
        return CATALOG_ROWS * ROW_HEIGHT - 6;
    }

    private int scrollbarThumbHeight() {
        int trackHeight = scrollbarTrackHeight();
        return filteredCatalog.isEmpty() ? trackHeight
                : Math.max(12, Math.min(trackHeight,
                trackHeight * CATALOG_ROWS / filteredCatalog.size()));
    }

    private int scrollbarThumbY() {
        int maximum = maximumCatalogOffset();
        int travel = scrollbarTrackHeight() - scrollbarThumbHeight();
        return scrollbarTrackY()
                + (maximum == 0 ? 0 : travel * catalogOffset / maximum);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 8, 0x404040);
        graphics.drawString(font,
                Component.translatable("gui.laowu.cat_trait_editor.installed",
                        installedTraits().size(), 4),
                INSTALLED_X + 4, 31, 0x404040, false);

        for (int slot = 0; slot < INSTALLED_ROWS; slot++) {
            CatTrait trait = installedTrait(slot);
            if (trait == null) continue;
            int y = FIRST_ROW_Y + slot * ROW_HEIGHT;
            drawTraitRow(graphics, trait, y, INSTALLED_X + 5, true);
        }
        for (int slot = 0; slot < CATALOG_ROWS; slot++) {
            CatTrait trait = catalogTrait(slot);
            if (trait == null) continue;
            int y = FIRST_ROW_Y + slot * ROW_HEIGHT;
            drawTraitRow(graphics, trait, y, CATALOG_X + 5, false);
        }

        graphics.drawCenteredString(font,
                Component.translatable("screen.laowu.cat_trait_editor.shift_hint"),
                imageWidth / 2, imageHeight - 14, 0x404040);
    }

    private void drawTraitRow(GuiGraphics graphics, CatTrait trait, int y,
                              int x, boolean installedPanel) {
        Integer colour = trait.rarity().textFormatting().getColor();
        graphics.drawString(font, trait.title(), x, y + 4,
                colour == null ? 0xFFFFFF : colour, false);
        Component secondary = installedPanel
                ? stateText(trait)
                : Component.translatable("gui.laowu.cat_trait_editor.catalog_state",
                Component.translatable("trait.laowu.rarity."
                        + trait.rarity().serializedName()), stateText(trait));
        graphics.drawString(font, secondary, x, y + 15, 0xFFF1F1F1, false);
    }

    private Component stateText(CatTrait trait) {
        int level = menu.level(trait);
        if (level <= 0) {
            return Component.translatable("gui.laowu.cat_trait_editor.absent");
        }
        if (!trait.upgradable()) {
            return Component.translatable("gui.laowu.cat_trait_editor.fixed");
        }
        return Component.translatable("gui.laowu.cat_trait_editor.level_value",
                romanLevel(level));
    }

    private void renderTraitDescription(GuiGraphics graphics, int mouseX, int mouseY) {
        CatTrait trait = hoveredTrait(mouseX - leftPos, mouseY - topPos);
        if (trait == null) return;
        int level = Math.max(1, menu.level(trait));
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(trait.title().copy().withStyle(
                trait.rarity().textFormatting()).getVisualOrderText());
        lines.add(Component.translatable("gui.laowu.cat_trait.rarity_and_level",
                        Component.translatable("trait.laowu.rarity."
                                + trait.rarity().serializedName()),
                        romanLevel(level), romanLevel(trait.maxLevel()))
                .withStyle(ChatFormatting.GRAY).getVisualOrderText());
        lines.add(FormattedCharSequence.EMPTY);
        lines.addAll(font.split(trait.description(level), 230));
        if (trait.upgradable() && level < trait.maxLevel()) {
            lines.add(FormattedCharSequence.EMPTY);
            lines.addAll(font.split(trait.nextLevelDescription(level).copy()
                    .withStyle(ChatFormatting.GREEN), 230));
        }
        graphics.renderTooltip(font, lines, mouseX, mouseY);
    }

    private CatTrait hoveredTrait(int localX, int localY) {
        if (localY < FIRST_ROW_Y) return null;
        if (localX >= INSTALLED_X && localX < INSTALLED_X + INSTALLED_WIDTH) {
            int row = (localY - FIRST_ROW_Y) / ROW_HEIGHT;
            if (row >= 0 && row < INSTALLED_ROWS) return installedTrait(row);
        }
        if (localX >= CATALOG_X && localX < CATALOG_X + CATALOG_WIDTH) {
            int row = (localY - FIRST_ROW_Y) / ROW_HEIGHT;
            if (row >= 0 && row < CATALOG_ROWS) return catalogTrait(row);
        }
        return null;
    }

    private static int rarityOrder(CatTrait trait) {
        return switch (trait.rarity()) {
            case COMMON -> 0;
            case GOOD -> 1;
            case EXCELLENT -> 2;
            case DEFECT -> 3;
        };
    }

    private static String romanLevel(int level) {
        return switch (Mth.clamp(level, 1, 7)) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            default -> "VII";
        };
    }
}
