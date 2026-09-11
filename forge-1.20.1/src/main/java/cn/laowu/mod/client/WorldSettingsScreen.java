package cn.laowu.mod.client;

import cn.laowu.mod.CareerCatBehavior;
import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.CatSuitSetting;
import cn.laowu.mod.CatSuitSettings;
import cn.laowu.mod.GlobalConfig;
import cn.laowu.mod.ServerConfig;
import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitConfig;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** All tabs edit one draft; only the authoritative server can commit unlocked fields. */
public final class WorldSettingsScreen extends Screen {
    private final Screen parent;
    private CompoundTag draft;
    private final Map<CatStat, EditBox> fields = new EnumMap<>(CatStat.class);
    private final Map<CatOutfitType, Map<CatSuitSetting, EditBox>> careerFields = new EnumMap<>(CatOutfitType.class);
    private CatSuitSetting selectedSuitSetting = CatSuitSetting.DAMAGE;
    private boolean suitBonusesPage;
    private Button previousCareer, nextCareer, careerSelector, suitSection;
    private final List<Button> switches = new ArrayList<>();
    private CatStat selected = CatStat.HEALTH;
    private CatOutfitType selectedCareer = CatOutfitType.TERMINATOR;
    private boolean careerPage;
    private boolean traitPage;
    private static final int TRAIT_ROWS = 6;
    private final List<Button> traitButtons = new ArrayList<>();
    private List<CatTrait> filteredTraits = List.of();
    private EditBox traitSearch;
    private String traitQuery = "";
    private int traitOffset;
    private Button traitsTab, previousTraits, nextTraits, traitHelp;
    private boolean editable;
    private Button save, attributesTab, careersTab, resetCareers;
    private int left, top, panelWidth;

    public WorldSettingsScreen(Screen parent) {
        super(Component.translatable("screen.laowu.world.title"));
        this.parent = parent;
        draft = ClientWorldSettings.values();
        ModNetwork.requestWorldSettings(false, new CompoundTag());
    }

    @Override protected void init() {
        if (draft == null) draft = ServerConfig.snapshot();
        editable = draft.getBoolean("can_edit");
        fields.clear();
        careerFields.clear();
        switches.clear();
        traitButtons.clear();
        panelWidth = Math.min(460, width - 20);
        left = (width - panelWidth) / 2;
        top = Math.max(4, (height - 234) / 2);
        int tabWidth = (panelWidth - 12) / 3;
        attributesTab = addRenderableWidget(Button.builder(Component.translatable("screen.laowu.world.tab_attributes"),
                b -> changePage(0)).bounds(left, top + 30, tabWidth, 18).build());
        careersTab = addRenderableWidget(Button.builder(Component.translatable("screen.laowu.world.tab_careers"),
                b -> changePage(1)).bounds(left + tabWidth + 6, top + 30, tabWidth, 18).build());
        traitsTab = addRenderableWidget(Button.builder(Component.translatable("screen.laowu.world.tab_traits"),
                b -> changePage(2)).bounds(left + 2 * (tabWidth + 6), top + 30,
                        panelWidth - 2 * (tabWidth + 6), 18).build());
        for (CatStat stat : CatStat.values()) {
            EditBox box = input(stat.ordinal(), stat.serializedName(), draft.getDouble(stat.serializedName()),
                    Component.translatable("stat.laowu.cat." + stat.serializedName()),
                    Component.translatable("screen.laowu.world.multiplier_help").append("\n")
                            .append(Component.translatable("screen.laowu.world.formula." + stat.serializedName())));
            box.setResponder(value -> { selected = stat; updateSave(); });
            fields.put(stat, addRenderableWidget(box));
        }
        for (CatOutfitType outfit : ServerConfig.CAREERS) {
            var inputs = new EnumMap<CatSuitSetting, EditBox>(CatSuitSetting.class);
            for (CatSuitSetting setting : CatSuitSetting.values()) {
                List<CatSuitSetting> section = setting.stat() == null ? CatSuitSetting.COMBAT : CatSuitSetting.BONUSES;
                int index = section.indexOf(setting);
                Component help = suitSettingName(setting).copy().append("\n")
                        .append(Component.translatable("screen.laowu.world.suit_help." + setting.id()));
                if (!setting.appliesTo(outfit))
                    help = Component.translatable("screen.laowu.world.suit_support").append("\n").append(help);
                EditBox box = input(index, setting.lockKey(outfit), setting.read(draft, outfit),
                        suitSettingName(setting), help);
                box.setY(top + 80 + (index / 2) * 22);
                box.setEditable(editable && setting.appliesTo(outfit) && !isLocked(setting.lockKey(outfit)));
                box.setResponder(value -> { selectedSuitSetting = setting; updateSave(); });
                inputs.put(setting, addRenderableWidget(box));
            }
            careerFields.put(outfit, inputs);
        }
        int selectorWidth = panelWidth / 2;
        previousCareer = addRenderableWidget(Button.builder(Component.literal("<"), b -> changeCareer(-1))
                .bounds(left, top + 54, 20, 18).build());
        careerSelector = addRenderableWidget(Button.builder(careerName(selectedCareer), b -> changeCareer(1))
                .bounds(left + 22, top + 54, selectorWidth - 46, 18).build());
        nextCareer = addRenderableWidget(Button.builder(Component.literal(">"), b -> changeCareer(1))
                .bounds(left + selectorWidth - 22, top + 54, 20, 18).build());
        int buttonWidth = (panelWidth - selectorWidth - 12) / 2;
        suitSection = addRenderableWidget(Button.builder(Component.empty(), b -> {
            suitBonusesPage = !suitBonusesPage;
            selectedSuitSetting = suitBonusesPage ? CatSuitSetting.ATTACK_STAT : CatSuitSetting.DAMAGE;
            clearCareerFocus();
            updatePageVisibility();
        }).bounds(left + selectorWidth + 4, top + 54, buttonWidth, 18)
                .tooltip(Tooltip.create(Component.translatable("screen.laowu.world.suit_section_help"))).build());
        resetCareers = addRenderableWidget(Button.builder(
                Component.translatable("screen.laowu.world.career_reset"), b -> resetCareerDefaults())
                .bounds(left + selectorWidth + buttonWidth + 8, top + 54, buttonWidth, 18)
                .tooltip(Tooltip.create(Component.translatable("screen.laowu.world.career_reset_help"))).build());
        switches.add(addRenderableWidget(toggle("show_hell_recipes", top + 120)));
        switches.add(addRenderableWidget(toggle("wild_cats_flee", top + 120)));
        switches.add(addRenderableWidget(toggle("cats_hiss", top + 142)));
        initTraits();
        save = addRenderableWidget(Button.builder(Component.translatable("screen.laowu.world.save"), b -> {
            for (CatStat stat : CatStat.values())
                draft.putDouble(stat.serializedName(), Double.parseDouble(fields.get(stat).getValue()));
            for (CatOutfitType outfit : ServerConfig.CAREERS)
                for (CatSuitSetting setting : CatSuitSetting.values())
                    setting.write(draft, outfit, Double.parseDouble(careerFields.get(outfit).get(setting).getValue()));
            save.active = false;
            ModNetwork.requestWorldSettings(true, draft.copy());
        }).bounds(left, top + 212, (panelWidth - 6) / 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(left + (panelWidth + 6) / 2, top + 212, (panelWidth - 6) / 2, 20).build());
        updatePageVisibility();
        updateSave();
    }

    private EditBox input(int index, String key, double value, Component label, Component help) {
        int column = panelWidth / 2;
        EditBox box = new EditBox(font, left + (index % 2) * column + column - 88,
                top + 54 + (index / 2) * 22, 78, 18, label);
        box.setMaxLength(24);
        // Keep exact stored precision, including very small finite scientific notation.
        box.setValue(value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value));
        box.setEditable(editable && !isLocked(key));
        box.setTextColorUneditable(0xFFAAAAAA);
        box.setTooltip(settingTooltip(key, help));
        return box;
    }

    private void changePage(int page) {
        careerPage = page == 1;
        traitPage = page == 2;
        setFocused(null);
        fields.values().forEach(box -> box.setFocused(false));
        clearCareerFocus();
        traitSearch.setFocused(false);
        // Do not recreate inputs: unsaved text (even invalid text) survives tab switches.
        updatePageVisibility();
    }
    private void clearCareerFocus() {
        careerFields.values().forEach(inputs -> inputs.values().forEach(box -> box.setFocused(false)));
        setFocused(null);
    }
    private void changeCareer(int direction) {
        selectedCareer = ServerConfig.CAREERS.get(Math.floorMod(
                ServerConfig.CAREERS.indexOf(selectedCareer) + direction, ServerConfig.CAREERS.size()));
        clearCareerFocus();
        updatePageVisibility();
    }
    private void resetCareerDefaults() {
        CatSuitSetting previousSelection = selectedSuitSetting;
        ClientWorldSettings.editableSuitDefaults(draft, selectedCareer).forEach((setting, value) ->
                careerFields.get(selectedCareer).get(setting).setValue(Double.toString(value)));
        selectedSuitSetting = previousSelection;
        // Reset this suit's unlocked drafts across both sections; Save is the only commit action.
        updateSave();
    }
    private void updatePageVisibility() {
        fields.values().forEach(box -> box.visible = !careerPage && !traitPage);
        careerFields.forEach((outfit, inputs) -> inputs.forEach((setting, box) ->
                box.visible = careerPage && outfit == selectedCareer && (setting.stat() != null) == suitBonusesPage));
        previousCareer.visible = nextCareer.visible = careerSelector.visible = suitSection.visible = careerPage;
        careerSelector.setMessage(careerName(selectedCareer));
        suitSection.setMessage(Component.translatable(suitBonusesPage
                ? "screen.laowu.world.suit_bonuses" : "screen.laowu.world.suit_combat"));
        resetCareers.visible = careerPage;
        resetCareers.active = !ClientWorldSettings.editableSuitDefaults(draft, selectedCareer).isEmpty();
        switches.forEach(button -> button.visible = !careerPage && !traitPage);
        attributesTab.active = careerPage || traitPage;
        careersTab.active = !careerPage;
        traitsTab.active = !traitPage;
        traitSearch.visible = traitPage;
        traitHelp.visible = traitPage;
        previousTraits.visible = nextTraits.visible = traitPage;
        refreshTraits();
    }
    private void initTraits() {
        traitSearch = new EditBox(font, left, top + 54, panelWidth - 26, 18,
                Component.translatable("screen.laowu.world.trait_search"));
        traitSearch.setMaxLength(128);
        traitSearch.setHint(Component.translatable("screen.laowu.world.trait_search"));
        traitSearch.setValue(traitQuery);
        traitSearch.setResponder(value -> {
            traitQuery = value;
            traitOffset = 0;
            filterTraits();
        });
        addRenderableWidget(traitSearch);
        traitHelp = addRenderableWidget(Button.builder(Component.literal("?"), b -> {})
                .bounds(left + panelWidth - 22, top + 54, 22, 18)
                .tooltip(settingTooltip(CatTraitConfig.KEY,
                        Component.translatable("screen.laowu.world.traits_help"))).build());
        for (int row = 0; row < TRAIT_ROWS; row++) {
            final int index = row;
            traitButtons.add(addRenderableWidget(Button.builder(Component.empty(), b -> {
                if (!editable || isLocked(CatTraitConfig.KEY)) return;
                CatTrait trait = filteredTraits.get(traitOffset + index);
                var disabled = new java.util.TreeSet<>(CatTraitConfig.read(draft));
                String id = trait.id().toString();
                if (!disabled.remove(id)) disabled.add(id);
                // Preserve unrecognized IDs instead of silently deleting future-version settings.
                CatTraitConfig.write(draft, disabled);
                refreshTraits();
            }).bounds(left, top + 76 + row * 19, panelWidth, 18).build()));
        }
        previousTraits = addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            traitOffset = Math.max(0, traitOffset - TRAIT_ROWS);
            refreshTraits();
        }).bounds(left, top + 192, 26, 16).build());
        nextTraits = addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            traitOffset += TRAIT_ROWS;
            refreshTraits();
        }).bounds(left + panelWidth - 26, top + 192, 26, 16).build());
        filterTraits();
    }
    private void filterTraits() {
        String query = traitQuery.strip().toLowerCase(Locale.ROOT);
        filteredTraits = java.util.Arrays.stream(CatTrait.values()).filter(trait -> query.isEmpty()
                || (trait.title().getString() + " " + trait.id() + " "
                + trait.description(1).getString()).toLowerCase(Locale.ROOT).contains(query)).toList();
        refreshTraits();
    }
    private void refreshTraits() {
        if (previousTraits == null || nextTraits == null) return;
        traitOffset = Math.min(traitOffset, Math.max(0, (filteredTraits.size() - 1) / TRAIT_ROWS * TRAIT_ROWS));
        var disabled = CatTraitConfig.read(draft);
        for (int row = 0; row < traitButtons.size(); row++) {
            Button b = traitButtons.get(row);
            int index = traitOffset + row;
            b.visible = traitPage && index < filteredTraits.size();
            b.active = b.visible && editable && !isLocked(CatTraitConfig.KEY);
            if (index >= filteredTraits.size()) continue;
            CatTrait trait = filteredTraits.get(index);
            boolean banned = disabled.contains(trait.id().toString());
            b.active &= banned || disabled.size() < CatTraitConfig.MAX_IDS;
            Component label = Component.translatable(banned ? "screen.laowu.world.trait_banned"
                    : "screen.laowu.world.trait_allowed", trait.title());
            b.setMessage(Component.literal(font.plainSubstrByWidth(label.getString(), panelWidth - 12)));
            b.setTooltip(settingTooltip(CatTraitConfig.KEY, trait.title().copy().append("\n")
                    .append(trait.id().toString()).append("\n").append(trait.description(1))));
        }
        previousTraits.active = traitOffset > 0;
        nextTraits.active = traitOffset + TRAIT_ROWS < filteredTraits.size();
    }
    private Button toggle(String key, int y) {
        Button b = Button.builder(toggleLabel(key), button -> {
            draft.putBoolean(key, !draft.getBoolean(key));
            button.setMessage(toggleLabel(key));
        }).bounds(left + (key.equals("wild_cats_flee") ? (panelWidth + 6) / 2 : 0),
                y, key.equals("cats_hiss") ? panelWidth : (panelWidth - 6) / 2, 20)
                .tooltip(settingTooltip(key, Component.translatable("screen.laowu.world." + key + ".help"))).build();
        b.active = editable && !isLocked(key);
        return b;
    }
    private Component toggleLabel(String key) {
        return settingLabel(key, Component.translatable("screen.laowu.world." + key,
                Component.translatable(draft.getBoolean(key) ? "options.on" : "options.off")));
    }
    private boolean isLocked(String key) {
        return draft != null && draft.getCompound(ServerConfig.LOCKS_TAG).getBoolean(key);
    }
    private boolean hasEditableSetting() {
        for (CatStat stat : CatStat.values()) if (!isLocked(stat.serializedName())) return true;
        for (CatOutfitType outfit : ServerConfig.CAREERS)
            for (CatSuitSetting setting : CatSuitSetting.values())
                if (setting.appliesTo(outfit) && !isLocked(setting.lockKey(outfit))) return true;
        for (String key : GlobalConfig.SWITCHES) if (!isLocked(key)) return true;
        return !isLocked(CatTraitConfig.KEY);
    }
    private boolean hasGlobalLocks() {
        if (draft == null) return false;
        CompoundTag locks = draft.getCompound(ServerConfig.LOCKS_TAG);
        for (String key : locks.getAllKeys()) if (locks.getBoolean(key)) return true;
        return false;
    }
    private Component settingLabel(String key, Component label) {
        return isLocked(key) ? Component.translatable("screen.laowu.world.global_value", label) : label;
    }
    private Tooltip settingTooltip(String key, Component help) {
        if (!isLocked(key)) return Tooltip.create(help);
        return Tooltip.create(Component.translatable("screen.laowu.world.global_locked", GlobalConfig.FILE_NAME)
                .append("\n\n").append(help));
    }
    private static boolean validInput(EditBox box) {
        try { return ServerConfig.validMultiplier(Double.parseDouble(box.getValue())); }
        catch (NumberFormatException ex) { return false; }
    }
    private static boolean validSuitInput(CatSuitSetting setting, EditBox box) {
        try { return setting.valid(Double.parseDouble(box.getValue())); }
        catch (NumberFormatException ex) { return false; }
    }
    private CatSuitSettings previewSuit() {
        var inputs = careerFields.get(selectedCareer);
        if (!inputs.entrySet().stream().allMatch(entry -> validSuitInput(entry.getKey(), entry.getValue()))) return null;
        return CatSuitSettings.read(selectedCareer, setting -> Double.parseDouble(inputs.get(setting).getValue()));
    }
    private void updateSave() {
        if (save == null) return;
        save.active = editable && hasEditableSetting() && fields.size() == CatStat.values().length
                && careerFields.size() == ServerConfig.CAREERS.size()
                && fields.values().stream().allMatch(WorldSettingsScreen::validInput)
                && careerFields.values().stream().allMatch(inputs -> inputs.size() == CatSuitSetting.values().length
                    && inputs.entrySet().stream().allMatch(entry -> validSuitInput(entry.getKey(), entry.getValue())));
    }
    public void receive(CompoundTag tag) {
        draft = tag.copy();
        editable = tag.getBoolean("can_edit");
        if (minecraft != null) { clearWidgets(); init(); }
    }
    private static Component careerName(CatOutfitType outfit) {
        return Component.translatable("screen.laowu.world.career." + outfit.id());
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.flush();
        renderBackground(g);
        ConfigScreenRendering.beginForeground(g);
        try {
            g.drawCenteredString(font, title, width / 2, top, 0xFFFFFFFF);
            g.drawCenteredString(font, Component.translatable(!editable ? "screen.laowu.world.read_only"
                    : hasGlobalLocks() ? "screen.laowu.world.global_notice" : "screen.laowu.world.editable"),
                    width / 2, top + 16, 0xFFAAAAAA);
            if (traitPage) {
                Component count = Component.translatable("screen.laowu.world.trait_page",
                        traitOffset / TRAIT_ROWS + 1, Math.max(1, (filteredTraits.size() + TRAIT_ROWS - 1) / TRAIT_ROWS),
                        CatTraitConfig.read(draft).size());
                g.drawCenteredString(font, count, width / 2, top + 196, 0xFFCCCCCC);
                if (filteredTraits.isEmpty())
                    g.drawCenteredString(font, Component.translatable("screen.laowu.world.trait_empty"),
                            width / 2, top + 116, 0xFFAAAAAA);
            } else if (careerPage) {
                List<CatSuitSetting> section = suitBonusesPage ? CatSuitSetting.BONUSES : CatSuitSetting.COMBAT;
                for (int index = 0; index < section.size(); index++) {
                    CatSuitSetting setting = section.get(index);
                    EditBox box = careerFields.get(selectedCareer).get(setting);
                    if (box.isFocused() || box.isMouseOver(mouseX, mouseY)) selectedSuitSetting = setting;
                    int column = panelWidth / 2;
                    Component label = settingLabel(setting.lockKey(selectedCareer), suitSettingName(setting));
                    g.drawString(font, font.plainSubstrByWidth(label.getString(), Math.max(12, column - 96)),
                            left + (index % 2) * column, top + 85 + (index / 2) * 22, 0xFFFFFFFF);
                }
            } else {
                for (CatStat stat : CatStat.values()) {
                    EditBox box = fields.get(stat);
                    if (box.isFocused() || box.isMouseOver(mouseX, mouseY)) selected = stat;
                    drawLabel(g, stat.ordinal(), settingLabel(stat.serializedName(),
                            Component.translatable("stat.laowu.cat." + stat.serializedName())));
                }
            }
            if (!traitPage) {
            EditBox selectedBox = fields.get(selected);
            double multiplier = validInput(selectedBox) ? Double.parseDouble(selectedBox.getValue()) : 1;
            CatSuitSettings preview = careerPage ? previewSuit() : null;
            Component exampleTitle = careerPage
                    ? Component.translatable("screen.laowu.world.suit_example", suitSettingName(selectedSuitSetting))
                    : Component.translatable("screen.laowu.world.example." + selected.serializedName());
            g.drawCenteredString(font, exampleTitle, width / 2, top + 174, 0xFFFFCC66);
            int[] points = {0, 50, 100, 150};
            for (int i = 0; i < points.length; i++) {
                String value;
                if (careerPage) {
                    value = preview == null ? "—" : suitExample(preview, points[i]);
                } else value = example(selected, points[i] * multiplier);
                drawExample(g, points[i] + " → " + value, left + panelWidth * (2 * i + 1) / 8);
            }
            }
            // Avoid Screen.render's second background pass under Modern UI on 1.21.
            for (var widget : renderables) widget.render(g, mouseX, mouseY, partialTick);
        } finally {
            ConfigScreenRendering.endForeground(g);
        }
    }
    private static Component suitSettingName(CatSuitSetting setting) {
        return Component.translatable("screen.laowu.world.suit_setting." + setting.id());
    }
    private String suitExample(CatSuitSettings settings, int base) {
        var snapshot = CareerCatBehavior.snapshot(selectedCareer, base, settings);
        return switch (selectedSuitSetting) {
            case HEALTH, HEALTH_STAT -> number(snapshot.health());
            case ARMOR -> number(snapshot.armor());
            case TOUGHNESS -> number(snapshot.toughness());
            case STAMINA_STAT -> number(snapshot.armor()) + "/" + number(snapshot.toughness());
            case INTERVAL_BASE, INTERVAL_PER_SPEED, MIN_INTERVAL ->
                    snapshot.attacks() ? snapshot.attackIntervalTicks() + "t" : "—";
            case SPEED_STAT -> number(cn.laowu.mod.genetics.CatAttributeEffects.movementMultiplier(
                    settings.attribute(base, CatStat.SPEED))) + "×";
            case INTELLIGENCE_STAT -> number(cn.laowu.mod.genetics.CatAttributeEffects.criticalDamageMultiplier(
                    settings.attribute(base, CatStat.INTELLIGENCE))) + "×";
            case LUCK_STAT -> number(100 * cn.laowu.mod.genetics.CatAttributeEffects.criticalChance(
                    settings.attribute(base, CatStat.LUCK))) + "%";
            case DAMAGE, ATTACK_STAT -> snapshot.attacks() ? number(snapshot.attackDamage()) : "—";
        };
    }
    private void drawLabel(GuiGraphics g, int index, Component label) {
        int column = panelWidth / 2;
        String text = font.plainSubstrByWidth(label.getString(), Math.max(12, column - 96));
        g.drawString(font, text, left + (index % 2) * column, top + 59 + (index / 2) * 22, 0xFFFFFFFF);
    }
    private void drawExample(GuiGraphics g, String text, int center) {
        float scale = Math.min(1.0F, (panelWidth / 4.0F - 6) / Math.max(1, font.width(text)));
        g.pose().pushPose();
        g.pose().translate(center, top + 190, 0);
        g.pose().scale(scale, scale, 1.0F);
        g.drawCenteredString(font, text, 0, 0, 0xFFFFFFFF);
        g.pose().popPose();
    }
    private static String example(CatStat stat, double e) {
        return switch (stat) {
            case ATTACK -> number(2 + 0.08 * e);
            case HEALTH -> number(10 + 0.4 * e);
            case SPEED -> number(0.75 + 0.005 * e) + "×";
            case STAMINA -> number(2 + 0.16 * e) + "/" + number(0.05 * e);
            case INTELLIGENCE -> number(1 + 0.01 * e) + "×";
            case LUCK -> number(Math.min(100, 2 + 0.18 * e)) + "%";
        };
    }
    private static String number(double value) {
        return String.format(Locale.ROOT, "%.3f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
