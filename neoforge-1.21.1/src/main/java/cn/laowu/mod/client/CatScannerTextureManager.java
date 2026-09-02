package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatAttributeEffects;
import cn.laowu.mod.genetics.CatAttributeProfile;
import cn.laowu.mod.genetics.CatStat;
import cn.laowu.mod.genetics.CatTraitInstance;
import cn.laowu.mod.genetics.CatTraitProfile;
import cn.laowu.mod.genetics.CatTraitRarity;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.animal.Cat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;

/**
 * Composites live cat stats into the scanner's own top-face texture.
 *
 * <p>The old implementation rendered the stats as a second piece of geometry.
 * Even a tiny depth offset became conspicuous after aggressive item-display
 * rotations. Baking the pixels into the same texture as the scanner makes the
 * screen and its contents physically inseparable in every display context.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class CatScannerTextureManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation ACTIVE_TEXTURE =
            LaoWuMod.id("textures/item/cat_scanner_active.png");
    private static final ResourceLocation INACTIVE_TEXTURE =
            LaoWuMod.id("textures/item/cat_scanner_inactive.png");
    private static final ResourceLocation ATTRIBUTE_ICONS =
            LaoWuMod.id("textures/gui/cat_attribute_icons.png");
    private static final ResourceLocation NUMBER_GLYPHS =
            LaoWuMod.id("textures/gui/cat_stat_numbers.png");
    private static final ResourceLocation TIER_ICONS =
            LaoWuMod.id("textures/gui/cat_stat_tiers.png");
    private static final ResourceLocation TRAIT_FRAMES =
            LaoWuMod.id("textures/gui/cat_trait_frames.png");
    private static final ResourceLocation LIVE_OPAQUE =
            LaoWuMod.id("dynamic/cat_scanner_screen_opaque");
    private static final ResourceLocation LIVE_TRANSLUCENT =
            LaoWuMod.id("dynamic/cat_scanner_screen_translucent");
    private static final ResourceLocation ACTIVE_OPAQUE =
            LaoWuMod.id("dynamic/cat_scanner_active_opaque");
    private static final ResourceLocation ACTIVE_TRANSLUCENT =
            LaoWuMod.id("dynamic/cat_scanner_active_translucent");
    private static final ResourceLocation INACTIVE_OPAQUE =
            LaoWuMod.id("dynamic/cat_scanner_inactive_opaque");
    private static final ResourceLocation INACTIVE_TRANSLUCENT =
            LaoWuMod.id("dynamic/cat_scanner_inactive_translucent");

    // The authored glass is 16x10 model pixels. Rotating the logical canvas
    // makes it a 120x192 portrait display at twelve texture pixels per model
    // pixel, with enough vertical room for stats followed by four trait cards.
    static final int TEXTURE_SCALE = 12;
    static final int CANVAS_WIDTH = 120;
    static final int CANVAS_HEIGHT = 192;
    private static final int STATS_X = 24;
    private static final int STATS_Y = 4;
    private static final int ROW_Y = 13;
    private static final int ROW_SPACING = 9;
    private static final int TRAIT_SOURCE_WIDTH = 72;
    private static final int TRAIT_SOURCE_HEIGHT = 27;
    static final int TRAIT_X = 31;
    static final int TRAIT_Y = 76;
    static final int TRAIT_SPACING = 23;
    static final int TRAIT_WIDTH = 58;
    static final int TRAIT_HEIGHT = 22;

    private static String lastKey;
    private static Layers liveLayers;
    private static Layers activeLayers;
    private static Layers inactiveLayers;

    public static Layers resolve(Cat cat, CatAttributeProfile profile,
                                 CatTraitProfile traits) {
        int[] current = new int[CatStat.values().length];
        int[] limits = new int[current.length];
        for (int index = 0; index < current.length; index++) {
            CatStat stat = CatStat.values()[index];
            current[index] = CatAttributeEffects.effectiveValue(cat, profile, traits, stat);
            limits[index] = profile.potential(stat);
        }
        return resolveValues(current, limits, traits);
    }

    /** Resolves an NBT-backed pancake which has no live cat entity context. */
    public static Layers resolve(CatAttributeProfile profile,
                                 CatTraitProfile traits,
                                 boolean night, boolean day) {
        int[] current = new int[CatStat.values().length];
        int[] limits = new int[current.length];
        for (int index = 0; index < current.length; index++) {
            CatStat stat = CatStat.values()[index];
            current[index] = CatAttributeEffects.effectiveValue(
                    profile, traits, stat, night, day);
            limits[index] = profile.potential(stat);
        }
        return resolveValues(current, limits, traits);
    }

    private static Layers resolveValues(int[] current, int[] limits,
                                        CatTraitProfile traits) {
        StringBuilder key = new StringBuilder(current.length * 9);
        for (int index = 0; index < current.length; index++) {
            key.append(current[index]).append('/').append(limits[index]).append(';');
        }
        for (CatTraitInstance instance : traits.traits()) {
            key.append(instance.trait().serializedName())
                    .append('@').append(instance.level()).append(';');
        }

        String valueKey = key.toString();
        if (liveLayers != null && valueKey.equals(lastKey)) return liveLayers;

        try (NativeImage composed = compose(current, limits, traits)) {
            release(liveLayers);
            liveLayers = registerSplit(composed, LIVE_OPAQUE, LIVE_TRANSLUCENT);
            lastKey = valueKey;
            return liveLayers;
        } catch (Exception exception) {
            LOGGER.error("Could not compose the cat scanner screen", exception);
            return resolveActive();
        }
    }

    public static Layers resolveInactive() {
        if (inactiveLayers != null) return inactiveLayers;
        try (NativeImage source = load(INACTIVE_TEXTURE)) {
            inactiveLayers = registerSplit(source, INACTIVE_OPAQUE, INACTIVE_TRANSLUCENT);
            return inactiveLayers;
        } catch (Exception exception) {
            LOGGER.error("Could not split the inactive cat scanner texture", exception);
            return new Layers(INACTIVE_TEXTURE, null);
        }
    }

    public static Layers resolveActive() {
        if (activeLayers != null) return activeLayers;
        try (NativeImage source = load(ACTIVE_TEXTURE)) {
            activeLayers = registerSplit(source, ACTIVE_OPAQUE, ACTIVE_TRANSLUCENT);
            return activeLayers;
        } catch (Exception exception) {
            LOGGER.error("Could not split the active cat scanner texture", exception);
            return new Layers(ACTIVE_TEXTURE, null);
        }
    }

    private static Layers registerSplit(NativeImage source,
                                        ResourceLocation opaqueLocation,
                                        ResourceLocation translucentLocation) {
        NativeImage opaque = new NativeImage(source.getWidth(), source.getHeight(), true);
        NativeImage translucent = new NativeImage(source.getWidth(), source.getHeight(), true);
        boolean opaqueOwned = false;
        boolean translucentOwned = false;
        try {
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    int colour = source.getPixelRGBA(x, y);
                    int alpha = colour >>> 24;
                    if (alpha == 255) opaque.setPixelRGBA(x, y, colour);
                    else if (alpha != 0) translucent.setPixelRGBA(x, y, colour);
                }
            }

            var textures = Minecraft.getInstance().getTextureManager();
            textures.register(opaqueLocation, new DynamicTexture(opaque));
            opaqueOwned = true;
            textures.register(translucentLocation, new DynamicTexture(translucent));
            translucentOwned = true;
            return new Layers(opaqueLocation, translucentLocation);
        } finally {
            if (!opaqueOwned) opaque.close();
            if (!translucentOwned) translucent.close();
        }
    }

    private static NativeImage compose(int[] current, int[] limits,
                                       CatTraitProfile traits) throws IOException {
        try (NativeImage base = load(ACTIVE_TEXTURE);
             NativeImage attributes = load(ATTRIBUTE_ICONS);
             NativeImage numbers = load(NUMBER_GLYPHS);
             NativeImage tiers = load(TIER_ICONS);
             NativeImage traitFrames = load(TRAIT_FRAMES)) {
            NativeImage output = new NativeImage(base.getWidth() * TEXTURE_SCALE,
                    base.getHeight() * TEXTURE_SCALE, true);
            try {
                for (int y = 0; y < output.getHeight(); y++) {
                    for (int x = 0; x < output.getWidth(); x++) {
                        output.setPixelRGBA(x, y,
                                base.getPixelRGBA(x / TEXTURE_SCALE, y / TEXTURE_SCALE));
                    }
                }

                for (int row = 0; row < CatStat.values().length; row++) {
                    int y = STATS_Y + ROW_Y + row * ROW_SPACING;
                    int now = current[row];
                    int limit = limits[row];
                    boolean nowAbnormal = now < 0 || now > 999;
                    boolean limitAbnormal = limit < 0 || limit > 999;

                    drawSprite(output, attributes, STATS_X + 5, y, 8, 8,
                            row * 8, 0);
                    drawNumber(output, numbers,
                            nowAbnormal ? "???" : Integer.toString(now),
                            STATS_X + 29, y + 1);
                    drawSprite(output, tiers, STATS_X + 32, y + 1, 6, 6,
                            tierIndex(now, nowAbnormal) * 6, 0);
                    drawNumber(output, numbers,
                            limitAbnormal ? "???" : Integer.toString(limit),
                            STATS_X + 56, y + 1);
                    drawSprite(output, tiers, STATS_X + 59, y + 1, 6, 6,
                            tierIndex(limit, limitAbnormal) * 6, 0);
                }

                int traitCount = Math.min(CatTraitProfile.MAX_TRAITS, traits.traits().size());
                for (int index = 0; index < traitCount; index++) {
                    CatTraitInstance instance = traits.traits().get(index);
                    int displayedLevel = displayedLevel(instance);
                    drawBorderPreservingSprite(output, traitFrames,
                            TRAIT_X, TRAIT_Y + index * TRAIT_SPACING,
                            TRAIT_WIDTH, TRAIT_HEIGHT,
                            (displayedLevel - 1) * TRAIT_SOURCE_WIDTH,
                            instance.trait().rarity().frameIndex() * TRAIT_SOURCE_HEIGHT,
                            TRAIT_SOURCE_WIDTH, TRAIT_SOURCE_HEIGHT);
                }
                return output;
            } catch (RuntimeException exception) {
                output.close();
                throw exception;
            }
        }
    }

    private static void drawNumber(NativeImage target, NativeImage glyphs,
                                   String text, int rightExclusive, int y) {
        int width = 7 + Math.max(0, text.length() - 1) * 4;
        int x = rightExclusive - width;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            int glyph = character == '?' ? 0 : 1 + character - '0';
            drawSprite(target, glyphs, x + index * 4, y, 7, 7,
                    glyph * 7, 0);
        }
    }

    private static void drawSprite(NativeImage target, NativeImage source,
                                   int x, int y, int width, int height,
                                   int u, int v) {
        for (int sourceY = 0; sourceY < height; sourceY++) {
            for (int sourceX = 0; sourceX < width; sourceX++) {
                int colour = source.getPixelRGBA(u + sourceX, v + sourceY);
                drawCanvasPixel(target, x + sourceX, y + sourceY, colour);
            }
        }
    }

    private static void drawBorderPreservingSprite(NativeImage target, NativeImage source,
                                                   int x, int y, int width, int height,
                                                   int u, int v,
                                                   int sourceWidth, int sourceHeight) {
        // Keep the single-pixel outline, level tab, and right-hand cap intact;
        // only the empty centre of the authored 72x27 card is compressed.
        final int left = 8;
        final int right = 5;
        final int top = 3;
        final int bottom = 3;
        for (int targetY = 0; targetY < height; targetY++) {
            int sourceY = v + scaleCoordinate(targetY, height, sourceHeight,
                    top, bottom);
            for (int targetX = 0; targetX < width; targetX++) {
                int sourceX = u + scaleCoordinate(targetX, width, sourceWidth,
                        left, right);
                drawCanvasPixel(target, x + targetX, y + targetY,
                        source.getPixelRGBA(sourceX, sourceY));
            }
        }
    }

    private static int scaleCoordinate(int coordinate, int targetSize,
                                       int sourceSize, int leading, int trailing) {
        if (coordinate < leading) return coordinate;
        if (coordinate >= targetSize - trailing) {
            return sourceSize - (targetSize - coordinate);
        }

        int targetMiddle = targetSize - leading - trailing;
        int sourceMiddle = sourceSize - leading - trailing;
        if (targetMiddle <= 1 || sourceMiddle <= 1) return leading;
        return leading + (coordinate - leading) * (sourceMiddle - 1)
                / (targetMiddle - 1);
    }

    private static void drawCanvasPixel(NativeImage target, int x, int y, int colour) {
        int alpha = colour >>> 24;
        if (alpha == 0) return;

        // Keep the panel on the originally selected top glass face, but retain
        // the corrected portrait-X mirror. The top face is V=0..10.
        int targetX = y;
        int targetY = x;
        if (targetX < 0 || targetX >= target.getWidth()
                || targetY < 0 || targetY >= target.getHeight()) return;
        target.setPixelRGBA(targetX, targetY,
                blend(target.getPixelRGBA(targetX, targetY), colour));
    }

    private static int blend(int background, int foreground) {
        int alpha = foreground >>> 24;
        if (alpha >= 255) return foreground;
        int inverse = 255 - alpha;
        int red = ((foreground & 0xff) * alpha + (background & 0xff) * inverse) / 255;
        int green = (((foreground >>> 8) & 0xff) * alpha
                + ((background >>> 8) & 0xff) * inverse) / 255;
        int blue = (((foreground >>> 16) & 0xff) * alpha
                + ((background >>> 16) & 0xff) * inverse) / 255;
        int backgroundAlpha = background >>> 24;
        int outputAlpha = alpha + backgroundAlpha * inverse / 255;
        return outputAlpha << 24 | blue << 16 | green << 8 | red;
    }

    private static NativeImage load(ResourceLocation location) throws IOException {
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        try (InputStream stream = resources.open(location)) {
            return NativeImage.read(stream);
        }
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

    static int displayedLevel(CatTraitInstance instance) {
        if (instance.trait().upgradable()) return instance.level();
        return instance.trait().rarity() == CatTraitRarity.DEFECT ? 1 : 7;
    }

    public static void clear() {
        release(liveLayers);
        release(activeLayers);
        release(inactiveLayers);
        liveLayers = null;
        activeLayers = null;
        inactiveLayers = null;
        lastKey = null;
    }

    private static void release(Layers layers) {
        if (layers == null) return;
        var textures = Minecraft.getInstance().getTextureManager();
        textures.release(layers.opaque());
        if (layers.translucent() != null) textures.release(layers.translucent());
    }

    public record Layers(ResourceLocation opaque, ResourceLocation translucent) {}

    private CatScannerTextureManager() {}
}
