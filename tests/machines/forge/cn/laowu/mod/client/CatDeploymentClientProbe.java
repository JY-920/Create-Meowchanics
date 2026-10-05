package cn.laowu.mod.client;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.create.CatDeploymentBlock;
import cn.laowu.mod.create.CatDeploymentBlockEntity;
import cn.laowu.mod.item.CatPancakeItem;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Real baked models and registered BER; detached client BEs never edit the fixture world. */
final class CatDeploymentClientProbe {
    static void verify(Minecraft mc) throws Exception {
        fluidsAndTooltips(mc);
        check(mc.level != null && mc.level.isClientSide, "Deployment probe requires the isolated client world");
        String[] ids = {"cat_deployment_platform", "cat_ejecting_deployment_platform"};
        var blocks = List.of(LaoWuMod.CAT_DEPLOYMENT_PLATFORM.get(), LaoWuMod.CAT_EJECTING_DEPLOYMENT_PLATFORM.get());
        var items = List.of(LaoWuMod.CAT_DEPLOYMENT_PLATFORM_ITEM.get(), LaoWuMod.CAT_EJECTING_DEPLOYMENT_PLATFORM_ITEM.get());
        for (int i = 0; i < ids.length; i++) {
            var expected = LaoWuMod.id("block/" + ids[i]);
            var atlas = mc.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
            check(atlas.getSprite(expected).contents().name().equals(expected), "Authored texture not stitched: " + ids[i]);
            try (var stream = mc.getResourceManager().getResource(LaoWuMod.id("textures/block/" + ids[i] + ".png")).orElseThrow().open();
                 var image = NativeImage.read(stream)) {
                check(image.getWidth() == 64 && image.getHeight() == 64, "Authored 64px texture lost: " + ids[i]);
            }
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                var state = blocks.get(i).defaultBlockState().setValue(CatDeploymentBlock.FACING, facing);
                model(mc, mc.getBlockRenderer().getBlockModel(state), state, expected, 36, "block " + state);
                var vertices = new CatMixerIdleProbe.Capture();
                mc.getBlockRenderer().renderSingleBlock(state, new PoseStack(), type -> vertices,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                check(vertices.points.size() == 144, "Static housing must emit all six authored cubes: " + state);
                bounds(vertices.points, 0, 13 / 16d, "Housing height " + facing);
                finite(vertices.points, "Housing " + facing);
                var be = entity(mc, i == 1, facing);
                renderer(mc, be);
            }
            var stack = new ItemStack(items.get(i));
            model(mc, mc.getItemRenderer().getModel(stack, mc.level, null, 0), null, expected,
                    i == 0 ? 36 : 48, "inventory " + ids[i]);
            var vertices = new CatMixerIdleProbe.Capture();
            mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.GUI, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, new PoseStack(), type -> vertices, mc.level, 0);
            check(vertices.points.size() == (i == 0 ? 144 : 192), "Inventory omitted authored assembly: " + ids[i]);
            finite(vertices.points, "Inventory " + ids[i]);
        }
        var state = blocks.get(1).defaultBlockState();
        for (String name : new String[]{"PLATE", "ROD"}) {
            var field = CatDeploymentRenderer.class.getDeclaredField(name);
            field.setAccessible(true);
            var partial = (PartialModel) field.get(null);
            String suffix = name.toLowerCase(java.util.Locale.ROOT);
            check(partial.modelLocation().equals(LaoWuMod.id("block/cat_ejecting_deployment_platform_" + suffix)),
                    "Renderer references wrong " + suffix + " partial");
            model(mc, partial.get(), state, LaoWuMod.id("block/cat_ejecting_deployment_platform"), 6, suffix + " partial");
        }
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            var be = entity(mc, true, facing);
            for (int phase = 0; phase < 3; phase++) {
                animation(be, phase == 0 ? 0 : phase == 1 ? 18 : 11);
                var sample = sample(mc, be, .5f);
                check(sample.hardware.points.size() == 48, "Actual BER must draw exactly plate + rod: " + facing);
                check(sample.cat.points.isEmpty(), "Empty platform rendered a cat");
                List<Vec3> plate = sample.hardware.points.subList(0, 24), rod = sample.hardware.points.subList(24, 48);
                double[] plateMin = {.75, 1.25, 1.75}, plateMax = {.875, 1.375, 1.875};
                double[] rodMin = {.625, .6875, .75}, rodMax = {.75, 1.25, 1.75};
                bounds(plate, plateMin[phase], plateMax[phase], "Actual animated plate " + facing + "/" + phase);
                bounds(rod, rodMin[phase], rodMax[phase], "Actual nested rod " + facing + "/" + phase);
                for (var point : sample.hardware.points) {
                    check(point.x >= .125 - 1e-5 && point.x <= .875 + 1e-5
                            && point.z >= .125 - 1e-5 && point.z <= .875 + 1e-5,
                            "Facing rotation detached moving assembly: " + facing + "/" + point);
                }
                finite(sample.hardware.points, "Moving assembly " + facing);
            }
            animation(be, 0);
            var restored = sample(mc, be, .5f);
            bounds(restored.hardware.points, .625, .875, "Retracted assembly after animation");
            // Use a native vanilla coat rather than inventing any probe texture.
            be.inventory.setStackInSlot(0, CatPancakeItem.variantStack(net.minecraft.resources.ResourceLocation.tryParse("minecraft:red")));
            var low = sample(mc, be, .5f);
            check(low.cat.points.size() > 100, "Held Cat Pancake did not emit real custom geometry: " + facing);
            flatPancake(low.cat.points, 14 / 16d, "Retracted ejector " + facing);
            animation(be, 11);
            var high = sample(mc, be, .5f);
            check(high.cat.points.size() == low.cat.points.size(), "Animation changed pancake geometry count");
            flatPancake(high.cat.points, 14 / 16d + 1, "Extended ejector " + facing);
            for (int v = 0; v < low.cat.points.size(); v++) {
                var a = low.cat.points.get(v); var b = high.cat.points.get(v);
                check(Math.abs(b.y - a.y - 1) < 1e-5 && Math.abs(b.x - a.x) < 1e-5 && Math.abs(b.z - a.z) < 1e-5,
                        "Cat Pancake must travel one block with the plate, not stretch or detach: " + facing);
            }
            be.inventory.setStackInSlot(0, ItemStack.EMPTY);
            check(sample(mc, be, .5f).cat.points.isEmpty(), "Removed pancake retained renderer geometry");
            var normal = entity(mc, false, facing);
            check(sample(mc, normal, .5f).hardware.points.isEmpty(), "Normal platform drew ejector hardware");
            normal.inventory.setStackInSlot(0, CatPancakeItem.variantStack(net.minecraft.resources.ResourceLocation.tryParse("minecraft:red")));
            var normalPancake = sample(mc, normal, .5f);
            check(normalPancake.cat.points.size() > 100, "Normal platform omitted Cat Pancake");
            flatPancake(normalPancake.cat.points, 13 / 16d, "Normal platform " + facing);
            normal.inventory.setStackInSlot(0, ItemStack.EMPTY);
            check(sample(mc, normal, .5f).cat.points.isEmpty(), "Cleared normal platform retained Cat Pancake");
        }
        var empty = scene(mc, false, "cat-deployment-empty.png");
        var loaded = scene(mc, true, "cat-deployment-loaded.png");
        var halfFuel=scene(mc,false,"cat-deployment-fuel-half.png",2000);
        var fullFuel=scene(mc,false,"cat-deployment-fuel-full.png",4000);
        for(int column=0;column<3;column++){
            int halfChanged=0,fullChanged=0;
            for(int y=0;y<480;y++)for(int x=column*320;x<(column+1)*320;x++){
                int i=y*960+x;
                if(empty[i]!=halfFuel[i])halfChanged++;
                if(halfFuel[i]!=fullFuel[i])fullChanged++;
            }
            check(halfChanged>100&&fullChanged>100,"Fuel must be visible through real tank windows, with changing level: "+column+"/"+halfChanged+"/"+fullChanged);
        }
        for (int column = 0; column < 3; column++) {
            int difference = 0;
            for (int y = 0; y < 480; y++) for (int x = column * 320; x < (column + 1) * 320; x++)
                if (empty[y * 960 + x] != loaded[y * 960 + x]) difference++;
            check(difference > 30, "Held pancake is hidden by real GPU geometry in scene " + column + ": pixels=" + difference);
        }
        System.out.println("PASS: CAT DEPLOYMENT CLIENT - eight facing models, full items, exact sprites, registered plate/rod partials, low/half/peak vertices, moving and cleared Cat Pancakes, actual GPU previews");
    }

private static void fluidsAndTooltips(Minecraft mc) {
        for(boolean ejecting : new boolean[]{false,true}) for(Direction facing : Direction.Plane.HORIZONTAL) {
            var be=entity(mc,ejecting,facing);
            for(int amount : new int[]{0,1000,2000,4000}) {
                be.tank.setFluid(amount==0 ? net.minecraftforge.fluids.FluidStack.EMPTY : new net.minecraftforge.fluids.FluidStack(LaoWuMod.HISSING_GAS.get(),amount));
                var liquid=new CatMixerIdleProbe.Capture();
                var ignored=new CatMixerIdleProbe.Capture();
                renderer(mc,be).render(be,.5f,new PoseStack(),t->t==net.createmod.catnip.render.PonderRenderTypes.fluid()?liquid:ignored,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
                if(amount==0) check(liquid.points.isEmpty(),"Empty tank must not draw fuel");
                else {
                    check(liquid.points.size()>=20,"Filled deployment tank must emit visible liquid geometry");
                    double bottom=3.01/16d, full=(ejecting?9.99:12.99)/16d;
                    bounds(liquid.points,bottom,bottom+(full-bottom)*amount/4000d,"Fuel level follows stored amount");
                    for(var point:liquid.points) check(point.x>3/16d&&point.x<13/16d&&point.z>3/16d&&point.z<13/16d,"Fuel stays within tank cavity");
                }
            }
            be.tank.setFluid(net.minecraftforge.fluids.FluidStack.EMPTY);
            var item=new ItemStack((ejecting?LaoWuMod.CAT_EJECTING_DEPLOYMENT_PLATFORM_ITEM:LaoWuMod.CAT_DEPLOYMENT_PLATFORM_ITEM).get());
            var lines=new java.util.ArrayList<net.minecraft.network.chat.Component>();
            item.getItem().appendHoverText(item,mc.level,lines,net.minecraft.world.item.TooltipFlag.NORMAL);
            check(lines.size()==1,"Unselected platform tooltip keeps fuel info but not duplicated cost/target instructions");
        }
    }

    private static void model(Minecraft mc, BakedModel model, BlockState state,
                              net.minecraft.resources.ResourceLocation expected, int expectedFaces, String label) {
        check(model != null && model != mc.getModelManager().getMissingModel(), "Missing baked model: " + label);
        check(model.getParticleIcon().contents().name().equals(expected), "Wrong/missing particle sprite: " + label);
        var faces = new ArrayList<net.minecraft.client.renderer.block.model.BakedQuad>();
        faces.addAll(model.getQuads(state, null, RandomSource.create(1)));
        for (Direction side : Direction.values()) faces.addAll(model.getQuads(state, side, RandomSource.create(1)));
        check(faces.size() == expectedFaces, "Missing authored faces: " + label + " count=" + faces.size());
        for (var face : faces) {
            check(!face.getSprite().contents().name().equals(MissingTextureAtlasSprite.getLocation()), "Missing face texture: " + label);
            check(face.getSprite().contents().name().equals(expected), "Face uses wrong authored texture: " + label);
        }
    }

    private static CatDeploymentBlockEntity entity(Minecraft mc, boolean ejecting, Direction facing) {
        var state = (ejecting ? LaoWuMod.CAT_EJECTING_DEPLOYMENT_PLATFORM.get() : LaoWuMod.CAT_DEPLOYMENT_PLATFORM.get())
                .defaultBlockState().setValue(CatDeploymentBlock.FACING, facing);
        var be = new CatDeploymentBlockEntity(BlockPos.ZERO, state);
        be.setLevel(mc.level);
        return be;
    }

    private static BlockEntityRenderer<CatDeploymentBlockEntity> renderer(Minecraft mc, CatDeploymentBlockEntity be) {
        var renderer = mc.getBlockEntityRenderDispatcher().getRenderer(be);
        check(renderer instanceof CatDeploymentRenderer, "Actual deployment BER is not registered");
        return renderer;
    }

    private static void animation(CatDeploymentBlockEntity be, int ticks) {
        var tag = be.getUpdateTag();
        tag.putInt("Animation", ticks);
        be.load(tag);
    }

    private record Sample(CatMixerIdleProbe.Capture hardware, CatMixerIdleProbe.Capture cat) {}
    private static Sample sample(Minecraft mc, CatDeploymentBlockEntity be, float partial) {
        var hardware = new CatMixerIdleProbe.Capture();
        var cat = new CatMixerIdleProbe.Capture();
        var pose = new PoseStack();
        var before = new Matrix4f(pose.last().pose());
        renderer(mc, be).render(be, partial, pose, type -> type == RenderType.cutoutMipped() ? hardware : cat,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        check(before.equals(pose.last().pose()) && pose.clear(), "Deployment BER leaked pose transform/depth");
        return new Sample(hardware, cat);
    }

    /** FIXED already supplies the flat pancake pose; a second X=90 turn stands it up. */
    private static void flatPancake(List<Vec3> points, double surface, String label) {
        check(!points.isEmpty(), "No pancake vertices: " + label);
        double minY = points.stream().mapToDouble(point -> point.y).min().orElseThrow();
        double maxY = points.stream().mapToDouble(point -> point.y).max().orElseThrow();
        double minX = points.stream().mapToDouble(point -> point.x).min().orElseThrow();
        double maxX = points.stream().mapToDouble(point -> point.x).max().orElseThrow();
        double minZ = points.stream().mapToDouble(point -> point.z).min().orElseThrow();
        double maxZ = points.stream().mapToDouble(point -> point.z).max().orElseThrow();
        double height = maxY - minY, length = Math.max(maxX - minX, maxZ - minZ);
        check(height < length * .5, "Cat Pancake must lie flat, not stand upright: " + label
                + " height=" + height + " horizontalLength=" + length);
        double gap = minY - surface;
        check(gap >= -1e-5 && gap < .08, "Pancake feet must sit above the tabletop with <0.08 clearance: "
                + label + " surface=" + surface + " feet=" + minY + " gap=" + gap);
    }

    private static void bounds(List<Vec3> points, double expectedMin, double expectedMax, String label) {
        check(!points.isEmpty(), "No vertices for " + label);
        double min = points.stream().mapToDouble(point -> point.y).min().orElseThrow();
        double max = points.stream().mapToDouble(point -> point.y).max().orElseThrow();
        check(Math.abs(min - expectedMin) < 1e-5 && Math.abs(max - expectedMax) < 1e-5,
                label + ": expected Y " + expectedMin + ".." + expectedMax + ", actual " + min + ".." + max);
    }
    private static void finite(List<Vec3> points, String label) {
        for (var point : points) check(Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z),
                "Invalid renderer vertex " + label + "/" + point);
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }

    private static int[] scene(Minecraft mc, boolean loaded, String file) throws Exception {
        return scene(mc,loaded,file,0);
    }
    private static int[] scene(Minecraft mc, boolean loaded, String file,int fuel) throws Exception {
        int[] pixels = new int[960 * 480];
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var view = RenderSystem.getModelViewStack();
        view.pushPose(); view.setIdentity(); RenderSystem.applyModelViewMatrix();
        TextureTarget output = null;
        try (var guard = new CatPerformanceOutline.State(); var framebuffer = new PerformanceSceneSnapshot.Target()) {
            output = new TextureTarget(960, 480, true, Minecraft.ON_OSX); output.bindWrite(true);
            RenderSystem.clearColor(.08f, .1f, .12f, 1); RenderSystem.clearDepth(1); RenderSystem.depthMask(true);
            RenderSystem.clear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT | org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 960, 480, 0, 1000, 21000), VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.setShaderColor(1, 1, 1, 1); RenderSystem.enableDepthTest(); RenderSystem.enableCull();
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            var buffers = mc.renderBuffers().bufferSource();
            for (int column = 0; column < 3; column++) {
                var be = entity(mc, column != 0, Direction.NORTH);
                animation(be, column == 2 ? 11 : 0);
                if(fuel>0)be.tank.setFluid(new net.minecraftforge.fluids.FluidStack(LaoWuMod.HISSING_GAS.get(),fuel));
                if (loaded) be.inventory.setStackInSlot(0,
                        CatPancakeItem.variantStack(net.minecraft.resources.ResourceLocation.tryParse("minecraft:red")));
                var pose = new PoseStack();
                pose.translate(160 + column * 320, 330, -11000); pose.scale(140, -140, 140);
                pose.mulPose(Axis.XP.rotationDegrees(25)); pose.mulPose(Axis.YP.rotationDegrees(225)); pose.translate(-.5, 0, -.5);
                mc.getBlockRenderer().renderSingleBlock(be.getBlockState(), pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                renderer(mc, be).render(be, .5f, pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            buffers.endBatch();
            try (var image = new NativeImage(960, 480, false)) {
                RenderSystem.bindTexture(output.getColorTextureId()); image.downloadTexture(0, false);
                int background = image.getPixelRGBA(0, 0), visible = 0;
                for (int y = 0; y < 480; y++) for (int x = 0; x < 960; x++) {
                    pixels[y * 960 + x] = image.getPixelRGBA(x, y);
                    if (pixels[y * 960 + x] != background) visible++;
                }
                check(visible > 5000, "Deployment scenes emitted no real GPU pixels");
                image.flipY(); image.writeToFile(java.nio.file.Path.of(file));
            }
        } finally {
            if (output != null) output.destroyBuffers();
            view.popPose(); RenderSystem.applyModelViewMatrix(); RenderSystem.setProjectionMatrix(projection, sorting);
        }
        return pixels;
    }
}
