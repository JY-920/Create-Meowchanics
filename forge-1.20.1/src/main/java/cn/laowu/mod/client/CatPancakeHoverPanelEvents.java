package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatTraitData;
import cn.laowu.mod.genetics.CatTraitProfile;
import cn.laowu.mod.item.CatPancakeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Adds the goggles-gated compact attribute panel beside cat-pancake tooltips. */
@Mod.EventBusSubscriber(modid = LaoWuMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class CatPancakeHoverPanelEvents {
    @SubscribeEvent
    public static void beforeTooltip(RenderTooltipEvent.Pre event) {
        if (!(event.getItemStack().getItem() instanceof CatPancakeItem)) return;
        if (!CatStatsGoggleOverlay.isWearingCatGoggles(
                Minecraft.getInstance().player)) return;

        int panelWidth = CatStatsGoggleOverlay.LIMITS_PANEL_WIDTH;
        int panelX = Mth.clamp(event.getX() - panelWidth - 6, 4,
                Math.max(4, event.getScreenWidth() - panelWidth - 4));
        int panelY = Mth.clamp(event.getY() - CatStatsGoggleOverlay.PANEL_HEIGHT / 2,
                4, Math.max(4,
                        event.getScreenHeight() - CatStatsGoggleOverlay.PANEL_HEIGHT - 4));

        var pose = event.getGraphics().pose();
        pose.pushPose();
        // Match vanilla tooltip depth so the panel is always above slot items,
        // stack counts and JEI/container decorations.
        pose.translate(0.0D, 0.0D, 400.0D);
        CatStatsGoggleOverlay.renderPanel(event.getGraphics(),
                CatAttributeData.read(event.getItemStack()).orElse(null),
                CatTraitData.read(event.getItemStack()).orElse(CatTraitProfile.EMPTY),
                true, panelX, panelY,
                cn.laowu.mod.genetics.CatTraitEffects.isNight(
                        Minecraft.getInstance().level),
                cn.laowu.mod.genetics.CatTraitEffects.isDay(
                        Minecraft.getInstance().level), null);
        pose.popPose();

        // The ordinary tooltip keeps its vanilla position on the mouse's right;
        // the stat panel occupies the opposite side.
    }

    private CatPancakeHoverPanelEvents() {}
}
