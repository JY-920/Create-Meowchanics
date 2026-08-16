package cn.laowu.mod.item;

import cn.laowu.mod.CatPoseData;
import cn.laowu.mod.CatPancakeBehavior;
import cn.laowu.mod.CatClothesData;
import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.entity.CatPancakeProjectile;
import cn.laowu.mod.network.ModNetwork;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

public final class CatPancakeItem extends Item {
    public static final ResourceLocation DEFAULT_VARIANT = CatVariant.RED.location();
    public static final String CAT_DATA_TAG = "LaoWuCatData";
    public static final String CAT_TEXTURE_TAG = "LaoWuCatTexture";
    public static final String CAT_VARIANT_TAG = "LaoWuCatVariant";
    public static final String BABY_TAG = "LaoWuBabyPancake";
    private static final String TAMED_TAG = "LaoWuTamedPancake";
    private static final String FAN_TICKS_TAG = "LaoWuCatPancakeFanTicks";
    private static final int FAN_CHECK_INTERVAL = 5;
    private static final int REQUIRED_FAN_TICKS = 30;
    private static final int FAN_SEARCH_RADIUS = 16;
    private static final int MAX_CHARGE_TICKS = 80;

    public CatPancakeItem(Properties properties) {
        super(properties);
    }

    public static ItemStack capture(Cat cat) {
        ItemStack pancake = new ItemStack(LaoWuMod.CAT_PANCAKE.get());
        CompoundTag root = new CompoundTag();
        CompoundTag catData = cat.saveWithoutId(new CompoundTag());
        removePositionAndIdentity(catData);
        CatPancakeBehavior.sanitizeCapturedData(catData);
        clearTransientState(catData.getCompound("NeoForgeData"));
        root.put(CAT_DATA_TAG, catData);
        root.putBoolean(TAMED_TAG, cat.isTame());
        if (cat.isBaby()) root.putBoolean(BABY_TAG, true);
        CatOutfitType outfit = CatClothesData.getOutfit(cat);
        if (outfit != CatOutfitType.NONE) {
            root.putBoolean(CatClothesData.EQUIPPED_TAG, true);
            root.putString(CatClothesData.OUTFIT_TAG, outfit.id());
        }

        cat.getVariant().unwrapKey()
                .ifPresent(variant -> root.putString(CAT_VARIANT_TAG, variant.location().toString()));
        root.putString(CAT_TEXTURE_TAG, cat.getVariant().value().texture().toString());
        ItemCustomData.set(pancake, root);
        if (cat.hasCustomName()) {
            pancake.set(DataComponents.CUSTOM_NAME,
                    Component.translatable("item.laowu.cat_pancake.named", cat.getDisplayName()));
        }
        return pancake;
    }

    /** Captures a killed profession cat in a state that can later be restored alive. */
    public static ItemStack captureDeathDrop(Cat cat) {
        ItemStack pancake = capture(cat);
        CompoundTag root = ItemCustomData.copy(pancake);
        if (root.contains(CAT_DATA_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag catData = root.getCompound(CAT_DATA_TAG);
            catData.putFloat("Health", Math.max(1.0F, cat.getMaxHealth()));
            catData.remove("DeathTime");
            catData.remove("HurtTime");
            catData.remove("HurtByTimestamp");
            root.put(CAT_DATA_TAG, catData);
            ItemCustomData.set(pancake, root);
        }
        return pancake;
    }

    public static ResourceLocation texture(ItemStack stack) {
        String value = ItemCustomData.copy(stack).getString(CAT_TEXTURE_TAG);
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        CatVariant fallback = BuiltInRegistries.CAT_VARIANT.get(CatVariant.RED);
        return parsed != null ? parsed : fallback.texture();
    }

    /** Stable orange-cat representative used by the creative tab and recipe viewers. */
    public static ItemStack defaultDisplayStack() {
        return variantStack(DEFAULT_VARIANT);
    }

    /** Stable orange kitten representative used by JEI for the spout recipe. */
    public static ItemStack defaultBabyDisplayStack() {
        return babyVariantStack(DEFAULT_VARIANT);
    }

    /** JEI intentionally shows one stable skin while recipes still match every cat variant. */
    public static List<ItemStack> jeiDisplayStacks() {
        return List.of(defaultDisplayStack());
    }

    /** Concrete component-bearing stacks used when all registered cat variants are needed. */
    public static List<ItemStack> allVariantStacks() {
        List<ItemStack> result = new ArrayList<>();
        BuiltInRegistries.CAT_VARIANT.entrySet().forEach(entry -> {
            ItemStack stack = new ItemStack(LaoWuMod.CAT_PANCAKE.get());
            ResourceLocation id = entry.getKey().location();
            ItemCustomData.update(stack, tag -> {
                tag.putString(CAT_VARIANT_TAG, id.toString());
                tag.putString(CAT_TEXTURE_TAG, entry.getValue().texture().toString());
            });
            result.add(stack);
        });
        return List.copyOf(result);
    }

    public static ItemStack variantStack(ResourceLocation variantId) {
        var variant = BuiltInRegistries.CAT_VARIANT.getHolder(variantId)
                .orElseGet(() -> BuiltInRegistries.CAT_VARIANT.getHolderOrThrow(CatVariant.RED));
        ItemStack stack = new ItemStack(LaoWuMod.CAT_PANCAKE.get());
        ItemCustomData.update(stack, tag -> {
            tag.putString(CAT_VARIANT_TAG, variant.unwrapKey()
                    .map(key -> key.location().toString()).orElse(DEFAULT_VARIANT.toString()));
            tag.putString(CAT_TEXTURE_TAG, variant.value().texture().toString());
        });
        return stack;
    }

    public static ItemStack babyVariantStack(ResourceLocation variantId) {
        ItemStack stack = variantStack(variantId);
        ItemCustomData.update(stack, tag -> tag.putBoolean(BABY_TAG, true));
        return stack;
    }

    public static boolean isBaby(ItemStack stack) {
        CompoundTag root = ItemCustomData.copy(stack);
        if (root.getBoolean(BABY_TAG)) return true;
        return root.contains(CAT_DATA_TAG, Tag.TAG_COMPOUND)
                && root.getCompound(CAT_DATA_TAG).getInt("Age") < 0;
    }

    public static ResourceLocation variantId(ItemStack stack) {
        CompoundTag root = ItemCustomData.copy(stack);
        ResourceLocation saved = ResourceLocation.tryParse(root.getString(CAT_VARIANT_TAG));
        if (saved != null && BuiltInRegistries.CAT_VARIANT.containsKey(saved)) return saved;
        ResourceLocation renderedTexture = texture(stack);
        for (var entry : BuiltInRegistries.CAT_VARIANT.entrySet()) {
            if (entry.getValue().texture().equals(renderedTexture)) return entry.getKey().location();
        }
        return DEFAULT_VARIANT;
    }

    public static ItemStack recipeDisplayStack(ResourceLocation variantId, boolean terminator) {
        return recipeDisplayStack(variantId,
                terminator ? CatOutfitType.TERMINATOR : CatOutfitType.NONE);
    }

    public static ItemStack recipeDisplayStack(ResourceLocation variantId, CatOutfitType outfit) {
        ItemStack stack = variantStack(variantId);
        ItemCustomData.update(stack, tag -> tag.putBoolean(TAMED_TAG, true));
        if (outfit != CatOutfitType.NONE) equipOutfit(stack, outfit);
        return stack;
    }

    public static boolean hasTerminatorSuit(ItemStack stack) {
        return getOutfit(stack) == CatOutfitType.TERMINATOR;
    }

    public static boolean hasOutfit(ItemStack stack) {
        return getOutfit(stack) != CatOutfitType.NONE;
    }

    public static CatOutfitType getOutfit(ItemStack stack) {
        CompoundTag root = ItemCustomData.copy(stack);
        CatOutfitType direct = CatOutfitType.byId(root.getString(CatClothesData.OUTFIT_TAG));
        if (direct != CatOutfitType.NONE) return direct;
        if (root.getBoolean(CatClothesData.EQUIPPED_TAG)) return CatOutfitType.TERMINATOR;
        if (!root.contains(CAT_DATA_TAG, Tag.TAG_COMPOUND)) return CatOutfitType.NONE;
        CompoundTag catData = root.getCompound(CAT_DATA_TAG);
        if (!catData.contains("NeoForgeData", Tag.TAG_COMPOUND)) return CatOutfitType.NONE;
        CompoundTag neoData = catData.getCompound("NeoForgeData");
        CatOutfitType nested = CatOutfitType.byId(neoData.getString(CatClothesData.OUTFIT_TAG));
        if (nested != CatOutfitType.NONE) return nested;
        return neoData.getBoolean(CatClothesData.EQUIPPED_TAG)
                ? CatOutfitType.TERMINATOR : CatOutfitType.NONE;
    }

    public static boolean isTamed(ItemStack stack) {
        CompoundTag root = ItemCustomData.copy(stack);
        if (root.getBoolean(TAMED_TAG)) return true;
        if (!root.contains(CAT_DATA_TAG, Tag.TAG_COMPOUND)) return false;
        CompoundTag catData = root.getCompound(CAT_DATA_TAG);
        return catData.hasUUID("Owner")
                || catData.contains("OwnerUUID", Tag.TAG_STRING)
                && !catData.getString("OwnerUUID").isBlank();
    }

    public static void equipTerminatorSuit(ItemStack stack) {
        equipOutfit(stack, CatOutfitType.TERMINATOR);
    }

    public static void equipOutfit(ItemStack stack, CatOutfitType outfit) {
        if (outfit == CatOutfitType.NONE) return;
        ItemCustomData.update(stack, root -> {
            root.putBoolean(CatClothesData.EQUIPPED_TAG, true);
            root.putString(CatClothesData.OUTFIT_TAG, outfit.id());
            if (root.contains(CAT_DATA_TAG, Tag.TAG_COMPOUND)) {
                CompoundTag catData = root.getCompound(CAT_DATA_TAG);
                CompoundTag neoData = catData.contains("NeoForgeData", Tag.TAG_COMPOUND)
                        ? catData.getCompound("NeoForgeData") : new CompoundTag();
                neoData.putBoolean(CatClothesData.EQUIPPED_TAG, true);
                neoData.putString(CatClothesData.OUTFIT_TAG, outfit.id());
                catData.put("NeoForgeData", neoData);
                root.put(CAT_DATA_TAG, catData);
            }
        });
    }

    public static void removeTerminatorSuit(ItemStack stack) {
        removeOutfit(stack);
    }

    public static void removeOutfit(ItemStack stack) {
        ItemCustomData.update(stack, root -> {
            root.remove(CatClothesData.EQUIPPED_TAG);
            root.remove(CatClothesData.OUTFIT_TAG);
            if (!root.contains(CAT_DATA_TAG, Tag.TAG_COMPOUND)) return;
            CompoundTag catData = root.getCompound(CAT_DATA_TAG);
            if (!catData.contains("NeoForgeData", Tag.TAG_COMPOUND)) return;
            CompoundTag neoData = catData.getCompound("NeoForgeData");
            neoData.remove(CatClothesData.EQUIPPED_TAG);
            neoData.remove(CatClothesData.OUTFIT_TAG);
            catData.put("NeoForgeData", neoData);
            root.put(CAT_DATA_TAG, catData);
        });
    }

    @Override
    public Component getName(ItemStack stack) {
        CatOutfitType outfit = getOutfit(stack);
        if (outfit == CatOutfitType.NONE) return super.getName(stack);
        Component baseName = stack.get(DataComponents.CUSTOM_NAME);
        if (baseName == null) baseName = Component.translatable(getDescriptionId());
        return Component.translatable("item.laowu.cat_pancake.outfit_named",
                Component.translatable("item.laowu.cat_pancake.prefix." + outfit.id()), baseName);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (!(user instanceof Player player)) return;
        int chargeTicks = getUseDuration(stack, user) - timeLeft;
        if (chargeTicks < 2) return;

        float power = chargePower(chargeTicks);
        if (!level.isClientSide) {
            ItemStack projectileStack = stack.copyWithCount(1);
            CatPancakeProjectile projectile = new CatPancakeProjectile(level, player, projectileStack,
                    2.0F + 8.0F * power, 1.0F + 2.0F * power);
            float speed = 0.75F + 2.75F * power;
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(),
                    0.0F, speed, 0.5F);
            level.addFreshEntity(projectile);

            if (level instanceof ServerLevel serverLevel) {
                ModNetwork.playLogisticsSound(serverLevel, player.blockPosition(), false);
            }
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    private static float chargePower(int chargeTicks) {
        float time = Math.min(chargeTicks, MAX_CHARGE_TICKS) / (float) MAX_CHARGE_TICKS;
        return Math.min((time * time + time * 2.0F) / 3.0F, 1.0F);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (entity.level().isClientSide || !(entity.level() instanceof ServerLevel level)) return false;
        if (entity.getAge() % FAN_CHECK_INTERVAL != 0) return false;

        CompoundTag data = entity.getPersistentData();
        if (!insideActiveCreateAirCurrent(entity)) {
            data.remove(FAN_TICKS_TAG);
            return false;
        }

        int ticks = data.getInt(FAN_TICKS_TAG) + FAN_CHECK_INTERVAL;
        data.putInt(FAN_TICKS_TAG, ticks);
        level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.1D, entity.getZ(),
                2, 0.12D, 0.04D, 0.12D, 0.02D);
        if (ticks >= REQUIRED_FAN_TICKS) restoreCat(level, entity, stack);
        return false;
    }

    private static boolean insideActiveCreateAirCurrent(ItemEntity entity) {
        Level level = entity.level();
        BlockPos center = entity.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -FAN_SEARCH_RADIUS; x <= FAN_SEARCH_RADIUS; x++) {
            for (int y = -FAN_SEARCH_RADIUS; y <= FAN_SEARCH_RADIUS; y++) {
                for (int z = -FAN_SEARCH_RADIUS; z <= FAN_SEARCH_RADIUS; z++) {
                    cursor.setWithOffset(center, x, y, z);
                    if (!level.hasChunkAt(cursor)) continue;
                    if (!(level.getBlockEntity(cursor) instanceof EncasedFanBlockEntity fan)
                            || fan.getSpeed() == 0.0F || fan.getAirCurrent() == null
                            || fan.getAirCurrent().bounds == null) continue;
                    if (fan.getAirCurrent().bounds.inflate(0.08D).contains(entity.position())) return true;
                }
            }
        }
        return false;
    }

    private static void restoreCat(ServerLevel level, ItemEntity pancakeEntity, ItemStack stack) {
        Cat cat = EntityType.CAT.create(level);
        if (cat == null) return;

        CompoundTag root = ItemCustomData.copy(stack);
        if (root.contains(CAT_DATA_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag saved = root.getCompound(CAT_DATA_TAG).copy();
            removePositionAndIdentity(saved);
            cat.load(saved);
        } else if (root.contains(CAT_VARIANT_TAG)) {
            ResourceLocation key = ResourceLocation.tryParse(root.getString(CAT_VARIANT_TAG));
            if (key != null && BuiltInRegistries.CAT_VARIANT.containsKey(key)) {
                BuiltInRegistries.CAT_VARIANT.getHolder(key).ifPresent(cat::setVariant);
            }
        }

        if (isBaby(stack) && !root.contains(CAT_DATA_TAG, Tag.TAG_COMPOUND)) {
            cat.setAge(-24000);
        }

        clearTransientState(cat.getPersistentData());
        CatOutfitType outfit = getOutfit(stack);
        if (outfit != CatOutfitType.NONE) {
            cat.getPersistentData().putBoolean(CatClothesData.EQUIPPED_TAG, true);
            cat.getPersistentData().putString(CatClothesData.OUTFIT_TAG, outfit.id());
        }
        CatPoseData.setPose(cat, 0);
        cat.moveTo(pancakeEntity.getX(), pancakeEntity.getY() + 0.15D, pancakeEntity.getZ(),
                pancakeEntity.getYRot(), 0.0F);
        cat.setDeltaMovement(pancakeEntity.getDeltaMovement().scale(0.2D));
        if (!level.addFreshEntity(cat)) return;

        pancakeEntity.discard();
        ModNetwork.syncToTracking(cat, 0);
        ModNetwork.syncCatChestToTracking(cat);
        ModNetwork.syncCatClothesToTracking(cat);
        level.sendParticles(ParticleTypes.POOF, cat.getX(), cat.getY() + 0.4D, cat.getZ(),
                16, 0.25D, 0.2D, 0.25D, 0.04D);
        level.playSound(null, cat.blockPosition(), SoundEvents.WOOL_BREAK, SoundSource.NEUTRAL, 1.0F, 0.65F);
        cat.playSound(SoundEvents.CAT_AMBIENT, 0.8F, 1.0F);
    }

    private static void removePositionAndIdentity(CompoundTag tag) {
        tag.remove("UUID");
        tag.remove("UUIDMost");
        tag.remove("UUIDLeast");
        tag.remove("Pos");
        tag.remove("Motion");
        tag.remove("Rotation");
        tag.remove("FallDistance");
        tag.remove("Fire");
    }

    private static void clearTransientState(CompoundTag data) {
        data.remove(CatPoseData.TAG);
        data.remove("LaoWuAudioSession");
        data.remove("LaoWuHissingFightTarget");
        data.remove("LaoWuHissingAttackCooldown");
        data.remove("LaoWuHissingAttractionActive");
        data.remove("LaoWuHissingPositionLocked");
        data.remove("LaoWuHissingLockX");
        data.remove("LaoWuHissingLockZ");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.laowu.cat_pancake.fan")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player,
                                                   HumanoidArm arm, ItemStack stack,
                                                   float partialTick, float equipProgress,
                                                   float swingProgress) {
                if (!player.isUsingItem() || player.getUseItemRemainingTicks() <= 0
                        || !player.getUseItem().is(CatPancakeItem.this)) return false;
                HumanoidArm usedArm = player.getUsedItemHand() == InteractionHand.MAIN_HAND
                        ? player.getMainArm() : player.getMainArm().getOpposite();
                if (usedArm != arm) return false;

                int side = arm == HumanoidArm.RIGHT ? 1 : -1;
                poseStack.translate(side * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
                poseStack.translate(side * -0.2785682F, 0.18344387F, 0.15731531F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-13.935F));
                poseStack.mulPose(Axis.YP.rotationDegrees(side * 35.3F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(side * -9.785F));

                float useTicks = stack.getUseDuration(player)
                        - (player.getUseItemRemainingTicks() - partialTick + 1.0F);
                float pull = useTicks / MAX_CHARGE_TICKS;
                pull = Math.min((pull * pull + pull * 2.0F) / 3.0F, 1.0F);
                if (pull > 0.1F) {
                    float wobble = Mth.sin((useTicks - 0.1F) * 1.3F) * (pull - 0.1F);
                    poseStack.translate(0.0F, wobble * 0.004F, 0.0F);
                }
                poseStack.translate(0.0F, 0.0F, pull * 0.04F);
                poseStack.scale(1.0F, 1.0F, 1.0F + pull * 0.2F);
                poseStack.mulPose(Axis.YN.rotationDegrees(side * 45.0F));
                return true;
            }

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft minecraft = Minecraft.getInstance();
                    renderer = new cn.laowu.mod.client.CatPancakeItemRenderer(
                            minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
                }
                return renderer;
            }
        });
    }
}
