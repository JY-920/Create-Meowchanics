package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatAttributeData;
import cn.laowu.mod.genetics.CatAttributeProfile;
import cn.laowu.mod.genetics.CatStat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.jetbrains.annotations.Nullable;

/** Pixel-authored cat attribute panel shown while goggles target a cat. */
public final class CatStatsGoggleOverlay {
    public static final IGuiOverlay OVERLAY = CatStatsGoggleOverlay::render;

    private static final ResourceLocation PANEL = LaoWuMod.id("textures/gui/cat_stats_panel.png");
    private static final ResourceLocation LIMITS_PANEL =
            LaoWuMod.id("textures/gui/cat_stats_panel_limits.png");
    private static final ResourceLocation ATTRIBUTE_ICONS =
            LaoWuMod.id("textures/gui/cat_attribute_icons.png");
    private static final ResourceLocation NUMBER_GLYPHS =
            LaoWuMod.id("textures/gui/cat_stat_numbers.png");
    private static final ResourceLocation TIER_ICONS =
            LaoWuMod.id("textures/gui/cat_stat_tiers.png");

    static final int PANEL_WIDTH = 65;
    static final int LIMITS_PANEL_WIDTH = 71;
    static final int PANEL_HEIGHT = 72;
    private static final int ROW_COUNT = 6;
    private static final int ROW_SPACING = 9;
    private static final int ICON_X = 5;
    private static final int ROW_Y = 13;
    private static final int CURRENT_RIGHT = 29;
    private static final int TIER_X = 32;
    private static final int MAX_RIGHT = 56;
    private static final int MAX_TIER_X = 59;

    private static int hoverTicks;
    private static int lastEntityId = -1;

    private static void render(ForgeGui forgeGui, GuiGraphics graphics, float partialTicks,
                               int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui
                || minecraft.player == null
                || minecraft.gameMode == null
                || minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR
                || !isWearingCatGoggles(minecraft.player)) {
            reset();
            return;
        }

        Entity target = minecraft.hitResult instanceof EntityHitResult entityHit
                ? entityHit.getEntity() : findTargetedPancake(minecraft);
        CatAttributeProfile profile;
        if (target instanceof Cat cat) {
            profile = CatAttributeData.read(cat).orElse(null);
        } else if (target instanceof ItemEntity itemEntity
                && itemEntity.getItem().getItem() instanceof cn.laowu.mod.item.CatPancakeItem) {
            profile = CatAttributeData.read(itemEntity.getItem()).orElse(null);
        } else {
            reset();
            return;
        }
        if (profile == null) {
            reset();
            return;
        }

        if (lastEntityId != target.getId()) hoverTicks = 0;
        lastEntityId = target.getId();
        hoverTicks++;

        boolean revealLimits = minecraft.player.isShiftKeyDown();
        int panelWidth = revealLimits ? LIMITS_PANEL_WIDTH : PANEL_WIDTH;
        CClient config = AllConfigs.client();
        int x = screenWidth / 2 + config.overlayOffsetX.get();
        int y = screenHeight / 2 - PANEL_HEIGHT / 2 + config.overlayOffsetY.get();
        x = Mth.clamp(x, 4, Math.max(4, screenWidth - panelWidth - 4));
        y = Mth.clamp(y, 4, Math.max(4, screenHeight - PANEL_HEIGHT - 4));

        float fade = Mth.clamp((hoverTicks + partialTicks) / 24.0F, 0.0F, 1.0F);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        if (fade < 1.0F) {
            pose.translate(Math.pow(1.0F - fade, 3.0D)
                    * Math.signum(config.overlayOffsetX.get() + 0.5F) * 8.0D,
                    0.0D, 0.0D);
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, fade);
        renderPanel(graphics, profile, revealLimits, x, y);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
    }

    static void renderPanel(GuiGraphics graphics, @Nullable CatAttributeProfile profile,
                            boolean revealLimits, int x, int y) {
        int panelWidth = revealLimits ? LIMITS_PANEL_WIDTH : PANEL_WIDTH;
        graphics.blit(revealLimits ? LIMITS_PANEL : PANEL, x, y, 0, 0,
                panelWidth, PANEL_HEIGHT, panelWidth, PANEL_HEIGHT);

        CatStat[] stats = CatStat.values();
        for (int row = 0; row < ROW_COUNT; row++) {
            CatStat stat = stats[row];
            int rowY = y + ROW_Y + row * ROW_SPACING;
            int current = profile == null ? -1 : profile.current(stat);
            int limit = profile == null ? -1 : profile.potential(stat);
            boolean currentAbnormal = profile == null || isCurrentAbnormal(current, limit);
            boolean limitAbnormal = profile == null || isLimitAbnormal(current, limit);

            graphics.blit(ATTRIBUTE_ICONS, x + ICON_X, rowY,
                    attributeIconIndex(stat) * 8, 0, 8, 8, 48, 8);
            renderConnectedNumber(graphics, currentAbnormal ? "???" : Integer.toString(current),
                    x + CURRENT_RIGHT, rowY + 1);
            graphics.blit(TIER_ICONS, x + TIER_X, rowY + 1,
                    tierIndex(current, currentAbnormal) * 6, 0, 6, 6, 42, 6);
            renderConnectedNumber(graphics,
                    revealLimits && !limitAbnormal ? Integer.toString(limit) : "???",
                    x + MAX_RIGHT, rowY + 1);
            if (revealLimits) {
                graphics.blit(TIER_ICONS, x + MAX_TIER_X, rowY + 1,
                        tierIndex(limit, limitAbnormal) * 6, 0, 6, 6, 42, 6);
            }
        }
    }

    /** Draws seven-pixel glyphs at a four-pixel stride, matching the supplied joined digits. */
    private static void renderConnectedNumber(GuiGraphics graphics, String text,
                                              int rightExclusive, int y) {
        int width = 7 + Math.max(0, text.length() - 1) * 4;
        int x = rightExclusive - width;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            int glyph = character == '?' ? 0 : 1 + character - '0';
            graphics.blit(NUMBER_GLYPHS, x + index * 4, y,
                    glyph * 7, 0, 7, 7, 77, 7);
        }
    }

    private static boolean isCurrentAbnormal(int current, int limit) {
        return current < 0 || current > 999 || limit < current;
    }

    private static boolean isLimitAbnormal(int current, int limit) {
        return limit < 0 || limit > 999 || limit < current;
    }

    private static int tierIndex(int value, boolean abnormal) {
        if (abnormal) return 0;
        if (value < 20) return 1;
        if (value < 40) return 2;
        if (value < 60) return 3;
        if (value < 80) return 4;
        if (value < 100) return 5;
        return 6;
    }

    private static int attributeIconIndex(CatStat stat) {
        return switch (stat) {
            case SPEED -> 3;
            case STAMINA -> 2;
            default -> stat.ordinal();
        };
    }

    private static void reset() {
        hoverTicks = 0;
        lastEntityId = -1;
    }

    static boolean isWearingCatGoggles(@Nullable Player player) {
        return cn.laowu.mod.item.CatEngineerGogglesItem.isWornBy(player);
    }

    @Nullable
    private static ItemEntity findTargetedPancake(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) return null;
        Vec3 start = minecraft.player.getEyePosition();
        Vec3 end = start.add(minecraft.player.getViewVector(1.0F).scale(5.0D));
        double closest = minecraft.hitResult == null
                || minecraft.hitResult.getType() == HitResult.Type.MISS
                ? start.distanceToSqr(end)
                : start.distanceToSqr(minecraft.hitResult.getLocation());
        AABB search = minecraft.player.getBoundingBox()
                .expandTowards(end.subtract(start)).inflate(1.0D);
        ItemEntity result = null;
        for (ItemEntity item : minecraft.level.getEntitiesOfClass(ItemEntity.class, search,
                entity -> entity.getItem().getItem()
                        instanceof cn.laowu.mod.item.CatPancakeItem)) {
            var hit = item.getBoundingBox().inflate(0.2D).clip(start, end);
            if (hit.isEmpty()) continue;
            double distance = start.distanceToSqr(hit.get());
            if (distance < closest) {
                closest = distance;
                result = item;
            }
        }
        return result;
    }

    private CatStatsGoggleOverlay() {}
}
