package cn.laowu.mod.item;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import java.util.List;

/** A summon item as well as a placeable shelf; usable without Create. */
public final class BoguShelfBlockItem extends BlockItem {
 public BoguShelfBlockItem(Block block,Properties properties){super(block,properties);}
 @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){
  lines.add(Component.translatable("item.laowu.round_bogu_shelf.tooltip").withStyle(ChatFormatting.GRAY));
  lines.add(Component.translatable("item.laowu.boss_cat_treat.tooltip.warning").withStyle(ChatFormatting.RED));
 }
}
