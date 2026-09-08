package cn.laowu.mod.client;

import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HeldItemTransformScreen extends Screen {
    private static final int ROTATION_X = 0;
    private static final int ROTATION_Y = 1;
    private static final int ROTATION_Z = 2;
    private static final int OFFSET_X = 3;
    private static final int OFFSET_Y = 4;
    private static final int OFFSET_Z = 5;
    private static final int SCALE = 6;

    private final ItemStack previewStack;
    private final double[] values = new double[7];
    private final List<TransformSlider> sliders = new ArrayList<>();
    private HeldItemTransformState.Target target = HeldItemTransformState.Target.GUI;
    private Button modeButton;
    private boolean saved;

    public HeldItemTransformScreen(ItemStack previewStack) {
        super(Component.translatable("screen.laowu.held_item_transform", previewStack.getHoverName()));
        this.previewStack = previewStack;
        load(HeldItemTransformState.configured(previewStack, target));
        updatePreview();
    }

    @Override
    protected void init() {
        sliders.clear();
        int panelX = Math.max(width / 2 + 8, 168);
        int sliderWidth = Math.max(120, Math.min(220, width - panelX - 16));
        modeButton = addRenderableWidget(Button.builder(modeLabel(), button -> switchTarget())
                .bounds(panelX, 30, sliderWidth, 20).build());
        int y = 56;
        addSlider(panelX, y, sliderWidth, ROTATION_X, "screen.laowu.held_item_transform.rotation_x", -180, 180, 1);
        addSlider(panelX, y += 22, sliderWidth, ROTATION_Y, "screen.laowu.held_item_transform.rotation_y", -180, 180, 1);
        addSlider(panelX, y += 22, sliderWidth, ROTATION_Z, "screen.laowu.held_item_transform.rotation_z", -180, 180, 1);
        addSlider(panelX, y += 22, sliderWidth, OFFSET_X, "screen.laowu.held_item_transform.offset_x", -2.0, 2.0, 0.01);
        addSlider(panelX, y += 22, sliderWidth, OFFSET_Y, "screen.laowu.held_item_transform.offset_y", -2.0, 2.0, 0.01);
        addSlider(panelX, y += 22, sliderWidth, OFFSET_Z, "screen.laowu.held_item_transform.offset_z", -2.0, 2.0, 0.01);
        addSlider(panelX, y += 22, sliderWidth, SCALE, "screen.laowu.held_item_transform.scale", 0.1, 3.0, 0.01);

        int buttonY = Math.max(y + 27, height - 29);
        int buttonWidth = Math.max(55, (sliderWidth - 8) / 3);
        addRenderableWidget(Button.builder(Component.translatable("screen.laowu.held_item_transform.reset"), b -> reset())
                .bounds(panelX, buttonY, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.laowu.held_item_transform.save"), b -> saveAndClose())
                .bounds(panelX + buttonWidth + 4, buttonY, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(panelX + (buttonWidth + 4) * 2, buttonY, buttonWidth, 20).build());
    }

    private void addSlider(int x, int y, int width, int index, String label, double min, double max, double step) {
        TransformSlider slider = new TransformSlider(x, y, width, index, label, min, max, step);
        sliders.add(slider);
        addRenderableWidget(slider);
    }

    private void reset() {
        load(HeldItemTransformState.defaults(previewStack, target));
        sliders.forEach(TransformSlider::syncFromValues);
        updatePreview();
    }

    private Component modeLabel() {
        return Component.translatable(switch (target) {
            case GUI -> "screen.laowu.held_item_transform.mode_gui";
            case FIRST_PERSON -> "screen.laowu.held_item_transform.mode_first_person";
            case THIRD_PERSON -> "screen.laowu.held_item_transform.mode_third_person";
        });
    }

    private void switchTarget() {
        HeldItemTransformState.cancelPreview();
        target = switch (target) {
            case GUI -> HeldItemTransformState.Target.FIRST_PERSON;
            case FIRST_PERSON -> HeldItemTransformState.Target.THIRD_PERSON;
            case THIRD_PERSON -> HeldItemTransformState.Target.GUI;
        };
        load(HeldItemTransformState.configured(previewStack, target));
        sliders.forEach(TransformSlider::syncFromValues);
        modeButton.setMessage(modeLabel());
        updatePreview();
    }

    private void saveAndClose() {
        HeldItemTransformState.save(previewStack, target, currentValues());
        saved = true;
        if (minecraft != null) minecraft.setScreen(null);
    }

    private void load(HeldItemTransformState.Values transform) {
        values[ROTATION_X] = transform.rotationX();
        values[ROTATION_Y] = transform.rotationY();
        values[ROTATION_Z] = transform.rotationZ();
        values[OFFSET_X] = transform.offsetX();
        values[OFFSET_Y] = transform.offsetY();
        values[OFFSET_Z] = transform.offsetZ();
        values[SCALE] = transform.scale();
    }

    private HeldItemTransformState.Values currentValues() {
        return new HeldItemTransformState.Values(
                values[ROTATION_X], values[ROTATION_Y], values[ROTATION_Z],
                values[OFFSET_X], values[OFFSET_Y], values[OFFSET_Z], values[SCALE]);
    }

    private void updatePreview() {
        HeldItemTransformState.preview(previewStack, target, currentValues());
    }

    @Override
    public void onClose() {
        HeldItemTransformState.cancelPreview();
        super.onClose();
    }

    @Override
    public void removed() {
        if (!saved) HeldItemTransformState.cancelPreview();
        super.removed();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int previewLeft = 16;
        int previewRight = Math.max(previewLeft + 120, width / 2 - 8);
        int previewTop = 31;
        int previewBottom = Math.max(previewTop + 150, height - 16);
        graphics.fill(previewLeft, previewTop, previewRight, previewBottom, 0xA0202020);

        if (minecraft != null) {
            graphics.pose().pushPose();
            graphics.pose().translate((previewLeft + previewRight) / 2.0F,
                    (previewTop + previewBottom) / 2.0F + 18.0F, 100.0F);
            graphics.pose().scale(64.0F, -64.0F, 64.0F);
            Lighting.setupFor3DItems();
            minecraft.getItemRenderer().renderStatic(previewStack,
                    switch (target) {
                        case GUI -> ItemDisplayContext.GUI;
                        case FIRST_PERSON -> ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
                        case THIRD_PERSON -> ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
                    },
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    graphics.pose(), graphics.bufferSource(), minecraft.level, 0);
            graphics.flush();
            Lighting.setupForFlatItems();
            graphics.pose().popPose();
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        graphics.drawCenteredString(font,
                Component.translatable(switch (target) {
                    case GUI -> "screen.laowu.held_item_transform.preview_gui";
                    case FIRST_PERSON -> "screen.laowu.held_item_transform.preview_first_person";
                    case THIRD_PERSON -> "screen.laowu.held_item_transform.preview_third_person";
                }),
                (previewLeft + previewRight) / 2, previewTop + 6, 0xC8C8C8);
    }

    private static double normalize(double value, double min, double max) {
        return Mth.clamp((value - min) / (max - min), 0.0D, 1.0D);
    }

    private final class TransformSlider extends AbstractSliderButton {
        private final int index;
        private final String label;
        private final double min;
        private final double max;
        private final double step;

        private TransformSlider(int x, int y, int width, int index, String label,
                                double min, double max, double step) {
            super(x, y, width, 20, Component.empty(), normalize(values[index], min, max));
            this.index = index;
            this.label = label;
            this.min = min;
            this.max = max;
            this.step = step;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            String formatted = step >= 1.0D
                    ? String.format(Locale.ROOT, "%.0f°", values[index])
                    : String.format(Locale.ROOT, "%.2f", values[index]);
            setMessage(Component.translatable(label).append(": " + formatted));
        }

        @Override
        protected void applyValue() {
            double raw = min + value * (max - min);
            values[index] = Math.round(raw / step) * step;
            updateMessage();
            updatePreview();
        }

        private void syncFromValues() {
            value = normalize(values[index], min, max);
            updateMessage();
        }
    }
}
