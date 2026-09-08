package cn.laowu.mod.item;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.compat.curios.CatGogglesCuriosCompat;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

/** Create goggles with a cat-themed appearance; functionality is inherited unchanged. */
public final class CatEngineerGogglesItem extends GogglesItem {
    public CatEngineerGogglesItem(Properties properties) {
        super(properties);
    }

    /** Checks both the vanilla helmet slot and Curios' optional head slot. */
    public static boolean isWornBy(Player player) {
        if (player == null) return false;
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(LaoWuMod.CAT_ENGINEER_GOGGLES.get())) {
            return true;
        }
        return ModList.get().isLoaded("curios")
                && CatGogglesCuriosCompat.isWornBy(player);
    }
}
