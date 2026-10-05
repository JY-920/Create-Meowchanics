package cn.laowu.mod.compat.create;

/** Decides which mixins can be loaded without resolving optional Create classes. */
public final class CreateMixinPolicy {
    public static boolean shouldApply(String targetClassName, String mixinClassName, boolean createPresent) {
        if (createPresent) return true;
        return !targetClassName.startsWith("com.simibubi.create.")
                && !targetClassName.startsWith("net.createmod.")
                && !mixinClassName.endsWith(".ContraptionCatPancakeCollisionMixin");
    }

    private CreateMixinPolicy() {}
}

