package cn.laowu.mod.client;

import cn.laowu.mod.CareerCatBehavior;
import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.CatSuitSetting;
import cn.laowu.mod.CatSuitSettings;
import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.Locale;

/** Client-only key handling and computed combat previews for career suits. */
public final class CareerSuitTooltip {
    private static final FontHelper.Palette PALETTE =
            FontHelper.Palette.STANDARD_CREATE;

    public static void modify(ItemTooltipEvent event, Item item,
                              CatOutfitType outfit) {
        int insertionIndex = Math.min(1, event.getToolTip().size());
        if (Screen.hasShiftDown()) {
            insertionIndex = addSnapshot(event.getToolTip(), insertionIndex,
                    outfit, 50);
            event.getToolTip().add(insertionIndex++, Component.empty());
            addSnapshot(event.getToolTip(), insertionIndex, outfit, 100);
            return;
        }

        if (Screen.hasControlDown()) {
            CatSuitSettings settings = CatSuitSettings.current(outfit);
            String coefficient = format(cn.laowu.mod.ServerConfig.careerDamageCoefficient(outfit));
            event.getToolTip().add(insertionIndex++, Component.translatable(
                    "item.laowu.career_suit.configured_damage",
                    coefficient).withStyle(ChatFormatting.GOLD));
            event.getToolTip().add(insertionIndex++, Component.translatable(
                    "screen.laowu.world.formula_note").withStyle(ChatFormatting.GRAY));
            // Rebuild from the same localized sections so the formula shows
            // the live K, instead of Create caching the original percentage.
            for (int section = 1; section <= 3; section++) {
                if (section > 1) event.getToolTip().add(insertionIndex++, Component.empty());
                event.getToolTip().add(insertionIndex++, Component.translatable(
                        item.getDescriptionId() + ".tooltip.condition" + section).withStyle(ChatFormatting.GRAY));
                String detail = Component.translatable(
                        item.getDescriptionId() + ".tooltip.behaviour" + section).getString();
                if (section == 1 && outfit != CatOutfitType.TRANSPORT)
                    detail = detail.replace("_K", "_" + coefficient);
                if (section == 2 && outfit != CatOutfitType.TRANSPORT)
                    detail = Component.translatable("item.laowu.career_suit.configured_interval",
                            format(settings.value(CatSuitSetting.MIN_INTERVAL)),
                            format(settings.value(CatSuitSetting.INTERVAL_BASE)),
                            format(settings.value(CatSuitSetting.INTERVAL_PER_SPEED))).getString();
                if (section == 3) detail = bonusDescription(settings);
                List<Component> lines = TooltipHelper.cutStringTextComponent(
                        detail, PALETTE.primary(), PALETTE.highlight(), 1);
                event.getToolTip().addAll(insertionIndex, lines);
                insertionIndex += lines.size();
            }
            return;
        }

        String summary = Component.translatable(
                item.getDescriptionId() + ".tooltip.summary").getString();
        List<Component> intro = TooltipHelper.cutStringTextComponent(summary, PALETTE);
        event.getToolTip().addAll(insertionIndex, intro);
        insertionIndex += intro.size();
        event.getToolTip().add(insertionIndex++, Component.empty());
        List<Component> ctrlHint = TooltipHelper.cutStringTextComponent(
                Component.translatable(
                        "item.laowu.career_suit.hold_ctrl").getString(), PALETTE);
        event.getToolTip().addAll(insertionIndex, ctrlHint);
        insertionIndex += ctrlHint.size();
        event.getToolTip().addAll(insertionIndex,
                TooltipHelper.cutStringTextComponent(Component.translatable(
                        "item.laowu.career_suit.hold_shift").getString(), PALETTE));
    }

    private static String bonusDescription(CatSuitSettings settings) {
        var entries = new java.util.ArrayList<String>();
        for (CatSuitSetting setting : CatSuitSetting.values()) {
            if (setting.isAttackSetting() || settings.value(setting) == 0) continue;
            entries.add(Component.translatable("item.laowu.career_suit.configured_bonus",
                    Component.translatable("screen.laowu.world.suit_setting." + setting.id()),
                    format(settings.value(setting))).getString());
        }
        return entries.isEmpty() ? Component.translatable("item.laowu.career_suit.no_bonus").getString()
                : String.join("; ", entries);
    }

    private static int addSnapshot(List<Component> tooltip, int index,
                                   CatOutfitType outfit, int attributeValue) {
        CareerCatBehavior.CareerSnapshot snapshot =
                CareerCatBehavior.snapshot(outfit, attributeValue);
        tooltip.add(index++, Component.translatable(
                        "item.laowu.career_suit.snapshot_title", attributeValue)
                .withStyle(ChatFormatting.GOLD));
        index = addValue(tooltip, index, "health", format(snapshot.health()));
        index = addValue(tooltip, index, "armor", format(snapshot.armor()));
        index = addValue(tooltip, index, "toughness",
                format(snapshot.toughness()));
        index = addValue(tooltip, index, "damage",
                snapshot.attacks() ? format(snapshot.attackDamage())
                        : Component.translatable(
                                "item.laowu.career_suit.no_attack").getString());
        String attackSpeed = snapshot.attacks()
                ? Component.translatable("item.laowu.career_suit.attack_rate",
                        snapshot.attackIntervalTicks(),
                        format(20.0D / snapshot.attackIntervalTicks())).getString()
                : Component.translatable(
                        "item.laowu.career_suit.no_attack").getString();
        return addValue(tooltip, index, "speed", attackSpeed);
    }

    private static int addValue(List<Component> tooltip, int index,
                                String key, String value) {
        String line = Component.translatable(
                "item.laowu.career_suit.snapshot." + key, value).getString();
        List<Component> wrapped = TooltipHelper.cutStringTextComponent(
                line, PALETTE.primary(), PALETTE.highlight(), 1);
        tooltip.addAll(index, wrapped);
        return index + wrapped.size();
    }

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-6D) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    private CareerSuitTooltip() {
    }
}
