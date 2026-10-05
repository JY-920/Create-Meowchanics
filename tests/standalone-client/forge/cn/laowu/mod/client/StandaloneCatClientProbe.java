package cn.laowu.mod.client;
import cn.laowu.mod.*;
import cn.laowu.mod.compat.create.CreateIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
@EventBusSubscriber(modid="laowu", value=Dist.CLIENT)
public final class StandaloneCatClientProbe {
    private static int ticks, frames;
    private static net.minecraft.world.entity.animal.Cat previewCat;
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        var mc=Minecraft.getInstance();
        if (++ticks > 2400) { System.err.println("FAIL: STANDALONE CLIENT TIMEOUT"); mc.stop(); return; }
        if(mc.getOverlay()!=null || mc.level==null || mc.player==null) return;
        try {
            if(frames++==0) {
                org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
                if(CreateIntegration.isLoaded()) throw new AssertionError("Create unexpectedly present");
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
                    new ItemStack(item).getTooltipLines(mc.player,net.minecraft.world.item.TooltipFlag.NORMAL);
                }
                var cat=EntityType.CAT.create(mc.level);
                previewCat=cat;
                var carrier=new cn.laowu.mod.entity.CatFlightCarrier(LaoWuMod.CAT_FLIGHT_CARRIER.get(),mc.level);
                mc.player.startRiding(carrier,true);
                var playerModel=new net.minecraft.client.model.PlayerModel<net.minecraft.world.entity.LivingEntity>(
                        mc.getEntityModels().bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER),false);
                playerModel.setupAnim(mc.player,0,0,0,0,0);
                if(playerModel.leftArm.xRot>-2.5F||playerModel.rightArm.xRot>-2.5F)
                    throw new AssertionError("Standalone pilot rider pose missing");
                mc.player.stopRiding();
                var cannon=new cn.laowu.mod.entity.EngineeringCannon(LaoWuMod.ENGINEERING_CANNON.get(),mc.level);
                cannon.setPos(mc.player.position());
                mc.getEntityRenderDispatcher().getRenderer(cannon).render(cannon,0,0,
                        new com.mojang.blaze3d.vertex.PoseStack(),mc.renderBuffers().bufferSource(),
                        net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
                mc.renderBuffers().bufferSource().endBatch();
                cat.setId(-99821); cat.setPos(mc.player.position());
                mc.level.putNonPlayerEntity(cat.getId(),cat);
                var model=mc.getItemRenderer().getModel(new ItemStack(LaoWuMod.CAT_PANCAKE.get()),mc.level,null,0);
                if(model==mc.getModelManager().getMissingModel())throw new AssertionError("Core item model missing");
                mc.setScreen(new CatProfileScreen(new CatProfileMenu(91,mc.player.getInventory(),cat),mc.player.getInventory(),Component.literal("Standalone")));
            } else if (frames < 60 && frames % 3 == 0) {
                var outfits=CatOutfitType.values();
                var outfit=outfits[(frames/3-1)%outfits.length];
                previewCat.getPersistentData().putString(CatClothesData.OUTFIT_TAG,outfit.id());
            } else if (frames>=80) {
                if(!(mc.screen instanceof CatProfileScreen))throw new AssertionError("Profile screen not retained");
                net.minecraft.client.Screenshot.grab(mc.gameDirectory,mc.getMainRenderTarget(),message->{});
                System.out.println("PASS: STANDALONE CAT CLIENT + PROFILE + ITEM MODEL");
                mc.stop();
            }
        } catch(Throwable error) { error.printStackTrace();System.err.println("FAIL: STANDALONE CAT CLIENT");mc.stop(); }
    }
}
