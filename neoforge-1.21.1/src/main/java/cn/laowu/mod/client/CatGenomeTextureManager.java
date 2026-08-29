package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatGenome;
import cn.laowu.mod.genetics.CatGenomeData;
import cn.laowu.mod.genetics.CatRegion;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Builds and caches 64x32 cat textures assembled from semantic texture regions. */
@OnlyIn(Dist.CLIENT)
public final class CatGenomeTextureManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation REGION_MAP = LaoWuMod.id("textures/entity/cat/region_map.png");
    private static final int MAX_TEXTURES = 256;

    private static final LinkedHashMap<String, ResourceLocation> COMPOSITES =
            new LinkedHashMap<>(32, 0.75F, true);
    private static final Map<ResourceLocation, NativeImage> SOURCE_IMAGES = new HashMap<>();
    private static NativeImage regionMap;

    public static ResourceLocation resolve(Cat cat) {
        return resolve(CatGenomeData.read(cat).orElse(null), cat.getVariant().value().texture());
    }

    public static ResourceLocation resolve(ItemStack stack, ResourceLocation fallback) {
        return resolve(CatGenomeData.read(stack).orElse(null), fallback);
    }

    private static synchronized ResourceLocation resolve(CatGenome genome, ResourceLocation fallback) {
        if (genome == null) return fallback;
        if (genome.isUniform()) return textureForMaterial(genome.material(CatRegion.HEAD_PRIMARY), fallback);

        String key = genome.phenotypeKey();
        ResourceLocation cached = COMPOSITES.get(key);
        if (cached != null) return cached;

        try {
            NativeImage composed = compose(genome, fallback);
            ResourceLocation location = LaoWuMod.id("dynamic/cat/" + sha256(key));
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(composed));
            COMPOSITES.put(key, location);
            evictOldest();
            return location;
        } catch (Exception exception) {
            LOGGER.error("Could not compose cat genome texture {}", key, exception);
            return fallback;
        }
    }

    private static NativeImage compose(CatGenome genome, ResourceLocation fallback) throws IOException {
        NativeImage mask = regionMap();
        NativeImage output = new NativeImage(mask.getWidth(), mask.getHeight(), true);

        for (int y = 0; y < mask.getHeight(); y++) {
            for (int x = 0; x < mask.getWidth(); x++) {
                int alpha = Byte.toUnsignedInt(mask.getLuminanceOrAlpha(x, y));
                if (alpha == 0) continue;
                int rgb = Byte.toUnsignedInt(mask.getRedOrLuminance(x, y)) << 16
                        | Byte.toUnsignedInt(mask.getGreenOrLuminance(x, y)) << 8
                        | Byte.toUnsignedInt(mask.getBlueOrLuminance(x, y));
                CatRegion region = CatRegion.fromMapRgb(rgb);
                if (region == null) continue;

                ResourceLocation texture = textureForMaterial(genome.material(region), fallback);
                NativeImage source = sourceImage(texture);
                int sourceX = Math.min(source.getWidth() - 1, x * source.getWidth() / mask.getWidth());
                int sourceY = Math.min(source.getHeight() - 1, y * source.getHeight() / mask.getHeight());
                output.setPixelRGBA(x, y, source.getPixelRGBA(sourceX, sourceY));
            }
        }
        return output;
    }

    private static NativeImage regionMap() throws IOException {
        if (regionMap == null) regionMap = load(REGION_MAP);
        return regionMap;
    }

    private static NativeImage sourceImage(ResourceLocation texture) throws IOException {
        NativeImage cached = SOURCE_IMAGES.get(texture);
        if (cached != null) return cached;
        NativeImage loaded = load(texture);
        SOURCE_IMAGES.put(texture, loaded);
        return loaded;
    }

    private static NativeImage load(ResourceLocation location) throws IOException {
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        try (InputStream stream = resources.open(location)) {
            return NativeImage.read(stream);
        }
    }

    private static ResourceLocation textureForMaterial(ResourceLocation material,
                                                       ResourceLocation fallback) {
        CatVariant variant = BuiltInRegistries.CAT_VARIANT.get(material);
        return variant == null ? fallback : variant.texture();
    }

    private static void evictOldest() {
        while (COMPOSITES.size() > MAX_TEXTURES) {
            Iterator<Map.Entry<String, ResourceLocation>> iterator = COMPOSITES.entrySet().iterator();
            if (!iterator.hasNext()) return;
            ResourceLocation old = iterator.next().getValue();
            iterator.remove();
            Minecraft.getInstance().getTextureManager().release(old);
        }
    }

    /** Called by NeoForge's client resource reload event; releases both RAM and GPU objects. */
    public static synchronized void clear() {
        var textureManager = Minecraft.getInstance().getTextureManager();
        COMPOSITES.values().forEach(textureManager::release);
        COMPOSITES.clear();
        SOURCE_IMAGES.values().forEach(NativeImage::close);
        SOURCE_IMAGES.clear();
        if (regionMap != null) {
            regionMap.close();
            regionMap = null;
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(24);
            for (int index = 0; index < 12; index++) {
                result.append(String.format("%02x", digest[index]));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private CatGenomeTextureManager() {}
}
