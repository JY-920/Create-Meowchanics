package cn.laowu.mod.client;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;

/** Forge 1.20.1 camera-local +Z looks away from the viewer, unlike 1.21.1. */
public final class CatTeamPreviewFrame {
    public static Quaternionf facingCamera(Quaternionfc cameraRotation) {
        // Turn the badge, not the camera: +Z overlays are now in front of the frame,
        // and +X points screen-right so the player's face is not mirrored.
        return new Quaternionf(cameraRotation).rotateY((float) Math.PI);
    }

    private CatTeamPreviewFrame() {}
}
