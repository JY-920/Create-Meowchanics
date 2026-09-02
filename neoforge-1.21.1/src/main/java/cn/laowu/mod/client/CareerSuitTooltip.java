package cn.laowu.mod.client;

import cn.laowu.mod.CareerCatBehavior;
import cn.laowu.mod.CatOutfitType;
import com.simibubi.create.foundation.item.ItemDescription;
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
            ItemDescription description = ItemDescription.create(item, PALETTE);
            if (description == null) return;
            // Create prefixes the Shift page with its own Shift hint and one
            // blank line. They no longer describe this custom Ctrl page.
            List<Component> details = description.linesOnShift();
            int firstContentLine = Math.min(2, details.size());
            event.getToolTip().addAll(insertionIndex,
                    details.subList(firstContentLine, details.size()));
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
