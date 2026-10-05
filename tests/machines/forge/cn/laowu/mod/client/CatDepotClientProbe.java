package cn.laowu.mod.client;

import cn.laowu.mod.create.CatDepotRegistration;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import com.simibubi.create.content.logistics.depot.DepotRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;

final class CatDepotClientProbe {
    static void verify(Minecraft mc) throws Exception {
        var state = CatDepotRegistration.CAT_DEPOT.get().defaultBlockState();
        var model = mc.getBlockRenderer().getBlockModel(state);
        CatMachinesClientProbe.check(model != mc.getModelManager().getMissingModel(), "Cat depot shell must bake");
        var quads = model.getQuads(state, null, RandomSource.create(1));
        CatMachinesClientProbe.check(quads.size() == 42, "All seven updated authored depot parts must bake their six faces");
        for (var quad : quads) CatMachinesClientProbe.check(!quad.getSprite().contents().name().equals(MissingTextureAtlasSprite.getLocation()), "Depot must not contain missing texture faces");
        var depot = CatDepotRegistration.DEPOT_BE.get().create(BlockPos.ZERO, state);
        net.minecraft.client.renderer.blockentity.BlockEntityRenderer<?> renderer = mc.getBlockEntityRenderDispatcher().getRenderer(depot);
        CatMachinesClientProbe.check(renderer instanceof DepotRenderer,
                "Cat depot must use native Create item rendering");
        depot.setHeldItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT));
        var nativeVertices = new ItemVertices();
        var catVertices = new ItemVertices();
        renderHeld(new DepotRenderer(null), depot, new PoseStack(), type -> nativeVertices);
        renderHeld((DepotRenderer) renderer, depot, new PoseStack(), type -> catVertices);
        CatMachinesClientProbe.check(nativeVertices.count > 0 && nativeVertices.count == catVertices.count,
                "Cat depot must emit the same actual held-item geometry as native depot");
        CatMachinesClientProbe.check(Math.abs(catVertices.minY - nativeVertices.minY) < .00001
                        && Math.abs(catVertices.maxY - nativeVertices.maxY) < .00001,
                "Held item vertices must follow cat's 13px tabletop with native clearance");
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var view = RenderSystem.getModelViewStack();
        view.pushPose(); view.setIdentity(); RenderSystem.applyModelViewMatrix();
        TextureTarget output = null;
        try (var guard = new CatPerformanceOutline.State(); var framebuffer = new PerformanceSceneSnapshot.Target()) {
            output = new TextureTarget(400, 400, true, Minecraft.ON_OSX); output.bindWrite(true);
            RenderSystem.clearColor(.10f, .12f, .15f, 1); RenderSystem.clearDepth(1); RenderSystem.depthMask(true);
            RenderSystem.clear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT | org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 400, 400, 0, 1000, 21000), VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.setShaderColor(1, 1, 1, 1); RenderSystem.enableDepthTest(); RenderSystem.enableCull();
            var pose = new PoseStack(); pose.translate(200, 210, -11000);
            pose.scale(190, -190, 190); pose.mulPose(Axis.XP.rotationDegrees(25)); pose.mulPose(Axis.YP.rotationDegrees(225));
            pose.translate(-.5, -.5, -.5);
            var buffers = mc.renderBuffers().bufferSource();
            mc.getBlockRenderer().renderSingleBlock(state, pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            renderHeld((DepotRenderer) renderer, depot, pose, buffers);
            buffers.endBatch();
            try (var image = new NativeImage(400, 400, false)) {
                RenderSystem.bindTexture(output.getColorTextureId()); image.downloadTexture(0, false);
                int background = image.getPixelRGBA(0, 0), visible = 0;
                for (int y = 0; y < 400; y++) for (int x = 0; x < 400; x++) if (image.getPixelRGBA(x, y) != background) visible++;
                CatMachinesClientProbe.check(visible > 5000, "Cat depot must emit actual GPU pixels");
                image.flipY(); image.writeToFile(java.nio.file.Path.of("cat-depot-preview.png"));
            }
        } finally {
            if (output != null) output.destroyBuffers();
            view.popPose(); RenderSystem.applyModelViewMatrix(); RenderSystem.setProjectionMatrix(projection, sorting);
        }
        System.out.println("PASS: CAT DEPOT baked shell, native item renderer, GPU preview");
    }

    private static final class ItemVertices implements com.mojang.blaze3d.vertex.VertexConsumer {
        int count;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        public com.mojang.blaze3d.vertex.VertexConsumer vertex(double x, double y, double z) {
            count++; minY = Math.min(minY, y); maxY = Math.max(maxY, y); return this;
        }
        public com.mojang.blaze3d.vertex.VertexConsumer color(int r, int g, int b, int a) { return this; }
        public com.mojang.blaze3d.vertex.VertexConsumer uv(float u, float v) { return this; }
        public com.mojang.blaze3d.vertex.VertexConsumer overlayCoords(int u, int v) { return this; }
        public com.mojang.blaze3d.vertex.VertexConsumer uv2(int u, int v) { return this; }
        public com.mojang.blaze3d.vertex.VertexConsumer normal(float x, float y, float z) { return this; }
        public void endVertex() {}
        public void defaultColor(int r, int g, int b, int a) {}
        public void unsetDefaultColor() {}
    }

    // Invoke the real renderer core outside a loaded world; vanilla item models need no world.
    // Public SafeBlockEntityRenderer.render intentionally rejects worldless preview entities.
    private static void renderHeld(DepotRenderer renderer, com.simibubi.create.content.logistics.depot.DepotBlockEntity depot,
                                   PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers) throws Exception {
        var method = DepotRenderer.class.getDeclaredMethod("renderSafe", com.simibubi.create.content.logistics.depot.DepotBlockEntity.class,
                float.class, PoseStack.class, net.minecraft.client.renderer.MultiBufferSource.class, int.class, int.class);
        method.setAccessible(true);
        var before = new Matrix4f(pose.last().pose());
        method.invoke(renderer, depot, 0f, pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        CatMachinesClientProbe.check(before.equals(pose.last().pose()), "Depot rendering must restore caller pose");
    }
}
