package cn.laowu.mod.item;

import cn.laowu.mod.CatPoseData;
import cn.laowu.mod.CatPancakeBehavior;
import cn.laowu.mod.CatClothesData;
import cn.laowu.mod.CatOutfitType;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.entity.CatPancakeProjectile;
import cn.laowu.mod.network.ModNetwork;
import cn.laowu.mod.genetics.CatGenomeData;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatAttributeProfile;
import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatTraitProfile;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
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
import net.minecraft.util.RandomSource;
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
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

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
    private static final String PRE_TERMINATOR_NAME_TAG = "LaoWuPreTerminatorName";
    private static final String FAN_TICKS_TAG = "LaoWuCatPancakeFanTicks";
    private static final int FAN_CHECK_INTERVAL = 5;
    private static final int REQUIRED_FAN_TICKS = 30;
    private static final int FAN_SEARCH_RADIUS = 16;
    private static final int MAX_CHARGE_TICKS = 80;
    private static final int CAREER_DEATH_POTENTIAL_LOSS = 20;

    public CatPancakeItem(Properties properties) {
        super(properties);
    }

    public static ItemStack capture(Cat cat) {
        ItemStack pancake = new ItemStack(LaoWuMod.CAT_PANCAKE.get());
        // Ensure the six-dimensional genes enter both the full entity snapshot
        // and the compact top-level pancake bridge before the cat is removed.
        CatAttributeData.ensure(cat);
        CatTraitData.ensure(cat);
        CompoundTag root = pancake.getOrCreateTag();
        CompoundTag catData = cat.saveWithoutId(new CompoundTag());
        removePositionAndIdentity(catData);
        CatPancakeBehavior.sanitizeCapturedData(catData);
        clearTransientState(catData.getCompound("ForgeData"));
        root.put(CAT_DATA_TAG, catData);
        root.putBoolean(TAMED_TAG, cat.isTame());
        if (cat.isBaby()) root.putBoolean(BABY_TAG, true);
        CatOutfitType outfit = CatClothesData.getOutfit(cat);
        if (outfit != CatOutfitType.NONE) {
            root.putBoolean(CatClothesData.EQUIPPED_TAG, true);
            root.putString(CatClothesData.OUTFIT_TAG, outfit.id());
        }

        ResourceLocation variant = BuiltInRegistries.CAT_VARIANT.getKey(cat.getVariant());
        if (variant != null) root.putString(CAT_VARIANT_TAG, variant.toString());
        root.putString(CAT_TEXTURE_TAG, cat.getVariant().texture().toString());
        CatGenomeData.copyToStack(cat, pancake);
        CatAttributeData.copyToStack(cat, pancake);
        CatTraitData.copyToStack(cat, pancake);
        if (cat.hasCustomName()) {
            pancake.setHoverName(Component.translatable("item.laowu.cat_pancake.named", cat.getDisplayName()));
        }
        if (outfit != CatOutfitType.NONE) applyOutfitDisplayName(pancake, outfit);
        return pancake;
    }

    /** Captures a killed profession cat in a state that can later be restored alive. */
    public static ItemStack captureDeathDrop(Cat cat) {
        ItemStack pancake = capture(cat);
        CatAttributeProfile attributes = CatAttributeData.ensure(cat);
        CatStat[] stats = CatStat.values();
        CatStat reducedStat = stats[cat.getRandom().nextInt(stats.length)];
        int reducedPotential = Math.max(CatAttributeProfile.MIN_VALUE,
                attributes.potential(reducedStat) - CAREER_DEATH_POTENTIAL_LOSS);
        CatAttributeData.set(pancake, attributes.withValues(reducedStat,
                attributes.current(reducedStat), reducedPotential));

        CompoundTag root = pancake.getTag();
        if (root != null && root.contains(CAT_DATA_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag catData = root.getCompound(CAT_DATA_TAG);
            catData.putFloat("Health", Math.max(1.0F, cat.getMaxHealth()));
            catData.remove("DeathTime");
            catData.remove("HurtTime");
            catData.remove("HurtByTimestamp");
        }
        return pancake;
    }

    public static ResourceLocation texture(ItemStack stack) {
        CompoundTag root = stack.getTag();
        String value = root == null ? "" : root.getString(CAT_TEXTURE_TAG);
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        CatVariant fallback = BuiltInRegistries.CAT_VARIANT.get(CatVariant.RED);
        return parsed != null ? parsed : fallback.texture();
    }

    /** Stable orange-cat representative used by the creative tab and recipe viewers. */
    public static ItemStack defaultDisplayStack() {
        return withDisplayGenes(variantStack(DEFAULT_VARIANT), 0x4C414F57554CL, false);
    }

    /** Stable orange kitten representative used by JEI for the spout recipe. */
    public static ItemStack defaultBabyDisplayStack() {
        return withDisplayGenes(babyVariantStack(DEFAULT_VARIANT), 0x42414259434154L, true);
    }

    private static ItemStack withDisplayGenes(ItemStack stack, long seed, boolean injected) {
        CatGenomeData.set(stack, cn.laowu.mod.genetics.CatGenome.uniform(DEFAULT_VARIANT));
        RandomSource random = RandomSource.create(seed);
        CatAttributeData.set(stack, cn.laowu.mod.genetics.CatAttributeProfile.founder(random));
        CatTraitData.set(stack, injected
                ? CatTraitProfile.injected(random) : CatTraitProfile.founder(random));
        return stack;
    }

    /** JEI intentionally shows one stable skin while recipes still match every cat variant. */
    public static List<ItemStack> jeiDisplayStacks() {
        return List.of(defaultDisplayStack());
    }

    /** Concrete NBT-bearing stacks used by recipe viewers, one for every registered cat variant. */
    public static List<ItemStack> allVariantStacks() {
        List<ItemStack> result = new ArrayList<>();
        BuiltInRegistries.CAT_VARIANT.entrySet().forEach(entry -> {
            ItemStack stack = new ItemStack(LaoWuMod.CAT_PANCAKE.get());
            ResourceLocation id = entry.getKey().location();
            stack.getOrCreateTag().putString(CAT_VARIANT_TAG, id.toString());
            stack.getOrCreateTag().putString(CAT_TEXTURE_TAG, entry.getValue().texture().toString());
            result.add(stack);
        });
        return List.copyOf(result);
    }

    /** Creates a usable pancake stack with the same variant/texture tags as a captured cat. */
    public static ItemStack variantStack(ResourceLocation variantId) {
        CatVariant variant = BuiltInRegistries.CAT_VARIANT.get(variantId);
        if (variant == null) variant = BuiltInRegistries.CAT_VARIANT.get(CatVariant.RED);
        ItemStack stack = new ItemStack(LaoWuMod.CAT_PANCAKE.get());
        ResourceLocation resolvedId = BuiltInRegistries.CAT_VARIANT.getKey(variant);
        stack.getOrCreateTag().putString(CAT_VARIANT_TAG, resolvedId.toString());
        stack.getOrCreateTag().putString(CAT_TEXTURE_TAG, variant.texture().toString());
        return stack;
    }

    public static ItemStack babyVariantStack(ResourceLocation variantId) {
        ItemStack stack = variantStack(variantId);
        stack.getOrCreateTag().putBoolean(BABY_TAG, true);
        return stack;
    }

    /** Supports both new explicit markers and captured pancakes from older builds. */
    public static boolean isBaby(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null) return false;
        if (root.getBoolean(BABY_TAG)) return true;
        return root.contains(CAT_DATA_TAG, Tag.TAG_COMPOUND)
                && root.getCompound(CAT_DATA_TAG).getInt("Age") < 0;
    }

    /**
     * Converts an item-form kitten pancake into its adult form without losing
     * its texture, genome, attributes, owner, name or outfit data.
     */
    public static void makeAdult(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null) return;
        root.remove(BABY_TAG);
        if (root.contains(CAT_DATA_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag catData = root.getCompound(CAT_DATA_TAG);
            catData.putInt("Age", 0);
            root.put(CAT_DATA_TAG, catData);
        }
    }

    public static ResourceLocation variantId(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root != null) {
            ResourceLocation saved = ResourceLocation.tryParse(root.getString(CAT_VARIANT_TAG));
            if (saved != null && BuiltInRegistries.CAT_VARIANT.containsKey(saved)) return saved;
        }
        ResourceLocation renderedTexture = texture(stack);
        for (var entry : BuiltInRegistries.CAT_VARIANT.entrySet()) {
            if (entry.getValue().texture().equals(renderedTexture)) return entry.getKey().location();
        }
        return CatVariant.RED.location();
    }

    /** NBT-complete representative used by paired Create/JEI recipe pages. */
    public static ItemStack recipeDisplayStack(ResourceLocation variantId, boolean terminator) {
        return recipeDisplayStack(variantId,
                terminator ? CatOutfitType.TERMINATOR : CatOutfitType.NONE);
    }

    public static ItemStack recipeDisplayStack(ResourceLocation variantId, CatOutfitType outfit) {
        ItemStack stack = variantStack(variantId);
        stack.getOrCreateTag().putBoolean(TAMED_TAG, true);
        if (outfit != CatOutfitType.NONE) equipOutfit(stack, outfit);
        return stack;
    }

    /** True for both newly deployed stacks and older captured cats carrying the marker in ForgeData. */
    public static boolean hasTerminatorSuit(ItemStack stack) {
        return getOutfit(stack) == CatOutfitType.TERMINATOR;
    }

    public static boolean hasOutfit(ItemStack stack) {
        return getOutfit(stack) != CatOutfitType.NONE;
    }

    /** Legacy boolean-only pancakes are interpreted as Terminator pancakes. */
    public static CatOutfitType getOutfit(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null) return CatOutfitType.NONE;
        CatOutfitType direct = CatOutfitType.byId(root.getString(CatClothesData.OUTFIT_TAG));
        if (direct != CatOutfitType.NONE) return direct;
        if (root.getBoolean(CatClothesData.EQUIPPED_TAG)) return CatOutfitType.TERMINATOR;
        if (!root.contains(CAT_DATA_TAG, Tag.TAG_COMPOUND)) return CatOutfitType.NONE;
        CompoundTag catData = root.getCompound(CAT_DATA_TAG);
        if (!catData.contains("ForgeData", Tag.TAG_COMPOUND)) return CatOutfitType.NONE;
        CompoundTag forgeData = catData.getCompound("ForgeData");
        CatOutfitType nested = CatOutfitType.byId(forgeData.getString(CatClothesData.OUTFIT_TAG));
        if (nested != CatOutfitType.NONE) return nested;
        return forgeData.getBoolean(CatClothesData.EQUIPPED_TAG)
                ? CatOutfitType.TERMINATOR : CatOutfitType.NONE;
    }

    /** Reads the owner saved by vanilla so old captured pancakes remain compatible. */
    public static boolean isTamed(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null) return false;
        if (root.getBoolean(TAMED_TAG)) return true;
        if (!root.contains(CAT_DATA_TAG, Tag.TAG_COMPOUND)) return false;
        CompoundTag catData = root.getCompound(CAT_DATA_TAG);
        return catData.hasUUID("Owner")
                || catData.contains("OwnerUUID", Tag.TAG_STRING)
                && !catData.getString("OwnerUUID").isBlank();
    }

    /** Adds the Terminator marker without replacing texture, name, owner or captured-cat data. */
    public static void equipTerminatorSuit(ItemStack stack) {
        equipOutfit(stack, CatOutfitType.TERMINATOR);
    }

    /** Adds an outfit marker without replacing texture, owner or captured-cat data. */
    public static void equipOutfit(ItemStack stack, CatOutfitType outfit) {
        if (outfit == CatOutfitType.NONE) return;
        CompoundTag root = stack.getOrCreateTag();
        root.putBoolean(CatClothesData.EQUIPPED_TAG, true);
        root.putString(CatClothesData.OUTFIT_TAG, outfit.id());
        if (root.contains(CAT_DATA_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag catData = root.getCompound(CAT_DATA_TAG);
            CompoundTag forgeData = catData.contains("ForgeData", Tag.TAG_COMPOUND)
                    ? catData.getCompound("ForgeData") : new CompoundTag();
            forgeData.putBoolean(CatClothesData.EQUIPPED_TAG, true);
            forgeData.putString(CatClothesData.OUTFIT_TAG, outfit.id());
            catData.put("ForgeData", forgeData);
            root.put(CAT_DATA_TAG, catData);
        }
        rememberOriginalDisplayName(stack);
    }

    /** Removes only the outfit marker; all captured cat identity data is retained. */
    public static void removeTerminatorSuit(ItemStack stack) {
        removeOutfit(stack);
    }

    public static void removeOutfit(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null) return;
        root.remove(CatClothesData.EQUIPPED_TAG);
        root.remove(CatClothesData.OUTFIT_TAG);
        if (root.contains(CAT_DATA_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag catData = root.getCompound(CAT_DATA_TAG);
            if (catData.contains("ForgeData", Tag.TAG_COMPOUND)) {
                CompoundTag forgeData = catData.getCompound("ForgeData");
                forgeData.remove(CatClothesData.EQUIPPED_TAG);
                forgeData.remove(CatClothesData.OUTFIT_TAG);
                catData.put("ForgeData", forgeData);
                root.put(CAT_DATA_TAG, catData);
            }
        }

        if (root.contains(PRE_TERMINATOR_NAME_TAG, Tag.TAG_STRING)) {
            String previousName = root.getString(PRE_TERMINATOR_NAME_TAG);
            root.remove(PRE_TERMINATOR_NAME_TAG);
            stack.resetHoverName();
            stack.getOrCreateTagElement("display").putString("Name", previousName);
        }
    }

    private static void applyOutfitDisplayName(ItemStack stack, CatOutfitType outfit) {
        if (outfit == CatOutfitType.NONE || !stack.hasCustomHoverName()) return;
        rememberOriginalDisplayName(stack);
    }

    private static void rememberOriginalDisplayName(ItemStack stack) {
        CompoundTag root = stack.getOrCreateTag();
        CompoundTag display = root.getCompound("display");
        if (display.contains("Name", Tag.TAG_STRING)
                && !root.contains(PRE_TERMINATOR_NAME_TAG, Tag.TAG_STRING)) {
            String currentName = display.getString("Name");
            // Older builds stored their generated outfit name as a literal
            // custom name. Do not preserve that stale prefix when migrating.
            boolean generatedOutfitName = false;
            try {
                Component parsed = Component.Serializer.fromJson(currentName);
                if (parsed != null && parsed.getContents()
                        instanceof net.minecraft.network.chat.contents.TranslatableContents translated) {
                    generatedOutfitName = translated.getKey().startsWith(
                            "item.laowu.cat_pancake.");
                }
            } catch (RuntimeException ignored) {
            }
            if (!generatedOutfitName) {
                root.putString(PRE_TERMINATOR_NAME_TAG, currentName);
            }
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        CatOutfitType outfit = getOutfit(stack);
        if (outfit != CatOutfitType.NONE) {
            clearLegacyGeneratedDisplayName(stack);
            Component baseName = savedBaseName(stack);
            return Component.translatable("item.laowu.cat_pancake.outfit_named",
                    Component.translatable(outfitItemNameKey(outfit)),
                    baseName);
        }
        return super.getName(stack);
    }

    private static void clearLegacyGeneratedDisplayName(ItemStack stack) {
        if (!stack.hasCustomHoverName()) return;
        CompoundTag root = stack.getTag();
        if (root == null || root.contains(PRE_TERMINATOR_NAME_TAG, Tag.TAG_STRING)) return;
        Component current = stack.getHoverName();
        if (current.getContents()
                instanceof net.minecraft.network.chat.contents.TranslatableContents translated
                && translated.getKey().startsWith("item.laowu.cat_pancake.")) {
            stack.resetHoverName();
        }
    }

    private static Component savedBaseName(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root != null && root.contains(PRE_TERMINATOR_NAME_TAG, Tag.TAG_STRING)) {
            try {
                Component parsed = Component.Serializer.fromJson(
                        root.getString(PRE_TERMINATOR_NAME_TAG));
                if (parsed != null) return parsed;
            } catch (RuntimeException ignored) {
                // Corrupt legacy display data falls back to the normal item name.
            }
        }
        return Component.translatable(stack.getItem().getDescriptionId());
    }

    private static String outfitItemNameKey(CatOutfitType outfit) {
        return "item.laowu.cat_pancake.prefix." + outfit.id();
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
        int chargeTicks = getUseDuration(stack) - timeLeft;
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
    public int getUseDuration(ItemStack stack) {
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

        CompoundTag root = stack.getTag();
        if (root != null && root.contains(CAT_DATA_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag saved = root.getCompound(CAT_DATA_TAG).copy();
            removePositionAndIdentity(saved);
            cat.load(saved);
        } else if (root != null && root.contains(CAT_VARIANT_TAG)) {
            ResourceLocation key = ResourceLocation.tryParse(root.getString(CAT_VARIANT_TAG));
            if (key != null && BuiltInRegistries.CAT_VARIANT.containsKey(key)) {
                cat.setVariant(BuiltInRegistries.CAT_VARIANT.get(key));
            }
        }

        // Generated kitten pancakes carry no full entity snapshot. Captured
        // pancakes already restored their exact remaining growth age above.
        if (isBaby(stack)
                && (root == null || !root.contains(CAT_DATA_TAG, Tag.TAG_COMPOUND))) {
            cat.setAge(-24000);
        }

        clearTransientState(cat.getPersistentData());
        CatGenomeData.applyFromStack(stack, cat);
        CatAttributeData.applyFromStack(stack, cat);
        CatTraitData.applyFromStack(stack, cat);
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
        if (CatGenomeData.has(cat)) ModNetwork.syncCatGenomeToTracking(cat);
        ModNetwork.syncCatAttributesToTracking(cat);
        ModNetwork.syncCatTraitsToTracking(cat);
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
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
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

                float useTicks = stack.getUseDuration()
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
