package cn.laowu.mod.client;
import cn.laowu.mod.*;
import cn.laowu.mod.create.*;
import cn.laowu.mod.item.CatPancakeItem;
import cn.laowu.mod.genetics.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

/** Weakly cached, unticked client preview; it never produces sounds or changes the saved cat. */
final class CarrierCatPreview {
    private record Preview(ItemStack stack, Cat cat) {}
    private static final java.util.Map<CatCarrierBlockEntity, java.lang.ref.WeakReference<Preview>> CACHE = new java.util.WeakHashMap<>();
    static void render(CatCarrierBlockEntity box, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (box.catStack().isEmpty() || box.getLevel() == null) { CACHE.remove(box); return; }
        var reference = CACHE.get(box);
        Preview cached = reference == null ? null : reference.get();
        if (cached == null || cached.cat().level() != box.getLevel()
                || !ItemStack.isSameItemSameComponents(cached.stack(), box.catStack())) {
            ItemStack stack = box.catStack();
            Cat cat = net.minecraft.world.entity.EntityType.CAT.create(box.getLevel());
            if (cat == null) return;
            var tag = cn.laowu.mod.item.ItemCustomData.copy(stack);
            if (tag != null && tag.contains(CatPancakeItem.CAT_DATA_TAG))
                cat.load(tag.getCompound(CatPancakeItem.CAT_DATA_TAG).copy());
            else {
                cat.setVariant(net.minecraft.core.registries.BuiltInRegistries.CAT_VARIANT.getHolder(CatPancakeItem.variantId(stack))
                        .orElseGet(() -> net.minecraft.core.registries.BuiltInRegistries.CAT_VARIANT.getHolderOrThrow(net.minecraft.world.entity.animal.CatVariant.RED)));
            }
            CatGenomeData.read(stack).ifPresent(g -> CatGenomeData.set(cat, g));
            CatTraitData.read(stack).ifPresent(t -> CatTraitData.set(cat, t));
            cat.getPersistentData().putBoolean(CatClothesData.EQUIPPED_TAG, CatPancakeItem.getOutfit(stack) != CatOutfitType.NONE);
            cat.getPersistentData().putString(CatClothesData.OUTFIT_TAG, CatPancakeItem.getOutfit(stack).id());
            cat.setAge(CatPancakeItem.isBaby(stack) ? -24000 : 0);
            CatPoseData.setPose(cat, 0);
            cat.setSilent(true);
            cat.setInSittingPose(true);
            cat.setCustomNameVisible(false);
            cached = new Preview(stack.copy(), cat);
            CACHE.put(box, new java.lang.ref.WeakReference<>(cached));
        }
        Cat cat = cached.cat();
        cat.setPos(box.getBlockPos().getX() + .5, box.getBlockPos().getY(), box.getBlockPos().getZ() + .5);
        cat.setYRot(0); cat.yRotO = 0; cat.yBodyRot = 0; cat.yBodyRotO = 0; cat.setYHeadRot(0); cat.yHeadRotO = 0;
        pose.pushPose();
        pose.translate(.5, .075, .5);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-box.getBlockState().getValue(CatCarrierBlock.FACING).toYRot()));
        int bigLevel = CatTraitData.read(box.catStack()).orElse(CatTraitProfile.EMPTY).level(CatTrait.BIG_CHONKY_CAT);
        float scale = .55F / (bigLevel > 0 ? CatTrait.BIG_CHONKY_CAT.bigCatScalePercent(bigLevel) / 100F : 1F);
        pose.scale(scale, scale, scale);
        net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().render(cat, 0, 0, 0, 0, partial, pose, buffers, light);
        pose.popPose();
    }
}
