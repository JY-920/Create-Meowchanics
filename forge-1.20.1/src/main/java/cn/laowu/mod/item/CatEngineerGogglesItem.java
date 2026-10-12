package cn.laowu.mod.item;

import cn.laowu.mod.LaoWuMod;
import cn.laowu.mod.compat.curios.CatGogglesCuriosCompat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

/** Cat goggles work independently; Create recognizes them through its wearing predicate. */
public final class CatEngineerGogglesItem extends Item implements Equipable {
    public CatEngineerGogglesItem(Properties properties) {
        super(properties);
        DispenserBlock.registerBehavior(this, ArmorItem.DISPENSE_ITEM_BEHAVIOR);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return swapWithEquipmentSlot(this, level, player, hand);
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
