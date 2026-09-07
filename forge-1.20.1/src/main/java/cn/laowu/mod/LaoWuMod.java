package cn.laowu.mod;

import cn.laowu.mod.item.CatPancakeItem;
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
import cn.laowu.mod.item.CatScannerItem;
import cn.laowu.mod.item.CatFilterItem;
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
    public static final RegistryObject<ParticleType<NozzleFluidPuffData>> NOZZLE_FLUID_PUFF =
            PARTICLE_TYPES.register("nozzle_fluid_puff", () ->
                    new ParticleType<>(false, NozzleFluidPuffData.DESERIALIZER) {
                        @Override
                        public Codec<NozzleFluidPuffData> codec() {
                            return NozzleFluidPuffData.CODEC;
                        }
                    });

    public static final RegistryObject<Block> CAT_BLOCK = BLOCKS.register("cat_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DIAMOND_BLOCK)));

    public static final RegistryObject<Block> CAT_ENGINE = BLOCKS.register("cat_engine",
            () -> new CatEngineBlock(BlockBehaviour.Properties.of()
                    .noOcclusion().strength(3.0F, 6.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<BlockEntityType<CatEngineBlockEntity>> CAT_ENGINE_BE =
            BLOCK_ENTITIES.register("cat_engine", () -> BlockEntityType.Builder
                    .of(CatEngineBlockEntity::new, CAT_ENGINE.get()).build(null));
    public static final RegistryObject<Block> INFILTRATION_TANK = BLOCKS.register("infiltration_tank",
            () -> new InfiltrationTankBlock(BlockBehaviour.Properties.of()
                    .noOcclusion().strength(3.0F, 6.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<BlockEntityType<InfiltrationTankBlockEntity>> INFILTRATION_TANK_BE =
            BLOCK_ENTITIES.register("infiltration_tank", () -> BlockEntityType.Builder
                    .of(InfiltrationTankBlockEntity::new, INFILTRATION_TANK.get()).build(null));
    public static final RegistryObject<Block> HISSING_COLLECTOR = BLOCKS.register("hissing_collector",
            () -> new HissingCollectorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .noOcclusion().strength(1.5F, 6.0F)));
    public static final RegistryObject<BlockEntityType<HissingCollectorBlockEntity>> HISSING_COLLECTOR_BE =
            BLOCK_ENTITIES.register("hissing_collector", () -> BlockEntityType.Builder
                    .of(HissingCollectorBlockEntity::new, HISSING_COLLECTOR.get()).build(null));
    public static final RegistryObject<Block> DEVOURING_CAT = BLOCKS.register("devouring_cat",
            () -> new DevouringCatBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .noOcclusion().strength(3.0F, 6.0F)));
    public static final RegistryObject<BlockEntityType<DevouringCatBlockEntity>> DEVOURING_CAT_BE =
            BLOCK_ENTITIES.register("devouring_cat", () -> BlockEntityType.Builder
                    .of(DevouringCatBlockEntity::new, DEVOURING_CAT.get()).build(null));
    public static final RegistryObject<Block> BASIC_BREEDING_BOX = BLOCKS.register(
            "basic_breeding_box", () -> new BreedingBoxBlock(BreedingBoxTier.BASIC,
                    BlockBehaviour.Properties.copy(Blocks.BARREL).noOcclusion().strength(0.8F)));
    public static final RegistryObject<Block> INTERMEDIATE_BREEDING_BOX = BLOCKS.register(
            "intermediate_breeding_box", () -> new BreedingBoxBlock(BreedingBoxTier.INTERMEDIATE,
                    BlockBehaviour.Properties.copy(Blocks.BARREL).noOcclusion().strength(1.2F)));
    public static final RegistryObject<Block> ADVANCED_BREEDING_BOX = BLOCKS.register(
            "advanced_breeding_box", () -> new BreedingBoxBlock(BreedingBoxTier.ADVANCED,
                    BlockBehaviour.Properties.copy(Blocks.BARREL).noOcclusion().strength(1.6F)));
    public static final RegistryObject<BlockEntityType<BreedingBoxBlockEntity>> BREEDING_BOX_BE =
            BLOCK_ENTITIES.register("breeding_box", () -> BlockEntityType.Builder
                    .of(BreedingBoxBlockEntity::new, BASIC_BREEDING_BOX.get(),
                            INTERMEDIATE_BREEDING_BOX.get(), ADVANCED_BREEDING_BOX.get())
                    .build(null));
    public static final RegistryObject<Block> ADOPTION_BOX = BLOCKS.register("adoption_box",
            () -> new AdoptionBoxBlock(BlockBehaviour.Properties.copy(Blocks.BARREL)
                    .noOcclusion().strength(0.8F)));
    public static final RegistryObject<BlockEntityType<AdoptionBoxBlockEntity>> ADOPTION_BOX_BE =
            BLOCK_ENTITIES.register("adoption_box", () -> BlockEntityType.Builder
                    .of(AdoptionBoxBlockEntity::new, ADOPTION_BOX.get()).build(null));
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
    public static final RegistryObject<Item> CAT_FILTER = ITEMS.register("cat_filter",
            () -> new CatFilterItem(new Item.Properties().stacksTo(1)));
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
    public static final RegistryObject<Item> INCOMPLETE_CAT_COMPONENT = ITEMS.register(
            "incomplete_cat_component", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> INCOMPLETE_TERMINATOR_SUIT = ITEMS.register(
            "incomplete_terminator_suit", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> INCOMPLETE_FISHING_SUIT = ITEMS.register(
            "incomplete_fishing_suit", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> INCOMPLETE_FLIGHT_SUIT = ITEMS.register(
            "incomplete_flight_suit", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> INCOMPLETE_TRANSPORT_SUIT = ITEMS.register(
            "incomplete_transport_suit", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> INCOMPLETE_FIRE_SUIT = ITEMS.register(
            "incomplete_fire_suit", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> INCOMPLETE_HONEY_SUIT = ITEMS.register(
            "incomplete_honey_suit", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> INCOMPLETE_DYNAMITE_SUIT = ITEMS.register(
            "incomplete_dynamite_suit", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CAT_TOTEM = ITEMS.register("cat_totem",
            () -> new CatTotemItem(new Item.Properties()));
    public static final RegistryObject<Item> CAT_ENGINEER_GOGGLES = ITEMS.register("cat_engineer_goggles",
            () -> new CatEngineerGogglesItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CAT_BLOCK_ITEM = ITEMS.register("cat_block",
            () -> new BlockItem(CAT_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> CAT_UPGRADE_SMITHING_TEMPLATE = ITEMS.register(
            "cat_upgrade_smithing_template", CatSmithingTemplateItem::new);
    public static final RegistryObject<Item> CAT_SWORD = ITEMS.register("cat_sword",
            () -> new SwordItem(CatToolTier.INSTANCE, 3, -2.4F,
                    new Item.Properties().durability(Items.DIAMOND_SWORD.getMaxDamage())));
    public static final RegistryObject<Item> CAT_PICKAXE = ITEMS.register("cat_pickaxe",
            () -> new PickaxeItem(CatToolTier.INSTANCE, 1, -2.8F,
                    new Item.Properties().durability(Items.DIAMOND_PICKAXE.getMaxDamage())));
    public static final RegistryObject<Item> CAT_AXE = ITEMS.register("cat_axe",
            () -> new AxeItem(CatToolTier.INSTANCE, 5.0F, -3.0F,
                    new Item.Properties().durability(Items.DIAMOND_AXE.getMaxDamage())));
    public static final RegistryObject<Item> CAT_SHOVEL = ITEMS.register("cat_shovel",
            () -> new ShovelItem(CatToolTier.INSTANCE, 1.5F, -3.0F,
                    new Item.Properties().durability(Items.DIAMOND_SHOVEL.getMaxDamage())));
    public static final RegistryObject<Item> CAT_HOE = ITEMS.register("cat_hoe",
            () -> new CatHoeItem(CatToolTier.INSTANCE, -3, 0.0F,
                    new Item.Properties().durability(Items.DIAMOND_HOE.getMaxDamage())));
    public static final RegistryObject<Item> CAT_CANNON = ITEMS.register("cat_cannon",
            () -> new CatCannonItem(new Item.Properties().stacksTo(1)));
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
    public static final RegistryObject<Item> INCOMPLETE_CAT_GRENADE = ITEMS.register("incomplete_cat_grenade",
            () -> new Item(new Item.Properties()));
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
    public static final RegistryObject<Item> CAT_ENGINE_ITEM = ITEMS.register("cat_engine",
            () -> new CatEngineBlockItem(CAT_ENGINE.get(), new Item.Properties()));
    public static final RegistryObject<Item> INFILTRATION_TANK_ITEM = ITEMS.register("infiltration_tank",
            () -> new BlockItem(INFILTRATION_TANK.get(), new Item.Properties()));
    public static final RegistryObject<Item> HISSING_COLLECTOR_ITEM = ITEMS.register("hissing_collector",
            () -> new BlockItem(HISSING_COLLECTOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> DEVOURING_CAT_ITEM = ITEMS.register("devouring_cat",
            () -> new DevouringCatBlockItem(DEVOURING_CAT.get(), new Item.Properties()));
    public static final RegistryObject<Item> BASIC_BREEDING_BOX_ITEM = ITEMS.register(
            "basic_breeding_box", () -> new BreedingBoxBlockItem(
                    BASIC_BREEDING_BOX.get(), new Item.Properties()));
    public static final RegistryObject<Item> INTERMEDIATE_BREEDING_BOX_ITEM = ITEMS.register(
            "intermediate_breeding_box", () -> new BreedingBoxBlockItem(
                    INTERMEDIATE_BREEDING_BOX.get(), new Item.Properties()));
    public static final RegistryObject<Item> ADVANCED_BREEDING_BOX_ITEM = ITEMS.register(
            "advanced_breeding_box", () -> new BreedingBoxBlockItem(
                    ADVANCED_BREEDING_BOX.get(), new Item.Properties()));
    public static final RegistryObject<Item> ADOPTION_BOX_ITEM = ITEMS.register("adoption_box",
            () -> new AdoptionBoxBlockItem(ADOPTION_BOX.get(), new Item.Properties()));
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
                        output.accept(CAT_UPGRADE_SMITHING_TEMPLATE.get());
                        output.accept(CatTotemItem.emptyStack(CAT_TOTEM.get()));
                        output.accept(CAT_HELMET.get());
                        output.accept(CAT_CHESTPLATE.get());
                        output.accept(CAT_LEGGINGS.get());
                        output.accept(CAT_BOOTS.get());
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
                        output.accept(CAT_GRENADE.get());
                        output.accept(CAT_SHELL.get());
                        output.accept(HISSING_GAS_BUCKET.get());
                        output.accept(LIQUID_CAT_BUCKET.get());
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
                                output.accept(TERMINATOR_SUIT.get());
                                output.accept(FISHING_SUIT.get());
                                output.accept(FLIGHT_SUIT.get());
                                output.accept(FIRE_SUIT.get());
                                output.accept(HONEY_SUIT.get());
                                output.accept(TRANSPORT_SUIT.get());
                                output.accept(DYNAMITE_SUIT.get());
                            })
                            .build());
    public static final RegistryObject<MenuType<CatPackageMenu>> CAT_PACKAGE_MENU = MENUS.register(
            "cat_package", () -> IForgeMenuType.create(CatPackageMenu::new));
    public static final RegistryObject<MenuType<BreedingBoxMenu>> BREEDING_BOX_MENU = MENUS.register(
            "breeding_box", () -> IForgeMenuType.create(BreedingBoxMenu::new));
    public static final RegistryObject<MenuType<AdoptionBoxMenu>> ADOPTION_BOX_MENU = MENUS.register(
            "adoption_box", () -> IForgeMenuType.create(AdoptionBoxMenu::new));
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
    public static final RegistryObject<MenuType<CatFilterMenu>> CAT_FILTER_MENU =
            MENUS.register("cat_filter", () -> IForgeMenuType.create(CatFilterMenu::new));
    public static final RegistryObject<EntityType<CatPancakeProjectile>> CAT_PANCAKE_PROJECTILE =
            ENTITY_TYPES.register("cat_pancake_projectile", () -> EntityType.Builder
                    .<CatPancakeProjectile>of(CatPancakeProjectile::new, MobCategory.MISC)
                    .sized(0.55F, 0.18F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("cat_pancake_projectile"));
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
            RECIPE_TYPES.register("infiltrating", () -> new RecipeType<>() {
                @Override public String toString() { return MOD_ID + ":infiltrating"; }
            });
    public static final RegistryObject<RecipeSerializer<InfiltratingRecipe>> INFILTRATING_SERIALIZER =
            RECIPE_SERIALIZERS.register("infiltrating",
                    () -> new ProcessingRecipeSerializer<>(InfiltratingRecipe::new));
    public static final RegistryObject<RecipeSerializer<RandomBabyCatPancakeFillingRecipe>>
            RANDOM_BABY_CAT_PANCAKE_FILLING_SERIALIZER =
            RECIPE_SERIALIZERS.register("random_baby_cat_pancake_filling",
                    () -> new ProcessingRecipeSerializer<>(
                            RandomBabyCatPancakeFillingRecipe::new));
    public static final RegistryObject<RecipeSerializer<PheromoneCatFoodMixingRecipe>>
            PHEROMONE_CAT_FOOD_MIXING_SERIALIZER =
            RECIPE_SERIALIZERS.register("pheromone_cat_food_mixing",
                    () -> new ProcessingRecipeSerializer<>(
                            PheromoneCatFoodMixingRecipe::new));

    public LaoWuMod(FMLJavaModLoadingContext context) {
        CraftingHelper.register(id("cat_pancake"), CatPancakeIngredient.Serializer.INSTANCE);
        CraftingHelper.register(id("cat_pancake_variant"),
                CatPancakeVariantIngredient.Serializer.INSTANCE);
        CraftingHelper.register(id("any_block"), AnyBlockIngredient.Serializer.INSTANCE);
        CraftingHelper.register(id("named_player_name_tag"),
                NamedPlayerNameTagIngredient.Serializer.INSTANCE);
        IEventBus modBus = context.getModEventBus();
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
        CREATIVE_TABS.register(modBus);
        RECIPE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        LOOT_MODIFIERS.register(modBus);
        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "laowu-client.toml");
        MinecraftForge.EVENT_BUS.register(CommonEvents.class);
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
                    // Register this as an ordinary Create kinetic source:
                    // 64 SU/RPM * 96 RPM = 6144 SU total capacity.
                    BlockStressValues.CAPACITIES.register(CAT_ENGINE.get(),
                            () -> (double) CatEngineBlockEntity.STRESS_CAPACITY_PER_RPM);
                    BlockStressValues.setGeneratorSpeed((int) CatEngineBlockEntity.GENERATED_RPM)
                            .accept(CAT_ENGINE.get());
                    // Create supplies the standard RPM and stress-capacity lines.
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
                    registerCareerSuitDescription(TERMINATOR_SUIT.get());
                    registerCareerSuitDescription(FISHING_SUIT.get());
                    registerCareerSuitDescription(FLIGHT_SUIT.get());
                    registerCareerSuitDescription(FIRE_SUIT.get());
                    registerCareerSuitDescription(HONEY_SUIT.get());
                    registerCareerSuitDescription(TRANSPORT_SUIT.get());
                    registerCareerSuitDescription(DYNAMITE_SUIT.get());
                    GogglesItem.addIsWearingPredicate(CatEngineerGogglesItem::isWornBy);
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

    private static RegistryObject<Item> registerAttributeCan(
            String name, CatStat stat, CatAttributeCanItem.Tier tier) {
        return ITEMS.register(name,
                () -> new CatAttributeCanItem(new Item.Properties(), stat, tier));
    }

    /** Career suits use Ctrl for formulas and Shift for computed 50/100 previews. */
    private static void registerCareerSuitDescription(Item item) {
        if (!(item instanceof TerminatorSuitItem suit)) return;
        TooltipModifier.REGISTRY.register(item,
                event -> CareerSuitTooltip.modify(event, item, suit.outfit()));
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
