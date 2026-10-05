import cn.laowu.mod.compat.create.CreateMixinPolicy;

public final class CreateMixinPolicyRegression {
    public static void main(String[] args) {
        String prefix = "cn.laowu.mod.mixin.";
        check(!CreateMixinPolicy.shouldApply("com.simibubi.create.content.kinetics.belt.BeltBlock", prefix+"CatBeltCasingMixin", false), "Missing Create must skip its block mixins");
        check(!CreateMixinPolicy.shouldApply("net.createmod.ponder.foundation.PonderScene", prefix+"Example", false), "Missing Create must skip Create library targets");
        check(!CreateMixinPolicy.shouldApply("net.minecraft.world.entity.Entity", prefix+"ContraptionCatPancakeCollisionMixin", false), "Vanilla targets can still depend on Create contraptions");
        check(CreateMixinPolicy.shouldApply("net.minecraft.world.entity.animal.Cat", prefix+"CatNavigationMixin", false), "Cat navigation must remain active");
        check(CreateMixinPolicy.shouldApply("net.minecraft.world.entity.LivingEntity", prefix+"LivingEntityCatAccessoryDamageMixin", false), "Accessory damage must remain active");
        check(CreateMixinPolicy.shouldApply("com.simibubi.create.content.kinetics.belt.BeltBlock", prefix+"CatBeltCasingMixin", true), "Installed Create must keep its belt integration");
        check(CreateMixinPolicy.shouldApply("net.minecraft.world.entity.Entity", prefix+"ContraptionCatPancakeCollisionMixin", true), "Installed Create must keep contraption collision behavior");
        System.out.println("PASS: 7 optional Create mixin activation cases");
    }
    private static void check(boolean value, String message) { if(!value)throw new AssertionError(message); }
}
