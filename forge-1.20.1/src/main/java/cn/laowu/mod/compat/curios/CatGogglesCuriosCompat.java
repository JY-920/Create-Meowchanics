package cn.laowu.mod.compat.curios;

import cn.laowu.mod.LaoWuMod;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;

/** Common-side Curios bridge; loaded only when the optional mod is present. */
public final class CatGogglesCuriosCompat {
    public static boolean isWornBy(Player player) {
        return CuriosApi.getCuriosInventory(player).resolve()
                .map(handler -> handler.isEquipped(LaoWuMod.CAT_ENGINEER_GOGGLES.get()))
                .orElse(false);
    }

    private CatGogglesCuriosCompat() {}
}
