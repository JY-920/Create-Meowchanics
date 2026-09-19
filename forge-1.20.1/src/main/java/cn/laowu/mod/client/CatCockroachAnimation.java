package cn.laowu.mod.client;

import com.google.gson.*;
import net.minecraft.client.model.geom.ModelPart;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Original Blockbench tracks split into full dash and wings-only hurt reactions. */
public final class CatCockroachAnimation {
    private static final JsonObject DASH = load("cockroach_dash"), WINGS = load("cockroach_wings");
    private static JsonObject load(String name) {
        try (var input = CatCockroachAnimation.class.getResourceAsStream("/assets/laowu/cat_animation_clips/" + name + ".json")) {
            if (input == null) throw new IOException(name);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) { throw new IllegalStateException("Cannot read supplied cockroach animation " + name, e); }
    }
    private static float[] sample(JsonObject clip, String bone, String channel, float age) {
        var frames = clip.getAsJsonObject("tracks").getAsJsonArray(bone);
        if (frames == null) return new float[3];
        float length = clip.get("length").getAsFloat();
        float time = clip.get("loop").getAsBoolean() ? Math.max(0, age / 20) % length : Math.min(length, Math.max(0, age / 20));
        var selected = new ArrayList<JsonObject>();
        for (var entry : frames) if (entry.getAsJsonObject().get("channel").getAsString().equals(channel)) selected.add(entry.getAsJsonObject());
        if (selected.isEmpty()) return new float[3];
        int i = 0; while (i + 1 < selected.size() && selected.get(i + 1).get("time").getAsFloat() <= time) i++;
        JsonObject a = selected.get(i), b = selected.get(Math.min(i + 1, selected.size() - 1));
        float span = b.get("time").getAsFloat() - a.get("time").getAsFloat();
        float t = span <= 0 ? 0 : Math.min(1, Math.max(0, (time - a.get("time").getAsFloat()) / span));
        float[] result = new float[3];
        for (int n = 0; n < 3; n++) {
            float v1 = a.getAsJsonArray("value").get(n).getAsFloat(), v2 = b.getAsJsonArray("value").get(n).getAsFloat();
            if (a.get("interpolation").getAsString().equals("catmullrom")) {
                float v0 = selected.get(Math.max(0, i - 1)).getAsJsonArray("value").get(n).getAsFloat();
                float v3 = selected.get(Math.min(selected.size() - 1, i + 2)).getAsJsonArray("value").get(n).getAsFloat();
                result[n] = .5F * ((2*v1) + (-v0+v2)*t + (2*v0-5*v1+4*v2-v3)*t*t + (-v0+3*v1-3*v2+v3)*t*t*t);
            } else result[n] = v1 + (v2-v1)*t;
        }
        return result;
    }
    public static void dash(float age, ModelPart leftHind, ModelPart rightHind, ModelPart leftFront, ModelPart rightFront) {
        limb(leftHind, "left_hind_leg", age); limb(rightHind, "right_hind_leg", age);
        limb(leftFront, "left_front_leg", age); limb(rightFront, "right_front_leg", age);
    }
    private static void limb(ModelPart part, String name, float age) {
        part.resetPose();
        float[] pos = sample(DASH, name, "position", age), rot = sample(DASH, name, "rotation", age);
        part.x -= pos[0]; part.y -= pos[1]; part.z += pos[2];
        part.xRot = rad(-rot[0]); part.yRot = rad(-rot[1]); part.zRot = rad(rot[2]);
    }
    public static void wings(Map<String, RuntimeBlockbenchModel.GroupTransform> transforms, int mode, float age) {
        wings(transforms,mode,age,1);
    }
    public static void wings(Map<String, RuntimeBlockbenchModel.GroupTransform> transforms, int mode, float age,float weight) {
        if (mode == 0 || weight <= 0) return;
        JsonObject clip = mode == 1 ? WINGS : DASH;
        for (String bone : List.of("group10", "group11")) {
            float[] rot = sample(clip, bone, "rotation", age);
            // Outfit import reflects X before native Y conversion; do not mirror these a second time.
            transforms.put(bone, RuntimeBlockbenchModel.GroupTransform.rotation(rad(-rot[0])*weight, rad(-rot[1])*weight, rad(rot[2])*weight));
        }
    }
    private static float rad(float deg) { return deg * (float)Math.PI / 180; }
    private CatCockroachAnimation() {}
}
