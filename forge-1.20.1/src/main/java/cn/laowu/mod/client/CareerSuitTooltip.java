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
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.Locale;

/** Client-only key handling and computed combat previews for career suits. */
public final class CareerSuitTooltip {
    private static final FontHelper.Palette PALETTE =
            FontHelper.Palette.STANDARD_CREATE;

    public static void modify(ItemTooltipEvent event, Item item,
                              CatOutfitType outfit) {
        modify(event, item, outfit, Screen.hasShiftDown(), Screen.hasControlDown());
    }

    /** Same real tooltip builder with explicit keys for deterministic client regression coverage. */
    static void modify(ItemTooltipEvent event, Item item, CatOutfitType outfit,
                       boolean shiftDown, boolean controlDown) {
        int insertionIndex = Math.min(1, event.getToolTip().size());
        if (outfit.isPreviewOnly()) {
            event.getToolTip().addAll(insertionIndex, TooltipHelper.cutStringTextComponent(
                    Component.translatable(item.getDescriptionId() + ".tooltip.summary").getString(), PALETTE));
            return;
        }
        if (shiftDown) {
            insertionIndex = addSnapshot(event.getToolTip(), insertionIndex,
                    outfit, 50);
            event.getToolTip().add(insertionIndex++, Component.empty());
            addSnapshot(event.getToolTip(), insertionIndex, outfit, 100);
            return;
        }

        if (controlDown) {
            CatSuitSettings settings = CatSuitSettings.current(outfit);
            String coefficient = format(cn.laowu.mod.ServerConfig.careerDamageCoefficient(outfit));
            if (!outfit.isSupport()) event.getToolTip().add(insertionIndex++, Component.translatable(
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
                if (section == 1 && !outfit.isSupport())
                    detail = Component.translatable("item.laowu.career_suit.damage_detail",
                            detail.replace("_K", "_" + coefficient)).getString();
                if (section == 3) detail = bonusDescription(settings);
                List<Component> lines = TooltipHelper.cutStringTextComponent(
                        detail, PALETTE.primary(), PALETTE.highlight(), 1);
                event.getToolTip().addAll(insertionIndex, lines);
                insertionIndex += lines.size();
                if (section == 1 && !outfit.isSupport()) {
                    String interval = Component.translatable("item.laowu.career_suit.configured_interval",
                            format(settings.value(CatSuitSetting.MIN_INTERVAL)),
                            format(settings.value(CatSuitSetting.INTERVAL_BASE)),
                            format(settings.value(CatSuitSetting.INTERVAL_PER_SPEED))).getString();
                    var intervalLines = TooltipHelper.cutStringTextComponent(interval, PALETTE.primary(), PALETTE.highlight(), 1);
                    event.getToolTip().addAll(insertionIndex, intervalLines);
                    insertionIndex += intervalLines.size();
                }
            }
            return;
        }

        event.getToolTip().add(insertionIndex++, Component.translatable(
                "item.laowu.career_suit.role",
                Component.translatable("item.laowu.career_suit.role." + outfit.role().name().toLowerCase(Locale.ROOT)))
                .withStyle(ChatFormatting.GRAY));
        for (String section : List.of("combat", "work")) {
            String description = Component.translatable("item.laowu.career_suit." + section,
                    Component.translatable(item.getDescriptionId() + ".tooltip." + section)).getString();
            var lines = TooltipHelper.cutStringTextComponent(description, PALETTE);
            event.getToolTip().addAll(insertionIndex, lines);
            insertionIndex += lines.size();
        }
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
        if (settings.outfit().isSupport()) entries.add(Component.translatable("item.laowu.career_suit.support_movement").getString());
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
        index = addValue(tooltip, index, "speed", attackSpeed);
        if (outfit == CatOutfitType.MEDICAL) {
            double intelligence = cn.laowu.mod.ServerConfig.scale(cn.laowu.mod.genetics.CatStat.INTELLIGENCE,
                    CatSuitSettings.current(outfit).attribute(attributeValue, cn.laowu.mod.genetics.CatStat.INTELLIGENCE));
            index = addValue(tooltip, index, "healing", format(cn.laowu.mod.CatSupportRules.healingPerSecond(intelligence)));
            index = addValue(tooltip, index, "healing_radius", format(cn.laowu.mod.CatSupportRules.medicalRadius(intelligence)));
        }
        if (outfit == CatOutfitType.MUSIC) {
            var settings = CatSuitSettings.current(outfit);
            double speed = cn.laowu.mod.ServerConfig.scale(cn.laowu.mod.genetics.CatStat.SPEED,
                    settings.attribute(attributeValue, cn.laowu.mod.genetics.CatStat.SPEED));
            double intelligence = cn.laowu.mod.ServerConfig.scale(cn.laowu.mod.genetics.CatStat.INTELLIGENCE,
                    settings.attribute(attributeValue, cn.laowu.mod.genetics.CatStat.INTELLIGENCE));
            index = addValue(tooltip, index, "music_haste", format(cn.laowu.mod.CatMusicRules.haste(speed) * 100));
            index = addValue(tooltip, index, "music_radius", format(cn.laowu.mod.CatMusicRules.radius(intelligence)));
        }
        if (outfit == CatOutfitType.AGENT) {
            int intelligence = CatSuitSettings.current(outfit).attribute(attributeValue,
                    cn.laowu.mod.genetics.CatStat.INTELLIGENCE);
            index = addValue(tooltip, index, "watch_radius", format(cn.laowu.mod.CatAgentWatch.radius(intelligence)));
        }
        if (outfit == CatOutfitType.FLIGHT) {
            var settings = CatSuitSettings.current(outfit);
            double stamina = cn.laowu.mod.ServerConfig.scale(cn.laowu.mod.genetics.CatStat.STAMINA,
                    (int)Math.round(attributeValue + settings.value(CatSuitSetting.STAMINA_STAT)));
            double speed = cn.laowu.mod.ServerConfig.scale(cn.laowu.mod.genetics.CatStat.SPEED,
                    (int)Math.round(attributeValue + settings.value(CatSuitSetting.SPEED_STAT)));
            index = addValue(tooltip, index, "flight_time", format(cn.laowu.mod.CatPilotFlightRules.durationTicks(stamina) / 20.0));
            index = addValue(tooltip, index, "flight_speed", format(cn.laowu.mod.CatPilotFlightRules.speedPerTick(speed) * 20));
        }
        return index;
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
