package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/** Tiny generated masks shared by vanilla, hissing and pancake cat geometry. */
public final class CatAppearanceTextures {
    private static final ResourceLocation WHITE_EYES = LaoWuMod.id(
            "dynamic/cat_appearance/white_eyes");
    private static final ResourceLocation TEARS = LaoWuMod.id(
            "dynamic/cat_appearance/tears");
    private static final ResourceLocation BOOTS = LaoWuMod.id(
            "dynamic/cat_appearance/boots");
    private static final Set<ResourceLocation> REGISTERED = new HashSet<>();

    public static synchronized ResourceLocation whiteEyes() {
        return register(WHITE_EYES, image -> {
            int white = FastColor.ABGR32.color(255, 255, 255, 255);
            // The vanilla cat front face occupies U=5..9; both eyes are two
            // pixels wide on V=6. Every runtime cat model keeps this exact UV.
            for (int x : new int[] {5, 6, 8, 9}) {
                image.setPixelRGBA(x, 6, white);
            }
        });
    }

    /** Tear pixels used when a Blockbench pose cannot use the vanilla head bone. */
    public static synchronized ResourceLocation tears() {
        return register(TEARS, image -> {
            int blue = FastColor.ABGR32.color(220, 255, 178, 66);
            // Use the outside edge of each eye so the projected streak never
            // runs through the protruding central nose cube.
            for (int x : new int[] {5, 9}) {
                image.setPixelRGBA(x, 7, blue);
                image.setPixelRGBA(x, 8, blue);
            }
        });
    }

    /** Brown paw regions shared by the hissing and flattened pancake UVs. */
    public static synchronized ResourceLocation boots() {
        return register(BOOTS, image -> {
            int brown = FastColor.ABGR32.color(255, 36, 70, 126);
            // Hind-leg side faces and sole.
            fill(image, 8, 19, 16, 21, brown);
            fill(image, 12, 13, 14, 15, brown);
            // Front-leg side faces and sole.
            fill(image, 40, 10, 48, 12, brown);
            fill(image, 44, 0, 46, 2, brown);
        });
    }

    private static ResourceLocation register(ResourceLocation location,
                                             Consumer<NativeImage> painter) {
        if (REGISTERED.contains(location)) return location;
        NativeImage image = new NativeImage(64, 32, true);
        painter.accept(image);
        Minecraft.getInstance().getTextureManager().register(
                location, new DynamicTexture(image));
        REGISTERED.add(location);
        return location;
    }

    private static void fill(NativeImage image, int minX, int minY,
                             int maxX, int maxY, int colour) {
        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                image.setPixelRGBA(x, y, colour);
            }
        }
    }

    public static synchronized void clear() {
        REGISTERED.forEach(location ->
                Minecraft.getInstance().getTextureManager().release(location));
        REGISTERED.clear();
    }

    private CatAppearanceTextures() {}
}

