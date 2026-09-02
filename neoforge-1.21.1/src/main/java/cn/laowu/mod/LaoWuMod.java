package cn.laowu.mod;

import cn.laowu.mod.item.CatPancakeItem;
import cn.laowu.mod.item.CatFilterItem;
import cn.laowu.mod.item.CatCannonItem;
import cn.laowu.mod.item.CatSmithingTemplateItem;
import cn.laowu.mod.item.CatToolTier;
import cn.laowu.mod.item.HissingGasBucketItem;
import cn.laowu.mod.item.CatBallItem;
import cn.laowu.mod.item.CatPouchItem;
import cn.laowu.mod.item.CatGrenadeBoxItem;
import cn.laowu.mod.item.CatStripItem;
import cn.laowu.mod.item.CatFoodItem;
import cn.laowu.mod.item.PheromoneCatFoodItem;
import cn.laowu.mod.item.CatAttributeCanItem;
import cn.laowu.mod.item.CatTraitFishItem;
import cn.laowu.mod.item.BreedingOnlyCatCanItem;
import cn.laowu.mod.item.BreedingCatFoodItem;
import cn.laowu.mod.item.MaterialDebugWandItem;
import cn.laowu.mod.item.CatScannerItem;
import cn.laowu.mod.item.ButterBreadItem;
import cn.laowu.mod.item.CatGrenadeItem;
import cn.laowu.mod.item.CatEngineBlockItem;
import cn.laowu.mod.item.DevouringCatBlockItem;
import cn.laowu.mod.item.KimiArmorItem;
import cn.laowu.mod.item.CatEngineerGogglesItem;
import cn.laowu.mod.item.CatFurItem;
import cn.laowu.mod.item.CatHoeItem;
import cn.laowu.mod.item.CatTotemItem;
import cn.laowu.mod.item.TerminatorSuitItem;
import cn.laowu.mod.item.AdoptionBoxBlockItem;
import cn.laowu.mod.entity.CatPancakeProjectile;
import cn.laowu.mod.entity.CatBallEntity;
import cn.laowu.mod.entity.ButterCatBoss;
import cn.laowu.mod.entity.FishingRodProjectile;
import cn.laowu.mod.entity.MechanicalLaserProjectile;
import cn.laowu.mod.entity.HoneyMissileProjectile;
import cn.laowu.mod.entity.LogisticsSupportProjectile;
import cn.laowu.mod.entity.DynamiteProjectile;
import cn.laowu.mod.effect.HissingAttackEffect;
import cn.laowu.mod.fluid.HissingGasFluidType;
import cn.laowu.mod.fluid.LiquidCatFluidType;
import cn.laowu.mod.recipe.InfiltratingRecipe;
import cn.laowu.mod.recipe.CatPancakeIngredient;
import cn.laowu.mod.recipe.CatPancakeVariantIngredient;
import cn.laowu.mod.recipe.AnyBlockIngredient;
import cn.laowu.mod.recipe.NamedPlayerNameTagIngredient;
import cn.laowu.mod.recipe.PheromoneCatFoodMixingRecipe;
import cn.laowu.mod.recipe.RandomBabyCatPancakeFillingRecipe;
import cn.laowu.mod.loot.CatToolEmpoweredLootModifier;
import cn.laowu.mod.particle.NozzleFluidPuffData;
import cn.laowu.mod.network.ModNetwork;
import cn.laowu.mod.create.InfiltrationTankBlock;
import cn.laowu.mod.create.InfiltrationTankBlockEntity;
import cn.laowu.mod.create.HissingCollectorBlock;
import cn.laowu.mod.create.HissingCollectorBlockEntity;
import cn.laowu.mod.create.CatEngineBlock;
import cn.laowu.mod.create.CatEngineBlockEntity;
import cn.laowu.mod.create.DevouringCatBlock;
import cn.laowu.mod.create.DevouringCatBlockEntity;
import cn.laowu.mod.create.BreedingBoxBlock;
import cn.laowu.mod.create.BreedingBoxBlockEntity;
import cn.laowu.mod.create.BreedingBoxTier;
import cn.laowu.mod.create.AdoptionBoxBlock;
import cn.laowu.mod.create.AdoptionBoxBlockEntity;
import cn.laowu.mod.genetics.CatBreedingMode;
import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.BreedingBoxMenu;
import cn.laowu.mod.item.BreedingBoxBlockItem;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.mojang.serialization.MapCodec;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;

import java.util.List;

@Mod(LaoWuMod.MOD_ID)
public final class LaoWuMod {
    public static final String MOD_ID = "laowu";
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);
    private static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, MOD_ID);
    private static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, MOD_ID);
    private static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(Registries.POTION, MOD_ID);
    private static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, MOD_ID);
    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, MOD_ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MOD_ID);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, MOD_ID);
    private static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, MOD_ID);
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MOD_ID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>,
            MapCodec<CatToolEmpoweredLootModifier>> CAT_TOOL_EMPOWERED_LOOT =
            LOOT_MODIFIERS.register("cat_tool_empowered", () -> CatToolEmpoweredLootModifier.CODEC);
    public static final DeferredHolder<ParticleType<?>, ParticleType<NozzleFluidPuffData>> NOZZLE_FLUID_PUFF =
            PARTICLE_TYPES.register("nozzle_fluid_puff", () -> new ParticleType<>(false) {
                @Override
                public MapCodec<NozzleFluidPuffData> codec() {
                    return NozzleFluidPuffData.CODEC;
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, NozzleFluidPuffData> streamCodec() {
                    return NozzleFluidPuffData.STREAM_CODEC;
                }
            });

    public static final DeferredBlock<Block> CAT_BLOCK = BLOCKS.register("cat_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK)));

    public static final DeferredBlock<CatEngineBlock> CAT_ENGINE = BLOCKS.register("cat_engine",
            () -> new CatEngineBlock(BlockBehaviour.Properties.of()
                    .noOcclusion().strength(3.0F, 6.0F).requiresCorrectToolForDrops()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CatEngineBlockEntity>> CAT_ENGINE_BE =
            BLOCK_ENTITIES.register("cat_engine", () -> BlockEntityType.Builder
                    .of(CatEngineBlockEntity::new, CAT_ENGINE.get()).build(null));
    public static final DeferredBlock<InfiltrationTankBlock> INFILTRATION_TANK = BLOCKS.register("infiltration_tank",
            () -> new InfiltrationTankBlock(BlockBehaviour.Properties.of()
                    .noOcclusion().strength(3.0F, 6.0F).requiresCorrectToolForDrops()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfiltrationTankBlockEntity>> INFILTRATION_TANK_BE =
            BLOCK_ENTITIES.register("infiltration_tank", () -> BlockEntityType.Builder
                    .of(InfiltrationTankBlockEntity::new, INFILTRATION_TANK.get()).build(null));
    public static final DeferredBlock<HissingCollectorBlock> HISSING_COLLECTOR = BLOCKS.register("hissing_collector",
            () -> new HissingCollectorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .noOcclusion().strength(1.5F, 6.0F)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HissingCollectorBlockEntity>> HISSING_COLLECTOR_BE =
            BLOCK_ENTITIES.register("hissing_collector", () -> BlockEntityType.Builder
                    .of(HissingCollectorBlockEntity::new, HISSING_COLLECTOR.get()).build(null));
    public static final DeferredBlock<DevouringCatBlock> DEVOURING_CAT = BLOCKS.register("devouring_cat",
            () -> new DevouringCatBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .noOcclusion().strength(3.0F, 6.0F)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DevouringCatBlockEntity>> DEVOURING_CAT_BE =
            BLOCK_ENTITIES.register("devouring_cat", () -> BlockEntityType.Builder
                    .of(DevouringCatBlockEntity::new, DEVOURING_CAT.get()).build(null));
    public static final DeferredBlock<BreedingBoxBlock> BASIC_BREEDING_BOX = BLOCKS.register("basic_breeding_box",
            () -> new BreedingBoxBlock(BreedingBoxTier.BASIC, BlockBehaviour.Properties.of().noOcclusion().strength(3.0F, 6.0F)));
    public static final DeferredBlock<BreedingBoxBlock> INTERMEDIATE_BREEDING_BOX = BLOCKS.register("intermediate_breeding_box",
            () -> new BreedingBoxBlock(BreedingBoxTier.INTERMEDIATE, BlockBehaviour.Properties.of().noOcclusion().strength(3.0F, 6.0F)));
    public static final DeferredBlock<BreedingBoxBlock> ADVANCED_BREEDING_BOX = BLOCKS.register("advanced_breeding_box",
            () -> new BreedingBoxBlock(BreedingBoxTier.ADVANCED, BlockBehaviour.Properties.of().noOcclusion().strength(3.0F, 6.0F)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BreedingBoxBlockEntity>> BREEDING_BOX_BE =
            BLOCK_ENTITIES.register("breeding_box", () -> BlockEntityType.Builder
                    .of(BreedingBoxBlockEntity::new, BASIC_BREEDING_BOX.get(), INTERMEDIATE_BREEDING_BOX.get(), ADVANCED_BREEDING_BOX.get())
                    .build(null));
    public static final DeferredBlock<AdoptionBoxBlock> ADOPTION_BOX = BLOCKS.register("adoption_box",
            () -> new AdoptionBoxBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)
                    .noOcclusion().strength(0.8F)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdoptionBoxBlockEntity>> ADOPTION_BOX_BE =
            BLOCK_ENTITIES.register("adoption_box", () -> BlockEntityType.Builder
                    .of(AdoptionBoxBlockEntity::new, ADOPTION_BOX.get()).build(null));
    public static final DeferredHolder<EntityType<?>, EntityType<ButterCatBoss>> BUTTER_CAT =
            ENTITY_TYPES.register("butter_cat", () -> EntityType.Builder
                    .<ButterCatBoss>of(ButterCatBoss::new, MobCategory.MONSTER)
                    .sized(ButterCatBoss.BASE_WIDTH * ButterCatBoss.MODEL_SCALE,
                            ButterCatBoss.BASE_HEIGHT * ButterCatBoss.MODEL_SCALE)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("butter_cat"));
    public static final DeferredItem<CatPancakeItem> CAT_PANCAKE = ITEMS.register("cat_pancake",
            () -> new CatPancakeItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<CatFilterItem> CAT_FILTER = ITEMS.register("cat_filter",
            () -> new CatFilterItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<cn.laowu.mod.item.FusionDebugWandItem> FUSION_DEBUG_WAND =
            ITEMS.register("fusion_debug_wand",
                    () -> new cn.laowu.mod.item.FusionDebugWandItem(new Item.Properties()
                            .stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
    public static final DeferredItem<cn.laowu.mod.item.AttributeDebugWandItem> ATTRIBUTE_DEBUG_WAND =
            ITEMS.register("attribute_debug_wand",
                    () -> new cn.laowu.mod.item.AttributeDebugWandItem(new Item.Properties()
                            .stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
    public static final DeferredItem<cn.laowu.mod.item.TraitDebugWandItem> TRAIT_DEBUG_WAND =
            ITEMS.register("trait_debug_wand",
                    () -> new cn.laowu.mod.item.TraitDebugWandItem(new Item.Properties()
                            .stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
    public static final DeferredItem<MaterialDebugWandItem> MATERIAL_DEBUG_WAND =
            ITEMS.register("material_debug_wand",
                    () -> new MaterialDebugWandItem(new Item.Properties().stacksTo(1)
                            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
    public static final DeferredItem<CatScannerItem> CAT_SCANNER = ITEMS.register("cat_scanner",
            () -> new CatScannerItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<ButterBreadItem> BUTTER_BREAD = ITEMS.register("butter_bread",
            () -> new ButterBreadItem(new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> BUTTER_CAT_SPAWN_EGG = ITEMS.register(
            "butter_cat_spawn_egg", () -> new SpawnEggItem(BUTTER_CAT.get(),
                    0xE8B94F, 0xFFF1A3, new Item.Properties()));
    public static final DeferredItem<TerminatorSuitItem> TERMINATOR_SUIT = ITEMS.register("terminator_suit",
            () -> new TerminatorSuitItem(new Item.Properties(), CatOutfitType.TERMINATOR));
    public static final DeferredItem<TerminatorSuitItem> FISHING_SUIT = ITEMS.register("fishing_suit",
            () -> new TerminatorSuitItem(new Item.Properties(), CatOutfitType.FISHING));
    public static final DeferredItem<TerminatorSuitItem> FLIGHT_SUIT = ITEMS.register("flight_suit",
            () -> new TerminatorSuitItem(new Item.Properties(), CatOutfitType.FLIGHT));
    public static final DeferredItem<TerminatorSuitItem> FIRE_SUIT = ITEMS.register("fire_suit",
            () -> new TerminatorSuitItem(new Item.Properties(), CatOutfitType.FIRE));
    public static final DeferredItem<TerminatorSuitItem> HONEY_SUIT = ITEMS.register("honey_suit",
            () -> new TerminatorSuitItem(new Item.Properties(), CatOutfitType.HONEY));
    public static final DeferredItem<TerminatorSuitItem> TRANSPORT_SUIT = ITEMS.register("transport_suit",
            () -> new TerminatorSuitItem(new Item.Properties(), CatOutfitType.TRANSPORT));
    public static final DeferredItem<TerminatorSuitItem> DYNAMITE_SUIT = ITEMS.register("dynamite_suit",
            () -> new TerminatorSuitItem(new Item.Properties(), CatOutfitType.DYNAMITE));
    public static final DeferredItem<Item> CAT_INGOT = ITEMS.register("cat_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CAT_SHEET = ITEMS.register("cat_sheet",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<CatFurItem> CAT_FUR = ITEMS.register("cat_fur",
            () -> new CatFurItem(new Item.Properties()));
    public static final DeferredItem<Item> CAT_SPRING = ITEMS.register("cat_spring",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CAT_GEAR = ITEMS.register("cat_gear",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CAT_PELLET = ITEMS.register("cat_pellet",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CAT_COMPONENT = ITEMS.register("cat_component",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> INCOMPLETE_CAT_COMPONENT = ITEMS.register(
            "incomplete_cat_component", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> INCOMPLETE_TERMINATOR_SUIT = ITEMS.register(
            "incomplete_terminator_suit", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> INCOMPLETE_FISHING_SUIT = ITEMS.register(
            "incomplete_fishing_suit", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> INCOMPLETE_FLIGHT_SUIT = ITEMS.register(
            "incomplete_flight_suit", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> INCOMPLETE_TRANSPORT_SUIT = ITEMS.register(
            "incomplete_transport_suit", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> INCOMPLETE_FIRE_SUIT = ITEMS.register(
            "incomplete_fire_suit", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> INCOMPLETE_HONEY_SUIT = ITEMS.register(
            "incomplete_honey_suit", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> INCOMPLETE_DYNAMITE_SUIT = ITEMS.register(
            "incomplete_dynamite_suit", () -> new Item(new Item.Properties()));
    public static final DeferredItem<CatTotemItem> CAT_TOTEM = ITEMS.register("cat_totem",
            () -> new CatTotemItem(new Item.Properties()));
    public static final DeferredItem<CatEngineerGogglesItem> CAT_ENGINEER_GOGGLES =
            ITEMS.register("cat_engineer_goggles",
                    () -> new CatEngineerGogglesItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<BlockItem> CAT_BLOCK_ITEM = ITEMS.register("cat_block",
            () -> new BlockItem(CAT_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<CatSmithingTemplateItem> CAT_UPGRADE_SMITHING_TEMPLATE = ITEMS.register(
            "cat_upgrade_smithing_template", CatSmithingTemplateItem::new);
    public static final DeferredItem<SwordItem> CAT_SWORD = ITEMS.register("cat_sword",
            () -> new SwordItem(CatToolTier.INSTANCE, new Item.Properties()
                    .attributes(SwordItem.createAttributes(CatToolTier.INSTANCE, 3, -2.4F))
                    .durability(Items.DIAMOND_SWORD.getDefaultInstance().getMaxDamage())));
    public static final DeferredItem<PickaxeItem> CAT_PICKAXE = ITEMS.register("cat_pickaxe",
            () -> new PickaxeItem(CatToolTier.INSTANCE, new Item.Properties()
                    .attributes(PickaxeItem.createAttributes(CatToolTier.INSTANCE, 1, -2.8F))
                    .durability(Items.DIAMOND_PICKAXE.getDefaultInstance().getMaxDamage())));
    public static final DeferredItem<AxeItem> CAT_AXE = ITEMS.register("cat_axe",
            () -> new AxeItem(CatToolTier.INSTANCE, new Item.Properties()
                    .attributes(AxeItem.createAttributes(CatToolTier.INSTANCE, 5.0F, -3.0F))
                    .durability(Items.DIAMOND_AXE.getDefaultInstance().getMaxDamage())));
    public static final DeferredItem<ShovelItem> CAT_SHOVEL = ITEMS.register("cat_shovel",
            () -> new ShovelItem(CatToolTier.INSTANCE, new Item.Properties()
                    .attributes(ShovelItem.createAttributes(CatToolTier.INSTANCE, 1.5F, -3.0F))
                    .durability(Items.DIAMOND_SHOVEL.getDefaultInstance().getMaxDamage())));
    public static final DeferredItem<CatHoeItem> CAT_HOE = ITEMS.register("cat_hoe",
            () -> new CatHoeItem(CatToolTier.INSTANCE, new Item.Properties()
                    .attributes(HoeItem.createAttributes(CatToolTier.INSTANCE, -3.0F, 0.0F))
                    .durability(Items.DIAMOND_HOE.getDefaultInstance().getMaxDamage())));
    public static final DeferredItem<CatCannonItem> CAT_CANNON = ITEMS.register("cat_cannon",
            () -> new CatCannonItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<CatBallItem> CAT_BALL = ITEMS.register("cat_ball",
            () -> new CatBallItem(new Item.Properties()));
    public static final DeferredItem<CatStripItem> CAT_STRIP = ITEMS.register("cat_strip",
            () -> new CatStripItem(new Item.Properties()));
    public static final DeferredItem<CatPouchItem> CAT_POUCH = ITEMS.register("cat_pouch",
            () -> new CatPouchItem(new Item.Properties()));
    public static final DeferredItem<CatGrenadeBoxItem> CAT_BOX = ITEMS.register("cat_box",
            () -> new CatGrenadeBoxItem(new Item.Properties()));
    public static final DeferredItem<CatFoodItem> CAT_FOOD = ITEMS.register("cat_food",
            () -> new CatFoodItem(new Item.Properties()));
    public static final DeferredItem<PheromoneCatFoodItem> PHEROMONE_CAT_FOOD = ITEMS.register(
            "pheromone_cat_food", () -> new PheromoneCatFoodItem(new Item.Properties()));
    public static final DeferredItem<BreedingOnlyCatCanItem> CAT_CAN = ITEMS.register("cat_can",
            () -> new BreedingOnlyCatCanItem(new Item.Properties()));
    public static final DeferredItem<BreedingOnlyCatCanItem> GOLDEN_CAT_CAN = ITEMS.register(
            "golden_cat_can", () -> new BreedingOnlyCatCanItem(new Item.Properties()));
    public static final DeferredItem<CatAttributeCanItem> ATTACK_CAT_CAN = registerAttributeCan(
            "attack_cat_can", CatStat.ATTACK, CatAttributeCanItem.Tier.NORMAL);
    public static final DeferredItem<CatAttributeCanItem> HEALTH_CAT_CAN = registerAttributeCan(
            "health_cat_can", CatStat.HEALTH, CatAttributeCanItem.Tier.NORMAL);
    public static final DeferredItem<CatAttributeCanItem> SPEED_CAT_CAN = registerAttributeCan(
            "speed_cat_can", CatStat.SPEED, CatAttributeCanItem.Tier.NORMAL);
    public static final DeferredItem<CatAttributeCanItem> STAMINA_CAT_CAN = registerAttributeCan(
            "stamina_cat_can", CatStat.STAMINA, CatAttributeCanItem.Tier.NORMAL);
    public static final DeferredItem<CatAttributeCanItem> INTELLIGENCE_CAT_CAN = registerAttributeCan(
            "intelligence_cat_can", CatStat.INTELLIGENCE, CatAttributeCanItem.Tier.NORMAL);
    public static final DeferredItem<CatAttributeCanItem> LUCK_CAT_CAN = registerAttributeCan(
            "luck_cat_can", CatStat.LUCK, CatAttributeCanItem.Tier.NORMAL);
    public static final DeferredItem<CatAttributeCanItem> GOLDEN_ATTACK_CAT_CAN = registerAttributeCan(
            "golden_attack_cat_can", CatStat.ATTACK, CatAttributeCanItem.Tier.GOLDEN);
    public static final DeferredItem<CatAttributeCanItem> GOLDEN_HEALTH_CAT_CAN = registerAttributeCan(
            "golden_health_cat_can", CatStat.HEALTH, CatAttributeCanItem.Tier.GOLDEN);
    public static final DeferredItem<CatAttributeCanItem> GOLDEN_SPEED_CAT_CAN = registerAttributeCan(
            "golden_speed_cat_can", CatStat.SPEED, CatAttributeCanItem.Tier.GOLDEN);
    public static final DeferredItem<CatAttributeCanItem> GOLDEN_STAMINA_CAT_CAN = registerAttributeCan(
            "golden_stamina_cat_can", CatStat.STAMINA, CatAttributeCanItem.Tier.GOLDEN);
    public static final DeferredItem<CatAttributeCanItem> GOLDEN_INTELLIGENCE_CAT_CAN = registerAttributeCan(
            "golden_intelligence_cat_can", CatStat.INTELLIGENCE, CatAttributeCanItem.Tier.GOLDEN);
    public static final DeferredItem<CatAttributeCanItem> GOLDEN_LUCK_CAT_CAN = registerAttributeCan(
            "golden_luck_cat_can", CatStat.LUCK, CatAttributeCanItem.Tier.GOLDEN);
    public static final DeferredItem<CatAttributeCanItem> SUPER_ATTACK_CAT_CAN = registerAttributeCan(
            "super_attack_cat_can", CatStat.ATTACK, CatAttributeCanItem.Tier.SUPER);
    public static final DeferredItem<CatAttributeCanItem> SUPER_HEALTH_CAT_CAN = registerAttributeCan(
            "super_health_cat_can", CatStat.HEALTH, CatAttributeCanItem.Tier.SUPER);
    public static final DeferredItem<CatAttributeCanItem> SUPER_SPEED_CAT_CAN = registerAttributeCan(
            "super_speed_cat_can", CatStat.SPEED, CatAttributeCanItem.Tier.SUPER);
    public static final DeferredItem<CatAttributeCanItem> SUPER_STAMINA_CAT_CAN = registerAttributeCan(
            "super_stamina_cat_can", CatStat.STAMINA, CatAttributeCanItem.Tier.SUPER);
    public static final DeferredItem<CatAttributeCanItem> SUPER_INTELLIGENCE_CAT_CAN = registerAttributeCan(
            "super_intelligence_cat_can", CatStat.INTELLIGENCE, CatAttributeCanItem.Tier.SUPER);
    public static final DeferredItem<CatAttributeCanItem> SUPER_LUCK_CAT_CAN = registerAttributeCan(
            "super_luck_cat_can", CatStat.LUCK, CatAttributeCanItem.Tier.SUPER);
    public static final DeferredItem<CatTraitFishItem> DRIED_FISH = ITEMS.register("dried_fish",
            () -> new CatTraitFishItem(new Item.Properties().food(
                    new FoodProperties.Builder().nutrition(5).saturationModifier(0.6F).build()),
                    CatTraitFishItem.Tier.NORMAL));
    public static final DeferredItem<CatTraitFishItem> GOLDEN_DRIED_FISH = ITEMS.register(
            "golden_dried_fish", () -> new CatTraitFishItem(new Item.Properties().food(
                    new FoodProperties.Builder().nutrition(6).saturationModifier(0.8F).build()),
                    CatTraitFishItem.Tier.GOLDEN));
    public static final DeferredItem<CatTraitFishItem> SUPER_DRIED_FISH = ITEMS.register(
            "super_dried_fish", () -> new CatTraitFishItem(new Item.Properties(),
                    CatTraitFishItem.Tier.SUPER));
    public static final DeferredItem<BreedingCatFoodItem> BREEDING_CAT_FOOD = ITEMS.register(
            "breeding_cat_food", () -> new BreedingCatFoodItem(
                    new Item.Properties(), CatBreedingMode.NORMAL));
    public static final DeferredItem<BreedingCatFoodItem> MUTATION_CAT_FOOD = ITEMS.register(
            "mutation_cat_food", () -> new BreedingCatFoodItem(
                    new Item.Properties(), CatBreedingMode.MUTATION));
    public static final DeferredItem<BreedingCatFoodItem> ATTACK_BREEDING_CAT_FOOD = ITEMS.register(
            "attack_breeding_cat_food", () -> new BreedingCatFoodItem(
                    new Item.Properties(), CatBreedingMode.ATTACK));
    public static final DeferredItem<BreedingCatFoodItem> HEALTH_BREEDING_CAT_FOOD = ITEMS.register(
            "health_breeding_cat_food", () -> new BreedingCatFoodItem(
                    new Item.Properties(), CatBreedingMode.HEALTH));
    public static final DeferredItem<BreedingCatFoodItem> SPEED_BREEDING_CAT_FOOD = ITEMS.register(
            "speed_breeding_cat_food", () -> new BreedingCatFoodItem(
                    new Item.Properties(), CatBreedingMode.SPEED));
    public static final DeferredItem<BreedingCatFoodItem> STAMINA_BREEDING_CAT_FOOD = ITEMS.register(
            "stamina_breeding_cat_food", () -> new BreedingCatFoodItem(
                    new Item.Properties(), CatBreedingMode.STAMINA));
    public static final DeferredItem<BreedingCatFoodItem> INTELLIGENCE_BREEDING_CAT_FOOD = ITEMS.register(
            "intelligence_breeding_cat_food", () -> new BreedingCatFoodItem(
                    new Item.Properties(), CatBreedingMode.INTELLIGENCE));
    public static final DeferredItem<BreedingCatFoodItem> LUCK_BREEDING_CAT_FOOD = ITEMS.register(
            "luck_breeding_cat_food", () -> new BreedingCatFoodItem(
                    new Item.Properties(), CatBreedingMode.LUCK));
    public static final DeferredItem<CatGrenadeItem> CAT_GRENADE = ITEMS.register("cat_grenade",
            () -> new CatGrenadeItem(new Item.Properties()));
    public static final DeferredItem<Item> CAT_SHELL = ITEMS.register("cat_shell",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> INCOMPLETE_CAT_GRENADE = ITEMS.register("incomplete_cat_grenade",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CAT_POWDER = ITEMS.register("cat_powder",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CAT_DOUGH = ITEMS.register("cat_dough",
            () -> new Item(new Item.Properties()));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> CAT_ARMOR_MATERIAL =
            ARMOR_MATERIALS.register("cat", LaoWuMod::createCatArmorMaterial);
    public static final DeferredItem<KimiArmorItem> CAT_HELMET = ITEMS.register("cat_helmet",
            () -> new KimiArmorItem(CAT_ARMOR_MATERIAL, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(Items.DIAMOND_HELMET.getDefaultInstance().getMaxDamage())));
    public static final DeferredItem<KimiArmorItem> CAT_CHESTPLATE = ITEMS.register("cat_chestplate",
            () -> new KimiArmorItem(CAT_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(Items.DIAMOND_CHESTPLATE.getDefaultInstance().getMaxDamage())));
    public static final DeferredItem<KimiArmorItem> CAT_LEGGINGS = ITEMS.register("cat_leggings",
            () -> new KimiArmorItem(CAT_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(Items.DIAMOND_LEGGINGS.getDefaultInstance().getMaxDamage())));
    public static final DeferredItem<KimiArmorItem> CAT_BOOTS = ITEMS.register("cat_boots",
            () -> new KimiArmorItem(CAT_ARMOR_MATERIAL, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(Items.DIAMOND_BOOTS.getDefaultInstance().getMaxDamage())));
    public static final DeferredHolder<MobEffect, HissingAttackEffect> HISSING_ATTACK = MOB_EFFECTS.register(
            "hissing_attack", HissingAttackEffect::new);
    /** Three minutes; amplifier zero is potion level I. */
    public static final DeferredHolder<Potion, Potion> HISSING_POTION = POTIONS.register("hissing",
            () -> new Potion("hissing",
                    new MobEffectInstance(HISSING_ATTACK, 20 * 60 * 3, 0)));
    public static final DeferredHolder<Potion, Potion> STRONG_HISSING_POTION = POTIONS.register("strong_hissing",
            () -> new Potion("hissing",
                    new MobEffectInstance(HISSING_ATTACK, 20 * 60 * 3, 1)));
    public static final DeferredHolder<Potion, Potion> POWERFUL_HISSING_POTION = POTIONS.register("powerful_hissing",
            () -> new Potion("hissing",
                    new MobEffectInstance(HISSING_ATTACK, 20 * 60 * 3, 2)));
    public static final DeferredItem<CatEngineBlockItem> CAT_ENGINE_ITEM = ITEMS.register("cat_engine",
            () -> new CatEngineBlockItem(CAT_ENGINE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> INFILTRATION_TANK_ITEM = ITEMS.register("infiltration_tank",
            () -> new BlockItem(INFILTRATION_TANK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> HISSING_COLLECTOR_ITEM = ITEMS.register("hissing_collector",
            () -> new BlockItem(HISSING_COLLECTOR.get(), new Item.Properties()));
    public static final DeferredItem<DevouringCatBlockItem> DEVOURING_CAT_ITEM = ITEMS.register("devouring_cat",
            () -> new DevouringCatBlockItem(DEVOURING_CAT.get(), new Item.Properties()));
    public static final DeferredItem<BreedingBoxBlockItem> BASIC_BREEDING_BOX_ITEM = ITEMS.register("basic_breeding_box",
            () -> new BreedingBoxBlockItem(BASIC_BREEDING_BOX.get(), new Item.Properties()));
    public static final DeferredItem<BreedingBoxBlockItem> INTERMEDIATE_BREEDING_BOX_ITEM = ITEMS.register("intermediate_breeding_box",
            () -> new BreedingBoxBlockItem(INTERMEDIATE_BREEDING_BOX.get(), new Item.Properties()));
    public static final DeferredItem<BreedingBoxBlockItem> ADVANCED_BREEDING_BOX_ITEM = ITEMS.register("advanced_breeding_box",
            () -> new BreedingBoxBlockItem(ADVANCED_BREEDING_BOX.get(), new Item.Properties()));
    public static final DeferredItem<AdoptionBoxBlockItem> ADOPTION_BOX_ITEM = ITEMS.register("adoption_box",
            () -> new AdoptionBoxBlockItem(ADOPTION_BOX.get(), new Item.Properties()));
    public static final DeferredHolder<FluidType, HissingGasFluidType> HISSING_GAS_TYPE = FLUID_TYPES.register(
            "hissing_gas", HissingGasFluidType::new);
    public static final DeferredHolder<Fluid, FlowingFluid> HISSING_GAS = FLUIDS.register(
            "hissing_gas", () -> new BaseFlowingFluid.Source(hissingGasProperties()));
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_HISSING_GAS = FLUIDS.register(
            "flowing_hissing_gas", () -> new BaseFlowingFluid.Flowing(hissingGasProperties()));
    public static final DeferredItem<HissingGasBucketItem> HISSING_GAS_BUCKET = ITEMS.register("hissing_gas_bucket",
            () -> new HissingGasBucketItem(HISSING_GAS,
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final DeferredHolder<FluidType, LiquidCatFluidType> LIQUID_CAT_TYPE = FLUID_TYPES.register(
            "liquid_cat", LiquidCatFluidType::new);
    public static final DeferredHolder<Fluid, FlowingFluid> LIQUID_CAT = FLUIDS.register(
            "liquid_cat", () -> new BaseFlowingFluid.Source(liquidCatProperties()));
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_LIQUID_CAT = FLUIDS.register(
            "flowing_liquid_cat", () -> new BaseFlowingFluid.Flowing(liquidCatProperties()));
    public static final DeferredBlock<LiquidBlock> LIQUID_CAT_BLOCK = BLOCKS.register("liquid_cat",
            () -> new LiquidBlock(LIQUID_CAT.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.LAVA)
                    .mapColor(MapColor.COLOR_YELLOW)
                    .lightLevel(state -> 0)));
    public static final DeferredItem<BucketItem> LIQUID_CAT_BUCKET = ITEMS.register("liquid_cat_bucket",
            () -> new BucketItem(LIQUID_CAT.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> LAOWU_TAB = CREATIVE_TABS.register("laowu",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.laowu"))
                    .icon(() -> CAT_ENGINE_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(CAT_ENGINE_ITEM.get());
                        output.accept(INFILTRATION_TANK_ITEM.get());
                        output.accept(HISSING_COLLECTOR_ITEM.get());
                        output.accept(DEVOURING_CAT_ITEM.get());
                        output.accept(ADOPTION_BOX_ITEM.get());
                        output.accept(CAT_BLOCK_ITEM.get());
                        output.accept(CAT_INGOT.get());
                        output.accept(CAT_SHEET.get());
                        output.accept(CAT_FUR.get());
                        output.accept(CAT_SPRING.get());
                        output.accept(CAT_GEAR.get());
                        output.accept(CAT_PELLET.get());
                        output.accept(CAT_COMPONENT.get());
                        output.accept(CatTotemItem.emptyStack(CAT_TOTEM.get()));
                        output.accept(CAT_ENGINEER_GOGGLES.get());
                        output.accept(CAT_UPGRADE_SMITHING_TEMPLATE.get());
                        output.accept(CAT_SWORD.get());
                        output.accept(CAT_PICKAXE.get());
                        output.accept(CAT_AXE.get());
                        output.accept(CAT_SHOVEL.get());
                        output.accept(CAT_HOE.get());
                        output.accept(CAT_CANNON.get());
                        output.accept(CAT_BALL.get());
                        output.accept(CAT_STRIP.get());
                        output.accept(CAT_POUCH.get());
                        output.accept(CAT_BOX.get());
                        output.accept(CAT_FOOD.get());
                        output.accept(PHEROMONE_CAT_FOOD.get());
                        output.accept(CAT_POWDER.get());
                        output.accept(CAT_DOUGH.get());
                        output.accept(CatPancakeItem.defaultDisplayStack());
                        output.accept(BUTTER_BREAD.get());
                        output.accept(BUTTER_CAT_SPAWN_EGG.get());
                        output.accept(AllItems.CARDBOARD_SWORD.get());
                        output.accept(AllBlocks.SEATS.get(DyeColor.RED).get());
                        output.accept(TERMINATOR_SUIT.get());
                        output.accept(FISHING_SUIT.get());
                        output.accept(FLIGHT_SUIT.get());
                        output.accept(FIRE_SUIT.get());
                        output.accept(HONEY_SUIT.get());
                        output.accept(TRANSPORT_SUIT.get());
                        output.accept(DYNAMITE_SUIT.get());
                        output.accept(CAT_HELMET.get());
                        output.accept(CAT_CHESTPLATE.get());
                        output.accept(CAT_LEGGINGS.get());
                        output.accept(CAT_BOOTS.get());
                        output.accept(CAT_GRENADE.get());
                        output.accept(CAT_SHELL.get());
                        output.accept(HISSING_GAS_BUCKET.get());
                        output.accept(LIQUID_CAT_BUCKET.get());
                        output.accept(PotionContents.createItemStack(Items.POTION, HISSING_POTION));
                        output.accept(PotionContents.createItemStack(Items.POTION, STRONG_HISSING_POTION));
                        output.accept(PotionContents.createItemStack(Items.POTION, POWERFUL_HISSING_POTION));
                    })
                    .build());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CAT_PROGRESSION_TAB =
            CREATIVE_TABS.register("cat_progression", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.laowu.cat_progression"))
                    .icon(() -> ADVANCED_BREEDING_BOX_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(BASIC_BREEDING_BOX_ITEM.get());
                        output.accept(INTERMEDIATE_BREEDING_BOX_ITEM.get());
                        output.accept(ADVANCED_BREEDING_BOX_ITEM.get());
                        output.accept(BREEDING_CAT_FOOD.get());
                        output.accept(MUTATION_CAT_FOOD.get());
                        output.accept(ATTACK_BREEDING_CAT_FOOD.get());
                        output.accept(HEALTH_BREEDING_CAT_FOOD.get());
                        output.accept(SPEED_BREEDING_CAT_FOOD.get());
                        output.accept(STAMINA_BREEDING_CAT_FOOD.get());
                        output.accept(INTELLIGENCE_BREEDING_CAT_FOOD.get());
                        output.accept(LUCK_BREEDING_CAT_FOOD.get());
                        output.accept(CAT_CAN.get());
                        output.accept(GOLDEN_CAT_CAN.get());
                        output.accept(ATTACK_CAT_CAN.get());
                        output.accept(HEALTH_CAT_CAN.get());
                        output.accept(SPEED_CAT_CAN.get());
                        output.accept(STAMINA_CAT_CAN.get());
                        output.accept(INTELLIGENCE_CAT_CAN.get());
                        output.accept(LUCK_CAT_CAN.get());
                        output.accept(GOLDEN_ATTACK_CAT_CAN.get());
                        output.accept(GOLDEN_HEALTH_CAT_CAN.get());
                        output.accept(GOLDEN_SPEED_CAT_CAN.get());
                        output.accept(GOLDEN_STAMINA_CAT_CAN.get());
                        output.accept(GOLDEN_INTELLIGENCE_CAT_CAN.get());
                        output.accept(GOLDEN_LUCK_CAT_CAN.get());
                        output.accept(SUPER_ATTACK_CAT_CAN.get());
                        output.accept(SUPER_HEALTH_CAT_CAN.get());
                        output.accept(SUPER_SPEED_CAT_CAN.get());
                        output.accept(SUPER_STAMINA_CAT_CAN.get());
                        output.accept(SUPER_INTELLIGENCE_CAT_CAN.get());
                        output.accept(SUPER_LUCK_CAT_CAN.get());
                        output.accept(DRIED_FISH.get());
                        output.accept(GOLDEN_DRIED_FISH.get());
                        output.accept(SUPER_DRIED_FISH.get());
                        output.accept(CAT_SCANNER.get());
                        output.accept(CAT_FILTER.get());
                        output.accept(CAT_ENGINEER_GOGGLES.get());
                        output.accept(FUSION_DEBUG_WAND.get());
                        output.accept(ATTRIBUTE_DEBUG_WAND.get());
                        output.accept(TRAIT_DEBUG_WAND.get());
                        output.accept(MATERIAL_DEBUG_WAND.get());
                    })
                    .build());
    public static final DeferredHolder<MenuType<?>, MenuType<CatPackageMenu>> CAT_PACKAGE_MENU = MENUS.register(
            "cat_package", () -> IMenuTypeExtension.create(CatPackageMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<BreedingBoxMenu>> BREEDING_BOX_MENU = MENUS.register(
            "breeding_box", () -> IMenuTypeExtension.create(BreedingBoxMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<AdoptionBoxMenu>> ADOPTION_BOX_MENU = MENUS.register(
            "adoption_box", () -> IMenuTypeExtension.create(AdoptionBoxMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<CatTraitEditorMenu>> CAT_TRAIT_EDITOR_MENU =
            MENUS.register("cat_trait_editor",
                    () -> IMenuTypeExtension.create(CatTraitEditorMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<CatAttributeEditorMenu>> CAT_ATTRIBUTE_EDITOR_MENU =
            MENUS.register("cat_attribute_editor",
                    () -> IMenuTypeExtension.create(CatAttributeEditorMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<CatProfileMenu>> CAT_PROFILE_MENU =
            MENUS.register("cat_profile",
                    () -> IMenuTypeExtension.create(CatProfileMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<CatFilterMenu>> CAT_FILTER_MENU =
            MENUS.register("cat_filter", () -> IMenuTypeExtension.create(CatFilterMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<CatMaterialEditorMenu>> CAT_MATERIAL_EDITOR_MENU =
            MENUS.register("cat_material_editor", () -> IMenuTypeExtension.create(CatMaterialEditorMenu::new));
    public static final DeferredHolder<EntityType<?>, EntityType<CatPancakeProjectile>> CAT_PANCAKE_PROJECTILE =
            ENTITY_TYPES.register("cat_pancake_projectile", () -> EntityType.Builder
                    .<CatPancakeProjectile>of(CatPancakeProjectile::new, MobCategory.MISC)
                    .sized(0.55F, 0.18F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("cat_pancake_projectile"));
    public static final DeferredHolder<EntityType<?>, EntityType<FishingRodProjectile>> FISHING_ROD_PROJECTILE =
            ENTITY_TYPES.register("fishing_rod_projectile", () -> EntityType.Builder
                    .<FishingRodProjectile>of(FishingRodProjectile::new, MobCategory.MISC)
                    .sized(0.28F, 0.28F).clientTrackingRange(10).updateInterval(1)
                    .build("fishing_rod_projectile"));
    public static final DeferredHolder<EntityType<?>, EntityType<MechanicalLaserProjectile>>
            MECHANICAL_LASER_PROJECTILE = ENTITY_TYPES.register("mechanical_laser_projectile",
                    () -> EntityType.Builder
                            .<MechanicalLaserProjectile>of(MechanicalLaserProjectile::new, MobCategory.MISC)
                            .sized(0.14F, 0.14F).clientTrackingRange(18).updateInterval(1)
                            .build("mechanical_laser_projectile"));
    public static final DeferredHolder<EntityType<?>, EntityType<HoneyMissileProjectile>>
            HONEY_MISSILE_PROJECTILE = ENTITY_TYPES.register("honey_missile_projectile",
                    () -> EntityType.Builder
                            .<HoneyMissileProjectile>of(HoneyMissileProjectile::new, MobCategory.MISC)
                            .sized(0.28F, 0.28F).clientTrackingRange(14).updateInterval(1)
                            .build("honey_missile_projectile"));
    public static final DeferredHolder<EntityType<?>, EntityType<DynamiteProjectile>>
            DYNAMITE_PROJECTILE = ENTITY_TYPES.register("dynamite_projectile",
                    () -> EntityType.Builder
                            .<DynamiteProjectile>of(DynamiteProjectile::new, MobCategory.MISC)
                            .sized(0.30F, 0.30F).clientTrackingRange(12).updateInterval(1)
                            .build("dynamite_projectile"));
    public static final DeferredHolder<EntityType<?>, EntityType<LogisticsSupportProjectile>>
            LOGISTICS_SUPPORT_PROJECTILE = ENTITY_TYPES.register("logistics_support_projectile",
                    () -> EntityType.Builder
                            .<LogisticsSupportProjectile>of(LogisticsSupportProjectile::new, MobCategory.MISC)
                            .sized(0.42F, 0.32F).clientTrackingRange(14).updateInterval(1)
                            .build("logistics_support_projectile"));
    public static final DeferredHolder<EntityType<?>, EntityType<CatBallEntity>> CAT_BALL_ENTITY =
            ENTITY_TYPES.register("cat_ball", () -> EntityType.Builder
                    .<CatBallEntity>of(CatBallEntity::new, MobCategory.MISC)
                    // Exact rotated model bounds, enlarged uniformly by 1.4x.
                    .sized(CatBallEntity.MODEL_DIAMETER_PIXELS * CatBallEntity.WORLD_SCALE / 16.0F,
                            CatBallEntity.MODEL_HEIGHT_PIXELS * CatBallEntity.WORLD_SCALE / 16.0F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("cat_ball"));
    public static final DeferredHolder<RecipeType<?>, RecipeType<InfiltratingRecipe>> INFILTRATING_TYPE =
            RECIPE_TYPES.register("infiltrating", () -> new RecipeType<>() {
                @Override public String toString() { return MOD_ID + ":infiltrating"; }
            });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<InfiltratingRecipe>> INFILTRATING_SERIALIZER =
            RECIPE_SERIALIZERS.register("infiltrating",
                    () -> new StandardProcessingRecipe.Serializer<>(InfiltratingRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RandomBabyCatPancakeFillingRecipe>>
            RANDOM_BABY_CAT_PANCAKE_FILLING_SERIALIZER =
            RECIPE_SERIALIZERS.register("random_baby_cat_pancake_filling",
                    () -> new StandardProcessingRecipe.Serializer<>(
                            RandomBabyCatPancakeFillingRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PheromoneCatFoodMixingRecipe>>
            PHEROMONE_CAT_FOOD_MIXING_SERIALIZER = RECIPE_SERIALIZERS.register(
                    "pheromone_cat_food_mixing",
                    () -> new StandardProcessingRecipe.Serializer<>(PheromoneCatFoodMixingRecipe::new));
    public static final DeferredHolder<IngredientType<?>, IngredientType<CatPancakeIngredient>> CAT_PANCAKE_INGREDIENT =
            INGREDIENT_TYPES.register("cat_pancake", CatPancakeIngredient::createType);
    public static final DeferredHolder<IngredientType<?>, IngredientType<CatPancakeVariantIngredient>>
            CAT_PANCAKE_VARIANT_INGREDIENT = INGREDIENT_TYPES.register(
            "cat_pancake_variant", CatPancakeVariantIngredient::createType);
    public static final DeferredHolder<IngredientType<?>, IngredientType<AnyBlockIngredient>>
            ANY_BLOCK_INGREDIENT = INGREDIENT_TYPES.register("any_block", AnyBlockIngredient::createType);
    public static final DeferredHolder<IngredientType<?>, IngredientType<NamedPlayerNameTagIngredient>>
            NAMED_PLAYER_NAME_TAG_INGREDIENT = INGREDIENT_TYPES.register(
                    "named_player_name_tag", NamedPlayerNameTagIngredient::createType);

    public LaoWuMod(IEventBus modBus, ModContainer modContainer) {
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        ENTITY_TYPES.register(modBus);
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
        MOB_EFFECTS.register(modBus);
        POTIONS.register(modBus);
        PARTICLE_TYPES.register(modBus);
        ARMOR_MATERIALS.register(modBus);
        CREATIVE_TABS.register(modBus);
        RECIPE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        INGREDIENT_TYPES.register(modBus);
        LOOT_MODIFIERS.register(modBus);
        modBus.addListener(LaoWuMod::registerCapabilities);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "laowu-client.toml");
        NeoForge.EVENT_BUS.register(CommonEvents.class);
        ModNetwork.register(modBus);
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                event.enqueueWork(() -> {
                    DispenserBlock.registerProjectileBehavior(CAT_PANCAKE.get());
                    BlockStressValues.CAPACITIES.register(CAT_ENGINE.get(),
                            () -> (double) CatEngineBlockEntity.STRESS_CAPACITY_PER_RPM);
                    BlockStressValues.setGeneratorSpeed((int) CatEngineBlockEntity.GENERATED_RPM)
                            .accept(CAT_ENGINE.get());
                    // Use Create's own kinetic tooltip, identical to diesel_engine:
                    // capacity title, 64x RPM and the maximum 6144 SU line.
                    TooltipModifier.REGISTRY.register(CAT_ENGINE_ITEM.get(),
                            createAlwaysVisibleDescription(CAT_ENGINE_ITEM.get())
                                    .andThen(new KineticStats(CAT_ENGINE.get())));
                    registerAlwaysVisibleDescription(INFILTRATION_TANK_ITEM.get());
                    registerAlwaysVisibleDescription(HISSING_COLLECTOR_ITEM.get());
                    registerAlwaysVisibleDescription(DEVOURING_CAT_ITEM.get());
                    registerAlwaysVisibleDescription(BASIC_BREEDING_BOX_ITEM.get());
                    registerAlwaysVisibleDescription(INTERMEDIATE_BREEDING_BOX_ITEM.get());
                    registerAlwaysVisibleDescription(ADVANCED_BREEDING_BOX_ITEM.get());
                    registerAlwaysVisibleDescription(ADOPTION_BOX_ITEM.get());
                    registerDescription(CAT_CANNON.get());
                    registerDescription(CAT_BALL.get());
                    registerDescription(CAT_STRIP.get());
                    registerAlwaysVisibleDescription(CAT_FOOD.get());
                    registerAlwaysVisibleDescription(CAT_CAN.get());
                    registerAlwaysVisibleDescription(GOLDEN_CAT_CAN.get());
                    registerAlwaysVisibleDescription(ATTACK_CAT_CAN.get());
                    registerAlwaysVisibleDescription(HEALTH_CAT_CAN.get());
                    registerAlwaysVisibleDescription(SPEED_CAT_CAN.get());
                    registerAlwaysVisibleDescription(STAMINA_CAT_CAN.get());
                    registerAlwaysVisibleDescription(INTELLIGENCE_CAT_CAN.get());
                    registerAlwaysVisibleDescription(LUCK_CAT_CAN.get());
                    registerAlwaysVisibleDescription(GOLDEN_ATTACK_CAT_CAN.get());
                    registerAlwaysVisibleDescription(GOLDEN_HEALTH_CAT_CAN.get());
                    registerAlwaysVisibleDescription(GOLDEN_SPEED_CAT_CAN.get());
                    registerAlwaysVisibleDescription(GOLDEN_STAMINA_CAT_CAN.get());
                    registerAlwaysVisibleDescription(GOLDEN_INTELLIGENCE_CAT_CAN.get());
                    registerAlwaysVisibleDescription(GOLDEN_LUCK_CAT_CAN.get());
                    registerAlwaysVisibleDescription(SUPER_ATTACK_CAT_CAN.get());
                    registerAlwaysVisibleDescription(SUPER_HEALTH_CAT_CAN.get());
                    registerAlwaysVisibleDescription(SUPER_SPEED_CAT_CAN.get());
                    registerAlwaysVisibleDescription(SUPER_STAMINA_CAT_CAN.get());
                    registerAlwaysVisibleDescription(SUPER_INTELLIGENCE_CAT_CAN.get());
                    registerAlwaysVisibleDescription(SUPER_LUCK_CAT_CAN.get());
                    registerAlwaysVisibleDescription(DRIED_FISH.get());
                    registerAlwaysVisibleDescription(GOLDEN_DRIED_FISH.get());
                    registerAlwaysVisibleDescription(SUPER_DRIED_FISH.get());
                    registerAlwaysVisibleDescription(BREEDING_CAT_FOOD.get());
                    registerAlwaysVisibleDescription(MUTATION_CAT_FOOD.get());
                    registerAlwaysVisibleDescription(ATTACK_BREEDING_CAT_FOOD.get());
                    registerAlwaysVisibleDescription(HEALTH_BREEDING_CAT_FOOD.get());
                    registerAlwaysVisibleDescription(SPEED_BREEDING_CAT_FOOD.get());
                    registerAlwaysVisibleDescription(STAMINA_BREEDING_CAT_FOOD.get());
                    registerAlwaysVisibleDescription(INTELLIGENCE_BREEDING_CAT_FOOD.get());
                    registerAlwaysVisibleDescription(LUCK_BREEDING_CAT_FOOD.get());
                    registerAlwaysVisibleDescription(CAT_SCANNER.get());
                    registerDescription(CAT_ENGINEER_GOGGLES.get());
                    GogglesItem.addIsWearingPredicate(CatEngineerGogglesItem::isWornBy);
                    if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
                        cn.laowu.mod.compat.curios.CatGogglesCuriosCompat.registerCurio();
                    }
                    registerDescription(CAT_HELMET.get());
                    registerDescription(CAT_CHESTPLATE.get());
                    registerDescription(CAT_LEGGINGS.get());
                    registerDescription(CAT_BOOTS.get());
                    registerDescription(CAT_SWORD.get());
                    registerDescription(CAT_PICKAXE.get());
                    registerDescription(CAT_AXE.get());
                    registerDescription(CAT_SHOVEL.get());
                    registerDescription(CAT_HOE.get());
                }));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CAT_ENGINE_BE.get(),
                CatEngineBlockEntity::getFluidHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, INFILTRATION_TANK_BE.get(),
                InfiltrationTankBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, INFILTRATION_TANK_BE.get(),
                InfiltrationTankBlockEntity::getFluidHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, HISSING_COLLECTOR_BE.get(),
                HissingCollectorBlockEntity::getFluidHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DEVOURING_CAT_BE.get(),
                DevouringCatBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DEVOURING_CAT_BE.get(),
                DevouringCatBlockEntity::getFluidHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BREEDING_BOX_BE.get(),
                BreedingBoxBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ADOPTION_BOX_BE.get(),
                AdoptionBoxBlockEntity::getItemHandler);
    }

    private static TooltipModifier createDescription(Item item) {
        return new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE);
    }

    private static void registerDescription(Item item) {
        TooltipModifier.REGISTRY.register(item, createDescription(item));
    }

    /** A Create-formatted summary without the usual hold-Shift gate. */
    private static TooltipModifier createAlwaysVisibleDescription(Item item) {
        return event -> {
            String summary = Component.translatable(
                    item.getDescriptionId() + ".tooltip.summary").getString();
            event.getToolTip().addAll(Math.min(1, event.getToolTip().size()),
                    TooltipHelper.cutStringTextComponent(
                            summary, FontHelper.Palette.STANDARD_CREATE));
        };
    }

    private static void registerAlwaysVisibleDescription(Item item) {
        TooltipModifier.REGISTRY.register(item, createAlwaysVisibleDescription(item));
    }

    private static DeferredItem<CatAttributeCanItem> registerAttributeCan(
            String name, CatStat stat, CatAttributeCanItem.Tier tier) {
        return ITEMS.register(name,
                () -> new CatAttributeCanItem(new Item.Properties(), stat, tier));
    }

    private static ArmorMaterial createCatArmorMaterial() {
        ArmorMaterial iron = ArmorMaterials.DIAMOND.value();
        return new ArmorMaterial(
                iron.defense(),
                iron.enchantmentValue(),
                iron.equipSound(),
                iron.repairIngredient(),
                List.of(new ArmorMaterial.Layer(id("kimi_armor"))),
                iron.toughness(),
                iron.knockbackResistance());
    }

    private static BaseFlowingFluid.Properties hissingGasProperties() {
        // Intentionally omit .block(...). Create detects the resulting AIR
        // legacy state and consumes exposed pipe output as vapour. The custom
        // bucket is referenced separately so it can exchange with tanks without
        // gaining normal world-placement behaviour.
        return new BaseFlowingFluid.Properties(HISSING_GAS_TYPE, HISSING_GAS, FLOWING_HISSING_GAS)
                .bucket(HISSING_GAS_BUCKET)
                .slopeFindDistance(1)
                .levelDecreasePerBlock(1)
                .tickRate(5)
                .explosionResistance(0.0F);
    }

    private static BaseFlowingFluid.Properties liquidCatProperties() {
        // Match lava's overworld flow cadence and horizontal reach while
        // remaining a distinct, non-luminous and non-renewable NeoForge fluid.
        return new BaseFlowingFluid.Properties(LIQUID_CAT_TYPE, LIQUID_CAT, FLOWING_LIQUID_CAT)
                .block(LIQUID_CAT_BLOCK)
                .bucket(LIQUID_CAT_BUCKET)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .tickRate(30)
                .explosionResistance(100.0F);
    }

}
