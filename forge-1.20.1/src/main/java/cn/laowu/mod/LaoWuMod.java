package cn.laowu.mod;

import cn.laowu.mod.item.CatPancakeItem;
import cn.laowu.mod.item.CatCannonItem;
import cn.laowu.mod.item.CatSmithingTemplateItem;
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
import cn.laowu.mod.item.AttributeDebugWandItem;
import cn.laowu.mod.item.TraitDebugWandItem;
import cn.laowu.mod.item.MaterialDebugWandItem;
import cn.laowu.mod.item.CatGrenadeItem;
import cn.laowu.mod.item.CatEngineBlockItem;
import cn.laowu.mod.item.DevouringCatBlockItem;
import cn.laowu.mod.item.KimiArmorItem;
import cn.laowu.mod.item.TerminatorSuitItem;
import cn.laowu.mod.item.CatEngineerGogglesItem;
import cn.laowu.mod.item.CatHoeItem;
import cn.laowu.mod.item.CatFurItem;
import cn.laowu.mod.item.CatTotemItem;
import cn.laowu.mod.item.FusionDebugWandItem;
import cn.laowu.mod.item.BreedingBoxBlockItem;
import cn.laowu.mod.item.AdoptionBoxBlockItem;
import cn.laowu.mod.item.WishAdoptionBoxBlockItem;
import cn.laowu.mod.item.CatScannerItem;
import cn.laowu.mod.item.CatFilterItem;
import cn.laowu.mod.item.CreatureFilterItem;
import cn.laowu.mod.item.ButterBreadItem;
import cn.laowu.mod.client.CareerSuitTooltip;
import cn.laowu.mod.genetics.CatBreedingMode;
import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.loot.CatToolEmpoweredLootModifier;
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
import cn.laowu.mod.network.ModNetwork;
import cn.laowu.mod.particle.NozzleFluidPuffData;
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
import cn.laowu.mod.create.WishAdoptionBoxBlock;
import cn.laowu.mod.create.WishAdoptionBoxBlockEntity;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleType;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeSerializer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.common.ForgeSpawnEggItem;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;

@Mod(LaoWuMod.MOD_ID)
public final class LaoWuMod {
    public static final String MOD_ID = "laowu";
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    private static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, MOD_ID);
    private static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MOD_ID);
    private static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, MOD_ID);
    private static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MOD_ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MOD_ID);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, MOD_ID);
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MOD_ID);
    public static final RegistryObject<Codec<CatToolEmpoweredLootModifier>> CAT_TOOL_EMPOWERED_LOOT =
            LOOT_MODIFIERS.register("cat_tool_empowered", () -> CatToolEmpoweredLootModifier.CODEC);
    public static final RegistryObject<net.minecraft.core.particles.SimpleParticleType> CAT_HEALING_SMOKE =
            PARTICLE_TYPES.register("cat_healing_smoke", () -> new net.minecraft.core.particles.SimpleParticleType(false));
    public static final RegistryObject<net.minecraft.core.particles.SimpleParticleType> CAT_AGENT_SMOKE =
            PARTICLE_TYPES.register("cat_agent_smoke", () -> new net.minecraft.core.particles.SimpleParticleType(false));
    public static final RegistryObject<ParticleType<NozzleFluidPuffData>> NOZZLE_FLUID_PUFF =
            cn.laowu.mod.compat.create.CreateIntegration.register(PARTICLE_TYPES, "nozzle_fluid_puff", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::particle_types_nozzle_fluid_puff : null);

    public static final RegistryObject<Block> CAT_BLOCK = BLOCKS.register("cat_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DIAMOND_BLOCK)));

    public static final RegistryObject<Block> CAT_ENGINE = cn.laowu.mod.compat.create.CreateIntegration.registerBlock(BLOCKS, "cat_engine",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::blocks_cat_engine : null);
    public static final RegistryObject<BlockEntityType<CatEngineBlockEntity>> CAT_ENGINE_BE =
            cn.laowu.mod.compat.create.CreateIntegration.register(BLOCK_ENTITIES, "cat_engine", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::block_entities_cat_engine : null);
    public static final RegistryObject<Block> INFILTRATION_TANK = cn.laowu.mod.compat.create.CreateIntegration.registerBlock(BLOCKS, "infiltration_tank",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::blocks_infiltration_tank : null);
    public static final RegistryObject<BlockEntityType<InfiltrationTankBlockEntity>> INFILTRATION_TANK_BE =
            cn.laowu.mod.compat.create.CreateIntegration.register(BLOCK_ENTITIES, "infiltration_tank", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::block_entities_infiltration_tank : null);
    public static final RegistryObject<Block> HISSING_COLLECTOR = cn.laowu.mod.compat.create.CreateIntegration.registerBlock(BLOCKS, "hissing_collector",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::blocks_hissing_collector : null);
    public static final RegistryObject<BlockEntityType<HissingCollectorBlockEntity>> HISSING_COLLECTOR_BE =
            cn.laowu.mod.compat.create.CreateIntegration.register(BLOCK_ENTITIES, "hissing_collector", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::block_entities_hissing_collector : null);
    public static final RegistryObject<Block> DEVOURING_CAT = cn.laowu.mod.compat.create.CreateIntegration.registerBlock(BLOCKS, "devouring_cat",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::blocks_devouring_cat : null);
    public static final RegistryObject<BlockEntityType<DevouringCatBlockEntity>> DEVOURING_CAT_BE =
            cn.laowu.mod.compat.create.CreateIntegration.register(BLOCK_ENTITIES, "devouring_cat", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::block_entities_devouring_cat : null);
    public static final RegistryObject<Block> BASIC_BREEDING_BOX = cn.laowu.mod.compat.create.CreateIntegration.registerBlock(BLOCKS, "basic_breeding_box", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::blocks_basic_breeding_box : null);
    public static final RegistryObject<Block> INTERMEDIATE_BREEDING_BOX = cn.laowu.mod.compat.create.CreateIntegration.registerBlock(BLOCKS, "intermediate_breeding_box", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::blocks_intermediate_breeding_box : null);
    public static final RegistryObject<Block> ADVANCED_BREEDING_BOX = cn.laowu.mod.compat.create.CreateIntegration.registerBlock(BLOCKS, "advanced_breeding_box", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::blocks_advanced_breeding_box : null);
    public static final RegistryObject<BlockEntityType<BreedingBoxBlockEntity>> BREEDING_BOX_BE =
            cn.laowu.mod.compat.create.CreateIntegration.register(BLOCK_ENTITIES, "breeding_box", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::block_entities_breeding_box : null);
    public static final RegistryObject<Block> CAT_CARRIER = cn.laowu.mod.compat.create.CreateIntegration.registerBlock(BLOCKS, "cat_carrier",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::blocks_cat_carrier : null);
    public static final RegistryObject<BlockEntityType<cn.laowu.mod.create.CatCarrierBlockEntity>> CAT_CARRIER_BE =
            cn.laowu.mod.compat.create.CreateIntegration.register(BLOCK_ENTITIES, "cat_carrier", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::block_entities_cat_carrier : null);
    public static final RegistryObject<Item> CAT_CARRIER_ITEM = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "cat_carrier",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_cat_carrier : null);
    public static final RegistryObject<Block> CAT_EDITOR = cn.laowu.mod.compat.create.CreateIntegration.registerBlock(BLOCKS, "cat_editor", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::blocks_cat_editor : null);
    public static final RegistryObject<Item> CAT_EDITOR_ITEM = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "cat_editor", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_cat_editor : null);
    public static final RegistryObject<Item> CAT_TRAIT_TOKEN = ITEMS.register("cat_trait_token", () -> new cn.laowu.mod.item.CatTraitTokenItem(new Item.Properties()));
    public static final RegistryObject<MenuType<CatEditorMenu>> CAT_EDITOR_MENU = cn.laowu.mod.compat.create.CreateIntegration.register(MENUS, "cat_editor", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::menus_cat_editor : null);
    public static final RegistryObject<Block> CAT_DEPLOYMENT_PLATFORM = cn.laowu.mod.compat.create.CreateIntegration.registerBlock(BLOCKS, "cat_deployment_platform",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::blocks_cat_deployment_platform : null);
    public static final RegistryObject<Block> CAT_EJECTING_DEPLOYMENT_PLATFORM = cn.laowu.mod.compat.create.CreateIntegration.registerBlock(BLOCKS, "cat_ejecting_deployment_platform",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::blocks_cat_ejecting_deployment_platform : null);
    public static final RegistryObject<BlockEntityType<cn.laowu.mod.create.CatDeploymentBlockEntity>> CAT_DEPLOYMENT_BE = cn.laowu.mod.compat.create.CreateIntegration.register(BLOCK_ENTITIES, "cat_deployment_platform",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::block_entities_cat_deployment_platform : null);
    public static final RegistryObject<Item> CAT_DEPLOYMENT_PLATFORM_ITEM = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "cat_deployment_platform",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_cat_deployment_platform : null);
    public static final RegistryObject<Item> CAT_EJECTING_DEPLOYMENT_PLATFORM_ITEM = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "cat_ejecting_deployment_platform",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_cat_ejecting_deployment_platform : null);
    public static final RegistryObject<Item> CAT_LASER_POINTER = ITEMS.register("cat_laser_pointer",
            () -> new cn.laowu.mod.item.CatLaserPointerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CAT_STORAGE_BOX = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "cat_storage_box",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_cat_storage_box : null);

    public static final RegistryObject<Block> ADOPTION_BOX = cn.laowu.mod.compat.create.CreateIntegration.registerBlock(BLOCKS, "adoption_box",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::blocks_adoption_box : null);
    public static final RegistryObject<BlockEntityType<AdoptionBoxBlockEntity>> ADOPTION_BOX_BE =
            cn.laowu.mod.compat.create.CreateIntegration.register(BLOCK_ENTITIES, "adoption_box", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::block_entities_adoption_box : null);
    public static final RegistryObject<Block> WISH_ADOPTION_BOX = cn.laowu.mod.compat.create.CreateIntegration.registerBlock(BLOCKS, "wish_adoption_box",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::blocks_wish_adoption_box : null);
    public static final RegistryObject<BlockEntityType<WishAdoptionBoxBlockEntity>> WISH_ADOPTION_BOX_BE =
            cn.laowu.mod.compat.create.CreateIntegration.register(BLOCK_ENTITIES, "wish_adoption_box", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::block_entities_wish_adoption_box : null);
    public static final RegistryObject<EntityType<cn.laowu.mod.entity.GiantCatBoss>> GIANT_CAT_BOSS =
            ENTITY_TYPES.register("giant_cat_boss", () -> EntityType.Builder
                    .<cn.laowu.mod.entity.GiantCatBoss>of(cn.laowu.mod.entity.GiantCatBoss::new, MobCategory.MONSTER)
                    .sized(2.0F, 3.0F).clientTrackingRange(16).updateInterval(1).build("giant_cat_boss"));
    public static final RegistryObject<Item> GIANT_CAT_TREAT =
            ITEMS.register("giant_cat_treat", () -> new cn.laowu.mod.item.GiantCatTreatItem(new Item.Properties()));
    public static final RegistryObject<EntityType<ButterCatBoss>> BUTTER_CAT =
            ENTITY_TYPES.register("butter_cat", () -> EntityType.Builder
                    .<ButterCatBoss>of(ButterCatBoss::new, MobCategory.MONSTER)
                    .sized(ButterCatBoss.BASE_WIDTH * ButterCatBoss.MODEL_SCALE,
                            ButterCatBoss.BASE_HEIGHT * ButterCatBoss.MODEL_SCALE)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("butter_cat"));
    public static final RegistryObject<Item> CAT_PANCAKE = ITEMS.register("cat_pancake",
            () -> new CatPancakeItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FUSION_DEBUG_WAND = ITEMS.register("fusion_debug_wand",
            () -> new FusionDebugWandItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ATTRIBUTE_DEBUG_WAND = ITEMS.register(
            "attribute_debug_wand",
            () -> new AttributeDebugWandItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> TRAIT_DEBUG_WAND = ITEMS.register(
            "trait_debug_wand",
            () -> new TraitDebugWandItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MATERIAL_DEBUG_WAND = ITEMS.register(
            "material_debug_wand",
            () -> new MaterialDebugWandItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CAT_SCANNER = ITEMS.register("cat_scanner",
            () -> new CatScannerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CREATURE_FILTER = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "creature_filter",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_creature_filter : null);
    public static final RegistryObject<Item> CAT_FILTER = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "cat_filter",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_cat_filter : null);
    public static final RegistryObject<Item> BUTTER_BREAD = ITEMS.register("butter_bread",
            () -> new ButterBreadItem(new Item.Properties()));
    public static final RegistryObject<Item> BUTTER_CAT_SPAWN_EGG = ITEMS.register(
            "butter_cat_spawn_egg", () -> new ForgeSpawnEggItem(
                    () -> BUTTER_CAT.get(), 0xE8B94F, 0xFFF1A3,
                    new Item.Properties()));
    public static final RegistryObject<Item> TERMINATOR_SUIT = ITEMS.register("terminator_suit",
            () -> new TerminatorSuitItem(new Item.Properties(), CatOutfitType.TERMINATOR));
    public static final RegistryObject<Item> FISHING_SUIT = ITEMS.register("fishing_suit",
            () -> new TerminatorSuitItem(new Item.Properties(), CatOutfitType.FISHING));
    public static final RegistryObject<Item> FLIGHT_SUIT = ITEMS.register("flight_suit",
            () -> new TerminatorSuitItem(new Item.Properties(), CatOutfitType.FLIGHT));
    public static final RegistryObject<Item> FIRE_SUIT = ITEMS.register("fire_suit",
            () -> new TerminatorSuitItem(new Item.Properties(), CatOutfitType.FIRE));
    public static final RegistryObject<Item> HONEY_SUIT = ITEMS.register("honey_suit",
            () -> new TerminatorSuitItem(new Item.Properties(), CatOutfitType.HONEY));
    public static final RegistryObject<Item> TRANSPORT_SUIT = ITEMS.register("transport_suit",
            () -> new TerminatorSuitItem(new Item.Properties(), CatOutfitType.TRANSPORT));
    public static final RegistryObject<Item> DYNAMITE_SUIT = ITEMS.register("dynamite_suit",
            () -> new TerminatorSuitItem(new Item.Properties(), CatOutfitType.DYNAMITE));
    public static final RegistryObject<Item> ENGINEERING_SUIT = ITEMS.register("engineering_suit",
            () -> new TerminatorSuitItem(new Item.Properties(), CatOutfitType.ENGINEERING));
    public static final RegistryObject<Item> MEDICAL_SUIT = ITEMS.register("medical_suit",
            () -> new TerminatorSuitItem(new Item.Properties(), CatOutfitType.MEDICAL));
    public static final RegistryObject<Item> MUSIC_SUIT = ITEMS.register("music_suit",
            () -> new TerminatorSuitItem(new Item.Properties(), CatOutfitType.MUSIC));
    public static final RegistryObject<Item> AGENT_SUIT = ITEMS.register("agent_suit",
            () -> new TerminatorSuitItem(new Item.Properties(), CatOutfitType.AGENT));
    public static final RegistryObject<Item> DIVING_SUIT = ITEMS.register("diving_suit",
            () -> new TerminatorSuitItem(new Item.Properties(), CatOutfitType.DIVING));
    public static final RegistryObject<Item> COCKROACH_SUIT = ITEMS.register("cockroach_suit",
            () -> new TerminatorSuitItem(new Item.Properties(), CatOutfitType.COCKROACH));
    public static final RegistryObject<Item> CAT_INGOT = ITEMS.register("cat_ingot",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CAT_SHEET = ITEMS.register("cat_sheet",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CAT_FUR = ITEMS.register("cat_fur",
            () -> new CatFurItem(new Item.Properties()));
    public static final RegistryObject<Item> CAT_SPRING = ITEMS.register("cat_spring",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CAT_GEAR = ITEMS.register("cat_gear",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CAT_PELLET = ITEMS.register("cat_pellet",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CAT_COMPONENT = ITEMS.register("cat_component",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> INCOMPLETE_CAT_COMPONENT = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "incomplete_cat_component", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_incomplete_cat_component : null);
    public static final RegistryObject<Item> INCOMPLETE_TERMINATOR_SUIT = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "incomplete_terminator_suit", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_incomplete_terminator_suit : null);
    public static final RegistryObject<Item> INCOMPLETE_FISHING_SUIT = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "incomplete_fishing_suit", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_incomplete_fishing_suit : null);
    public static final RegistryObject<Item> INCOMPLETE_FLIGHT_SUIT = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "incomplete_flight_suit", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_incomplete_flight_suit : null);
    public static final RegistryObject<Item> INCOMPLETE_TRANSPORT_SUIT = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "incomplete_transport_suit", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_incomplete_transport_suit : null);
    public static final RegistryObject<Item> INCOMPLETE_FIRE_SUIT = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "incomplete_fire_suit", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_incomplete_fire_suit : null);
    public static final RegistryObject<Item> INCOMPLETE_HONEY_SUIT = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "incomplete_honey_suit", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_incomplete_honey_suit : null);
    public static final RegistryObject<Item> INCOMPLETE_DYNAMITE_SUIT = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "incomplete_dynamite_suit", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_incomplete_dynamite_suit : null);
    public static final RegistryObject<Item> INCOMPLETE_ENGINEERING_SUIT = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "incomplete_engineering_suit", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_incomplete_engineering_suit : null);
    public static final RegistryObject<Item> INCOMPLETE_MEDICAL_SUIT = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "incomplete_medical_suit", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_incomplete_medical_suit : null);
    public static final RegistryObject<Item> INCOMPLETE_MUSIC_SUIT = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "incomplete_music_suit", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_incomplete_music_suit : null);
    public static final RegistryObject<Item> INCOMPLETE_AGENT_SUIT = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "incomplete_agent_suit", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_incomplete_agent_suit : null);
    public static final RegistryObject<Item> INCOMPLETE_DIVING_SUIT = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "incomplete_diving_suit", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_incomplete_diving_suit : null);
    public static final RegistryObject<Item> INCOMPLETE_COCKROACH_SUIT = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "incomplete_cockroach_suit", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_incomplete_cockroach_suit : null);
    public static final RegistryObject<Item> CAT_TOTEM = ITEMS.register("cat_totem",
            () -> new CatTotemItem(new Item.Properties()));
    public static final RegistryObject<Item> CAT_ENGINEER_GOGGLES = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "cat_engineer_goggles",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_cat_engineer_goggles : null);
    public static final RegistryObject<Item> CAT_BLOCK_ITEM = ITEMS.register("cat_block",
            () -> new BlockItem(CAT_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> CAT_UPGRADE_SMITHING_TEMPLATE = ITEMS.register(
            "cat_upgrade_smithing_template", CatSmithingTemplateItem::new);
    public static final RegistryObject<Item> CAT_SWORD = ITEMS.register("cat_sword",
            () -> new SwordItem(Tiers.DIAMOND, 3, -2.4F,
                    new Item.Properties().durability(Items.DIAMOND_SWORD.getMaxDamage())) {
                @Override
                public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
                    return ingredient.is(CAT_INGOT.get());
                }
            });
    public static final RegistryObject<Item> CAT_PICKAXE = ITEMS.register("cat_pickaxe",
            () -> new PickaxeItem(Tiers.DIAMOND, 1, -2.8F,
                    new Item.Properties().durability(Items.DIAMOND_PICKAXE.getMaxDamage())) {
                @Override
                public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
                    return ingredient.is(CAT_INGOT.get());
                }
            });
    public static final RegistryObject<Item> CAT_AXE = ITEMS.register("cat_axe",
            () -> new AxeItem(Tiers.DIAMOND, 5.0F, -3.0F,
                    new Item.Properties().durability(Items.DIAMOND_AXE.getMaxDamage())) {
                @Override
                public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
                    return ingredient.is(CAT_INGOT.get());
                }
            });
    public static final RegistryObject<Item> CAT_SHOVEL = ITEMS.register("cat_shovel",
            () -> new ShovelItem(Tiers.DIAMOND, 1.5F, -3.0F,
                    new Item.Properties().durability(Items.DIAMOND_SHOVEL.getMaxDamage())) {
                @Override
                public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
                    return ingredient.is(CAT_INGOT.get());
                }
            });
    public static final RegistryObject<Item> CAT_HOE = ITEMS.register("cat_hoe",
            () -> new CatHoeItem(Tiers.DIAMOND, -3, 0.0F,
                    new Item.Properties().durability(Items.DIAMOND_HOE.getMaxDamage())));
    public static final RegistryObject<Item> CAT_CANNON = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "cat_cannon",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_cat_cannon : null);
    public static final RegistryObject<Item> CAT_BALL = ITEMS.register("cat_ball",
            () -> new CatBallItem(new Item.Properties()));
    public static final RegistryObject<Item> CAT_STRIP = ITEMS.register("cat_strip",
            () -> new CatStripItem(new Item.Properties()));
    public static final RegistryObject<Item> CAT_POUCH = ITEMS.register("cat_pouch",
            () -> new CatPouchItem(new Item.Properties()));
    public static final RegistryObject<Item> CAT_BOX = ITEMS.register("cat_box",
            () -> new CatGrenadeBoxItem(new Item.Properties()));
    public static final RegistryObject<Item> CAT_FOOD = ITEMS.register("cat_food",
            () -> new CatFoodItem(new Item.Properties()));
    public static final RegistryObject<Item> PHEROMONE_CAT_FOOD = ITEMS.register(
            "pheromone_cat_food", () -> new PheromoneCatFoodItem(new Item.Properties()));
    public static final RegistryObject<Item> CAT_CAN = ITEMS.register("cat_can",
            () -> new BreedingOnlyCatCanItem(new Item.Properties()));
    public static final RegistryObject<Item> GOLDEN_CAT_CAN = ITEMS.register("golden_cat_can",
            () -> new BreedingOnlyCatCanItem(new Item.Properties()));
    public static final RegistryObject<Item> ATTACK_CAT_CAN = registerAttributeCan(
            "attack_cat_can", CatStat.ATTACK, CatAttributeCanItem.Tier.NORMAL);
    public static final RegistryObject<Item> HEALTH_CAT_CAN = registerAttributeCan(
            "health_cat_can", CatStat.HEALTH, CatAttributeCanItem.Tier.NORMAL);
    public static final RegistryObject<Item> SPEED_CAT_CAN = registerAttributeCan(
            "speed_cat_can", CatStat.SPEED, CatAttributeCanItem.Tier.NORMAL);
    public static final RegistryObject<Item> STAMINA_CAT_CAN = registerAttributeCan(
            "stamina_cat_can", CatStat.STAMINA, CatAttributeCanItem.Tier.NORMAL);
    public static final RegistryObject<Item> INTELLIGENCE_CAT_CAN = registerAttributeCan(
            "intelligence_cat_can", CatStat.INTELLIGENCE, CatAttributeCanItem.Tier.NORMAL);
    public static final RegistryObject<Item> LUCK_CAT_CAN = registerAttributeCan(
            "luck_cat_can", CatStat.LUCK, CatAttributeCanItem.Tier.NORMAL);
    public static final RegistryObject<Item> GOLDEN_ATTACK_CAT_CAN = registerAttributeCan(
            "golden_attack_cat_can", CatStat.ATTACK, CatAttributeCanItem.Tier.GOLDEN);
    public static final RegistryObject<Item> GOLDEN_HEALTH_CAT_CAN = registerAttributeCan(
            "golden_health_cat_can", CatStat.HEALTH, CatAttributeCanItem.Tier.GOLDEN);
    public static final RegistryObject<Item> GOLDEN_SPEED_CAT_CAN = registerAttributeCan(
            "golden_speed_cat_can", CatStat.SPEED, CatAttributeCanItem.Tier.GOLDEN);
    public static final RegistryObject<Item> GOLDEN_STAMINA_CAT_CAN = registerAttributeCan(
            "golden_stamina_cat_can", CatStat.STAMINA, CatAttributeCanItem.Tier.GOLDEN);
    public static final RegistryObject<Item> GOLDEN_INTELLIGENCE_CAT_CAN = registerAttributeCan(
            "golden_intelligence_cat_can", CatStat.INTELLIGENCE,
            CatAttributeCanItem.Tier.GOLDEN);
    public static final RegistryObject<Item> GOLDEN_LUCK_CAT_CAN = registerAttributeCan(
            "golden_luck_cat_can", CatStat.LUCK, CatAttributeCanItem.Tier.GOLDEN);
    public static final RegistryObject<Item> SUPER_ATTACK_CAT_CAN = registerAttributeCan(
            "super_attack_cat_can", CatStat.ATTACK, CatAttributeCanItem.Tier.SUPER);
    public static final RegistryObject<Item> SUPER_HEALTH_CAT_CAN = registerAttributeCan(
            "super_health_cat_can", CatStat.HEALTH, CatAttributeCanItem.Tier.SUPER);
    public static final RegistryObject<Item> SUPER_SPEED_CAT_CAN = registerAttributeCan(
            "super_speed_cat_can", CatStat.SPEED, CatAttributeCanItem.Tier.SUPER);
    public static final RegistryObject<Item> SUPER_STAMINA_CAT_CAN = registerAttributeCan(
            "super_stamina_cat_can", CatStat.STAMINA, CatAttributeCanItem.Tier.SUPER);
    public static final RegistryObject<Item> SUPER_INTELLIGENCE_CAT_CAN = registerAttributeCan(
            "super_intelligence_cat_can", CatStat.INTELLIGENCE, CatAttributeCanItem.Tier.SUPER);
    public static final RegistryObject<Item> SUPER_LUCK_CAT_CAN = registerAttributeCan(
            "super_luck_cat_can", CatStat.LUCK, CatAttributeCanItem.Tier.SUPER);
    public static final RegistryObject<Item> DRIED_FISH = ITEMS.register("dried_fish",
            () -> new CatTraitFishItem(new Item.Properties().food(
                    new FoodProperties.Builder().nutrition(5).saturationMod(0.6F).build()),
                    CatTraitFishItem.Tier.NORMAL));
    public static final RegistryObject<Item> GOLDEN_DRIED_FISH = ITEMS.register(
            "golden_dried_fish",
            () -> new CatTraitFishItem(new Item.Properties().food(
                    new FoodProperties.Builder().nutrition(6).saturationMod(0.8F).build()),
                    CatTraitFishItem.Tier.GOLDEN));
    public static final RegistryObject<Item> SUPER_DRIED_FISH = ITEMS.register(
            "super_dried_fish",
            () -> new CatTraitFishItem(new Item.Properties(), CatTraitFishItem.Tier.SUPER));
    public static final RegistryObject<Item> BREEDING_CAT_FOOD = ITEMS.register(
            "breeding_cat_food", () -> new BreedingCatFoodItem(
                    new Item.Properties(), CatBreedingMode.NORMAL));
    public static final RegistryObject<Item> MUTATION_CAT_FOOD = ITEMS.register(
            "mutation_cat_food", () -> new BreedingCatFoodItem(
                    new Item.Properties(), CatBreedingMode.MUTATION));
    public static final RegistryObject<Item> ATTACK_BREEDING_CAT_FOOD = ITEMS.register(
            "attack_breeding_cat_food", () -> new BreedingCatFoodItem(
                    new Item.Properties(), CatBreedingMode.ATTACK));
    public static final RegistryObject<Item> HEALTH_BREEDING_CAT_FOOD = ITEMS.register(
            "health_breeding_cat_food", () -> new BreedingCatFoodItem(
                    new Item.Properties(), CatBreedingMode.HEALTH));
    public static final RegistryObject<Item> SPEED_BREEDING_CAT_FOOD = ITEMS.register(
            "speed_breeding_cat_food", () -> new BreedingCatFoodItem(
                    new Item.Properties(), CatBreedingMode.SPEED));
    public static final RegistryObject<Item> STAMINA_BREEDING_CAT_FOOD = ITEMS.register(
            "stamina_breeding_cat_food", () -> new BreedingCatFoodItem(
                    new Item.Properties(), CatBreedingMode.STAMINA));
    public static final RegistryObject<Item> INTELLIGENCE_BREEDING_CAT_FOOD = ITEMS.register(
            "intelligence_breeding_cat_food", () -> new BreedingCatFoodItem(
                    new Item.Properties(), CatBreedingMode.INTELLIGENCE));
    public static final RegistryObject<Item> LUCK_BREEDING_CAT_FOOD = ITEMS.register(
            "luck_breeding_cat_food", () -> new BreedingCatFoodItem(
                    new Item.Properties(), CatBreedingMode.LUCK));
    public static final RegistryObject<Item> CAT_GRENADE = ITEMS.register("cat_grenade",
            () -> new CatGrenadeItem(new Item.Properties()));
    public static final RegistryObject<Item> CAT_SHELL = ITEMS.register("cat_shell",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> INCOMPLETE_CAT_GRENADE = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "incomplete_cat_grenade",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_incomplete_cat_grenade : null);
    public static final RegistryObject<Item> CAT_POWDER = ITEMS.register("cat_powder",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CAT_DOUGH = ITEMS.register("cat_dough",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CAT_HELMET = ITEMS.register("cat_helmet",
            () -> new KimiArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(Items.DIAMOND_HELMET.getMaxDamage())));
    public static final RegistryObject<Item> CAT_CHESTPLATE = ITEMS.register("cat_chestplate",
            () -> new KimiArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(Items.DIAMOND_CHESTPLATE.getMaxDamage())));
    public static final RegistryObject<Item> CAT_LEGGINGS = ITEMS.register("cat_leggings",
            () -> new KimiArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(Items.DIAMOND_LEGGINGS.getMaxDamage())));
    public static final RegistryObject<Item> CAT_BOOTS = ITEMS.register("cat_boots",
            () -> new KimiArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(Items.DIAMOND_BOOTS.getMaxDamage())));
    public static final RegistryObject<MobEffect> HISSING_ATTACK = MOB_EFFECTS.register(
            "hissing_attack", HissingAttackEffect::new);
    /** Three minutes; amplifier zero is potion level I. */
    public static final RegistryObject<Potion> HISSING_POTION = POTIONS.register("hissing",
            () -> new Potion("hissing",
                    new MobEffectInstance(HISSING_ATTACK.get(), 20 * 60 * 3, 0)));
    public static final RegistryObject<Potion> STRONG_HISSING_POTION = POTIONS.register("strong_hissing",
            () -> new Potion("hissing",
                    new MobEffectInstance(HISSING_ATTACK.get(), 20 * 60 * 3, 1)));
    public static final RegistryObject<Potion> POWERFUL_HISSING_POTION = POTIONS.register("powerful_hissing",
            () -> new Potion("hissing",
                    new MobEffectInstance(HISSING_ATTACK.get(), 20 * 60 * 3, 2)));
    public static final RegistryObject<Item> CAT_ENGINE_ITEM = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "cat_engine",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_cat_engine : null);
    public static final RegistryObject<Item> INFILTRATION_TANK_ITEM = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "infiltration_tank",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_infiltration_tank : null);
    public static final RegistryObject<Item> HISSING_COLLECTOR_ITEM = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "hissing_collector",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_hissing_collector : null);
    public static final RegistryObject<Item> DEVOURING_CAT_ITEM = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "devouring_cat",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_devouring_cat : null);
    public static final RegistryObject<Item> BASIC_BREEDING_BOX_ITEM = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "basic_breeding_box", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_basic_breeding_box : null);
    public static final RegistryObject<Item> INTERMEDIATE_BREEDING_BOX_ITEM = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "intermediate_breeding_box", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_intermediate_breeding_box : null);
    public static final RegistryObject<Item> ADVANCED_BREEDING_BOX_ITEM = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "advanced_breeding_box", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_advanced_breeding_box : null);
    public static final RegistryObject<Item> ADOPTION_BOX_ITEM = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "adoption_box",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_adoption_box : null);
    public static final RegistryObject<Item> WISH_ADOPTION_BOX_ITEM = cn.laowu.mod.compat.create.CreateIntegration.registerItem(ITEMS, "wish_adoption_box",
            cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::items_wish_adoption_box : null);
    public static final RegistryObject<FluidType> HISSING_GAS_TYPE = FLUID_TYPES.register(
            "hissing_gas", HissingGasFluidType::new);
    public static final RegistryObject<FlowingFluid> HISSING_GAS = FLUIDS.register(
            "hissing_gas", () -> new ForgeFlowingFluid.Source(hissingGasProperties()));
    public static final RegistryObject<FlowingFluid> FLOWING_HISSING_GAS = FLUIDS.register(
            "flowing_hissing_gas", () -> new ForgeFlowingFluid.Flowing(hissingGasProperties()));
    public static final RegistryObject<Item> HISSING_GAS_BUCKET = ITEMS.register("hissing_gas_bucket",
            () -> new HissingGasBucketItem(HISSING_GAS,
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final RegistryObject<FluidType> LIQUID_CAT_TYPE = FLUID_TYPES.register(
            "liquid_cat", LiquidCatFluidType::new);
    public static final RegistryObject<FlowingFluid> LIQUID_CAT = FLUIDS.register(
            "liquid_cat", () -> new ForgeFlowingFluid.Source(liquidCatProperties()));
    public static final RegistryObject<FlowingFluid> FLOWING_LIQUID_CAT = FLUIDS.register(
            "flowing_liquid_cat", () -> new ForgeFlowingFluid.Flowing(liquidCatProperties()));
    public static final RegistryObject<LiquidBlock> LIQUID_CAT_BLOCK = BLOCKS.register("liquid_cat",
            () -> new LiquidBlock(LIQUID_CAT, BlockBehaviour.Properties.copy(Blocks.LAVA)
                    .mapColor(MapColor.COLOR_YELLOW)
                    .lightLevel(state -> 0)));
    public static final RegistryObject<Item> LIQUID_CAT_BUCKET = ITEMS.register("liquid_cat_bucket",
            () -> new BucketItem(LIQUID_CAT,
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final RegistryObject<CreativeModeTab> LAOWU_TAB = CREATIVE_TABS.register("laowu",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.laowu"))
                    .icon(() -> (cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? CAT_ENGINE_ITEM.get() : CAT_PANCAKE.get()).getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        if (CAT_ENGINE_ITEM.isPresent()) output.accept(CAT_ENGINE_ITEM.get());
                        if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded()) output.accept(cn.laowu.mod.create.CatMachineBlocks.CAT_CASING_ITEM.get());
                        if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded()) output.accept(cn.laowu.mod.create.CatMachineBlocks.CAT_AUTO_LASER_ITEM.get());
                        if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded()) output.accept(cn.laowu.mod.create.CreatureTransmitterRegistration.ITEM.get());
                        if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded()) output.accept(cn.laowu.mod.create.CatMachineBlocks.HAJI_BASIN_ITEM.get());
                        if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded()) output.accept(cn.laowu.mod.create.CatMachineBlocks.CAT_PRESS_ITEM.get());
                        if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded()) output.accept(cn.laowu.mod.create.CatMachineBlocks.CAT_MIXER_ITEM.get());
                        if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded()) output.accept(cn.laowu.mod.create.CatDepotRegistration.CAT_DEPOT_ITEM.get());
                        if (INFILTRATION_TANK_ITEM.isPresent()) output.accept(INFILTRATION_TANK_ITEM.get());
                        if (HISSING_COLLECTOR_ITEM.isPresent()) output.accept(HISSING_COLLECTOR_ITEM.get());
                        if (DEVOURING_CAT_ITEM.isPresent()) output.accept(DEVOURING_CAT_ITEM.get());
                        if (ADOPTION_BOX_ITEM.isPresent()) output.accept(ADOPTION_BOX_ITEM.get());
                        if (WISH_ADOPTION_BOX_ITEM.isPresent()) output.accept(WISH_ADOPTION_BOX_ITEM.get());
                        if (CAT_CARRIER_ITEM.isPresent()) output.accept(CAT_CARRIER_ITEM.get());
                        if (CAT_EDITOR_ITEM.isPresent()) output.accept(CAT_EDITOR_ITEM.get());
                        if (CAT_DEPLOYMENT_PLATFORM_ITEM.isPresent()) output.accept(CAT_DEPLOYMENT_PLATFORM_ITEM.get());
                        if (CAT_EJECTING_DEPLOYMENT_PLATFORM_ITEM.isPresent()) output.accept(CAT_EJECTING_DEPLOYMENT_PLATFORM_ITEM.get());
                        if (CAT_BLOCK_ITEM.isPresent()) output.accept(CAT_BLOCK_ITEM.get());
                        if (CAT_INGOT.isPresent()) output.accept(CAT_INGOT.get());
                        if (CAT_SHEET.isPresent()) output.accept(CAT_SHEET.get());
                        if (CAT_FUR.isPresent()) output.accept(CAT_FUR.get());
                        if (CAT_SPRING.isPresent()) output.accept(CAT_SPRING.get());
                        if (CAT_GEAR.isPresent()) output.accept(CAT_GEAR.get());
                        if (CAT_PELLET.isPresent()) output.accept(CAT_PELLET.get());
                        if (CAT_COMPONENT.isPresent()) output.accept(CAT_COMPONENT.get());
                        if (CAT_UPGRADE_SMITHING_TEMPLATE.isPresent()) output.accept(CAT_UPGRADE_SMITHING_TEMPLATE.get());
                        output.accept(CatTotemItem.emptyStack(CAT_TOTEM.get()));
                        if (CAT_HELMET.isPresent()) output.accept(CAT_HELMET.get());
                        if (CAT_CHESTPLATE.isPresent()) output.accept(CAT_CHESTPLATE.get());
                        if (CAT_LEGGINGS.isPresent()) output.accept(CAT_LEGGINGS.get());
                        if (CAT_BOOTS.isPresent()) output.accept(CAT_BOOTS.get());
                        if (CAT_SWORD.isPresent()) output.accept(CAT_SWORD.get());
                        if (CAT_PICKAXE.isPresent()) output.accept(CAT_PICKAXE.get());
                        if (CAT_AXE.isPresent()) output.accept(CAT_AXE.get());
                        if (CAT_SHOVEL.isPresent()) output.accept(CAT_SHOVEL.get());
                        if (CAT_HOE.isPresent()) output.accept(CAT_HOE.get());
                        if (CAT_CANNON.isPresent()) output.accept(CAT_CANNON.get());
                        if (CAT_BALL.isPresent()) output.accept(CAT_BALL.get());
                        if (CAT_STRIP.isPresent()) output.accept(CAT_STRIP.get());
                        if (CAT_POUCH.isPresent()) output.accept(CAT_POUCH.get());
                        if (CAT_BOX.isPresent()) output.accept(CAT_BOX.get());
                        if (CAT_FOOD.isPresent()) output.accept(CAT_FOOD.get());
                        if (PHEROMONE_CAT_FOOD.isPresent()) output.accept(PHEROMONE_CAT_FOOD.get());
                        if (CAT_POWDER.isPresent()) output.accept(CAT_POWDER.get());
                        if (CAT_DOUGH.isPresent()) output.accept(CAT_DOUGH.get());
                        output.accept(CatPancakeItem.defaultDisplayStack());
                        if (BUTTER_BREAD.isPresent()) output.accept(BUTTER_BREAD.get());
                        if (GIANT_CAT_TREAT.isPresent()) output.accept(GIANT_CAT_TREAT.get());
                        if (BUTTER_CAT_SPAWN_EGG.isPresent()) output.accept(BUTTER_CAT_SPAWN_EGG.get());
                        if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded()) output.accept(AllItems.CARDBOARD_SWORD.get());
                        if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded()) output.accept(AllBlocks.SEATS.get(DyeColor.RED).get());
                        if (CAT_GRENADE.isPresent()) output.accept(CAT_GRENADE.get());
                        if (CAT_SHELL.isPresent()) output.accept(CAT_SHELL.get());
                        if (HISSING_GAS_BUCKET.isPresent()) output.accept(HISSING_GAS_BUCKET.get());
                        if (LIQUID_CAT_BUCKET.isPresent()) output.accept(LIQUID_CAT_BUCKET.get());
                        output.accept(PotionUtils.setPotion(
                                new ItemStack(Items.POTION), HISSING_POTION.get()));
                        output.accept(PotionUtils.setPotion(
                                new ItemStack(Items.POTION), STRONG_HISSING_POTION.get()));
                        output.accept(PotionUtils.setPotion(
                                new ItemStack(Items.POTION), POWERFUL_HISSING_POTION.get()));
                    })
                    .build());
    public static final RegistryObject<CreativeModeTab> CAT_PROGRESSION_TAB =
            CREATIVE_TABS.register("cat_progression",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.laowu.cat_progression"))
                            .icon(() -> (cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? ADVANCED_BREEDING_BOX_ITEM.get() : CAT_FOOD.get()).getDefaultInstance())
                            .displayItems((parameters, output) -> {
                                if (BASIC_BREEDING_BOX_ITEM.isPresent()) output.accept(BASIC_BREEDING_BOX_ITEM.get());
                                if (INTERMEDIATE_BREEDING_BOX_ITEM.isPresent()) output.accept(INTERMEDIATE_BREEDING_BOX_ITEM.get());
                               if (ADVANCED_BREEDING_BOX_ITEM.isPresent()) output.accept(ADVANCED_BREEDING_BOX_ITEM.get());
                                if (CAT_LASER_POINTER.isPresent()) output.accept(CAT_LASER_POINTER.get());
                                if (CAT_STORAGE_BOX.isPresent()) output.accept(CAT_STORAGE_BOX.get());

                                if (BREEDING_CAT_FOOD.isPresent()) output.accept(BREEDING_CAT_FOOD.get());
                                if (MUTATION_CAT_FOOD.isPresent()) output.accept(MUTATION_CAT_FOOD.get());
                                if (ATTACK_BREEDING_CAT_FOOD.isPresent()) output.accept(ATTACK_BREEDING_CAT_FOOD.get());
                                if (HEALTH_BREEDING_CAT_FOOD.isPresent()) output.accept(HEALTH_BREEDING_CAT_FOOD.get());
                                if (SPEED_BREEDING_CAT_FOOD.isPresent()) output.accept(SPEED_BREEDING_CAT_FOOD.get());
                                if (STAMINA_BREEDING_CAT_FOOD.isPresent()) output.accept(STAMINA_BREEDING_CAT_FOOD.get());
                                if (INTELLIGENCE_BREEDING_CAT_FOOD.isPresent()) output.accept(INTELLIGENCE_BREEDING_CAT_FOOD.get());
                                if (LUCK_BREEDING_CAT_FOOD.isPresent()) output.accept(LUCK_BREEDING_CAT_FOOD.get());

                                if (CAT_CAN.isPresent()) output.accept(CAT_CAN.get());
                                if (GOLDEN_CAT_CAN.isPresent()) output.accept(GOLDEN_CAT_CAN.get());
                                if (ATTACK_CAT_CAN.isPresent()) output.accept(ATTACK_CAT_CAN.get());
                                if (HEALTH_CAT_CAN.isPresent()) output.accept(HEALTH_CAT_CAN.get());
                                if (SPEED_CAT_CAN.isPresent()) output.accept(SPEED_CAT_CAN.get());
                                if (STAMINA_CAT_CAN.isPresent()) output.accept(STAMINA_CAT_CAN.get());
                                if (INTELLIGENCE_CAT_CAN.isPresent()) output.accept(INTELLIGENCE_CAT_CAN.get());
                                if (LUCK_CAT_CAN.isPresent()) output.accept(LUCK_CAT_CAN.get());
                                if (GOLDEN_ATTACK_CAT_CAN.isPresent()) output.accept(GOLDEN_ATTACK_CAT_CAN.get());
                                if (GOLDEN_HEALTH_CAT_CAN.isPresent()) output.accept(GOLDEN_HEALTH_CAT_CAN.get());
                                if (GOLDEN_SPEED_CAT_CAN.isPresent()) output.accept(GOLDEN_SPEED_CAT_CAN.get());
                                if (GOLDEN_STAMINA_CAT_CAN.isPresent()) output.accept(GOLDEN_STAMINA_CAT_CAN.get());
                                if (GOLDEN_INTELLIGENCE_CAT_CAN.isPresent()) output.accept(GOLDEN_INTELLIGENCE_CAT_CAN.get());
                                if (GOLDEN_LUCK_CAT_CAN.isPresent()) output.accept(GOLDEN_LUCK_CAT_CAN.get());
                                if (SUPER_ATTACK_CAT_CAN.isPresent()) output.accept(SUPER_ATTACK_CAT_CAN.get());
                                if (SUPER_HEALTH_CAT_CAN.isPresent()) output.accept(SUPER_HEALTH_CAT_CAN.get());
                                if (SUPER_SPEED_CAT_CAN.isPresent()) output.accept(SUPER_SPEED_CAT_CAN.get());
                                if (SUPER_STAMINA_CAT_CAN.isPresent()) output.accept(SUPER_STAMINA_CAT_CAN.get());
                                if (SUPER_INTELLIGENCE_CAT_CAN.isPresent()) output.accept(SUPER_INTELLIGENCE_CAT_CAN.get());
                                if (SUPER_LUCK_CAT_CAN.isPresent()) output.accept(SUPER_LUCK_CAT_CAN.get());
                                if (DRIED_FISH.isPresent()) output.accept(DRIED_FISH.get());
                                if (GOLDEN_DRIED_FISH.isPresent()) output.accept(GOLDEN_DRIED_FISH.get());
                                if (SUPER_DRIED_FISH.isPresent()) output.accept(SUPER_DRIED_FISH.get());

                                if (CAT_SCANNER.isPresent()) output.accept(CAT_SCANNER.get());
                                if (CAT_FILTER.isPresent()) output.accept(CAT_FILTER.get());
                                if (CREATURE_FILTER.isPresent()) output.accept(CREATURE_FILTER.get());
                                if (CAT_ENGINEER_GOGGLES.isPresent()) output.accept(CAT_ENGINEER_GOGGLES.get());
                                if (FUSION_DEBUG_WAND.isPresent()) output.accept(FUSION_DEBUG_WAND.get());
                                if (ATTRIBUTE_DEBUG_WAND.isPresent()) output.accept(ATTRIBUTE_DEBUG_WAND.get());
                                if (TRAIT_DEBUG_WAND.isPresent()) output.accept(TRAIT_DEBUG_WAND.get());
                                if (MATERIAL_DEBUG_WAND.isPresent()) output.accept(MATERIAL_DEBUG_WAND.get());
                                if (TERMINATOR_SUIT.isPresent()) output.accept(TERMINATOR_SUIT.get());
                                if (FISHING_SUIT.isPresent()) output.accept(FISHING_SUIT.get());
                                if (FLIGHT_SUIT.isPresent()) output.accept(FLIGHT_SUIT.get());
                                if (FIRE_SUIT.isPresent()) output.accept(FIRE_SUIT.get());
                                if (HONEY_SUIT.isPresent()) output.accept(HONEY_SUIT.get());
                                if (TRANSPORT_SUIT.isPresent()) output.accept(TRANSPORT_SUIT.get());
                                if (DYNAMITE_SUIT.isPresent()) output.accept(DYNAMITE_SUIT.get());
                                if (ENGINEERING_SUIT.isPresent()) output.accept(ENGINEERING_SUIT.get());
                                if (MEDICAL_SUIT.isPresent()) output.accept(MEDICAL_SUIT.get());
                                if (MUSIC_SUIT.isPresent()) output.accept(MUSIC_SUIT.get());
                                if (AGENT_SUIT.isPresent()) output.accept(AGENT_SUIT.get());
                                if (DIVING_SUIT.isPresent()) output.accept(DIVING_SUIT.get());
                                if (COCKROACH_SUIT.isPresent()) output.accept(COCKROACH_SUIT.get());
                            })
                            .build());
    public static final RegistryObject<CreativeModeTab> CAT_ACCESSORIES_TAB =
            CREATIVE_TABS.register("cat_accessories", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.laowu.cat_accessories"))
                    .icon(cn.laowu.mod.accessory.CatAccessoryItems::tabIcon)
                    .displayItems((parameters, output) -> cn.laowu.mod.accessory.CatAccessoryItems.display(output))
                    .build());
    public static final RegistryObject<MenuType<CatPackageMenu>> CAT_PACKAGE_MENU = cn.laowu.mod.compat.create.CreateIntegration.register(MENUS, "cat_package", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::menus_cat_package : null);
    public static final RegistryObject<MenuType<BreedingBoxMenu>> BREEDING_BOX_MENU = cn.laowu.mod.compat.create.CreateIntegration.register(MENUS, "breeding_box", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::menus_breeding_box : null);
    public static final RegistryObject<MenuType<AdoptionBoxMenu>> ADOPTION_BOX_MENU = cn.laowu.mod.compat.create.CreateIntegration.register(MENUS, "adoption_box", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::menus_adoption_box : null);
    public static final RegistryObject<MenuType<WishAdoptionBoxMenu>> WISH_ADOPTION_BOX_MENU = cn.laowu.mod.compat.create.CreateIntegration.register(MENUS, "wish_adoption_box", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::menus_wish_adoption_box : null);
    public static final RegistryObject<MenuType<CatAttributeEditorMenu>> CAT_ATTRIBUTE_EDITOR_MENU =
            MENUS.register("cat_attribute_editor",
                    () -> IForgeMenuType.create(CatAttributeEditorMenu::new));
    public static final RegistryObject<MenuType<CatTraitEditorMenu>> CAT_TRAIT_EDITOR_MENU =
            MENUS.register("cat_trait_editor",
                    () -> IForgeMenuType.create(CatTraitEditorMenu::new));
    public static final RegistryObject<MenuType<CatMaterialEditorMenu>> CAT_MATERIAL_EDITOR_MENU =
            MENUS.register("cat_material_editor",
                    () -> IForgeMenuType.create(CatMaterialEditorMenu::new));
    public static final RegistryObject<MenuType<CatProfileMenu>> CAT_PROFILE_MENU =
            MENUS.register("cat_profile", () -> IForgeMenuType.create(CatProfileMenu::new));
    public static final RegistryObject<MenuType<CreatureFilterMenu>> CREATURE_FILTER_MENU =
            cn.laowu.mod.compat.create.CreateIntegration.register(MENUS, "creature_filter", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::menus_creature_filter : null);
    public static final RegistryObject<MenuType<CatFilterMenu>> CAT_FILTER_MENU =
            cn.laowu.mod.compat.create.CreateIntegration.register(MENUS, "cat_filter", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::menus_cat_filter : null);
    public static final RegistryObject<EntityType<CatPancakeProjectile>> CAT_PANCAKE_PROJECTILE =
            ENTITY_TYPES.register("cat_pancake_projectile", () -> EntityType.Builder
                    .<CatPancakeProjectile>of(CatPancakeProjectile::new, MobCategory.MISC)
                    .sized(0.55F, 0.18F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("cat_pancake_projectile"));
    public static final RegistryObject<EntityType<cn.laowu.mod.entity.CatFlightCarrier>> CAT_FLIGHT_CARRIER =
            ENTITY_TYPES.register("cat_flight_carrier", () -> EntityType.Builder
                    .<cn.laowu.mod.entity.CatFlightCarrier>of(cn.laowu.mod.entity.CatFlightCarrier::new, MobCategory.MISC)
                    .sized(0.85F, 3.3F).clientTrackingRange(10).updateInterval(1).build("cat_flight_carrier"));
    public static final RegistryObject<EntityType<cn.laowu.mod.entity.CatGiantCarrier>> CAT_GIANT_CARRIER =
            ENTITY_TYPES.register("cat_giant_carrier", () -> EntityType.Builder
                    .<cn.laowu.mod.entity.CatGiantCarrier>of(cn.laowu.mod.entity.CatGiantCarrier::new, MobCategory.MISC)
                    .sized(CatGiantMount.WIDTH, cn.laowu.mod.entity.CatGiantCarrier.RIDE_HEIGHT).clientTrackingRange(12).updateInterval(1).build("cat_giant_carrier"));
    public static final RegistryObject<EntityType<cn.laowu.mod.entity.CatDivingCarrier>> CAT_DIVING_CARRIER =
            ENTITY_TYPES.register("cat_diving_carrier", () -> EntityType.Builder
                    .<cn.laowu.mod.entity.CatDivingCarrier>of(cn.laowu.mod.entity.CatDivingCarrier::new, MobCategory.MISC)
                    .sized(0.85F, 2.4F).clientTrackingRange(10).updateInterval(1).build("cat_diving_carrier"));
    public static final RegistryObject<EntityType<cn.laowu.mod.entity.CatHoneyPatch>> CAT_HONEY_PATCH =
            ENTITY_TYPES.register("cat_honey_patch", () -> EntityType.Builder
                    .<cn.laowu.mod.entity.CatHoneyPatch>of(cn.laowu.mod.entity.CatHoneyPatch::new, MobCategory.MISC)
                    .sized(2.5F, .1F).clientTrackingRange(4).updateInterval(20).build("laowu:cat_honey_patch"));
    public static final RegistryObject<EntityType<cn.laowu.mod.entity.AgentSmokeBomb>> AGENT_SMOKE_BOMB =
            ENTITY_TYPES.register("agent_smoke_bomb", () -> EntityType.Builder
                    .<cn.laowu.mod.entity.AgentSmokeBomb>of(cn.laowu.mod.entity.AgentSmokeBomb::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(10).updateInterval(1).build("agent_smoke_bomb"));
    public static final RegistryObject<EntityType<cn.laowu.mod.entity.EngineeringCannon>> ENGINEERING_CANNON =
            ENTITY_TYPES.register("engineering_cannon", () -> EntityType.Builder
                    .<cn.laowu.mod.entity.EngineeringCannon>of(cn.laowu.mod.entity.EngineeringCannon::new, MobCategory.MISC)
                    .sized(1.1F, 1.1F).clientTrackingRange(10).updateInterval(1).build("engineering_cannon"));
    public static final RegistryObject<EntityType<cn.laowu.mod.entity.EngineeringCogwheelProjectile>> ENGINEERING_COGWHEEL_PROJECTILE =
            ENTITY_TYPES.register("engineering_cogwheel_projectile", () -> EntityType.Builder
                    .<cn.laowu.mod.entity.EngineeringCogwheelProjectile>of(cn.laowu.mod.entity.EngineeringCogwheelProjectile::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F).clientTrackingRange(10).updateInterval(1).build("engineering_cogwheel_projectile"));
    public static final RegistryObject<EntityType<FishingRodProjectile>> FISHING_ROD_PROJECTILE =
            ENTITY_TYPES.register("fishing_rod_projectile", () -> EntityType.Builder
                    .<FishingRodProjectile>of(FishingRodProjectile::new, MobCategory.MISC)
                    .sized(0.28F, 0.28F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("fishing_rod_projectile"));
    public static final RegistryObject<EntityType<MechanicalLaserProjectile>>
            MECHANICAL_LASER_PROJECTILE = ENTITY_TYPES.register(
                    "mechanical_laser_projectile", () -> EntityType.Builder
                            .<MechanicalLaserProjectile>of(MechanicalLaserProjectile::new,
                                    MobCategory.MISC)
                            .sized(0.14F, 0.14F)
                            .clientTrackingRange(18)
                            .updateInterval(1)
                            .build("mechanical_laser_projectile"));
    public static final RegistryObject<EntityType<HoneyMissileProjectile>>
            HONEY_MISSILE_PROJECTILE = ENTITY_TYPES.register(
                    "honey_missile_projectile", () -> EntityType.Builder
                            .<HoneyMissileProjectile>of(HoneyMissileProjectile::new,
                                    MobCategory.MISC)
                            .sized(0.28F, 0.28F)
                            .clientTrackingRange(14)
                            .updateInterval(1)
                            .build("honey_missile_projectile"));
    public static final RegistryObject<EntityType<DynamiteProjectile>>
            DYNAMITE_PROJECTILE = ENTITY_TYPES.register(
                    "dynamite_projectile", () -> EntityType.Builder
                            .<DynamiteProjectile>of(DynamiteProjectile::new,
                                    MobCategory.MISC)
                            .sized(0.30F, 0.30F)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("dynamite_projectile"));
    public static final RegistryObject<EntityType<LogisticsSupportProjectile>>
            LOGISTICS_SUPPORT_PROJECTILE = ENTITY_TYPES.register(
                    "logistics_support_projectile", () -> EntityType.Builder
                            .<LogisticsSupportProjectile>of(LogisticsSupportProjectile::new,
                                    MobCategory.MISC)
                            .sized(0.42F, 0.32F)
                            .clientTrackingRange(14)
                            .updateInterval(1)
                            .build("logistics_support_projectile"));
    public static final RegistryObject<EntityType<CatBallEntity>> CAT_BALL_ENTITY =
            ENTITY_TYPES.register("cat_ball", () -> EntityType.Builder
                    .<CatBallEntity>of(CatBallEntity::new, MobCategory.MISC)
                    // Exact rotated model bounds, enlarged uniformly by 1.4x.
                    .sized(CatBallEntity.MODEL_DIAMETER_PIXELS * CatBallEntity.WORLD_SCALE / 16.0F,
                            CatBallEntity.MODEL_HEIGHT_PIXELS * CatBallEntity.WORLD_SCALE / 16.0F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("cat_ball"));
    public static final RegistryObject<RecipeType<InfiltratingRecipe>> INFILTRATING_TYPE =
            cn.laowu.mod.compat.create.CreateIntegration.register(RECIPE_TYPES, "infiltrating", cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::recipe_types_infiltrating : null);
    public static final RegistryObject<RecipeSerializer<InfiltratingRecipe>> INFILTRATING_SERIALIZER =
            cn.laowu.mod.compat.create.CreateIntegration.register(RECIPE_SERIALIZERS, "infiltrating",
                    cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::recipe_serializers_infiltrating : null);
    public static final RegistryObject<RecipeSerializer<RandomBabyCatPancakeFillingRecipe>>
            RANDOM_BABY_CAT_PANCAKE_FILLING_SERIALIZER =
            cn.laowu.mod.compat.create.CreateIntegration.register(RECIPE_SERIALIZERS, "random_baby_cat_pancake_filling",
                    cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::recipe_serializers_random_baby_cat_pancake_filling : null);
    public static final RegistryObject<RecipeSerializer<PheromoneCatFoodMixingRecipe>>
            PHEROMONE_CAT_FOOD_MIXING_SERIALIZER =
            cn.laowu.mod.compat.create.CreateIntegration.register(RECIPE_SERIALIZERS, "pheromone_cat_food_mixing",
                    cn.laowu.mod.compat.create.CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateFactories::recipe_serializers_pheromone_cat_food_mixing : null);

    public static final RegistryObject<RecipeSerializer<cn.laowu.mod.recipe.KimiArmorDyeRecipe>>
            KIMI_ARMOR_DYE_SERIALIZER = RECIPE_SERIALIZERS.register("kimi_armor_dye",
            () -> new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(cn.laowu.mod.recipe.KimiArmorDyeRecipe::new));

    public static final RegistryObject<RecipeSerializer<cn.laowu.mod.recipe.WishAdoptionBoxRecipe>>
            WISH_ADOPTION_BOX_RECIPE = RECIPE_SERIALIZERS.register("wish_adoption_box",
            cn.laowu.mod.recipe.WishAdoptionBoxRecipe.Serializer::new);

    public static final RegistryObject<RecipeSerializer<cn.laowu.mod.recipe.LaserPointerDyeRecipe>>
            LASER_POINTER_DYE_SERIALIZER = RECIPE_SERIALIZERS.register("laser_pointer_dye",
            () -> new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(cn.laowu.mod.recipe.LaserPointerDyeRecipe::new));

    public LaoWuMod(FMLJavaModLoadingContext context) {
        CraftingHelper.register(id("cat_pancake"), CatPancakeIngredient.Serializer.INSTANCE);
        CraftingHelper.register(id("cat_pancake_variant"),
                CatPancakeVariantIngredient.Serializer.INSTANCE);
        CraftingHelper.register(id("any_block"), AnyBlockIngredient.Serializer.INSTANCE);
        CraftingHelper.register(id("named_player_name_tag"),
                NamedPlayerNameTagIngredient.Serializer.INSTANCE);
        IEventBus modBus = context.getModEventBus();
        cn.laowu.mod.accessory.CatAccessoryItems.register((name, factory) -> ITEMS.register(name, factory));
        ITEMS.register(modBus);
        if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded())
            cn.laowu.mod.create.CatMachineBlocks.register(modBus);
        if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded() && net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT)
            cn.laowu.mod.compat.create.CreateClientEvents.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        ENTITY_TYPES.register(modBus);
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
        MOB_EFFECTS.register(modBus);
        POTIONS.register(modBus);
        PARTICLE_TYPES.register(modBus);
        CREATIVE_TABS.register(modBus);
        RECIPE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        LOOT_MODIFIERS.register(modBus);
        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "laowu-client.toml");
        context.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC, "laowu-server.toml");
        modBus.addListener((net.minecraftforge.fml.event.config.ModConfigEvent.Loading event) -> {
            if (event.getConfig().getSpec() == ServerConfig.SPEC && ServerConfig.migrateLegacyBalanceDefaults())
                event.getConfig().save();
        });
        context.registerConfig(ModConfig.Type.COMMON, GlobalConfig.SPEC, GlobalConfig.FILE_NAME);
        MinecraftForge.EVENT_BUS.register(CommonEvents.class);
        if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded())
            MinecraftForge.EVENT_BUS.register(cn.laowu.mod.compat.create.CreateProcessingEvents.class);
        MinecraftForge.EVENT_BUS.register(cn.laowu.mod.accessory.CatAccessoryEvents.class);
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.server.ServerAboutToStartEvent event) -> ServerConfig.resetWorldState());
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.server.ServerStoppedEvent event) -> ServerConfig.resetWorldState());
        ModNetwork.register();
        modBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                event.enqueueWork(() -> {
                    DispenserBlock.registerBehavior(CAT_PANCAKE.get(),
                            new AbstractProjectileDispenseBehavior() {
                                @Override
                                protected Projectile getProjectile(
                                        net.minecraft.world.level.Level level,
                                        Position position, ItemStack stack) {
                                    boolean highExplosive =
                                            cn.laowu.mod.genetics.CatTraitData.read(stack)
                                                    .map(profile -> profile.has(
                                                            cn.laowu.mod.genetics.CatTrait
                                                                    .HIGH_EXPLOSIVE_FUEL))
                                                    .orElse(false);
                                    return new CatPancakeProjectile(level,
                                            position.x(), position.y(), position.z(),
                                            stack, highExplosive ? 20.0F : 8.0F,
                                            highExplosive ? 3.5F : 2.0F);
                                }

                                @Override
                                protected float getPower() {
                                    return 1.55F;
                                }
                            });
                    if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded())
                        cn.laowu.mod.compat.create.CreateStartup.initialize();
                }));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private static RegistryObject<Item> registerAttributeCan(
            String name, CatStat stat, CatAttributeCanItem.Tier tier) {
        return ITEMS.register(name,
                () -> new CatAttributeCanItem(new Item.Properties(), stat, tier));
    }

    private static ForgeFlowingFluid.Properties hissingGasProperties() {
        // Intentionally omit .block(...). Create detects the resulting AIR
        // legacy state and consumes exposed pipe output as vapour. The custom
        // bucket is referenced separately so it can exchange with tanks without
        // gaining normal world-placement behaviour.
        return new ForgeFlowingFluid.Properties(HISSING_GAS_TYPE, HISSING_GAS, FLOWING_HISSING_GAS)
                .bucket(HISSING_GAS_BUCKET)
                .slopeFindDistance(1)
                .levelDecreasePerBlock(1)
                .tickRate(5)
                .explosionResistance(0.0F);
    }

    private static ForgeFlowingFluid.Properties liquidCatProperties() {
        // Match lava's overworld flow cadence and horizontal reach while
        // remaining a distinct, non-luminous and non-renewable Forge fluid.
        return new ForgeFlowingFluid.Properties(LIQUID_CAT_TYPE, LIQUID_CAT, FLOWING_LIQUID_CAT)
                .block(LIQUID_CAT_BLOCK)
                .bucket(LIQUID_CAT_BUCKET)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .tickRate(30)
                .explosionResistance(100.0F);
    }

}
