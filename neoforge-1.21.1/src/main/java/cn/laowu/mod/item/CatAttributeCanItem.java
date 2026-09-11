package cn.laowu.mod.item;

import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatAttributeProfile;
import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Trains one base attribute; super cans raise its base and ceiling to at least 90. */
public final class CatAttributeCanItem extends Item {
    private static final int SUPER_TARGET_VALUE = 90;
    private final CatStat stat;
    private final Tier tier;

    public CatAttributeCanItem(Properties properties, CatStat stat, Tier tier) {
        super(properties);
        this.stat = stat;
        this.tier = tier;
    }

    public CatStat stat() { return stat; }

    public Tier tier() { return tier; }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                   LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Cat cat)) return InteractionResult.PASS;
        if (cat.level().isClientSide) return InteractionResult.SUCCESS;
        CatAttributeProfile profile = CatAttributeData.ensure(cat);
        Optional<CatAttributeProfile> trained = train(profile);
        if (trained.isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "item.laowu.cat_attribute_can.at_limit"), true);
            return InteractionResult.CONSUME;
        }
        CatAttributeData.set(cat, trained.get());
        ModNetwork.syncCatAttributesToTracking(cat);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        cat.playSound(SoundEvents.GENERIC_EAT, 1.0F,
                tier == Tier.SUPER ? 1.35F : tier == Tier.GOLDEN ? 1.2F : 1.05F);
        if (cat.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    cat.getX(), cat.getY() + cat.getBbHeight() * 0.65D, cat.getZ(),
                    tier == Tier.SUPER ? 18 : tier == Tier.GOLDEN ? 12 : 7,
                    0.3D, 0.3D, 0.3D, 0.04D);
        }
        return InteractionResult.CONSUME;
    }

    /** Shared by direct cat interaction and Create item-application recipes. */
    public Optional<CatAttributeProfile> train(CatAttributeProfile profile) {
        int current = profile.current(stat);
        int ceiling = profile.potential(stat);
        if (tier == Tier.SUPER) {
            int trainedCurrent = Math.max(current, SUPER_TARGET_VALUE);
            int trainedCeiling = Math.max(ceiling, SUPER_TARGET_VALUE);
            return current == trainedCurrent && ceiling == trainedCeiling
                    ? Optional.empty()
                    : Optional.of(profile.withValues(stat, trainedCurrent, trainedCeiling));
        }
        int trained = tier.targetValue(current, ceiling);
        return trained <= current ? Optional.empty()
                : Optional.of(profile.withValues(stat, trained, ceiling));
    }

    public enum Tier {
        NORMAL(1), GOLDEN(10), SUPER(Integer.MAX_VALUE);

        private final int increase;

        Tier(int increase) { this.increase = increase; }

        private int targetValue(int current, int ceiling) {
            if (this == SUPER) return Math.max(current, SUPER_TARGET_VALUE);
            return Math.min(ceiling, current + increase);
        }
    }
}
