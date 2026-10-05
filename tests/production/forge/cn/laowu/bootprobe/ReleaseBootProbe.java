package cn.laowu.bootprobe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Separate test-only mod: never linked into or added to the laowu runtime JAR. */
@Mod("laowu_boot_probe")
public final class ReleaseBootProbe {
    public ReleaseBootProbe(){
        System.out.println("BOOT PROBE: separate test mod loaded");
        if(net.minecraftforge.fml.loading.FMLEnvironment.dist==Dist.CLIENT) {
            net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus().addListener(Setup::client);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(Ticks::tick);
        }
    }
    public static final class Setup {
        public static void client(FMLClientSetupEvent event) {
            event.enqueueWork(()->{
                System.out.println("BOOT PROBE: client setup reached");
                org.lwjgl.glfw.GLFW.glfwHideWindow(Minecraft.getInstance().getWindow().getWindow());
            });
        }
    }
    public static final class Ticks {
        private static int titleTicks;
        private static int clientTicks;
        public static void tick(TickEvent.ClientTickEvent event) {
            if(event.phase!=TickEvent.Phase.END)return;
            var mc=Minecraft.getInstance();
            mc.options.pauseOnLostFocus=false;
            if(++clientTicks==1||clientTicks%80==0)System.out.println("BOOT PROBE: tick="+clientTicks
                +" screen="+(mc.screen==null?"null":mc.screen.getClass().getName())
                +" overlay="+(mc.getOverlay()==null?"null":mc.getOverlay().getClass().getName()));
            if(mc.getOverlay()!=null)return;
            if(mc.screen instanceof net.minecraftforge.client.gui.LoadingErrorScreen warnings) {
                try {
                    var errors=warnings.getClass().getDeclaredField("modLoadErrors");
                    errors.setAccessible(true);
                    var errorList=(java.util.List<?>)errors.get(warnings);
                    if(!errorList.isEmpty())throw new AssertionError("Actual Forge mod loading errors: "+errorList);
                    var messages=warnings.getClass().getDeclaredField("modLoadWarnings");
                    messages.setAccessible(true);
                    System.out.println("BOOT PROBE: acknowledge only non-fatal Forge warnings: "+messages.get(warnings));
                } catch(ReflectiveOperationException failure){throw new AssertionError("Inspect Forge loading state",failure);}
                mc.setScreen(new TitleScreen());return;
            }
            // A fresh isolated game directory has no first-launch accessibility preference.
            // Skip only that settings page, never a mod-loading error screen.
            if(mc.screen instanceof AccessibilityOnboardingScreen){mc.setScreen(new TitleScreen());return;}
            if(!(mc.screen instanceof TitleScreen))return;
            // Prove this is SRG production Minecraft, not a named development launch.
            try { net.minecraft.client.renderer.entity.layers.CapeLayer.class.getDeclaredMethod("m_6494_",
                com.mojang.blaze3d.vertex.PoseStack.class,net.minecraft.client.renderer.MultiBufferSource.class,
                int.class,net.minecraft.client.player.AbstractClientPlayer.class,
                float.class,float.class,float.class,float.class,float.class,float.class); }
            catch(ReflectiveOperationException failure){throw new AssertionError("Not a Forge SRG production client",failure);}
            if(++titleTicks==20){System.out.println("PASS: FORGE PRODUCTION JAR BOOT - SRG client reached title screen");mc.stop();}
        }
    }
}
