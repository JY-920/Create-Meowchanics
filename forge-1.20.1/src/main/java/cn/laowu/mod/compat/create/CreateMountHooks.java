package cn.laowu.mod.compat.create;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
public final class CreateMountHooks {
    public static boolean matches(ItemStack stack) { return com.simibubi.create.AllItems.WRENCH.isIn(stack); }
    public static boolean isHanging(Player player) {
        return com.simibubi.create.content.kinetics.chainConveyor.ServerChainConveyorHandler.hangingPlayers.containsKey(player.getUUID());
    }
    private CreateMountHooks() {}
}
