package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.item.KimiArmorDye;
import cn.laowu.mod.item.KimiDyePalette;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import java.util.*;

/** Generated only for colours actually seen by this client; original resources remain untouched. */
public final class KimiArmorDyeTextures {
    private record Key(ResourceLocation source, int code) {}
    private static final Map<ResourceLocation, NativeImage> SOURCES = new HashMap<>();
    private static final LinkedHashMap<Key, ResourceLocation> CACHE = new LinkedHashMap<>(32,.75F,true);
    public static NativeImage source(ResourceLocation id) throws java.io.IOException {
        var cached = SOURCES.get(id);
        if (cached != null) return cached;
        try (var stream = Minecraft.getInstance().getResourceManager().open(id)) {
            var image = NativeImage.read(stream);
            SOURCES.put(id, image);
            return image;
        }
    }
    public static ResourceLocation texture(ResourceLocation source, ItemStack stack) {
        int code = KimiArmorDye.read(stack);
        if (code == 0) return source;
        Key key = new Key(source, code);
        var existing = CACHE.get(key);
        if (existing != null) return existing;
        try {
            NativeImage base = source(source);
            NativeImage output = new NativeImage(base.getWidth(), base.getHeight(), false);
            for (int y=0;y<base.getHeight();y++) for(int x=0;x<base.getWidth();x++) {
                int abgr = base.getPixelRGBA(x,y);
                int rgb = (abgr & 255) << 16 | abgr & 0xff00 | (abgr >> 16) & 255;
                int region = KimiDyePalette.region(source.getPath(), x, y, base.getWidth(), base.getHeight(), rgb);
                int dye = region < 0 ? -1 : KimiDyePalette.channel(code,region);
                if (dye >= 0 && (abgr >>> 24) != 0) {
                    int target = dyeRgb(DyeColor.byId(dye));
                    int result = KimiDyePalette.shade(rgb,target,region);
                    abgr = (abgr & 0xff000000) | ((result & 255) << 16) | (result & 0xff00) | (result >> 16);
                }
                output.setPixelRGBA(x,y,abgr);
            }
            ResourceLocation id = LaoWuMod.id("dynamic/kimi/" + source.getNamespace() + "/" + source.getPath() + "_" + code);
            Minecraft.getInstance().getTextureManager().register(id,new DynamicTexture(output));
            CACHE.put(key,id);
            if(CACHE.size()>256) {
                var iterator=CACHE.entrySet().iterator();
                var oldest=iterator.next(); iterator.remove();
                Minecraft.getInstance().getTextureManager().release(oldest.getValue());
            }
            return id;
        } catch (java.io.IOException exception) { return source; }
    }
    private static int dyeRgb(DyeColor dye) {
        return dye.getTextureDiffuseColor();
    }
    public static void clear() {
        if (!RenderSystem.isOnRenderThreadOrInit()) { RenderSystem.recordRenderCall(KimiArmorDyeTextures::clear); return; }
        for (var id:CACHE.values()) Minecraft.getInstance().getTextureManager().release(id);
        CACHE.clear();
        SOURCES.values().forEach(NativeImage::close); SOURCES.clear();
    }
    private KimiArmorDyeTextures() {}
}
