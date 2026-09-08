package cn.laowu.mod.item;

import cn.laowu.mod.CatAttributeEditorMenu;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;

import java.util.List;

/** Development-only wand opening a vanilla-widget attribute editor. */
public final class AttributeDebugWandItem extends Item {
    public AttributeDebugWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                   LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Cat cat)) return InteractionResult.PASS;
        return openEditor(player, cat);
    }

    public InteractionResult interactItemEntity(Player player, ItemEntity itemEntity,
                                                 InteractionHand hand) {
        if (!itemEntity.getItem().is(cn.laowu.mod.LaoWuMod.CAT_PANCAKE.get())) {
            return InteractionResult.PASS;
        }
        return openEditor(player, itemEntity);
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemEntity target = findLookedAtPancake(player);
        if (target == null) {
            return net.minecraft.world.InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        InteractionResult result = openEditor(player, target);
        return new net.minecraft.world.InteractionResultHolder<>(
                result, player.getItemInHand(hand));
    }

    private static InteractionResult openEditor(Player player, Entity target) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;

        if (target instanceof Cat cat) {
            CatAttributeData.ensure(cat);
            ModNetwork.syncCatAttributesToTracking(cat);
        } else if (target instanceof ItemEntity itemEntity) {
            ItemStack edited = itemEntity.getItem().copy();
            CatAttributeData.ensure(edited, itemEntity.level().random);
            itemEntity.setItem(edited);
        } else {
            return InteractionResult.PASS;
        }
        serverPlayer.openMenu(
                new SimpleMenuProvider((id, inventory, ignored) ->
                        target instanceof Cat cat
                                ? new CatAttributeEditorMenu(id, inventory, cat)
                                : new CatAttributeEditorMenu(id, inventory,
                                (ItemEntity) target),
                        Component.translatable("screen.laowu.cat_attribute_editor")),
                buffer -> buffer.writeUUID(target.getUUID()));
        return InteractionResult.CONSUME;
    }

    private static ItemEntity findLookedAtPancake(Player player) {
        double reach = 5.0D;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(reach));
        AABB search = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, search,
                entity -> entity instanceof ItemEntity itemEntity
                        && itemEntity.isAlive()
                        && itemEntity.getItem().is(cn.laowu.mod.LaoWuMod.CAT_PANCAKE.get()),
                reach * reach);
        return hit != null && hit.getEntity() instanceof ItemEntity itemEntity
                ? itemEntity : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.laowu.attribute_debug_wand")
                .withStyle(ChatFormatting.GRAY));
    }
}
