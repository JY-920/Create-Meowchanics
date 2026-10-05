package cn.laowu.mod.compat.create;

import static cn.laowu.mod.LaoWuMod.*;
import cn.laowu.mod.client.CareerSuitTooltip;
import cn.laowu.mod.create.CatEngineBlockEntity;
import cn.laowu.mod.item.CatEngineerGogglesItem;
import cn.laowu.mod.item.TerminatorSuitItem;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/** Stress and tooltip setup lives entirely in the optional Create integration. */
public final class CreateStartup {
    public static void initialize() {
        BlockStressValues.CAPACITIES.register(CAT_ENGINE.get(),
                () -> (double) CatEngineBlockEntity.STRESS_CAPACITY_PER_RPM);
        BlockStressValues.setGeneratorSpeed((int) CatEngineBlockEntity.GENERATED_RPM)
                .accept(CAT_ENGINE.get());
        // Use Create's own kinetic tooltip, identical to diesel_engine:
        // capacity title, 64x RPM and the maximum 6144 SU line.
        TooltipModifier.REGISTRY.register(CAT_ENGINE_ITEM.get(),
                createAlwaysVisibleDescription(CAT_ENGINE_ITEM.get())
                        .andThen(new KineticStats(CAT_ENGINE.get())));
        registerAlwaysVisibleDescription(INFILTRATION_TANK_ITEM.get());
        registerAlwaysVisibleDescription(HISSING_COLLECTOR_ITEM.get());
        registerAlwaysVisibleDescription(DEVOURING_CAT_ITEM.get());
        registerAlwaysVisibleDescription(BASIC_BREEDING_BOX_ITEM.get());
        registerAlwaysVisibleDescription(INTERMEDIATE_BREEDING_BOX_ITEM.get());
        registerAlwaysVisibleDescription(ADVANCED_BREEDING_BOX_ITEM.get());
        registerAlwaysVisibleDescription(ADOPTION_BOX_ITEM.get());
        registerAlwaysVisibleDescription(WISH_ADOPTION_BOX_ITEM.get());
        registerAlwaysVisibleDescription(CAT_CARRIER_ITEM.get());
        registerAlwaysVisibleDescription(CAT_EDITOR_ITEM.get());
        registerAlwaysVisibleDescription(CAT_DEPLOYMENT_PLATFORM_ITEM.get());
        registerAlwaysVisibleDescription(CAT_EJECTING_DEPLOYMENT_PLATFORM_ITEM.get());
        registerAlwaysVisibleDescription(CAT_LASER_POINTER.get());
        registerAlwaysVisibleDescription(CAT_STORAGE_BOX.get());
        registerAlwaysVisibleDescription(cn.laowu.mod.create.CatMachineBlocks.CAT_AUTO_LASER_ITEM.get());
        registerAlwaysVisibleDescription(cn.laowu.mod.create.CatMachineBlocks.HAJI_BASIN_ITEM.get());
        registerAlwaysVisibleDescription(cn.laowu.mod.create.CatDepotRegistration.CAT_DEPOT_ITEM.get());
        registerAlwaysVisibleDescription(cn.laowu.mod.create.CatMachineBlocks.CAT_MIXER_ITEM.get());
        registerAlwaysVisibleDescription(cn.laowu.mod.create.CatMachineBlocks.CAT_PRESS_ITEM.get());
        registerDescription(CAT_CANNON.get());
        registerDescription(CAT_BALL.get());
        registerDescription(CAT_STRIP.get());
        registerAlwaysVisibleDescription(CAT_FOOD.get());
        registerAlwaysVisibleDescription(CAT_CAN.get());
        registerAlwaysVisibleDescription(GOLDEN_CAT_CAN.get());
        registerAlwaysVisibleDescription(ATTACK_CAT_CAN.get());
        registerAlwaysVisibleDescription(HEALTH_CAT_CAN.get());
        registerAlwaysVisibleDescription(SPEED_CAT_CAN.get());
        registerAlwaysVisibleDescription(STAMINA_CAT_CAN.get());
        registerAlwaysVisibleDescription(INTELLIGENCE_CAT_CAN.get());
        registerAlwaysVisibleDescription(LUCK_CAT_CAN.get());
        registerAlwaysVisibleDescription(GOLDEN_ATTACK_CAT_CAN.get());
        registerAlwaysVisibleDescription(GOLDEN_HEALTH_CAT_CAN.get());
        registerAlwaysVisibleDescription(GOLDEN_SPEED_CAT_CAN.get());
        registerAlwaysVisibleDescription(GOLDEN_STAMINA_CAT_CAN.get());
        registerAlwaysVisibleDescription(GOLDEN_INTELLIGENCE_CAT_CAN.get());
        registerAlwaysVisibleDescription(GOLDEN_LUCK_CAT_CAN.get());
        registerAlwaysVisibleDescription(SUPER_ATTACK_CAT_CAN.get());
        registerAlwaysVisibleDescription(SUPER_HEALTH_CAT_CAN.get());
        registerAlwaysVisibleDescription(SUPER_SPEED_CAT_CAN.get());
        registerAlwaysVisibleDescription(SUPER_STAMINA_CAT_CAN.get());
        registerAlwaysVisibleDescription(SUPER_INTELLIGENCE_CAT_CAN.get());
        registerAlwaysVisibleDescription(SUPER_LUCK_CAT_CAN.get());
        registerAlwaysVisibleDescription(DRIED_FISH.get());
        registerAlwaysVisibleDescription(GOLDEN_DRIED_FISH.get());
        registerAlwaysVisibleDescription(SUPER_DRIED_FISH.get());
        registerAlwaysVisibleDescription(BREEDING_CAT_FOOD.get());
        registerAlwaysVisibleDescription(MUTATION_CAT_FOOD.get());
        registerAlwaysVisibleDescription(ATTACK_BREEDING_CAT_FOOD.get());
        registerAlwaysVisibleDescription(HEALTH_BREEDING_CAT_FOOD.get());
        registerAlwaysVisibleDescription(SPEED_BREEDING_CAT_FOOD.get());
        registerAlwaysVisibleDescription(STAMINA_BREEDING_CAT_FOOD.get());
        registerAlwaysVisibleDescription(INTELLIGENCE_BREEDING_CAT_FOOD.get());
        registerAlwaysVisibleDescription(LUCK_BREEDING_CAT_FOOD.get());
        registerAlwaysVisibleDescription(CAT_SCANNER.get());
        registerDescription(CAT_ENGINEER_GOGGLES.get());
        GogglesItem.addIsWearingPredicate(CatEngineerGogglesItem::isWornBy);
        if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
            cn.laowu.mod.compat.curios.CatGogglesCuriosCompat.registerCurio();
        }
        registerDescription(CAT_HELMET.get());
        registerDescription(CAT_CHESTPLATE.get());
        registerDescription(CAT_LEGGINGS.get());
        registerDescription(CAT_BOOTS.get());
        registerDescription(CAT_SWORD.get());
        registerDescription(CAT_PICKAXE.get());
        registerDescription(CAT_AXE.get());
        registerDescription(CAT_SHOVEL.get());
        registerDescription(CAT_HOE.get());
    }

    private static TooltipModifier createDescription(Item item) {
        return new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE);
    }

    private static void registerDescription(Item item) {
        TooltipModifier.REGISTRY.register(item, createDescription(item));
    }

    /** A Create-formatted summary without the usual hold-Shift gate. */
    private static TooltipModifier createAlwaysVisibleDescription(Item item) {
        return event -> {
            String summary = Component.translatable(
                    item.getDescriptionId() + ".tooltip.summary").getString();
            event.getToolTip().addAll(Math.min(1, event.getToolTip().size()),
                    TooltipHelper.cutStringTextComponent(
                            summary, FontHelper.Palette.STANDARD_CREATE));
        };
    }

    private static void registerAlwaysVisibleDescription(Item item) {
        TooltipModifier.REGISTRY.register(item, createAlwaysVisibleDescription(item));
    }

    private CreateStartup() {}
}
