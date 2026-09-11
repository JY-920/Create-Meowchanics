package cn.laowu.mod.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.DyeColor;
import java.util.List;

public final class KimiArmorDye {
    public static final String TAG = "LaoWuKimiDyes";
    public static int read(ItemStack stack) {
        var root = stack.getTag();
        if (root == null) return 0;
        int raw = root.getInt(TAG), result = 0;
        for (int region = 0; region < 3; region++) {
            int dye = KimiDyePalette.channel(raw, region);
            if (dye >= 0) result = KimiDyePalette.replace(result, region, dye);
        }
        return result;
    }
    public static void write(ItemStack stack, int code) {
        var root = stack.getOrCreateTag();
        if (code == 0) root.remove(TAG); else root.putInt(TAG, code);
        if (root.isEmpty()) stack.setTag(null);
    }
    public static void tooltip(ItemStack stack, List<Component> lines) {
        int code = read(stack);
        boolean helmet = stack.getItem() instanceof KimiArmorItem armor
                && armor.getType() == net.minecraft.world.item.ArmorItem.Type.HELMET;
        for (int region = 0; region < (helmet ? 3 : 2); region++) {
            int dye = KimiDyePalette.channel(code, region);
            lines.add(Component.translatable("tooltip.laowu.kimi_dye.region_" + region,
                    Component.translatable(dye < 0 ? "tooltip.laowu.kimi_dye.default"
                            : "tooltip.laowu.cat_team." + DyeColor.byId(dye).getName()))
                    .withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable(helmet ? "tooltip.laowu.kimi_dye.crafting"
                : "tooltip.laowu.kimi_dye.crafting_body").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("tooltip.laowu.kimi_dye.reset").withStyle(ChatFormatting.DARK_GRAY));
    }
    private KimiArmorDye() {}
}
