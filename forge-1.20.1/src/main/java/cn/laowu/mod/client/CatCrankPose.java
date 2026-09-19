package cn.laowu.mod.client;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Pure pose/coordinate math: MCP-authored body motion with exact rod contacts. */
public final class CatCrankPose {
    public static final int BONES = 8, STRIDE = 9, VALUES = BONES * STRIDE;
    private static final float REFERENCE_SEAT_Y = 1.784F;
    private static final String[] NAMES = {"head", "body", "left_hind_leg", "right_hind_leg",
            "left_front_leg", "right_front_leg", "tail1", "tail2"};
    private static final float[][] CLIP = load();

    public static boolean available() { return CLIP != null; }

    /** The exact Create handle transform: face the block, then rotate about the positive axis. */
    public static Vector3f handleOffset(int facingX, int facingZ, double degrees, float rodZ) {
        Vector3f point = new Vector3f(-7, 0, rodZ - 8);
        point.rotate(new Quaternionf().rotateTo(0, 0, -1, facingX, 0, facingZ));
        float angle = (float) Math.toRadians(degrees % 360.0);
        return point.rotateAxis(angle, facingX != 0 ? 1 : 0, 0, facingZ != 0 ? 1 : 0);
    }

    /** Inverse of LivingEntityRenderer's yaw, before ModelPart's X/Y reflection. */
    public static Vector3f toLocal(Vector3f worldPixels, float bodyYaw) {
        return worldPixels.rotateY((float) Math.toRadians(bodyYaw - 180.0F));
    }

    public static void evaluate(double phase, float outerScale, boolean baby, float seatY,
                                Vector3f leftGrip, Vector3f rightGrip, float[] output) {
        if (CLIP == null || output.length != VALUES || !Double.isFinite(phase)
                || !Float.isFinite(outerScale) || outerScale <= 0 || !Float.isFinite(seatY))
            throw new IllegalArgumentException("Invalid crank pose input");
        double wrapped = phase - Math.floor(phase);
        double frame = wrapped * (CLIP.length - 1);
        int a = Math.min((int) frame, CLIP.length - 2);
        float alpha = (float) (frame - a);
        for (int i = 0; i < VALUES; i++)
            output[i] = CLIP[a][i] + (CLIP[a + 1][i] - CLIP[a][i]) * alpha;

        float size = outerScale * (baby ? 0.5F : 1.0F);
        Vector3f[] positions = new Vector3f[BONES];
        Quaternionf[] rotations = new Quaternionf[BONES];
        Vector3f[] scales = new Vector3f[BONES];
        for (int b = 0; b < BONES; b++) {
            int i = b * STRIDE;
            positions[b] = new Vector3f(output[i] * size,
                    seatY + (output[i + 1] - REFERENCE_SEAT_Y) * size, output[i + 2] * size);
            rotations[b] = new Quaternionf().rotationZYX(output[i + 5], output[i + 4], output[i + 3]);
            scales[b] = new Vector3f(output[i + 6], output[i + 7], output[i + 8]);
        }

        // Only a smaller cat follows the handle with its body when it cannot reach.
        // Full-size cats retain the authored torso position; retarget only their paws.
        // Do not turn a kitten's short forelegs into arbitrarily long telescoping arms.
        Vector3f shift = new Vector3f();
        Vector3f[] grips = {leftGrip, rightGrip};
        for (int pass = 0; size < 1.0F && pass < 8; pass++) {
            for (int side = 0; side < 2; side++) {
                int b = 4 + side;
                Vector3f shoulder = anchor(positions[b], rotations[b], size).add(shift);
                Vector3f delta = new Vector3f(grips[side]).sub(shoulder);
                // Honor the reach deliberately authored for this frame. A fixed cap
                // would pull an adult torso forward and undo its rearward offset.
                float length = delta.length();
                float maximum = Math.max(11.8F, 10 * scales[b].y + 0.01F) * size;
                if (length > maximum) shift.add(delta.mul((length - maximum) / length));
            }
        }
        for (Vector3f p : positions) p.add(shift);
        for (int side = 0; side < 2; side++) {
            int b = 4 + side;
            Vector3f shoulder = anchor(positions[b], rotations[b], size);
            link(positions, rotations, scales, b, shoulder, grips[side], 10, 1, size);
        }

        for (int side = 0; side < 2; side++) {
            int b = 2 + side;
            Vector3f hip = anchor(positions[b], rotations[b], 2 * size);
            Vector3f toe = new Vector3f(0, -6 * scales[b].y * size, 2 * size)
                    .rotate(rotations[b]).add(positions[b]).sub(shift);
            float margin = Math.min(3, 1.25F * size);
            toe.x = clamp(toe.x, -8 + margin, 8 - margin);
            toe.z = clamp(toe.z, -8 + margin, 8 - margin);
            // Rising is authored by extending the legs, never by stretching the torso.
            // Keep that authored reach so the feet remain planted while standing up.
            float maximum = Math.max(7.2F, 6 * scales[b].y + 0.01F) * size;
            for (int pass = 0; pass < 16; pass++) {
                Quaternionf q = downTo(new Vector3f(toe).sub(hip));
                float bottom = size * (Math.abs(new Vector3f(1, 0, 0).rotate(q).y)
                        + Math.abs(new Vector3f(0, 0, 1).rotate(q).y));
                toe.y = Math.max(toe.y, seatY + bottom + 0.04F);
                Vector3f direction = new Vector3f(toe).sub(hip);
                if (direction.length() > maximum)
                    toe.set(direction.normalize(maximum).add(hip));
            }
            link(positions, rotations, scales, b, hip, toe, 6, 2, size);
        }

        for (int b = 0; b < BONES; b++) {
            int i = b * STRIDE;
            Vector3f p = positions[b], s = scales[b];
            float partSize = outerScale * (baby ? b == 0 ? 0.75F : 0.5F : 1);
            float yOffset = baby && b == 0 ? -1.484F * outerScale : 0.016F * outerScale;
            output[i] = -p.x / partSize;
            output[i + 1] = 24 - (p.y - yOffset) / partSize;
            output[i + 2] = p.z / partSize - (baby && b == 0 ? 4 : 0);
            Vector3f r = rotations[b].getEulerAnglesZYX(new Vector3f());
            output[i + 3] = -r.x;
            output[i + 4] = -r.y;
            output[i + 5] = r.z;
            output[i + 6] = s.x;
            output[i + 7] = s.y;
            output[i + 8] = s.z;
        }
    }

    private static Vector3f anchor(Vector3f p, Quaternionf q, float depth) {
        return new Vector3f(0, 0, depth).rotate(q).add(p);
    }

    private static void link(Vector3f[] p, Quaternionf[] q, Vector3f[] s, int b,
                             Vector3f from, Vector3f to, float length, float depth, float size) {
        Vector3f delta = new Vector3f(to).sub(from);
        q[b] = downTo(delta);
        p[b].set(from).sub(new Vector3f(0, 0, depth * size).rotate(q[b]));
        s[b].set(1, Math.max(0.001F, delta.length() / (length * size)), 1);
    }

    private static Quaternionf downTo(Vector3f direction) {
        if (direction.lengthSquared() < 1.0E-10F) return new Quaternionf();
        return new Quaternionf().rotationTo(new Vector3f(0, -1, 0), new Vector3f(direction).normalize());
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float[][] load() {
        String resource = "/assets/laowu/cat_animation_clips/cat_engineering_crank.json";
        try (var input = CatCrankPose.class.getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("Missing " + resource);
            var json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            if (json.get("format_version").getAsInt() != 1
                    || json.get("duration_seconds").getAsDouble() != 3
                    || json.get("sample_rate").getAsInt() != 60)
                throw new IllegalStateException("Invalid clip header");
            var names = json.getAsJsonArray("bone_order");
            if (names.size() != BONES) throw new IllegalStateException("Invalid bone count");
            for (int i = 0; i < BONES; i++)
                if (!NAMES[i].equals(names.get(i).getAsString())) throw new IllegalStateException("Invalid bone order");
            var frames = json.getAsJsonArray("frames");
            if (frames.size() != 181) throw new IllegalStateException("Invalid sample count");
            float[][] result = new float[frames.size()][VALUES];
            for (int f = 0; f < result.length; f++) {
                var row = frames.get(f).getAsJsonArray();
                if (row.size() != VALUES) throw new IllegalStateException("Invalid pose");
                for (int c = 0; c < VALUES; c++) {
                    float value = row.get(c).getAsFloat();
                    if (!Float.isFinite(value) || c % STRIDE >= 6 && value <= 0)
                        throw new IllegalStateException("Invalid bone component");
                    result[f][c] = value;
                }
            }
            return result;
        } catch (Exception failure) {
            LogUtils.getLogger().error("Unable to load engineering cat animation; using vanilla pose", failure);
            return null;
        }
    }

    private CatCrankPose() {}
}
