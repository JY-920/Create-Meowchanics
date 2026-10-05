package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

@Mod.EventBusSubscriber(modid = LaoWuMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private static final ResourceLocation CAT_ENGINEER_GOGGLES_WORN_MODEL =
            LaoWuMod.id("item/cat_engineer_goggles_worn");
    public static final KeyMapping OPEN_HELD_ITEM_TRANSFORM = new KeyMapping(
            "key.laowu.held_item_transform",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            "key.categories.laowu");
    public static final KeyMapping CAT_ARMOR_POUNCE = new KeyMapping(
            "key.laowu.cat_armor_pounce",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_X,
            "key.categories.laowu");
    public static final KeyMapping CAT_TOOL_EMPOWER = new KeyMapping(
            "key.laowu.cat_tool_empower",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_LALT,
            "key.categories.laowu");
    public static final KeyMapping PILOT_DESCEND = new KeyMapping(
            "key.laowu.pilot_descend", InputConstants.Type.KEYSYM, InputConstants.KEY_LCONTROL,
            "key.categories.laowu");
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
        event.register(PILOT_DESCEND);
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("cat_stats", CatStatsGoggleOverlay.OVERLAY);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded()) cn.laowu.mod.compat.create.CreateClientEvents.particles(event);
        event.registerSpriteSet(LaoWuMod.CAT_HEALING_SMOKE.get(),CatHealingSmokeParticle.Provider::new);
        event.registerSpriteSet(LaoWuMod.CAT_AGENT_SMOKE.get(),CatHealingSmokeParticle.SmokeProvider::new);

    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded()) cn.laowu.mod.compat.create.CreateClientEvents.reloads(event);
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager ->
                CatTraitTokenItemRenderer.clearCache());
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> KimiArmorDyeTextures.clear());

        event.registerReloadListener((ResourceManagerReloadListener) resourceManager ->
                CatGenomeTextureManager.clear());
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager ->
                CatAppearanceTextures.clear());
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded()) cn.laowu.mod.compat.create.CreateClientEvents.initDeployment();
        event.enqueueWork(() -> {
            if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded()) cn.laowu.mod.compat.create.CreateClientEvents.setup();
            if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded()) cn.laowu.mod.compat.create.CreateClientEvents.menus();
            for (var armor : java.util.List.of(LaoWuMod.CAT_HELMET.get(), LaoWuMod.CAT_CHESTPLATE.get(),
                    LaoWuMod.CAT_LEGGINGS.get(), LaoWuMod.CAT_BOOTS.get()))
                ItemProperties.register(armor, LaoWuMod.id("kimi_dyed"),
                        (stack, level, entity, seed) -> cn.laowu.mod.item.KimiArmorDye.read(stack) == 0 ? 0F : 1F);
            MenuScreens.register(LaoWuMod.CAT_ATTRIBUTE_EDITOR_MENU.get(),
                    CatAttributeEditorScreen::new);
            MenuScreens.register(LaoWuMod.CAT_TRAIT_EDITOR_MENU.get(),
                    CatTraitEditorScreen::new);
            MenuScreens.register(LaoWuMod.CAT_MATERIAL_EDITOR_MENU.get(),
                    CatMaterialEditorScreen::new);
            MenuScreens.register(LaoWuMod.CAT_PROFILE_MENU.get(), CatProfileScreen::new);
            // KineticBlockEntityRenderer deliberately leaves rotating parts to
            // Flywheel whenever visualization is available. Keep our animated
            // Blockbench body in the normal BER and let Create's native shaft
            // visual provide the correctly lit, RPM-driven transmission rod.

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

        });
    }

    @SubscribeEvent
    public static void addMedicalPatientLayers(EntityRenderersEvent.AddLayers event) {
        for (var type : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE)
            if (type != EntityType.CAT) addMedicalLayer(event.getEntityRenderer(type));
        for (var skin : event.getSkins()) addMedicalLayer(event.getPlayerSkin(skin));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addMedicalLayer(net.minecraft.client.renderer.entity.EntityRenderer<?> renderer) {
        if (renderer instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer living)
            living.addLayer(new MedicalPatientLayer(living));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded()) cn.laowu.mod.compat.create.CreateClientEvents.renderers(event);
        event.registerEntityRenderer(EntityType.CAT, HissingCatRenderer::new);
        event.registerEntityRenderer(LaoWuMod.CAT_PANCAKE_PROJECTILE.get(),
                context -> new ThrownItemRenderer<>(context, 1.1F, false));
        event.registerEntityRenderer(LaoWuMod.FISHING_ROD_PROJECTILE.get(),
                FishingRodProjectileRenderer::new);
        event.registerEntityRenderer(LaoWuMod.MECHANICAL_LASER_PROJECTILE.get(),
                MechanicalLaserProjectileRenderer::new);
        event.registerEntityRenderer(LaoWuMod.ENGINEERING_CANNON.get(), EngineeringCannonRenderer::new);
        event.registerEntityRenderer(LaoWuMod.CAT_FLIGHT_CARRIER.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(LaoWuMod.CAT_DIVING_CARRIER.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(LaoWuMod.CAT_GIANT_CARRIER.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(LaoWuMod.CAT_HONEY_PATCH.get(),CatHoneyPatchRenderer::new);
        // Retain the legacy saved entity, but never draw its old fire-charge item.
        event.registerEntityRenderer(LaoWuMod.AGENT_SMOKE_BOMB.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(LaoWuMod.ENGINEERING_COGWHEEL_PROJECTILE.get(), EngineeringCogwheelRenderer::new);
        event.registerEntityRenderer(LaoWuMod.HONEY_MISSILE_PROJECTILE.get(),
                HoneyMissileProjectileRenderer::new);
        event.registerEntityRenderer(LaoWuMod.DYNAMITE_PROJECTILE.get(),
                DynamiteProjectileRenderer::new);
        event.registerEntityRenderer(LaoWuMod.LOGISTICS_SUPPORT_PROJECTILE.get(),
                // The package model inherits Minecraft's 0.25x ground transform;
                // 2.4x here produces a final 0.6x package-sized projectile.
                context -> new ThrownItemRenderer<>(context, 2.4F, false));
        event.registerEntityRenderer(LaoWuMod.CAT_BALL_ENTITY.get(),
                CatBallEntityRenderer::new);
        event.registerEntityRenderer(LaoWuMod.BUTTER_CAT.get(), ButterCatRenderer::new);
        event.registerEntityRenderer(LaoWuMod.GIANT_CAT_BOSS.get(), GiantCatBossRenderer::new);








    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded()) cn.laowu.mod.compat.create.CreateClientEvents.models(event);
        if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded()) cn.laowu.mod.compat.create.CreateClientEvents.initDeployment();

        event.register(CatScannerItemRenderer.INVENTORY_MODEL);
        event.register(CatScannerItemRenderer.HANDHELD_MODEL);
    }

    @SubscribeEvent
    public static void wrapCatEngineerGogglesModel(ModelEvent.ModifyBakingResult event) {
        ModelResourceLocation inventoryId = new ModelResourceLocation(
                LaoWuMod.id("cat_engineer_goggles"), "inventory");
        var inventoryModel = event.getModels().get(inventoryId);
        var wornModel = event.getModels().get(CAT_ENGINEER_GOGGLES_WORN_MODEL);
        if (inventoryModel != null && wornModel != null) {
            event.getModels().put(inventoryId,
                    new CatEngineerGogglesModel(inventoryModel, wornModel));
        }

        ModelResourceLocation scannerId = new ModelResourceLocation(
                LaoWuMod.id("cat_scanner"), "inventory");
        var scannerFlatModel = event.getModels().get(scannerId);
        var scannerHandheldModel = event.getModels().get(
                CatScannerItemRenderer.HANDHELD_MODEL);
        if (scannerFlatModel != null && scannerHandheldModel != null) {
            event.getModels().put(scannerId,
                    new CatScannerBakedModel(scannerFlatModel, scannerHandheldModel));
        }
    }

    @SubscribeEvent
    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        if (cn.laowu.mod.compat.create.CreateIntegration.isLoaded()) cn.laowu.mod.compat.create.CreateClientEvents.registerItemDecorations(event);
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

    private ClientModEvents() {}
}
