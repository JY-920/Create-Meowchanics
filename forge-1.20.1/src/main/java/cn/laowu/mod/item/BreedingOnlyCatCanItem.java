package cn.laowu.mod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A recipe material that can also put a tamed adult cat into love mode.
 * Unlike vanilla fish, it never tames, heals, or grows a cat.
 */
public final class BreedingOnlyCatCanItem extends Item {
    public BreedingOnlyCatCanItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                   LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Cat cat)
                || !cat.isTame()
                || !cat.canFallInLove()) {
            return InteractionResult.PASS;
        }

        if (!cat.level().isClientSide) {
            cat.setInLove(player);
            cat.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(cat.level().isClientSide);
    }
}
