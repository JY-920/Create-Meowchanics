package cn.laowu.mod;
import cn.laowu.mod.compat.create.CreateIntegration;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
/** Standalone cats use empty-hand riding interaction; Create mode retains the original wrench. */
public final class CatMountTool {
    public static boolean matches(ItemStack stack) {
        return CreateIntegration.isLoaded() ? cn.laowu.mod.compat.create.CreateMountHooks.matches(stack) : stack.isEmpty();
    }
    public static boolean isHanging(Player player) {
        return CreateIntegration.isLoaded() && cn.laowu.mod.compat.create.CreateMountHooks.isHanging(player);
    }
    private CatMountTool() {}
}
