package cn.laowu.mod.compat.curios;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Common-side Curios bridge; loaded only when the optional mod is present. */
public final class CatGogglesCuriosCompat {
    /** Registers the cat goggles as a Curios item equippable in the head slot. */
    public static void registerCurio() {
        CuriosApi.registerCurio(LaoWuMod.CAT_ENGINEER_GOGGLES.get(), new ICurioItem() {
            @Override
            public boolean canEquip(SlotContext slotContext, ItemStack stack) {
                return "head".equals(slotContext.identifier());
            }
        });
    }

    public static boolean isWornBy(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.isEquipped(LaoWuMod.CAT_ENGINEER_GOGGLES.get()))
                .orElse(false);
    }

    private CatGogglesCuriosCompat() {}
}
