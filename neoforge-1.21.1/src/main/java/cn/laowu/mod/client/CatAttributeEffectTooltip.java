package cn.laowu.mod.client;

import cn.laowu.mod.CareerCatBehavior;
import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.genetics.CatAttributeEffects;
import cn.laowu.mod.genetics.CatStat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Converts one displayed cat stat into the mechanics it currently controls. */
final class CatAttributeEffectTooltip {
    static void render(GuiGraphics graphics, Font font, CatStat stat, int value,
                       boolean limit, CatOutfitType outfit,
                       int mouseX, int mouseY) {
        int resolved = Math.max(0, value);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(limit
                                ? "gui.laowu.cat_stats.effect.limit_title"
                                : "gui.laowu.cat_stats.effect.current_title",
                        Component.translatable("attribute.laowu.cat."
                                + stat.serializedName()), resolved)
                .withStyle(ChatFormatting.AQUA));

        switch (stat) {
            case HEALTH -> {
                var snapshot = CareerCatBehavior.snapshotEffective(
                        outfit, resolved, 0, 0, 0);
                lines.add(line("gui.laowu.cat_stats.effect.max_health",
                        number(snapshot.health())));
            }
            case ATTACK -> {
                if (outfit == CatOutfitType.NONE) {
                    lines.add(Component.translatable(
                                    "gui.laowu.cat_stats.effect.no_career_damage")
                            .withStyle(ChatFormatting.GRAY));
                } else {
                    var snapshot = CareerCatBehavior.snapshotEffective(
                            outfit, 0, resolved, 0, 0);
                    if (snapshot.attacks()) {
                    lines.add(line("gui.laowu.cat_stats.effect.career_damage",
                            number(snapshot.attackDamage())));
                    } else if (outfit == CatOutfitType.TRANSPORT) {
                        lines.add(Component.translatable(
                                        "gui.laowu.cat_stats.effect.support_no_attack")
                                .withStyle(ChatFormatting.GRAY));
                    }
                }
            }
            case STAMINA -> {
                var snapshot = CareerCatBehavior.snapshotEffective(
                        outfit, 0, 0, 0, resolved);
                lines.add(line("gui.laowu.cat_stats.effect.armor",
                        number(snapshot.armor())));
                lines.add(line("gui.laowu.cat_stats.effect.toughness",
                        number(snapshot.toughness())));
            }
            case SPEED -> {
                lines.add(line("gui.laowu.cat_stats.effect.movement",
                        number(CatAttributeEffects.movementMultiplier(resolved))));
                var snapshot = CareerCatBehavior.snapshotEffective(
                        outfit, 0, 0, resolved, 0);
                if (snapshot.attacks()) {
                    int interval = snapshot.attackIntervalTicks();
                    lines.add(Component.translatable(
                                    "gui.laowu.cat_stats.effect.attack_interval",
                                    interval, number(20.0D / interval))
                            .withStyle(ChatFormatting.GRAY));
                }
            }
            case INTELLIGENCE -> lines.add(line(
                    "gui.laowu.cat_stats.effect.training",
                    number(CatAttributeEffects.trainingMultiplier(resolved))));
            case LUCK -> {
                lines.add(line("gui.laowu.cat_stats.effect.critical",
                        number(CatAttributeEffects.criticalChance(resolved) * 100.0D)));
                lines.add(line("gui.laowu.cat_stats.effect.fishing_luck",
                        number(CatAttributeEffects.fishingLootLuck(resolved))));
            }
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private static Component line(String key, String value) {
        return Component.translatable(key, value).withStyle(ChatFormatting.GRAY);
    }

    private static String number(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return Long.toString(Math.round(value));
        }
        String result = String.format(Locale.ROOT, "%.2f", value);
        while (result.endsWith("0")) result = result.substring(0, result.length() - 1);
        return result.endsWith(".") ? result.substring(0, result.length() - 1) : result;
    }

    private CatAttributeEffectTooltip() {}
}

