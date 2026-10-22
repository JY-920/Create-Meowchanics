package cn.laowu.mod.client;
import cn.laowu.mod.*;
import cn.laowu.mod.compat.create.CreateIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid="laowu", value=Dist.CLIENT)
public final class StandaloneCatClientProbe {
    private static int ticks, frames;
    private static boolean failed;
    private static net.minecraft.world.entity.animal.Cat previewCat;
    @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        
        var mc=Minecraft.getInstance();
        if(failed)return;
        if (ticks==200 && mc.level==null && mc.getSingleplayerServer()==null) mc.createWorldOpenFlows().openWorld("Standalone",mc::stop);
        if (++ticks > 2400) { System.err.println("FAIL: STANDALONE CLIENT TIMEOUT"); mc.stop(); return; }
        if(mc.getOverlay()!=null || mc.level==null || mc.player==null || !mc.gameRenderer.getMainCamera().isInitialized()) return;
        try {
            if(frames++==0) {
                org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
                mc.options.guiScale().set(2);
                org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),1280,900);
                if(CreateIntegration.isLoaded()!=Boolean.getBoolean("laowu.create_expected")) throw new AssertionError("Unexpected Create environment");
                var server=mc.getSingleplayerServer();
                var playerId=mc.player.getUUID();
                server.execute(()->{
                    var player=server.getPlayerList().getPlayer(playerId);
                    if(player!=null) {
                        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                        player.setHealth(player.getMaxHealth());
                        player.setAirSupply(300);
                    }
                });
                OptionalCreateClientRegression.renderCoreItems(mc);
                OptionalCreateClientRegression.verifyStandaloneExtras(mc);
                if(!Boolean.getBoolean("laowu.render_only"))OptionalCreateClientRegression.verifyTooltipsAndIcon(mc);
                var blockPos=mc.player.blockPosition().offset(2,0,0);
                mc.level.setBlock(blockPos,net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(),3);
                var oldHit=mc.hitResult;
                mc.hitResult=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(blockPos),
                        net.minecraft.core.Direction.UP,blockPos,false);
                if(CatWorldTarget.find(mc,5)!=null)throw new AssertionError("Vanilla chest is not a cat");
                mc.hitResult=oldHit;
                net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS,true,mc.level.registryAccess());
                for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                    if(!net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("laowu"))continue;
                    new ItemStack(item).getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(mc.level),mc.player,net.minecraft.world.item.TooltipFlag.NORMAL);
                }
                var cat=EntityType.CAT.create(mc.level);
                previewCat=cat;
                var carrier=new cn.laowu.mod.entity.CatFlightCarrier(LaoWuMod.CAT_FLIGHT_CARRIER.get(),mc.level);
                mc.player.startRiding(carrier,true);
                var playerModel=new net.minecraft.client.model.PlayerModel<net.minecraft.world.entity.LivingEntity>(
                        mc.getEntityModels().bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER),false);
                playerModel.setupAnim(mc.player,0,0,0,0,0);
                if(!CreateIntegration.isLoaded()&&(playerModel.leftArm.xRot>-2.5F||playerModel.rightArm.xRot>-2.5F))
                    throw new AssertionError("Standalone pilot rider pose missing");
                mc.player.stopRiding();
                var cannon=new cn.laowu.mod.entity.EngineeringCannon(LaoWuMod.ENGINEERING_CANNON.get(),mc.level);
                cannon.setPos(mc.player.position());
                mc.getEntityRenderDispatcher().getRenderer(cannon).render(cannon,0,0,
                        new com.mojang.blaze3d.vertex.PoseStack(),mc.renderBuffers().bufferSource(),
                        net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
                mc.renderBuffers().bufferSource().endBatch();
                cat.setId(-99821); cat.setPos(mc.player.position());
                mc.level.addEntity(cat);
                var model=mc.getItemRenderer().getModel(new ItemStack(LaoWuMod.CAT_PANCAKE.get()),mc.level,null,0);
                if(model==mc.getModelManager().getMissingModel())throw new AssertionError("Core item model missing");
                mc.setScreen(new CatProfileScreen(new CatProfileMenu(91,mc.player.getInventory(),cat),mc.player.getInventory(),Component.literal("Standalone")));
            } else if (frames < 60 && frames % 3 == 0) {
                var outfits=CatOutfitType.values();
                var outfit=outfits[(frames/3-1)%outfits.length];
                previewCat.getPersistentData().putString(CatClothesData.OUTFIT_TAG,outfit.id());
            } else if (frames>=80) {
                if (frames==81) {
                    var pancake=cn.laowu.mod.item.CatPancakeItem.defaultDisplayStack();
                    var texture=CatGenomeTextureManager.resolve(pancake,cn.laowu.mod.item.CatPancakeItem.texture(pancake));
                    if(mc.getResourceManager().getResource(cn.laowu.mod.item.CatPancakeItem.texture(pancake)).isEmpty())
                        throw new AssertionError("Default pancake texture missing: "+texture);
                    System.out.println("PANCAKE TEXTURE: "+texture);
                    mc.player.getInventory().setItem(0,pancake);
                    mc.player.getInventory().setItem(1,new ItemStack(LaoWuMod.CAT_CAN.get()));
                    mc.player.getInventory().setItem(2,new ItemStack(LaoWuMod.CAT_STORAGE_BOX.get()));
                    for (var block:java.util.List.of(LaoWuMod.BASIC_BREEDING_BOX.get(),LaoWuMod.INTERMEDIATE_BREEDING_BOX.get(),LaoWuMod.ADVANCED_BREEDING_BOX.get(),LaoWuMod.ADOPTION_BOX.get(),LaoWuMod.WISH_ADOPTION_BOX.get())) {
                        var pos=mc.player.blockPosition().offset(2,0,0);
                        mc.level.setBlock(pos,block.defaultBlockState(),3);
                        var entity=mc.level.getBlockEntity(pos);
                        var renderer=mc.getBlockEntityRenderDispatcher().getRenderer(entity);
                        if(renderer==null)throw new AssertionError("Core care renderer missing: "+block);
                        renderer.render(entity,0,new com.mojang.blaze3d.vertex.PoseStack(),mc.renderBuffers().bufferSource(),net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                        mc.renderBuffers().bufferSource().endBatch();
                    }
                    mc.setScreen(net.minecraft.client.gui.screens.MenuScreens.getScreenFactory(LaoWuMod.ADOPTION_BOX_MENU.get()).orElseThrow().create(new AdoptionBoxMenu(92,mc.player.getInventory(),(cn.laowu.mod.create.AdoptionBoxBlockEntity)null),mc.player.getInventory(),Component.literal("领养箱")));
                    if(!(mc.screen instanceof AdoptionBoxScreen))throw new AssertionError("Core adoption screen registration missing");
                } else if(frames==100) {
                    if(!(mc.screen instanceof AdoptionBoxScreen))throw new AssertionError("Adoption GUI not retained for actual rendering");
                    net.minecraft.client.Screenshot.grab(mc.gameDirectory,mc.getMainRenderTarget(),message->{});
                    mc.setScreen(net.minecraft.client.gui.screens.MenuScreens.getScreenFactory(LaoWuMod.WISH_ADOPTION_BOX_MENU.get()).orElseThrow().create(new WishAdoptionBoxMenu(93,mc.player.getInventory(),(cn.laowu.mod.create.WishAdoptionBoxBlockEntity)null),mc.player.getInventory(),Component.literal("心愿领养箱")));
                    if(!(mc.screen instanceof WishAdoptionBoxScreen))throw new AssertionError("Core wish screen registration missing");
                } else if(frames==120) {
                    if(!(mc.screen instanceof WishAdoptionBoxScreen))throw new AssertionError("Wish GUI not retained for actual rendering");
                    net.minecraft.client.Screenshot.grab(mc.gameDirectory,mc.getMainRenderTarget(),message->{});
                    mc.setScreen(net.minecraft.client.gui.screens.MenuScreens.getScreenFactory(LaoWuMod.BREEDING_BOX_MENU.get()).orElseThrow().create(new BreedingBoxMenu(94,mc.player.getInventory(),(cn.laowu.mod.create.BreedingBoxData)null),mc.player.getInventory(),Component.literal("繁育箱")));
                    if(!(mc.screen instanceof BreedingBoxScreen))throw new AssertionError("Core breeding screen registration missing");
                } else if(frames>=140) {
                    if(Boolean.getBoolean("laowu.jei_pancake_visual")&&frames>140) {
                        if(frames>=175) {
                            if(!mc.screen.getClass().getName().startsWith("mezz.jei."))
                                throw new AssertionError("Actual JEI recipe screen did not open");
                            net.minecraft.client.Screenshot.grab(mc.gameDirectory,"jei-orange-pancake.png",mc.getMainRenderTarget(),message->{});
                            System.out.println("PASS: REAL JEI ORANGE PANCAKE SCREENSHOT");mc.stop();
                        }
                        return;
                    }
                    if (net.neoforged.fml.ModList.get().isLoaded("jei")) StandaloneRecipeJeiProbe.verify(mc);
                    if(!(mc.screen instanceof BreedingBoxScreen))throw new AssertionError("Breeding GUI not retained for actual rendering");
                    net.minecraft.client.Screenshot.grab(mc.gameDirectory,mc.getMainRenderTarget(),message->{});
                    System.out.println("PASS: CAT CLIENT + ALL CARE GUI REGISTRATIONS + FIVE RENDERERS + PANCAKE TEXTURE; Create="+CreateIntegration.isLoaded());
                    if(Boolean.getBoolean("laowu.jei_pancake_visual")) {
                        StandaloneRecipeJeiProbe.showPancakeRecipe();return;
                    }
                    mc.stop();
                }
            }
        } catch(Throwable error) { failed=true;error.printStackTrace();System.err.println("FAIL: STANDALONE CAT CLIENT");mc.stop(); }
    }
}
