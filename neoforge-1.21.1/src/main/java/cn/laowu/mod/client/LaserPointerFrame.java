package cn.laowu.mod.client;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Replaces animated hand rotation with a stable +Z-forward frame, retaining position and scale. */
public final class LaserPointerFrame {
    public static boolean orient(Matrix4f current, Vector3f localTarget, Vector3f renderUp,
                                 Vector3f fallbackUp) {
        Vector3f forward = current.transformDirection(localTarget, new Vector3f());
        if (!forward.isFinite() || forward.lengthSquared() < 1e-6F) return false;
        forward.normalize();
        Vector3f up = new Vector3f(renderUp).normalize();
        Vector3f right = new Vector3f(up).cross(forward);
        if (right.lengthSquared() < 1e-5F) right.set(fallbackUp).cross(forward);
        if (right.lengthSquared() < 1e-6F) return false;
        right.normalize();
        up.set(forward).cross(right).normalize();
        Vector3f scale = current.getScale(new Vector3f());
        Vector3f position = current.getTranslation(new Vector3f());
        current.identity()
                .setColumn(0, new Vector4f(right.mul(scale.x), 0))
                .setColumn(1, new Vector4f(up.mul(scale.y), 0))
                .setColumn(2, new Vector4f(forward.mul(scale.z), 0))
                .setColumn(3, new Vector4f(position, 1));
        return true;
    }
    private LaserPointerFrame() {}
}
