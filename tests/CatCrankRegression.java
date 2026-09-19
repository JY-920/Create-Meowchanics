import cn.laowu.mod.client.CatCrankPose;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class CatCrankRegression {
    private static int checks;
    private static float maximumError, minimumClearance = Float.MAX_VALUE;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static Vector3f point(float[] pose, int bone, float x, float y, float z, float scale, boolean baby) {
        int i = bone * 9;
        Vector3f p = new Vector3f(x * pose[i + 6], y * pose[i + 7], z * pose[i + 8])
                .rotate(new Quaternionf().rotationZYX(pose[i + 5], pose[i + 4], pose[i + 3]))
                .add(pose[i], pose[i + 1], pose[i + 2]);
        float partScale = scale * (baby ? bone == 0 ? .75F : .5F : 1);
        float offset = baby && bone == 0 ? -1.484F * scale : .016F * scale;
        return new Vector3f(-p.x * partScale, (24 - p.y) * partScale + offset,
                (p.z + (baby && bone == 0 ? 4 : 0)) * partScale);
    }
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--missing-resource")) {
            check(!CatCrankPose.available(), "Absent clip safely unavailable");
            System.out.println("PASS: safe missing crank animation fallback");
            return;
        }
        check(CatCrankPose.available(), "MCP animation available");
        int[][] directions = {{0,1,90},{0,-1,270},{1,0,0},{-1,0,180}};
        for (int[] d : directions) for (boolean baby : new boolean[]{false,true})
            for (float scale : new float[]{1,1.15F,1.45F,1.75F})
                for (float seatY : new float[]{-.44F,.0666667F,1.8F,2.25F})
                for (int frame = 0; frame <= 720; frame++) {
                    double angle = frame * .5 - 180;
                    Vector3f left = CatCrankPose.toLocal(CatCrankPose.handleOffset(d[0], d[1], angle, 4), d[2]);
                    Vector3f right = CatCrankPose.toLocal(CatCrankPose.handleOffset(d[0], d[1], angle, 7), d[2]);
                    check(Math.abs(left.x + 4) < .0001F && Math.abs(right.x + 1) < .0001F, "Rod axis faces correct side");
                    check(Math.abs(left.distance(right) - 3) < .0001F, "Separate 3px grip sites");
                    double phase = Math.atan2(left.y, left.z) / (Math.PI * 2);
                    left.y += seatY + 16; right.y += seatY + 16;
                    float[] pose = new float[CatCrankPose.VALUES];
                    CatCrankPose.evaluate(phase, scale, baby, seatY, left, right, pose);
                    for (float value : pose) check(Float.isFinite(value), "Finite pose");
                    check(Math.abs(pose[15] - 1) < .00001F && Math.abs(pose[16] - .72F) < .00001F
                            && Math.abs(pose[17] - 1) < .00001F, "Torso dimensions stay fixed throughout the cycle");
                    float effectiveSize = scale * (baby ? .5F : 1);
                    Vector3f expectedHead = point(pose, 1, .35F, .1F, -1.4F, scale, baby)
                            .sub(0, 0, 4 * effectiveSize);
                    check(point(pose, 0, 0, 0, 0, scale, baby).distance(expectedHead) < .004F,
                            "Only torso and boxes retreat; head keeps its forward reference");
                    if (!baby) {
                        float expectedBodyZ = 13.3F + 3.6F * (float) Math.cos(phase * Math.PI * 2);
                        check(Math.abs(pose[11] - expectedBodyZ) < .001F,
                                "Final adult pose preserves full 4px retreat, scale=" + scale
                                        + ", phase=" + phase + ", bodyZ=" + pose[11]);
                        float upper = Math.max(0, (float) Math.sin(phase * Math.PI * 2));
                        double cycle = phase + .25 - Math.floor(phase + .25);
                        double rise = cycle < .5 ? cycle * 2 : (1 - cycle) * 2;
                        double stand = rise * rise * rise * (10 + rise * (-15 + 6 * rise));
                        float expectedY = seatY + ((float) (17.45 + 5.7 * stand) - 1.784F) * scale;
                        // Allow the small interpolation error of the 60 Hz source clip.
                        check(Math.abs(point(pose, 1, 0, 0, 0, scale, false).y - expectedY) < .005F,
                                "Raise rigid torso as handle rises, phase=" + phase + ", scale=" + scale);
                        if (scale == 1 && upper > .9999F)
                            check(pose[43] < 1.12F && pose[52] < 1.12F, "Standing reduces high-point arm stretch");
                    }
                    for (int side = 0; side < 2; side++) {
                        float error = point(pose, 4 + side, 0, 10, 1, scale, baby).distance(side == 0 ? left : right);
                        maximumError = Math.max(maximumError, error);
                        check(error < .001F, "Paw contact, " + baby + "/" + scale + "/" + angle + ": " + error);
                    }
                    for (int bone = 2; bone < 4; bone++) {
                        float footClearance = Float.MAX_VALUE;
                        for (float x : new float[]{-1,1}) for (float y : new float[]{0,6}) for (float z : new float[]{1,3}) {
                            float clearance = point(pose, bone, x, y, z, scale, baby).y - seatY;
                            minimumClearance = Math.min(minimumClearance, clearance);
                            footClearance = Math.min(footClearance, clearance);
                            check(clearance > -.005F, "Hind foot does not cross seat: " + clearance);
                        }
                        if (!baby && scale == 1)
                            check(footClearance < .07F, "Ordinary adult feet stay planted, not hovering: " + footClearance);
                    }
                }
        System.out.println("PASS: " + checks + " crank checks; maximum contact error " + maximumError
                + "px, minimum foot clearance " + minimumClearance + "px");
    }
}
