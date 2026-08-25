package cn.laowu.mod.item;

import cn.laowu.mod.genetics.CatGenome;
import cn.laowu.mod.genetics.CatGenomeData;
import cn.laowu.mod.genetics.CatRegion;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatAttributeProfile;
import cn.laowu.mod.genetics.CatBreedingLogic;
import cn.laowu.mod.genetics.CatBreedingMode;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatTraitProfile;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Development tool for simulating one normal-food advanced-box breeding. */
public final class FusionDebugWandItem extends Item {
    private static final String FIRST_UUID = "LaoWuFusionParent1";
    private static final String SECOND_UUID = "LaoWuFusionParent2";
    private static final String FIRST_NAME = "LaoWuFusionParent1Name";
    private static final String SECOND_NAME = "LaoWuFusionParent2Name";

    public FusionDebugWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                   LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Cat cat)) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;

        CompoundTag tag = stack.getOrCreateTag();
        if (player.isShiftKeyDown()) {
            clearSelection(tag);
            player.displayClientMessage(Component.translatable(
                    "message.laowu.fusion_wand.cleared"), true);
            return InteractionResult.CONSUME;
        }

        // Once a pair is complete, selecting another cat starts a new pair.
        if (!tag.hasUUID(FIRST_UUID) || tag.hasUUID(SECOND_UUID)) {
            clearSelection(tag);
            saveParent(tag, FIRST_UUID, FIRST_NAME, cat);
            ensureAndSync(cat);
            player.displayClientMessage(Component.translatable(
                    "message.laowu.fusion_wand.first", cat.getDisplayName()), true);
            markSelected((ServerLevel) player.level(), cat);
            return InteractionResult.CONSUME;
        }

        if (tag.getUUID(FIRST_UUID).equals(cat.getUUID())) {
            player.displayClientMessage(Component.translatable(
                    "message.laowu.fusion_wand.same_parent"), true);
            return InteractionResult.CONSUME;
        }

        saveParent(tag, SECOND_UUID, SECOND_NAME, cat);
        ensureAndSync(cat);
        player.displayClientMessage(Component.translatable(
                "message.laowu.fusion_wand.second", cat.getDisplayName()), true);
        markSelected((ServerLevel) player.level(), cat);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.FAIL;

        ItemStack stack = context.getItemInHand();
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.hasUUID(FIRST_UUID) || !tag.hasUUID(SECOND_UUID)) {
            player.displayClientMessage(Component.translatable(
                    "message.laowu.fusion_wand.need_parents"), true);
            return InteractionResult.FAIL;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        Entity firstEntity = serverLevel.getEntity(tag.getUUID(FIRST_UUID));
        Entity secondEntity = serverLevel.getEntity(tag.getUUID(SECOND_UUID));
        if (!(firstEntity instanceof Cat first) || !first.isAlive()
                || !(secondEntity instanceof Cat second) || !second.isAlive()) {
            player.displayClientMessage(Component.translatable(
                    "message.laowu.fusion_wand.parents_unavailable"), true);
            return InteractionResult.FAIL;
        }

        Cat child = EntityType.CAT.create(serverLevel);
        if (child == null) return InteractionResult.FAIL;

        Registry<CatVariant> variants = serverLevel.registryAccess()
                .registryOrThrow(Registries.CAT_VARIANT);
        RandomSource random = serverLevel.getRandom();
        CatAttributeProfile firstAttributes = CatAttributeData.ensure(first);
        CatAttributeProfile secondAttributes = CatAttributeData.ensure(second);
        CatTraitProfile firstTraits = CatTraitData.ensure(first);
        CatTraitProfile secondTraits = CatTraitData.ensure(second);
        float mutationChance = CatBreedingLogic.effectiveMutationChance(
                0.30F, CatBreedingMode.NORMAL, firstAttributes, firstTraits,
                secondAttributes, secondTraits);
        CatGenome genome = CatGenome.fuse(
                CatGenomeData.ensure(first), CatGenomeData.ensure(second),
                variants.keySet(), mutationChance, random);
        CatAttributeProfile attributes = CatAttributeProfile.breed(
                firstAttributes, secondAttributes, CatBreedingMode.NORMAL,
                mutationChance, random);

        Vec3 spawn = Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace()));
        child.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot() + 180.0F, 0.0F);
        child.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(child.blockPosition()),
                MobSpawnType.TRIGGERED, null, null);
        child.setAge(0);
        child.setPersistenceRequired();
        CatVariant compatibilityVariant = variants.get(genome.material(CatRegion.BODY_FRONT));
        if (compatibilityVariant != null) child.setVariant(compatibilityVariant);
        CatGenomeData.set(child, genome);
        CatAttributeData.set(child, attributes);
        CatTraitData.set(child, CatTraitProfile.breed(
                firstTraits, secondTraits, mutationChance, random));

        if (!serverLevel.addFreshEntity(child)) return InteractionResult.FAIL;
        ModNetwork.syncCatGenomeToTracking(child);
        ModNetwork.syncCatAttributesToTracking(child);
        ModNetwork.syncCatTraitsToTracking(child);
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                child.getX(), child.getY() + 0.5D, child.getZ(),
                24, 0.35D, 0.35D, 0.35D, 0.08D);
        child.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.15F);
        player.displayClientMessage(Component.translatable(
                "message.laowu.fusion_wand.spawned"), true);
        clearSelection(tag);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        tooltip.add(Component.translatable("tooltip.laowu.fusion_wand.usage")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.laowu.fusion_wand.mutation")
                .withStyle(ChatFormatting.DARK_GRAY));
        if (tag != null && tag.hasUUID(FIRST_UUID)) {
            tooltip.add(Component.translatable("tooltip.laowu.fusion_wand.parent1",
                    tag.getString(FIRST_NAME)).withStyle(ChatFormatting.AQUA));
        }
        if (tag != null && tag.hasUUID(SECOND_UUID)) {
            tooltip.add(Component.translatable("tooltip.laowu.fusion_wand.parent2",
                    tag.getString(SECOND_NAME)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private static void ensureAndSync(Cat cat) {
        CatGenomeData.ensure(cat);
        CatAttributeData.ensure(cat);
        ModNetwork.syncCatGenomeToTracking(cat);
        ModNetwork.syncCatAttributesToTracking(cat);
    }

    private static void saveParent(CompoundTag tag, String uuidKey, String nameKey, Cat cat) {
        tag.putUUID(uuidKey, cat.getUUID());
        tag.putString(nameKey, cat.getDisplayName().getString());
    }

    private static void clearSelection(CompoundTag tag) {
        tag.remove(FIRST_UUID);
        tag.remove(SECOND_UUID);
        tag.remove(FIRST_NAME);
        tag.remove(SECOND_NAME);
    }

    private static void markSelected(ServerLevel level, Cat cat) {
        level.sendParticles(ParticleTypes.END_ROD,
                cat.getX(), cat.getY() + 0.5D, cat.getZ(),
                8, 0.2D, 0.25D, 0.2D, 0.02D);
        cat.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, 1.5F);
    }
}
