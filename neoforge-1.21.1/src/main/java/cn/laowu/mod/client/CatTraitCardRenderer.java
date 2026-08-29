package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatTraitInstance;
import cn.laowu.mod.genetics.CatTraitProfile;
import cn.laowu.mod.genetics.CatTraitRarity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Shared rendering for the four 72x27 trait cards and their detailed tooltip. */
final class CatTraitCardRenderer {
    static final int CARD_WIDTH = 72;
    static final int CARD_HEIGHT = 27;
    private static final int LEVELS = 7;
    private static final int RARITIES = 4;
    private static final ResourceLocation FRAMES =
            LaoWuMod.id("textures/gui/cat_trait_frames.png");

    static void renderCards(GuiGraphics graphics, Font font,
                            CatTraitProfile profile, int x, int y,
                            int rowSpacing) {
        List<CatTraitInstance> traits = resolved(profile).traits();
        for (int index = 0; index < traits.size() && index < 4; index++) {
            CatTraitInstance instance = traits.get(index);
            int cardY = y + index * rowSpacing;
            int displayedLevel = displayedLevel(instance);
            graphics.blit(FRAMES, x, cardY,
                    (displayedLevel - 1) * CARD_WIDTH,
                    instance.trait().rarity().frameIndex() * CARD_HEIGHT,
                    CARD_WIDTH, CARD_HEIGHT,
                    CARD_WIDTH * LEVELS, CARD_HEIGHT * RARITIES);

            Component title = instance.trait().title();
            int contentX = x + 7;
            int contentWidth = CARD_WIDTH - 9;
            int titleX = contentX + (contentWidth - font.width(title)) / 2;
            int titleY = cardY + (CARD_HEIGHT - font.lineHeight) / 2;
            graphics.drawString(font, title, titleX, titleY,
                    instance.trait().rarity().cardTextColour(), false);
        }
    }

    static boolean renderTooltip(GuiGraphics graphics, Font font,
                                 CatTraitProfile profile, int x, int y,
                                 int rowSpacing, int mouseX, int mouseY) {
        CatTraitInstance hovered = traitAt(profile, x, y, rowSpacing, mouseX, mouseY);
        if (hovered == null) return false;

        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(hovered.trait().title().copy()
                .withStyle(hovered.trait().rarity().textFormatting())
                .getVisualOrderText());
        lines.add(Component.translatable("gui.laowu.cat_trait.rarity_and_level",
                        Component.translatable("trait.laowu.rarity."
                                + hovered.trait().rarity().serializedName()),
                        romanLevel(hovered.level()),
                        romanLevel(hovered.trait().maxLevel()))
                .withStyle(ChatFormatting.GRAY).getVisualOrderText());
        lines.add(FormattedCharSequence.EMPTY);
        lines.addAll(font.split(hovered.trait().description(hovered.level()).copy()
                .withStyle(ChatFormatting.WHITE), 220));
        lines.add(FormattedCharSequence.EMPTY);
        if (!hovered.trait().upgradable()) {
            lines.add(Component.translatable("gui.laowu.cat_trait.not_upgradable")
                    .withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
        } else if (hovered.level() >= hovered.trait().maxLevel()) {
            lines.add(Component.translatable("gui.laowu.cat_trait.max_level")
                    .withStyle(ChatFormatting.GOLD).getVisualOrderText());
        } else {
            lines.addAll(font.split(hovered.trait()
                    .nextLevelDescription(hovered.level()).copy()
                    .withStyle(ChatFormatting.GREEN), 220));
        }
        graphics.renderTooltip(font, lines, mouseX, mouseY);
        return true;
    }

    private static CatTraitInstance traitAt(CatTraitProfile profile,
                                             int x, int y, int rowSpacing,
                                             int mouseX, int mouseY) {
        List<CatTraitInstance> traits = resolved(profile).traits();
        for (int index = 0; index < traits.size() && index < 4; index++) {
            int cardY = y + index * rowSpacing;
            if (mouseX >= x && mouseX < x + CARD_WIDTH
                    && mouseY >= cardY && mouseY < cardY + CARD_HEIGHT) {
                return traits.get(index);
            }
        }
        return null;
    }

    private static int displayedLevel(CatTraitInstance instance) {
        if (instance.trait().upgradable()) return instance.level();
        return instance.trait().rarity() == CatTraitRarity.DEFECT ? 1 : 7;
    }

    private static String romanLevel(int level) {
        return switch (Math.max(1, Math.min(7, level))) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            default -> "VII";
        };
    }

    private static CatTraitProfile resolved(CatTraitProfile profile) {
        return profile == null ? CatTraitProfile.EMPTY : profile;
    }

    private CatTraitCardRenderer() {}
}
