package cn.laowu.mod.compat.create;
import cn.laowu.mod.client.*;
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
public final class CreateClientEvents {
    public static java.util.List<net.minecraft.network.chat.Component> tooltip(String text) {
        return com.simibubi.create.foundation.item.TooltipHelper.cutStringTextComponent(text, net.createmod.catnip.lang.FontHelper.Palette.STANDARD_CREATE);
    }
    public static java.util.List<net.minecraft.network.chat.Component> tooltip(String text, net.minecraft.network.chat.Style primary, net.minecraft.network.chat.Style highlight, int indent) {
        return com.simibubi.create.foundation.item.TooltipHelper.cutStringTextComponent(text, primary, highlight, indent);
    }
    public static void renderPlayerInventory(net.minecraft.client.gui.GuiGraphics graphics, int x, int y) {
        com.simibubi.create.foundation.gui.AllGuiTextures.PLAYER_INVENTORY.render(graphics, x, y);
    }
    private static final ResourceLocation CAT_ENGINEER_GOGGLES_WORN_MODEL = LaoWuMod.id("item/cat_engineer_goggles_worn");
    public static void register(net.neoforged.bus.api.IEventBus bus) {
        bus.register(CatBeltModel.class);
        bus.register(CatMachinesClient.class);
        bus.register(CatDepotClient.class);
        bus.register(cn.laowu.mod.ponder.generated.GeneratedPonderForgeClient.class);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(CatAutoLaserFilterHighlight.class);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(CreatureTransmitterRange.class);
    }
    public static void initDeployment() { CatDeploymentRenderer.init(); }
    public static void setup() {
            SimpleBlockEntityVisualizer.builder(LaoWuMod.CAT_ENGINE_BE.get())
                    .factory(ShaftVisual::new)
                    .neverSkipVanillaRender()
                    .apply();
            ItemProperties.register(LaoWuMod.CAT_STORAGE_BOX.get(), LaoWuMod.id("filled"),
                    (stack, level, entity, seed) -> cn.laowu.mod.item.CatStorageBoxItem.count(stack) > 0 ? 1.0F : 0.0F);
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

            registerCareerSuitDescription(LaoWuMod.ENGINEERING_SUIT.get());

            registerCareerSuitDescription(LaoWuMod.MEDICAL_SUIT.get());

            registerCareerSuitDescription(LaoWuMod.MUSIC_SUIT.get());

            registerCareerSuitDescription(LaoWuMod.AGENT_SUIT.get());

            registerCareerSuitDescription(LaoWuMod.DIVING_SUIT.get());

            registerCareerSuitDescription(LaoWuMod.COCKROACH_SUIT.get());

    }
    public static void menus(RegisterMenuScreensEvent event) {

        event.register(LaoWuMod.CAT_PACKAGE_MENU.get(), CatPackageScreen::new);

        event.register(LaoWuMod.BREEDING_BOX_MENU.get(), BreedingBoxScreen::new);

        event.register(LaoWuMod.ADOPTION_BOX_MENU.get(), AdoptionBoxScreen::new);

        event.register(LaoWuMod.WISH_ADOPTION_BOX_MENU.get(), WishAdoptionBoxScreen::new);

        event.register(LaoWuMod.CAT_EDITOR_MENU.get(), CatEditorScreen::new);

        event.register(LaoWuMod.CAT_FILTER_MENU.get(), CatFilterScreen::new);

        event.register(LaoWuMod.CREATURE_FILTER_MENU.get(), CreatureFilterScreen::new);
    }
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(LaoWuMod.CAT_ENGINE_BE.get(), CatEngineRenderer::new);
        event.registerBlockEntityRenderer(LaoWuMod.DEVOURING_CAT_BE.get(), DevouringCatRenderer::new);
        event.registerBlockEntityRenderer(LaoWuMod.INFILTRATION_TANK_BE.get(), InfiltrationTankRenderer::new);
        event.registerBlockEntityRenderer(LaoWuMod.BREEDING_BOX_BE.get(), BreedingBoxRenderer::new);
        event.registerBlockEntityRenderer(LaoWuMod.ADOPTION_BOX_BE.get(), AdoptionBoxRenderer::new);
        event.registerBlockEntityRenderer(LaoWuMod.WISH_ADOPTION_BOX_BE.get(), WishAdoptionBoxRenderer::new);
        event.registerBlockEntityRenderer(LaoWuMod.CAT_CARRIER_BE.get(), CatCarrierRenderer::new);
        event.registerBlockEntityRenderer(LaoWuMod.CAT_DEPLOYMENT_BE.get(), CatDeploymentRenderer::new);
    }
    public static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(LaoWuMod.NOZZLE_FLUID_PUFF.get(),
                NozzleFluidPuffParticle.Provider::new);
    }
    public static void reloads(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager ->
                NozzleFluidPuffParticle.clearColourCache());
    }
    public static void models(ModelEvent.RegisterAdditional event) {

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

    private static void registerCareerSuitDescription(net.minecraft.world.item.Item item) {
        if (!(item instanceof cn.laowu.mod.item.TerminatorSuitItem suit)) return;
        TooltipModifier.REGISTRY.register(item,
                event -> CareerSuitTooltip.modify(event, item, suit.outfit()));
    }
}
