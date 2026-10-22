package cn.laowu.mod.mixin;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.compat.create.CreateIntegration;
import cn.laowu.mod.item.CatPancakeItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Roll only committed output, never recipe preview or furnace capacity checks. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class StandaloneCatDoughFurnaceMixin {
    @Inject(method="burn(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/item/crafting/RecipeHolder;Lnet/minecraft/core/NonNullList;ILnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;)Z",at=@At("RETURN"))
    private static void laowu$rollCookedCat(RegistryAccess registries, RecipeHolder<?> recipe,
                                           NonNullList<ItemStack> items, int maxStackSize, AbstractFurnaceBlockEntity furnace,
                                           CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValueZ() || CreateIntegration.isLoaded() || recipe == null
                || !LaoWuMod.id("standalone/cat_pancake_smelting").equals(recipe.id())) return;
        ItemStack output=items.get(2);
        if (output.is(LaoWuMod.CAT_PANCAKE.get()) && output.getCount()==1)
            items.set(2,CatPancakeItem.randomVariantStack(
                    RandomSource.create(java.util.concurrent.ThreadLocalRandom.current().nextLong())));
    }
}
