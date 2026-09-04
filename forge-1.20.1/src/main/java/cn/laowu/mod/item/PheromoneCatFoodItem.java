package cn.laowu.mod.item;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitData;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Cat food bound to the player name copied from a renamed Name Tag. */
public final class PheromoneCatFoodItem extends Item {
    public static final String OWNER_NAME_TAG = "LaoWuPheromoneOwnerName";

    public PheromoneCatFoodItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createForOwner(String ownerName) {
        ItemStack stack = new ItemStack(LaoWuMod.PHEROMONE_CAT_FOOD.get());
        String normalized = ownerName == null ? "" : ownerName.trim();
        if (!normalized.isEmpty()) stack.getOrCreateTag().putString(OWNER_NAME_TAG, normalized);
        return stack;
    }

    public static Optional<String> ownerName(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(OWNER_NAME_TAG)) {
            return Optional.empty();
        }
        String owner = stack.getTag().getString(OWNER_NAME_TAG).trim();
        return owner.isEmpty() ? Optional.empty() : Optional.of(owner);
    }

    /** Resolves the player encoded by this food for entity and deployer use. */
    public static Optional<UUID> resolveOwner(MinecraftServer server, ItemStack stack) {
        if (server == null) return Optional.empty();
        return ownerName(stack).flatMap(ownerName -> {
            ServerPlayer online = server.getPlayerList().getPlayerByName(ownerName);
            if (online != null) return Optional.of(online.getUUID());
            return server.getProfileCache().get(ownerName).map(GameProfile::getId);
        });
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                   LivingEntity target,
                                                   InteractionHand hand) {
        if (!(target instanceof Cat cat)) return InteractionResult.PASS;
        if (cat.level().isClientSide) return InteractionResult.SUCCESS;

        if (cat.isTame() || cat.getOwnerUUID() != null) {
            player.displayClientMessage(Component.translatable(
                    "item.laowu.pheromone_cat_food.message.already_owned"), true);
            return InteractionResult.FAIL;
        }

        Optional<String> encodedName = ownerName(stack);
        if (encodedName.isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "item.laowu.pheromone_cat_food.message.unbound"), true);
            return InteractionResult.FAIL;
        }

        Optional<UUID> ownerId = resolveOwner(cat.getServer(), stack);
        if (ownerId.isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "item.laowu.pheromone_cat_food.message.unknown_owner",
                    encodedName.get()), true);
            return InteractionResult.FAIL;
        }

        cat.setTame(true);
        cat.setOwnerUUID(ownerId.get());
        cat.setPersistenceRequired();
        cat.setTarget(null);
        cat.setOrderedToSit(false);
        if (cat.isBaby() && !CatTraitData.ensure(cat).has(CatTrait.LOLI)) {
            cat.setAge(0);
        }

        if (!player.getAbilities().instabuild) stack.shrink(1);
        cat.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.15F);
        if (cat.level() instanceof ServerLevel level) {
            level.broadcastEntityEvent(cat, (byte) 7);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    cat.getX(), cat.getY() + cat.getBbHeight() * 0.65D, cat.getZ(),
                    12, 0.35D, 0.35D, 0.35D, 0.05D);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(ownerName(stack)
                .<Component>map(owner -> Component.translatable(
                        "item.laowu.pheromone_cat_food.tooltip.owner", owner)
                        .withStyle(ChatFormatting.AQUA))
                .orElseGet(() -> Component.translatable(
                        "item.laowu.pheromone_cat_food.tooltip.unbound")
                        .withStyle(ChatFormatting.GRAY)));
        tooltip.addAll(TooltipHelper.cutStringTextComponent(
                Component.translatable(
                        "item.laowu.pheromone_cat_food.tooltip.summary").getString(),
                FontHelper.Palette.STANDARD_CREATE));
    }
}
