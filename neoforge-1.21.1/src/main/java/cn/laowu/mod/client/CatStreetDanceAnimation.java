package cn.laowu.mod.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.model.geom.ModelPart;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Plays the approved Blockbench Thomas flare on the eight vanilla cat bones. */
public final class CatStreetDanceAnimation {
    public static final float SOURCE_SECONDS = 2.4F;
    public static final float PLAYBACK_SPEED = 2.0F;
    public static final float PERIOD_TICKS = SOURCE_SECONDS * 20.0F / PLAYBACK_SPEED;
    private static final String RESOURCE = "/assets/laowu/animations/cat_thomas_flare.json";
    private static final String[] BONES = {
            "head", "body", "left_hind_leg", "right_hind_leg",
            "left_front_leg", "right_front_leg", "tail1", "tail2"
    };
    private static final int COMPONENTS = 9;
    private static final int STRIDE = BONES.length * COMPONENTS;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Clip CLIP = load();

    public static boolean isAvailable() {
        return CLIP != null;
    }

    public static void apply(float age, ModelPart head, ModelPart body,
                             ModelPart leftHind, ModelPart rightHind,
                             ModelPart leftFront, ModelPart rightFront,
                             ModelPart tail1, ModelPart tail2) {
        if (CLIP == null || !Float.isFinite(age)) return;
        float wrapped = age % PERIOD_TICKS;
        if (wrapped < 0.0F) wrapped += PERIOD_TICKS;
        float time = wrapped * PLAYBACK_SPEED / 20.0F;
        int frame = Math.min((int) (time * CLIP.sampleRate), CLIP.times.length - 2);
        // Blockbench serializes times to five decimals. Respect those actual
        // times instead of introducing even tiny drift from uniform sampling.
        while (frame > 0 && time < CLIP.times[frame]) frame--;
        while (frame < CLIP.times.length - 2 && time > CLIP.times[frame + 1]) frame++;
        float alpha = Math.max(0.0F, Math.min(1.0F,
                (time - CLIP.times[frame]) / (CLIP.times[frame + 1] - CLIP.times[frame])));
        int from = frame * STRIDE, to = from + STRIDE;
        applyBone(head, 0, from, to, alpha);
        applyBone(body, 1, from, to, alpha);
        applyBone(leftHind, 2, from, to, alpha);
        applyBone(rightHind, 3, from, to, alpha);
        applyBone(leftFront, 4, from, to, alpha);
        applyBone(rightFront, 5, from, to, alpha);
        applyBone(tail1, 6, from, to, alpha);
        applyBone(tail2, 7, from, to, alpha);
    }

    private static void applyBone(ModelPart part, int bone, int from, int to, float alpha) {
        from += bone * COMPONENTS;
        to += bone * COMPONENTS;
        part.setPos(lerp(from, to, 0, alpha), lerp(from, to, 1, alpha), lerp(from, to, 2, alpha));
        // These are continuous, unwrapped ZYX Euler tracks from Blockbench.
        // Shortest-angle wrapping here would change the approved motion.
        part.xRot = lerp(from, to, 3, alpha);
        part.yRot = lerp(from, to, 4, alpha);
        part.zRot = lerp(from, to, 5, alpha);
        part.xScale = lerp(from, to, 6, alpha);
        part.yScale = lerp(from, to, 7, alpha);
        part.zScale = lerp(from, to, 8, alpha);
    }

    private static float lerp(int from, int to, int component, float alpha) {
        float a = CLIP.values[from + component];
        return a + (CLIP.values[to + component] - a) * alpha;
    }

    private static Clip load() {
        // One small immutable clip per client, not a per-cat parse or file read.
        try (InputStream stream = CatStreetDanceAnimation.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing " + RESOURCE);
            JsonObject json = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (json.get("format_version").getAsInt() != 1
                    || Math.abs(json.get("duration_seconds").getAsFloat() - SOURCE_SECONDS) > 0.0001F) {
                throw new IllegalStateException("Unsupported Thomas flare clip");
            }
            JsonArray names = json.getAsJsonArray("bone_order");
            if (names.size() != BONES.length) throw new IllegalStateException("Incorrect bone count");
            for (int bone = 0; bone < BONES.length; bone++) {
                if (!BONES[bone].equals(names.get(bone).getAsString())) {
                    throw new IllegalStateException("Incorrect bone order");
                }
            }
            JsonArray sourceFrames = json.getAsJsonArray("frames");
            JsonArray sourceTimes = json.getAsJsonArray("frame_times");
            if (sourceFrames.size() != 145 || sourceTimes.size() != sourceFrames.size()) {
                throw new IllegalStateException("Incorrect frame count");
            }
            int rate = json.get("sample_rate").getAsInt();
            if (rate != 60) throw new IllegalStateException("Incorrect sample rate");
            float[] times = new float[sourceTimes.size()];
            float[] values = new float[sourceFrames.size() * STRIDE];
            for (int frame = 0; frame < sourceFrames.size(); frame++) {
                times[frame] = sourceTimes.get(frame).getAsFloat();
                if (!Float.isFinite(times[frame])
                        || Math.abs(times[frame] - frame / (float) rate) > 0.00001F) {
                    throw new IllegalStateException("Invalid sample time");
                }
                JsonArray row = sourceFrames.get(frame).getAsJsonArray();
                if (row.size() != STRIDE) throw new IllegalStateException("Incomplete pose");
                for (int component = 0; component < STRIDE; component++) {
                    float value = row.get(component).getAsFloat();
                    if (!Float.isFinite(value) || (component % COMPONENTS >= 6 && value <= 0.0F)) {
                        throw new IllegalStateException("Invalid pose component");
                    }
                    values[frame * STRIDE + component] = value;
                }
            }
            return new Clip(times, values, rate);
        } catch (Exception exception) {
            // An absent/corrupt resource must never prevent entering the world.
            LOGGER.error("Unable to load cat Thomas flare; retaining vanilla cat poses", exception);
            return null;
        }
    }

    private record Clip(float[] times, float[] values, int sampleRate) {}

    private CatStreetDanceAnimation() {}
}
