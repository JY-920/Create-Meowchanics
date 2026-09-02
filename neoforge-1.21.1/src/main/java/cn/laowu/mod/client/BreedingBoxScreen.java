package cn.laowu.mod.client;

import cn.laowu.mod.BreedingBoxMenu;
import cn.laowu.mod.CatClothesData;
import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.CatPoseData;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.create.BreedingBoxTier;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatAttributeEffects;
import cn.laowu.mod.genetics.CatAttributeProfile;
import cn.laowu.mod.genetics.CatGenomeData;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.genetics.CatTraitEffects;
import cn.laowu.mod.genetics.CatTraitProfile;
import cn.laowu.mod.item.CatPancakeItem;
import cn.laowu.mod.item.ItemCustomData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Pixel-authored breeding UI with live parent previews and compact stat grids. */
public final class BreedingBoxScreen extends AbstractContainerScreen<BreedingBoxMenu> {
    private static final ResourceLocation BACKGROUND =
            LaoWuMod.id("textures/gui/breeding_box.png");
    private static final ResourceLocation MUTATION_LEVELS =
            LaoWuMod.id("textures/gui/breeding_box_reference.png");
    private static final ResourceLocation LEVEL_ICONS =
            LaoWuMod.id("textures/gui/breeding_box_level_icons.png");
    private static final ResourceLocation PROGRESS_FILL =
            LaoWuMod.id("textures/gui/breeding_box_progress_fill.png");
    private static final ResourceLocation ATTRIBUTE_ICONS =
            LaoWuMod.id("textures/gui/cat_attribute_icons.png");
    private static final ResourceLocation NUMBER_GLYPHS =
            LaoWuMod.id("textures/gui/cat_stat_numbers.png");
    private static final ResourceLocation BLUE_NUMBER_GLYPHS =
            LaoWuMod.id("textures/gui/cat_stat_numbers_blue.png");
    private static final ResourceLocation TIER_ICONS =
            LaoWuMod.id("textures/gui/cat_stat_tiers.png");
    /** Flat fill behind the mutation digits in the supplied pixel panel. */
    private static final int MUTATION_NUMBER_BACKGROUND = 0xFFB59370;

    private static final int PANEL_WIDTH = 211;
    private static final int PANEL_HEIGHT = 212;
    private static final int MAIN_BOX_WIDTH = 171;
    private static final int TRAIT_CARD_Y = 57;
    private static final int TRAIT_CARD_SPACING = 28;
    private static final int LEFT_TRAIT_X = 10;
    private static final int RIGHT_TRAIT_X = 87;
    private static final CatStat[] GRID_STATS = {
            CatStat.ATTACK, CatStat.HEALTH, CatStat.SPEED,
            CatStat.STAMINA, CatStat.INTELLIGENCE, CatStat.LUCK
    };

    private ItemStack cachedFather = ItemStack.EMPTY;
    private ItemStack cachedMother = ItemStack.EMPTY;
    private Cat fatherPreview;
    private Cat motherPreview;

    public BreedingBoxScreen(BreedingBoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PANEL_WIDTH;
        imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        // Center the large paper box itself. The controls remain attached to
        // its right edge and therefore intentionally extend to the right.
        leftPos = (width - MAIN_BOX_WIDTH) / 2;
        topPos = (height - PANEL_HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (renderAttributeTooltip(graphics, mouseX, mouseY)) return;
        renderTraitTooltip(graphics, mouseX, mouseY);
        renderControlTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0,
                PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);

        int progressWidth = menu.progressWidth(154);
        if (progressWidth > 0) {
            // The authored fill contains both the line and the two cats. Blit
            // only the completed horizontal prefix so every crossed pixel,
            // including the silhouettes, changes to the completed palette.
            graphics.blit(PROGRESS_FILL, leftPos + 9, topPos + 171,
                    0, 0, progressWidth, 30, 154, 30);
        }

        ItemStack father = menu.father();
        ItemStack mother = menu.mother();
        if (!father.isEmpty()) {
            fatherPreview = updatePreview(father, cachedFather, fatherPreview);
            cachedFather = father.copy();
            renderTraitCards(graphics, father, LEFT_TRAIT_X);
            renderParentStats(graphics, father, fatherPreview, 0);
            renderParentCat(graphics, fatherPreview, leftPos + 46, topPos + 51,
                    mouseX, mouseY);
        } else {
            cachedFather = ItemStack.EMPTY;
            fatherPreview = null;
        }
        if (!mother.isEmpty()) {
            motherPreview = updatePreview(mother, cachedMother, motherPreview);
            cachedMother = mother.copy();
            renderTraitCards(graphics, mother, RIGHT_TRAIT_X);
            renderParentStats(graphics, mother, motherPreview, 87);
            renderParentCat(graphics, motherPreview, leftPos + 126, topPos + 51,
                    mouseX, mouseY);
        } else {
            cachedMother = ItemStack.EMPTY;
            motherPreview = null;
        }

        renderMutationAndLevel(graphics, menu.tier(), menu.effectiveMutationPercent());
    }

    private void renderMutationAndLevel(GuiGraphics graphics, BreedingBoxTier tier,
                                        float effectiveMutationPercent) {
        // Reuse the supplied pixel frame and icon, then replace its authored
        // fixed tier number with the live food- and parent-luck-adjusted rate.
        graphics.blit(MUTATION_LEVELS, leftPos + 172, topPos + 90,
                186, 225, 34, 13, 512, 512);
        // The reference sheet contains an authored "00.0" example. Clear only
        // its three digit cells before drawing the live value; keep the hand-
        // drawn decimal point, percent glyph, frame and mutation icon intact.
        graphics.fill(leftPos + 180, topPos + 93,
                leftPos + 190, topPos + 100, MUTATION_NUMBER_BACKGROUND);
        graphics.fill(leftPos + 191, topPos + 93,
                leftPos + 196, topPos + 100, MUTATION_NUMBER_BACKGROUND);
        renderMutationNumber(graphics, effectiveMutationPercent,
                leftPos + 180, topPos + 93);
        if (tier != BreedingBoxTier.BASIC) {
            graphics.blit(LEVEL_ICONS, leftPos + 182, topPos + 131,
                    (tier.level() - 2) * 18, 0, 18, 18, 36, 18);
        }
    }

    private static void renderMutationNumber(GuiGraphics graphics, float percent,
                                             int x, int y) {
        // The supplied frame already contains the exact decimal point,
        // percent glyph, separators and palette. Replace only its three
        // variable digit cells so none of those authored pixels are covered.
        String value = String.format(Locale.ROOT, "%04.1f",
                Math.min(99.9F, Math.max(0.0F, percent)));
        drawMutationDigit(graphics, value.charAt(0) - '0', x, y);
        drawMutationDigit(graphics, value.charAt(1) - '0', x + 4, y);
        drawMutationDigit(graphics, value.charAt(3) - '0', x + 10, y);
    }

    private static void drawMutationDigit(GuiGraphics graphics, int digit, int x, int y) {
        graphics.blit(NUMBER_GLYPHS, x, y,
                (digit + 1) * 7, 0, 7, 7, 77, 7);
    }

    private void renderParentStats(GuiGraphics graphics, ItemStack stack,
                                   Cat preview, int parentX) {
        CatAttributeProfile profile = CatAttributeData.read(stack).orElse(null);
        CatTraitProfile traits = CatTraitData.read(stack).orElse(CatTraitProfile.EMPTY);
        boolean limits = hasShiftDown();
        boolean night = minecraft != null && CatTraitEffects.isNight(minecraft.level);
        boolean day = minecraft != null && CatTraitEffects.isDay(minecraft.level);
        for (int index = 0; index < GRID_STATS.length; index++) {
            CatStat stat = GRID_STATS[index];
            int column = index / 3;
            int row = index % 3;
            int x = leftPos + parentX + (column == 0 ? 12 : 43);
            int y = topPos + 170 + row * 9;
            int value = profile == null ? -1
                    : limits ? profile.potential(stat)
                    : preview != null ? CatAttributeEffects.effectiveValue(
                            preview, profile, traits, stat)
                    : CatAttributeEffects.effectiveValue(
                            profile, traits, stat, night, day);
            boolean abnormal = value < 0 || value > 999;

            graphics.blit(ATTRIBUTE_ICONS, x, y,
                    attributeIconIndex(stat) * 8, 0, 8, 8, 48, 8);
            renderThreeDigits(graphics, abnormal ? 0 : value,
                    x + 23, y + 1, limits);
            graphics.blit(TIER_ICONS, x + 23, y + 1,
                    tierIndex(value, abnormal) * 6, 0, 6, 6, 42, 6);
        }
    }

    private static void renderThreeDigits(GuiGraphics graphics, int value,
                                          int rightExclusive, int y, boolean blue) {
        String digits = String.format(Locale.ROOT, "%03d", Math.max(0, Math.min(999, value)));
        int x = rightExclusive - 15;
        for (int index = 0; index < digits.length(); index++) {
            int digit = digits.charAt(index) - '0';
            ResourceLocation texture = blue ? BLUE_NUMBER_GLYPHS : NUMBER_GLYPHS;
            int sourceGlyph = digit + 1;
            graphics.blit(texture, x + index * 4, y,
                    sourceGlyph * 7, 0, 7, 7, 77, 7);
        }
    }

    private static int tierIndex(int value, boolean abnormal) {
        if (abnormal) return 0;
        if (value < 20) return 1;
        if (value < 40) return 2;
        if (value < 60) return 3;
        if (value < 80) return 4;
        if (value < 100) return 5;
        return 6;
    }

    private static int attributeIconIndex(CatStat stat) {
        return stat.ordinal();
    }

    private void renderTraitCards(GuiGraphics graphics, ItemStack stack, int localX) {
        CatTraitCardRenderer.renderCards(graphics, font,
                CatTraitData.read(stack).orElse(CatTraitProfile.EMPTY),
                leftPos + localX, topPos + TRAIT_CARD_Y, TRAIT_CARD_SPACING);
    }

    private boolean renderAttributeTooltip(GuiGraphics graphics,
                                           int mouseX, int mouseY) {
        ItemStack[] parents = {menu.father(), menu.mother()};
        Cat[] previews = {fatherPreview, motherPreview};
        int[] parentOffsets = {0, 87};
        boolean limits = hasShiftDown();
        boolean night = minecraft != null && CatTraitEffects.isNight(minecraft.level);
        boolean day = minecraft != null && CatTraitEffects.isDay(minecraft.level);
        for (int parent = 0; parent < parents.length; parent++) {
            ItemStack stack = parents[parent];
            if (stack.isEmpty()) continue;
            CatAttributeProfile profile = CatAttributeData.read(stack).orElse(null);
            if (profile == null) continue;
            CatTraitProfile traits = CatTraitData.read(stack)
                    .orElse(CatTraitProfile.EMPTY);
            for (int index = 0; index < GRID_STATS.length; index++) {
                int column = index / 3;
                int row = index % 3;
                int localX = parentOffsets[parent] + (column == 0 ? 12 : 43);
                int localY = 170 + row * 9;
                if (!inside(mouseX, mouseY, localX, localY, 29, 8)) continue;
                CatStat stat = GRID_STATS[index];
                int value = limits ? profile.potential(stat)
                        : previews[parent] != null
                        ? CatAttributeEffects.effectiveValue(
                                previews[parent], profile, traits, stat)
                        : CatAttributeEffects.effectiveValue(
                                profile, traits, stat, night, day);
                CatAttributeEffectTooltip.render(graphics, font, stat, value,
                        limits, CatPancakeItem.getOutfit(stack), mouseX, mouseY);
                return true;
            }
        }
        return false;
    }

    private void renderTraitTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (CatTraitCardRenderer.renderTooltip(graphics, font,
                CatTraitData.read(menu.father()).orElse(CatTraitProfile.EMPTY),
                leftPos + LEFT_TRAIT_X, topPos + TRAIT_CARD_Y,
                TRAIT_CARD_SPACING, mouseX, mouseY)) return;
        CatTraitCardRenderer.renderTooltip(graphics, font,
                CatTraitData.read(menu.mother()).orElse(CatTraitProfile.EMPTY),
                leftPos + RIGHT_TRAIT_X, topPos + TRAIT_CARD_Y,
                TRAIT_CARD_SPACING, mouseX, mouseY);
    }

    private void renderControlTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (inside(mouseX, mouseY, 172, 90, 34, 13)) {
            String chance = String.format(Locale.ROOT, "%.1f",
                    menu.effectiveMutationPercent());
            renderWrappedTooltip(graphics, List.of(
                    Component.translatable(
                            "gui.laowu.breeding_box.mutation.title")
                            .withStyle(ChatFormatting.GOLD),
                    Component.translatable(
                            "gui.laowu.breeding_box.mutation.current", chance)
                            .withStyle(ChatFormatting.AQUA),
                    Component.translatable(
                            "gui.laowu.breeding_box.mutation.source")
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable(
                            "gui.laowu.breeding_box.mutation.attributes")
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable(
                            "gui.laowu.breeding_box.mutation.other")
                            .withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            return;
        }
        if (inside(mouseX, mouseY, 180, 110, 20, 21)) {
            renderWrappedTooltip(graphics, List.of(
                    Component.translatable("gui.laowu.breeding_box.info.title")
                            .withStyle(ChatFormatting.GOLD),
                    Component.translatable("gui.laowu.breeding_box.info.insert")
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.laowu.breeding_box.info.extract")
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.laowu.breeding_box.info.start")
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.laowu.breeding_box.info.shift")
                            .withStyle(ChatFormatting.BLUE),
                    Component.translatable("gui.laowu.breeding_box.info.attributes")
                            .withStyle(ChatFormatting.AQUA),
                    Component.translatable("gui.laowu.breeding_box.info.traits")
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.translatable("gui.laowu.breeding_box.info.appearance")
                            .withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
            return;
        }
        if (inside(mouseX, mouseY, 180, 131, 20, 23)) {
            BreedingBoxTier tier = menu.tier();
            String chance = String.format(Locale.ROOT, "%.0f",
                    tier.mutationChance() * 100.0F);
            int seconds = Math.max(1, tier.durationTicks() / 20);
            Component duration = seconds >= 60 && seconds % 60 != 0
                    ? Component.translatable(
                    "gui.laowu.breeding_box.level.minutes_seconds",
                    seconds / 60, seconds % 60)
                    : seconds % 60 == 0
                    ? Component.translatable(
                    "gui.laowu.breeding_box.level.minutes", seconds / 60)
                    : Component.translatable(
                    "gui.laowu.breeding_box.level.seconds", seconds);
            List<Component> levelTooltip = new ArrayList<>(List.of(
                    Component.translatable("gui.laowu.breeding_box.level.title")
                            .withStyle(ChatFormatting.GOLD),
                    Component.translatable("gui.laowu.breeding_box.level.current",
                                    romanTier(tier.level()))
                            .withStyle(ChatFormatting.AQUA),
                    Component.translatable("gui.laowu.breeding_box.level.mutation",
                                    chance)
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.laowu.breeding_box.level.duration",
                                    duration)
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.laowu.breeding_box.level.effect")
                            .withStyle(ChatFormatting.GRAY)));
            if (tier == BreedingBoxTier.ADVANCED) {
                levelTooltip.add(Component.translatable(
                                "gui.laowu.breeding_box.level.advanced_replacement")
                        .withStyle(ChatFormatting.YELLOW));
            }
            renderWrappedTooltip(graphics, levelTooltip, mouseX, mouseY);
        }
    }

    private void renderWrappedTooltip(GuiGraphics graphics,
                                      List<Component> components,
                                      int mouseX, int mouseY) {
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component component : components) {
            wrapped.addAll(font.split(component, 250));
        }
        graphics.renderTooltip(font, wrapped, mouseX, mouseY);
    }

    private boolean inside(int mouseX, int mouseY, int x, int y,
                           int width, int height) {
        int screenX = leftPos + x;
        int screenY = topPos + y;
        return mouseX >= screenX && mouseX < screenX + width
                && mouseY >= screenY && mouseY < screenY + height;
    }

    private static String romanTier(int level) {
        return switch (level) {
            case 2 -> "II";
            case 3 -> "III";
            default -> "I";
        };
    }

    private Cat updatePreview(ItemStack stack, ItemStack cached, Cat existing) {
        if (existing != null && ItemStack.isSameItemSameComponents(stack, cached)) return existing;
        if (minecraft == null || minecraft.level == null) return null;
        Cat cat = EntityType.CAT.create(minecraft.level);
        if (cat == null) return null;

        CompoundTag root = ItemCustomData.copy(stack);
        if (!root.isEmpty() && root.contains(CatPancakeItem.CAT_DATA_TAG, Tag.TAG_COMPOUND)) {
            cat.load(root.getCompound(CatPancakeItem.CAT_DATA_TAG).copy());
        } else {
            Holder<CatVariant> variant = BuiltInRegistries.CAT_VARIANT.getHolder(
                    CatPancakeItem.variantId(stack)).orElseGet(
                    () -> BuiltInRegistries.CAT_VARIANT.getHolderOrThrow(CatVariant.RED));
            cat.setVariant(variant);
        }
        CatGenomeData.read(stack).ifPresent(genome -> CatGenomeData.set(cat, genome));
        CatTraitData.read(stack).ifPresent(profile -> CatTraitData.set(cat, profile));
        CatOutfitType outfit = CatPancakeItem.getOutfit(stack);
        if (outfit != CatOutfitType.NONE) {
            cat.getPersistentData().putBoolean(CatClothesData.EQUIPPED_TAG, true);
            cat.getPersistentData().putString(CatClothesData.OUTFIT_TAG, outfit.id());
        }
        cat.setAge(CatPancakeItem.isBaby(stack) ? -24_000 : 0);
        cat.setOrderedToSit(false);
        CatPoseData.setPose(cat, 0);
        return cat;
    }

    private static void renderParentCat(GuiGraphics graphics, Cat cat,
                                        int x, int y, int mouseX, int mouseY) {
        if (cat == null) return;
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                x - 15, y - 15, x + 15, y + 15, 30, 0.0625F,
                (float) x - mouseX, (float) y - 30.0F - mouseY, cat);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // All labels and headings are authored into the supplied pixel UI.
    }
}
