package cn.laowu.mod.mixin;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.compat.create.CreateIntegration;
import cn.laowu.mod.item.CatPancakeItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Roll only committed output, never recipe preview or furnace capacity checks. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class StandaloneCatDoughFurnaceMixin {
    // Explicit named/SRG selectors: the release annotation processor does not
    // emit a usable mapping for this Forge-patched private method.
    @Inject(method={
            "burn(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/item/crafting/Recipe;Lnet/minecraft/core/NonNullList;I)Z",
            "m_266209_(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/item/crafting/Recipe;Lnet/minecraft/core/NonNullList;I)Z"
    },at=@At("RETURN"),remap=false,require=1)
    private void laowu$rollCookedCat(RegistryAccess registries, Recipe<?> recipe,
                                           NonNullList<ItemStack> items, int maxStackSize,
                                           CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValueZ() || CreateIntegration.isLoaded() || recipe == null
                || !LaoWuMod.id("standalone/cat_pancake_smelting").equals(recipe.getId())) return;
        ItemStack output=items.get(2);
        if (output.is(LaoWuMod.CAT_PANCAKE.get()) && output.getCount()==1)
            items.set(2,CatPancakeItem.randomVariantStack(
                    RandomSource.create(java.util.concurrent.ThreadLocalRandom.current().nextLong())));
    }
}
