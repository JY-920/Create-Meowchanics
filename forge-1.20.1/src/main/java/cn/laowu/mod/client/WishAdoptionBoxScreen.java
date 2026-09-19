package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.WishAdoptionBoxMenu;
import cn.laowu.mod.create.WishAdoptionOffer;
import cn.laowu.mod.genetics.CatStat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import java.util.ArrayList;

/** Six fixed stat rows using the same icon atlas, order and joined pixel digits as the cat panel. */
public final class WishAdoptionBoxScreen extends AbstractContainerScreen<WishAdoptionBoxMenu> {
    public static final ResourceLocation TEXTURE = LaoWuMod.id("textures/gui/wish_adoption_box.png");
    public static final int REWARD_X = 116, REWARD_Y = 44;
    public static final int ICON_X = 30, ROW_Y = 23, ROW_SPACING = 9;
    private Button lock;
    public WishAdoptionBoxScreen(WishAdoptionBoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WishAdoptionBoxMenu.SCREEN_WIDTH;
        imageHeight = WishAdoptionBoxMenu.SCREEN_HEIGHT;
    }
    @Override protected void init() {
        super.init();
        lock = addRenderableWidget(new LockButton(leftPos + 133, topPos + 77, lockLabel(), button -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, menu.locked() ? 0 : 1);
        }));
        lock.setTooltip(Tooltip.create(Component.translatable("gui.laowu.wish_adoption.lock_hint")));
    }
    private Component lockLabel() {
        return Component.translatable(menu.locked() ? "gui.laowu.wish_adoption.locked" : "gui.laowu.wish_adoption.lock");
    }
    private final class LockButton extends Button {
        LockButton(int x, int y, Component label, OnPress action) { super(x, y, 16, 16, label, action, DEFAULT_NARRATION); }
        @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
            g.blit(TEXTURE, getX(), getY(), menu.locked() ? 197 : 148, menu.locked() ? 149 : 123, 16, 16, 256, 256);
            if (isHoveredOrFocused()) g.fill(getX()+1, getY()+1, getX()+15, getY()+14, 0x30FFFFFF);
        }
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        if (lock != null) lock.setMessage(lockLabel());
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        var offer = menu.offer();
        if (offer != null && isHovering(REWARD_X, REWARD_Y, 16, 16, mouseX, mouseY))
            g.renderTooltip(font, offer.rewardStack(), mouseX, mouseY);
        else if (isHovering(26, 6, 61, 78, mouseX, mouseY)) {
            var lines = new ArrayList<Component>();
            lines.add(Component.translatable(offer != null && offer.maximum()
                    ? "gui.laowu.wish_adoption.max_hint" : "gui.laowu.wish_adoption.now_hint"));
            if (offer != null) for (CatStat stat : CatStat.values()) {
                var condition = condition(offer, stat);
                lines.add(condition == null ? Component.translatable("gui.laowu.wish_adoption.no_requirement",
                        Component.translatable("attribute.laowu.cat." + stat.serializedName())) : conditionText(condition));
            }
            lines.add(Component.translatable("gui.laowu.wish_adoption.input_hint"));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        g.blit(TEXTURE, leftPos + 16, topPos, 31, 46, 143, 164, 256, 256);
        AllGuiTextures.PLAYER_INVENTORY.render(g, leftPos, topPos + WishAdoptionBoxMenu.PLAYER_PANEL_Y);
        var offer = menu.offer();
        if (offer != null) g.renderItem(offer.rewardStack(), leftPos + REWARD_X, topPos + REWARD_Y);
    }
    private static WishAdoptionOffer.Condition condition(WishAdoptionOffer offer, CatStat stat) {
        return offer == null ? null : offer.conditions().stream().filter(c -> c.stat() == stat).findFirst().orElse(null);
    }
    public static Component conditionText(WishAdoptionOffer.Condition condition) {
        String name = Component.translatable("attribute.laowu.cat." + condition.stat().serializedName()).getString();
        return Component.literal(condition.min() < 0 ? name + "≤" + condition.max()
                : condition.max() < 0 ? name + "≥" + condition.min()
                : condition.min() + "≤" + name + "≤" + condition.max());
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        var offer = menu.offer();
        // Use the panel's authored NOW/MAX header pixels as well.
        ResourceLocation panel = LaoWuMod.id("textures/gui/cat_stats_panel_limits.png");
        g.blit(panel, 43, 8, offer != null && offer.maximum() ? 40 : 13, 5, 19, 7, 71, 72);
        for (CatStat stat : CatStat.values()) {
            int y = ROW_Y + stat.ordinal() * ROW_SPACING;
            var c = condition(offer, stat);
            if (c == null) g.setColor(.25F, .25F, .25F, 1);
            CatStatsGoggleOverlay.renderAttributeIcon(g, stat, ICON_X, y);
            g.setColor(1, 1, 1, 1);
            if (c == null) continue; // A dim icon is the only mark for an unconstrained attribute.
            int x = ICON_X + 11;
            if (c.min() >= 0 && c.max() >= 0) {
                CatStatsGoggleOverlay.renderConnectedNumberLeft(g, Integer.toString(c.min()), x, y+1);
                int width = 7 + (Integer.toString(c.min()).length()-1) * 4;
                drawRelation(g, x+width+1, y+1, '~');
                CatStatsGoggleOverlay.renderConnectedNumberLeft(g, Integer.toString(c.max()), x+width+8, y+1);
            } else {
                drawRelation(g, x+1, y+1, c.min() < 0 ? '<' : '>');
                CatStatsGoggleOverlay.renderConnectedNumberLeft(g, Integer.toString(c.min() < 0 ? c.max() : c.min()), x+8, y+1);
            }
        }
        if (offer != null) {
            String tier = switch (offer.rewardStack().getRarity()) {
                case COMMON -> "common";
                case UNCOMMON, RARE -> "rare";
                case EPIC -> "epic";
            };
            int color = tier.equals("epic") ? 0xFF80FF : tier.equals("rare") ? 0x80C0FF : 0xFFF1DB;
            g.drawCenteredString(font, Component.translatable("gui.laowu.wish_adoption." + tier), 124, 23, color);
        }
    }
    /** Code-native 5x7 operators; digits themselves always come from the shared pixel atlas. */
    private static void drawRelation(GuiGraphics g, int x, int y, char relation) {
        String[] pixels = relation == '~' ? new String[]{"00000","00000","01101","10010","00000","00000","00000"}
                : relation == '<' ? new String[]{"00010","00100","01000","00100","00010","00000","01110"}
                : new String[]{"01000","00100","00010","00100","01000","00000","01110"};
        for (int row=0;row<7;row++) for(int col=0;col<5;col++)
            if(pixels[row].charAt(col)=='1') g.fill(x+col,y+row,x+col+1,y+row+1,0xFFFFECE6);
    }
}
