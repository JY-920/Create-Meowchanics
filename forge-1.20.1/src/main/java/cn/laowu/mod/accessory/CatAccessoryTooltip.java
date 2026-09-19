package cn.laowu.mod.accessory;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Shared tooltip also covers third-party/KubeJS items recognized by synced definitions. */
public final class CatAccessoryTooltip {
    public static void append(ItemStack stack, List<Component> lines) {
        CatAccessoryDefinition def = CatAccessoryRegistry.find(stack, true);
        if (def == null) return;
        List<Component> details = new ArrayList<>();
        appendDetails(stack, def, details);
        Component marker = Component.translatable("cat_accessory.laowu.label").withStyle(ChatFormatting.GOLD);
        details.add(marker);
        // Keep the complete accessory block below the name, before mod IDs, NBT and tag footers.
        int afterName = Math.min(1, lines.size());
        lines.subList(afterName, lines.size()).removeIf(line -> line.getString().equals(marker.getString()));
        lines.addAll(afterName, details);
    }

    private static void appendDetails(ItemStack stack, CatAccessoryDefinition def, List<Component> lines) {
        if (!def.enabled()) {
            lines.add(Component.translatable("cat_accessory.laowu.disabled").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (def.id().equals("laowu:cat_butter_cube"))
            lines.add(Component.translatable("cat_accessory.laowu.boss_drop").withStyle(ChatFormatting.DARK_PURPLE));
        if (!def.description().isEmpty()) lines.add(Component.literal(def.description()).withStyle(ChatFormatting.GRAY));
        if(stack.getMaxDamage()>0)lines.add(Component.translatable("cat_accessory.laowu.durability",
                Math.max(0,stack.getMaxDamage()-stack.getDamageValue()),stack.getMaxDamage()).withStyle(ChatFormatting.GRAY));
        if (def.charge().capacity() > 0) {
            var data = CatAccessoryStackData.read(stack);
            int current = data.contains("Charge", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)
                    ? def.charge().clamp(data.getInt("Charge")) : def.charge().initial();
            lines.add(Component.translatable("cat_accessory.laowu.charge", current, def.charge().capacity())
                    .withStyle(ChatFormatting.AQUA));
        }
        if (!def.requiredOutfit().equals("any")) lines.add(Component.translatable("cat_accessory.laowu.requires",
                Component.translatable("cat_accessory.laowu.outfit." + def.requiredOutfit())).withStyle(ChatFormatting.GRAY));
        def.effects().forEach((key, amount) -> {
            if (amount == 0) return;
            String effect = key;
            String value = format(amount);
            if (CatAccessoryDefinition.STATS.contains(key)) value = (amount > 0 ? "+" : "") + value;
            else if (key.equals("aggro_bias")) effect += amount > 0 ? ".positive" : ".negative";
            else if (key.equals("extra_strike_chance") || key.equals("pilot_dodge_per_speed")
                    || key.equals("moving_damage_reduction") || key.equals("medical_guard")
                    || key.equals("music_movement_bonus")) value += "%";
            lines.add(Component.translatable("cat_accessory.laowu.effect." + effect, value)
                    .withStyle(ChatFormatting.GRAY));
        });
    }
    private static String format(double number) { return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString(); }
    private CatAccessoryTooltip() {}
}
