package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatTraitRegistry;
import cn.laowu.mod.item.CatTraitTokenItem;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Original bottle pixels extruded like a generated item; only liquid changes colour. */
public final class CatTraitTokenItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation TEXTURE = LaoWuMod.id("textures/item/cat_trait_token.png");
    private static List<Pixel> pixels;
    private static float whiteU;
    private static float whiteV;

    public CatTraitTokenItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    /** Called by the client resource reload listener. Never retains a NativeImage. */
    public static void clearCache() { pixels = null; }

    @Override
    public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager manager) {
        clearCache();
    }

    private static List<Pixel> pixels() {
        if (pixels != null) return pixels;
        List<Pixel> result = new ArrayList<>();
        try (var input = Minecraft.getInstance().getResourceManager().open(TEXTURE);
             NativeImage image = NativeImage.read(input)) {
            boolean whiteFound = false;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int abgr = image.getPixelRGBA(x, y);
                    if (abgr == -1) {
                        whiteU = (x + 0.5F) / image.getWidth();
                        whiteV = (y + 0.5F) / image.getHeight();
                        whiteFound = true;
                    }
                    if ((abgr >>> 24) == 0) continue;
                    int argb = (abgr & 0xFF00FF00) | ((abgr & 255) << 16) | ((abgr >>> 16) & 255);
                    result.add(new Pixel(x / (float) image.getWidth(), 1 - (y + 1F) / image.getHeight(),
                            (x + 1F) / image.getWidth(), 1 - y / (float) image.getHeight(), argb));
                }
            }
            // The supplied original has two pure-white glass highlights. Sampling
            // their centres gives a neutral texel, without rewriting any bitmap.
            if (!whiteFound) throw new IOException("Trait bottle needs an opaque white highlight texel");
        } catch (IOException exception) {
            com.mojang.logging.LogUtils.getLogger().warn("Cannot load trait bottle pixels", exception);
            result.clear();
        }
        pixels = List.copyOf(result);
        return pixels;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        var id = CatTraitTokenItem.traitId(stack);
        int frame = id == null ? 2 : CatTraitRegistry.resolve(id, true).rarity().frameIndex();
        List<Pixel> sprite = pixels();
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        for (Pixel p : sprite) {
            int colour = CatTraitTokenColours.recolour(p.argb(), frame);
            float a = 7.5F / 16, b = 8.5F / 16;
            quad(consumer, pose, colour, light, overlay, 0, 0, 1,
                    p.x0(), p.y0(), b, p.x1(), p.y0(), b, p.x1(), p.y1(), b, p.x0(), p.y1(), b);
            quad(consumer, pose, colour, light, overlay, 0, 0, -1,
                    p.x1(), p.y0(), a, p.x0(), p.y0(), a, p.x0(), p.y1(), a, p.x1(), p.y1(), a);
            quad(consumer, pose, colour, light, overlay, -1, 0, 0,
                    p.x0(), p.y0(), a, p.x0(), p.y0(), b, p.x0(), p.y1(), b, p.x0(), p.y1(), a);
            quad(consumer, pose, colour, light, overlay, 1, 0, 0,
                    p.x1(), p.y0(), b, p.x1(), p.y0(), a, p.x1(), p.y1(), a, p.x1(), p.y1(), b);
            quad(consumer, pose, colour, light, overlay, 0, 1, 0,
                    p.x0(), p.y1(), b, p.x1(), p.y1(), b, p.x1(), p.y1(), a, p.x0(), p.y1(), a);
            quad(consumer, pose, colour, light, overlay, 0, -1, 0,
                    p.x0(), p.y0(), a, p.x1(), p.y0(), a, p.x1(), p.y0(), b, p.x0(), p.y0(), b);
        }
    }

    private static void quad(VertexConsumer consumer, PoseStack pose, int colour, int light, int overlay,
                             float nx, float ny, float nz, float... xyz) {
        for (int i = 0; i < 12; i += 3) {
            consumer.addVertex(pose.last().pose(), xyz[i], xyz[i + 1], xyz[i + 2])
                    .setColor((colour >>> 16) & 255, (colour >>> 8) & 255, colour & 255, colour >>> 24)
                    .setUv(whiteU, whiteV).setOverlay(overlay).setLight(light)
                    .setNormal(pose.last(), nx, ny, nz);
        }
    }

    private record Pixel(float x0, float y0, float x1, float y1, int argb) {}
}
