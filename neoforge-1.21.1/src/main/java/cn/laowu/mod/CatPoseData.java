package cn.laowu.mod;

import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.animal.Cat;

public final class CatPoseData {
    public static final String TAG = "LaoWuHissingPose";
    public static final int NORMAL = 0;
    public static final int HISSING = 1;
    public static final int PANCAKE = 2;

    public static int getPose(Cat cat) {
        if (!cat.getPersistentData().contains(TAG, Tag.TAG_ANY_NUMERIC)) return 0;
        int pose = cat.getPersistentData().getInt(TAG);
        return pose >= NORMAL && pose <= PANCAKE ? pose : NORMAL;
    }
    public static boolean isHissing(Cat cat) { return getPose(cat) == HISSING; }
    public static boolean isPancake(Cat cat) { return getPose(cat) == PANCAKE; }
    public static void setPose(Cat cat, int pose) {
        cat.getPersistentData().putInt(TAG, Math.max(NORMAL, Math.min(PANCAKE, pose)));
    }
    private CatPoseData() {}
}
