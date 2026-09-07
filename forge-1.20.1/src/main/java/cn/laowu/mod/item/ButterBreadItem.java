package cn.laowu.mod.item;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.entity.ButterCatBoss;
import cn.laowu.mod.genetics.CatGenomeData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;

/** Converts an untamed vanilla cat into the Butter Cat boss. */
public final class ButterBreadItem extends Item {
    public ButterBreadItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                   LivingEntity target,
                                                   InteractionHand hand) {
        if (!(target instanceof Cat cat) || cat.isTame()) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }

        ButterCatBoss boss = LaoWuMod.BUTTER_CAT.get().create(serverLevel);
        if (boss == null) return InteractionResult.FAIL;

        boss.moveTo(cat.getX(), cat.getY(), cat.getZ(), cat.getYRot(), cat.getXRot());
        boss.setYHeadRot(cat.getYHeadRot());
        boss.yBodyRot = cat.yBodyRot;
        boss.setPersistenceRequired();
        boss.setInheritedGenome(CatGenomeData.getOrFallback(cat));
        boss.beginSummoning();
        if (cat.hasCustomName()) {
            boss.setCustomName(cat.getCustomName());
            boss.setCustomNameVisible(cat.isCustomNameVisible());
        }

        if (!serverLevel.addFreshEntity(boss)) return InteractionResult.FAIL;
        serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, boss.position());
        cat.discard();
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.laowu.butter_bread.tooltip")
                .withStyle(ChatFormatting.GRAY));
    }
}
