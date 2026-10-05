package cn.laowu.mod.client;
import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.create.CatMachineBlocks;
import com.simibubi.create.foundation.block.connected.CTModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="laowu", value=Dist.CLIENT, bus=EventBusSubscriber.Bus.MOD)
public final class CatMachinesClientProbe {
    static boolean ready, started, opened;
    static int probeTicks;
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            org.lwjgl.glfw.GLFW.glfwHideWindow(Minecraft.getInstance().getWindow().getWindow());
            ready=true;
        });
    }
    @EventBusSubscriber(modid="laowu", value=Dist.CLIENT, bus=EventBusSubscriber.Bus.GAME)
    public static final class Ticks {
        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
            var mc=Minecraft.getInstance();
            if(Boolean.getBoolean("laowu.creature_transmitter_probe")) {
                if(ready&&!started&&mc.getOverlay()==null){
                    probeTicks++;
                    if(probeTicks==200&&mc.level==null&&mc.getSingleplayerServer()==null&&!opened){opened=true;mc.createWorldOpenFlows().openWorld("Sixway",mc::stop);}
                    if(probeTicks>1800){System.err.println("FAIL: CREATURE TRANSMITTER WORLD timeout");mc.stop();return;}
                }
                if(!ready||mc.getOverlay()!=null||mc.level==null||mc.player==null)return;
                if(!started&&mc.screen!=null)return;
                started=true;
                try{if(CreatureTransmitterClientProbe.tick(mc))mc.stop();}
                catch(Throwable failure){failure.printStackTrace();System.err.println("FAIL: CREATURE TRANSMITTER CLIENT");mc.stop();}
                return;
            }
            if(Boolean.getBoolean("laowu.cat_editor_probe")) {
                if(ready&&!started&&mc.getOverlay()==null) {
                    probeTicks++;
                    if(probeTicks==200&&mc.level==null&&mc.getSingleplayerServer()==null&&!opened){opened=true;mc.createWorldOpenFlows().openWorld("Sixway",mc::stop);}
                    if(probeTicks>1800){System.err.println("FAIL: CAT EDITOR WORLD load timeout");mc.stop();return;}
                }
                if(!ready||started||mc.getOverlay()!=null||mc.level==null||mc.player==null||mc.screen!=null)return;
                started=true;
                try { CatEditorClientProbe.verify(mc); }
                catch(Throwable failure){failure.printStackTrace();System.err.println("FAIL: CAT EDITOR CLIENT");}
                finally{mc.stop();}
                return;
            }
            if(Boolean.getBoolean("laowu.deployment_probe")) {
                if(ready&&!started&&mc.getOverlay()==null) {
                    probeTicks++;
                    if(probeTicks==200&&mc.level==null&&mc.getSingleplayerServer()==null&&!opened){opened=true;mc.createWorldOpenFlows().openWorld("Sixway",mc::stop);}
                    if(probeTicks>1800){System.err.println("FAIL: CAT DEPLOYMENT WORLD load timeout");mc.stop();return;}
                }
                if(!ready||started||mc.getOverlay()!=null||mc.level==null||mc.player==null||mc.screen!=null)return;
                started=true;
                try { CatDeploymentClientProbe.verify(mc); }
                catch(Throwable failure){failure.printStackTrace();System.err.println("FAIL: CAT DEPLOYMENT CLIENT");}
                finally{mc.stop();}
                return;
            }
            if(Boolean.getBoolean("laowu.auto_laser_probe")||Boolean.getBoolean("laowu.auto_laser_world_probe")) {
                boolean world=Boolean.getBoolean("laowu.auto_laser_world_probe");
                if(world&&started) {
                    try { if(CreatureFilterNetworkClientProbe.tick(mc))mc.stop(); }
                    catch(Throwable failure){failure.printStackTrace();System.err.println("FAIL: CREATURE FILTER LIVE NETWORK");mc.stop();}
                    return;
                }
                if(world&&ready&&!started&&mc.getOverlay()==null) {
                    probeTicks++;
                    if(probeTicks==200&&mc.level==null&&mc.getSingleplayerServer()==null&&!opened){opened=true;mc.createWorldOpenFlows().openWorld("Sixway",mc::stop);}
                    if(probeTicks>1800){System.err.println("FAIL: AUTO LASER WORLD load timeout");mc.stop();return;}
                }
                if(!ready||started||mc.getOverlay()!=null||(world?(mc.level==null||mc.player==null||mc.screen!=null):mc.screen==null))return;
                started=true;
                try {CatAutoLaserClientProbe.verify(mc);}
                catch(Throwable failure){failure.printStackTrace();System.err.println("FAIL: AUTO LASER CLIENT");mc.stop();}
                if(!world)mc.stop();
                return;
            }
            if(Boolean.getBoolean("laowu.sixway_probe")){
                if(ready&&mc.getOverlay()==null&&!started){
                    probeTicks++;
                    if(probeTicks%100==0)System.out.println("SIXWAY WAIT screen="+mc.screen+" level="+(mc.level!=null));
                    if(probeTicks==200&&mc.level==null&&mc.getSingleplayerServer()==null&&!opened){opened=true;mc.createWorldOpenFlows().openWorld("Sixway",mc::stop);}
                    if(probeTicks>1800){System.err.println("FAIL: sixway world load timeout");mc.stop();return;}
                }
                if(!ready||started||mc.getOverlay()!=null||mc.level==null||com.simibubi.create.compat.jei.CreateJEI.runtime==null)return;
                if(Boolean.getBoolean("laowu.machine_recipe_probe")) {
                    started=true;
                    try { CatMachineRecipeClientProbe.verify(mc); System.out.println("PASS: CAT MACHINE RECIPES CLIENT"); }
                    catch(Throwable failure) { failure.printStackTrace(); System.err.println("FAIL: CAT MACHINE RECIPES CLIENT"); }
                    finally { mc.stop(); }
                    return;
                }
                if(Boolean.getBoolean("laowu.giant_visual_probe")) {
                    if(mc.gameRenderer.getMainCamera().getEntity()==null)return;
                    started=true;
                    try { GiantCatVisualProbe.verify(mc); System.out.println("PASS: GIANT VISUAL CLIENT"); }
                    catch(Throwable failure) { failure.printStackTrace(); System.err.println("FAIL: GIANT VISUAL CLIENT"); }
                    finally { mc.stop(); }
                    return;
                }
                try{if(!GiantCatRidingClientProbe.verifyTick(mc))return;
                    if(Boolean.getBoolean("laowu.giant_riding_probe")) {
                        started=true;System.out.println("PASS: GIANT REAL RIDING + BOB CLIENT");mc.stop();return;
                    }
                    if(!CatSixWayClientProbe.cancellationSynced(mc))return;}
                catch(Throwable failure){started=true;failure.printStackTrace();System.err.println("FAIL: SIXWAY CANCEL SYNC");mc.stop();return;}
                started=true;
                try{CatSixWayClientProbe.verify(mc);System.out.println("PASS: CAT SIXWAY CLIENT + JEI");}
                catch(Throwable failure){failure.printStackTrace();System.err.println("FAIL: CAT SIXWAY CLIENT + JEI");}
                finally{mc.stop();}
                return;
            }
            if(!ready||started||mc.getOverlay()!=null||mc.screen==null)return;
            started=true;
            try { verify(mc); System.out.println("PASS: CAT MACHINES CLIENT"); }
            catch(Throwable failure){ failure.printStackTrace(); System.err.println("FAIL: CAT MACHINES CLIENT"); }
            finally { mc.stop(); }
        }
    }
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static void verify(Minecraft mc) throws Exception {
        CatMixerIconProbe.verify(mc);
        AdoptionInputDisplayVisualProbe.verify(mc);
        for(var type:java.util.List.of(CatMachineBlocks.SHAFT_BE.get(),CatMachineBlocks.COG_BE.get(),CatMachineBlocks.LARGE_COG_BE.get())) {
            check(dev.engine_room.flywheel.api.visualization.VisualizerRegistry.getVisualizer(type)!=null,
                    "Rotating parts must remain visible with Flywheel enabled: "+type);
        }
        var blocks=java.util.List.of(CatMachineBlocks.CAT_CASING.get(),CatMachineBlocks.CAT_ENCASED_SHAFT.get(),
                CatMachineBlocks.CAT_ENCASED_COGWHEEL.get(),CatMachineBlocks.CAT_ENCASED_LARGE_COGWHEEL.get(),CatMachineBlocks.HAJI_BASIN.get());
        for(int i=0;i<blocks.size();i++) for(var state:blocks.get(i).getStateDefinition().getPossibleStates()) {
            var model=mc.getBlockRenderer().getBlockModel(state);
            check(model!=mc.getModelManager().getMissingModel(),"Every block state must bake: "+state);
            if(i<4)check(model instanceof CTModel,"Casing surfaces must use real connected-texture models: "+state);
            check(!model.getParticleIcon().contents().name().equals(MissingTextureAtlasSprite.getLocation()),"No missing particle sprite: "+state);
        }
        var atlas=mc.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
        check(atlas.getSprite(LaoWuMod.id("block/cat_casing_connected")).contents().name()
                .equals(LaoWuMod.id("block/cat_casing_connected")),"Connected atlas must actually be stitched");
        CatMachinesRenderProbe.verify(mc);
        CatBeltClientProbe.verify(mc);
        CatDepotClientProbe.verify(mc);
    }
}
