package cn.laowu.mod.client;

import cn.laowu.mod.CatFilterMenu;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.item.CatFilterRules;
import cn.laowu.mod.network.ModNetwork;
import com.simibubi.create.content.logistics.filter.AbstractFilterScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/** Create-style attribute, trait and identity editor for the cat filter. */
public final class CatFilterScreen extends AbstractFilterScreen<CatFilterMenu> {
    private static final ResourceLocation CREATE_FILTERS =
            ResourceLocation.fromNamespaceAndPath("create", "textures/gui/filters.png");
    private static final ResourceLocation ATTRIBUTE_ICONS =
            LaoWuMod.id("textures/gui/cat_attribute_icons.png");

    private static final int EXTENSION_HEIGHT = 40;
    private static final int SOURCE_FILTER_WIDTH = 241;
    private static final int FILTER_WIDTH = 273;
    private static final int HORIZONTAL_EXTENSION = FILTER_WIDTH - SOURCE_FILTER_WIDTH;
    private static final int FILTER_HEIGHT = 125;
    private static final int FOOTER_Y = 95;

    private static final int FIRST_ROW_Y = 17;
    private static final int ROW_HEIGHT = 12;
    private static final int COLUMN_WIDTH = 112;
    private static final int BAR_OFFSET_X = 36;
    private static final int BAR_WIDTH = 70;
    private static final int IDENTITY_PAGE = 2;
    private static final int IDENTITY_ROW_Y = 18;
    private static final int NAME_FIELD_Y = 40;

    private int page = CatFilterRules.CURRENT_PAGE;
    private Button currentButton;
    private Button potentialButton;
    private Button identityButton;
    private SelectionScrollInput growthSelector;
    private SelectionScrollInput ownershipSelector;
    private SelectionScrollInput careerSelector;
    private Label growthLabel;
    private Label ownershipLabel;
    private Label careerLabel;
    private EditBox nameBox;
    private SelectionScrollInput traitSelector;
    private Label traitSelectorLabel;
    private Button addTraitButton;
    private final List<Button> selectedTraitButtons = new ArrayList<>();
    private int draggingStat = -1;
    private boolean draggingMaximum;

    public CatFilterScreen(CatFilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, AllGuiTextures.ATTRIBUTE_FILTER);
    }

    @Override
    protected void init() {
        super.init();

        // AbstractFilterScreen lays out the 85px Attribute Filter first. Add
        // two real 20px trait rows, then recenter the taller complete window.
        topPos -= EXTENSION_HEIGHT / 2;
        leftPos -= HORIZONTAL_EXTENSION / 2;
        setWindowSize(FILTER_WIDTH, FILTER_HEIGHT + 4
                + AllGuiTextures.PLAYER_INVENTORY.getHeight());
        for (GuiEventListener listener : children()) {
            if (listener instanceof IconButton button) {
                button.setY(button.getY() + EXTENSION_HEIGHT / 2);
                button.setX(button.getX() + HORIZONTAL_EXTENSION / 2);
            }
        }

        currentButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.laowu.cat_filter.page.current"),
                button -> setPage(CatFilterRules.CURRENT_PAGE))
                .bounds(leftPos + 8, topPos + 101, 62, 18).build());
        potentialButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.laowu.cat_filter.page.limit"),
                button -> setPage(CatFilterRules.POTENTIAL_PAGE))
                .bounds(leftPos + 74, topPos + 101, 62, 18).build());
        identityButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.laowu.cat_filter.page.identity"),
                button -> setPage(IDENTITY_PAGE))
                .bounds(leftPos + 140, topPos + 101, 62, 18).build());

        initIdentityControls();

        traitSelectorLabel = new Label(leftPos + 13, topPos + 61,
                Component.empty()).colored(0xF3E8D4).withShadow();
        traitSelector = new LeftTooltipSelectionScrollInput(leftPos + 8, topPos + 56,
                216, 18);
        traitSelector.forOptions(traitOptions());
        traitSelector.setState(0);
        traitSelector.titled(Component.translatable(
                "gui.laowu.cat_filter.trait.selector"));
        traitSelector.writingTo(traitSelectorLabel);
        traitSelector.calling(selection -> refreshTraitButtons());
        addTraitButton = addRenderableWidget(Button.builder(Component.literal("+"),
                        button -> addSelectedTrait())
                .bounds(leftPos + 228, topPos + 56, 32, 18).build());
        addRenderableWidget(traitSelector);
        addRenderableWidget(traitSelectorLabel);

        selectedTraitButtons.clear();
        for (int slot = 0; slot < CatFilterMenu.MAX_REQUIRED_TRAITS; slot++) {
            int selectedSlot = slot;
            selectedTraitButtons.add(addRenderableWidget(Button.builder(
                            Component.literal("—"),
                            button -> removeSelectedTrait(selectedSlot))
                    .bounds(leftPos + 8 + slot * 63, topPos + 76,
                            60, 18).build()));
        }
        updatePageButtons();
        refreshTraitButtons();
    }

    private void initIdentityControls() {
        growthLabel = new Label(leftPos + 12, topPos + IDENTITY_ROW_Y + 5,
                Component.empty()).colored(0x3F3027);
        growthSelector = new SelectionScrollInput(leftPos + 8,
                topPos + IDENTITY_ROW_Y, 72, 18);
        configureIdentitySelector(growthSelector, growthLabel, growthOptions(),
                menu.growthSelection(), CatFilterMenu.GROWTH_FIELD);

        ownershipLabel = new Label(leftPos + 88, topPos + IDENTITY_ROW_Y + 5,
                Component.empty()).colored(0x3F3027);
        ownershipSelector = new SelectionScrollInput(leftPos + 84,
                topPos + IDENTITY_ROW_Y, 72, 18);
        configureIdentitySelector(ownershipSelector, ownershipLabel, ownershipOptions(),
                menu.ownershipSelection(), CatFilterMenu.OWNERSHIP_FIELD);

        careerLabel = new Label(leftPos + 164, topPos + IDENTITY_ROW_Y + 5,
                Component.empty()).colored(0x3F3027);
        careerSelector = new SelectionScrollInput(leftPos + 160,
                topPos + IDENTITY_ROW_Y, 100, 18);
        configureIdentitySelector(careerSelector, careerLabel, careerOptions(),
                menu.careerSelection(), CatFilterMenu.CAREER_FIELD);

        nameBox = new EditBox(font, leftPos + 43, topPos + NAME_FIELD_Y + 2,
                213, 12, Component.translatable("gui.laowu.cat_filter.name"));
        nameBox.setBordered(false);
        nameBox.setTextColor(0x3F3027);
        nameBox.setTextColorUneditable(0x6A5A4B);
        nameBox.setMaxLength(CatFilterRules.MAX_NAME_LENGTH);
        nameBox.setHint(Component.translatable("gui.laowu.cat_filter.name.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setValue(menu.nameQuery());
        nameBox.setResponder(value -> {
            menu.setNameQuery(value);
            ModNetwork.setCatFilterName(menu.containerId, value);
        });
        addRenderableWidget(nameBox);
    }

    private void configureIdentitySelector(SelectionScrollInput selector, Label label,
                                           List<Component> options, int state,
                                           int field) {
        selector.forOptions(options);
        selector.setState(Mth.clamp(state, 0, options.size() - 1));
        selector.writingTo(label);
        selector.calling(selection -> sendIdentity(field, selection));
        addRenderableWidget(selector);
        addRenderableWidget(label);
    }

    private static List<Component> traitOptions() {
        List<Component> options = new ArrayList<>(CatTrait.values().length + 1);
        options.add(Component.translatable("gui.laowu.cat_filter.trait.choose"));
        for (CatTrait trait : CatTrait.values()) {
            options.add(Component.translatable(
                    "gui.laowu.cat_filter.trait.selected", trait.title()));
        }
        return options;
    }

    private static List<Component> growthOptions() {
        List<Component> options = new ArrayList<>();
        for (CatFilterRules.GrowthFilter value : CatFilterRules.GrowthFilter.values()) {
            options.add(Component.translatable("gui.laowu.cat_filter.selector.growth",
                    Component.translatable("gui.laowu.cat_filter.growth." + value.id())));
        }
        return options;
    }

    private static List<Component> ownershipOptions() {
        List<Component> options = new ArrayList<>();
        for (CatFilterRules.OwnershipFilter value
                : CatFilterRules.OwnershipFilter.values()) {
            options.add(Component.translatable("gui.laowu.cat_filter.selector.ownership",
                    Component.translatable("gui.laowu.cat_filter.ownership." + value.id())));
        }
        return options;
    }

    private static List<Component> careerOptions() {
        List<Component> options = new ArrayList<>();
        for (CatFilterRules.CareerFilter value : CatFilterRules.CareerFilter.values()) {
            options.add(Component.translatable("gui.laowu.cat_filter.selector.career",
                    Component.translatable("gui.laowu.cat_filter.career." + value.id())));
        }
        return options;
    }

    private void setPage(int selectedPage) {
        page = Mth.clamp(selectedPage, CatFilterRules.CURRENT_PAGE, IDENTITY_PAGE);
        draggingStat = -1;
        if (page != IDENTITY_PAGE && nameBox != null) nameBox.setFocused(false);
        updatePageButtons();
    }

    private void updatePageButtons() {
        if (currentButton != null) currentButton.active = page != CatFilterRules.CURRENT_PAGE;
        if (potentialButton != null) potentialButton.active = page != CatFilterRules.POTENTIAL_PAGE;
        if (identityButton != null) identityButton.active = page != IDENTITY_PAGE;
        boolean identityVisible = page == IDENTITY_PAGE;
        setVisible(growthSelector, identityVisible);
        setVisible(ownershipSelector, identityVisible);
        setVisible(careerSelector, identityVisible);
        setVisible(growthLabel, identityVisible);
        setVisible(ownershipLabel, identityVisible);
        setVisible(careerLabel, identityVisible);
        if (nameBox != null) {
            nameBox.visible = identityVisible;
            nameBox.active = identityVisible;
        }
    }

    private static void setVisible(net.minecraft.client.gui.components.AbstractWidget widget,
                                   boolean visible) {
        if (widget == null) return;
        widget.visible = visible;
        widget.active = visible;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshTraitButtons();
        syncIdentityControls();
    }

    private void syncIdentityControls() {
        syncSelector(growthSelector, menu.growthSelection());
        syncSelector(ownershipSelector, menu.ownershipSelection());
        syncSelector(careerSelector, menu.careerSelection());
        if (nameBox != null && !nameBox.isFocused()
                && !nameBox.getValue().equals(menu.nameQuery())) {
            nameBox.setValue(menu.nameQuery());
        }
    }

    private static void syncSelector(SelectionScrollInput selector, int state) {
        if (selector != null && selector.getState() != state) selector.setState(state);
    }

    private void refreshTraitButtons() {
        if (traitSelector == null || addTraitButton == null
                || selectedTraitButtons.size() != CatFilterMenu.MAX_REQUIRED_TRAITS) return;
        CatTrait candidate = traitForSelection(traitSelector.getState());
        List<CatTrait> selected = menu.selectedTraits();
        addTraitButton.active = candidate != null
                && selected.size() < CatFilterMenu.MAX_REQUIRED_TRAITS
                && !selected.contains(candidate);
        for (int slot = 0; slot < selectedTraitButtons.size(); slot++) {
            CatTrait trait = menu.selectedTrait(slot);
            Button button = selectedTraitButtons.get(slot);
            button.setMessage(trait == null ? Component.literal("—") : trait.title());
            button.active = trait != null;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (traitSelector instanceof LeftTooltipSelectionScrollInput selector) {
            selector.renderLeftTooltip(graphics, mouseX, mouseY);
        }
        renderTraitPreview(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int inventoryX = getLeftOfCentered(AllGuiTextures.PLAYER_INVENTORY.getWidth());
        renderPlayerInventory(graphics, inventoryX, topPos + FILTER_HEIGHT + 4);
        renderExtendedFilterBackground(graphics);

        graphics.drawString(font, title,
                leftPos + (FILTER_WIDTH - 8) / 2 - font.width(title) / 2,
                topPos + 4, getTitleColor(), false);
        GuiGameElement.of(menu.contentHolder)
                .scale(4.0D)
                .at(leftPos + FILTER_WIDTH + 8.0F,
                        topPos + FILTER_HEIGHT - 52.0F, -200.0F)
                .render(graphics);

        if (page == IDENTITY_PAGE) {
            renderIdentityFields(graphics);
        } else {
            for (CatStat stat : CatStat.values()) renderRange(graphics, stat);
        }
    }

    private void renderIdentityFields(GuiGraphics graphics) {
        int x = leftPos + 40;
        int y = topPos + NAME_FIELD_Y;
        graphics.drawString(font, Component.translatable("gui.laowu.cat_filter.name"),
                leftPos + 8, y + 4, 0x3F3027, false);
        graphics.fill(x, y, leftPos + 260, y + 16, 0xFF5A4638);
        graphics.fill(x + 1, y + 1, leftPos + 259, y + 15, 0xFFE6D2AE);
    }

    /**
     * Compose a 125px-tall Attribute Filter from Create's own authored slices:
     * the original yellow header, a checkerboard body extended by two rows,
     * and the original footer moved down intact.
     */
    private void renderExtendedFilterBackground(GuiGraphics graphics) {
        blitExtendedRow(graphics, topPos, 99, 15);
        blitExtendedRow(graphics, topPos + 15, 114, 1);
        for (int row = 0; row < 78; row++) {
            blitExtendedRow(graphics, topPos + 16 + row, 115 + (row & 1), 1);
        }
        blitExtendedRow(graphics, topPos + 94, 153, 1);
        blitExtendedRow(graphics, topPos + FOOTER_Y, 154, 30);

        // Remove the four pre-drawn mode cells but retain the authored border,
        // highlight, separator, reset button area and confirmation arrow.
        graphics.fill(leftPos + 3, topPos + FOOTER_Y + 2,
                leftPos + FILTER_WIDTH - 39, topPos + FOOTER_Y + 28, 0xFFC6C6C6);
    }

    /** Inserts a tiled centre strip while preserving both authored side edges. */
    private void blitExtendedRow(GuiGraphics graphics, int destinationY,
                                 int sourceY, int height) {
        int split = SOURCE_FILTER_WIDTH / 2;
        graphics.blit(CREATE_FILTERS, leftPos, destinationY,
                0, sourceY, split, height, 256, 256);
        for (int offset = 0; offset < HORIZONTAL_EXTENSION; offset++) {
            graphics.blit(CREATE_FILTERS, leftPos + split + offset, destinationY,
                    split + (offset & 1), sourceY, 1, height, 256, 256);
        }
        graphics.blit(CREATE_FILTERS, leftPos + split + HORIZONTAL_EXTENSION,
                destinationY, split, sourceY,
                SOURCE_FILTER_WIDTH - split, height, 256, 256);
    }

    @Override
    public List<Rect2i> getExtraAreas() {
        return List.of(new Rect2i(leftPos + FILTER_WIDTH,
                topPos + FILTER_HEIGHT - 40, 80, 48));
    }

    private void renderRange(GuiGraphics graphics, CatStat stat) {
        int column = stat.ordinal() / 3;
        int row = stat.ordinal() % 3;
        int x = leftPos + 8 + column * COLUMN_WIDTH;
        int y = topPos + FIRST_ROW_Y + row * ROW_HEIGHT;
        int minimum = menu.min(page, stat);
        int maximum = menu.max(page, stat);

        graphics.blit(ATTRIBUTE_ICONS, x, y,
                stat.ordinal() * 8, 0, 8, 8, 48, 8);
        graphics.drawString(font, Component.translatable(
                        "gui.laowu.cat_filter.stat." + stat.serializedName()),
                x + 10, y, 0x3F3027, false);

        int barX = x + BAR_OFFSET_X;
        String value = minimum + "–" + maximum;
        int valueX = barX + (BAR_WIDTH - font.width(value)) / 2;
        graphics.drawString(font, value, valueX, y + 1, 0x3F3027, false);

        int trackY = y + 9;
        graphics.fill(barX, trackY, barX + BAR_WIDTH + 1, trackY + 2, 0xFF4C4037);
        int minX = sliderX(barX, minimum);
        int maxX = sliderX(barX, maximum);
        graphics.fill(minX, trackY, maxX + 1, trackY + 2, 0xFFE3B866);
        graphics.fill(minX - 1, trackY - 1, minX + 2, trackY + 3, 0xFF6A513D);
        graphics.fill(maxX - 1, trackY - 1, maxX + 2, trackY + 3, 0xFF6A513D);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        RangeHit hit = page == IDENTITY_PAGE ? null : findRange(mouseX, mouseY);
        if (hit != null) {
            int value = valueAt(hit.barX(), mouseX);
            int minimum = menu.min(page, hit.stat());
            int maximum = menu.max(page, hit.stat());
            draggingMaximum = value > maximum || value > minimum
                    && Math.abs(value - maximum) < Math.abs(value - minimum);
            draggingStat = hit.stat().ordinal();
            sendRange(hit.stat(), draggingMaximum, value);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (draggingStat >= 0) {
            CatStat stat = CatStat.values()[draggingStat];
            int column = stat.ordinal() / 3;
            int barX = leftPos + 8 + column * COLUMN_WIDTH + BAR_OFFSET_X;
            sendRange(stat, draggingMaximum, valueAt(barX, mouseX));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingStat = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private RangeHit findRange(double mouseX, double mouseY) {
        for (CatStat stat : CatStat.values()) {
            int column = stat.ordinal() / 3;
            int row = stat.ordinal() % 3;
            int x = leftPos + 8 + column * COLUMN_WIDTH;
            int y = topPos + FIRST_ROW_Y + row * ROW_HEIGHT;
            int barX = x + BAR_OFFSET_X;
            if (mouseX >= barX - 2 && mouseX <= barX + BAR_WIDTH + 2
                    && mouseY >= y + 6 && mouseY <= y + 12) {
                return new RangeHit(stat, barX);
            }
        }
        return null;
    }

    private int valueAt(int barX, double mouseX) {
        return Mth.clamp((int) Math.round((mouseX - barX) * 100.0D / BAR_WIDTH),
                CatFilterRules.MIN_VALUE, CatFilterRules.MAX_VALUE);
    }

    private int sliderX(int barX, int value) {
        return barX + Math.round(BAR_WIDTH * value / 100.0F);
    }

    private void sendRange(CatStat stat, boolean maximum, int value) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) return;
        int id = CatFilterMenu.rangeButton(page, stat, maximum, value);
        menu.clickMenuButton(minecraft.player, id);
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private void sendIdentity(int field, int selection) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) return;
        int id = CatFilterMenu.identityButton(field, selection);
        menu.clickMenuButton(minecraft.player, id);
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private void addSelectedTrait() {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) return;
        int selection = traitSelector == null ? 0 : traitSelector.getState();
        if (selection <= 0) return;
        int id = CatFilterMenu.addTraitButton(selection);
        menu.clickMenuButton(minecraft.player, id);
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        refreshTraitButtons();
    }

    private void removeSelectedTrait(int slot) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) return;
        if (menu.selectedTrait(slot) == null) return;
        int id = CatFilterMenu.removeTraitButton(slot);
        menu.clickMenuButton(minecraft.player, id);
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        refreshTraitButtons();
    }

    private void renderTraitPreview(GuiGraphics graphics, int mouseX, int mouseY) {
        CatTrait trait = null;
        boolean selectedEntry = false;
        if (mouseX >= leftPos + 8 && mouseX < leftPos + 192
                && mouseY >= topPos + 56 && mouseY < topPos + 74) {
            trait = traitForSelection(traitSelector == null ? 0 : traitSelector.getState());
        }
        if (trait == null) {
            for (int slot = 0; slot < selectedTraitButtons.size(); slot++) {
                if (selectedTraitButtons.get(slot).isMouseOver(mouseX, mouseY)) {
                    trait = menu.selectedTrait(slot);
                    selectedEntry = trait != null;
                    break;
                }
            }
        }
        if (trait == null) return;

        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(trait.title().copy().withStyle(
                trait.rarity().textFormatting()).getVisualOrderText());
        lines.add(Component.translatable("gui.laowu.cat_filter.trait.rarity",
                        Component.translatable("trait.laowu.rarity."
                                + trait.rarity().serializedName()))
                .withStyle(ChatFormatting.GRAY).getVisualOrderText());
        lines.add(Component.translatable("gui.laowu.cat_filter.trait.match_all_levels")
                .withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
        lines.add(FormattedCharSequence.EMPTY);
        if (trait.upgradable() && trait.maxLevel() > 1) {
            lines.addAll(font.split(Component.translatable(
                            "gui.laowu.cat_filter.trait.level_one_effect",
                            trait.description(1)).withStyle(ChatFormatting.WHITE), 220));
            lines.add(FormattedCharSequence.EMPTY);
            lines.addAll(font.split(Component.translatable(
                            "gui.laowu.cat_filter.trait.max_level_effect",
                            trait.description(trait.maxLevel()))
                    .withStyle(ChatFormatting.GREEN), 220));
        } else {
            lines.addAll(font.split(Component.translatable(
                            "gui.laowu.cat_filter.trait.fixed_effect",
                            trait.description(1)).withStyle(ChatFormatting.WHITE), 220));
        }
        if (selectedEntry) {
            lines.add(FormattedCharSequence.EMPTY);
            lines.add(Component.translatable("gui.laowu.cat_filter.trait.remove_hint")
                    .withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
        }
        graphics.renderTooltip(font, lines, mouseX, mouseY);
    }

    private static CatTrait traitForSelection(int selection) {
        return selection <= 0 || selection > CatTrait.values().length
                ? null : CatTrait.values()[selection - 1];
    }

    /**
     * Create normally anchors a selection list to the right of the pointer.
     * This screen also shows a trait-effect preview there, so keep the list on
     * the other side of the pointer while retaining Create's authored tooltip.
     */
    private static final class LeftTooltipSelectionScrollInput extends SelectionScrollInput {
        private List<Component> leftTooltip;

        private LeftTooltipSelectionScrollInput(int x, int y, int width, int height) {
            super(x, y, width, height);
        }

        @Override
        protected void updateTooltip() {
            super.updateTooltip();
            leftTooltip = List.copyOf(toolTip);
            toolTip.clear();
        }

        @Override
        public List<Component> getToolTip() {
            // Prevent both Create's widget renderer and any screen-level
            // tooltip pass from drawing the original right-hand copy.
            return List.of();
        }

        @Override
        protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY,
                                     float partialTick) {
            // Rendered exactly once by CatFilterScreen after all widgets.
        }

        private void renderLeftTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
            if (!isHoveredOrFocused() || leftTooltip == null || leftTooltip.isEmpty()) return;
            int tooltipWidth = leftTooltip.stream()
                    .mapToInt(Minecraft.getInstance().font::width)
                    .max()
                    .orElse(0);
            graphics.renderComponentTooltip(Minecraft.getInstance().font, leftTooltip,
                    mouseX - tooltipWidth - 24, mouseY);
        }
    }

    @Override
    protected boolean isButtonEnabled(IconButton button) {
        return true;
    }

    @Override
    protected List<IconButton> getTooltipButtons() {
        return List.of();
    }

    @Override
    protected List<MutableComponent> getTooltipDescriptions() {
        return List.of();
    }

    private record RangeHit(CatStat stat, int barX) {}
}
