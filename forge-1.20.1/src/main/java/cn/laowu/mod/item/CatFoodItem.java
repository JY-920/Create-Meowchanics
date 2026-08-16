package cn.laowu.mod.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Food made from a ground cat pancake; feeding it to a kitten grows it instantly. */
public final class CatFoodItem extends Item {
    public CatFoodItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                   LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Cat cat) || !cat.isBaby()) return InteractionResult.PASS;

        if (!cat.level().isClientSide) {
            cat.setAge(0);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            cat.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.15F);
            if (cat.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        cat.getX(), cat.getY() + cat.getBbHeight() * 0.65D, cat.getZ(),
                        12, 0.35D, 0.35D, 0.35D, 0.05D);
            }
        }
        return InteractionResult.sidedSuccess(cat.level().isClientSide);
    }
}
