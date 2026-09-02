package cn.laowu.mod.item;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatTraitInstance;
import cn.laowu.mod.genetics.CatTraitProfile;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** Fish treats used to level the upgradable traits stored by cats and cat pancakes. */
public final class CatTraitFishItem extends Item {
    private static final double PANCAKE_REACH = 5.0D;

    private final Tier tier;

    public CatTraitFishItem(Properties properties, Tier tier) {
        super(properties);
        this.tier = tier;
    }

    public Tier tier() {
        return tier;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                   LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Cat cat)) return InteractionResult.PASS;
        if (cat.level().isClientSide) return InteractionResult.SUCCESS;

        Optional<CatTraitProfile> upgraded = upgrade(CatTraitData.ensure(cat), cat.getRandom());
        if (upgraded.isEmpty()) {
            showAtLimit(player);
            return InteractionResult.CONSUME;
        }

        CatTraitData.set(cat, upgraded.get());
        ModNetwork.syncCatTraitsToTracking(cat);
        consume(stack, player);
        cat.playSound(SoundEvents.GENERIC_EAT, 1.0F, tier.pitch());
        sendParticles((ServerLevel) cat.level(), cat.getX(),
                cat.getY() + cat.getBbHeight() * 0.65D, cat.getZ());
        return InteractionResult.CONSUME;
    }

    /** Direct-use bridge for dropped pancakes, including pancakes riding Create belts. */
    public InteractionResult interactItemEntity(ItemStack held, Player player,
                                                 ItemEntity itemEntity) {
        if (!itemEntity.getItem().is(LaoWuMod.CAT_PANCAKE.get())) {
            return InteractionResult.PASS;
        }
        if (itemEntity.level().isClientSide) return InteractionResult.SUCCESS;

        ItemStack result = itemEntity.getItem().copy();
        Optional<CatTraitProfile> upgraded = upgrade(
                CatTraitData.ensure(result, itemEntity.level().random),
                itemEntity.level().random);
        if (upgraded.isEmpty()) {
            showAtLimit(player);
            return InteractionResult.CONSUME;
        }

        CatTraitData.set(result, upgraded.get());
        itemEntity.setItem(result);
        consume(held, player);
        itemEntity.level().playSound(null, itemEntity.getX(), itemEntity.getY(),
                itemEntity.getZ(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL,
                1.0F, tier.pitch());
        sendParticles((ServerLevel) itemEntity.level(), itemEntity.getX(),
                itemEntity.getY() + 0.2D, itemEntity.getZ());
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
                                                   InteractionHand hand) {
        ItemEntity pancake = findLookedAtPancake(player);
        if (pancake == null) return super.use(level, player, hand);
        InteractionResult result = interactItemEntity(player.getItemInHand(hand), player, pancake);
        return new InteractionResultHolder<>(result, player.getItemInHand(hand));
    }

    /** Shared by direct feeding and Create's stateful item-application recipe. */
    public Optional<CatTraitProfile> upgrade(CatTraitProfile profile, RandomSource random) {
        List<CatTraitInstance> candidates = profile.traits().stream()
                .filter(instance -> instance.trait().upgradable())
                .filter(instance -> instance.level() < instance.trait().maxLevel())
                .toList();
        if (candidates.isEmpty()) return Optional.empty();

        CatTraitProfile upgraded = profile;
        if (tier == Tier.SUPER) {
            for (CatTraitInstance instance : candidates) {
                upgraded = upgraded.withLevel(instance.trait(), instance.trait().maxLevel());
            }
        } else {
            CatTraitInstance selected = candidates.get(random.nextInt(candidates.size()));
            int targetLevel = tier == Tier.GOLDEN
                    ? selected.trait().maxLevel()
                    : selected.level() + 1;
            upgraded = upgraded.withLevel(selected.trait(), targetLevel);
        }
        return Optional.of(upgraded);
    }

    private void sendParticles(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z,
                tier.particleCount(), 0.3D, 0.25D, 0.3D, 0.04D);
    }

    private static void consume(ItemStack stack, Player player) {
        if (!player.getAbilities().instabuild) stack.shrink(1);
    }

    private static void showAtLimit(Player player) {
        player.displayClientMessage(Component.translatable(
                "item.laowu.cat_trait_fish.at_limit"), true);
    }

    private static ItemEntity findLookedAtPancake(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(PANCAKE_REACH));
        AABB search = player.getBoundingBox()
                .expandTowards(look.scale(PANCAKE_REACH)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, search,
                entity -> entity instanceof ItemEntity itemEntity
                        && itemEntity.isAlive()
                        && itemEntity.getItem().is(LaoWuMod.CAT_PANCAKE.get()),
                PANCAKE_REACH * PANCAKE_REACH);
        return hit != null && hit.getEntity() instanceof ItemEntity itemEntity
                ? itemEntity : null;
    }

    public enum Tier {
        NORMAL(1.05F, 7),
        GOLDEN(1.2F, 12),
        SUPER(1.35F, 18);

        private final float pitch;
        private final int particleCount;

        Tier(float pitch, int particleCount) {
            this.pitch = pitch;
            this.particleCount = particleCount;
        }

        private float pitch() {
            return pitch;
        }

        private int particleCount() {
            return particleCount;
        }
    }
}
