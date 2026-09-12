package cn.laowu.mod.client;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

/** Framebuffer images have one mip level; font/shader-pack samplers must not override their storage. */
final class PerformanceSamplers implements AutoCloseable {
    // Our shader JSONs use at most four samplers. Leave all other texture units untouched.
    private static final int UNITS = 4;
    private static int nesting;
    private final int[] previous;
    private boolean closed;

    PerformanceSamplers() {
        if (nesting++ != 0 || !(GL.getCapabilities().OpenGL33 || GL.getCapabilities().GL_ARB_sampler_objects)) {
            previous = null;
            return;
        }
        previous = new int[UNITS];
        for (int unit = 0; unit < UNITS; unit++) {
            previous[unit] = GL30.glGetIntegeri(GL33.GL_SAMPLER_BINDING, unit);
            if (previous[unit] != 0) GL33.glBindSampler(unit, 0);
        }
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        --nesting;
        if (previous != null) for (int unit = 0; unit < UNITS; unit++) {
            if (previous[unit] != 0) GL33.glBindSampler(unit, previous[unit]);
        }
    }
}
