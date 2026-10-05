package cn.laowu.mod.compat.create;

import cn.laowu.mod.*;
import static cn.laowu.mod.LaoWuMod.*;
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

/** Factories are resolved only when Create is present, never from the core bootstrap. */
public final class CreateFactories {
    public static ParticleType<NozzleFluidPuffData> particle_types_nozzle_fluid_puff() { return new ParticleType<>(false, NozzleFluidPuffData.DESERIALIZER) {
                        @Override
                        public Codec<NozzleFluidPuffData> codec() {
                            return NozzleFluidPuffData.CODEC;
                        }
                    }; }
    public static Block blocks_cat_engine() { return new CatEngineBlock(BlockBehaviour.Properties.of()
                    .noOcclusion().strength(3.0F, 6.0F).requiresCorrectToolForDrops()); }
    public static BlockEntityType<CatEngineBlockEntity> block_entities_cat_engine() { return BlockEntityType.Builder
                    .of(CatEngineBlockEntity::new, CAT_ENGINE.get()).build(null); }
    public static Block blocks_infiltration_tank() { return new InfiltrationTankBlock(BlockBehaviour.Properties.of()
                    .noOcclusion().strength(3.0F, 6.0F).requiresCorrectToolForDrops()); }
    public static BlockEntityType<InfiltrationTankBlockEntity> block_entities_infiltration_tank() { return BlockEntityType.Builder
                    .of(InfiltrationTankBlockEntity::new, INFILTRATION_TANK.get()).build(null); }
    public static Block blocks_hissing_collector() { return new HissingCollectorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .noOcclusion().strength(1.5F, 6.0F)); }
    public static BlockEntityType<HissingCollectorBlockEntity> block_entities_hissing_collector() { return BlockEntityType.Builder
                    .of(HissingCollectorBlockEntity::new, HISSING_COLLECTOR.get()).build(null); }
    public static Block blocks_devouring_cat() { return new DevouringCatBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .noOcclusion().strength(3.0F, 6.0F)); }
    public static BlockEntityType<DevouringCatBlockEntity> block_entities_devouring_cat() { return BlockEntityType.Builder
                    .of(DevouringCatBlockEntity::new, DEVOURING_CAT.get()).build(null); }
    public static Block blocks_basic_breeding_box() { return new BreedingBoxBlock(BreedingBoxTier.BASIC,
                    BlockBehaviour.Properties.copy(Blocks.BARREL).noOcclusion().strength(0.8F)); }
    public static Block blocks_intermediate_breeding_box() { return new BreedingBoxBlock(BreedingBoxTier.INTERMEDIATE,
                    BlockBehaviour.Properties.copy(Blocks.BARREL).noOcclusion().strength(1.2F)); }
    public static Block blocks_advanced_breeding_box() { return new BreedingBoxBlock(BreedingBoxTier.ADVANCED,
                    BlockBehaviour.Properties.copy(Blocks.BARREL).noOcclusion().strength(1.6F)); }
    public static BlockEntityType<BreedingBoxBlockEntity> block_entities_breeding_box() { return BlockEntityType.Builder
                    .of(BreedingBoxBlockEntity::new, BASIC_BREEDING_BOX.get(),
                            INTERMEDIATE_BREEDING_BOX.get(), ADVANCED_BREEDING_BOX.get())
                    .build(null); }
    public static Block blocks_cat_carrier() { return new cn.laowu.mod.create.CatCarrierBlock(BlockBehaviour.Properties.of().strength(2.0F).noOcclusion()); }
    public static BlockEntityType<cn.laowu.mod.create.CatCarrierBlockEntity> block_entities_cat_carrier() { return BlockEntityType.Builder
                    .of(cn.laowu.mod.create.CatCarrierBlockEntity::new, CAT_CARRIER.get()).build(null); }
    public static Item items_cat_carrier() { return new cn.laowu.mod.item.CatCarrierBlockItem(CAT_CARRIER.get(), new Item.Properties()); }
    public static Block blocks_cat_editor() { return new cn.laowu.mod.create.CatEditorBlock(BlockBehaviour.Properties.of().strength(2F).noOcclusion()); }
    public static Item items_cat_editor() { return new BlockItem(CAT_EDITOR.get(), new Item.Properties()); }
    public static MenuType<CatEditorMenu> menus_cat_editor() { return IForgeMenuType.create(CatEditorMenu::new); }
    public static Block blocks_cat_deployment_platform() { return new cn.laowu.mod.create.CatDeploymentBlock(BlockBehaviour.Properties.of().strength(2F).noOcclusion(), false); }
    public static Block blocks_cat_ejecting_deployment_platform() { return new cn.laowu.mod.create.CatDeploymentBlock(BlockBehaviour.Properties.of().strength(2F).noOcclusion(), true); }
    public static BlockEntityType<cn.laowu.mod.create.CatDeploymentBlockEntity> block_entities_cat_deployment_platform() { return BlockEntityType.Builder.of(cn.laowu.mod.create.CatDeploymentBlockEntity::new, CAT_DEPLOYMENT_PLATFORM.get(), CAT_EJECTING_DEPLOYMENT_PLATFORM.get()).build(null); }
    public static Item items_cat_deployment_platform() { return new cn.laowu.mod.item.CatDeploymentBlockItem(CAT_DEPLOYMENT_PLATFORM.get(), new Item.Properties()); }
    public static Item items_cat_ejecting_deployment_platform() { return new cn.laowu.mod.item.CatDeploymentBlockItem(CAT_EJECTING_DEPLOYMENT_PLATFORM.get(), new Item.Properties()); }
    public static Item items_cat_storage_box() { return new cn.laowu.mod.item.CatStorageBoxItem(new Item.Properties()); }
    public static Block blocks_adoption_box() { return new AdoptionBoxBlock(BlockBehaviour.Properties.copy(Blocks.BARREL)
                    .noOcclusion().strength(0.8F)); }
    public static BlockEntityType<AdoptionBoxBlockEntity> block_entities_adoption_box() { return BlockEntityType.Builder
                    .of(AdoptionBoxBlockEntity::new, ADOPTION_BOX.get()).build(null); }
    public static Block blocks_wish_adoption_box() { return new WishAdoptionBoxBlock(BlockBehaviour.Properties.copy(Blocks.BARREL)
                    .noOcclusion().strength(0.8F)); }
    public static BlockEntityType<WishAdoptionBoxBlockEntity> block_entities_wish_adoption_box() { return BlockEntityType.Builder
                    .of(WishAdoptionBoxBlockEntity::new, WISH_ADOPTION_BOX.get()).build(null); }
    public static Item items_creature_filter() { return new CreatureFilterItem(new Item.Properties()); }
    public static Item items_cat_filter() { return new CatFilterItem(new Item.Properties().stacksTo(1)); }
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
    public static Item items_cat_engineer_goggles() { return new CatEngineerGogglesItem(new Item.Properties().stacksTo(1)); }
    public static Item items_cat_cannon() { return new CatCannonItem(new Item.Properties().stacksTo(1)); }
    public static Item items_incomplete_cat_grenade() { return new com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem(new Item.Properties()); }
    public static Item items_cat_engine() { return new CatEngineBlockItem(CAT_ENGINE.get(), new Item.Properties()); }
    public static Item items_infiltration_tank() { return new BlockItem(INFILTRATION_TANK.get(), new Item.Properties()); }
    public static Item items_hissing_collector() { return new BlockItem(HISSING_COLLECTOR.get(), new Item.Properties()); }
    public static Item items_devouring_cat() { return new DevouringCatBlockItem(DEVOURING_CAT.get(), new Item.Properties()); }
    public static Item items_basic_breeding_box() { return new BreedingBoxBlockItem(
                    BASIC_BREEDING_BOX.get(), new Item.Properties()); }
    public static Item items_intermediate_breeding_box() { return new BreedingBoxBlockItem(
                    INTERMEDIATE_BREEDING_BOX.get(), new Item.Properties()); }
    public static Item items_advanced_breeding_box() { return new BreedingBoxBlockItem(
                    ADVANCED_BREEDING_BOX.get(), new Item.Properties()); }
    public static Item items_adoption_box() { return new AdoptionBoxBlockItem(ADOPTION_BOX.get(), new Item.Properties()); }
    public static Item items_wish_adoption_box() { return new WishAdoptionBoxBlockItem(WISH_ADOPTION_BOX.get(), new Item.Properties()); }
    public static MenuType<CatPackageMenu> menus_cat_package() { return IForgeMenuType.create(CatPackageMenu::new); }
    public static MenuType<BreedingBoxMenu> menus_breeding_box() { return IForgeMenuType.create(BreedingBoxMenu::new); }
    public static MenuType<AdoptionBoxMenu> menus_adoption_box() { return IForgeMenuType.create(AdoptionBoxMenu::new); }
    public static MenuType<WishAdoptionBoxMenu> menus_wish_adoption_box() { return IForgeMenuType.create(WishAdoptionBoxMenu::new); }
    public static MenuType<CreatureFilterMenu> menus_creature_filter() { return IForgeMenuType.create(CreatureFilterMenu::new); }
    public static MenuType<CatFilterMenu> menus_cat_filter() { return IForgeMenuType.create(CatFilterMenu::new); }
    public static RecipeType<InfiltratingRecipe> recipe_types_infiltrating() { return new RecipeType<>() {
                @Override public String toString() { return MOD_ID + ":infiltrating"; }
            }; }
    public static RecipeSerializer<InfiltratingRecipe> recipe_serializers_infiltrating() { return new ProcessingRecipeSerializer<>(InfiltratingRecipe::new); }
    public static RecipeSerializer<RandomBabyCatPancakeFillingRecipe> recipe_serializers_random_baby_cat_pancake_filling() { return new ProcessingRecipeSerializer<>(
                            RandomBabyCatPancakeFillingRecipe::new); }
    public static RecipeSerializer<PheromoneCatFoodMixingRecipe> recipe_serializers_pheromone_cat_food_mixing() { return new ProcessingRecipeSerializer<>(
                            PheromoneCatFoodMixingRecipe::new); }
    private CreateFactories() {}
}
