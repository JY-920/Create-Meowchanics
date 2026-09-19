package cn.laowu.mod.item;

import cn.laowu.mod.client.WishAdoptionBoxItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Block item that renders the supplied adoption-box model in every item context. */
public final class WishAdoptionBoxBlockItem extends BlockItem {
    public WishAdoptionBoxBlockItem(Block block, Properties properties) {
        super(block, properties);
    }


    /** The vanilla block-item payload carries only the trading card, never the 18-slot inventory. */
    public static net.minecraft.nbt.CompoundTag cardData(net.minecraft.world.item.ItemStack stack) {
        var data=stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        return data==null?new net.minecraft.nbt.CompoundTag():data.copyTag();
    }
    public static cn.laowu.mod.create.WishAdoptionOffer offer(net.minecraft.world.item.ItemStack stack) {
        return cn.laowu.mod.create.WishAdoptionOffer.load(cardData(stack).getCompound("Offer"));
    }
    public static void writeCard(net.minecraft.world.item.ItemStack stack,
            cn.laowu.mod.create.WishAdoptionOffer offer,boolean locked) {
        var tag=new net.minecraft.nbt.CompoundTag();
        if(offer!=null)tag.put("Offer",offer.save());
        tag.putBoolean("Locked",locked);
        BlockItem.setBlockEntityData(stack,cn.laowu.mod.LaoWuMod.WISH_ADOPTION_BOX_BE.get(),tag);
    }
    /** Server crafting/legacy migration only; tooltip and rendering must remain read-only. */
    public static void ensureOffer(net.minecraft.world.item.ItemStack stack,net.minecraft.util.RandomSource random) {
        if(!(stack.getItem() instanceof WishAdoptionBoxBlockItem))return;
        var data=cardData(stack);
        var existing=cn.laowu.mod.create.WishAdoptionOffer.load(data.getCompound("Offer"));
        var revised=existing==null?cn.laowu.mod.create.WishAdoptionOffer.roll(random,null):existing.normalized(random);
        if(revised!=existing)writeCard(stack,revised,data.getBoolean("Locked"));
    }
    @Override public void onCraftedBy(net.minecraft.world.item.ItemStack stack,
            net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player) {
        if(!level.isClientSide)ensureOffer(stack,level.random);
        super.onCraftedBy(stack,level,player);
    }
    @Override public void onCraftedPostProcess(net.minecraft.world.item.ItemStack stack,net.minecraft.world.level.Level level) {
        if(!level.isClientSide)ensureOffer(stack,level.random);
        super.onCraftedPostProcess(stack,level);
    }
    @Override public void inventoryTick(net.minecraft.world.item.ItemStack stack,net.minecraft.world.level.Level level,
            net.minecraft.world.entity.Entity entity,int slot,boolean selected) {
        if(!level.isClientSide)ensureOffer(stack,level.random); // Old /give or pre-update boxes initialize only once.
        super.inventoryTick(stack,level,entity,slot,selected);
    }
    @Override public net.minecraft.world.InteractionResult place(net.minecraft.world.item.context.BlockPlaceContext context) {
        if(!context.getLevel().isClientSide)ensureOffer(context.getItemInHand(),context.getLevel().random);
        return super.place(context);
    }
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,
            net.minecraft.world.item.Item.TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> tooltip,net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack,context,tooltip,flag);
        var card=offer(stack);
        if(card==null)return;
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.laowu.wish_adoption.reward",
                card.rewardStack().getHoverName()).withStyle(net.minecraft.ChatFormatting.GOLD));
        tooltip.add(net.minecraft.network.chat.Component.literal(card.maximum()?"MAX":"NOW")
                .withStyle(net.minecraft.ChatFormatting.AQUA));
        for(var condition:card.conditions()) {
            var name=net.minecraft.network.chat.Component.translatable("attribute.laowu.cat."+condition.stat().serializedName());
            var line=net.minecraft.network.chat.Component.empty();
            if(condition.min()>=0&&condition.max()>=0)line.append(condition.min()+"≤").append(name).append("≤"+condition.max());
            else if(condition.min()>=0)line.append(name).append("≥"+condition.min());
            else line.append(name).append("≤"+condition.max());
            tooltip.add(line.withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        if(cardData(stack).getBoolean("Locked"))tooltip.add(net.minecraft.network.chat.Component
                .translatable("tooltip.laowu.wish_adoption.locked").withStyle(net.minecraft.ChatFormatting.YELLOW));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft minecraft = Minecraft.getInstance();
                    renderer = new WishAdoptionBoxItemRenderer(
                            minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
                }
                return renderer;
            }
        });
    }
}
