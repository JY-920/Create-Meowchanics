package cn.laowu.mod.client;

import cn.laowu.mod.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Client-only volume control for the multi-cat hissing audio session. */
public final class HissingVolumeScreen extends Screen {
    private boolean changed;

    public HissingVolumeScreen() {
        super(Component.translatable("screen.laowu.hissing_volume"));
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(240, width - 32);
        int x = (width - panelWidth) / 2;
        int y = height / 2 - 10;
        addRenderableWidget(new VolumeSlider(x, y, panelWidth, 20));
        addRenderableWidget(Button.builder(Component.translatable("screen.laowu.world.title"),
                button -> minecraft.setScreen(new WorldSettingsScreen(this)))
                .bounds(x, y + 80, panelWidth, 20).build());
        addRenderableWidget(Button.builder(spawnLabel(), button -> {
            ClientConfig.NEARBY_CAT_SPAWNING.set(!ClientConfig.NEARBY_CAT_SPAWNING.get());
            ClientConfig.SPEC.save();
            button.setMessage(spawnLabel());
        }).bounds(x, y + 56, panelWidth, 20)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("screen.laowu.nearby_cat_spawning.description"))).build());
        addRenderableWidget(Button.builder(Component.translatable(
                                "screen.laowu.hissing_volume.reset"), button -> resetVolume())
                .bounds(x, y + 28, (panelWidth - 6) / 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                        button -> onClose())
                .bounds(x + (panelWidth - 6) / 2 + 6, y + 28,
                        (panelWidth - 6) / 2, 20).build());
    }

    private Component spawnLabel() {
        return Component.translatable("screen.laowu.nearby_cat_spawning",
                Component.translatable(ClientConfig.NEARBY_CAT_SPAWNING.get()
                        ? "options.on" : "options.off"));
    }

    private void resetVolume() {
        ClientConfig.HISSING_PAIR_VOLUME.set(1.0D);
        changed = true;
        clearWidgets();
        init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Flush previous UI batches before Modern UI processes the framebuffer.
        graphics.flush();
        renderBackground(graphics, mouseX, mouseY, partialTick);
        ConfigScreenRendering.beginForeground(graphics);
        try {
            graphics.drawCenteredString(font, title, width / 2, height / 2 - 48, 0xFFFFFFFF);
            graphics.drawCenteredString(font,
                    Component.translatable("screen.laowu.hissing_volume.description"),
                    width / 2, height / 2 - 31, 0xFFA0A0A0);
            // Screen.render in 1.21 draws the background again. Render only widgets
            // here so post-processing cannot blur the labels that were just drawn.
            for (var widget : renderables) widget.render(graphics, mouseX, mouseY, partialTick);
        } finally {
            ConfigScreenRendering.endForeground(graphics);
        }
    }

    @Override
    public void onClose() {
        if (changed) ClientConfig.SPEC.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private final class VolumeSlider extends AbstractSliderButton {
        private VolumeSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(),
                    Mth.clamp(ClientConfig.HISSING_PAIR_VOLUME.get() / 2.0D,
                            0.0D, 1.0D));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("screen.laowu.hissing_volume.value",
                    Math.round(value * 200.0D)));
        }

        @Override
        protected void applyValue() {
            // Five-percent steps are precise enough to configure audibly while
            // keeping the displayed integer and saved value in agreement.
            double configured = Math.round(value * 40.0D) / 20.0D;
            value = configured / 2.0D;
            ClientConfig.HISSING_PAIR_VOLUME.set(configured);
            changed = true;
            updateMessage();
        }
    }
}
