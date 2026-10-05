package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.create.*;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.item.CatPancakeItem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import java.util.Arrays;

/** Exercises the registered world renderers, including compact packet -> client display. */
public final class AdoptionInputDisplayVisualProbe {
    public static void verify(Minecraft mc) {
        try {
            nineInputs(mc);
            for (boolean wish : new boolean[]{false, true}) for (Direction facing : Direction.Plane.HORIZONTAL) {
                var ordinary = new AdoptionBoxBlockEntity(BlockPos.ZERO,
                        LaoWuMod.ADOPTION_BOX.get().defaultBlockState().setValue(AdoptionBoxBlock.FACING, facing));
                var special = new WishAdoptionBoxBlockEntity(BlockPos.ZERO,
                        LaoWuMod.WISH_ADOPTION_BOX.get().defaultBlockState().setValue(WishAdoptionBoxBlock.FACING, facing));
                int[][] frames = new int[4][];
                for (int phase = 0; phase < 4; phase++) {
                    ItemStack stack = phase == 1 || phase == 2
                            ? CatPancakeItem.variantStack(ResourceLocation.tryParse(
                                    phase == 1 ? "minecraft:all_black" : "minecraft:red")) : ItemStack.EMPTY;
                    Runnable draw;
                    if (wish) {
                        var box = special;
                        box.inventory().setStackInSlot(2, stack);
                        box.loadAdditional(box.getUpdateTag(RegistryAccess.EMPTY), RegistryAccess.EMPTY);
                        var renderer = mc.getBlockEntityRenderDispatcher().getRenderer(box);
                        draw = () -> render(mc, p -> renderer.render(box, 0, p, mc.renderBuffers().bufferSource(),
                                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY));
                    } else {
                        var box = ordinary;
                        box.inventory().setStackInSlot(2, stack);
                        box.loadAdditional(box.getUpdateTag(RegistryAccess.EMPTY), RegistryAccess.EMPTY);
                        var renderer = mc.getBlockEntityRenderDispatcher().getRenderer(box);
                        draw = () -> render(mc, p -> renderer.render(box, 0, p, mc.renderBuffers().bufferSource(),
                                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY));
                    }
                    frames[phase] = capture(mc, 384, 384,
                            "adoption-input-" + wish + "-" + facing.getName() + "-" + phase + ".png", draw);
                }
                int added = differences(frames[0], frames[1]), coat = differences(frames[1], frames[2]);
                if (added < 100) throw new AssertionError("Input pancake adds visible 3D geometry: " + wish + "/" + facing + " pixels=" + added);
                if (coat < 100) throw new AssertionError("Actual black/red input coat changes visible pancake: " + wish + "/" + facing + " pixels=" + coat);
                if (!Arrays.equals(frames[0], frames[3])) throw new AssertionError("Empty input restores empty box");
            }
            System.out.println("PASS: both adoption world renderers show actual input coats in four facings and clear on removal");
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    private static void render(Minecraft mc, java.util.function.Consumer<PoseStack> draw) {
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(-.9F, .9F, -.9F, .9F, -10, 10), VertexSorting.ORTHOGRAPHIC_Z);
        var pose = new PoseStack();
        pose.mulPose(Axis.XP.rotationDegrees(cameraTilt)); pose.mulPose(Axis.YP.rotationDegrees(205));
        pose.translate(-.5, -.3, -.5);
        draw.accept(pose);
        mc.renderBuffers().bufferSource().endBatch();
        if (!pose.clear()) throw new AssertionError("World renderer restores pose depth");
    }
    private static int differences(int[] a, int[] b) {
        int count = 0; for (int i = 0; i < a.length; i++) if (a[i] != b[i]) count++; return count;
    }
    private static int[] capture(Minecraft mc, int width, int height, String name, Runnable draw) throws Exception {
        int[] pixels = new int[width * height];
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix(); view.identity(); RenderSystem.applyModelViewMatrix();
        TextureTarget output = null;
        try (var guard = new CatPerformanceOutline.State(); var framebuffer = new PerformanceSceneSnapshot.Target()) {
            output = new TextureTarget(width, height, true, Minecraft.ON_OSX); output.bindWrite(true);
            RenderSystem.clearColor(0, 0, 0, 0); RenderSystem.clearDepth(1); RenderSystem.depthMask(true);
            RenderSystem.clear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT | org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.setShaderColor(1, 1, 1, 1); RenderSystem.enableDepthTest(); RenderSystem.enableCull();
            draw.run();
            try (var image = new NativeImage(width, height, false)) {
                RenderSystem.bindTexture(output.getColorTextureId()); image.downloadTexture(0, false);
                for (int x = 0; x < width; x++) for (int y = 0; y < height; y++) pixels[y * width + x] = image.getPixelRGBA(x, y);
                if (name != null) { image.flipY(); image.writeToFile(java.nio.file.Path.of(name)); }
            }
        } finally {
            if (output != null) output.destroyBuffers();
            view.popMatrix(); RenderSystem.applyModelViewMatrix(); RenderSystem.setProjectionMatrix(projection, sorting);
        }
        return pixels;
    }

    private static final String[] COATS = {"all_black", "red", "siamese", "white", "jellie",
            "calico", "british_shorthair", "persian", "ragdoll"};
    // Look inside the box; a low-angle ray through an edge slot correctly hits its wall.
    private static float cameraTilt = 80;
    private static void nineInputs(Minecraft mc) throws Exception {
        cameraTilt = 90;
        for (boolean large : new boolean[]{false, true})
        for (boolean wish : new boolean[]{false, true}) for (Direction facing : Direction.Plane.HORIZONTAL) {
            var ordinary = new AdoptionBoxBlockEntity(BlockPos.ZERO,
                    LaoWuMod.ADOPTION_BOX.get().defaultBlockState().setValue(AdoptionBoxBlock.FACING, facing));
            var special = new WishAdoptionBoxBlockEntity(BlockPos.ZERO,
                    LaoWuMod.WISH_ADOPTION_BOX.get().defaultBlockState().setValue(WishAdoptionBoxBlock.FACING, facing));
            var inventory = wish ? special.inventory() : ordinary.inventory();
            Runnable sync = () -> { ordinary.loadAdditional(ordinary.getUpdateTag(RegistryAccess.EMPTY), RegistryAccess.EMPTY); special.loadAdditional(special.getUpdateTag(RegistryAccess.EMPTY), RegistryAccess.EMPTY); };
            Runnable draw = () -> render(mc, p -> {
                if (wish) mc.getBlockEntityRenderDispatcher().getRenderer(special).render(special, 0, p,
                        mc.renderBuffers().bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                else mc.getBlockEntityRenderDispatcher().getRenderer(ordinary).render(ordinary, 0, p,
                        mc.renderBuffers().bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            });
            String prefix = "adoption-nine-" + wish + "-" + facing.getName() + (large ? "-large" : "");
            int[] empty = capture(mc, 640, 640, prefix + "-empty.png", draw);
            ItemStack[] cats = new ItemStack[9];
            int[][] single = new int[9][];
            for (int slot = 0; slot < 9; slot++) {
                cats[slot] = CatPancakeItem.variantStack(ResourceLocation.tryParse("minecraft:" + COATS[slot]));
                if (large) CatTraitData.set(cats[slot], CatTraitProfile.EMPTY.withLevel(CatTrait.BIG_CHONKY_CAT, 7));
                inventory.setStackInSlot(slot, cats[slot]);
                sync.run();
                single[slot] = capture(mc, 640, 640, null, draw);
                if (differences(empty, single[slot]) < 100)
                    throw new AssertionError("Each input slot must be visible: " + prefix + " slot=" + slot);
                for (int pixel = 0; pixel < empty.length; pixel++)
                    if (single[slot][pixel] != empty[pixel] && (empty[pixel] >>> 24) == 0)
                        throw new AssertionError("Each cat stays within the box footprint: " + prefix + " slot=" + slot);
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
            for (int a = 0; a < 9; a++) for (int b = a + 1; b < 9; b++) {
                int overlap = 0;
                for (int pixel = 0; pixel < empty.length; pixel++)
                    if (single[a][pixel] != empty[pixel] && single[b][pixel] != empty[pixel]) overlap++;
                if (overlap != 0) throw new AssertionError("Input cats overlap: " + prefix + " slots=" + a + "/" + b + " pixels=" + overlap);
            }
            for (int slot = 0; slot < 9; slot++) inventory.setStackInSlot(slot, cats[slot]);
            inventory.setStackInSlot(9, CatPancakeItem.variantStack(ResourceLocation.tryParse("minecraft:tabby")));
            sync.run();
            int[] full = capture(mc, 640, 640, prefix + "-full.png", draw);
            for (int slot = 0; slot < 9; slot++) {
                inventory.setStackInSlot(slot, ItemStack.EMPTY); sync.run();
                int[] removed = capture(mc, 640, 640, null, draw);
                if (differences(full, removed) < 100)
                    throw new AssertionError("Removing each of nine cats changes actual rendered geometry: " + prefix + " slot=" + slot);
                for (int pixel = 0; pixel < empty.length; pixel++)
                    if (single[slot][pixel] == empty[pixel] && full[pixel] != removed[pixel])
                        throw new AssertionError("Removing slot must not reposition other cats: " + prefix + " slot=" + slot);
                inventory.setStackInSlot(slot, CatPancakeItem.variantStack(ResourceLocation.tryParse("minecraft:tabby"))); sync.run();
                if (differences(full, capture(mc, 640, 640, null, draw)) < 100)
                    throw new AssertionError("Each of nine cats retains its own coat: " + prefix + " slot=" + slot);
                inventory.setStackInSlot(slot, cats[slot]); sync.run();
            }
            cameraTilt = 65;
            capture(mc, 640, 640, prefix + "-oblique.png", draw);
            cameraTilt = 90;
            for (int slot = 0; slot < 9; slot++) inventory.setStackInSlot(slot, ItemStack.EMPTY);
            sync.run();
            if (!Arrays.equals(empty, capture(mc, 640, 640, prefix + "-cleared.png", draw)))
                throw new AssertionError("Clearing all inputs restores empty box despite output cat: " + prefix);
        }
        cameraTilt = 80;
        System.out.println("PASS: nine distinct cats in both boxes/four facings; per-slot coat, stable removal, no overlap or output display");
    }
    private AdoptionInputDisplayVisualProbe() {}
}
