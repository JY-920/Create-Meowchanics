package cn.laowu.mod.client;

import cn.laowu.mod.entity.CatGiantCarrier;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Samples the same authored clips stored in giant_cat_mount.bbmodel.
 * Returns a fresh map of pixel-space offsets and radian rotations per cat/render.
 */
public final class GiantCatAnimation {
    private static final ResourceLocation CURVES = ResourceLocation.fromNamespaceAndPath("laowu", "cat_animation_clips/giant_cat_mount.json");
    private static final Map<Cat, State> STATES = new WeakHashMap<>();
    private static Map<String, Clip> clips;

    private GiantCatAnimation() {}

    /** Resource reload hook; existing cats also restart their phase cleanly. */
    public static void clearCache() {
        clips = null;
        STATES.clear();
    }

    /** Presentation blend after sample(); reading it never advances the transition. */
    public static float restWeight(Cat cat) {
        State state = STATES.get(cat);
        return state == null ? 0.0F : state.rest * state.rest * (3.0F - 2.0F * state.rest);
    }

    public static Map<String, RuntimeBlockbenchModel.GroupTransform> sample(Cat cat, float partialTick) {
        if (cat == null) return Map.of();
        if (clips == null) clips = load();
        if (clips.isEmpty()) return Map.of();

        Entity motionSource = cat.getVehicle() != null ? cat.getVehicle() : cat;
        boolean mounted = motionSource instanceof CatGiantCarrier;
        double vx = mounted ? motionSource.getX() - motionSource.xo
                : motionSource.getDeltaMovement().x;
        double vz = mounted ? motionSource.getZ() - motionSource.zo
                : motionSource.getDeltaMovement().z;
        double speed = mounted ? ((CatGiantCarrier) motionSource).horizontalSpeed()
                : Math.sqrt(vx * vx + vz * vz);
        boolean ground = mounted ? ((CatGiantCarrier) motionSource).grounded()
                : motionSource.onGround();
        boolean windup = mounted && ((CatGiantCarrier) motionSource).jumpWindup() > 0;
        float age = cat.tickCount + partialTick;
        State state = STATES.computeIfAbsent(cat, ignored -> new State(age, ground));
        float deltaTicks = Math.max(0.0F, Math.min(2.0F, age - state.lastAge));
        state.lastAge = age;
        float follow = 1.0F - (float) Math.exp(-deltaTicks * 0.42F);
        state.motion += (Math.min(1.0F, (float) speed / 0.16F) - state.motion) * follow;
        state.run += (runBlend((float) speed) - state.run) * follow;
        // Only the pose flag is synchronized. Vanilla interaction predicts the
        // unsynchronized orderedToSit field locally, which may survive mounting.
        boolean resting = !mounted && ground && cat.isInSittingPose();
        state.rest += ((resting ? 1.0F : 0.0F) - state.rest)
                * (1.0F - (float) Math.exp(-deltaTicks * (resting ? 0.14F : 0.06F)));
        float restWeight = restWeight(cat);
        state.phase += deltaTicks / 20.0F * (1.0F / lerp(0.8F, 0.52F, state.run));
        state.phase -= (float) Math.floor(state.phase);

        if (windup) {
            if (!state.wasWindup) state.jumpTime = 0.0F;
            state.jumpTime = advanceJumpTime(state.jumpTime, deltaTicks / 20.0F,
                    ground, true, state.wasGrounded, state.wasWindup);
            state.landing = 0.0F;
        } else if (!ground) {
            state.jumpTime = advanceJumpTime(state.jumpTime, deltaTicks / 20.0F,
                    false, false, state.wasGrounded, state.wasWindup);
            state.landing = 0.0F;
        } else if (!state.wasGrounded) {
            state.jumpTime = 0.72F;
            state.landing = 1.0F;
        } else if (state.wasWindup) {
            state.jumpTime = 0.0F;
        } else if (state.landing > 0.0F) {
            state.jumpTime = Math.min(0.9F, state.jumpTime + deltaTicks / 20.0F);
            state.landing = Math.max(0.0F, state.landing - deltaTicks / 4.0F);
        }
        state.wasGrounded = ground;
        state.wasWindup = windup;

        // Lateral travel and yaw change anticipate the cat leaning into a turn.
        float yaw = (float) Math.toRadians(cat.getYRot());
        float sideways = speed < 0.015 ? 0.0F
                : (float) ((vx * Math.cos(yaw) + vz * Math.sin(yaw)) / speed);
        float yawChange = wrapDegrees(cat.getYRot() - cat.yRotO);
        float targetTurn = speed < 0.015 ? 0.0F
                : clamp(sideways * 0.025F + yawChange / 550.0F, -0.045F, 0.045F)
                * (1.0F - restWeight);
        state.turn += (targetTurn - state.turn) * (1.0F - (float) Math.exp(-deltaTicks * 0.3F));

        Map<String, float[]> pose = new HashMap<>();
        sampleInto(pose, clips.get("idle"), age / 20.0F, 1.0F - restWeight);
        float stride = locomotionWeight(ground, windup, state.motion) * (1.0F - restWeight);
        sampleInto(pose, clips.get("walk"), state.phase * length("walk"),
                stride * (1.0F - state.run));
        sampleInto(pose, clips.get("run"), state.phase * length("run"),
                stride * state.run);
        float jumpWeight = windup || !ground ? 1.0F : state.landing;
        sampleInto(pose, clips.get("jump"), state.jumpTime, jumpWeight);
        sampleInto(pose, clips.get("rest"), restWeight * length("rest"), 1.0F);

        addRotation(pose, "group5", 0.0F, 0.0F, state.turn);
        addRotation(pose, "head", 0.0F, 0.0F, -state.turn * 0.15F);
        addRotation(pose, "tail1", 0.0F, -state.turn * 0.8F, 0.0F);
        addRotation(pose, "tail2", 0.0F, -state.turn * 1.2F, 0.0F);

        Map<String, RuntimeBlockbenchModel.GroupTransform> output = new HashMap<>();
        for (Map.Entry<String, float[]> entry : pose.entrySet()) {
            float[] v = entry.getValue();
            output.put(entry.getKey(), RuntimeBlockbenchModel.GroupTransform.scaled(
                    v[0], v[1], v[2], v[3], v[4], v[5],
                    clamp(1.0F + v[6], 0.55F, 1.45F),
                    clamp(1.0F + v[7], 0.55F, 1.45F),
                    clamp(1.0F + v[8], 0.55F, 1.45F)));
        }
        return output;
    }

    private static float length(String name) {
        Clip clip = clips.get(name);
        return clip == null ? 1.0F : clip.length;
    }

    private static void sampleInto(Map<String, float[]> pose, Clip clip, float seconds, float weight) {
        if (clip == null || weight <= 0.0001F) return;
        float time = clip.loop ? seconds % clip.length : clamp(seconds, 0.0F, clip.length);
        for (Map.Entry<String, Channels> entry : clip.bones.entrySet()) {
            float[] dest = pose.computeIfAbsent(entry.getKey(), ignored -> new float[9]);
            Channels channels = entry.getValue();
            accumulate(dest, channels.position, time, weight, 0, false);
            accumulate(dest, channels.rotation, time, weight, 3, true);
            accumulate(dest, channels.scale, time, weight, 6, false);
        }
    }

    private static void accumulate(float[] dest, float[][] keys, float time,
                                   float weight, int offset, boolean degrees) {
        if (keys == null || keys.length == 0) return;
        float[] value = interpolate(keys, time);
        float[] angles = degrees ? rotationRadians(value[0], value[1], value[2]) : value;
        for (int axis = 0; axis < 3; axis++) {
            float component = angles[axis];
            if (offset == 6) component -= 1.0F;
            dest[offset + axis] += component * weight;
        }
    }

    /** Zero-tangent cubic Hermite keeps all authored keys and loop seams smooth. */
    static float[] interpolate(float[][] keys, float time) {
        if (time <= keys[0][0]) return new float[] { keys[0][1], keys[0][2], keys[0][3] };
        for (int i = 1; i < keys.length; i++) {
            float[] end = keys[i];
            if (time > end[0]) continue;
            float[] start = keys[i - 1];
            float u = clamp((time - start[0]) / (end[0] - start[0]), 0.0F, 1.0F);
            float smooth = u * u * (3.0F - 2.0F * u);
            return new float[] {
                    lerp(start[1], end[1], smooth),
                    lerp(start[2], end[2], smooth),
                    lerp(start[3], end[3], smooth)
            };
        }
        float[] end = keys[keys.length - 1];
        return new float[] { end[1], end[2], end[3] };
    }

    private static void addRotation(Map<String, float[]> pose, String bone,
                                    float x, float y, float z) {
        float[] value = pose.computeIfAbsent(bone, ignored -> new float[9]);
        value[3] += x;
        value[4] += y;
        value[5] += z;
    }

    private static Map<String, Clip> load() {
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResourceOrThrow(CURVES);
            try (Reader reader = resource.openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                Map<String, Clip> loaded = new HashMap<>();
                for (Map.Entry<String, JsonElement> clipEntry : root.getAsJsonObject("clips").entrySet()) {
                    JsonObject raw = clipEntry.getValue().getAsJsonObject();
                    Map<String, Channels> bones = new HashMap<>();
                    for (Map.Entry<String, JsonElement> bone : raw.getAsJsonObject("bones").entrySet()) {
                        JsonObject channels = bone.getValue().getAsJsonObject();
                        bones.put(bone.getKey(), new Channels(
                                readChannel(channels, "position"),
                                readChannel(channels, "rotation"),
                                readChannel(channels, "scale")));
                    }
                    loaded.put(clipEntry.getKey(), new Clip(raw.get("length").getAsFloat(),
                            raw.get("loop").getAsBoolean(), bones));
                }
                return loaded;
            }
        } catch (Exception exception) {
            // Missing reload-time assets must never break cat rendering.
            return Map.of();
        }
    }

    private static float[][] readChannel(JsonObject channels, String name) {
        if (!channels.has(name)) return null;
        JsonArray raw = channels.getAsJsonArray(name);
        float[][] keys = new float[raw.size()][4];
        for (int i = 0; i < raw.size(); i++) {
            JsonArray frame = raw.get(i).getAsJsonArray();
            for (int j = 0; j < 4; j++) keys[i][j] = frame.get(j).getAsFloat();
        }
        return keys;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    static float runBlend(float speed) {
        return clamp((speed - 0.235F) / 0.065F, 0.0F, 1.0F);
    }

    static float locomotionWeight(boolean grounded, boolean windup, float motion) {
        return grounded && !windup ? motion : 0.0F;
    }

    static float advanceJumpTime(float current, float deltaSeconds,
                                 boolean grounded, boolean windup,
                                 boolean wasGrounded, boolean wasWindup) {
        if (windup) return Math.min(0.20F, current + deltaSeconds);
        if (grounded) return current;
        float start = wasGrounded ? wasWindup ? Math.max(0.20F, current) : 0.0F : current;
        return Math.min(0.68F, start + deltaSeconds);
    }

    static float[] rotationRadians(float x, float y, float z) {
        return new float[] {
                -(float) Math.toRadians(x),
                (float) Math.toRadians(y),
                -(float) Math.toRadians(z)
        };
    }

    private static float lerp(float start, float end, float weight) {
        return start + (end - start) * weight;
    }

    private static float wrapDegrees(float value) {
        value %= 360.0F;
        if (value >= 180.0F) value -= 360.0F;
        if (value < -180.0F) value += 360.0F;
        return value;
    }

    private record Clip(float length, boolean loop, Map<String, Channels> bones) {}
    private record Channels(float[][] position, float[][] rotation, float[][] scale) {}

    private static final class State {
        private float lastAge;
        private float phase;
        private float motion;
        private float run;
        private float jumpTime;
        private float landing;
        private float turn;
        private float rest;
        private boolean wasGrounded;
        private boolean wasWindup;

        private State(float age, boolean grounded) {
            this.lastAge = age;
            this.wasGrounded = grounded;
            this.jumpTime = grounded ? 0.0F : 0.3F;
        }
    }
}
