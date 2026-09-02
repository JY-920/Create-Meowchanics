package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.client.BreedingBoxRenderer;
import cn.laowu.mod.client.BreedingBoxScreen;
import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.content.kinetics.base.ShaftVisual;
import com.simibubi.create.foundation.item.TooltipModifier;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = LaoWuMod.MOD_ID, value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD)
public final class ClientModEvents {
    private static final ResourceLocation CAT_ENGINEER_GOGGLES_WORN_MODEL =
            LaoWuMod.id("item/cat_engineer_goggles_worn");
    public static final KeyMapping OPEN_HELD_ITEM_TRANSFORM = new KeyMapping(
            "key.laowu.held_item_transform",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            "key.categories.laowu");
    public static final KeyMapping CAT_ARMOR_POUNCE = new KeyMapping(
            "key.laowu.cat_armor_pounce", InputConstants.Type.KEYSYM,
            InputConstants.KEY_X, "key.categories.laowu");
    public static final KeyMapping CAT_TOOL_EMPOWER = new KeyMapping(
            "key.laowu.cat_tool_empower", InputConstants.Type.KEYSYM,
            InputConstants.KEY_LALT, "key.categories.laowu");
    public static final KeyMapping HISSING_VOLUME = new KeyMapping(
            "key.laowu.hissing_volume",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_V,
            "key.categories.laowu");

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_HELD_ITEM_TRANSFORM);
        event.register(CAT_ARMOR_POUNCE);
        event.register(CAT_TOOL_EMPOWER);
        event.register(HISSING_VOLUME);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LaoWuMod.id("cat_stats"),
                CatStatsGoggleOverlay.OVERLAY);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(LaoWuMod.NOZZLE_FLUID_PUFF.get(),
                NozzleFluidPuffParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager ->
                NozzleFluidPuffParticle.clearColourCache());
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager ->
                CatGenomeTextureManager.clear());
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager ->
                CatAppearanceTextures.clear());
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(LaoWuMod.CAT_PACKAGE_MENU.get(), CatPackageScreen::new);
        event.register(LaoWuMod.BREEDING_BOX_MENU.get(), BreedingBoxScreen::new);
        event.register(LaoWuMod.ADOPTION_BOX_MENU.get(), AdoptionBoxScreen::new);
        event.register(LaoWuMod.CAT_TRAIT_EDITOR_MENU.get(), CatTraitEditorScreen::new);
        event.register(LaoWuMod.CAT_ATTRIBUTE_EDITOR_MENU.get(), CatAttributeEditorScreen::new);
        event.register(LaoWuMod.CAT_MATERIAL_EDITOR_MENU.get(), CatMaterialEditorScreen::new);
        event.register(LaoWuMod.CAT_PROFILE_MENU.get(), CatProfileScreen::new);
        event.register(LaoWuMod.CAT_FILTER_MENU.get(), CatFilterScreen::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // KineticBlockEntityRenderer deliberately leaves rotating parts to
            // Flywheel whenever visualization is available. Keep our animated
            // Blockbench body in the normal BER and let Create's native shaft
            // visual provide the correctly lit, RPM-driven transmission rod.
            SimpleBlockEntityVisualizer.builder(LaoWuMod.CAT_ENGINE_BE.get())
                    .factory(ShaftVisual::new)
                    .neverSkipVanillaRender()
                    .apply();
            ItemProperties.register(LaoWuMod.CAT_POUCH.get(), LaoWuMod.id("filled"),
                    (stack, level, entity, seed) -> cn.laowu.mod.item.CatPouchItem.count(stack) > 0 ? 1.0F : 0.0F);
            registerEmpoweredProperty(LaoWuMod.CAT_SWORD.get());
            registerEmpoweredProperty(LaoWuMod.CAT_PICKAXE.get());
            registerEmpoweredProperty(LaoWuMod.CAT_AXE.get());
            registerEmpoweredProperty(LaoWuMod.CAT_SHOVEL.get());
            registerEmpoweredProperty(LaoWuMod.CAT_HOE.get());
            // Alpha is fully opaque in LiquidCatFluidType; the solid layer also
            // prevents it from inheriting water-like translucency.
            ItemBlockRenderTypes.setRenderLayer(LaoWuMod.LIQUID_CAT.get(), RenderType.solid());
            ItemBlockRenderTypes.setRenderLayer(LaoWuMod.FLOWING_LIQUID_CAT.get(), RenderType.solid());
            // Preserve the supplied model's one-pixel face details; mipmapping
            // turns those small UV islands into large solid colour squares.
            ItemBlockRenderTypes.setRenderLayer(LaoWuMod.HISSING_COLLECTOR.get(), RenderType.cutout());
            if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
                cn.laowu.mod.compat.curios.CatGogglesCuriosClientCompat.registerRenderer();
            }
            registerCareerSuitDescription(LaoWuMod.TERMINATOR_SUIT.get());
            registerCareerSuitDescription(LaoWuMod.FISHING_SUIT.get());
            registerCareerSuitDescription(LaoWuMod.FLIGHT_SUIT.get());
            registerCareerSuitDescription(LaoWuMod.FIRE_SUIT.get());
            registerCareerSuitDescription(LaoWuMod.HONEY_SUIT.get());
            registerCareerSuitDescription(LaoWuMod.TRANSPORT_SUIT.get());
            registerCareerSuitDescription(LaoWuMod.DYNAMITE_SUIT.get());
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityType.CAT, HissingCatRenderer::new);
        event.registerEntityRenderer(LaoWuMod.CAT_PANCAKE_PROJECTILE.get(),
                context -> new ThrownItemRenderer<>(context, 1.1F, false));
        event.registerEntityRenderer(LaoWuMod.FISHING_ROD_PROJECTILE.get(),
                FishingRodProjectileRenderer::new);
        event.registerEntityRenderer(LaoWuMod.MECHANICAL_LASER_PROJECTILE.get(),
                MechanicalLaserProjectileRenderer::new);
        event.registerEntityRenderer(LaoWuMod.HONEY_MISSILE_PROJECTILE.get(),
                HoneyMissileProjectileRenderer::new);
        event.registerEntityRenderer(LaoWuMod.DYNAMITE_PROJECTILE.get(),
                DynamiteProjectileRenderer::new);
        event.registerEntityRenderer(LaoWuMod.LOGISTICS_SUPPORT_PROJECTILE.get(),
                context -> new ThrownItemRenderer<>(context, 0.75F, false));
        event.registerEntityRenderer(LaoWuMod.CAT_BALL_ENTITY.get(),
                CatBallEntityRenderer::new);
        event.registerEntityRenderer(LaoWuMod.BUTTER_CAT.get(), ButterCatRenderer::new);
        event.registerBlockEntityRenderer(LaoWuMod.CAT_ENGINE_BE.get(), CatEngineRenderer::new);
        event.registerBlockEntityRenderer(LaoWuMod.DEVOURING_CAT_BE.get(), DevouringCatRenderer::new);
        event.registerBlockEntityRenderer(LaoWuMod.INFILTRATION_TANK_BE.get(), InfiltrationTankRenderer::new);
        event.registerBlockEntityRenderer(LaoWuMod.BREEDING_BOX_BE.get(), BreedingBoxRenderer::new);
        event.registerBlockEntityRenderer(LaoWuMod.ADOPTION_BOX_BE.get(), AdoptionBoxRenderer::new);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(CAT_ENGINEER_GOGGLES_WORN_MODEL));
        event.register(ModelResourceLocation.standalone(
                CatScannerItemRenderer.INVENTORY_MODEL));
        event.register(ModelResourceLocation.standalone(
                CatScannerItemRenderer.HANDHELD_MODEL));
    }

    @SubscribeEvent
    public static void wrapCatEngineerGogglesModel(ModelEvent.ModifyBakingResult event) {
        ModelResourceLocation inventoryId = ModelResourceLocation.inventory(
                LaoWuMod.id("cat_engineer_goggles"));
        var inventoryModel = event.getModels().get(inventoryId);
        var wornModel = event.getModels().get(ModelResourceLocation.standalone(
                CAT_ENGINEER_GOGGLES_WORN_MODEL));
        if (wornModel == null) wornModel = event.getModels().get(CAT_ENGINEER_GOGGLES_WORN_MODEL);
        if (inventoryModel != null && wornModel != null) {
            event.getModels().put(inventoryId,
                    new CatEngineerGogglesModel(inventoryModel, wornModel));
        }

        ModelResourceLocation scannerId = ModelResourceLocation.inventory(
                LaoWuMod.id("cat_scanner"));
        var scannerFlatModel = event.getModels().get(scannerId);
        var scannerHandheldModel = event.getModels().get(
                ModelResourceLocation.standalone(
                        CatScannerItemRenderer.HANDHELD_MODEL));
        if (scannerFlatModel != null && scannerHandheldModel != null) {
            event.getModels().put(scannerId,
                    new CatScannerBakedModel(scannerFlatModel,
                            scannerHandheldModel));
        }
    }

    @SubscribeEvent
    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        event.register(LaoWuMod.CAT_CANNON.get(), (graphics, font, cannon, x, y) -> {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player == null) return false;
            net.minecraft.world.item.ItemStack ammo = net.minecraft.world.item.ItemStack.EMPTY;
            for (var candidate : player.getInventory().items) {
                if (candidate.is(LaoWuMod.CAT_PANCAKE.get())
                        || candidate.is(LaoWuMod.CAT_GRENADE.get())) {
                    ammo = candidate.copyWithCount(1);
                    break;
                }
                if (candidate.getItem() instanceof cn.laowu.mod.item.CatPouchItem
                        && cn.laowu.mod.item.CatPouchItem.count(candidate) > 0)
                    ammo = cn.laowu.mod.item.CatPouchItem.peek(candidate);
            }
            if (ammo.isEmpty()) return false;
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(x, y + 8, 100.0F);
            pose.scale(0.5F, 0.5F, 0.5F);
            graphics.renderItem(ammo, 0, 0);
            pose.popPose();
            return false;
        });
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(HissingCatModel.LAYER, HissingCatModel::createLayer);
        event.registerLayerDefinition(CatAppearanceModel.LAYER,
                CatAppearanceModel::createLayer);
        event.registerLayerDefinition(KimiArmorModel.LAYER, KimiArmorModel::createLayer);
        event.registerLayerDefinition(KimiArmorModel.SLIM_LAYER, KimiArmorModel::createSlimLayer);
    }

    private static void registerEmpoweredProperty(net.minecraft.world.item.Item item) {
        ItemProperties.register(item, LaoWuMod.id("empowered"),
                (stack, level, entity, seed) ->
                        cn.laowu.mod.item.CatToolBehavior.isEmpowered(stack) ? 1.0F : 0.0F);
    }

    private static void registerCareerSuitDescription(net.minecraft.world.item.Item item) {
        if (!(item instanceof cn.laowu.mod.item.TerminatorSuitItem suit)) return;
        TooltipModifier.REGISTRY.register(item,
                event -> CareerSuitTooltip.modify(event, item, suit.outfit()));
    }
    private ClientModEvents() {}
}
