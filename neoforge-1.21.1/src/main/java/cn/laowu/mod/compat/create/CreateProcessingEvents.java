package cn.laowu.mod.compat.create;

import cn.laowu.mod.*;
import cn.laowu.mod.genetics.*;
import cn.laowu.mod.item.*;
import com.simibubi.create.content.kinetics.deployer.DeployerRecipeSearchEvent;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;

/** Create-only processing hooks, registered only while Create is present. */
public final class CreateProcessingEvents {
    /** Supplies component-bearing results for deployer recipes whose state cannot be static JSON. */
    @SubscribeEvent
    public static void preserveStatefulDeployerApplicationNbt(DeployerRecipeSearchEvent event) {
        var inventory = event.getInventory();
        ItemStack pancake = inventory.getItem(0);
        ItemStack held = inventory.getItem(1);
        var holder = event.getRecipe();

        if (pancake.is(LaoWuMod.CAT_TOTEM.get()) && held.is(Items.TOTEM_OF_UNDYING)) {
            if (!CatTotemItem.canLoad(pancake)) {
                event.setCanceled(true);
                return;
            }
            if (holder != null && holder.value() instanceof ProcessingRecipe<?, ?> recipe
                    && holder.id().equals(LaoWuMod.id("cat_totem_charging"))) {
                ItemStack result = pancake.copyWithCount(1);
                CatTotemItem.addCharge(result);
                recipe.enforceNextResult(() -> result.copy());
            }
            return;
        }
        if (!pancake.is(LaoWuMod.CAT_PANCAKE.get())) return;

        var blockMaterial = CatMaterialRegistry.blockMaterial(held);
        if (blockMaterial.isPresent()) {
            var level = event.getBlockEntity().getLevel();
            if (level == null) return;
            event.addRecipe(() -> asDeployerRecipe(level.getRecipeManager().byKey(
                    LaoWuMod.id("cat_pancake_block_material_deploying"))), 200);
            holder = event.getRecipe();
            if (holder != null && holder.value() instanceof ProcessingRecipe<?, ?> recipe
                    && holder.id().equals(LaoWuMod.id(
                    "cat_pancake_block_material_deploying"))) {
                ItemStack result = pancake.copyWithCount(1);
                CatGenomeData.set(result, CatGenome.uniform(blockMaterial.get()));
                recipe.enforceNextResult(() -> result.copy());
            }
            return;
        }

        if (held.getItem() instanceof PheromoneCatFoodItem) {
            if (CatPancakeItem.hasOwner(pancake)) {
                event.setCanceled(true);
                return;
            }
            if (holder == null || !(holder.value() instanceof ProcessingRecipe<?, ?> recipe)
                    || !holder.id().equals(LaoWuMod.id(
                    "pheromone_cat_food_item_application"))) return;

            var level = event.getBlockEntity().getLevel();
            if (level == null) return;
            var ownerId = PheromoneCatFoodItem.resolveOwner(level.getServer(), held);
            if (ownerId.isEmpty()) {
                event.setCanceled(true);
                return;
            }

            ItemStack result = pancake.copyWithCount(1);
            CatPancakeItem.setOwner(result, ownerId.get(), PheromoneCatFoodItem.ownerName(held).orElse(""));
            CatPancakeItem.makeAdult(result);
            recipe.enforceNextResult(() -> result.copy());
            return;
        }

        if (held.getItem() instanceof CatAttributeCanItem can) {
            if (holder == null || !(holder.value() instanceof ProcessingRecipe<?, ?> recipe)) return;
            var itemId = BuiltInRegistries.ITEM.getKey(held.getItem());
            if (itemId == null || !itemId.getNamespace().equals(LaoWuMod.MOD_ID)
                    || !holder.id().equals(LaoWuMod.id(
                    itemId.getPath() + "_item_application"))) return;

            var level = event.getBlockEntity().getLevel();
            if (level == null) return;
            ItemStack result = pancake.copyWithCount(1);
            CatAttributeProfile profile = CatAttributeData.ensure(result, level.getRandom());
            var trained = can.train(profile);
            if (trained.isEmpty()) {
                event.setCanceled(true);
                return;
            }
            CatAttributeData.set(result, trained.get());
            recipe.enforceNextResult(() -> result.copy());
            return;
        }

        if (held.getItem() instanceof CatTraitFishItem fish) {
            if (holder == null || !(holder.value() instanceof ProcessingRecipe<?, ?> recipe)) return;
            var itemId = BuiltInRegistries.ITEM.getKey(held.getItem());
            if (itemId == null || !itemId.getNamespace().equals(LaoWuMod.MOD_ID)
                    || !holder.id().equals(LaoWuMod.id(
                    itemId.getPath() + "_item_application"))) return;

            var level = event.getBlockEntity().getLevel();
            if (level == null) return;
            ItemStack result = pancake.copyWithCount(1);
            CatTraitProfile profile = CatTraitData.ensure(result, level.getRandom());
            var upgraded = fish.upgrade(profile, level.getRandom());
            if (upgraded.isEmpty()) {
                event.setCanceled(true);
                return;
            }
            CatTraitData.set(result, upgraded.get());
            recipe.enforceNextResult(() -> result.copy());
            return;
        }

        if (held.is(LaoWuMod.CAT_FOOD.get())) {
            if (!CatPancakeItem.isBaby(pancake)
                    || CatTraitData.read(pancake)
                    .map(profile -> profile.has(CatTrait.LOLI)).orElse(false)) {
                event.setCanceled(true);
                return;
            }
            if (holder != null && holder.value() instanceof ProcessingRecipe<?, ?> recipe
                    && holder.id().equals(LaoWuMod.id("cat_food_growing"))) {
                ItemStack result = pancake.copyWithCount(1);
                CatPancakeItem.makeAdult(result);
                recipe.enforceNextResult(() -> result.copy());
            }
            return;
        }

        CatOutfitType applyingType = held.getItem() instanceof TerminatorSuitItem suit
                ? suit.outfit() : CatOutfitType.NONE;
        boolean applying = applyingType != CatOutfitType.NONE;
        boolean shearing = held.is(Items.SHEARS);
        if (!applying && !shearing) return;

        CatOutfitType fittedType = CatPancakeItem.getOutfit(pancake);
        boolean fitted = fittedType != CatOutfitType.NONE;
        if (applying && (!CatPancakeItem.isTamed(pancake) || fitted)
                || shearing && !fitted) {
            event.setCanceled(true);
            return;
        }

        if (holder == null || !(holder.value() instanceof ProcessingRecipe<?, ?> recipe)
                || !holder.id().getNamespace().equals(LaoWuMod.MOD_ID)) return;
        String expectedRecipe = applying
                ? applyingType.id() + "_suit_item_application"
                : fittedType.id() + "_suit_shearing";
        if (!holder.id().getPath().startsWith(expectedRecipe)) return;

        ItemStack result = pancake.copyWithCount(1);
        if (applying) CatPancakeItem.equipOutfit(result, applyingType);
        else CatPancakeItem.removeOutfit(result);
        recipe.enforceNextResult(() -> result.copy());
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    private static java.util.Optional<? extends net.minecraft.world.item.crafting.RecipeHolder<? extends net.minecraft.world.item.crafting.Recipe<? extends net.minecraft.world.item.crafting.RecipeInput>>>
    asDeployerRecipe(java.util.Optional<net.minecraft.world.item.crafting.RecipeHolder<?>> recipe) {
        return (java.util.Optional) recipe;
    }


    private CreateProcessingEvents() {}
}

