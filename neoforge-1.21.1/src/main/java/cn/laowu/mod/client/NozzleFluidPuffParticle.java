package cn.laowu.mod.client;

import cn.laowu.mod.particle.NozzleFluidPuffData;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ExplodeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Vanilla nozzle flower/POOF animation tinted by the dispersed fluid. */
public final class NozzleFluidPuffParticle extends ExplodeParticle {
    private static final Map<ResourceLocation, Integer> AVERAGE_TEXTURE_COLOURS =
            new ConcurrentHashMap<>();

    private NozzleFluidPuffParticle(ClientLevel level, double x, double y, double z,
                                    double velocityX, double velocityY, double velocityZ,
                                    FluidStack fluid, SpriteSet sprites) {
        super(level, x, y, z, velocityX, velocityY, velocityZ, sprites);
        int colour = resolveDisplayedFluidColour(fluid);
        float red = FastColor.ARGB32.red(colour) / 255.0F;
        float green = FastColor.ARGB32.green(colour) / 255.0F;
        float blue = FastColor.ARGB32.blue(colour) / 255.0F;
        float alpha = FastColor.ARGB32.alpha(colour) / 255.0F;
        setColor(red, green, blue);
        setAlpha(alpha == 0.0F ? 1.0F : alpha);
        scale(0.8F);
    }

    /**
     * NeoForge's tint is only a multiplier. Lava, honey and many add-on fluids
     * return white because their actual colour lives in the still texture.
     * Sample that texture and multiply it by the stack tint, matching the
     * colour seen on Create's real fluid particle rather than producing a
     * white flower sprite.
     */
    private static int resolveDisplayedFluidColour(FluidStack fluid) {
        IClientFluidTypeExtensions extensions =
                IClientFluidTypeExtensions.of(fluid.getFluid());
        int tint = extensions.getTintColor(fluid);
        ResourceLocation texture = extensions.getStillTexture(fluid);
        int textureColour = texture == null ? 0xFFFFFFFF
                : AVERAGE_TEXTURE_COLOURS.computeIfAbsent(
                        texture, NozzleFluidPuffParticle::sampleTextureColour);

        int tintAlpha = FastColor.ARGB32.alpha(tint);
        if (tintAlpha == 0) tintAlpha = 255;
        return FastColor.ARGB32.color(
                tintAlpha,
                FastColor.ARGB32.red(textureColour) * FastColor.ARGB32.red(tint) / 255,
                FastColor.ARGB32.green(textureColour) * FastColor.ARGB32.green(tint) / 255,
                FastColor.ARGB32.blue(textureColour) * FastColor.ARGB32.blue(tint) / 255);
    }

    private static int sampleTextureColour(ResourceLocation texture) {
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation()))
            return 0xFFFFFFFF;

        NativeImage image = sprite.contents().getOriginalImage();
        long red = 0L;
        long green = 0L;
        long blue = 0L;
        long weight = 0L;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixelRGBA(x, y);
                int alpha = FastColor.ABGR32.alpha(pixel);
                if (alpha < 16) continue;
                red += (long) FastColor.ABGR32.red(pixel) * alpha;
                green += (long) FastColor.ABGR32.green(pixel) * alpha;
                blue += (long) FastColor.ABGR32.blue(pixel) * alpha;
                weight += alpha;
            }
        }
        if (weight == 0L) return 0xFFFFFFFF;
        return FastColor.ARGB32.color(255,
                (int) (red / weight),
                (int) (green / weight),
                (int) (blue / weight));
    }

    public static void clearColourCache() {
        AVERAGE_TEXTURE_COLOURS.clear();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<NozzleFluidPuffData> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(NozzleFluidPuffData data, ClientLevel level,
                                       double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            return new NozzleFluidPuffParticle(level, x, y, z,
                    velocityX, velocityY, velocityZ, data.fluid(), sprites);
        }
    }
}
