package cn.laowu.mod.item;

import cn.laowu.mod.*;
import net.minecraft.world.item.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

public final class CatLaserPointerItem extends Item {
    public CatLaserPointerItem(Properties p) { super(p); }
    public static void dye(ItemStack stack, DyeColor dye) {
        var data = ItemCustomData.copy(stack);
        data.putInt("LaserDye", dye.getId());
        data.putInt("LaserColor", dye.getTextureDiffuseColor());
        ItemCustomData.set(stack, data);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        var data = ItemCustomData.copy(stack);
        DyeColor dye = DyeColor.RED;
        if (data != null && data.contains("LaserDye")) dye = DyeColor.byId(data.getInt("LaserDye"));
        else if (data != null && data.contains("LaserColor")) {
            for (DyeColor candidate : DyeColor.values())
                if (candidate.getTextureDiffuseColor() == color(stack)) dye = candidate;
        }
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.laowu.laser.color",
                net.minecraft.network.chat.Component.translatable("tooltip.laowu.cat_team." + dye.getName()))
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
    public static int color(ItemStack stack) {
        var tag = cn.laowu.mod.item.ItemCustomData.copy(stack);
        return tag != null && tag.contains("LaserColor") ? tag.getInt("LaserColor") & 0xFFFFFF : 0xFF2020;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (level.isClientSide) cn.laowu.mod.client.CatLaserWheelScreen.open();
            return InteractionResultHolder.consume(stack);
        }
        if (player instanceof ServerPlayer server && !player.getCooldowns().isOnCooldown(this)) {
            var aim = CatLaserTargeting.aim(player);
            int count = CatLaserCommands.issue(server, aim);
            cn.laowu.mod.network.ModNetwork.sendLaserMark(server, count > 0 && aim.target() != null ? aim.target() : null);
            server.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.laowu.laser.order", count), true);
            player.getCooldowns().addCooldown(this, 5);
        }
        return InteractionResultHolder.consume(stack);
    }
    @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        return context.getPlayer() == null ? InteractionResult.PASS
                : use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player p,
            net.minecraft.world.entity.LivingEntity target, InteractionHand hand) {
        return use(p.level(), p, hand).getResult();
    }

    @Override public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    var mc = net.minecraft.client.Minecraft.getInstance();
                    renderer = new cn.laowu.mod.client.CatLaserPointerItemRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                }
                return renderer;
            }
        });
    }
}
