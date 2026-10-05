package cn.laowu.mod.compat.create;
import cn.laowu.mod.client.*;
import cn.laowu.mod.LaoWuMod;
import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.content.kinetics.base.ShaftVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
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
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        bus.register(CatBeltModel.class);
        bus.register(CatMachinesClient.class);
        bus.register(CatDepotClient.class);
        bus.register(cn.laowu.mod.ponder.generated.GeneratedPonderForgeClient.class);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(CatAutoLaserFilterHighlight.class);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(CreatureTransmitterRange.class);
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

            if (ModList.get().isLoaded("curios")) {
                cn.laowu.mod.compat.curios.CatGogglesCuriosClientCompat.registerRenderer();
            }
    }
    public static void menus() {

            MenuScreens.register(LaoWuMod.CAT_PACKAGE_MENU.get(), CatPackageScreen::new);

            MenuScreens.register(LaoWuMod.BREEDING_BOX_MENU.get(), BreedingBoxScreen::new);

            MenuScreens.register(LaoWuMod.ADOPTION_BOX_MENU.get(), AdoptionBoxScreen::new);

            MenuScreens.register(LaoWuMod.WISH_ADOPTION_BOX_MENU.get(), WishAdoptionBoxScreen::new);

            MenuScreens.register(LaoWuMod.CAT_EDITOR_MENU.get(), CatEditorScreen::new);

            MenuScreens.register(LaoWuMod.CAT_FILTER_MENU.get(), CatFilterScreen::new);

            MenuScreens.register(LaoWuMod.CREATURE_FILTER_MENU.get(), CreatureFilterScreen::new);
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
        event.register(LaoWuMod.id("block/cat_ejecting_deployment_platform_plate"));

        event.register(LaoWuMod.id("block/cat_ejecting_deployment_platform_rod"));

        event.register(CAT_ENGINEER_GOGGLES_WORN_MODEL);
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

}
