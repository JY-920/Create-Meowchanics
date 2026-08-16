package cn.laowu.mod.item;

import cn.laowu.mod.CatClothesData;
import cn.laowu.mod.CatOutfitType;
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

/** Wearable upgrade applied directly to cats by players or Create deployers. */
public final class TerminatorSuitItem extends Item {
    private final CatOutfitType outfit;

    public TerminatorSuitItem(Properties properties, CatOutfitType outfit) {
        super(properties);
        this.outfit = outfit;
    }

    public CatOutfitType outfit() {
        return outfit;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                  LivingEntity target, InteractionHand hand) {
        return tryEquip(stack, player, target, outfit);
    }

    public static InteractionResult tryEquip(ItemStack stack, Player player, LivingEntity target) {
        CatOutfitType type = stack.getItem() instanceof TerminatorSuitItem suit
                ? suit.outfit : CatOutfitType.NONE;
        return tryEquip(stack, player, target, type);
    }

    public static InteractionResult tryEquip(ItemStack stack, Player player, LivingEntity target,
                                             CatOutfitType outfit) {
        if (!(target instanceof Cat cat) || !cat.isTame() || CatClothesData.isEquipped(cat)) {
            return InteractionResult.PASS;
        }
        if (outfit == CatOutfitType.NONE) return InteractionResult.PASS;

        if (!cat.level().isClientSide) {
            CatClothesData.equip(cat, outfit);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            cat.playSound(SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.9F);
            if (cat.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        cat.getX(), cat.getY() + cat.getBbHeight() * 0.55D, cat.getZ(),
                        14, 0.28D, 0.22D, 0.28D, 0.04D);
            }
        }
        return InteractionResult.sidedSuccess(cat.level().isClientSide);
    }
}
