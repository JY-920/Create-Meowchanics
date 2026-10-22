package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.CatGenome;
import cn.laowu.mod.genetics.CatGenomeData;
import cn.laowu.mod.genetics.CatMaterialRegistry;
import cn.laowu.mod.genetics.CatRegion;
import cn.laowu.mod.genetics.CatTrait;
import cn.laowu.mod.genetics.CatTraitData;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Builds and caches 64x32 cat textures assembled from semantic texture regions. */
@OnlyIn(Dist.CLIENT)
public final class CatGenomeTextureManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation REGION_MAP = LaoWuMod.id("textures/entity/cat/region_map.png");
    private static final ResourceLocation VANILLA_RED_CAT =
            new ResourceLocation("minecraft", "textures/entity/cat/red.png");
    private static final int MAX_TEXTURES = 256;
    private static final int MAX_BLOCK_MATERIALS = 256;

    private static final LinkedHashMap<String, ResourceLocation> COMPOSITES =
            new LinkedHashMap<>(32, 0.75F, true);
    private static final Map<ResourceLocation, NativeImage> SOURCE_IMAGES = new HashMap<>();
    private static final Map<ResourceLocation, NativeImage> NATIVE_SOURCE_IMAGES = new HashMap<>();
    private static final LinkedHashMap<ResourceLocation, NativeImage> BLOCK_SOURCE_IMAGES =
            new LinkedHashMap<>(32, 0.75F, true);
    private static final LinkedHashMap<ResourceLocation, ResourceLocation> BLOCK_TEXTURES =
            new LinkedHashMap<>(32, 0.75F, true);
    private static NativeImage regionMap;
    private static java.util.List<CemEyePixel> cemEyePixels;
    private record CemEyePixel(int x, int y, CatRegion region, boolean pupil) {}

    public static ResourceLocation resolve(Cat cat) {
        boolean rainbow = CatTraitData.read(cat)
                .map(profile -> profile.has(CatTrait.RAINBOW_CAT)).orElse(false);
        return resolve(CatGenomeData.read(cat).orElse(null),
                cat.getVariant().texture(), rainbow);
    }

    public static ResourceLocation resolve(ItemStack stack, ResourceLocation fallback) {
        boolean rainbow = CatTraitData.read(stack)
                .map(profile -> profile.has(CatTrait.RAINBOW_CAT)).orElse(false);
        return resolve(CatGenomeData.read(stack).orElse(null), fallback, rainbow);
    }

    /** Our authored poses use vanilla UVs, not a CEM pack's separate eye planes. */
    public static ResourceLocation resolveNativeGeometry(Cat cat) {
        boolean rainbow = CatTraitData.read(cat)
                .map(profile -> profile.has(CatTrait.RAINBOW_CAT)).orElse(false);
        return resolve(CatGenomeData.read(cat).orElse(null),
                cat.getVariant().texture(), rainbow, true);
    }

    public static ResourceLocation resolveResourceGeometry(Cat cat) {
        boolean rainbow = CatTraitData.read(cat)
                .map(profile -> profile.has(CatTrait.RAINBOW_CAT)).orElse(false);
        return resolve(CatGenomeData.read(cat).orElse(null),
                cat.getVariant().texture(), rainbow, false, true);
    }

    /** Resolves a stored phenotype for non-vanilla cat-shaped entities. */
    public static ResourceLocation resolve(CatGenome genome, ResourceLocation fallback) {
        return resolve(genome, fallback, false);
    }

    /** The articulated boxer exposes caps hidden inside the ordinary cat model. */
    public static synchronized ResourceLocation resolveFighter(CatGenome genome, ResourceLocation fallback) {
        if (genome == null) return fallback;
        String key = "fighter|" + genome.phenotypeKey();
        ResourceLocation cached = COMPOSITES.get(key);
        if (cached != null) return cached;
        NativeImage image = null;
        try {
            image = genome.isUniform()
                    ? copy(sourceImageForMaterial(genome.material(CatRegion.HEAD_PRIMARY), fallback))
                    : compose(genome, fallback);
            fillHiddenCaps(image);
            ResourceLocation location = LaoWuMod.id("dynamic/cat/fighter/" + sha256(key));
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
            image = null; // Ownership transferred to the texture manager.
            COMPOSITES.put(key, location);
            evictOldest();
            return location;
        } catch (Exception exception) {
            LOGGER.error("Could not compose boxing cat texture {}", key, exception);
            return fallback;
        } finally {
            if (image != null) image.close();
        }
    }

    private static void fillHiddenCaps(NativeImage image) {
        int width = image.getWidth(), height = image.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++)
            pixels[y * width + x] = image.getPixelRGBA(x, y);
        // Use original neighbours only, so newly filled pixels cannot smear a colour across the atlas.
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            if ((pixels[y * width + x] >>> 24) != 0) continue;
            int nearest = 0, distance = Integer.MAX_VALUE;
            for (int index = 0; index < pixels.length; index++) {
                if ((pixels[index] >>> 24) == 0) continue;
                int dx = index % width - x, dy = index / width - y;
                int candidate = dx * dx + dy * dy;
                if (candidate < distance) { distance = candidate; nearest = pixels[index]; }
            }
            if (distance != Integer.MAX_VALUE) image.setPixelRGBA(x, y, nearest | 0xFF000000);
        }
    }

    private static synchronized ResourceLocation resolve(CatGenome genome,
                                                         ResourceLocation fallback,
                                                         boolean rainbow) {
        return resolve(genome, fallback, rainbow, false);
    }

    private static synchronized ResourceLocation resolve(CatGenome genome,
                                                         ResourceLocation fallback,
                                                         boolean rainbow, boolean nativeAtlas) {
        return resolve(genome, fallback, rainbow, nativeAtlas, false);
    }

    private static synchronized ResourceLocation resolve(CatGenome genome,
                                                         ResourceLocation fallback,
                                                         boolean rainbow, boolean nativeAtlas,
                                                         boolean resourceAtlas) {
        if (!nativeAtlas && !rainbow && genome == null) return fallback;
        if (!nativeAtlas && !resourceAtlas && !rainbow && genome.isUniform()) {
            return textureForMaterial(genome.material(CatRegion.HEAD_PRIMARY), fallback);
        }

        String key = (genome == null ? "fallback=" + fallback : genome.phenotypeKey())
                + (rainbow ? "|appearance=rainbow" : "") + (nativeAtlas ? "|native-atlas" : "")
                + (resourceAtlas ? "|resource-atlas" : "");
        ResourceLocation cached = COMPOSITES.get(key);
        if (cached != null) return cached;

        try {
            NativeImage composed;
            if (genome == null) {
                composed = copy(sourceImage(fallback, nativeAtlas));
            } else if (genome.isUniform()) {
                composed = copy(sourceImageForMaterial(
                        genome.material(CatRegion.HEAD_PRIMARY), fallback, nativeAtlas));
            } else {
                composed = compose(genome, fallback, nativeAtlas, resourceAtlas);
            }
            if (rainbow) applyRainbow(composed);
            if (resourceAtlas) applyCemEyeMaterials(composed, genome, fallback);
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
        return compose(genome, fallback, false);
    }

    private static NativeImage compose(CatGenome genome, ResourceLocation fallback,
                                       boolean nativeAtlas) throws IOException {
        return compose(genome, fallback, nativeAtlas, false);
    }

    private static NativeImage compose(CatGenome genome, ResourceLocation fallback,
                                       boolean nativeAtlas, boolean resourceAtlas) throws IOException {
        NativeImage mask = regionMap();
        NativeImage output = new NativeImage(mask.getWidth(), mask.getHeight(), true);
        NativeImage auxiliary = resourceAtlas
                ? sourceImageForMaterial(genome.material(CatRegion.HEAD_PRIMARY), fallback) : null;

        for (int y = 0; y < mask.getHeight(); y++) {
            for (int x = 0; x < mask.getWidth(); x++) {
                int alpha = Byte.toUnsignedInt(mask.getLuminanceOrAlpha(x, y));
                if (alpha == 0) {
                    // CEM packs use atlas space outside our vanilla semantic mask.
                    if (auxiliary != null) output.setPixelRGBA(x,y,auxiliary.getPixelRGBA(
                            x * auxiliary.getWidth() / mask.getWidth(),
                            y * auxiliary.getHeight() / mask.getHeight()));
                    continue;
                }
                int rgb = Byte.toUnsignedInt(mask.getRedOrLuminance(x, y)) << 16
                        | Byte.toUnsignedInt(mask.getGreenOrLuminance(x, y)) << 8
                        | Byte.toUnsignedInt(mask.getBlueOrLuminance(x, y));
                CatRegion region = CatRegion.fromMapRgb(rgb);
                if (region == null) continue;

                NativeImage source = sourceImageForMaterial(genome.material(region), fallback, nativeAtlas);
                // The vanilla face axis is x=7. Mirror the right-eye lookup
                // from the left-eye UV so every material produces matching
                // eye size and pixel structure, even when the two eyes use
                // different material ids.
                int materialX = region == CatRegion.RIGHT_EYE ? 14 - x : x;
                int sourceX = Math.min(source.getWidth() - 1,
                        materialX * source.getWidth() / mask.getWidth());
                int sourceY = Math.min(source.getHeight() - 1, y * source.getHeight() / mask.getHeight());
                output.setPixelRGBA(x, y, source.getPixelRGBA(sourceX, sourceY));
            }
        }
        return output;
    }

    private static void applyCemEyeMaterials(NativeImage output, CatGenome genome,
                                             ResourceLocation fallback) throws IOException {
        for (CemEyePixel pixel : cemEyePixels()) {
            NativeImage source = genome == null ? sourceImage(fallback)
                    : sourceImageForMaterial(genome.material(pixel.region()), fallback);
            int colour = source.getPixelRGBA(pixel.x() * source.getWidth() / 64,
                    pixel.y() * source.getHeight() / 32);
            // Block/legacy materials only paint the vanilla eye locus.
            if ((colour >>> 24) == 0) colour = source.getPixelRGBA(
                    (pixel.pupil() ? 6 : 5) * source.getWidth() / 64,6 * source.getHeight() / 32);
            output.setPixelRGBA(pixel.x() * output.getWidth() / 64,
                    pixel.y() * output.getHeight() / 32,colour);
        }
    }

    private static java.util.List<CemEyePixel> cemEyePixels() {
        if (cemEyePixels != null) return cemEyePixels;
        var pixels = new java.util.ArrayList<CemEyePixel>();
        // Read the active pack's declared eye UVs rather than hardcoding Fresh's atlas.
        try (var stream = Minecraft.getInstance().getResourceManager().open(
                ResourceLocation.withDefaultNamespace("optifine/cem/cat.jem"));
             var reader = new java.io.InputStreamReader(stream, StandardCharsets.UTF_8)) {
            collectCemEyePixels(com.google.gson.JsonParser.parseReader(reader), pixels);
        } catch (IOException | RuntimeException ignored) {
            // Unrecognised packs still retain their complete auxiliary head atlas.
        }
        cemEyePixels = java.util.List.copyOf(pixels);
        return cemEyePixels;
    }

    private static void collectCemEyePixels(com.google.gson.JsonElement value,
                                            java.util.List<CemEyePixel> pixels) {
        if (value.isJsonArray()) {
            value.getAsJsonArray().forEach(child -> collectCemEyePixels(child,pixels));
        } else if (value.isJsonObject()) {
            var object = value.getAsJsonObject();
            String id = object.has("id") ? object.get("id").getAsString() : "";
            boolean left = id.equals("l_pupil") || id.equals("l_eye_white");
            boolean right = id.equals("r_pupil") || id.equals("r_eye_white");
            if ((left || right) && object.has("boxes")) {
                for (var box : object.getAsJsonArray("boxes")) {
                    var face = box.getAsJsonObject().getAsJsonArray("uvNorth");
                    if (face == null || face.size() != 4) continue;
                    int x1=face.get(0).getAsInt(),y1=face.get(1).getAsInt();
                    int x2=face.get(2).getAsInt(),y2=face.get(3).getAsInt();
                    for(int y=Math.max(0,Math.min(y1,y2));y<Math.min(32,Math.max(y1,y2));y++)
                        for(int x=Math.max(0,Math.min(x1,x2));x<Math.min(64,Math.max(x1,x2));x++)
                            pixels.add(new CemEyePixel(x,y,left ? CatRegion.LEFT_EYE : CatRegion.RIGHT_EYE,
                                    id.endsWith("pupil")));
                }
            }
            object.entrySet().forEach(entry -> collectCemEyePixels(entry.getValue(),pixels));
        }
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

    private static NativeImage sourceImage(ResourceLocation texture, boolean nativeAtlas) throws IOException {
        if (!nativeAtlas) return sourceImage(texture);
        NativeImage cached = NATIVE_SOURCE_IMAGES.get(texture);
        if (cached != null) return cached;
        for (var resource : Minecraft.getInstance().getResourceManager().getResourceStack(texture)) {
            if (!"vanilla".equals(resource.sourcePackId())) continue;
            try (InputStream stream = resource.open()) {
                NativeImage loaded = NativeImage.read(stream);
                NATIVE_SOURCE_IMAGES.put(texture, loaded);
                return loaded;
            }
        }
        // Custom materials without a vanilla resource still belong to their supplying pack.
        return sourceImage(texture);
    }

    private static NativeImage sourceImageForMaterial(ResourceLocation material,
                                                      ResourceLocation fallback) throws IOException {
        return sourceImageForMaterial(material, fallback, false);
    }

    private static NativeImage sourceImageForMaterial(ResourceLocation material,
                                                      ResourceLocation fallback,
                                                      boolean nativeAtlas) throws IOException {
        ResourceLocation fixed = CatMaterialRegistry.fixedTexture(material).orElse(null);
        if (fixed != null) return sourceImage(fixed, nativeAtlas);

        ResourceLocation blockId = CatMaterialRegistry.blockId(material).orElse(null);
        if (blockId != null) {
            NativeImage cached = BLOCK_SOURCE_IMAGES.get(material);
            if (cached != null) return cached;
            NativeImage mapped = buildBlockMaterialImage(blockId);
            BLOCK_SOURCE_IMAGES.put(material, mapped);
            evictOldestBlockSource();
            return mapped;
        }

        CatVariant variant = BuiltInRegistries.CAT_VARIANT.get(material);
        return sourceImage(variant == null ? fallback : variant.texture(), nativeAtlas);
    }

    private static NativeImage load(ResourceLocation location) throws IOException {
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        try (InputStream stream = resources.open(location)) {
            return NativeImage.read(stream);
        }
    }

    private static ResourceLocation textureForMaterial(ResourceLocation material,
                                                       ResourceLocation fallback) {
        ResourceLocation fixed = CatMaterialRegistry.fixedTexture(material).orElse(null);
        if (fixed != null) return fixed;

        ResourceLocation blockId = CatMaterialRegistry.blockId(material).orElse(null);
        if (blockId != null) {
            ResourceLocation cached = BLOCK_TEXTURES.get(material);
            if (cached != null) return cached;
            try {
                NativeImage source = sourceImageForMaterial(material, fallback);
                NativeImage textureCopy = copy(source);
                ResourceLocation location = LaoWuMod.id("dynamic/cat_block/"
                        + sha256(material.toString()));
                Minecraft.getInstance().getTextureManager().register(location,
                        new DynamicTexture(textureCopy));
                BLOCK_TEXTURES.put(material, location);
                evictOldestBlockTexture();
                return location;
            } catch (IOException exception) {
                LOGGER.error("Could not map block material {} to a cat texture", blockId, exception);
                return fallback;
            }
        }

        CatVariant variant = BuiltInRegistries.CAT_VARIANT.get(material);
        return variant == null ? fallback : variant.texture();
    }

    /**
     * Converts the particle texture of one block into the exact 64x32 cat UV.
     * Vanilla cat luminance is retained as subtle face/body shading so an
     * arbitrary cube texture still reads as a cat rather than a flat cutout.
     */
    private static NativeImage buildBlockMaterialImage(ResourceLocation blockId) throws IOException {
        Block block = BuiltInRegistries.BLOCK.getOptional(blockId)
                .orElseThrow(() -> new IOException("Unknown block " + blockId));
        BlockState state = block.defaultBlockState();
        TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer()
                .getBlockModel(state).getParticleIcon();
        NativeImage blockPixels = sprite.contents().getOriginalImage();
        NativeImage mask = regionMap();
        NativeImage base = sourceImage(VANILLA_RED_CAT);
        NativeImage output = new NativeImage(mask.getWidth(), mask.getHeight(), true);

        int sampleWidth = Math.max(1, sprite.contents().width());
        int sampleHeight = Math.max(1, sprite.contents().height());
        int[] eyePalette = twoBrightestColours(blockPixels, sampleWidth, sampleHeight);
        for (int y = 0; y < mask.getHeight(); y++) {
            for (int x = 0; x < mask.getWidth(); x++) {
                int maskAlpha = Byte.toUnsignedInt(mask.getLuminanceOrAlpha(x, y));
                if (maskAlpha == 0) continue;
                int rgb = Byte.toUnsignedInt(mask.getRedOrLuminance(x, y)) << 16
                        | Byte.toUnsignedInt(mask.getGreenOrLuminance(x, y)) << 8
                        | Byte.toUnsignedInt(mask.getBlueOrLuminance(x, y));
                CatRegion region = CatRegion.fromMapRgb(rgb);
                if (region == null) continue;

                int materialX = region == CatRegion.RIGHT_EYE ? 14 - x : x;
                if (region == CatRegion.LEFT_EYE || region == CatRegion.RIGHT_EYE) {
                    // Each vanilla eye is two pixels wide. Use the two
                    // brightest distinct colours in the sampled block, then
                    // mirror their order on the opposite eye.
                    int paletteIndex = Math.max(0, Math.min(1, materialX - 5));
                    int eyeColour = eyePalette[paletteIndex];
                    output.setPixelRGBA(x, y, FastColor.ABGR32.color(maskAlpha,
                            FastColor.ABGR32.blue(eyeColour),
                            FastColor.ABGR32.green(eyeColour),
                            FastColor.ABGR32.red(eyeColour)));
                    continue;
                }
                int sampled = blockPixels.getPixelRGBA(
                        Math.floorMod(materialX * 3 + y, sampleWidth),
                        Math.floorMod(y * 3 + materialX / 2, sampleHeight));
                int basePixel = base.getPixelRGBA(x, y);
                int baseLuma = (FastColor.ABGR32.red(basePixel) * 54
                        + FastColor.ABGR32.green(basePixel) * 183
                        + FastColor.ABGR32.blue(basePixel) * 19) >> 8;
                double shade = 0.58D + baseLuma / 400.0D;
                if (region == CatRegion.MUZZLE && baseLuma < 95) {
                    shade *= 0.38D;
                }

                int red = clamp((int) (FastColor.ABGR32.red(sampled) * shade));
                int green = clamp((int) (FastColor.ABGR32.green(sampled) * shade));
                int blue = clamp((int) (FastColor.ABGR32.blue(sampled) * shade));
                int alpha = FastColor.ABGR32.alpha(sampled) * maskAlpha / 255;
                output.setPixelRGBA(x, y,
                        FastColor.ABGR32.color(alpha, blue, green, red));
            }
        }
        return output;
    }

    private static int[] twoBrightestColours(NativeImage image, int width, int height) {
        Set<Integer> seen = new HashSet<>();
        int brightest = FastColor.ABGR32.color(255, 255, 255, 255);
        int second = brightest;
        int brightestScore = -1;
        int secondScore = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getPixelRGBA(x, y);
                if (FastColor.ABGR32.alpha(pixel) == 0) continue;
                int opaque = FastColor.ABGR32.opaque(pixel);
                if (!seen.add(opaque)) continue;
                int score = perceivedBrightness(opaque);
                if (score > brightestScore) {
                    second = brightest;
                    secondScore = brightestScore;
                    brightest = opaque;
                    brightestScore = score;
                } else if (score > secondScore) {
                    second = opaque;
                    secondScore = score;
                }
            }
        }
        if (brightestScore < 0) return new int[] {brightest, brightest};
        if (secondScore < 0) second = brightest;
        return new int[] {brightest, second};
    }

    /** Integer approximation of perceptual sRGB luminance. */
    private static int perceivedBrightness(int abgr) {
        return FastColor.ABGR32.red(abgr) * 2126
                + FastColor.ABGR32.green(abgr) * 7152
                + FastColor.ABGR32.blue(abgr) * 722;
    }

    /** Saves the exact mapped PNG for external editing or later data-pack registration. */
    public static synchronized Path exportBlockMaterial(BlockState state) throws IOException {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId == null) throw new IOException("Unregistered block");
        NativeImage mapped = buildBlockMaterialImage(blockId);
        try {
            Path output = Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("create_meowchanics")
                    .resolve("cat_material_exports")
                    .resolve(blockId.getNamespace())
                    .resolve(blockId.getPath() + ".png");
            Files.createDirectories(output.getParent());
            mapped.writeToFile(output);
            return output;
        } finally {
            mapped.close();
        }
    }

    private static NativeImage copy(NativeImage source) {
        NativeImage copy = new NativeImage(source.getWidth(), source.getHeight(), true);
        copy.copyFrom(source);
        return copy;
    }

    /** Recolours fur into six spatial bands while preserving eyes and shading. */
    private static void applyRainbow(NativeImage image) throws IOException {
        int[][] palette = {
                {255, 64, 64}, {255, 145, 40}, {255, 226, 48},
                {55, 220, 88}, {58, 126, 255}, {182, 72, 255}
        };
        NativeImage mask = regionMap();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixelRGBA(x, y);
                int alpha = FastColor.ABGR32.alpha(pixel);
                if (alpha == 0) continue;

                int mapX = Math.min(mask.getWidth() - 1,
                        x * mask.getWidth() / image.getWidth());
                int mapY = Math.min(mask.getHeight() - 1,
                        y * mask.getHeight() / image.getHeight());
                int rgb = Byte.toUnsignedInt(mask.getRedOrLuminance(mapX, mapY)) << 16
                        | Byte.toUnsignedInt(mask.getGreenOrLuminance(mapX, mapY)) << 8
                        | Byte.toUnsignedInt(mask.getBlueOrLuminance(mapX, mapY));
                CatRegion region = CatRegion.fromMapRgb(rgb);
                if (region == CatRegion.LEFT_EYE || region == CatRegion.RIGHT_EYE) continue;

                int sourceLuma = (FastColor.ABGR32.red(pixel) * 54
                        + FastColor.ABGR32.green(pixel) * 183
                        + FastColor.ABGR32.blue(pixel) * 19) >> 8;
                // Keep very dark face details readable instead of tinting them
                // into bright confetti.
                if (sourceLuma < 36) continue;
                int[] colour = palette[Math.floorMod(x / 3 + y / 2, palette.length)];
                double shade = Math.max(0.34D, Math.min(1.08D,
                        0.35D + sourceLuma / 255.0D * 0.75D));
                int red = clamp((int) Math.round(colour[0] * shade));
                int green = clamp((int) Math.round(colour[1] * shade));
                int blue = clamp((int) Math.round(colour[2] * shade));
                image.setPixelRGBA(x, y,
                        FastColor.ABGR32.color(alpha, blue, green, red));
            }
        }
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
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

    private static void evictOldestBlockSource() {
        while (BLOCK_SOURCE_IMAGES.size() > MAX_BLOCK_MATERIALS) {
            Iterator<Map.Entry<ResourceLocation, NativeImage>> iterator =
                    BLOCK_SOURCE_IMAGES.entrySet().iterator();
            if (!iterator.hasNext()) return;
            NativeImage old = iterator.next().getValue();
            iterator.remove();
            old.close();
        }
    }

    private static void evictOldestBlockTexture() {
        while (BLOCK_TEXTURES.size() > MAX_BLOCK_MATERIALS) {
            Iterator<Map.Entry<ResourceLocation, ResourceLocation>> iterator =
                    BLOCK_TEXTURES.entrySet().iterator();
            if (!iterator.hasNext()) return;
            ResourceLocation old = iterator.next().getValue();
            iterator.remove();
            Minecraft.getInstance().getTextureManager().release(old);
        }
    }

    /** Called by Forge's client resource reload event; releases both RAM and GPU objects. */
    public static synchronized void clear() {
        var textureManager = Minecraft.getInstance().getTextureManager();
        COMPOSITES.values().forEach(textureManager::release);
        COMPOSITES.clear();
        BLOCK_TEXTURES.values().forEach(textureManager::release);
        BLOCK_TEXTURES.clear();
        SOURCE_IMAGES.values().forEach(NativeImage::close);
        SOURCE_IMAGES.clear();
        NATIVE_SOURCE_IMAGES.values().forEach(NativeImage::close);
        NATIVE_SOURCE_IMAGES.clear();
        cemEyePixels = null;
        BLOCK_SOURCE_IMAGES.values().forEach(NativeImage::close);
        BLOCK_SOURCE_IMAGES.clear();
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
