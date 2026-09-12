package cn.laowu.mod.client;

import cn.laowu.mod.ClientConfig;
import cn.laowu.mod.item.CatLaserPointerItem;
import cn.laowu.mod.network.ModNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.List;

/** Three independent settings; this client-only screen never opens a container or pauses a world. */
public final class CatLaserWheelScreen extends Screen {
    private static boolean openingPressHeld;
    private int radius, centerX, centerY;
    private List<CatLaserWheelLayout.Span> spans = List.of();
    private boolean requested, ready, aggressive;

    private CatLaserWheelScreen() { super(Component.translatable("gui.laowu.laser_wheel.title")); }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (openingPressHeld || mc.screen != null || mc.player == null) return;
        openingPressHeld = true;
        mc.setScreen(new CatLaserWheelScreen());
    }

    /** Poll the physical binding: opening a Screen releases KeyMapping state, not the held mouse button. */
    public static void inputTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { openingPressHeld = false; return; }
        InputConstants.Key key = mc.options.keyUse.getKey();
        boolean down = key.getType() == InputConstants.Type.MOUSE
                ? GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), key.getValue()) == GLFW.GLFW_PRESS
                : key.getType() == InputConstants.Type.KEYSYM && key.getValue() >= 0
                    && InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getValue());
        if (!down) openingPressHeld = false;
    }

    public static void receive(boolean aggressive) {
        if (Minecraft.getInstance().screen instanceof CatLaserWheelScreen screen) {
            screen.aggressive = aggressive;
            screen.ready = true;
        }
    }

    @Override protected void init() {
        centerX = width / 2;
        centerY = height / 2;
        radius = Math.max(24, Math.min(96, Math.min(width / 2 - 8, height / 2 - 25)));
        spans = CatLaserWheelLayout.spans(radius);
        if (!requested) {
            requested = true;
            ModNetwork.requestLaserSettings(-1);
        }
    }

    @Override public void tick() {
        if (minecraft == null || minecraft.player == null || !minecraft.player.isAlive()
                || !(minecraft.player.getMainHandItem().getItem() instanceof CatLaserPointerItem
                || minecraft.player.getOffhandItem().getItem() instanceof CatLaserPointerItem)) onClose();
    }

    private void choose(int section) {
        if (section == 0) {
            ClientConfig.CAT_HEALTH_BARS.set(!ClientConfig.CAT_HEALTH_BARS.get());
            ClientConfig.SPEC.save();
        } else if (section == 1) {
            CatTeamPreview.toggle();
        } else if (section == 2 && ready) {
            ready = false; // Display the server's reply, never treat a local checkbox as authority.
            ModNetwork.requestLaserSettings(aggressive ? 0 : 1);
        }
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 1) { onClose(); return true; }
        if (button != 0) return super.mouseClicked(x, y, button);
        if (Math.hypot(x - centerX, y - centerY) < radius * .31) { onClose(); return true; }
        int section = CatLaserWheelLayout.sectionAt(x - centerX, y - centerY, radius);
        if (section >= 0) { choose(section); return true; }
        return false;
    }

    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_3) {
            choose(key - GLFW.GLFW_KEY_1);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        // A light veil instead of background blur, so the cats remain visible while toggling.
        graphics.fill(0, 0, width, height, 0x55000000);
        ConfigScreenRendering.beginForeground(graphics);
        try {
            int hovered = CatLaserWheelLayout.sectionAt(mouseX - centerX, mouseY - centerY, radius);
            for (var span : spans) {
                int color = span.section() == hovered ? 0xFFE6BA75 : 0xF09B7050;
                graphics.fill(centerX + span.x0(), centerY + span.y(),
                        centerX + span.x1(), centerY + span.y() + 1, color);
            }
            graphics.drawCenteredString(font, title, centerX, centerY - radius - 18, 0xFFF3D9);
            float scale = Math.min(1, radius / 92.0F);
            for (int section = 0; section < 3; section++) {
                double angle = CatLaserWheelLayout.labelAngle(section);
                int x = centerX + (int) Math.round(Math.cos(angle) * radius * .66);
                int y = centerY + (int) Math.round(Math.sin(angle) * radius * .66);
                boolean enabled = section == 0 ? ClientConfig.CAT_HEALTH_BARS.get()
                        : section == 1 ? CatTeamPreview.enabled() : aggressive;
                String state = section == 2 ? !ready ? "loading" : aggressive ? "aggressive" : "defensive"
                        : enabled ? "on" : "off";
                graphics.pose().pushPose();
                graphics.pose().translate(x, y, 1);
                graphics.pose().scale(scale, scale, 1);
                graphics.drawCenteredString(font, Component.translatable("gui.laowu.laser_wheel.section." + section),
                        0, -12, 0xFFF3D9);
                graphics.drawCenteredString(font, Component.translatable("gui.laowu.laser_wheel." + state),
                        0, 2, section == 2 && !ready ? 0xD0C0AA : enabled ? 0xCCFFAE : 0xFFFFFF);
                graphics.pose().popPose();
            }
            graphics.drawCenteredString(font, Component.translatable("gui.laowu.laser_wheel.close"),
                    centerX, centerY - 4, 0xFFF3D9);
            graphics.drawCenteredString(font, Component.translatable("gui.laowu.laser_wheel.help"),
                    centerX, centerY + radius + 9, 0xE8D7BF);
        } finally {
            ConfigScreenRendering.endForeground(graphics);
        }
    }

    @Override public boolean isPauseScreen() { return false; }
}
