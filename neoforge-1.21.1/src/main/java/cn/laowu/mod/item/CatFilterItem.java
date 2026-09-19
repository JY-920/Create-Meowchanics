package cn.laowu.mod.item;

import cn.laowu.mod.CatFilterMenu;
import cn.laowu.mod.genetics.CatStat;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

/** Configurable Create filter for cat attributes, traits and captured identity. */
public final class CatFilterItem extends FilterItem {
    public CatFilterItem(Properties properties) {
        super(properties);
    }

    /** Reset all predicates, then copy this moment's server offer; no link to future rerolls. */
    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof cn.laowu.mod.create.WishAdoptionBoxBlockEntity box))
            return super.useOn(context);
        var player = context.getPlayer();
        if (player == null) return net.minecraft.world.InteractionResult.FAIL;
        if (level.isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
        ItemStack stack = context.getItemInHand();
        if (!player.isAlive() || player.isSpectator() || stack.getItem() != this
                || player.getItemInHand(context.getHand()) != stack
                || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) > 64)
            return net.minecraft.world.InteractionResult.FAIL;
        // A menu may later save a stale draft over the newly imported filter.
        if (player.containerMenu instanceof CatFilterMenu) return net.minecraft.world.InteractionResult.FAIL;
        var eye = player.getEyePosition();
        var sight = level.clip(new net.minecraft.world.level.ClipContext(eye,
                net.minecraft.world.phys.Vec3.atCenterOf(pos),
                net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        if (sight.getType() != net.minecraft.world.phys.HitResult.Type.MISS && !sight.getBlockPos().equals(pos))
            return net.minecraft.world.InteractionResult.FAIL;
        box.ensureOffer();
        var offer = box.offer();
        if (offer == null) {
            player.displayClientMessage(Component.translatable("message.laowu.cat_filter.no_offer"), true);
            return net.minecraft.world.InteractionResult.CONSUME;
        }

        int[] currentMin = new int[CatFilterRules.STAT_COUNT], currentMax = new int[CatFilterRules.STAT_COUNT];
        int[] limitMin = new int[CatFilterRules.STAT_COUNT], limitMax = new int[CatFilterRules.STAT_COUNT];
        java.util.Arrays.fill(currentMax, CatFilterRules.MAX_CURRENT_VALUE);
        java.util.Arrays.fill(limitMax, CatFilterRules.MAX_POTENTIAL_VALUE);
        int mask = 0;
        for (var condition : offer.conditions()) {
            int index = condition.stat().ordinal();
            mask |= 1 << index;
            int[] minimum = offer.maximum() ? limitMin : currentMin;
            int[] maximum = offer.maximum() ? limitMax : currentMax;
            if (condition.min() >= 0) minimum[index] = condition.min();
            if (condition.max() >= 0) maximum[index] = condition.max();
        }
        // Fresh rules clear both pages, trait/identity/name predicates and every AND/OR/inversion flag.
        var rules = CatFilterRules.fromValues(currentMin, currentMax, limitMin, limitMax, List.of())
                .withLogic(new CatFilterLogic(offer.maximum() ? 0 : mask, offer.maximum() ? mask : 0, 0))
                .withBaseCurrent(!offer.maximum());
        rules.write(stack); // Replace only the filter settings; preserve count and the item's custom name.
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.displayClientMessage(Component.translatable("message.laowu.cat_filter.wish_imported",
                offer.maximum() ? "MAX" : "NOW", offer.conditions().size()), true);
        return net.minecraft.world.InteractionResult.CONSUME;
    }

    @Override
    public List<Component> makeSummary(ItemStack stack) {
        CatFilterRules rules = CatFilterRules.read(stack);
        if (rules.isDefault()) {
            return List.of(Component.translatable("item.laowu.cat_filter.summary.default")
                    .withStyle(ChatFormatting.GRAY));
        }

        return List.of(CatFilterDescription.describe(rules).copy().withStyle(ChatFormatting.GRAY));
    }

    /** Create normally hides a configured filter's summary while Shift is held. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        List<Component> summary = makeSummary(stack);
        if (summary.isEmpty()) return;
        tooltip.add(CommonComponents.SPACE);
        tooltip.addAll(summary);
        tooltip.add(Component.translatable("item.laowu.cat_filter.wish_import").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory,
                                            Player player) {
        return new CatFilterMenu(containerId, inventory, player.getMainHandItem());
    }

    @Override
    public FilterItemStack makeStackWrapper(ItemStack stack) {
        return new CatFilterItemStack(stack);
    }

    @Override
    public DataComponentType<?> getComponentType() {
        // The filter's ranges and required traits live in the legacy custom
        // data component, so item-copying recipes preserve them through it.
        return DataComponents.CUSTOM_DATA;
    }

    @Override
    public ItemStack[] getFilterItems(ItemStack stack) {
        return new ItemStack[]{CatPancakeItem.defaultDisplayStack()};
    }
}
