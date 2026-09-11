import cn.laowu.mod.client.CatTeamPreviewFrame;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class CatTeamPreviewFrameRegression {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        int cases = 0;
        for (int yaw = -180; yaw <= 180; yaw += 15) {
            for (int pitch = -90; pitch <= 90; pitch += 15) {
                for (float roll : new float[]{-.6f, 0, .6f}) {
                    // Camera.setRotation in Minecraft 1.20.1: forward is rotated +Z.
                    Quaternionf camera = new Quaternionf().rotationYXZ(
                            (float) Math.toRadians(-yaw), (float) Math.toRadians(pitch), roll);
                    Quaternionf original = new Quaternionf(camera);
                    Vector3f forward = new Vector3f(0, 0, 1).rotate(camera);
                    Vector3f up = new Vector3f(0, 1, 0).rotate(camera);
                    Vector3f right = new Vector3f(-1, 0, 0).rotate(camera);
                    Quaternionf badge = CatTeamPreviewFrame.facingCamera(camera);
                    Vector3f face = new Vector3f(0, 0, .002f).rotate(badge);
                    Vector3f hat = new Vector3f(0, 0, .004f).rotate(badge);
                    check(face.dot(forward) < -.0019f, "Face behind opaque frame");
                    check(hat.dot(forward) < face.dot(forward), "Hat behind face");
                    check(new Vector3f(1, 0, 0).rotate(badge).dot(right) > .9999f,
                            "Face UV horizontally mirrored");
                    check(new Vector3f(0, 1, 0).rotate(badge).dot(up) > .9999f,
                            "Badge upside down");
                    check(camera.equals(original), "Mutated shared camera rotation");
                    // Reproduce the old bug: the original positive depth goes behind the frame.
                    check(new Vector3f(0, 0, .002f).rotate(camera).dot(forward) > .0019f,
                            "Old-bug reproduction failed");
                    cases++;
                }
            }
        }
        System.out.println("PASS: " + cases + " camera poses; frame < face < hat; UV orientation; no camera mutation");
    }
}
