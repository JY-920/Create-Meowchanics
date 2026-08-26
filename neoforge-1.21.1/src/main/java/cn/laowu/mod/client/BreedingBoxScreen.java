package cn.laowu.mod.client;

import cn.laowu.mod.BreedingBoxMenu;
import cn.laowu.mod.CatPoseData;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.create.BreedingBoxTier;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatAttributeProfile;
import cn.laowu.mod.genetics.CatGenomeData;
import cn.laowu.mod.genetics.CatStat;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/** Pixel-authored breeding UI with live parent previews and compact stat grids. */
public final class BreedingBoxScreen extends AbstractContainerScreen<BreedingBoxMenu> {
    private static final ResourceLocation BACKGROUND =
            LaoWuMod.id("textures/gui/breeding_box.png");
    private static final ResourceLocation MUTATION_LEVELS =
            LaoWuMod.id("textures/gui/breeding_box_mutation_levels.png");
    private static final ResourceLocation LEVEL_ICONS =
            LaoWuMod.id("textures/gui/breeding_box_level_icons.png");
    private static final ResourceLocation PROGRESS_FILL =
            LaoWuMod.id("textures/gui/breeding_box_progress_fill.png");
    private static final ResourceLocation PARENT_LEFT =
            LaoWuMod.id("textures/gui/breeding_box_parent_left.png");
    private static final ResourceLocation PARENT_RIGHT =
            LaoWuMod.id("textures/gui/breeding_box_parent_right.png");
    private static final ResourceLocation ATTRIBUTE_ICONS =
            LaoWuMod.id("textures/gui/cat_attribute_icons.png");
    private static final ResourceLocation NUMBER_GLYPHS =
            LaoWuMod.id("textures/gui/cat_stat_numbers.png");
    private static final ResourceLocation BLUE_NUMBER_GLYPHS =
            LaoWuMod.id("textures/gui/cat_stat_numbers_blue.png");
    private static final ResourceLocation TIER_ICONS =
            LaoWuMod.id("textures/gui/cat_stat_tiers.png");

    private static final int PANEL_WIDTH = 211;
    private static final int PANEL_HEIGHT = 212;
    private static final int MAIN_BOX_WIDTH = 171;
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
            graphics.blit(PARENT_LEFT, leftPos, topPos, 0, 0,
                    PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
            renderParentStats(graphics, father, 0);
            fatherPreview = updatePreview(father, cachedFather, fatherPreview);
            cachedFather = father.copy();
            renderParentCat(graphics, fatherPreview, leftPos + 46, topPos + 51,
                    mouseX, mouseY);
        } else {
            cachedFather = ItemStack.EMPTY;
            fatherPreview = null;
        }
        if (!mother.isEmpty()) {
            graphics.blit(PARENT_RIGHT, leftPos, topPos, 0, 0,
                    PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
            renderParentStats(graphics, mother, 87);
            motherPreview = updatePreview(mother, cachedMother, motherPreview);
            cachedMother = mother.copy();
            renderParentCat(graphics, motherPreview, leftPos + 126, topPos + 51,
                    mouseX, mouseY);
        } else {
            cachedMother = ItemStack.EMPTY;
            motherPreview = null;
        }

        renderMutationAndLevel(graphics, menu.tier());
    }

    private void renderMutationAndLevel(GuiGraphics graphics, BreedingBoxTier tier) {
        int tierIndex = tier.level() - 1;
        graphics.blit(MUTATION_LEVELS, leftPos + 172, topPos + 90,
                tierIndex * 34, 0, 34, 13, 102, 13);
        if (tier != BreedingBoxTier.BASIC) {
            graphics.blit(LEVEL_ICONS, leftPos + 182, topPos + 131,
                    (tier.level() - 2) * 18, 0, 18, 18, 36, 18);
        }
    }

    private void renderParentStats(GuiGraphics graphics, ItemStack stack, int parentX) {
        CatAttributeProfile profile = CatAttributeData.read(stack).orElse(null);
        boolean limits = hasShiftDown();
        for (int index = 0; index < GRID_STATS.length; index++) {
            CatStat stat = GRID_STATS[index];
            int column = index / 3;
            int row = index % 3;
            int x = leftPos + parentX + (column == 0 ? 12 : 43);
            int y = topPos + 170 + row * 9;
            int value = profile == null ? -1
                    : limits ? profile.potential(stat) : profile.current(stat);
            boolean abnormal = value < 0 || value > 100;

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
        String digits = String.format(Locale.ROOT, "%03d", Math.max(0, Math.min(100, value)));
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
        return switch (stat) {
            case SPEED -> 3;
            case STAMINA -> 2;
            default -> stat.ordinal();
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
        cat.setAge(0);
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
