package cn.laowu.mod.client;

import java.util.HashMap;
import java.util.Map;
import cn.laowu.mod.client.RuntimeBlockbenchModel.GroupTransform;

/** Deterministic, synchronized combat poses layered over the artist's locomotion clips. */
public final class GiantCatBossAnimation {
    private static final float PI = (float) Math.PI;
    private GiantCatBossAnimation() {}

    /** Phase numbers are the GiantCatBoss network contract; ticks are interpolated server phase age. */
    public static Map<String, GroupTransform> sample(byte phase, float ticks,
            float rollRecoveryStartTicks, Map<String, GroupTransform> locomotion) {
        return sample(phase, ticks, (byte) (phase == 5 ? 4 : 7),
                phase == 5 ? rollRecoveryStartTicks : 20, locomotion);
    }

    /** Recovery starts from the synchronized interrupted pose, also for late observers. */
    public static Map<String, GroupTransform> sample(byte phase, float ticks,
            byte recoveryFromPhase, float recoveryFromTicks, Map<String, GroupTransform> locomotion) {
        ticks = Math.max(0, ticks);
        if (phase == 5 && recoveryFromPhase != 3 && recoveryFromPhase != 4) {
            recoveryFromPhase = 4; recoveryFromTicks = 60;
        } else if (phase == 8 && recoveryFromPhase != 6 && recoveryFromPhase != 7) {
            recoveryFromPhase = 7; recoveryFromTicks = 20;
        }
        Map<String, GroupTransform> target = new HashMap<>();
        float weight = 0;
        switch (phase) {
            case 3 -> { target = roll(PI / 2); weight = smooth(ticks / 16); }
            case 4 -> { target = rolling(ticks, locomotion); weight = 1; }
            case 5 -> {
                if (recoveryFromPhase == 3) {
                    // Anticipation may still cancel normally before rolling.
                    target = sample((byte) 3, recoveryFromTicks, 0, locomotion);
                    weight = 1 - smooth(ticks / 30);
                } else {
                    target = finishInterruptedRoll(ticks, recoveryFromTicks, locomotion);
                    weight = 1;
                }
            }
            case 6 -> { target = crouch(); weight = smooth(ticks / 24); }
            case 7 -> { target = blend(crouch(), seated(), smooth(ticks / 10)); weight = 1; }
            case 8 -> {
                if (recoveryFromPhase == 6) {
                    // Cancelled anticipation is not an impact: simply unsquash.
                    target = sample((byte) 6, recoveryFromTicks, 0, locomotion);
                    weight = 1 - smooth(ticks / 40);
                } else {
                    // A low ceiling can cause landing before the airborne pose
                    // reaches its seated key. Settle from that exact partial pose.
                    var landing = sample((byte) 7, recoveryFromTicks, 0, locomotion);
                    target = blend(landing, seated(), smooth(ticks / 8));
                    weight = 1 - smooth((ticks - 8) / 32);
                }
            }
            default -> { }
        }
        return blend(locomotion, target, weight);
    }

    /** Finish the last revolution already upright, keeping the three-turn budget. */
    private static Map<String, GroupTransform> rolling(float ticks, Map<String, GroupTransform> locomotion) {
        float angle = rollAngle(ticks);
        var pose = blend(locomotion, roll(angle), 1 - smooth((ticks - 40) / 20));
        setRollAngle(pose, angle);
        return pose;
    }

    private static float rollAngle(float ticks) {
        ticks = Math.max(0, Math.min(60, ticks));
        if (ticks <= 40) return PI / 2 + ticks * PI / 10;
        return forwardAngle(4.5F * PI, 6 * PI, 2 * PI, (ticks - 40) / 20);
    }

    private static float rollSpeed(float ticks) {
        if (ticks <= 40) return PI / 10;
        if (ticks >= 60) return 0;
        float u = (ticks - 40) / 20;
        return ((6*u-6*u*u)*1.5F*PI+(3*u*u-4*u+1)*2*PI)/20;
    }

    private static Map<String, GroupTransform> finishInterruptedRoll(float ticks, float sourceTicks,
            Map<String, GroupTransform> locomotion) {
        float start = rollAngle(sourceTicks);
        float end = sourceTicks >= 60 ? 6 * PI : (float) Math.ceil(start / (2 * PI)) * 2 * PI;
        float remaining = Math.max(0, end - start);
        // A collision continues to the NEXT upright orientation, never the
        // nearest reverse angle. Remaining recovery ticks are standing buffer.
        float duration = remaining < .0001F ? 8 : Math.max(4, Math.min(20, remaining / (PI / 10) * 1.5F));
        float u = clamp(ticks / duration);
        var pose = blend(rolling(sourceTicks, locomotion), locomotion, smooth(u));
        float tangent = Math.min(rollSpeed(sourceTicks) * duration, remaining * 2);
        setRollAngle(pose, forwardAngle(start, end, tangent, u));
        return pose;
    }

    /** Cubic Hermite with zero ending speed and a bounded forward starting tangent. */
    private static float forwardAngle(float start, float end, float tangent, float progress) {
        float u = clamp(progress);
        return start + (end-start)*smooth(u) + tangent*u*(1-u)*(1-u);
    }

    private static void setRollAngle(Map<String, GroupTransform> pose, float angle) {
        var root = pose.getOrDefault("group2", GroupTransform.IDENTITY);
        pose.put("group2", GroupTransform.scaled(root.x(),root.y(),root.z(),root.xRot(),root.yRot(),angle,
                root.scaleX(),root.scaleY(),root.scaleZ()));
    }

    private static Map<String, GroupTransform> roll(float angle) {
        Map<String, GroupTransform> pose = new HashMap<>();
        rotation(pose, "group2", 0, 0, angle);
        rotation(pose, "head", .12F, 0, 0);
        rotation(pose, "left_front_leg", -.65F, 0, -.2F);
        rotation(pose, "right_front_leg", -.65F, 0, .2F);
        rotation(pose, "left_hind_leg", .65F, 0, -.18F);
        rotation(pose, "right_hind_leg", .65F, 0, .18F);
        rotation(pose, "tail1", .5F, 0, 0);
        rotation(pose, "tail2", .45F, 0, 0);
        put(pose, "tail1_volume", 0, 0, 0, 0, 0, 0, 1.2F, 1, 1.2F);
        put(pose, "tail2", 0, 0, 0, .45F, 0, 0, 1.2F, 1, 1.2F);
        return pose;
    }

    private static Map<String, GroupTransform> crouch() {
        Map<String, GroupTransform> pose = new HashMap<>();
        put(pose, "group2", 0, 0, 0, 0, 0, 0, 1.08F, .78F, 1.04F);
        rotation(pose, "left_front_leg", -.22F, 0, -.08F);
        rotation(pose, "right_front_leg", -.22F, 0, .08F);
        rotation(pose, "left_hind_leg", .42F, 0, -.12F);
        rotation(pose, "right_hind_leg", .42F, 0, .12F);
        rotation(pose, "head", -.14F, 0, 0);
        return pose;
    }

    private static Map<String, GroupTransform> seated() {
        Map<String, GroupTransform> pose = new HashMap<>();
        // The torso's authored long axis becomes vertical. Counter-rotate the
        // head and forearms rather than reclining the entire cat with its face up.
        float upright = PI / 2;
        rotation(pose, "group2", -upright, 0, 0);
        put(pose, "head", 0, -2, 0, upright, 0, 0, 1, 1, 1);
        // At this root angle local +Z lowers a bone in world Y and local -Y
        // moves it in front of the chest. Short forearms reach forward and
        // slope gently down from the chest sides, leaving the paws suspended;
        // only the rear soles share the rump's ground-contact plane.
        put(pose, "left_front_leg", 2.5F, -8.55F, -.5F, upright-.9F, -.314F, 0, 1, .7F, 1);
        put(pose, "right_front_leg", -2.5F, -8.55F, -.5F, upright-.9F, .314F, 0, 1, .7F, 1);
        put(pose, "left_hind_leg", 2.2F, 0, 2, 0, 0, 0, 1, 1, 1);
        put(pose, "right_hind_leg", -2.2F, 0, 2, 0, 0, 0, 1, 1, 1);
        rotation(pose, "tail1", 2.3F, .28F, 0);
        rotation(pose, "tail2", .8F, .3F, 0);
        return pose;
    }

    private static Map<String, GroupTransform> blend(Map<String, GroupTransform> from,
            Map<String, GroupTransform> to, float weight) {
        Map<String, GroupTransform> result = new HashMap<>();
        var names = new java.util.HashSet<>(from.keySet()); names.addAll(to.keySet());
        for (String name : names) {
            var a = from.getOrDefault(name, GroupTransform.IDENTITY);
            var b = to.getOrDefault(name, GroupTransform.IDENTITY);
            result.put(name, GroupTransform.scaled(
                    lerp(a.x(),b.x(),weight),lerp(a.y(),b.y(),weight),lerp(a.z(),b.z(),weight),
                    lerp(a.xRot(),b.xRot(),weight),lerp(a.yRot(),b.yRot(),weight),lerp(a.zRot(),b.zRot(),weight),
                    lerp(a.scaleX(),b.scaleX(),weight),lerp(a.scaleY(),b.scaleY(),weight),lerp(a.scaleZ(),b.scaleZ(),weight)));
        }
        return result;
    }
    private static void rotation(Map<String, GroupTransform> pose,String bone,float x,float y,float z) {
        put(pose,bone,0,0,0,x,y,z,1,1,1);
    }
    private static void put(Map<String, GroupTransform> pose,String bone,float x,float y,float z,
            float rx,float ry,float rz,float sx,float sy,float sz) {
        pose.put(bone,GroupTransform.scaled(x,y,z,rx,ry,rz,sx,sy,sz));
    }
    private static float lerp(float a,float b,float t) { return a+(b-a)*t; }
    private static float clamp(float t) { return Math.max(0,Math.min(1,t)); }
    private static float smooth(float t) { t=clamp(t);return t*t*(3-2*t); }
}
