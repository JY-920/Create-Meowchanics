package cn.laowu.mod;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final Pose POSE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> HELD_ITEM_TRANSFORMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> GUI_ITEM_TRANSFORMS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        POSE = definePose(b, "hissing_pose", "哈气姿势");
        HELD_ITEM_TRANSFORMS = b
                .comment("游戏内按 K 调整过的手持物品显示参数。",
                        "每项格式：物品注册名|X旋转|Y旋转|Z旋转|X位移|Y位移|Z位移|缩放。",
                        "不同物品会分别保存，只影响第一人称和第三人称手持显示。")
                .defineListAllowEmpty("held_item_transforms", List.of(), value -> value instanceof String);
        GUI_ITEM_TRANSFORMS = b
                .comment("游戏内按 K 调整过的物品栏与容器 GUI 物品显示参数。",
                        "每项格式：物品注册名|X旋转|Y旋转|Z旋转|X位移|Y位移|Z位移|缩放。",
                        "该配置与手持显示参数独立保存，不影响掉落物和方块展示框。")
                .defineListAllowEmpty("gui_item_transforms", List.of(), value -> value instanceof String);
        SPEC = b.build();
    }

    private static Pose definePose(ForgeConfigSpec.Builder b, String group, String title) {
        b.comment(title + "。Pitch 为 X 轴角度，Roll 为 Z 轴角度，Offset 单位为模型像素。").push(group);
        Pose p = new Pose(
                angle(b, "bodyPitch", 90, "躯干前后倾斜角度。"),
                offset(b, "bodyYOffset", -1, "躯干高度；负值向上。"),
                offset(b, "bodyZOffset", -1, "躯干前后偏移；负值向前。"),
                angle(b, "chestPitch", 40, "胸部相对躯干的旋转角度。"),
                offset(b, "chestYOffset", -1.65, "胸部高度微调；负值向上。"),
                offset(b, "chestZOffset", 2, "胸部前后位置；负值向猫头方向，正值向尾巴方向。"),
                angle(b, "bellyPitch", 0, "腹部相对躯干的旋转角度。"),
                offset(b, "bellyYOffset", -3, "腹部高度微调；负值向上，用于拱背。"),
                offset(b, "bellyZOffset", 1, "腹部前后位置；负值向猫头方向，正值向尾巴方向。"),
                angle(b, "hipsPitch", -35, "臀部相对躯干的旋转角度。"),
                offset(b, "hipsYOffset", -1, "臀部高度微调；负值向上。"),
                offset(b, "hipsZOffset", 1, "臀部前后位置；负值向猫头方向，正值向尾巴方向。"),
                angle(b, "headPitch", 20, "头部俯仰角度。"),
                angle(b, "headRoll", 20, "从正面看头部左右歪斜角度。"),
                offset(b, "headYOffset", 1, "头部高度；负值向上。"),
                offset(b, "headZOffset", 1, "头部前后偏移；负值向前。"),
                angle(b, "hindLegPitch", -12, "后腿前后旋转角度。"),
                offset(b, "hindLegYOffset", -5, "后腿高度；负值向上。"),
                offset(b, "hindLegZOffset", 0, "后腿前后偏移。"),
                scale(b, "hindLegLengthScale", 2, "后腿长度倍率；1.0 为原版。"),
                angle(b, "frontLegPitch", 10, "前腿前后旋转角度。"),
                offset(b, "frontLegYOffset", 0, "前腿高度；负值向上。"),
                offset(b, "frontLegZOffset", 1, "前腿前后偏移。"),
                scale(b, "frontLegLengthScale", 1, "前腿长度倍率；1.0 为原版。"),
                angle(b, "tailBasePitch", -35, "尾巴根部旋转角度；负值使尾巴向腹部下方收起。"),
                offset(b, "tailBaseYOffset", 0, "尾巴根部高度；负值向上。"),
                offset(b, "tailBaseZOffset", 0, "尾巴根部前后偏移；负值向前。"),
                scale(b, "tailBaseLengthScale", 0.65, "尾巴根部长度倍率；夹尾巴建议 0.5~0.8。"),
                angle(b, "tailTipPitch", -20, "尾巴末端旋转角度；负值使尾尖夹向后腿之间。"),
                offset(b, "tailTipYOffset", -0.7, "尾巴末端高度；数值已对齐缩短后的根段末端。"),
                offset(b, "tailTipZOffset", -9, "尾巴末端前后偏移；数值已对齐缩短后的根段末端。"),
                scale(b, "tailTipLengthScale", 0.55, "尾巴末端长度倍率；夹尾巴建议 0.5~0.8。")
        );
        b.pop();
        return p;
    }

    private static ForgeConfigSpec.DoubleValue angle(ForgeConfigSpec.Builder b, String k, double v, String c) {
        return b.comment(c).defineInRange(k, v, -180, 180);
    }
    private static ForgeConfigSpec.DoubleValue offset(ForgeConfigSpec.Builder b, String k, double v, String c) {
        return b.comment(c).defineInRange(k, v, -16, 16);
    }
    private static ForgeConfigSpec.DoubleValue scale(ForgeConfigSpec.Builder b, String k, double v, String c) {
        return b.comment(c).defineInRange(k, v, 0.5, 2);
    }

    public record Pose(
            ForgeConfigSpec.DoubleValue bodyPitch, ForgeConfigSpec.DoubleValue bodyYOffset, ForgeConfigSpec.DoubleValue bodyZOffset,
            ForgeConfigSpec.DoubleValue chestPitch, ForgeConfigSpec.DoubleValue chestYOffset, ForgeConfigSpec.DoubleValue chestZOffset,
            ForgeConfigSpec.DoubleValue bellyPitch, ForgeConfigSpec.DoubleValue bellyYOffset, ForgeConfigSpec.DoubleValue bellyZOffset,
            ForgeConfigSpec.DoubleValue hipsPitch, ForgeConfigSpec.DoubleValue hipsYOffset, ForgeConfigSpec.DoubleValue hipsZOffset,
            ForgeConfigSpec.DoubleValue headPitch, ForgeConfigSpec.DoubleValue headRoll, ForgeConfigSpec.DoubleValue headYOffset, ForgeConfigSpec.DoubleValue headZOffset,
            ForgeConfigSpec.DoubleValue hindLegPitch, ForgeConfigSpec.DoubleValue hindLegYOffset, ForgeConfigSpec.DoubleValue hindLegZOffset, ForgeConfigSpec.DoubleValue hindLegLengthScale,
            ForgeConfigSpec.DoubleValue frontLegPitch, ForgeConfigSpec.DoubleValue frontLegYOffset, ForgeConfigSpec.DoubleValue frontLegZOffset, ForgeConfigSpec.DoubleValue frontLegLengthScale,
            ForgeConfigSpec.DoubleValue tailBasePitch, ForgeConfigSpec.DoubleValue tailBaseYOffset,
            ForgeConfigSpec.DoubleValue tailBaseZOffset, ForgeConfigSpec.DoubleValue tailBaseLengthScale,
            ForgeConfigSpec.DoubleValue tailTipPitch, ForgeConfigSpec.DoubleValue tailTipYOffset,
            ForgeConfigSpec.DoubleValue tailTipZOffset, ForgeConfigSpec.DoubleValue tailTipLengthScale) {}

    private ClientConfig() {}
}
