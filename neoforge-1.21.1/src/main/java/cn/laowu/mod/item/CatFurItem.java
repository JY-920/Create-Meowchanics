package cn.laowu.mod.item;

import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Cat fur carries an always-visible Create-styled acquisition hint. */
public final class CatFurItem extends Item {
    public CatFurItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        String description = Component.translatable(
                "item.laowu.cat_fur.tooltip").getString();
        tooltip.addAll(TooltipHelper.cutStringTextComponent(
                description, FontHelper.Palette.STANDARD_CREATE));
    }
}
