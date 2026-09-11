package cn.laowu.mod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

/** Rendering boundary between optional background post-processing and readable controls. */
final class ConfigScreenRendering {
    static void beginForeground(GuiGraphics graphics) {
        graphics.flush();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 10);
    }

    static void endForeground(GuiGraphics graphics) {
        try {
            // Submit text while the foreground pose/color/blend state is still active.
            graphics.flush();
        } finally {
            graphics.pose().popPose();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
        }
    }

    private ConfigScreenRendering() {}
}
