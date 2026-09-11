package cn.laowu.mod.item;

import cn.laowu.mod.client.CatCarrierItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Block item that renders the supplied cat-carrier model in every item context. */
public final class CatCarrierBlockItem extends BlockItem {
    public CatCarrierBlockItem(Block block, Properties properties) {
        super(block, properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context,
                                java.util.List<net.minecraft.network.chat.Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        var data = stack.getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        var fluid = data.getCompound("Tank").getCompound("Fluid");
        int amount = "laowu:hissing_gas".equals(fluid.getString("id"))
                ? Math.max(0, Math.min(8000, fluid.getInt("amount"))) : 0;
        var cat = data.getCompound("Cat");
        // The 1.21 ItemStack codec defaults a missing count to one.
        boolean hasCat = "laowu:cat_pancake".equals(cat.getString("id"))
                && (!cat.contains("count") || cat.getInt("count") > 0);
        tooltip.add(net.minecraft.network.chat.Component.translatable(
                "tooltip.laowu.cat_carrier.gas", amount, 8000).withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(net.minecraft.network.chat.Component.translatable(
                hasCat ? "tooltip.laowu.cat_carrier.occupied" : "tooltip.laowu.cat_carrier.empty")
                .withStyle(hasCat ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft minecraft = Minecraft.getInstance();
                    renderer = new CatCarrierItemRenderer(
                            minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
                }
                return renderer;
            }
        });
    }
}
