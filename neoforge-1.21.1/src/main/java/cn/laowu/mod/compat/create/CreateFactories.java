package cn.laowu.mod.compat.create;

import cn.laowu.mod.*;
import static cn.laowu.mod.LaoWuMod.*;
import cn.laowu.mod.item.CatPancakeItem;
import cn.laowu.mod.item.CatFilterItem;
import cn.laowu.mod.item.CreatureFilterItem;
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
import cn.laowu.mod.item.WishAdoptionBoxBlockItem;
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
import cn.laowu.mod.create.WishAdoptionBoxBlock;
import cn.laowu.mod.create.WishAdoptionBoxBlockEntity;
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

/** Factories are resolved only when Create is present, never from the core bootstrap. */
public final class CreateFactories {
    public static ParticleType<NozzleFluidPuffData> particle_types_nozzle_fluid_puff() { return new ParticleType<>(false) {
                @Override
                public MapCodec<NozzleFluidPuffData> codec() {
                    return NozzleFluidPuffData.CODEC;
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, NozzleFluidPuffData> streamCodec() {
                    return NozzleFluidPuffData.STREAM_CODEC;
                }
            }; }
    public static CatEngineBlock blocks_cat_engine() { return new CatEngineBlock(BlockBehaviour.Properties.of()
                    .noOcclusion().strength(3.0F, 6.0F).requiresCorrectToolForDrops()); }
    public static BlockEntityType<CatEngineBlockEntity> block_entities_cat_engine() { return BlockEntityType.Builder
                    .of(CatEngineBlockEntity::new, CAT_ENGINE.get()).build(null); }
    public static InfiltrationTankBlock blocks_infiltration_tank() { return new InfiltrationTankBlock(BlockBehaviour.Properties.of()
                    .noOcclusion().strength(3.0F, 6.0F).requiresCorrectToolForDrops()); }
    public static BlockEntityType<InfiltrationTankBlockEntity> block_entities_infiltration_tank() { return BlockEntityType.Builder
                    .of(InfiltrationTankBlockEntity::new, INFILTRATION_TANK.get()).build(null); }
    public static HissingCollectorBlock blocks_hissing_collector() { return new HissingCollectorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .noOcclusion().strength(1.5F, 6.0F)); }
    public static BlockEntityType<HissingCollectorBlockEntity> block_entities_hissing_collector() { return BlockEntityType.Builder
                    .of(HissingCollectorBlockEntity::new, HISSING_COLLECTOR.get()).build(null); }
    public static DevouringCatBlock blocks_devouring_cat() { return new DevouringCatBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .noOcclusion().strength(3.0F, 6.0F)); }
    public static BlockEntityType<DevouringCatBlockEntity> block_entities_devouring_cat() { return BlockEntityType.Builder
                    .of(DevouringCatBlockEntity::new, DEVOURING_CAT.get()).build(null); }
    public static BreedingBoxBlock blocks_basic_breeding_box() { return new BreedingBoxBlock(BreedingBoxTier.BASIC, BlockBehaviour.Properties.of().noOcclusion().strength(3.0F, 6.0F)); }
    public static BreedingBoxBlock blocks_intermediate_breeding_box() { return new BreedingBoxBlock(BreedingBoxTier.INTERMEDIATE, BlockBehaviour.Properties.of().noOcclusion().strength(3.0F, 6.0F)); }
    public static BreedingBoxBlock blocks_advanced_breeding_box() { return new BreedingBoxBlock(BreedingBoxTier.ADVANCED, BlockBehaviour.Properties.of().noOcclusion().strength(3.0F, 6.0F)); }
    public static BlockEntityType<BreedingBoxBlockEntity> block_entities_breeding_box() { return BlockEntityType.Builder
                    .of(BreedingBoxBlockEntity::new, BASIC_BREEDING_BOX.get(), INTERMEDIATE_BREEDING_BOX.get(), ADVANCED_BREEDING_BOX.get())
                    .build(null); }
    public static cn.laowu.mod.create.CatCarrierBlock blocks_cat_carrier() { return new cn.laowu.mod.create.CatCarrierBlock(BlockBehaviour.Properties.of().strength(2.0F).noOcclusion()); }
    public static BlockEntityType<cn.laowu.mod.create.CatCarrierBlockEntity> block_entities_cat_carrier() { return BlockEntityType.Builder
                    .of(cn.laowu.mod.create.CatCarrierBlockEntity::new, CAT_CARRIER.get()).build(null); }
    public static cn.laowu.mod.item.CatCarrierBlockItem items_cat_carrier() { return new cn.laowu.mod.item.CatCarrierBlockItem(CAT_CARRIER.get(), new Item.Properties()); }
    public static cn.laowu.mod.create.CatEditorBlock blocks_cat_editor() { return new cn.laowu.mod.create.CatEditorBlock(BlockBehaviour.Properties.of().strength(2F).noOcclusion()); }
    public static Item items_cat_editor() { return new BlockItem(CAT_EDITOR.get(), new Item.Properties()); }
    public static MenuType<CatEditorMenu> menus_cat_editor() { return IMenuTypeExtension.create(CatEditorMenu::new); }
    public static cn.laowu.mod.create.CatDeploymentBlock blocks_cat_deployment_platform() { return new cn.laowu.mod.create.CatDeploymentBlock(BlockBehaviour.Properties.of().strength(2F).noOcclusion(), false); }
    public static cn.laowu.mod.create.CatDeploymentBlock blocks_cat_ejecting_deployment_platform() { return new cn.laowu.mod.create.CatDeploymentBlock(BlockBehaviour.Properties.of().strength(2F).noOcclusion(), true); }
    public static BlockEntityType<cn.laowu.mod.create.CatDeploymentBlockEntity> block_entities_cat_deployment_platform() { return BlockEntityType.Builder.of(cn.laowu.mod.create.CatDeploymentBlockEntity::new, CAT_DEPLOYMENT_PLATFORM.get(), CAT_EJECTING_DEPLOYMENT_PLATFORM.get()).build(null); }
    public static cn.laowu.mod.item.CatDeploymentBlockItem items_cat_deployment_platform() { return new cn.laowu.mod.item.CatDeploymentBlockItem(CAT_DEPLOYMENT_PLATFORM.get(), new Item.Properties()); }
    public static cn.laowu.mod.item.CatDeploymentBlockItem items_cat_ejecting_deployment_platform() { return new cn.laowu.mod.item.CatDeploymentBlockItem(CAT_EJECTING_DEPLOYMENT_PLATFORM.get(), new Item.Properties()); }
    public static cn.laowu.mod.item.CatStorageBoxItem items_cat_storage_box() { return new cn.laowu.mod.item.CatStorageBoxItem(new Item.Properties()); }
    public static AdoptionBoxBlock blocks_adoption_box() { return new AdoptionBoxBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)
                    .noOcclusion().strength(0.8F)); }
    public static BlockEntityType<AdoptionBoxBlockEntity> block_entities_adoption_box() { return BlockEntityType.Builder
                    .of(AdoptionBoxBlockEntity::new, ADOPTION_BOX.get()).build(null); }
    public static WishAdoptionBoxBlock blocks_wish_adoption_box() { return new WishAdoptionBoxBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)
                    .noOcclusion().strength(0.8F)); }
    public static BlockEntityType<WishAdoptionBoxBlockEntity> block_entities_wish_adoption_box() { return BlockEntityType.Builder
                    .of(WishAdoptionBoxBlockEntity::new, WISH_ADOPTION_BOX.get()).build(null); }
    public static CreatureFilterItem items_creature_filter() { return new CreatureFilterItem(new Item.Properties()); }
    public static CatFilterItem items_cat_filter() { return new CatFilterItem(new Item.Properties().stacksTo(1)); }
    public static Item items_incomplete_cat_component() { return new com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem(new Item.Properties()); }
    public static Item items_incomplete_terminator_suit() { return new com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem(new Item.Properties()); }
    public static Item items_incomplete_fishing_suit() { return new com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem(new Item.Properties()); }
    public static Item items_incomplete_flight_suit() { return new com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem(new Item.Properties()); }
    public static Item items_incomplete_transport_suit() { return new com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem(new Item.Properties()); }
    public static Item items_incomplete_fire_suit() { return new com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem(new Item.Properties()); }
    public static Item items_incomplete_honey_suit() { return new com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem(new Item.Properties()); }
    public static Item items_incomplete_dynamite_suit() { return new com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem(new Item.Properties()); }
    public static Item items_incomplete_engineering_suit() { return new com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem(new Item.Properties()); }
    public static Item items_incomplete_medical_suit() { return new com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem(new Item.Properties()); }
    public static Item items_incomplete_music_suit() { return new com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem(new Item.Properties()); }
    public static Item items_incomplete_agent_suit() { return new com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem(new Item.Properties()); }
    public static Item items_incomplete_diving_suit() { return new com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem(new Item.Properties()); }
    public static Item items_incomplete_cockroach_suit() { return new com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem(new Item.Properties()); }
    public static CatEngineerGogglesItem items_cat_engineer_goggles() { return new CatEngineerGogglesItem(new Item.Properties().stacksTo(1)); }
    public static CatCannonItem items_cat_cannon() { return new CatCannonItem(new Item.Properties().stacksTo(1)); }
    public static Item items_incomplete_cat_grenade() { return new com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem(new Item.Properties()); }
    public static CatEngineBlockItem items_cat_engine() { return new CatEngineBlockItem(CAT_ENGINE.get(), new Item.Properties()); }
    public static BlockItem items_infiltration_tank() { return new BlockItem(INFILTRATION_TANK.get(), new Item.Properties()); }
    public static BlockItem items_hissing_collector() { return new BlockItem(HISSING_COLLECTOR.get(), new Item.Properties()); }
    public static DevouringCatBlockItem items_devouring_cat() { return new DevouringCatBlockItem(DEVOURING_CAT.get(), new Item.Properties()); }
    public static BreedingBoxBlockItem items_basic_breeding_box() { return new BreedingBoxBlockItem(BASIC_BREEDING_BOX.get(), new Item.Properties()); }
    public static BreedingBoxBlockItem items_intermediate_breeding_box() { return new BreedingBoxBlockItem(INTERMEDIATE_BREEDING_BOX.get(), new Item.Properties()); }
    public static BreedingBoxBlockItem items_advanced_breeding_box() { return new BreedingBoxBlockItem(ADVANCED_BREEDING_BOX.get(), new Item.Properties()); }
    public static AdoptionBoxBlockItem items_adoption_box() { return new AdoptionBoxBlockItem(ADOPTION_BOX.get(), new Item.Properties()); }
    public static WishAdoptionBoxBlockItem items_wish_adoption_box() { return new WishAdoptionBoxBlockItem(WISH_ADOPTION_BOX.get(), new Item.Properties()); }
    public static MenuType<CatPackageMenu> menus_cat_package() { return IMenuTypeExtension.create(CatPackageMenu::new); }
    public static MenuType<BreedingBoxMenu> menus_breeding_box() { return IMenuTypeExtension.create(BreedingBoxMenu::new); }
    public static MenuType<AdoptionBoxMenu> menus_adoption_box() { return IMenuTypeExtension.create(AdoptionBoxMenu::new); }
    public static MenuType<WishAdoptionBoxMenu> menus_wish_adoption_box() { return IMenuTypeExtension.create(WishAdoptionBoxMenu::new); }
    public static MenuType<CreatureFilterMenu> menus_creature_filter() { return IMenuTypeExtension.create(CreatureFilterMenu::new); }
    public static MenuType<CatFilterMenu> menus_cat_filter() { return IMenuTypeExtension.create(CatFilterMenu::new); }
    public static RecipeType<InfiltratingRecipe> recipe_types_infiltrating() { return new RecipeType<>() {
                @Override public String toString() { return MOD_ID + ":infiltrating"; }
            }; }
    public static RecipeSerializer<InfiltratingRecipe> recipe_serializers_infiltrating() { return new StandardProcessingRecipe.Serializer<>(InfiltratingRecipe::new); }
    public static RecipeSerializer<RandomBabyCatPancakeFillingRecipe> recipe_serializers_random_baby_cat_pancake_filling() { return new StandardProcessingRecipe.Serializer<>(
                            RandomBabyCatPancakeFillingRecipe::new); }
    public static RecipeSerializer<PheromoneCatFoodMixingRecipe> recipe_serializers_pheromone_cat_food_mixing() { return new StandardProcessingRecipe.Serializer<>(PheromoneCatFoodMixingRecipe::new); }
    private CreateFactories() {}
}
