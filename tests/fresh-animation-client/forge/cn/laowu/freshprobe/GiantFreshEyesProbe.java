package cn.laowu.freshprobe;

import cn.laowu.mod.client.GiantCatRenderer;
import cn.laowu.mod.client.HissingCatRenderer;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.genetics.*;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Real Fresh atlas + real giant renderers: eye remap must not replace resource-pack fur. */
final class GiantFreshEyesProbe {
    private static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
    static void verify(Minecraft mc) throws Exception {
        Cat cat=cat(mc);
        var field=HissingCatRenderer.class.getDeclaredField("giant"); field.setAccessible(true);
        var renderer=(GiantCatRenderer)field.get(mc.getEntityRenderDispatcher().getRenderer(cat));
        verifyPixels(mc,renderer.getTextureLocation(cat));
        var boss=LaoWuMod.GIANT_CAT_BOSS.get().create(mc.level);
        boss.setInheritedGenome(CatGenomeData.read(cat).orElseThrow());
        verifyPixels(mc,mc.getEntityRenderDispatcher().getRenderer(boss).getTextureLocation(boss));
        // Also exercise uniform phenotypes, not just the mixed phenotype.
        CatGenomeData.set(cat,CatGenome.uniform(ResourceLocation.withDefaultNamespace("black")));
        var location=renderer.getTextureLocation(cat);
        try(var stream=mc.getResourceManager().open(ResourceLocation.withDefaultNamespace("textures/entity/cat/black.png"));
            var pack=NativeImage.read(stream)) {
            var image=pixels(mc,location);
            check((image.getPixelRGBA(5,6)>>>24)!=0,"Uniform giant eye missing");
            check(image.getPixelRGBA(20,12)==pack.getPixelRGBA(20,12),"Uniform giant discarded active pack fur");
            for(int y=0;y<32;y++) for(int x=0;x<64;x++) {
                if(y==6 && (x==5 || x==6 || x==8 || x==9)) continue;
                check(image.getPixelRGBA(x,y)==pack.getPixelRGBA(x,y),
                        "Giant changed active pack atlas outside eye mask at "+x+","+y);
            }
        }
        Cat plain=EntityType.CAT.create(mc.level);
        check((pixels(mc,renderer.getTextureLocation(plain)).getPixelRGBA(5,6)>>>24)!=0,
                "Giant without stored genome lost its default eye");
        var plainBoss=LaoWuMod.GIANT_CAT_BOSS.get().create(mc.level);
        check((pixels(mc,mc.getEntityRenderDispatcher().getRenderer(plainBoss).getTextureLocation(plainBoss))
                .getPixelRGBA(5,6)>>>24)!=0,"Boss without inherited genome lost its default eye");
        System.out.println("PASS: GIANT FRESH EYES - pet and boss, genetic left/right pupils, active pack fur, uniform coat");
    }
    private static NativeImage pixels(Minecraft mc,ResourceLocation location) throws Exception {
        var texture=mc.getTextureManager().getTexture(location);
        check(texture instanceof DynamicTexture,"Giant did not remap resource-pack eyes into authored atlas");
        return ((DynamicTexture)texture).getPixels();
    }
    private static void verifyPixels(Minecraft mc,ResourceLocation location) throws Exception {
        var image=pixels(mc,location);
        try(var ls=mc.getResourceManager().open(ResourceLocation.withDefaultNamespace("textures/entity/cat/siamese.png"));
            var rs=mc.getResourceManager().open(ResourceLocation.withDefaultNamespace("textures/entity/cat/red.png"));
            var bs=mc.getResourceManager().open(ResourceLocation.withDefaultNamespace("textures/entity/cat/black.png"));
            var left=NativeImage.read(ls);var right=NativeImage.read(rs);var body=NativeImage.read(bs)) {
            check((image.getPixelRGBA(5,6)>>>24)!=0 && (image.getPixelRGBA(9,6)>>>24)!=0,"Giant eye whites missing");
            check(image.getPixelRGBA(6,6)==left.getPixelRGBA(3,3),"Giant left pupil lost Fresh/genetic eye material");
            check(image.getPixelRGBA(8,6)==right.getPixelRGBA(1,3),"Giant right pupil lost Fresh/genetic eye material");
            check(image.getPixelRGBA(20,12)==body.getPixelRGBA(20,12),"Giant replaced resource-pack body with vanilla fur");
        }
    }
    private static Cat cat(Minecraft mc) {
        Cat cat=EntityType.CAT.create(mc.level); cat.setTame(true);cat.setAge(0);
        var effects=new net.minecraft.nbt.CompoundTag();effects.putDouble("giant_mount",1);
        var state=new net.minecraft.nbt.CompoundTag();state.put("Effects",effects);
        cat.getPersistentData().put(cn.laowu.mod.accessory.CatAccessories.CLIENT_STATE,state);
        CatGenomeData.set(cat,CatGenome.uniform(ResourceLocation.withDefaultNamespace("black"))
            .withMaterial(CatRegion.LEFT_EYE,ResourceLocation.withDefaultNamespace("siamese"))
            .withMaterial(CatRegion.RIGHT_EYE,ResourceLocation.withDefaultNamespace("red")));
        return cat;
    }
    static void draw(Minecraft mc,GuiGraphics graphics,int width,int height) {
        graphics.flush();
        com.mojang.blaze3d.systems.RenderSystem.clearColor(.157F,.176F,.208F,1F);
        com.mojang.blaze3d.systems.RenderSystem.clear(16640,Minecraft.ON_OSX);
        graphics.fill(0,0,width,height,0xff282d35);
        graphics.drawString(mc.font,"Fresh giant: siamese left eye / red right eye; active pack black fur",12,16,0xffffffff);
        Cat cat=cat(mc);
        InventoryScreen.renderEntityInInventory(graphics,width/2,(int)(height*.90F),48,
            new Quaternionf().rotateZ((float)Math.PI).rotateY((float)Math.PI+.25F),null,cat);
        graphics.flush();
    }
}
